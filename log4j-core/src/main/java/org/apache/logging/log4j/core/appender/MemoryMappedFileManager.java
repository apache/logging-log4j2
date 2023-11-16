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
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.util.Closer;
import org.apache.logging.log4j.core.util.FileUtils;
import org.apache.logging.log4j.core.util.NullOutputStream;
import org.apache.logging.log4j.core.util.internal.UnsafeUtil;
import org.apache.logging.log4j.util.Constants;

// Lines too long...
// CHECKSTYLE:OFF
/**
 * Extends OutputStreamManager but instead of using a buffered output stream, this class maps a region of a file into
 * memory and writes to this memory region.
 * <p>
 *
 * @see <a href="http://www.codeproject.com/Tips/683614/Things-to-Know-about-Memory-Mapped-File-in-Java">
 *      http://www.codeproject.com/Tips/683614/Things-to-Know-about-Memory-Mapped-File-in-Java</a>
 * @see <a href="http://bugs.java.com/view_bug.do?bug_id=6893654">http://bugs.java.com/view_bug.do?bug_id=6893654</a>
 * @see <a href="http://bugs.java.com/view_bug.do?bug_id=4724038">http://bugs.java.com/view_bug.do?bug_id=4724038</a>
 * @see <a
 *      href="http://stackoverflow.com/questions/9261316/memory-mapped-mappedbytebuffer-or-direct-bytebuffer-for-db-implementation">
 *      http://stackoverflow.com/questions/9261316/memory-mapped-mappedbytebuffer-or-direct-bytebuffer-for-db-implementation</a>
 *
 * @since 2.1
 */
// CHECKSTYLE:ON
public class MemoryMappedFileManager extends OutputStreamManager {
    /**
     * Default length of region to map.
     */
    static final int DEFAULT_REGION_LENGTH = 32 * 1024 * 1024;

    private static final int MAX_REMAP_COUNT = 10;
    private static final MemoryMappedFileManagerFactory FACTORY = new MemoryMappedFileManagerFactory();
    private static final double NANOS_PER_MILLISEC = 1000.0 * 1000.0;

    private final boolean immediateFlush;
    private final int regionLength;
    private final String advertiseURI;
    private final RandomAccessFile randomAccessFile;
    private MappedByteBuffer mappedBuffer;
    private long mappingOffset;

    protected MemoryMappedFileManager(
            final RandomAccessFile file,
            final String fileName,
            final OutputStream os,
            final boolean immediateFlush,
            final long position,
            final int regionLength,
            final String advertiseURI,
            final Layout<? extends Serializable> layout,
            final boolean writeHeader)
            throws IOException {
        super(os, fileName, layout, writeHeader, ByteBuffer.wrap(Constants.EMPTY_BYTE_ARRAY));
        this.immediateFlush = immediateFlush;
        this.randomAccessFile = Objects.requireNonNull(file, "RandomAccessFile");
        this.regionLength = regionLength;
        this.advertiseURI = advertiseURI;
        this.mappedBuffer = mmap(randomAccessFile.getChannel(), getFileName(), position, regionLength);
        this.byteBuffer = mappedBuffer;
        this.mappingOffset = position;
    }

    /**
     * Returns the MemoryMappedFileManager.
     *
     * @param fileName The name of the file to manage.
     * @param append true if the file should be appended to, false if it should be overwritten.
     * @param immediateFlush true if the contents should be flushed to disk on every write
     * @param regionLength The mapped region length.
     * @param advertiseURI the URI to use when advertising the file
     * @param layout The layout.
     * @return A MemoryMappedFileManager for the File.
     */
    public static MemoryMappedFileManager getFileManager(
            final String fileName,
            final boolean append,
            final boolean immediateFlush,
            final int regionLength,
            final String advertiseURI,
            final Layout<? extends Serializable> layout) {
        return narrow(
                MemoryMappedFileManager.class,
                getManager(
                        fileName,
                        new FactoryData(append, immediateFlush, regionLength, advertiseURI, layout),
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
    protected synchronized void write(
            final byte[] bytes, final int offset, final int length, final boolean immediateFlush) {
        int currentOffset = offset;
        int currentLength = length;
        while (currentLength > mappedBuffer.remaining()) {
            final int chunk = mappedBuffer.remaining();
            mappedBuffer.put(bytes, currentOffset, chunk);
            currentOffset += chunk;
            currentLength -= chunk;
            remap();
        }
        mappedBuffer.put(bytes, currentOffset, currentLength);

        // no need to call flush() if force is true,
        // already done in AbstractOutputStreamAppender.append
    }

    private synchronized void remap() {
        final long offset = this.mappingOffset + mappedBuffer.position();
        final int length = mappedBuffer.remaining() + regionLength;
        try {
            unsafeUnmap(mappedBuffer);
            final long fileLength = randomAccessFile.length() + regionLength;
            LOGGER.debug(
                    "{} {} extending {} by {} bytes to {}",
                    getClass().getSimpleName(),
                    getName(),
                    getFileName(),
                    regionLength,
                    fileLength);

            final long startNanos = System.nanoTime();
            randomAccessFile.setLength(fileLength);
            final float millis = (float) ((System.nanoTime() - startNanos) / NANOS_PER_MILLISEC);
            LOGGER.debug(
                    "{} {} extended {} OK in {} millis", getClass().getSimpleName(), getName(), getFileName(), millis);

            mappedBuffer = mmap(randomAccessFile.getChannel(), getFileName(), offset, length);
            this.byteBuffer = mappedBuffer;
            mappingOffset = offset;
        } catch (final Exception ex) {
            logError("Unable to remap", ex);
        }
    }

    @Override
    public synchronized void flush() {
        mappedBuffer.force();
    }

    @Override
    public synchronized boolean closeOutputStream() {
        final long position = mappedBuffer.position();
        final long length = mappingOffset + position;
        try {
            unsafeUnmap(mappedBuffer);
        } catch (final Exception ex) {
            logError("Unable to unmap MappedBuffer", ex);
        }
        try {
            LOGGER.debug(
                    "MMapAppender closing. Setting {} length to {} (offset {} + position {})",
                    getFileName(),
                    length,
                    mappingOffset,
                    position);
            randomAccessFile.setLength(length);
            randomAccessFile.close();
            return true;
        } catch (final IOException ex) {
            logError("Unable to close MemoryMappedFile", ex);
            return false;
        }
    }

    public static MappedByteBuffer mmap(
            final FileChannel fileChannel, final String fileName, final long start, final int size) throws IOException {
        for (int i = 1; ; i++) {
            try {
                LOGGER.debug("MMapAppender remapping {} start={}, size={}", fileName, start, size);

                final long startNanos = System.nanoTime();
                final MappedByteBuffer map = fileChannel.map(FileChannel.MapMode.READ_WRITE, start, size);
                map.order(ByteOrder.nativeOrder());

                final float millis = (float) ((System.nanoTime() - startNanos) / NANOS_PER_MILLISEC);
                LOGGER.debug("MMapAppender remapped {} OK in {} millis", fileName, millis);

                return map;
            } catch (final IOException e) {
                if (e.getMessage() == null || !e.getMessage().endsWith("user-mapped section open")) {
                    throw e;
                }
                LOGGER.debug("Remap attempt {}/{} failed. Retrying...", i, MAX_REMAP_COUNT, e);
                if (i < MAX_REMAP_COUNT) {
                    Thread.yield();
                } else {
                    try {
                        Thread.sleep(1);
                    } catch (final InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                }
            }
        }
    }

    private static void unsafeUnmap(final MappedByteBuffer mbb) throws Exception {
        LOGGER.debug("MMapAppender unmapping old buffer...");
        final long startNanos = System.nanoTime();
        UnsafeUtil.clean(mbb);
        final float millis = (float) ((System.nanoTime() - startNanos) / NANOS_PER_MILLISEC);
        LOGGER.debug("MMapAppender unmapped buffer OK in {} millis", millis);
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
     *
     * @return whether each write should be force-sync'ed
     */
    public boolean isImmediateFlush() {
        return immediateFlush;
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

    @Override
    protected void flushBuffer(final ByteBuffer buffer) {
        // do nothing (do not call drain() to avoid spurious remapping)
    }

    @Override
    public ByteBuffer getByteBuffer() {
        return mappedBuffer;
    }

    @Override
    public ByteBuffer drain(final ByteBuffer buf) {
        remap();
        return mappedBuffer;
    }

    /**
     * Factory Data.
     */
    private static class FactoryData {
        private final boolean append;
        private final boolean immediateFlush;
        private final int regionLength;
        private final String advertiseURI;
        private final Layout<? extends Serializable> layout;

        /**
         * Constructor.
         *
         * @param append Append to existing file or truncate.
         * @param immediateFlush forces the memory content to be written to the storage device on every event
         * @param regionLength length of the mapped region
         * @param advertiseURI the URI to use when advertising the file
         * @param layout The layout.
         */
        public FactoryData(
                final boolean append,
                final boolean immediateFlush,
                final int regionLength,
                final String advertiseURI,
                final Layout<? extends Serializable> layout) {
            this.append = append;
            this.immediateFlush = immediateFlush;
            this.regionLength = regionLength;
            this.advertiseURI = advertiseURI;
            this.layout = layout;
        }
    }

    /**
     * Factory to create a MemoryMappedFileManager.
     */
    private static class MemoryMappedFileManagerFactory
            implements ManagerFactory<MemoryMappedFileManager, FactoryData> {

        /**
         * Create a MemoryMappedFileManager.
         *
         * @param name The name of the File.
         * @param data The FactoryData
         * @return The MemoryMappedFileManager for the File.
         */
        @SuppressWarnings("resource")
        @Override
        @SuppressFBWarnings(
                value = "PATH_TRAVERSAL_IN",
                justification = "The destination file should be specified in the configuration file.")
        public MemoryMappedFileManager createManager(final String name, final FactoryData data) {
            final File file = new File(name);
            if (!data.append) {
                file.delete();
            }

            final boolean writeHeader = !data.append || !file.exists();
            final OutputStream os = NullOutputStream.getInstance();
            RandomAccessFile raf = null;
            try {
                FileUtils.makeParentDirs(file);
                raf = new RandomAccessFile(name, "rw");
                final long position = (data.append) ? raf.length() : 0;
                raf.setLength(position + data.regionLength);
                return new MemoryMappedFileManager(
                        raf,
                        name,
                        os,
                        data.immediateFlush,
                        position,
                        data.regionLength,
                        data.advertiseURI,
                        data.layout,
                        writeHeader);
            } catch (final Exception ex) {
                LOGGER.error("MemoryMappedFileManager (" + name + ") " + ex, ex);
                Closer.closeSilently(raf);
            }
            return null;
        }
    }
}
