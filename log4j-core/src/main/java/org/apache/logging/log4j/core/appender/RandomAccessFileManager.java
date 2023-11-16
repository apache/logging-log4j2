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
import org.apache.logging.log4j.core.util.FileUtils;
import org.apache.logging.log4j.core.util.NullOutputStream;

/**
 * Extends OutputStreamManager but instead of using a buffered output stream,
 * this class uses a {@code ByteBuffer} and a {@code RandomAccessFile} to do the
 * I/O.
 */
public class RandomAccessFileManager extends OutputStreamManager {
    static final int DEFAULT_BUFFER_SIZE = 256 * 1024;

    private static final RandomAccessFileManagerFactory FACTORY = new RandomAccessFileManagerFactory();

    private final String advertiseURI;
    private final RandomAccessFile randomAccessFile;

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
        return narrow(
                RandomAccessFileManager.class,
                getManager(
                        fileName,
                        new FactoryData(append, immediateFlush, bufferSize, advertiseURI, layout, configuration),
                        FACTORY));
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
            randomAccessFile.write(bytes, offset, length);
        } catch (final IOException ex) {
            final String msg = "Error writing to RandomAccessFile " + getName();
            throw new AppenderLoggingException(msg, ex);
        }
    }

    @Override
    public synchronized void flush() {
        flushBuffer(byteBuffer);
    }

    @Override
    public synchronized boolean closeOutputStream() {
        flush();
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

    /**
     * Factory Data.
     */
    private static class FactoryData extends ConfigurationFactoryData {
        private final boolean append;
        private final boolean immediateFlush;
        private final int bufferSize;
        private final String advertiseURI;
        private final Layout<? extends Serializable> layout;

        /**
         * Constructor.
         *
         * @param append Append status.
         * @param bufferSize size of the buffer
         * @param configuration The configuration.
         */
        public FactoryData(
                final boolean append,
                final boolean immediateFlush,
                final int bufferSize,
                final String advertiseURI,
                final Layout<? extends Serializable> layout,
                final Configuration configuration) {
            super(configuration);
            this.append = append;
            this.immediateFlush = immediateFlush;
            this.bufferSize = bufferSize;
            this.advertiseURI = advertiseURI;
            this.layout = layout;
        }
    }

    /**
     * Factory to create a RandomAccessFileManager.
     */
    private static class RandomAccessFileManagerFactory
            implements ManagerFactory<RandomAccessFileManager, FactoryData> {

        /**
         * Create a RandomAccessFileManager.
         *
         * @param name The name of the File.
         * @param data The FactoryData
         * @return The RandomAccessFileManager for the File.
         */
        @Override
        @SuppressFBWarnings(
                value = "PATH_TRAVERSAL_IN",
                justification = "The destination file should be specified in the configuration file.")
        public RandomAccessFileManager createManager(final String name, final FactoryData data) {
            final File file = new File(name);
            if (!data.append) {
                file.delete();
            }

            final boolean writeHeader = !data.append || !file.exists();
            final OutputStream os = NullOutputStream.getInstance();
            RandomAccessFile raf;
            try {
                FileUtils.makeParentDirs(file);
                raf = new RandomAccessFile(name, "rw");
                if (data.append) {
                    raf.seek(raf.length());
                } else {
                    raf.setLength(0);
                }
                return new RandomAccessFileManager(
                        data.getLoggerContext(),
                        raf,
                        name,
                        os,
                        data.bufferSize,
                        data.advertiseURI,
                        data.layout,
                        writeHeader);
            } catch (final Exception ex) {
                LOGGER.error("RandomAccessFileManager (" + name + ") " + ex, ex);
            }
            return null;
        }
    }
}
