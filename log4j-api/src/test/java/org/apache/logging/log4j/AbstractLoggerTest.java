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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.message.ParameterizedMessageFactory;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.spi.MessageFactory2Adapter;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class AbstractLoggerTest extends AbstractLogger {
    static final StringBuilder CHAR_SEQ = new StringBuilder("CharSeq");
    private int charSeqCount;
    private int objectCount;

    // TODO add proper tests for ReusableMessage
    @Before
    public void before() throws Exception {
        final Field field = AbstractLogger.class.getDeclaredField("messageFactory");
        field.setAccessible(true); // make non-private

        final Field modifierField = Field.class.getDeclaredField("modifiers");
        modifierField.setAccessible(true);
        modifierField.setInt(field, field.getModifiers() &~ Modifier.FINAL); // make non-private

        field.set(this, new MessageFactory2Adapter(ParameterizedMessageFactory.INSTANCE));
    }

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

    private static LogEvent[] events = new LogEvent[] {
        new LogEvent(null, simple, null),
        new LogEvent(marker, simple, null),
        new LogEvent(null, simple, t),
        new LogEvent(marker, simple, t),

        new LogEvent(null, object, null),
        new LogEvent(marker, object, null),
        new LogEvent(null, object, t),
        new LogEvent(marker, object, t),

        new LogEvent(null, param, null),
        new LogEvent(marker, param, null),

        new LogEvent(null, simple, null),
        new LogEvent(null, simple, t),
        new LogEvent(marker, simple, null),
        new LogEvent(marker, simple, t),
        new LogEvent(marker, simple, null),

        new LogEvent(null, charSeq, null),
        new LogEvent(null, charSeq, t),
        new LogEvent(marker, charSeq, null),
        new LogEvent(marker, charSeq, t),
    };

    @Override
    public Level getLevel() {
        return currentLevel;
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final Message data, final Throwable t) {
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
        return true;
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
    public void testDebug() {
        currentLevel = Level.DEBUG;

        currentEvent = events[0];
        debug("Hello");
        debug((Marker) null, "Hello");
        currentEvent = events[1];
        debug(MarkerManager.getMarker("TEST"), "Hello");
        currentEvent = events[2];
        debug("Hello", t);
        debug((Marker) null, "Hello", t);
        currentEvent = events[3];
        debug(MarkerManager.getMarker("TEST"), "Hello", t);
        currentEvent = events[4];
        debug(obj);
        currentEvent = events[5];
        debug(MarkerManager.getMarker("TEST"), obj);
        currentEvent = events[6];
        debug(obj, t);
        debug((Marker) null, obj, t);
        currentEvent = events[7];
        debug(MarkerManager.getMarker("TEST"), obj, t);
        currentEvent = events[8];
        debug(pattern, p1, p2);
        currentEvent = events[9];
        debug(MarkerManager.getMarker("TEST"), pattern, p1, p2);
        currentEvent = events[10];
        debug(simple);
        debug((Marker) null, simple);
        debug((Marker) null, simple, null);
        currentEvent = events[11];
        debug(simple, t);
        debug((Marker) null, simple, t);
        currentEvent = events[12];
        debug(MarkerManager.getMarker("TEST"), simple, null);
        currentEvent = events[13];
        debug(MarkerManager.getMarker("TEST"), simple, t);
        currentEvent = events[14];
        debug(MarkerManager.getMarker("TEST"), simple);

        currentEvent = events[15];
        debug(CHAR_SEQ);
        currentEvent = events[16];
        debug(CHAR_SEQ, t);
        currentEvent = events[17];
        debug(MarkerManager.getMarker("TEST"), CHAR_SEQ);
        currentEvent = events[18];
        debug(MarkerManager.getMarker("TEST"), CHAR_SEQ, t);

        assertEquals("log(CharSeq) invocations", 4, charSeqCount);
        assertEquals("log(Object) invocations", 5, objectCount);
    }

    @Test
    public void testError() {
        currentLevel = Level.ERROR;

        currentEvent = events[0];
        error("Hello");
        error((Marker) null, "Hello");
        currentEvent = events[1];
        error(MarkerManager.getMarker("TEST"), "Hello");
        currentEvent = events[2];
        error("Hello", t);
        error((Marker) null, "Hello", t);
        currentEvent = events[3];
        error(MarkerManager.getMarker("TEST"), "Hello", t);
        currentEvent = events[4];
        error(obj);
        currentEvent = events[5];
        error(MarkerManager.getMarker("TEST"), obj);
        currentEvent = events[6];
        error(obj, t);
        error((Marker) null, obj, t);
        currentEvent = events[7];
        error(MarkerManager.getMarker("TEST"), obj, t);
        currentEvent = events[8];
        error(pattern, p1, p2);
        currentEvent = events[9];
        error(MarkerManager.getMarker("TEST"), pattern, p1, p2);
        currentEvent = events[10];
        error(simple);
        error((Marker) null, simple);
        error((Marker) null, simple, null);
        currentEvent = events[11];
        error(simple, t);
        error((Marker) null, simple, t);
        currentEvent = events[12];
        error(MarkerManager.getMarker("TEST"), simple, null);
        currentEvent = events[13];
        error(MarkerManager.getMarker("TEST"), simple, t);
        currentEvent = events[14];
        error(MarkerManager.getMarker("TEST"), simple);

        currentEvent = events[15];
        error(CHAR_SEQ);
        currentEvent = events[16];
        error(CHAR_SEQ, t);
        currentEvent = events[17];
        error(MarkerManager.getMarker("TEST"), CHAR_SEQ);
        currentEvent = events[18];
        error(MarkerManager.getMarker("TEST"), CHAR_SEQ, t);

        assertEquals("log(CharSeq) invocations", 4, charSeqCount);
        assertEquals("log(Object) invocations", 5, objectCount);
    }

    @Test
    public void testFatal() {
        currentLevel = Level.FATAL;

        currentEvent = events[0];
        fatal("Hello");
        fatal((Marker) null, "Hello");
        currentEvent = events[1];
        fatal(MarkerManager.getMarker("TEST"), "Hello");
        currentEvent = events[2];
        fatal("Hello", t);
        fatal((Marker) null, "Hello", t);
        currentEvent = events[3];
        fatal(MarkerManager.getMarker("TEST"), "Hello", t);
        currentEvent = events[4];
        fatal(obj);
        currentEvent = events[5];
        fatal(MarkerManager.getMarker("TEST"), obj);
        currentEvent = events[6];
        fatal(obj, t);
        fatal((Marker) null, obj, t);
        currentEvent = events[7];
        fatal(MarkerManager.getMarker("TEST"), obj, t);
        currentEvent = events[8];
        fatal(pattern, p1, p2);
        currentEvent = events[9];
        fatal(MarkerManager.getMarker("TEST"), pattern, p1, p2);
        currentEvent = events[10];
        fatal(simple);
        fatal((Marker) null, simple);
        fatal((Marker) null, simple, null);
        currentEvent = events[11];
        fatal(simple, t);
        fatal((Marker) null, simple, t);
        currentEvent = events[12];
        fatal(MarkerManager.getMarker("TEST"), simple, null);
        currentEvent = events[13];
        fatal(MarkerManager.getMarker("TEST"), simple, t);
        currentEvent = events[14];
        fatal(MarkerManager.getMarker("TEST"), simple);

        currentEvent = events[15];
        fatal(CHAR_SEQ);
        currentEvent = events[16];
        fatal(CHAR_SEQ, t);
        currentEvent = events[17];
        fatal(MarkerManager.getMarker("TEST"), CHAR_SEQ);
        currentEvent = events[18];
        fatal(MarkerManager.getMarker("TEST"), CHAR_SEQ, t);

        assertEquals("log(CharSeq) invocations", 4, charSeqCount);
        assertEquals("log(Object) invocations", 5, objectCount);
    }

    @Test
    public void testInfo() {
        currentLevel = Level.INFO;

        currentEvent = events[0];
        info("Hello");
        info((Marker) null, "Hello");
        currentEvent = events[1];
        info(MarkerManager.getMarker("TEST"), "Hello");
        currentEvent = events[2];
        info("Hello", t);
        info((Marker) null, "Hello", t);
        currentEvent = events[3];
        info(MarkerManager.getMarker("TEST"), "Hello", t);
        currentEvent = events[4];
        info(obj);
        currentEvent = events[5];
        info(MarkerManager.getMarker("TEST"), obj);
        currentEvent = events[6];
        info(obj, t);
        info((Marker) null, obj, t);
        currentEvent = events[7];
        info(MarkerManager.getMarker("TEST"), obj, t);
        currentEvent = events[8];
        info(pattern, p1, p2);
        currentEvent = events[9];
        info(MarkerManager.getMarker("TEST"), pattern, p1, p2);
        currentEvent = events[10];
        info(simple);
        info((Marker) null, simple);
        info((Marker) null, simple, null);
        currentEvent = events[11];
        info(simple, t);
        info((Marker) null, simple, t);
        currentEvent = events[12];
        info(MarkerManager.getMarker("TEST"), simple, null);
        currentEvent = events[13];
        info(MarkerManager.getMarker("TEST"), simple, t);
        currentEvent = events[14];
        info(MarkerManager.getMarker("TEST"), simple);

        currentEvent = events[15];
        info(CHAR_SEQ);
        currentEvent = events[16];
        info(CHAR_SEQ, t);
        currentEvent = events[17];
        info(MarkerManager.getMarker("TEST"), CHAR_SEQ);
        currentEvent = events[18];
        info(MarkerManager.getMarker("TEST"), CHAR_SEQ, t);

        assertEquals("log(CharSeq) invocations", 4, charSeqCount);
        assertEquals("log(Object) invocations", 5, objectCount);
    }

    @Test
    public void testLogDebug() {
        currentLevel = Level.DEBUG;

        currentEvent = events[0];
        log(Level.DEBUG, "Hello");
        log(Level.DEBUG, (Marker) null, "Hello");
        currentEvent = events[1];
        log(Level.DEBUG, MarkerManager.getMarker("TEST"), "Hello");
        currentEvent = events[2];
        log(Level.DEBUG, "Hello", t);
        log(Level.DEBUG, (Marker) null, "Hello", t);
        currentEvent = events[3];
        log(Level.DEBUG, MarkerManager.getMarker("TEST"), "Hello", t);
        currentEvent = events[4];
        log(Level.DEBUG, obj);
        currentEvent = events[5];
        log(Level.DEBUG, MarkerManager.getMarker("TEST"), obj);
        currentEvent = events[6];
        log(Level.DEBUG, obj, t);
        log(Level.DEBUG, (Marker) null, obj, t);
        currentEvent = events[7];
        log(Level.DEBUG, MarkerManager.getMarker("TEST"), obj, t);
        currentEvent = events[8];
        log(Level.DEBUG, pattern, p1, p2);
        currentEvent = events[9];
        log(Level.DEBUG, MarkerManager.getMarker("TEST"), pattern, p1, p2);
        currentEvent = events[10];
        log(Level.DEBUG, simple);
        log(Level.DEBUG, (Marker) null, simple);
        log(Level.DEBUG, (Marker) null, simple, null);
        currentEvent = events[11];
        log(Level.DEBUG, simple, t);
        log(Level.DEBUG, (Marker) null, simple, t);
        currentEvent = events[12];
        log(Level.DEBUG, MarkerManager.getMarker("TEST"), simple, null);
        currentEvent = events[13];
        log(Level.DEBUG, MarkerManager.getMarker("TEST"), simple, t);
        currentEvent = events[14];
        log(Level.DEBUG, MarkerManager.getMarker("TEST"), simple);

        currentEvent = events[15];
        log(Level.DEBUG, CHAR_SEQ);
        currentEvent = events[16];
        log(Level.DEBUG, CHAR_SEQ, t);
        currentEvent = events[17];
        log(Level.DEBUG, MarkerManager.getMarker("TEST"), CHAR_SEQ);
        currentEvent = events[18];
        log(Level.DEBUG, MarkerManager.getMarker("TEST"), CHAR_SEQ, t);

        assertEquals("log(CharSeq) invocations", 4, charSeqCount);
        assertEquals("log(Object) invocations", 5, objectCount);
    }

    @Test
    public void testLogError() {
        currentLevel = Level.ERROR;

        currentEvent = events[0];
        log(Level.ERROR, "Hello");
        log(Level.ERROR, (Marker) null, "Hello");
        currentEvent = events[1];
        log(Level.ERROR, MarkerManager.getMarker("TEST"), "Hello");
        currentEvent = events[2];
        log(Level.ERROR, "Hello", t);
        log(Level.ERROR, (Marker) null, "Hello", t);
        currentEvent = events[3];
        log(Level.ERROR, MarkerManager.getMarker("TEST"), "Hello", t);
        currentEvent = events[4];
        log(Level.ERROR, obj);
        currentEvent = events[5];
        log(Level.ERROR, MarkerManager.getMarker("TEST"), obj);
        currentEvent = events[6];
        log(Level.ERROR, obj, t);
        log(Level.ERROR, (Marker) null, obj, t);
        currentEvent = events[7];
        log(Level.ERROR, MarkerManager.getMarker("TEST"), obj, t);
        currentEvent = events[8];
        log(Level.ERROR, pattern, p1, p2);
        currentEvent = events[9];
        log(Level.ERROR, MarkerManager.getMarker("TEST"), pattern, p1, p2);
        currentEvent = events[10];
        log(Level.ERROR, simple);
        log(Level.ERROR, (Marker) null, simple);
        log(Level.ERROR, (Marker) null, simple, null);
        currentEvent = events[11];
        log(Level.ERROR, simple, t);
        log(Level.ERROR, (Marker) null, simple, t);
        currentEvent = events[12];
        log(Level.ERROR, MarkerManager.getMarker("TEST"), simple, null);
        currentEvent = events[13];
        log(Level.ERROR, MarkerManager.getMarker("TEST"), simple, t);
        currentEvent = events[14];
        log(Level.ERROR, MarkerManager.getMarker("TEST"), simple);

        currentEvent = events[15];
        log(Level.ERROR, CHAR_SEQ);
        currentEvent = events[16];
        log(Level.ERROR, CHAR_SEQ, t);
        currentEvent = events[17];
        log(Level.ERROR, MarkerManager.getMarker("TEST"), CHAR_SEQ);
        currentEvent = events[18];
        log(Level.ERROR, MarkerManager.getMarker("TEST"), CHAR_SEQ, t);

        assertEquals("log(CharSeq) invocations", 4, charSeqCount);
        assertEquals("log(Object) invocations", 5, objectCount);
    }

    @Test
    public void testLogFatal() {
        currentLevel = Level.FATAL;

        currentEvent = events[0];
        log(Level.FATAL, "Hello");
        log(Level.FATAL, (Marker) null, "Hello");
        currentEvent = events[1];
        log(Level.FATAL, MarkerManager.getMarker("TEST"), "Hello");
        currentEvent = events[2];
        log(Level.FATAL, "Hello", t);
        log(Level.FATAL, (Marker) null, "Hello", t);
        currentEvent = events[3];
        log(Level.FATAL, MarkerManager.getMarker("TEST"), "Hello", t);
        currentEvent = events[4];
        log(Level.FATAL, obj);
        currentEvent = events[5];
        log(Level.FATAL, MarkerManager.getMarker("TEST"), obj);
        currentEvent = events[6];
        log(Level.FATAL, obj, t);
        log(Level.FATAL, (Marker) null, obj, t);
        currentEvent = events[7];
        log(Level.FATAL, MarkerManager.getMarker("TEST"), obj, t);
        currentEvent = events[8];
        log(Level.FATAL, pattern, p1, p2);
        currentEvent = events[9];
        log(Level.FATAL, MarkerManager.getMarker("TEST"), pattern, p1, p2);
        currentEvent = events[10];
        log(Level.FATAL, simple);
        log(Level.FATAL, (Marker) null, simple);
        log(Level.FATAL, (Marker) null, simple, null);
        currentEvent = events[11];
        log(Level.FATAL, simple, t);
        log(Level.FATAL, (Marker) null, simple, t);
        currentEvent = events[12];
        log(Level.FATAL, MarkerManager.getMarker("TEST"), simple, null);
        currentEvent = events[13];
        log(Level.FATAL, MarkerManager.getMarker("TEST"), simple, t);
        currentEvent = events[14];
        log(Level.FATAL, MarkerManager.getMarker("TEST"), simple);

        currentEvent = events[15];
        log(Level.FATAL, CHAR_SEQ);
        currentEvent = events[16];
        log(Level.FATAL, CHAR_SEQ, t);
        currentEvent = events[17];
        log(Level.FATAL, MarkerManager.getMarker("TEST"), CHAR_SEQ);
        currentEvent = events[18];
        log(Level.FATAL, MarkerManager.getMarker("TEST"), CHAR_SEQ, t);

        assertEquals("log(CharSeq) invocations", 4, charSeqCount);
        assertEquals("log(Object) invocations", 5, objectCount);
    }

    @Test
    public void testLogInfo() {
        currentLevel = Level.INFO;

        currentEvent = events[0];
        log(Level.INFO, "Hello");
        log(Level.INFO, (Marker) null, "Hello");
        currentEvent = events[1];
        log(Level.INFO, MarkerManager.getMarker("TEST"), "Hello");
        currentEvent = events[2];
        log(Level.INFO, "Hello", t);
        log(Level.INFO, (Marker) null, "Hello", t);
        currentEvent = events[3];
        log(Level.INFO, MarkerManager.getMarker("TEST"), "Hello", t);
        currentEvent = events[4];
        log(Level.INFO, obj);
        currentEvent = events[5];
        log(Level.INFO, MarkerManager.getMarker("TEST"), obj);
        currentEvent = events[6];
        log(Level.INFO, obj, t);
        log(Level.INFO, (Marker) null, obj, t);
        currentEvent = events[7];
        log(Level.INFO, MarkerManager.getMarker("TEST"), obj, t);
        currentEvent = events[8];
        log(Level.INFO, pattern, p1, p2);
        currentEvent = events[9];
        log(Level.INFO, MarkerManager.getMarker("TEST"), pattern, p1, p2);
        currentEvent = events[10];
        log(Level.INFO, simple);
        log(Level.INFO, (Marker) null, simple);
        log(Level.INFO, (Marker) null, simple, null);
        currentEvent = events[11];
        log(Level.INFO, simple, t);
        log(Level.INFO, (Marker) null, simple, t);
        currentEvent = events[12];
        log(Level.INFO, MarkerManager.getMarker("TEST"), simple, null);
        currentEvent = events[13];
        log(Level.INFO, MarkerManager.getMarker("TEST"), simple, t);
        currentEvent = events[14];
        log(Level.INFO, MarkerManager.getMarker("TEST"), simple);

        currentEvent = events[15];
        log(Level.INFO, CHAR_SEQ);
        currentEvent = events[16];
        log(Level.INFO, CHAR_SEQ, t);
        currentEvent = events[17];
        log(Level.INFO, MarkerManager.getMarker("TEST"), CHAR_SEQ);
        currentEvent = events[18];
        log(Level.INFO, MarkerManager.getMarker("TEST"), CHAR_SEQ, t);

        assertEquals("log(CharSeq) invocations", 4, charSeqCount);
        assertEquals("log(Object) invocations", 5, objectCount);
    }

    @Test
    public void testLogTrace() {
        currentLevel = Level.TRACE;

        currentEvent = events[0];
        log(Level.TRACE, "Hello");
        log(Level.TRACE, (Marker) null, "Hello");
        currentEvent = events[1];
        log(Level.TRACE, MarkerManager.getMarker("TEST"), "Hello");
        currentEvent = events[2];
        log(Level.TRACE, "Hello", t);
        log(Level.TRACE, (Marker) null, "Hello", t);
        currentEvent = events[3];
        log(Level.TRACE, MarkerManager.getMarker("TEST"), "Hello", t);
        currentEvent = events[4];
        log(Level.TRACE, obj);
        currentEvent = events[5];
        log(Level.TRACE, MarkerManager.getMarker("TEST"), obj);
        currentEvent = events[6];
        log(Level.TRACE, obj, t);
        log(Level.TRACE, (Marker) null, obj, t);
        currentEvent = events[7];
        log(Level.TRACE, MarkerManager.getMarker("TEST"), obj, t);
        currentEvent = events[8];
        log(Level.TRACE, pattern, p1, p2);
        currentEvent = events[9];
        log(Level.TRACE, MarkerManager.getMarker("TEST"), pattern, p1, p2);
        currentEvent = events[10];
        log(Level.TRACE, simple);
        log(Level.TRACE, (Marker) null, simple);
        log(Level.TRACE, (Marker) null, simple, null);
        currentEvent = events[11];
        log(Level.TRACE, simple, t);
        log(Level.TRACE, (Marker) null, simple, t);
        currentEvent = events[12];
        log(Level.TRACE, MarkerManager.getMarker("TEST"), simple, null);
        currentEvent = events[13];
        log(Level.TRACE, MarkerManager.getMarker("TEST"), simple, t);
        currentEvent = events[14];
        log(Level.TRACE, MarkerManager.getMarker("TEST"), simple);

        currentEvent = events[15];
        log(Level.TRACE, CHAR_SEQ);
        currentEvent = events[16];
        log(Level.TRACE, CHAR_SEQ, t);
        currentEvent = events[17];
        log(Level.TRACE, MarkerManager.getMarker("TEST"), CHAR_SEQ);
        currentEvent = events[18];
        log(Level.TRACE, MarkerManager.getMarker("TEST"), CHAR_SEQ, t);

        assertEquals("log(CharSeq) invocations", 4, charSeqCount);
        assertEquals("log(Object) invocations", 5, objectCount);
    }

    @Test
    public void testLogWarn() {
        currentLevel = Level.WARN;

        currentEvent = events[0];
        log(Level.WARN, "Hello");
        log(Level.WARN, (Marker) null, "Hello");
        currentEvent = events[1];
        log(Level.WARN, MarkerManager.getMarker("TEST"), "Hello");
        currentEvent = events[2];
        log(Level.WARN, "Hello", t);
        log(Level.WARN, (Marker) null, "Hello", t);
        currentEvent = events[3];
        log(Level.WARN, MarkerManager.getMarker("TEST"), "Hello", t);
        currentEvent = events[4];
        log(Level.WARN, obj);
        currentEvent = events[5];
        log(Level.WARN, MarkerManager.getMarker("TEST"), obj);
        currentEvent = events[6];
        log(Level.WARN, obj, t);
        log(Level.WARN, (Marker) null, obj, t);
        currentEvent = events[7];
        log(Level.WARN, MarkerManager.getMarker("TEST"), obj, t);
        currentEvent = events[8];
        log(Level.WARN, pattern, p1, p2);
        currentEvent = events[9];
        log(Level.WARN, MarkerManager.getMarker("TEST"), pattern, p1, p2);
        currentEvent = events[10];
        log(Level.WARN, simple);
        log(Level.WARN, (Marker) null, simple);
        log(Level.WARN, (Marker) null, simple, null);
        currentEvent = events[11];
        log(Level.WARN, simple, t);
        log(Level.WARN, (Marker) null, simple, t);
        currentEvent = events[12];
        log(Level.WARN, MarkerManager.getMarker("TEST"), simple, null);
        currentEvent = events[13];
        log(Level.WARN, MarkerManager.getMarker("TEST"), simple, t);
        currentEvent = events[14];
        log(Level.WARN, MarkerManager.getMarker("TEST"), simple);

        currentEvent = events[15];
        log(Level.WARN, CHAR_SEQ);
        currentEvent = events[16];
        log(Level.WARN, CHAR_SEQ, t);
        currentEvent = events[17];
        log(Level.WARN, MarkerManager.getMarker("TEST"), CHAR_SEQ);
        currentEvent = events[18];
        log(Level.WARN, MarkerManager.getMarker("TEST"), CHAR_SEQ, t);

        assertEquals("log(CharSeq) invocations", 4, charSeqCount);
        assertEquals("log(Object) invocations", 5, objectCount);
    }

    @Test
    public void testTrace() {
        currentLevel = Level.TRACE;

        currentEvent = events[0];
        trace("Hello");
        trace((Marker) null, "Hello");
        currentEvent = events[1];
        trace(MarkerManager.getMarker("TEST"), "Hello");
        currentEvent = events[2];
        trace("Hello", t);
        trace((Marker) null, "Hello", t);
        currentEvent = events[3];
        trace(MarkerManager.getMarker("TEST"), "Hello", t);
        currentEvent = events[4];
        trace(obj);
        currentEvent = events[5];
        trace(MarkerManager.getMarker("TEST"), obj);
        currentEvent = events[6];
        trace(obj, t);
        trace((Marker) null, obj, t);
        currentEvent = events[7];
        trace(MarkerManager.getMarker("TEST"), obj, t);
        currentEvent = events[8];
        trace(pattern, p1, p2);
        currentEvent = events[9];
        trace(MarkerManager.getMarker("TEST"), pattern, p1, p2);
        currentEvent = events[10];
        trace(simple);
        trace((Marker) null, simple);
        trace((Marker) null, simple, null);
        currentEvent = events[11];
        trace(simple, t);
        trace((Marker) null, simple, t);
        currentEvent = events[12];
        trace(MarkerManager.getMarker("TEST"), simple, null);
        currentEvent = events[13];
        trace(MarkerManager.getMarker("TEST"), simple, t);
        currentEvent = events[14];
        trace(MarkerManager.getMarker("TEST"), simple);

        currentEvent = events[15];
        trace(CHAR_SEQ);
        currentEvent = events[16];
        trace(CHAR_SEQ, t);
        currentEvent = events[17];
        trace(MarkerManager.getMarker("TEST"), CHAR_SEQ);
        currentEvent = events[18];
        trace(MarkerManager.getMarker("TEST"), CHAR_SEQ, t);

        assertEquals("log(CharSeq) invocations", 4, charSeqCount);
        assertEquals("log(Object) invocations", 5, objectCount);
    }

    @Test
    public void testWarn() {
        currentLevel = Level.WARN;

        currentEvent = events[0];
        warn("Hello");
        warn((Marker) null, "Hello");
        currentEvent = events[1];
        warn(MarkerManager.getMarker("TEST"), "Hello");
        currentEvent = events[2];
        warn("Hello", t);
        warn((Marker) null, "Hello", t);
        currentEvent = events[3];
        warn(MarkerManager.getMarker("TEST"), "Hello", t);
        currentEvent = events[4];
        warn(obj);
        currentEvent = events[5];
        warn(MarkerManager.getMarker("TEST"), obj);
        currentEvent = events[6];
        warn(obj, t);
        warn((Marker) null, obj, t);
        currentEvent = events[7];
        warn(MarkerManager.getMarker("TEST"), obj, t);
        currentEvent = events[8];
        warn(pattern, p1, p2);
        currentEvent = events[9];
        warn(MarkerManager.getMarker("TEST"), pattern, p1, p2);
        currentEvent = events[10];
        warn(simple);
        warn((Marker) null, simple);
        warn((Marker) null, simple, null);
        currentEvent = events[11];
        warn(simple, t);
        warn((Marker) null, simple, t);
        currentEvent = events[12];
        warn(MarkerManager.getMarker("TEST"), simple, null);
        currentEvent = events[13];
        warn(MarkerManager.getMarker("TEST"), simple, t);
        currentEvent = events[14];
        warn(MarkerManager.getMarker("TEST"), simple);

        currentEvent = events[15];
        warn(CHAR_SEQ);
        currentEvent = events[16];
        warn(CHAR_SEQ, t);
        currentEvent = events[17];
        warn(MarkerManager.getMarker("TEST"), CHAR_SEQ);
        currentEvent = events[18];
        warn(MarkerManager.getMarker("TEST"), CHAR_SEQ, t);

        assertEquals("log(CharSeq) invocations", 4, charSeqCount);
        assertEquals("log(Object) invocations", 5, objectCount);
    }

}
