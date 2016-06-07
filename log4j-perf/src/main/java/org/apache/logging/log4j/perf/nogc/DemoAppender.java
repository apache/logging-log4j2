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
package org.apache.logging.log4j.perf.nogc;

import java.nio.ByteBuffer;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;

/**
 * Demo Appender that does not do any I/O.
 */
public class DemoAppender extends AbstractAppender implements ByteBufferDestination {
    private final ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[4096]);

    public long checksum;

    public DemoAppender(final Layout<?> layout) {
        super("demo", null, layout);
    }

    @Override
    public void append(final LogEvent event) {
        final Layout<?> layout = getLayout();
        if (layout instanceof NoGcLayout) {
            layout.encode(event, this);
            drain(byteBuffer);
        } else {
            final byte[] binary = getLayout().toByteArray(event);
            consume(binary, 0, binary.length);
        }
    }

    private void consume(final byte[] data, final int offset, final int length) {
        // need to do something with the result or the JVM may optimize everything away
        long sum = 0;
        for (int i = offset; i < length; i++) {
            sum += data[i];
        }
        checksum += sum;
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
}
