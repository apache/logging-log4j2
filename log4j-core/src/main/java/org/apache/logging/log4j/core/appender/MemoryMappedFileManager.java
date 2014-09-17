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
import java.lang.reflect.Method;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.util.Assert;
import org.apache.logging.log4j.core.util.Closer;

/**
 * Extends OutputStreamManager but instead of using a buffered output stream, this class maps a region of a file into
 * memory and writes to this memory region.
 * <p>
 * 
 * @see <a href="http://www.codeproject.com/Tips/683614/Things-to-Know-about-Memory-Mapped-File-in-Java">http://www.codeproject.com/Tips/683614/Things-to-Know-about-Memory-Mapped-File-in-Java</a>
 * @see <a href="http://bugs.java.com/view_bug.do?bug_id=6893654">http://bugs.java.com/view_bug.do?bug_id=6893654</a>
 * @see <a href="http://bugs.java.com/view_bug.do?bug_id=4724038">http://bugs.java.com/view_bug.do?bug_id=4724038</a>
 * @see <a
 *      href="http://stackoverflow.com/questions/9261316/memory-mapped-mappedbytebuffer-or-direct-bytebuffer-for-db-implementation">http://stackoverflow.com/questions/9261316/memory-mapped-mappedbytebuffer-or-direct-bytebuffer-for-db-implementation</a>
 * 
 */
public class MemoryMappedFileManager extends OutputStreamManager {
    static final int DEFAULT_REGION_LENGTH = 32 * 1024 * 1024;
    private static final MemoryMappedFileManagerFactory FACTORY = new MemoryMappedFileManagerFactory();

    private final boolean isForce;
    private final int regionLength;
    private final String advertiseURI;
    private final RandomAccessFile randomAccessFile;
    private final ThreadLocal<Boolean> isEndOfBatch = new ThreadLocal<Boolean>();
    private MappedByteBuffer mappedBuffer;
    private long mappingOffset;

    protected MemoryMappedFileManager(final RandomAccessFile file, final String fileName, final OutputStream os,
            final boolean force, final long position, final int regionLength, final String advertiseURI,
            final Layout<? extends Serializable> layout) throws IOException {
        super(os, fileName, layout);
        this.isForce = force;
        this.randomAccessFile = Assert.requireNonNull(file, "RandomAccessFile");
        this.regionLength = regionLength;
        this.advertiseURI = advertiseURI;
        this.isEndOfBatch.set(Boolean.FALSE);
        this.mappedBuffer = mmap(randomAccessFile.getChannel(), position, regionLength);
        this.mappingOffset = position;
    }

    /**
     * Returns the MemoryMappedFileManager.
     *
     * @param fileName The name of the file to manage.
     * @param append true if the file should be appended to, false if it should be overwritten.
     * @param isForce true if the contents should be flushed to disk on every write
     * @param regionLength The mapped region length.
     * @param advertiseURI the URI to use when advertising the file
     * @param layout The layout.
     * @return A MemoryMappedFileManager for the File.
     */
    public static MemoryMappedFileManager getFileManager(final String fileName, final boolean append,
            final boolean isForce, final int regionLength, final String advertiseURI,
            final Layout<? extends Serializable> layout) {
        return (MemoryMappedFileManager) getManager(fileName, new FactoryData(append, isForce, regionLength,
                advertiseURI, layout), FACTORY);
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

        while (length > mappedBuffer.remaining()) {
            final int chunk = mappedBuffer.remaining();
            mappedBuffer.put(bytes, offset, chunk);
            offset += chunk;
            length -= chunk;
            remap();
        }
        mappedBuffer.put(bytes, offset, length);

        // no need to call flush() if force is true,
        // already done in AbstractOutputStreamAppender.append
    }

    private synchronized void remap() {
        final long offset = this.mappingOffset + mappedBuffer.position();
        final int length = mappedBuffer.remaining() + regionLength;
        try {
            unsafeUnmap(mappedBuffer);
            final long fileLength = randomAccessFile.length() + regionLength;
            randomAccessFile.setLength(fileLength);
            mappedBuffer = mmap(randomAccessFile.getChannel(), offset, length);
            mappingOffset = offset;
        } catch (final Exception ex) {
            LOGGER.error("Unable to remap " + getName() + ". " + ex);
        }
    }

    @Override
    public synchronized void flush() {
        mappedBuffer.force();
    }

    @Override
    public synchronized void close() {
        final long length = mappingOffset + mappedBuffer.position();
        try {
            unsafeUnmap(mappedBuffer);
        } catch (Exception ex) {
            LOGGER.error("Unable to unmap MappedBuffer " + getName() + ". " + ex);
        }
        try {
            randomAccessFile.setLength(length);
            randomAccessFile.close();
        } catch (final IOException ex) {
            LOGGER.error("Unable to close MemoryMappedFile " + getName() + ". " + ex);
        }
    }

    public static MappedByteBuffer mmap(FileChannel fileChannel, long start, int size) throws IOException {
        for (int i = 1;; i++) {
            try {
                MappedByteBuffer map = fileChannel.map(FileChannel.MapMode.READ_WRITE, start, size);
                map.order(ByteOrder.nativeOrder());
                return map;
            } catch (IOException e) {
                if (e.getMessage() == null || !e.getMessage().endsWith("user-mapped section open")) {
                    throw e;
                }
                if (i < 10) {
                    Thread.yield();
                } else {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                }
            }
        }
    }

    private static void unsafeUnmap(final MappedByteBuffer mbb) throws PrivilegedActionException {
        AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
            public Object run() throws Exception {
                final Method getCleanerMethod = mbb.getClass().getMethod("cleaner");
                getCleanerMethod.setAccessible(true);
                final Object cleaner = getCleanerMethod.invoke(mbb); // sun.misc.Cleaner instance
                final Method cleanMethod = cleaner.getClass().getMethod("clean");
                cleanMethod.invoke(cleaner);
                return null;
            }
        });
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
     * Returns the length of the memory mapped region.
     * 
     * @return the length of the mapped region
     */
    public int getRegionLength() {
        return regionLength;
    }
    
    /**
     * Returns {@code true} if the content of the buffer should be forced to the storage device on every write,
     * {@code false} otherwise.
     * @return whether each write should be force-sync'ed
     */
    public boolean isImmediateFlush() {
        return isForce;
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
     * FileManager's content format is specified by:
     * <p/>
     * Key: "fileURI" Value: provided "advertiseURI" param.
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
        private final boolean force;
        private final int regionLength;
        private final String advertiseURI;
        private final Layout<? extends Serializable> layout;

        /**
         * Constructor.
         *
         * @param append Append to existing file or truncate.
         * @param force forces the memory content to be written to the storage device on every event
         * @param regionLength length of the mapped region
         */
        public FactoryData(final boolean append, final boolean force, final int regionLength,
                final String advertiseURI, final Layout<? extends Serializable> layout) {
            this.append = append;
            this.force = force;
            this.regionLength = regionLength;
            this.advertiseURI = advertiseURI;
            this.layout = layout;
        }
    }

    /**
     * Factory to create a MemoryMappedFileManager.
     */
    private static class MemoryMappedFileManagerFactory implements ManagerFactory<MemoryMappedFileManager, FactoryData> {

        /**
         * Create a MemoryMappedFileManager.
         *
         * @param name The name of the File.
         * @param data The FactoryData
         * @return The MemoryMappedFileManager for the File.
         */
        @SuppressWarnings("resource")
        @Override
        public MemoryMappedFileManager createManager(final String name, final FactoryData data) {
            final File file = new File(name);
            final File parent = file.getParentFile();
            if (null != parent && !parent.exists()) {
                parent.mkdirs();
            }
            if (!data.append) {
                file.delete();
            }

            final OutputStream os = new DummyOutputStream();
            RandomAccessFile raf = null;
            try {
                raf = new RandomAccessFile(name, "rw");
                final long position = (data.append) ? raf.length() : 0;
                raf.setLength(position + data.regionLength);
                return new MemoryMappedFileManager(raf, name, os, data.force, position, data.regionLength,
                        data.advertiseURI, data.layout);
            } catch (final Exception ex) {
                LOGGER.error("MemoryMappedFileManager (" + name + ") " + ex);
                Closer.closeSilently(raf);
            }
            return null;
        }
    }
}
