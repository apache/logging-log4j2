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
package org.apache.logging.log4j.core.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

import org.apache.logging.log4j.core.layout.ByteBufferDestination;

/**
 * Buffered OutputStream that implements the ByteBufferDestination interface.
 * <p>
 * This class is not thread-safe and it cannot be made thread-safe on its own since it exposes its buffer for direct
 * modification. Client code needs to make sure multiple threads don't modify the buffer or write to this OutputStream
 * concurrently.
 * </p>
 *
 * @see ByteBufferDestination
 */
public class ByteBufferDestinationOutputStream extends FilterOutputStream implements ByteBufferDestination {
    private static final int DEFAULT_BUFFER_SIZE = 8 * 1024;
    private final ByteBuffer byteBuffer;

    public ByteBufferDestinationOutputStream(final OutputStream out) {
        this(out, DEFAULT_BUFFER_SIZE);
    }

    public ByteBufferDestinationOutputStream(final OutputStream out, final int bufferSize) {
        super(Objects.requireNonNull(out));
        this.byteBuffer = ByteBuffer.wrap(new byte[Assert.valueIsAtLeast(bufferSize, 16)]);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    /**
     * Writes any data in the specified {@code ByteBuffer} to the wrapped {@code OutputStream}, returning the specified
     * {@code ByteBuffer}.
     *
     * @param buf the buffer to drain
     * @return the specified ByteBuffer
     */
    @Override
    public ByteBuffer drain(final ByteBuffer buf) {
        buf.flip();
        try {
            out.write(buf.array(), 0, buf.limit());
        } catch (final IOException ex) {
            throw new IllegalStateException("Could not write " + buf.limit() + " bytes to " + out, ex);
        }
        buf.clear();
        return buf;
    }

    /**
     * Writes any data remaining in the {@code ByteBuffer} to the {@code OutputStream}.
     */
    /**
     * Flushes this buffered output stream. This forces any buffered
     * output bytes to be written out to the underlying output stream.
     *
     * @exception  IOException  if an I/O error occurs.
     * @see        java.io.FilterOutputStream#out
     */
    public void flush() {
        flushBuffer();
        try {
            out.flush();
        } catch (final IOException ex) {
            throw new IllegalStateException("Could not flush " + out, ex);
        }
    }

    private void flushBuffer() {
        drain(byteBuffer);
    }

    /**
     * Writes <code>len</code> bytes from the specified byte array
     * starting at offset <code>off</code> to this buffered output stream.
     *
     * <p> Ordinarily this method stores bytes from the given array into this
     * stream's buffer, flushing the buffer to the underlying output stream as
     * needed.  If the requested length is at least as large as this stream's
     * buffer, however, then this method will flush the buffer and write the
     * bytes directly to the underlying output stream.  Thus redundant
     * <code>BufferedOutputStream</code>s will not copy data unnecessarily.
     *
     * @param      src     the data.
     * @param      offset   the start offset in the data.
     * @param      length   the number of bytes to write.
     * @exception  IOException  if an I/O error occurs.
     */
    public void write(final byte src[], final int offset, final int length) throws IOException {
        if (length >= byteBuffer.capacity()) {
            /* If the request length exceeds the size of the output buffer,
               flush the output buffer and then write the data directly.
               In this way buffered streams will cascade harmlessly. */
            flushBuffer();
            out.write(src, offset, length);
            return;
        }
        if (length > byteBuffer.remaining()) {
            flushBuffer();
        }
        byteBuffer.put(src, offset, length);
    }


    /**
     * Writes the specified byte to this buffered output stream.
     *
     * @param      b   the byte to be written.
     * @exception  IOException  if an I/O error occurs.
     */
    public void write(final int b) throws IOException {
        if (byteBuffer.remaining() <= 0) {
            flushBuffer();
        }
        byteBuffer.put((byte) b);
    }
}
