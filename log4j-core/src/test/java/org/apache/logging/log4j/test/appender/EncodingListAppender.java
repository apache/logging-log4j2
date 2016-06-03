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
package org.apache.logging.log4j.test.appender;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.apache.logging.log4j.core.layout.SerializedLayout;

import java.io.Serializable;
import java.nio.ByteBuffer;

/**
 * This appender is primarily used for testing. Use in a real environment is discouraged as the
 * List could eventually grow to cause an OutOfMemoryError.
 *
 * This appender will use {@link Layout#encode(Object, ByteBufferDestination)} (and not {@link Layout#toByteArray(LogEvent)}).
 */
public class EncodingListAppender extends ListAppender {

    public EncodingListAppender(final String name) {
        super(name);
    }

    public EncodingListAppender(final String name, final Filter filter, final Layout<? extends Serializable> layout, final boolean newline, final boolean raw) {
        super(name, filter, layout, newline, raw);
    }

    private class Destination implements ByteBufferDestination {
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[4096]);
        @Override
        public ByteBuffer getByteBuffer() {
            return byteBuffer;
        }

        @Override
        public ByteBuffer drain(final ByteBuffer buf) {
            throw new IllegalStateException("Unexpected message larger than 4096 bytes");
        }
    }

    @Override
    public synchronized void append(final LogEvent event) {
        final Layout<? extends Serializable> layout = getLayout();
        if (layout == null) {
            events.add(event);
        } else if (layout instanceof SerializedLayout) {
            final Destination content = new Destination();
            content.byteBuffer.put(layout.getHeader());
            layout.encode(event, content);
            content.getByteBuffer().flip();
            final byte[] record = new byte[content.getByteBuffer().remaining()];
            content.getByteBuffer().get(record);
            data.add(record);
        } else {
            final Destination content = new Destination();
            layout.encode(event, content);
            content.getByteBuffer().flip();
            final byte[] record = new byte[content.getByteBuffer().remaining()];
            content.getByteBuffer().get(record);
            write(record);
        }
    }

}
