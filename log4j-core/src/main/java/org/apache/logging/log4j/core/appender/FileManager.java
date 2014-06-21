/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.core.appender;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.core.Layout;


/**
 * Manages actual File I/O for File Appenders.
 */
public class FileManager extends OutputStreamManager {

    private static final FileManagerFactory FACTORY = new FileManagerFactory();

    private final boolean isAppend;
    private final boolean isLocking;
    private final String advertiseURI;
    private final int bufferSize;

    protected FileManager(final String fileName, final OutputStream os, final boolean append, final boolean locking,
            final String advertiseURI, final Layout<? extends Serializable> layout, final int bufferSize) {
        super(os, fileName, layout);
        this.isAppend = append;
        this.isLocking = locking;
        this.advertiseURI = advertiseURI;
        this.bufferSize = bufferSize;
    }

    /**
     * Returns the FileManager.
     * @param fileName The name of the file to manage.
     * @param append true if the file should be appended to, false if it should be overwritten.
     * @param locking true if the file should be locked while writing, false otherwise.
     * @param bufferedIo true if the contents should be buffered as they are written.
     * @param advertiseUri the URI to use when advertising the file
     * @param layout The layout
     * @param bufferSize buffer size for buffered IO
     * @return A FileManager for the File.
     */
    public static FileManager getFileManager(final String fileName, final boolean append, boolean locking,
            final boolean bufferedIo, final String advertiseUri, final Layout<? extends Serializable> layout,
            final int bufferSize) {

        if (locking && bufferedIo) {
            locking = false;
        }
        return (FileManager) getManager(fileName, new FactoryData(append, locking, bufferedIo, bufferSize,
                advertiseUri, layout), FACTORY);
    }

    @Override
    protected synchronized void write(final byte[] bytes, final int offset, final int length)  {

        if (isLocking) {
            final FileChannel channel = ((FileOutputStream) getOutputStream()).getChannel();
            try {
                /* Lock the whole file. This could be optimized to only lock from the current file
                   position. Note that locking may be advisory on some systems and mandatory on others,
                   so locking just from the current position would allow reading on systems where
                   locking is mandatory.  Also, Java 6 will throw an exception if the region of the
                   file is already locked by another FileChannel in the same JVM. Hopefully, that will
                   be avoided since every file should have a single file manager - unless two different
                   files strings are configured that somehow map to the same file.*/
                final FileLock lock = channel.lock(0, Long.MAX_VALUE, false);
                try {
                    super.write(bytes, offset, length);
                } finally {
                    lock.release();
                }
            } catch (final IOException ex) {
                throw new AppenderLoggingException("Unable to obtain lock on " + getName(), ex);
            }

        } else {
            super.write(bytes, offset, length);
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
     * FileManager's content format is specified by: <code>Key: "fileURI" Value: provided "advertiseURI" param</code>.
     *
     * @return Map of content format keys supporting FileManager
     */
    @Override
    public Map<String, String> getContentFormat() {
        final Map<String, String> result = new HashMap<String, String>(super.getContentFormat());
        result.put("fileURI", advertiseURI);
        return result;
    }

    /**
     * Factory Data.
     */
    private static class FactoryData {
        private final boolean append;
        private final boolean locking;
        private final boolean bufferedIO;
        private final int bufferSize;
        private final String advertiseURI;
        private final Layout<? extends Serializable> layout;

        /**
         * Constructor.
         * @param append Append status.
         * @param locking Locking status.
         * @param bufferedIO Buffering flag.
         * @param bufferSize Buffer size.
         * @param advertiseURI the URI to use when advertising the file
         */
        public FactoryData(final boolean append, final boolean locking, final boolean bufferedIO, final int bufferSize,
                final String advertiseURI, final Layout<? extends Serializable> layout) {
            this.append = append;
            this.locking = locking;
            this.bufferedIO = bufferedIO;
            this.bufferSize = bufferSize;
            this.advertiseURI = advertiseURI;
            this.layout = layout;
        }
    }

    /**
     * Factory to create a FileManager.
     */
    private static class FileManagerFactory implements ManagerFactory<FileManager, FactoryData> {

        /**
         * Create a FileManager.
         * @param name The name of the File.
         * @param data The FactoryData
         * @return The FileManager for the File.
         */
        @Override
        public FileManager createManager(final String name, final FactoryData data) {
            final File file = new File(name);
            final File parent = file.getParentFile();
            if (null != parent && !parent.exists()) {
                parent.mkdirs();
            }

            OutputStream os;
            try {
                os = new FileOutputStream(name, data.append);
                int bufferSize = data.bufferSize;
                if (data.bufferedIO) {
                    os = new BufferedOutputStream(os, bufferSize);
                } else {
                    bufferSize = -1; // signals to RollingFileManager not to use BufferedOutputStream
                }
                return new FileManager(name, os, data.append, data.locking, data.advertiseURI, data.layout, bufferSize);
            } catch (final FileNotFoundException ex) {
                LOGGER.error("FileManager (" + name + ") " + ex);
            }
            return null;
        }
    }

}
