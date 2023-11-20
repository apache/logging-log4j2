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
import org.apache.logging.log4j.core.appender.OutputStreamManager;

/**
 * ByteBufferDestination is the destination that {@link Encoder}s write binary data to. It encapsulates a
 * {@code ByteBuffer} and a {@code drain()} method the producer can call when the {@code ByteBuffer} is full.
 * <p>
 * This interface allows a producer to write arbitrary amounts of data to a destination.
 * </p>
 * @since 2.6
 */
public interface ByteBufferDestination {
    /**
     * Returns the buffer to write to.
     *
     * @return the buffer to write to
     */
    ByteBuffer getByteBuffer();

    /**
     * Consumes the buffer content and returns a buffer with more {@linkplain ByteBuffer#remaining() available} space
     * (which may or may not be the same instance).
     * <p>
     * Called by the producer when buffer becomes too full to write to.
     *
     * @param buf the buffer to drain
     * @return a buffer with more available space (which may or may not be the same instance)
     */
    ByteBuffer drain(ByteBuffer buf);

    /**
     * Writes the given data to this ByteBufferDestination entirely. Call of this method should *not* be protected
     * with synchronized on this ByteBufferDestination instance. ByteBufferDestination implementations should
     * synchronize themselves inside this method, if needed.
     *
     * @since 2.9 (see LOG4J2-1874)
     */
    void writeBytes(ByteBuffer data);

    /**
     * Writes the given data to this ByteBufferDestination. Call of this method should *not* be protected with
     * synchronized on this ByteBufferDestination instance. ByteBufferDestination implementations should
     * synchronize themselves inside this method, if needed.
     * <p>
     * This method should behave identically to {@code writeBytes(ByteBuffer.wrap(data, offset, length)}.
     * It is provided to allow callers not to generate extra garbage.
     * <p>
     * This method is called writeBytes() to avoid clashing with {@link OutputStreamManager#write(byte[], int, int)},
     * which might be overridden in user-defined subclasses as protected, hence adding it to interface and requiring
     * the method to be public breaks source compatibility.
     *
     * @since 2.9 (see LOG4J2-1874)
     */
    void writeBytes(byte[] data, int offset, int length);
}
