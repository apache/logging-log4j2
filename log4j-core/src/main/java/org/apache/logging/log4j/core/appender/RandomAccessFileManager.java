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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.internal.annotation.SuppressFBWarnings;
import org.apache.logging.log4j.core.util.Closer;
import org.apache.logging.log4j.core.util.FileUtils;
import org.apache.logging.log4j.core.util.NullOutputStream;

/**
 * Extends OutputStreamManager but instead of using a buffered output stream,
 * this class uses a {@code ByteBuffer} and a {@code RandomAccessFile} to do the
 * I/O.
 */
public class RandomAccessFileManager extends OutputStreamManager {
    static final int DEFAULT_BUFFER_SIZE = 256 * 1024;

    private final String advertiseURI;

    /**
     * The file, or {@code null} if it is not open yet.
     */
    private RandomAccessFile randomAccessFile;

    /**
     * {@code true}, if the previous content of the file must be discarded when {@link #openFile()} opens it.
     */
    private boolean truncateOnOpen;

    protected RandomAccessFileManager(
            final LoggerContext loggerContext,
            final RandomAccessFile file,
            final String fileName,
            final OutputStream os,
            final int bufferSize,
            final String advertiseURI,
            final Layout<? extends Serializable> layout,
            final boolean writeHeader) {
        super(loggerContext, os, fileName, false, layout, writeHeader, ByteBuffer.wrap(new byte[bufferSize]));
        this.randomAccessFile = file;
        this.advertiseURI = advertiseURI;
    }

    /**
     * Returns the RandomAccessFileManager.
     *
     * @param fileName The name of the file to manage.
     * @param append true if the file should be appended to, false if it should
     *            be overwritten.
     * @param immediateFlush true if the contents should be flushed to disk on every
     *            write
     * @param bufferSize The buffer size.
     * @param advertiseURI the URI to use when advertising the file
     * @param layout The layout.
     * @param configuration The configuration.
     * @return A RandomAccessFileManager for the File.
     */
    public static RandomAccessFileManager getFileManager(
            final String fileName,
            final boolean append,
            final boolean immediateFlush,
            final int bufferSize,
            final String advertiseURI,
            final Layout<? extends Serializable> layout,
            final Configuration configuration) {
        return getFileManager(fileName, append, false, immediateFlush, bufferSize, advertiseURI, layout, configuration);
    }

    /**
     * Returns the RandomAccessFileManager.
     *
     * @param fileName The name of the file to manage.
     * @param append true if the file should be appended to, false if it should
     *            be overwritten.
     * @param createOnDemand true if the file and its parent directories should only be created on the first write
     * @param immediateFlush true if the contents should be flushed to disk on every
     *            write
     * @param bufferSize The buffer size.
     * @param advertiseURI the URI to use when advertising the file
     * @param layout The layout.
     * @param configuration The configuration.
     * @return A RandomAccessFileManager for the File.
     * @since 2.27.0
     */
    public static RandomAccessFileManager getFileManager(
            final String fileName,
            final boolean append,
            final boolean createOnDemand,
            final boolean immediateFlush,
            final int bufferSize,
            final String advertiseURI,
            final Layout<? extends Serializable> layout,
            final Configuration configuration) {
        return narrow(
                RandomAccessFileManager.class,
                getManager(
                        fileName,
                        (name, data) -> {
                            final File file = new File(name);
                            // With `createOnDemand` the file is left untouched until the first write.
                            if (!append && !createOnDemand) {
                                file.delete();
                            }

                            final boolean writeHeader = !append || !file.exists();
                            final OutputStream os = NullOutputStream.getInstance();
                            RandomAccessFile raf = null;
                            try {
                                if (!createOnDemand) {
                                    FileUtils.makeParentDirs(file);
                                    raf = new RandomAccessFile(name, "rw");
                                    if (append) {
                                        raf.seek(raf.length());
                                    } else {
                                        raf.setLength(0);
                                    }
                                }
                                final RandomAccessFileManager manager = new RandomAccessFileManager(
                                        data.getLoggerContext(),
                                        raf,
                                        name,
                                        os,
                                        bufferSize,
                                        advertiseURI,
                                        layout,
                                        writeHeader);
                                // A `null` file is not open yet: `writeToDestination()` opens it on the first write.
                                manager.truncateOnOpen = !append;
                                return manager;
                            } catch (final Exception ex) {
                                LOGGER.error("RandomAccessFileManager (" + name + ") " + ex, ex);
                            }
                            return null;
                        },
                        new ConfigurationFactoryData(configuration)));
    }

    /**
     * No longer used, the {@link org.apache.logging.log4j.core.LogEvent#isEndOfBatch()} attribute is used instead.
     * @return {@link Boolean#FALSE}.
     * @deprecated end-of-batch on the event is used instead.
     */
    @Deprecated
    public Boolean isEndOfBatch() {
        return Boolean.FALSE;
    }

    /**
     * No longer used, the {@link org.apache.logging.log4j.core.LogEvent#isEndOfBatch()} attribute is used instead.
     * This method is a no-op.
     * @deprecated end-of-batch on the event is used instead.
     */
    @Deprecated
    public void setEndOfBatch(@SuppressWarnings("unused") final boolean endOfBatch) {}

    @Override
    protected void writeToDestination(final byte[] bytes, final int offset, final int length) {
        try {
            if (randomAccessFile == null) {
                openFile();
            }
            randomAccessFile.write(bytes, offset, length);
        } catch (final IOException ex) {
            final String msg = "Error writing to RandomAccessFile " + getName();
            throw new AppenderLoggingException(msg, ex);
        }
    }

    @SuppressFBWarnings(
            value = "PATH_TRAVERSAL_IN",
            justification = "The name of the accessed file is based on a configuration value.")
    private void openFile() throws IOException {
        final File file = new File(getFileName());
        FileUtils.makeParentDirs(file);
        final RandomAccessFile raf = new RandomAccessFile(file, "rw");
        try {
            if (truncateOnOpen) {
                raf.setLength(0);
            } else {
                raf.seek(raf.length());
            }
        } catch (final IOException e) {
            Closer.closeSilently(raf);
            throw e;
        }
        randomAccessFile = raf;
    }

    @Override
    public synchronized void flush() {
        flushBuffer(byteBuffer);
    }

    @Override
    public synchronized boolean closeOutputStream() {
        flush();
        if (randomAccessFile == null) {
            // the file was never opened
            return true;
        }
        try {
            randomAccessFile.close();
            return true;
        } catch (final IOException ex) {
            logError("Unable to close RandomAccessFile", ex);
            return false;
        }
    }

    /**
     * Returns the name of the File being managed.
     *
     * @return The name of the File being managed.
     */
    public String getFileName() {
        return getName();
    }

    /**
     * Returns the buffer capacity.
     * @return the buffer size
     */
    public int getBufferSize() {
        return byteBuffer.capacity();
    }

    /**
     * Gets this FileManager's content format specified by:
     * <p>
     * Key: "fileURI" Value: provided "advertiseURI" param.
     * </p>
     *
     * @return Map of content format keys supporting FileManager
     */
    @Override
    public Map<String, String> getContentFormat() {
        final Map<String, String> result = new HashMap<>(super.getContentFormat());
        result.put("fileURI", advertiseURI);
        return result;
    }
}
