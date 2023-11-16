/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.appender;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.FileUtils;

/**
 * Manages actual File I/O for File Appenders.
 */
public class FileManager extends OutputStreamManager {

    private static final FileManagerFactory FACTORY = new FileManagerFactory();

    private final boolean isAppend;
    private final boolean createOnDemand;
    private final boolean isLocking;
    private final String advertiseURI;
    private final int bufferSize;
    private final Set<PosixFilePermission> filePermissions;
    private final String fileOwner;
    private final String fileGroup;
    private final boolean attributeViewEnabled;

    /**
     * @deprecated
     */
    @Deprecated
    protected FileManager(
            final String fileName,
            final OutputStream os,
            final boolean append,
            final boolean locking,
            final String advertiseURI,
            final Layout<? extends Serializable> layout,
            final int bufferSize,
            final boolean writeHeader) {
        this(fileName, os, append, locking, advertiseURI, layout, writeHeader, ByteBuffer.wrap(new byte[bufferSize]));
    }

    /**
     * @deprecated
     * @since 2.6
     */
    @Deprecated
    protected FileManager(
            final String fileName,
            final OutputStream os,
            final boolean append,
            final boolean locking,
            final String advertiseURI,
            final Layout<? extends Serializable> layout,
            final boolean writeHeader,
            final ByteBuffer buffer) {
        super(os, fileName, layout, writeHeader, buffer);
        this.isAppend = append;
        this.createOnDemand = false;
        this.isLocking = locking;
        this.advertiseURI = advertiseURI;
        this.bufferSize = buffer.capacity();
        this.filePermissions = null;
        this.fileOwner = null;
        this.fileGroup = null;
        this.attributeViewEnabled = false;
    }

    /**
     * @deprecated
     * @since 2.7
     */
    @Deprecated
    protected FileManager(
            final LoggerContext loggerContext,
            final String fileName,
            final OutputStream os,
            final boolean append,
            final boolean locking,
            final boolean createOnDemand,
            final String advertiseURI,
            final Layout<? extends Serializable> layout,
            final boolean writeHeader,
            final ByteBuffer buffer) {
        super(loggerContext, os, fileName, createOnDemand, layout, writeHeader, buffer);
        this.isAppend = append;
        this.createOnDemand = createOnDemand;
        this.isLocking = locking;
        this.advertiseURI = advertiseURI;
        this.bufferSize = buffer.capacity();
        this.filePermissions = null;
        this.fileOwner = null;
        this.fileGroup = null;
        this.attributeViewEnabled = false;
    }

    /**
     * @since 2.9
     */
    @SuppressFBWarnings(
            value = "OVERLY_PERMISSIVE_FILE_PERMISSION",
            justification = "File permissions are specified in the configuration file.")
    protected FileManager(
            final LoggerContext loggerContext,
            final String fileName,
            final OutputStream os,
            final boolean append,
            final boolean locking,
            final boolean createOnDemand,
            final String advertiseURI,
            final Layout<? extends Serializable> layout,
            final String filePermissions,
            final String fileOwner,
            final String fileGroup,
            final boolean writeHeader,
            final ByteBuffer buffer) {
        super(loggerContext, os, fileName, createOnDemand, layout, writeHeader, buffer);
        this.isAppend = append;
        this.createOnDemand = createOnDemand;
        this.isLocking = locking;
        this.advertiseURI = advertiseURI;
        this.bufferSize = buffer.capacity();

        final Set<String> views = FileSystems.getDefault().supportedFileAttributeViews();
        if (views.contains("posix")) {
            this.filePermissions = filePermissions != null ? PosixFilePermissions.fromString(filePermissions) : null;
            this.fileGroup = fileGroup;
        } else {
            this.filePermissions = null;
            this.fileGroup = null;
            if (filePermissions != null) {
                LOGGER.warn("Posix file attribute permissions defined but it is not supported by this files system.");
            }
            if (fileGroup != null) {
                LOGGER.warn("Posix file attribute group defined but it is not supported by this files system.");
            }
        }

        if (views.contains("owner")) {
            this.fileOwner = fileOwner;
        } else {
            this.fileOwner = null;
            if (fileOwner != null) {
                LOGGER.warn("Owner file attribute defined but it is not supported by this files system.");
            }
        }

        // Supported and defined
        this.attributeViewEnabled = this.filePermissions != null || this.fileOwner != null || this.fileGroup != null;
    }

    /**
     * Returns the FileManager.
     * @param fileName The name of the file to manage.
     * @param append true if the file should be appended to, false if it should be overwritten.
     * @param locking true if the file should be locked while writing, false otherwise.
     * @param bufferedIo true if the contents should be buffered as they are written.
     * @param createOnDemand true if you want to lazy-create the file (a.k.a. on-demand.)
     * @param advertiseUri the URI to use when advertising the file
     * @param layout The layout
     * @param bufferSize buffer size for buffered IO
     * @param filePermissions File permissions
     * @param fileOwner File owner
     * @param fileGroup File group
     * @param configuration The configuration.
     * @return A FileManager for the File.
     */
    public static FileManager getFileManager(
            final String fileName,
            final boolean append,
            boolean locking,
            final boolean bufferedIo,
            final boolean createOnDemand,
            final String advertiseUri,
            final Layout<? extends Serializable> layout,
            final int bufferSize,
            final String filePermissions,
            final String fileOwner,
            final String fileGroup,
            final Configuration configuration) {

        if (locking && bufferedIo) {
            locking = false;
        }
        return narrow(
                FileManager.class,
                getManager(
                        fileName,
                        new FactoryData(
                                append,
                                locking,
                                bufferedIo,
                                bufferSize,
                                createOnDemand,
                                advertiseUri,
                                layout,
                                filePermissions,
                                fileOwner,
                                fileGroup,
                                configuration),
                        FACTORY));
    }

    @Override
    @SuppressFBWarnings(
            value = "PATH_TRAVERSAL_IN",
            justification = "The destination file is specified in the configuration file.")
    protected OutputStream createOutputStream() throws IOException {
        final String filename = getFileName();
        LOGGER.debug("Now writing to {} at {}", filename, new Date());
        final File file = new File(filename);
        createParentDir(file);
        final FileOutputStream fos = new FileOutputStream(file, isAppend);
        if (file.exists() && file.length() == 0) {
            try {
                final FileTime now = FileTime.fromMillis(System.currentTimeMillis());
                Files.setAttribute(file.toPath(), "creationTime", now);
            } catch (Exception ex) {
                LOGGER.warn("Unable to set current file time for {}", filename);
            }
            writeHeader(fos);
        }
        defineAttributeView(Paths.get(filename));
        return fos;
    }

    protected void createParentDir(final File file) {}

    protected void defineAttributeView(final Path path) {
        if (attributeViewEnabled) {
            try {
                // FileOutputStream may not create new file on all JVM
                path.toFile().createNewFile();

                FileUtils.defineFilePosixAttributeView(path, filePermissions, fileOwner, fileGroup);
            } catch (final Exception e) {
                LOGGER.error("Could not define attribute view on path \"{}\" got {}", path, e.getMessage(), e);
            }
        }
    }

    @Override
    protected synchronized void write(
            final byte[] bytes, final int offset, final int length, final boolean immediateFlush) {
        if (isLocking) {
            try {
                @SuppressWarnings("resource")
                final FileChannel channel = ((FileOutputStream) getOutputStream()).getChannel();
                /*
                 * Lock the whole file. This could be optimized to only lock from the current file position. Note that
                 * locking may be advisory on some systems and mandatory on others, so locking just from the current
                 * position would allow reading on systems where locking is mandatory. Also, Java 6 will throw an
                 * exception if the region of the file is already locked by another FileChannel in the same JVM.
                 * Hopefully, that will be avoided since every file should have a single file manager - unless two
                 * different files strings are configured that somehow map to the same file.
                 */
                try (final FileLock lock = channel.lock(0, Long.MAX_VALUE, false)) {
                    super.write(bytes, offset, length, immediateFlush);
                }
            } catch (final IOException ex) {
                throw new AppenderLoggingException("Unable to obtain lock on " + getName(), ex);
            }
        } else {
            super.write(bytes, offset, length, immediateFlush);
        }
    }

    /**
     * Overrides {@link OutputStreamManager#writeToDestination(byte[], int, int)} to add support for file locking.
     *
     * @param bytes the array containing data
     * @param offset from where to write
     * @param length how many bytes to write
     * @since 2.8
     */
    @Override
    protected synchronized void writeToDestination(final byte[] bytes, final int offset, final int length) {
        if (isLocking) {
            try {
                @SuppressWarnings("resource")
                final FileChannel channel = ((FileOutputStream) getOutputStream()).getChannel();
                /*
                 * Lock the whole file. This could be optimized to only lock from the current file position. Note that
                 * locking may be advisory on some systems and mandatory on others, so locking just from the current
                 * position would allow reading on systems where locking is mandatory. Also, Java 6 will throw an
                 * exception if the region of the file is already locked by another FileChannel in the same JVM.
                 * Hopefully, that will be avoided since every file should have a single file manager - unless two
                 * different files strings are configured that somehow map to the same file.
                 */
                try (final FileLock lock = channel.lock(0, Long.MAX_VALUE, false)) {
                    super.writeToDestination(bytes, offset, length);
                }
            } catch (final IOException ex) {
                throw new AppenderLoggingException("Unable to obtain lock on " + getName(), ex);
            }
        } else {
            super.writeToDestination(bytes, offset, length);
        }
    }

    /**
     * Returns the name of the File being managed.
     * @return The name of the File being managed.
     */
    public String getFileName() {
        return getName();
    }
    /**
     * Returns the append status.
     * @return true if the file will be appended to, false if it is overwritten.
     */
    public boolean isAppend() {
        return isAppend;
    }

    /**
     * Returns the lazy-create.
     * @return true if the file will be lazy-created.
     */
    public boolean isCreateOnDemand() {
        return createOnDemand;
    }

    /**
     * Returns the lock status.
     * @return true if the file will be locked when writing, false otherwise.
     */
    public boolean isLocking() {
        return isLocking;
    }

    /**
     * Returns the buffer size to use if the appender was configured with BufferedIO=true, otherwise returns a negative
     * number.
     * @return the buffer size, or a negative number if the output stream is not buffered
     */
    public int getBufferSize() {
        return bufferSize;
    }

    /**
     * Returns POSIX file permissions if defined and the OS supports POSIX file attribute,
     * null otherwise.
     * @return File POSIX permissions
     * @see PosixFileAttributeView
     */
    public Set<PosixFilePermission> getFilePermissions() {
        return filePermissions;
    }

    /**
     * Returns file owner if defined and the OS supports owner file attribute view,
     * null otherwise.
     * @return File owner
     * @see FileOwnerAttributeView
     */
    public String getFileOwner() {
        return fileOwner;
    }

    /**
     * Returns file group if defined and the OS supports POSIX/group file attribute view,
     * null otherwise.
     * @return File group
     * @see PosixFileAttributeView
     */
    public String getFileGroup() {
        return fileGroup;
    }

    /**
     * Returns true if file attribute view enabled for this file manager.
     *
     * @return True if POSIX or owner supported and defined false otherwise.
     */
    public boolean isAttributeViewEnabled() {
        return attributeViewEnabled;
    }

    /**
     * FileManager's content format is specified by: <code>Key: "fileURI" Value: provided "advertiseURI" param</code>.
     *
     * @return Map of content format keys supporting FileManager
     */
    @Override
    public Map<String, String> getContentFormat() {
        final Map<String, String> result = new HashMap<>(super.getContentFormat());
        result.put("fileURI", advertiseURI);
        return result;
    }

    /**
     * Factory Data.
     */
    private static class FactoryData extends ConfigurationFactoryData {
        private final boolean append;
        private final boolean locking;
        private final boolean bufferedIo;
        private final int bufferSize;
        private final boolean createOnDemand;
        private final String advertiseURI;
        private final Layout<? extends Serializable> layout;
        private final String filePermissions;
        private final String fileOwner;
        private final String fileGroup;

        /**
         * Constructor.
         * @param append Append status.
         * @param locking Locking status.
         * @param bufferedIo Buffering flag.
         * @param bufferSize Buffer size.
         * @param createOnDemand if you want to lazy-create the file (a.k.a. on-demand.)
         * @param advertiseURI the URI to use when advertising the file
         * @param layout The layout
         * @param filePermissions File permissions
         * @param fileOwner File owner
         * @param fileGroup File group
         * @param configuration the configuration
         */
        public FactoryData(
                final boolean append,
                final boolean locking,
                final boolean bufferedIo,
                final int bufferSize,
                final boolean createOnDemand,
                final String advertiseURI,
                final Layout<? extends Serializable> layout,
                final String filePermissions,
                final String fileOwner,
                final String fileGroup,
                final Configuration configuration) {
            super(configuration);
            this.append = append;
            this.locking = locking;
            this.bufferedIo = bufferedIo;
            this.bufferSize = bufferSize;
            this.createOnDemand = createOnDemand;
            this.advertiseURI = advertiseURI;
            this.layout = layout;
            this.filePermissions = filePermissions;
            this.fileOwner = fileOwner;
            this.fileGroup = fileGroup;
        }
    }

    /**
     * Factory to create a FileManager.
     */
    private static class FileManagerFactory implements ManagerFactory<FileManager, FactoryData> {

        /**
         * Creates a FileManager.
         * @param name The name of the File.
         * @param data The FactoryData
         * @return The FileManager for the File.
         */
        @Override
        @SuppressFBWarnings(
                value = "PATH_TRAVERSAL_IN",
                justification = "The destination file should be specified in the configuration file.")
        public FileManager createManager(final String name, final FactoryData data) {
            Objects.requireNonNull(name, "filename is missing");
            final File file = new File(name);
            try {
                FileUtils.makeParentDirs(file);
                final boolean writeHeader = !data.append || !file.exists();
                final int actualSize = data.bufferedIo ? data.bufferSize : Constants.ENCODER_BYTE_BUFFER_SIZE;
                final ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[actualSize]);
                final FileOutputStream fos = data.createOnDemand ? null : new FileOutputStream(file, data.append);
                final FileManager fm = new FileManager(
                        data.getLoggerContext(),
                        name,
                        fos,
                        data.append,
                        data.locking,
                        data.createOnDemand,
                        data.advertiseURI,
                        data.layout,
                        data.filePermissions,
                        data.fileOwner,
                        data.fileGroup,
                        writeHeader,
                        byteBuffer);
                if (fos != null && fm.attributeViewEnabled) {
                    fm.defineAttributeView(file.toPath());
                }
                return fm;
            } catch (final IOException ex) {
                LOGGER.error("FileManager (" + name + ") " + ex, ex);
            }
            return null;
        }
    }
}
