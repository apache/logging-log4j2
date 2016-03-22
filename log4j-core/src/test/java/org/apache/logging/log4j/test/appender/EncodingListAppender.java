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

    public EncodingListAppender(String name) {
        super(name);
    }

    public EncodingListAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean newline, boolean raw) {
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
            Destination content = new Destination();
            content.byteBuffer.put(layout.getHeader());
            layout.encode(event, content);
            content.getByteBuffer().flip();
            byte[] record = new byte[content.getByteBuffer().remaining()];
            content.getByteBuffer().get(record);
            data.add(record);
        } else {
            Destination content = new Destination();
            layout.encode(event, content);
            content.getByteBuffer().flip();
            byte[] record = new byte[content.getByteBuffer().remaining()];
            content.getByteBuffer().get(record);
            write(record);
        }
    }

}
