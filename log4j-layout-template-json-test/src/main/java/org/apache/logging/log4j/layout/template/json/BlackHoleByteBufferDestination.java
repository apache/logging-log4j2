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
package org.apache.logging.log4j.layout.template.json;

import java.nio.ByteBuffer;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.apache.logging.log4j.core.layout.ByteBufferDestinationHelper;

class BlackHoleByteBufferDestination implements ByteBufferDestination {

    private final ByteBuffer byteBuffer;

    BlackHoleByteBufferDestination(final int maxByteCount) {
        this.byteBuffer = ByteBuffer.allocate(maxByteCount);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    @Override
    @SuppressWarnings("ReferenceEquality")
    public ByteBuffer drain(final ByteBuffer byteBuffer) {
        byteBuffer.flip();
        if (this.byteBuffer != byteBuffer) {
            this.byteBuffer.clear();
            this.byteBuffer.put(byteBuffer);
        }
        byteBuffer.clear();
        return byteBuffer;
    }

    @Override
    public void writeBytes(final ByteBuffer byteBuffer) {
        ByteBufferDestinationHelper.writeToUnsynchronized(byteBuffer, this);
    }

    @Override
    public void writeBytes(final byte[] buffer, final int offset, final int length) {
        ByteBufferDestinationHelper.writeToUnsynchronized(buffer, offset, length, this);
    }
}
