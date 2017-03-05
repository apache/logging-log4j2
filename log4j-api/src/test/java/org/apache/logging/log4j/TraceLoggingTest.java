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
package org.apache.logging.log4j;

import org.apache.logging.log4j.message.DefaultFlowMessageFactory;
import org.apache.logging.log4j.message.EntryMessage;
import org.apache.logging.log4j.message.FlowMessageFactory;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.message.ReusableParameterizedMessage;
import org.apache.logging.log4j.message.ReusableParameterizedMessageTest;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class TraceLoggingTest extends AbstractLogger {
    static final StringBuilder CHAR_SEQ = new StringBuilder("CharSeq");
    private int charSeqCount;
    private int objectCount;

    private static class LogEvent {

        String markerName;
        Message data;
        Throwable t;

        public LogEvent(final String markerName, final Message data, final Throwable t) {
            this.markerName = markerName;
            this.data = data;
            this.t = t;
        }
    }

    private static final long serialVersionUID = 1L;

    private static Level currentLevel;

    private LogEvent currentEvent;

    private static Throwable t = new UnsupportedOperationException("Test");

    private static Class<AbstractLogger> obj = AbstractLogger.class;
    private static String pattern = "{}, {}";
    private static String p1 = "Long Beach";

    private static String p2 = "California";
    private static Message charSeq = new SimpleMessage(CHAR_SEQ);
    private static Message simple = new SimpleMessage("Hello");
    private static Message object = new ObjectMessage(obj);

    private static Message param = new ParameterizedMessage(pattern, p1, p2);

    private static String marker = "TEST";

    @Override
    public Level getLevel() {
        return currentLevel;
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final Message data, final Throwable t) {
        return true;
//        assertTrue("Incorrect Level. Expected " + currentLevel + ", actual " + level, level.equals(currentLevel));
//        if (marker == null) {
//            if (currentEvent.markerName != null) {
//                fail("Incorrect marker. Expected " + currentEvent.markerName + ", actual is null");
//            }
//        } else {
//            if (currentEvent.markerName == null) {
//                fail("Incorrect marker. Expected null. Actual is " + marker.getName());
//            } else {
//                assertTrue("Incorrect marker. Expected " + currentEvent.markerName + ", actual " +
//                    marker.getName(), currentEvent.markerName.equals(marker.getName()));
//            }
//        }
//        if (data == null) {
//            if (currentEvent.data != null) {
//                fail("Incorrect message. Expected " + currentEvent.data + ", actual is null");
//            }
//        } else {
//            if (currentEvent.data == null) {
//                fail("Incorrect message. Expected null. Actual is " + data.getFormattedMessage());
//            } else {
//                assertTrue("Incorrect message type. Expected " + currentEvent.data + ", actual " + data,
//                    data.getClass().isAssignableFrom(currentEvent.data.getClass()));
//                assertTrue("Incorrect message. Expected " + currentEvent.data.getFormattedMessage() + ", actual " +
//                    data.getFormattedMessage(),
//                    currentEvent.data.getFormattedMessage().equals(data.getFormattedMessage()));
//            }
//        }
//        if (t == null) {
//            if (currentEvent.t != null) {
//                fail("Incorrect Throwable. Expected " + currentEvent.t + ", actual is null");
//            }
//        } else {
//            if (currentEvent.t == null) {
//                fail("Incorrect Throwable. Expected null. Actual is " + t);
//            } else {
//                assertTrue("Incorrect Throwable. Expected " + currentEvent.t + ", actual " + t,
//                    currentEvent.t.equals(t));
//            }
//        }
//        return true;
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final CharSequence data, final Throwable t) {
        charSeqCount++;
        return isEnabled(level, marker, (Message) new SimpleMessage(data), t);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final Object data, final Throwable t) {
        objectCount++;
        return isEnabled(level, marker, new ObjectMessage(data), t);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String data) {
        return isEnabled(level, marker, (Message) new SimpleMessage(data), null);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String data, final Object... p1) {
        return isEnabled(level, marker, new ParameterizedMessage(data, p1), null);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String message, final Object p0) {
        return isEnabled(level, marker, new ParameterizedMessage(message, p0), null);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String message, final Object p0,
            final Object p1) {
        return isEnabled(level, marker, new ParameterizedMessage(message, p0, p1), null);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String message, final Object p0,
            final Object p1, final Object p2) {
        return isEnabled(level, marker, new ParameterizedMessage(message, p0, p1, p2), null);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String message, final Object p0,
            final Object p1, final Object p2, final Object p3) {
        return isEnabled(level, marker, new ParameterizedMessage(message, p0, p1, p2, p3), null);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String message, final Object p0,
            final Object p1, final Object p2, final Object p3,
            final Object p4) {
        return isEnabled(level, marker, new ParameterizedMessage(message, p0, p1, p2, p3, p4), null);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String message, final Object p0,
            final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5) {
        return isEnabled(level, marker, new ParameterizedMessage(message, p0, p1, p2, p3, p4, p5), null);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String message, final Object p0,
            final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6) {
        return isEnabled(level, marker, new ParameterizedMessage(message, p0, p1, p2, p3, p4, p5, p6), null);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String message, final Object p0,
            final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6,
            final Object p7) {
        return isEnabled(level, marker, new ParameterizedMessage(message, p0, p1, p2, p3, p4, p5, p6, p7), null);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String message, final Object p0,
            final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6,
            final Object p7, final Object p8) {
        return isEnabled(level, marker, new ParameterizedMessage(message, p0, p1, p2, p3, p4, p5, p6, p7, p8), null);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String message, final Object p0,
            final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6,
            final Object p7, final Object p8, final Object p9) {
        return isEnabled(level, marker, new ParameterizedMessage(message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9),
                null);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String data, final Throwable t) {
        return isEnabled(level, marker, (Message) new SimpleMessage(data), t);
    }

    @Override
    public void logMessage(final String fqcn, final Level level, final Marker marker, final Message data, final Throwable t) {
        assertTrue("Incorrect Level. Expected " + currentLevel + ", actual " + level, level.equals(currentLevel));
        if (marker == null) {
            if (currentEvent.markerName != null) {
                fail("Incorrect marker. Expected " + currentEvent.markerName + ", actual is null");
            }
        } else {
            if (currentEvent.markerName == null) {
                fail("Incorrect marker. Expected null. Actual is " + marker.getName());
            } else {
                assertTrue("Incorrect marker. Expected " + currentEvent.markerName + ", actual " +
                    marker.getName(), currentEvent.markerName.equals(marker.getName()));
            }
        }
        if (data == null) {
            if (currentEvent.data != null) {
                fail("Incorrect message. Expected " + currentEvent.data + ", actual is null");
            }
        } else {
            if (currentEvent.data == null) {
                fail("Incorrect message. Expected null. Actual is " + data.getFormattedMessage());
            } else {
                assertTrue("Incorrect message type. Expected " + currentEvent.data + ", actual " + data,
                    data.getClass().isAssignableFrom(currentEvent.data.getClass()));
                assertTrue("Incorrect message. Expected " + currentEvent.data.getFormattedMessage() + ", actual " +
                    data.getFormattedMessage(),
                    currentEvent.data.getFormattedMessage().equals(data.getFormattedMessage()));
            }
        }
        if (t == null) {
            if (currentEvent.t != null) {
                fail("Incorrect Throwable. Expected " + currentEvent.t + ", actual is null");
            }
        } else {
            if (currentEvent.t == null) {
                fail("Incorrect Throwable. Expected null. Actual is " + t);
            } else {
                assertTrue("Incorrect Throwable. Expected " + currentEvent.t + ", actual " + t,
                    currentEvent.t.equals(t));
            }
        }
    }

    @Test
    public void testTraceEntryExit() {
        currentLevel = Level.TRACE;
        final FlowMessageFactory fact = new DefaultFlowMessageFactory();

        final ParameterizedMessage paramMsg = new ParameterizedMessage("Tracy {}", "Logan");
        currentEvent = new LogEvent(ENTRY_MARKER.getName(), fact.newEntryMessage(paramMsg), null);
        final EntryMessage entry = traceEntry("Tracy {}", "Logan");

        final ReusableParameterizedMessage msg = ReusableParameterizedMessageTest.set(
                new ReusableParameterizedMessage(), "Tracy {}", "Logan");
        ReusableParameterizedMessageTest.set(msg, "Some other message {}", 123);
        currentEvent = new LogEvent(null, msg, null);
        trace("Some other message {}", 123);

        // ensure original entry message not overwritten
        assertEquals("Tracy Logan", entry.getMessage().getFormattedMessage());

        currentEvent = new LogEvent(EXIT_MARKER.getName(), fact.newExitMessage(entry), null);
        traceExit(entry);

        // ensure original entry message not overwritten
        assertEquals("Tracy Logan", entry.getMessage().getFormattedMessage());
    }

    @Test
    public void testTraceEntryMessage() {
        currentLevel = Level.TRACE;
        final FlowMessageFactory fact = new DefaultFlowMessageFactory();

        final ParameterizedMessage paramMsg = new ParameterizedMessage("Tracy {}", "Logan");
        currentEvent = new LogEvent(ENTRY_MARKER.getName(), fact.newEntryMessage(paramMsg), null);

        final ReusableParameterizedMessage msg = ReusableParameterizedMessageTest.set(
                new ReusableParameterizedMessage(), "Tracy {}", "Logan");
        final EntryMessage entry = traceEntry(msg);

        ReusableParameterizedMessageTest.set(msg, "Some other message {}", 123);
        currentEvent = new LogEvent(null, msg, null);
        trace("Some other message {}", 123);

        // ensure original entry message not overwritten
        assertEquals("Tracy Logan", entry.getMessage().getFormattedMessage());

        currentEvent = new LogEvent(EXIT_MARKER.getName(), fact.newExitMessage(entry), null);
        traceExit(entry);

        // ensure original entry message not overwritten
        assertEquals("Tracy Logan", entry.getMessage().getFormattedMessage());
    }
}
