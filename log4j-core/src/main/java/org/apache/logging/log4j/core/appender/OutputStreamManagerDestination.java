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

import java.nio.ByteBuffer;
import java.util.Objects;

import org.apache.logging.log4j.core.layout.ByteBufferDestination;

/**
 * Decorates an {@code OutputStreamManager} to adapt it to the {@code ByteBufferDestination} interface.
 */
public class OutputStreamManagerDestination implements ByteBufferDestination {
    private static final int DEFAULT_BUFFER_SIZE = 8 * 1024;
    private final ByteBuffer byteBuffer;
    private final boolean immediateFlush;
    private final OutputStreamManager outputStreamManager;

    public OutputStreamManagerDestination(final boolean immediateFlush, final OutputStreamManager outputStreamManager) {
        this(DEFAULT_BUFFER_SIZE, immediateFlush, outputStreamManager);
    }

    public OutputStreamManagerDestination(final int bufferSize, final boolean immediateFlush,
            final OutputStreamManager outputStreamManager) {

        this.byteBuffer = ByteBuffer.wrap(new byte[bufferSize]);
        this.immediateFlush = immediateFlush;
        this.outputStreamManager = Objects.requireNonNull(outputStreamManager, "outputStreamManager");
    }

    @Override
    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    @Override
    public ByteBuffer drain(final ByteBuffer buf) {
        buf.flip();
        if (buf.limit() > 0) {
            outputStreamManager.write(buf.array(), 0, buf.limit(), immediateFlush);
        }
        buf.clear();
        return buf;
    }
}
