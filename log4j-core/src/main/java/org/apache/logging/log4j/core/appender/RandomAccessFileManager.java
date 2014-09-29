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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.core.Layout;

/**
 * Extends OutputStreamManager but instead of using a buffered output stream,
 * this class uses a {@code ByteBuffer} and a {@code RandomAccessFile} to do the
 * I/O.
 */
public class RandomAccessFileManager extends OutputStreamManager {
    static final int DEFAULT_BUFFER_SIZE = 256 * 1024;

    private static final RandomAccessFileManagerFactory FACTORY = new RandomAccessFileManagerFactory();

    private final boolean isImmediateFlush;
    private final String advertiseURI;
    private final RandomAccessFile randomAccessFile;
    private final ByteBuffer buffer;
    private final ThreadLocal<Boolean> isEndOfBatch = new ThreadLocal<Boolean>();

    protected RandomAccessFileManager(final RandomAccessFile file,
            final String fileName, final OutputStream os,
            final boolean immediateFlush, final int bufferSize,
            final String advertiseURI, final Layout<? extends Serializable> layout) {
        super(os, fileName, layout);
        this.isImmediateFlush = immediateFlush;
        this.randomAccessFile = file;
        this.advertiseURI = advertiseURI;
        this.isEndOfBatch.set(Boolean.FALSE);
        this.buffer = ByteBuffer.allocate(bufferSize);
    }

    /**
     * Returns the RandomAccessFileManager.
     *
     * @param fileName The name of the file to manage.
     * @param append true if the file should be appended to, false if it should
     *            be overwritten.
     * @param isFlush true if the contents should be flushed to disk on every
     *            write
     * @param bufferSize The buffer size.
     * @param advertiseURI the URI to use when advertising the file
     * @param layout The layout.
     * @return A RandomAccessFileManager for the File.
     */
    public static RandomAccessFileManager getFileManager(final String fileName, final boolean append,
            final boolean isFlush, final int bufferSize, final String advertiseURI,
            final Layout<? extends Serializable> layout) {
        return (RandomAccessFileManager) getManager(fileName, new FactoryData(append,
                isFlush, bufferSize, advertiseURI, layout), FACTORY);
    }

    public Boolean isEndOfBatch() {
        return isEndOfBatch.get();
    }

    public void setEndOfBatch(final boolean isEndOfBatch) {
        this.isEndOfBatch.set(Boolean.valueOf(isEndOfBatch));
    }

    @Override
    protected synchronized void write(final byte[] bytes, int offset, int length) {
        super.write(bytes, offset, length); // writes to dummy output stream

        int chunk = 0;
        do {
            if (length > buffer.remaining()) {
                flush();
            }
            chunk = Math.min(length, buffer.remaining());
            buffer.put(bytes, offset, chunk);
            offset += chunk;
            length -= chunk;
        } while (length > 0);

        if (isImmediateFlush || isEndOfBatch.get() == Boolean.TRUE) {
            flush();
        }
    }

    @Override
    public synchronized void flush() {
        buffer.flip();
        try {
            randomAccessFile.write(buffer.array(), 0, buffer.limit());
        } catch (final IOException ex) {
            final String msg = "Error writing to RandomAccessFile " + getName();
            throw new AppenderLoggingException(msg, ex);
        }
        buffer.clear();
    }

    @Override
    public synchronized void close() {
        flush();
        try {
            randomAccessFile.close();
        } catch (final IOException ex) {
            LOGGER.error("Unable to close RandomAccessFile " + getName() + ". "
                    + ex);
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
        return buffer.capacity();
    }

    /** {@code OutputStream} subclass that does not write anything. */
    static class DummyOutputStream extends OutputStream {
        @Override
        public void write(final int b) throws IOException {
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
        }
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
        final Map<String, String> result = new HashMap<String, String>(
                super.getContentFormat());
        result.put("fileURI", advertiseURI);
        return result;
    }

    /**
     * Factory Data.
     */
    private static class FactoryData {
        private final boolean append;
        private final boolean immediateFlush;
        private final int bufferSize;
        private final String advertiseURI;
        private final Layout<? extends Serializable> layout;

        /**
         * Constructor.
         *
         * @param append Append status.
         * @param bufferSize TODO
         */
        public FactoryData(final boolean append, final boolean immediateFlush,
                final int bufferSize, final String advertiseURI, final Layout<? extends Serializable> layout) {
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
    private static class RandomAccessFileManagerFactory implements
            ManagerFactory<RandomAccessFileManager, FactoryData> {

        /**
         * Create a RandomAccessFileManager.
         *
         * @param name The name of the File.
         * @param data The FactoryData
         * @return The RandomAccessFileManager for the File.
         */
        @Override
        public RandomAccessFileManager createManager(final String name, final FactoryData data) {
            final File file = new File(name);
            final File parent = file.getParentFile();
            if (null != parent && !parent.exists()) {
                parent.mkdirs();
            }
            if (!data.append) {
                file.delete();
            }

            final OutputStream os = new DummyOutputStream();
            RandomAccessFile raf;
            try {
                raf = new RandomAccessFile(name, "rw");
                if (data.append) {
                    raf.seek(raf.length());
                } else {
                    raf.setLength(0);
                }
                return new RandomAccessFileManager(raf, name, os, data.immediateFlush,
                        data.bufferSize, data.advertiseURI, data.layout);
            } catch (final Exception ex) {
                LOGGER.error("RandomAccessFileManager (" + name + ") " + ex);
            }
            return null;
        }
    }

}
