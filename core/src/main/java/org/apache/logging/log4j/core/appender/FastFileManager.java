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

import org.apache.logging.log4j.core.Layout;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Extends OutputStreamManager but instead of using a buffered output stream,
 * this class uses a {@code ByteBuffer} and a {@code RandomAccessFile} to do the
 * I/O.
 */
public class FastFileManager extends OutputStreamManager {
    private static final int DEFAULT_BUFFER_SIZE = 256 * 1024;

    private static final FastFileManagerFactory FACTORY = new FastFileManagerFactory();

    private final boolean isImmediateFlush;
    private final String advertiseURI;
    private final RandomAccessFile randomAccessFile;
    private final ByteBuffer buffer;
    private ThreadLocal<Boolean> isEndOfBatch = new ThreadLocal<Boolean>();

    protected FastFileManager(final RandomAccessFile file, final String fileName,
            final OutputStream os, final boolean immediateFlush, final String advertiseURI,
            final Layout layout) {
        super(os, fileName, layout);
        this.isImmediateFlush = immediateFlush;
        this.randomAccessFile = file;
        this.advertiseURI = advertiseURI;
        isEndOfBatch.set(Boolean.FALSE);

        // TODO make buffer size configurable?
        buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
    }

    /**
     * Returns the FastFileManager.
     *
     * @param fileName The name of the file to manage.
     * @param append true if the file should be appended to, false if it should be overwritten.
     * @param isFlush true if the contents should be flushed to disk on every write
     * @param advertiseURI the URI to use when advertising the file
     * @param layout The layout.
     * @return A FastFileManager for the File.
     */
    public static FastFileManager getFileManager(final String fileName, final boolean append,
                                                 final boolean isFlush, final String advertiseURI,
                                                 final Layout layout) {
        return (FastFileManager) getManager(fileName, new FactoryData(append, isFlush, advertiseURI, layout), FACTORY);
    }

    public Boolean isEndOfBatch() {
        return isEndOfBatch.get();
    }

    public void setEndOfBatch(boolean isEndOfBatch) {
        this.isEndOfBatch.set(Boolean.valueOf(isEndOfBatch));
    }

    @Override
    protected synchronized void write(byte[] bytes, int offset, int length) {
        super.write(bytes, offset, length); // writes to dummy output stream

        if (length > buffer.remaining()) {
            flush();
        }
        buffer.put(bytes, offset, length);
        if (isImmediateFlush || isEndOfBatch.get() == Boolean.TRUE) {
            flush();
        }
    }

    @Override
    public void flush() {
        buffer.flip();
        try {
            randomAccessFile.write(buffer.array(), 0, buffer.limit());
        } catch (IOException ex) {
            String msg = "Error writing to RandomAccessFile " + getName();
            throw new AppenderRuntimeException(msg, ex);
        }
        buffer.clear();
    }

    @Override
    public void close() {
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

    /** {@code OutputStream} subclass that does not write anything. */
    private static class DummyOutputStream extends OutputStream {
        @Override
        public void write(int b) throws IOException {
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
        }
    }

    /**
     * FileManager's content format is specified by:
     * <p/>
     * Key: "fileURI" Value: provided "advertiseURI" param.
     *
     * @return Map of content format keys supporting FileManager
     */
    @Override
    public Map<String, String> getContentFormat() {
        Map<String, String> result = new HashMap<String, String>(
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
        private final String advertiseURI;
        private final Layout layout;

        /**
         * Constructor.
         *
         * @param append Append status.
         */
        public FactoryData(final boolean append, final boolean immediateFlush, final String advertiseURI,
                           final Layout layout) {
            this.append = append;
            this.immediateFlush = immediateFlush;
            this.advertiseURI = advertiseURI;
            this.layout = layout;
        }
    }

    /**
     * Factory to create a FastFileManager.
     */
    private static class FastFileManagerFactory implements ManagerFactory<FastFileManager, FactoryData> {

        /**
         * Create a FastFileManager.
         *
         * @param name The name of the File.
         * @param data The FactoryData
         * @return The FastFileManager for the File.
         */
        @Override
        public FastFileManager createManager(String name, FactoryData data) {
            File file = new File(name);
            final File parent = file.getParentFile();
            if (null != parent && !parent.exists()) {
                parent.mkdirs();
            }
            if (!data.append) {
                file.delete();
            }

            OutputStream os = new DummyOutputStream();
            RandomAccessFile raf;
            try {
                raf = new RandomAccessFile(name, "rw");
                return new FastFileManager(raf, name, os, data.immediateFlush, data.advertiseURI, data.layout);
            } catch (Exception ex) {
                LOGGER.error("FastFileManager (" + name + ") " + ex);
            }
            return null;
        }
    }

}
