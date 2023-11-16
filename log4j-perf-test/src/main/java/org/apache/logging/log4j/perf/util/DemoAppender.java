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
package org.apache.logging.log4j.perf.util;

import java.nio.ByteBuffer;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.apache.logging.log4j.core.layout.ByteBufferDestinationHelper;
import org.apache.logging.log4j.core.util.Constants;

/**
 * Demo Appender that does not do any I/O.
 */
public class DemoAppender extends AbstractAppender implements ByteBufferDestination {
    private final ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[4096]);

    public long checksum;

    public DemoAppender(final Layout<?> layout) {
        super("demo", null, layout, true, Property.EMPTY_ARRAY);
    }

    @Override
    public void append(final LogEvent event) {
        if (Constants.ENABLE_DIRECT_ENCODERS) {
            getLayout().encode(event, this);
            drain(byteBuffer);
        } else {
            final byte[] binary = getLayout().toByteArray(event);
            consume(binary, 0, binary.length);
        }
    }

    @Override
    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    @Override
    public ByteBuffer drain(final ByteBuffer buf) {
        buf.flip();
        consume(buf.array(), buf.position(), buf.limit());
        buf.clear();
        return buf;
    }

    @Override
    public void writeBytes(final ByteBuffer data) {
        ByteBufferDestinationHelper.writeToUnsynchronized(data, this);
    }

    @Override
    public void writeBytes(final byte[] data, final int offset, final int length) {
        ByteBufferDestinationHelper.writeToUnsynchronized(data, offset, length, this);
    }

    private void consume(final byte[] data, final int offset, final int length) {
        // need to do something with the result or the JVM may optimize everything away
        long sum = 0;
        for (int i = offset; i < length; i++) {
            sum += data[i];
        }
        checksum += sum;
    }
}
