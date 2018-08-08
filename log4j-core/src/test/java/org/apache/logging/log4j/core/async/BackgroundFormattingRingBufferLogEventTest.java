package org.apache.logging.log4j.core.async;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.categories.AsyncLoggers;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.time.internal.DummyNanoClock;
import org.apache.logging.log4j.core.time.internal.FixedPreciseClock;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageContentFormatter;
import org.apache.logging.log4j.message.ReusableMessageFactory;
import org.apache.logging.log4j.message.ReusableObjectMessage;
import org.apache.logging.log4j.util.StringBuilders;
import org.apache.logging.log4j.util.StringMap;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests the RingBufferLogEvent class when background message formatting is enabled.
 */
@Category(AsyncLoggers.class)
public class BackgroundFormattingRingBufferLogEventTest {

    @BeforeClass
    public static void beforeClass() {
        System.setProperty("log4j.format.msg.async", "true");
    }

    @AfterClass
    public static void afterClass() {
        System.clearProperty("log4j.format.msg.async");
    }

    private class TestReusableMessage extends ReusableObjectMessage {
        @Override
        public MessageContentFormatter getMessageContentFormatter() {
            return (formatString, parameters, parameterCount, buffer) -> StringBuilders.appendValue(buffer, "foo");
        }
    }

    @Test
    public void testMessageContentFormatters() {
        RingBufferLogEvent evt = RingBufferLogEvent.FACTORY.newInstance();
        final String loggerName = null;
        final Marker marker = null;
        final String fqcn = null;
        final Level level = null;
        final Message data = new TestReusableMessage();
        final Throwable t = null;
        final ContextStack contextStack = null;
        final String threadName = null;
        final StackTraceElement location = null;
        evt.setValues(null, loggerName, marker, fqcn, level, data, t, (StringMap) evt.getContextData(),
                contextStack, -1, threadName, -1, location, new FixedPreciseClock(), new DummyNanoClock(1));
        assertNotNull(evt.getMessage());
        assertEquals(evt, evt.getMessage());
        assertEquals("foo", evt.getFormattedMessage());

        final Message data2 = ReusableMessageFactory.INSTANCE.newMessage("test");
        RingBufferLogEvent evt2 = RingBufferLogEvent.FACTORY.newInstance();
        evt2.setValues(null, loggerName, marker, fqcn, level, data2, t, (StringMap) evt.getContextData(),
                contextStack, -1, threadName, -1, location, new FixedPreciseClock(), new DummyNanoClock(1));
        assertNotNull(evt2.getMessage());
        assertEquals(evt2, evt2.getMessage());
        assertEquals("test", evt2.getFormattedMessage());
    }
}
