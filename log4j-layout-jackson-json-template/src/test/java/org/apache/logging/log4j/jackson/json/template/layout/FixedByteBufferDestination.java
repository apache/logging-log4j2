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
package org.apache.logging.log4j.jackson.json.template.layout;

import org.apache.logging.log4j.core.layout.ByteBufferDestination;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

class FixedByteBufferDestination implements ByteBufferDestination {

    private final ByteBuffer byteBuffer;

    FixedByteBufferDestination(final int maxByteCount) {
        this.byteBuffer = ByteBuffer.allocate(maxByteCount);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    @Override
    @SuppressWarnings("ReferenceEquality")
    public ByteBuffer drain(final ByteBuffer sourceByteBuffer) {
        if (byteBuffer != sourceByteBuffer) {
            sourceByteBuffer.flip();
            byteBuffer.put(sourceByteBuffer);
        } else if (byteBuffer.remaining() == 0) {
            throw new BufferOverflowException();
        }
        return byteBuffer;
    }

    @Override
    public void writeBytes(final ByteBuffer sourceByteBuffer) {
        byteBuffer.put(sourceByteBuffer);
    }

    @Override
    public void writeBytes(final byte[] buffer, final int offset, final int length) {
        byteBuffer.put(buffer,offset,length);
    }

}
