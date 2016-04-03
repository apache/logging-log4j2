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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.apache.logging.log4j.core.util.Assert;

/**
 * Adapts an OutputStream to the ByteBufferDestination interface.
 *
 * @see ByteBufferDestination
 */
public class OutputStreamByteBufferDestinationAdapter implements ByteBufferDestination {
    private static final int DEFAULT_BUFFER_SIZE = 4 * 1024;
    private final OutputStream out;
    private final int bufferSize;
    private ByteBuffer byteBuffer;

    public OutputStreamByteBufferDestinationAdapter(final OutputStream out) {
        this(out, DEFAULT_BUFFER_SIZE);
    }

    public OutputStreamByteBufferDestinationAdapter(final OutputStream out, final int bufferSize) {
        this.out = Objects.requireNonNull(out);
        this.bufferSize = Assert.valueIsAtLeast(bufferSize, 16);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        if (byteBuffer == null) {
            byteBuffer = ByteBuffer.wrap(new byte[bufferSize]);
        }
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
    public void drain() {
        drain(getByteBuffer());
    }
}
