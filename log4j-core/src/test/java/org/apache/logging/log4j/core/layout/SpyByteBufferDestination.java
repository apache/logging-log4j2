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
import java.util.ArrayList;
import java.util.List;

/**
 * ByteBufferDestination for test purposes that counts how much data was added.
 */
public class SpyByteBufferDestination implements ByteBufferDestination {
    public final ByteBuffer buffer;
    public final ByteBuffer drained;
    public final List<Data> drainPoints = new ArrayList<>();

    public static class Data {
        public final int position;
        public final int limit;

        public Data(final int position, final int limit) {
            this.position = position;
            this.limit = limit;
        }

        public int length() {
            return limit - position;
        }
    }

    public SpyByteBufferDestination(final int bufferSize, final int destinationSize) {
        buffer = ByteBuffer.wrap(new byte[bufferSize]);
        drained = ByteBuffer.wrap(new byte[destinationSize]);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        return buffer;
    }

    @Override
    public ByteBuffer drain(final ByteBuffer buf) {
        buf.flip();
        drainPoints.add(new Data(buf.position(), buf.limit()));
        drained.put(buf);
        buf.clear();
        return buf;
    }
}
