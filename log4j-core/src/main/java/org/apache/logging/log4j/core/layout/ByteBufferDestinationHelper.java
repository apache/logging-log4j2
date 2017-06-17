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
package org.apache.logging.log4j.core.layout;

import java.nio.ByteBuffer;

public final class ByteBufferDestinationHelper {

    private ByteBufferDestinationHelper() {
    }

    /**
     * Writes the specified data to the specified destination. Doesn't synchronize on the destination object. The helper
     * method for {@link ByteBufferDestination#writeBytes(ByteBuffer)} implementations.
     *
     * @param data        the data to write
     * @param destination the {@code ByteBufferDestination} to write to
     */
    public static void writeToUnsynchronized(ByteBuffer data, ByteBufferDestination destination) {
        ByteBuffer buf = destination.getByteBuffer();
        while (data.remaining() > buf.remaining()) {
            int dataLimit = data.limit();
            data.limit(Math.min(data.limit(), data.position() + buf.remaining()));
            buf.put(data);
            data.limit(dataLimit);
            buf = destination.drain(buf);
        }
        buf.put(data);
        // No drain in the end.
    }

    /**
     * Writes the specified data to the specified destination. Doesn't synchronize on the destination object. The helper
     * method for {@link ByteBufferDestination#writeBytes(byte[], int, int)} implementations.
     *
     * @param data        the data to write
     * @param offset      where to start in the specified data array
     * @param length      the number of bytes to write
     * @param destination the {@code ByteBufferDestination} to write to
     */
    public static void writeToUnsynchronized(final byte[] data, int offset, int length,
            final ByteBufferDestination destination) {
        ByteBuffer buffer = destination.getByteBuffer();
        while (length > buffer.remaining()) {
            int chunk = buffer.remaining();
            buffer.put(data, offset, chunk);
            offset += chunk;
            length -= chunk;
            buffer = destination.drain(buffer);
        }
        buffer.put(data, offset, length);
        // No drain in the end.
    }
}
