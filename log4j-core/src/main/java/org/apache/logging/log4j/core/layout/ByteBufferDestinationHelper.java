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
package org.apache.logging.log4j.core.layout;

import java.nio.ByteBuffer;

/**
 * Helper class for ByteBufferDestination implementors.
 *
 * @since 2.9 (see LOG4J2-1874)
 * @deprecated use methods on ByteBufferDestination directly
 */
@Deprecated(since = "3.0.0")
public final class ByteBufferDestinationHelper {

    private ByteBufferDestinationHelper() {
    }

    /**
     * Writes the specified data to the specified destination. Doesn't synchronize on the destination object. The helper
     * method for {@link ByteBufferDestination#writeBytes(ByteBuffer)} implementations.
     *
     * @param source        the data to write
     * @param destination the {@code ByteBufferDestination} to write to
     * @deprecated use {@link ByteBufferDestination#unsynchronizedWrite(ByteBuffer)}
     */
    @Deprecated(since = "3.0.0")
    public static void writeToUnsynchronized(final ByteBuffer source, final ByteBufferDestination destination) {
        destination.unsynchronizedWrite(source);
    }

    /**
     * Writes the specified data to the specified destination. Doesn't synchronize on the destination object. The helper
     * method for {@link ByteBufferDestination#writeBytes(byte[], int, int)} implementations.
     *
     * @param data        the data to write
     * @param offset      where to start in the specified data array
     * @param length      the number of bytes to write
     * @param destination the {@code ByteBufferDestination} to write to
     * @deprecated use {@link ByteBufferDestination#unsynchronizedWrite(byte[], int, int)}
     */
    @Deprecated(since = "3.0.0")
    public static void writeToUnsynchronized(final byte[] data, final int offset, final int length,
            final ByteBufferDestination destination) {
        destination.unsynchronizedWrite(data, offset, length);
    }
}
