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
package org.apache.logging.log4j;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.message.ParameterizedMessageFactory;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.spi.MessageFactory2Adapter;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.junit.Log4jStaticResources;
import org.apache.logging.log4j.test.junit.StatusLoggerLevel;
import org.apache.logging.log4j.util.Constants;
import org.apache.logging.log4j.util.MessageSupplier;
import org.apache.logging.log4j.util.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junitpioneer.jupiter.SetSystemProperty;

@StatusLoggerLevel("WARN")
@ResourceLock(value = Log4jStaticResources.MARKER_MANAGER, mode = ResourceAccessMode.READ)
@SetSystemProperty(key = "log4j2.status.entries", value = "200")
@SetSystemProperty(key = "log4j2.StatusLogger.level", value = "WARN")
class AbstractLoggerTest {

    private static final StringBuilder CHAR_SEQ = new StringBuilder("CharSeq");

    // TODO add proper tests for ReusableMessage

    @SuppressWarnings("ThrowableInstanceNeverThrown")
    private static final Throwable t = new UnsupportedOperationException("Test");

    private static final Class<AbstractLogger> obj = AbstractLogger.class;
    private static final String pattern = "{}, {}";
    private static final String p1 = "Long Beach";

    private static final String p2 = "California";
    private static final Message charSeq = new SimpleMessage(CHAR_SEQ);
    private static final Message simple = new SimpleMessage("Hello");
    private static final Message object = new ObjectMessage(obj);

    private static final Message param = new ParameterizedMessage(pattern, p1, p2);

    private final Marker MARKER = MarkerManager.getMarker("TEST");
    private static final String MARKER_NAME = "TEST";

    private static final LogEvent[] EVENTS = new LogEvent[] {
        new LogEvent(null, simple, null),
        new LogEvent(MARKER_NAME, simple, null),
        new LogEvent(null, simple, t),
        new LogEvent(MARKER_NAME, simple, t),
        new LogEvent(null, object, null),
        new LogEvent(MARKER_NAME, object, null),
        new LogEvent(null, object, t),
        new LogEvent(MARKER_NAME, object, t),
        new LogEvent(null, param, null),
        new LogEvent(MARKER_NAME, param, null),
        new LogEvent(null, simple, null),
        new LogEvent(null, simple, t),
        new LogEvent(MARKER_NAME, simple, null),
        new LogEvent(MARKER_NAME, simple, t),
        new LogEvent(MARKER_NAME, simple, null),
        new LogEvent(null, charSeq, null),
        new LogEvent(null, charSeq, t),
        new LogEvent(MARKER_NAME, charSeq, null),
        new LogEvent(MARKER_NAME, charSeq, t),
    };

    @Test
    void testDebug() {
        final CountingLogger logger = new CountingLogger();
        logger.setCurrentLevel(Level.DEBUG);

        logger.setCurrentEvent(EVENTS[0]);
        logger.debug("Hello");
        logger.debug((Marker) null, "Hello");
        logger.setCurrentEvent(EVENTS[1]);
        logger.debug(MARKER, "Hello");
        logger.setCurrentEvent(EVENTS[2]);
        logger.debug("Hello", t);
        logger.debug((Marker) null, "Hello", t);
        logger.setCurrentEvent(EVENTS[3]);
        logger.debug(MARKER, "Hello", t);
        logger.setCurrentEvent(EVENTS[4]);
        logger.debug(obj);
        logger.setCurrentEvent(EVENTS[5]);
        logger.debug(MARKER, obj);
        logger.setCurrentEvent(EVENTS[6]);
        logger.debug(obj, t);
        logger.debug((Marker) null, obj, t);
        logger.setCurrentEvent(EVENTS[7]);
        logger.debug(MARKER, obj, t);
        logger.setCurrentEvent(EVENTS[8]);
        logger.debug(pattern, p1, p2);
        logger.setCurrentEvent(EVENTS[9]);
        logger.debug(MARKER, pattern, p1, p2);
        logger.setCurrentEvent(EVENTS[10]);
        logger.debug(simple);
        logger.debug((Marker) null, simple);
        logger.debug((Marker) null, simple, null);
        logger.setCurrentEvent(EVENTS[11]);
        logger.debug(simple, t);
        logger.debug((Marker) null, simple, t);
        logger.setCurrentEvent(EVENTS[12]);
        logger.debug(MARKER, simple, null);
        logger.setCurrentEvent(EVENTS[13]);
        logger.debug(MARKER, simple, t);
        logger.setCurrentEvent(EVENTS[14]);
        logger.debug(MARKER, simple);

        logger.setCurrentEvent(EVENTS[15]);
        logger.debug(CHAR_SEQ);
        logger.setCurrentEvent(EVENTS[16]);
        logger.debug(CHAR_SEQ, t);
        logger.setCurrentEvent(EVENTS[17]);
        logger.debug(MARKER, CHAR_SEQ);
        logger.setCurrentEvent(EVENTS[18]);
        logger.debug(MARKER, CHAR_SEQ, t);

        assertEquals(4, logger.getCharSeqCount(), "log(CharSeq) invocations");
        assertEquals(5, logger.getObjectCount(), "log(Object) invocations");
    }

    @Test
    void testError() {
        final CountingLogger logger = new CountingLogger();
        logger.setCurrentLevel(Level.ERROR);

        logger.setCurrentEvent(EVENTS[0]);
        logger.error("Hello");
        logger.error((Marker) null, "Hello");
        logger.setCurrentEvent(EVENTS[1]);
        logger.error(MARKER, "Hello");
        logger.setCurrentEvent(EVENTS[2]);
        logger.error("Hello", t);
        logger.error((Marker) null, "Hello", t);
        logger.setCurrentEvent(EVENTS[3]);
        logger.error(MARKER, "Hello", t);
        logger.setCurrentEvent(EVENTS[4]);
        logger.error(obj);
        logger.setCurrentEvent(EVENTS[5]);
        logger.error(MARKER, obj);
        logger.setCurrentEvent(EVENTS[6]);
        logger.error(obj, t);
        logger.error((Marker) null, obj, t);
        logger.setCurrentEvent(EVENTS[7]);
        logger.error(MARKER, obj, t);
        logger.setCurrentEvent(EVENTS[8]);
        logger.error(pattern, p1, p2);
        logger.setCurrentEvent(EVENTS[9]);
        logger.error(MARKER, pattern, p1, p2);
        logger.setCurrentEvent(EVENTS[10]);
        logger.error(simple);
        logger.error((Marker) null, simple);
        logger.error((Marker) null, simple, null);
        logger.setCurrentEvent(EVENTS[11]);
        logger.error(simple, t);
        logger.error((Marker) null, simple, t);
        logger.setCurrentEvent(EVENTS[12]);
        logger.error(MARKER, simple, null);
        logger.setCurrentEvent(EVENTS[13]);
        logger.error(MARKER, simple, t);
        logger.setCurrentEvent(EVENTS[14]);
        logger.error(MARKER, simple);

        logger.setCurrentEvent(EVENTS[15]);
        logger.error(CHAR_SEQ);
        logger.setCurrentEvent(EVENTS[16]);
        logger.error(CHAR_SEQ, t);
        logger.setCurrentEvent(EVENTS[17]);
        logger.error(MARKER, CHAR_SEQ);
        logger.setCurrentEvent(EVENTS[18]);
        logger.error(MARKER, CHAR_SEQ, t);

        assertEquals(4, logger.getCharSeqCount(), "log(CharSeq) invocations");
        assertEquals(5, logger.getObjectCount(), "log(Object) invocations");
    }

    @Test
    void testFatal() {
        final CountingLogger logger = new CountingLogger();
        logger.setCurrentLevel(Level.FATAL);

        logger.setCurrentEvent(EVENTS[0]);
        logger.fatal("Hello");
        logger.fatal((Marker) null, "Hello");
        logger.setCurrentEvent(EVENTS[1]);
        logger.fatal(MARKER, "Hello");
        logger.setCurrentEvent(EVENTS[2]);
        logger.fatal("Hello", t);
        logger.fatal((Marker) null, "Hello", t);
        logger.setCurrentEvent(EVENTS[3]);
        logger.fatal(MARKER, "Hello", t);
        logger.setCurrentEvent(EVENTS[4]);
        logger.fatal(obj);
        logger.setCurrentEvent(EVENTS[5]);
        logger.fatal(MARKER, obj);
        logger.setCurrentEvent(EVENTS[6]);
        logger.fatal(obj, t);
        logger.fatal((Marker) null, obj, t);
        logger.setCurrentEvent(EVENTS[7]);
        logger.fatal(MARKER, obj, t);
        logger.setCurrentEvent(EVENTS[8]);
        logger.fatal(pattern, p1, p2);
        logger.setCurrentEvent(EVENTS[9]);
        logger.fatal(MARKER, pattern, p1, p2);
        logger.setCurrentEvent(EVENTS[10]);
        logger.fatal(simple);
        logger.fatal((Marker) null, simple);
        logger.fatal((Marker) null, simple, null);
        logger.setCurrentEvent(EVENTS[11]);
        logger.fatal(simple, t);
        logger.fatal((Marker) null, simple, t);
        logger.setCurrentEvent(EVENTS[12]);
        logger.fatal(MARKER, simple, null);
        logger.setCurrentEvent(EVENTS[13]);
        logger.fatal(MARKER, simple, t);
        logger.setCurrentEvent(EVENTS[14]);
        logger.fatal(MARKER, simple);

        logger.setCurrentEvent(EVENTS[15]);
        logger.fatal(CHAR_SEQ);
        logger.setCurrentEvent(EVENTS[16]);
        logger.fatal(CHAR_SEQ, t);
        logger.setCurrentEvent(EVENTS[17]);
        logger.fatal(MARKER, CHAR_SEQ);
        logger.setCurrentEvent(EVENTS[18]);
        logger.fatal(MARKER, CHAR_SEQ, t);

        assertEquals(4, logger.getCharSeqCount(), "log(CharSeq) invocations");
        assertEquals(5, logger.getObjectCount(), "log(Object) invocations");
    }

    @Test
    void testInfo() {
        final CountingLogger logger = new CountingLogger();
        logger.setCurrentLevel(Level.INFO);

        logger.setCurrentEvent(EVENTS[0]);
        logger.info("Hello");
        logger.info((Marker) null, "Hello");
        logger.setCurrentEvent(EVENTS[1]);
        logger.info(MARKER, "Hello");
        logger.setCurrentEvent(EVENTS[2]);
        logger.info("Hello", t);
        logger.info((Marker) null, "Hello", t);
        logger.setCurrentEvent(EVENTS[3]);
        logger.info(MARKER, "Hello", t);
        logger.setCurrentEvent(EVENTS[4]);
        logger.info(obj);
        logger.setCurrentEvent(EVENTS[5]);
        logger.info(MARKER, obj);
        logger.setCurrentEvent(EVENTS[6]);
        logger.info(obj, t);
        logger.info((Marker) null, obj, t);
        logger.setCurrentEvent(EVENTS[7]);
        logger.info(MARKER, obj, t);
        logger.setCurrentEvent(EVENTS[8]);
        logger.info(pattern, p1, p2);
        logger.setCurrentEvent(EVENTS[9]);
        logger.info(MARKER, pattern, p1, p2);
        logger.setCurrentEvent(EVENTS[10]);
        logger.info(simple);
        logger.info((Marker) null, simple);
        logger.info((Marker) null, simple, null);
        logger.setCurrentEvent(EVENTS[11]);
        logger.info(simple, t);
        logger.info((Marker) null, simple, t);
        logger.setCurrentEvent(EVENTS[12]);
        logger.info(MARKER, simple, null);
        logger.setCurrentEvent(EVENTS[13]);
        logger.info(MARKER, simple, t);
        logger.setCurrentEvent(EVENTS[14]);
        logger.info(MARKER, simple);

        logger.setCurrentEvent(EVENTS[15]);
        logger.info(CHAR_SEQ);
        logger.setCurrentEvent(EVENTS[16]);
        logger.info(CHAR_SEQ, t);
        logger.setCurrentEvent(EVENTS[17]);
        logger.info(MARKER, CHAR_SEQ);
        logger.setCurrentEvent(EVENTS[18]);
        logger.info(MARKER, CHAR_SEQ, t);

        assertEquals(4, logger.getCharSeqCount(), "log(CharSeq) invocations");
        assertEquals(5, logger.getObjectCount(), "log(Object) invocations");
    }

    @Test
    void testLogDebug() {
        final CountingLogger logger = new CountingLogger();
        logger.setCurrentLevel(Level.DEBUG);

        logger.setCurrentEvent(EVENTS[0]);
        logger.log(Level.DEBUG, "Hello");
        logger.log(Level.DEBUG, (Marker) null, "Hello");
        logger.setCurrentEvent(EVENTS[1]);
        logger.log(Level.DEBUG, MARKER, "Hello");
        logger.setCurrentEvent(EVENTS[2]);
        logger.log(Level.DEBUG, "Hello", t);
        logger.log(Level.DEBUG, (Marker) null, "Hello", t);
        logger.setCurrentEvent(EVENTS[3]);
        logger.log(Level.DEBUG, MARKER, "Hello", t);
        logger.setCurrentEvent(EVENTS[4]);
        logger.log(Level.DEBUG, obj);
        logger.setCurrentEvent(EVENTS[5]);
        logger.log(Level.DEBUG, MARKER, obj);
        logger.setCurrentEvent(EVENTS[6]);
        logger.log(Level.DEBUG, obj, t);
        logger.log(Level.DEBUG, (Marker) null, obj, t);
        logger.setCurrentEvent(EVENTS[7]);
        logger.log(Level.DEBUG, MARKER, obj, t);
        logger.setCurrentEvent(EVENTS[8]);
        logger.log(Level.DEBUG, pattern, p1, p2);
        logger.setCurrentEvent(EVENTS[9]);
        logger.log(Level.DEBUG, MARKER, pattern, p1, p2);
        logger.setCurrentEvent(EVENTS[10]);
        logger.log(Level.DEBUG, simple);
        logger.log(Level.DEBUG, (Marker) null, simple);
        logger.log(Level.DEBUG, (Marker) null, simple, null);
        logger.setCurrentEvent(EVENTS[11]);
        logger.log(Level.DEBUG, simple, t);
        logger.log(Level.DEBUG, (Marker) null, simple, t);
        logger.setCurrentEvent(EVENTS[12]);
        logger.log(Level.DEBUG, MARKER, simple, null);
        logger.setCurrentEvent(EVENTS[13]);
        logger.log(Level.DEBUG, MARKER, simple, t);
        logger.setCurrentEvent(EVENTS[14]);
        logger.log(Level.DEBUG, MARKER, simple);

        logger.setCurrentEvent(EVENTS[15]);
        logger.log(Level.DEBUG, CHAR_SEQ);
        logger.setCurrentEvent(EVENTS[16]);
        logger.log(Level.DEBUG, CHAR_SEQ, t);
        logger.setCurrentEvent(EVENTS[17]);
        logger.log(Level.DEBUG, MARKER, CHAR_SEQ);
        logger.setCurrentEvent(EVENTS[18]);
        logger.log(Level.DEBUG, MARKER, CHAR_SEQ, t);

        assertEquals(4, logger.getCharSeqCount(), "log(CharSeq) invocations");
        assertEquals(5, logger.getObjectCount(), "log(Object) invocations");
    }

    @Test
    void testLogError() {
        final CountingLogger logger = new CountingLogger();
        logger.setCurrentLevel(Level.ERROR);

        logger.setCurrentEvent(EVENTS[0]);
        logger.log(Level.ERROR, "Hello");
        logger.log(Level.ERROR, (Marker) null, "Hello");
        logger.setCurrentEvent(EVENTS[1]);
        logger.log(Level.ERROR, MARKER, "Hello");
        logger.setCurrentEvent(EVENTS[2]);
        logger.log(Level.ERROR, "Hello", t);
        logger.log(Level.ERROR, (Marker) null, "Hello", t);
        logger.setCurrentEvent(EVENTS[3]);
        logger.log(Level.ERROR, MARKER, "Hello", t);
        logger.setCurrentEvent(EVENTS[4]);
        logger.log(Level.ERROR, obj);
        logger.setCurrentEvent(EVENTS[5]);
        logger.log(Level.ERROR, MARKER, obj);
        logger.setCurrentEvent(EVENTS[6]);
        logger.log(Level.ERROR, obj, t);
        logger.log(Level.ERROR, (Marker) null, obj, t);
        logger.setCurrentEvent(EVENTS[7]);
        logger.log(Level.ERROR, MARKER, obj, t);
        logger.setCurrentEvent(EVENTS[8]);
        logger.log(Level.ERROR, pattern, p1, p2);
        logger.setCurrentEvent(EVENTS[9]);
        logger.log(Level.ERROR, MARKER, pattern, p1, p2);
        logger.setCurrentEvent(EVENTS[10]);
        logger.log(Level.ERROR, simple);
        logger.log(Level.ERROR, (Marker) null, simple);
        logger.log(Level.ERROR, (Marker) null, simple, null);
        logger.setCurrentEvent(EVENTS[11]);
        logger.log(Level.ERROR, simple, t);
        logger.log(Level.ERROR, (Marker) null, simple, t);
        logger.setCurrentEvent(EVENTS[12]);
        logger.log(Level.ERROR, MARKER, simple, null);
        logger.setCurrentEvent(EVENTS[13]);
        logger.log(Level.ERROR, MARKER, simple, t);
        logger.setCurrentEvent(EVENTS[14]);
        logger.log(Level.ERROR, MARKER, simple);

        logger.setCurrentEvent(EVENTS[15]);
        logger.log(Level.ERROR, CHAR_SEQ);
        logger.setCurrentEvent(EVENTS[16]);
        logger.log(Level.ERROR, CHAR_SEQ, t);
        logger.setCurrentEvent(EVENTS[17]);
        logger.log(Level.ERROR, MARKER, CHAR_SEQ);
        logger.setCurrentEvent(EVENTS[18]);
        logger.log(Level.ERROR, MARKER, CHAR_SEQ, t);

        assertEquals(4, logger.getCharSeqCount(), "log(CharSeq) invocations");
        assertEquals(5, logger.getObjectCount(), "log(Object) invocations");
    }

    @Test
    void testLogFatal() {
        final CountingLogger logger = new CountingLogger();
        logger.setCurrentLevel(Level.FATAL);

        logger.setCurrentEvent(EVENTS[0]);
        logger.log(Level.FATAL, "Hello");
        logger.log(Level.FATAL, (Marker) null, "Hello");
        logger.setCurrentEvent(EVENTS[1]);
        logger.log(Level.FATAL, MARKER, "Hello");
        logger.setCurrentEvent(EVENTS[2]);
        logger.log(Level.FATAL, "Hello", t);
        logger.log(Level.FATAL, (Marker) null, "Hello", t);
        logger.setCurrentEvent(EVENTS[3]);
        logger.log(Level.FATAL, MARKER, "Hello", t);
        logger.setCurrentEvent(EVENTS[4]);
        logger.log(Level.FATAL, obj);
        logger.setCurrentEvent(EVENTS[5]);
        logger.log(Level.FATAL, MARKER, obj);
        logger.setCurrentEvent(EVENTS[6]);
        logger.log(Level.FATAL, obj, t);
        logger.log(Level.FATAL, (Marker) null, obj, t);
        logger.setCurrentEvent(EVENTS[7]);
        logger.log(Level.FATAL, MARKER, obj, t);
        logger.setCurrentEvent(EVENTS[8]);
        logger.log(Level.FATAL, pattern, p1, p2);
        logger.setCurrentEvent(EVENTS[9]);
        logger.log(Level.FATAL, MARKER, pattern, p1, p2);
        logger.setCurrentEvent(EVENTS[10]);
        logger.log(Level.FATAL, simple);
        logger.log(Level.FATAL, (Marker) null, simple);
        logger.log(Level.FATAL, (Marker) null, simple, null);
        logger.setCurrentEvent(EVENTS[11]);
        logger.log(Level.FATAL, simple, t);
        logger.log(Level.FATAL, (Marker) null, simple, t);
        logger.setCurrentEvent(EVENTS[12]);
        logger.log(Level.FATAL, MARKER, simple, null);
        logger.setCurrentEvent(EVENTS[13]);
        logger.log(Level.FATAL, MARKER, simple, t);
        logger.setCurrentEvent(EVENTS[14]);
        logger.log(Level.FATAL, MARKER, simple);

        logger.setCurrentEvent(EVENTS[15]);
        logger.log(Level.FATAL, CHAR_SEQ);
        logger.setCurrentEvent(EVENTS[16]);
        logger.log(Level.FATAL, CHAR_SEQ, t);
        logger.setCurrentEvent(EVENTS[17]);
        logger.log(Level.FATAL, MARKER, CHAR_SEQ);
        logger.setCurrentEvent(EVENTS[18]);
        logger.log(Level.FATAL, MARKER, CHAR_SEQ, t);

        assertEquals(4, logger.getCharSeqCount(), "log(CharSeq) invocations");
        assertEquals(5, logger.getObjectCount(), "log(Object) invocations");
    }

    @Test
    void testLogInfo() {
        final CountingLogger logger = new CountingLogger();
        logger.setCurrentLevel(Level.INFO);

        logger.setCurrentEvent(EVENTS[0]);
        logger.log(Level.INFO, "Hello");
        logger.log(Level.INFO, (Marker) null, "Hello");
        logger.setCurrentEvent(EVENTS[1]);
        logger.log(Level.INFO, MARKER, "Hello");
        logger.setCurrentEvent(EVENTS[2]);
        logger.log(Level.INFO, "Hello", t);
        logger.log(Level.INFO, (Marker) null, "Hello", t);
        logger.setCurrentEvent(EVENTS[3]);
        logger.log(Level.INFO, MARKER, "Hello", t);
        logger.setCurrentEvent(EVENTS[4]);
        logger.log(Level.INFO, obj);
        logger.setCurrentEvent(EVENTS[5]);
        logger.log(Level.INFO, MARKER, obj);
        logger.setCurrentEvent(EVENTS[6]);
        logger.log(Level.INFO, obj, t);
        logger.log(Level.INFO, (Marker) null, obj, t);
        logger.setCurrentEvent(EVENTS[7]);
        logger.log(Level.INFO, MARKER, obj, t);
        logger.setCurrentEvent(EVENTS[8]);
        logger.log(Level.INFO, pattern, p1, p2);
        logger.setCurrentEvent(EVENTS[9]);
        logger.log(Level.INFO, MARKER, pattern, p1, p2);
        logger.setCurrentEvent(EVENTS[10]);
        logger.log(Level.INFO, simple);
        logger.log(Level.INFO, (Marker) null, simple);
        logger.log(Level.INFO, (Marker) null, simple, null);
        logger.setCurrentEvent(EVENTS[11]);
        logger.log(Level.INFO, simple, t);
        logger.log(Level.INFO, (Marker) null, simple, t);
        logger.setCurrentEvent(EVENTS[12]);
        logger.log(Level.INFO, MARKER, simple, null);
        logger.setCurrentEvent(EVENTS[13]);
        logger.log(Level.INFO, MARKER, simple, t);
        logger.setCurrentEvent(EVENTS[14]);
        logger.log(Level.INFO, MARKER, simple);

        logger.setCurrentEvent(EVENTS[15]);
        logger.log(Level.INFO, CHAR_SEQ);
        logger.setCurrentEvent(EVENTS[16]);
        logger.log(Level.INFO, CHAR_SEQ, t);
        logger.setCurrentEvent(EVENTS[17]);
        logger.log(Level.INFO, MARKER, CHAR_SEQ);
        logger.setCurrentEvent(EVENTS[18]);
        logger.log(Level.INFO, MARKER, CHAR_SEQ, t);

        assertEquals(4, logger.getCharSeqCount(), "log(CharSeq) invocations");
        assertEquals(5, logger.getObjectCount(), "log(Object) invocations");
    }

    @Test
    void testLogTrace() {
        final CountingLogger logger = new CountingLogger();
        logger.setCurrentLevel(Level.TRACE);

        logger.setCurrentEvent(EVENTS[0]);
        logger.log(Level.TRACE, "Hello");
        logger.log(Level.TRACE, (Marker) null, "Hello");
        logger.setCurrentEvent(EVENTS[1]);
        logger.log(Level.TRACE, MARKER, "Hello");
        logger.setCurrentEvent(EVENTS[2]);
        logger.log(Level.TRACE, "Hello", t);
        logger.log(Level.TRACE, (Marker) null, "Hello", t);
        logger.setCurrentEvent(EVENTS[3]);
        logger.log(Level.TRACE, MARKER, "Hello", t);
        logger.setCurrentEvent(EVENTS[4]);
        logger.log(Level.TRACE, obj);
        logger.setCurrentEvent(EVENTS[5]);
        logger.log(Level.TRACE, MARKER, obj);
        logger.setCurrentEvent(EVENTS[6]);
        logger.log(Level.TRACE, obj, t);
        logger.log(Level.TRACE, (Marker) null, obj, t);
        logger.setCurrentEvent(EVENTS[7]);
        logger.log(Level.TRACE, MARKER, obj, t);
        logger.setCurrentEvent(EVENTS[8]);
        logger.log(Level.TRACE, pattern, p1, p2);
        logger.setCurrentEvent(EVENTS[9]);
        logger.log(Level.TRACE, MARKER, pattern, p1, p2);
        logger.setCurrentEvent(EVENTS[10]);
        logger.log(Level.TRACE, simple);
        logger.log(Level.TRACE, (Marker) null, simple);
        logger.log(Level.TRACE, (Marker) null, simple, null);
        logger.setCurrentEvent(EVENTS[11]);
        logger.log(Level.TRACE, simple, t);
        logger.log(Level.TRACE, (Marker) null, simple, t);
        logger.setCurrentEvent(EVENTS[12]);
        logger.log(Level.TRACE, MARKER, simple, null);
        logger.setCurrentEvent(EVENTS[13]);
        logger.log(Level.TRACE, MARKER, simple, t);
        logger.setCurrentEvent(EVENTS[14]);
        logger.log(Level.TRACE, MARKER, simple);

        logger.setCurrentEvent(EVENTS[15]);
        logger.log(Level.TRACE, CHAR_SEQ);
        logger.setCurrentEvent(EVENTS[16]);
        logger.log(Level.TRACE, CHAR_SEQ, t);
        logger.setCurrentEvent(EVENTS[17]);
        logger.log(Level.TRACE, MARKER, CHAR_SEQ);
        logger.setCurrentEvent(EVENTS[18]);
        logger.log(Level.TRACE, MARKER, CHAR_SEQ, t);

        assertEquals(4, logger.getCharSeqCount(), "log(CharSeq) invocations");
        assertEquals(5, logger.getObjectCount(), "log(Object) invocations");
    }

    @Test
    void testLogWarn() {
        final CountingLogger logger = new CountingLogger();
        logger.setCurrentLevel(Level.WARN);

        logger.setCurrentEvent(EVENTS[0]);
        logger.log(Level.WARN, "Hello");
        logger.log(Level.WARN, (Marker) null, "Hello");
        logger.setCurrentEvent(EVENTS[1]);
        logger.log(Level.WARN, MARKER, "Hello");
        logger.setCurrentEvent(EVENTS[2]);
        logger.log(Level.WARN, "Hello", t);
        logger.log(Level.WARN, (Marker) null, "Hello", t);
        logger.setCurrentEvent(EVENTS[3]);
        logger.log(Level.WARN, MARKER, "Hello", t);
        logger.setCurrentEvent(EVENTS[4]);
        logger.log(Level.WARN, obj);
        logger.setCurrentEvent(EVENTS[5]);
        logger.log(Level.WARN, MARKER, obj);
        logger.setCurrentEvent(EVENTS[6]);
        logger.log(Level.WARN, obj, t);
        logger.log(Level.WARN, (Marker) null, obj, t);
        logger.setCurrentEvent(EVENTS[7]);
        logger.log(Level.WARN, MARKER, obj, t);
        logger.setCurrentEvent(EVENTS[8]);
        logger.log(Level.WARN, pattern, p1, p2);
        logger.setCurrentEvent(EVENTS[9]);
        logger.log(Level.WARN, MARKER, pattern, p1, p2);
        logger.setCurrentEvent(EVENTS[10]);
        logger.log(Level.WARN, simple);
        logger.log(Level.WARN, (Marker) null, simple);
        logger.log(Level.WARN, (Marker) null, simple, null);
        logger.setCurrentEvent(EVENTS[11]);
        logger.log(Level.WARN, simple, t);
        logger.log(Level.WARN, (Marker) null, simple, t);
        logger.setCurrentEvent(EVENTS[12]);
        logger.log(Level.WARN, MARKER, simple, null);
        logger.setCurrentEvent(EVENTS[13]);
        logger.log(Level.WARN, MARKER, simple, t);
        logger.setCurrentEvent(EVENTS[14]);
        logger.log(Level.WARN, MARKER, simple);

        logger.setCurrentEvent(EVENTS[15]);
        logger.log(Level.WARN, CHAR_SEQ);
        logger.setCurrentEvent(EVENTS[16]);
        logger.log(Level.WARN, CHAR_SEQ, t);
        logger.setCurrentEvent(EVENTS[17]);
        logger.log(Level.WARN, MARKER, CHAR_SEQ);
        logger.setCurrentEvent(EVENTS[18]);
        logger.log(Level.WARN, MARKER, CHAR_SEQ, t);

        assertEquals(4, logger.getCharSeqCount(), "log(CharSeq) invocations");
        assertEquals(5, logger.getObjectCount(), "log(Object) invocations");
    }

    @Test
    void testTrace() {
        final CountingLogger logger = new CountingLogger();
        logger.setCurrentLevel(Level.TRACE);

        logger.setCurrentEvent(EVENTS[0]);
        logger.trace("Hello");
        logger.trace((Marker) null, "Hello");
        logger.setCurrentEvent(EVENTS[1]);
        logger.trace(MARKER, "Hello");
        logger.setCurrentEvent(EVENTS[2]);
        logger.trace("Hello", t);
        logger.trace((Marker) null, "Hello", t);
        logger.setCurrentEvent(EVENTS[3]);
        logger.trace(MARKER, "Hello", t);
        logger.setCurrentEvent(EVENTS[4]);
        logger.trace(obj);
        logger.setCurrentEvent(EVENTS[5]);
        logger.trace(MARKER, obj);
        logger.setCurrentEvent(EVENTS[6]);
        logger.trace(obj, t);
        logger.trace((Marker) null, obj, t);
        logger.setCurrentEvent(EVENTS[7]);
        logger.trace(MARKER, obj, t);
        logger.setCurrentEvent(EVENTS[8]);
        logger.trace(pattern, p1, p2);
        logger.setCurrentEvent(EVENTS[9]);
        logger.trace(MARKER, pattern, p1, p2);
        logger.setCurrentEvent(EVENTS[10]);
        logger.trace(simple);
        logger.trace((Marker) null, simple);
        logger.trace((Marker) null, simple, null);
        logger.setCurrentEvent(EVENTS[11]);
        logger.trace(simple, t);
        logger.trace((Marker) null, simple, t);
        logger.setCurrentEvent(EVENTS[12]);
        logger.trace(MARKER, simple, null);
        logger.setCurrentEvent(EVENTS[13]);
        logger.trace(MARKER, simple, t);
        logger.setCurrentEvent(EVENTS[14]);
        logger.trace(MARKER, simple);

        logger.setCurrentEvent(EVENTS[15]);
        logger.trace(CHAR_SEQ);
        logger.setCurrentEvent(EVENTS[16]);
        logger.trace(CHAR_SEQ, t);
        logger.setCurrentEvent(EVENTS[17]);
        logger.trace(MARKER, CHAR_SEQ);
        logger.setCurrentEvent(EVENTS[18]);
        logger.trace(MARKER, CHAR_SEQ, t);

        assertEquals(4, logger.getCharSeqCount(), "log(CharSeq) invocations");
        assertEquals(5, logger.getObjectCount(), "log(Object) invocations");
    }

    @Test
    void testWarn() {
        final CountingLogger logger = new CountingLogger();
        logger.setCurrentLevel(Level.WARN);

        logger.setCurrentEvent(EVENTS[0]);
        logger.warn("Hello");
        logger.warn((Marker) null, "Hello");
        logger.setCurrentEvent(EVENTS[1]);
        logger.warn(MARKER, "Hello");
        logger.setCurrentEvent(EVENTS[2]);
        logger.warn("Hello", t);
        logger.warn((Marker) null, "Hello", t);
        logger.setCurrentEvent(EVENTS[3]);
        logger.warn(MARKER, "Hello", t);
        logger.setCurrentEvent(EVENTS[4]);
        logger.warn(obj);
        logger.setCurrentEvent(EVENTS[5]);
        logger.warn(MARKER, obj);
        logger.setCurrentEvent(EVENTS[6]);
        logger.warn(obj, t);
        logger.warn((Marker) null, obj, t);
        logger.setCurrentEvent(EVENTS[7]);
        logger.warn(MARKER, obj, t);
        logger.setCurrentEvent(EVENTS[8]);
        logger.warn(pattern, p1, p2);
        logger.setCurrentEvent(EVENTS[9]);
        logger.warn(MARKER, pattern, p1, p2);
        logger.setCurrentEvent(EVENTS[10]);
        logger.warn(simple);
        logger.warn((Marker) null, simple);
        logger.warn((Marker) null, simple, null);
        logger.setCurrentEvent(EVENTS[11]);
        logger.warn(simple, t);
        logger.warn((Marker) null, simple, t);
        logger.setCurrentEvent(EVENTS[12]);
        logger.warn(MARKER, simple, null);
        logger.setCurrentEvent(EVENTS[13]);
        logger.warn(MARKER, simple, t);
        logger.setCurrentEvent(EVENTS[14]);
        logger.warn(MARKER, simple);

        logger.setCurrentEvent(EVENTS[15]);
        logger.warn(CHAR_SEQ);
        logger.setCurrentEvent(EVENTS[16]);
        logger.warn(CHAR_SEQ, t);
        logger.setCurrentEvent(EVENTS[17]);
        logger.warn(MARKER, CHAR_SEQ);
        logger.setCurrentEvent(EVENTS[18]);
        logger.warn(MARKER, CHAR_SEQ, t);

        assertEquals(4, logger.getCharSeqCount(), "log(CharSeq) invocations");
        assertEquals(5, logger.getObjectCount(), "log(Object) invocations");
    }

    @Test
    void testMessageWithThrowable() {
        final ThrowableExpectingLogger logger = new ThrowableExpectingLogger(true);
        final ThrowableMessage message = new ThrowableMessage(t);

        logger.debug(message);
        logger.error(message);
        logger.fatal(message);
        logger.info(message);
        logger.trace(message);
        logger.warn(message);
        logger.log(Level.INFO, message);

        logger.debug(MARKER, message);
        logger.error(MARKER, message);
        logger.fatal(MARKER, message);
        logger.info(MARKER, message);
        logger.trace(MARKER, message);
        logger.warn(MARKER, message);
        logger.log(Level.INFO, MARKER, message);
    }

    @Test
    void testMessageWithoutThrowable() {
        final ThrowableExpectingLogger logger = new ThrowableExpectingLogger(false);
        final ThrowableMessage message = new ThrowableMessage(null);

        logger.debug(message);
        logger.error(message);
        logger.fatal(message);
        logger.info(message);
        logger.trace(message);
        logger.warn(message);
        logger.log(Level.INFO, message);

        logger.debug(MARKER, message);
        logger.error(MARKER, message);
        logger.fatal(MARKER, message);
        logger.info(MARKER, message);
        logger.trace(MARKER, message);
        logger.warn(MARKER, message);
        logger.log(Level.INFO, MARKER, message);
    }

    @Test
    void testMessageSupplierWithThrowable() {
        final ThrowableExpectingLogger logger = new ThrowableExpectingLogger(true);
        final ThrowableMessage message = new ThrowableMessage(t);
        final MessageSupplier supplier = () -> message;

        logger.debug(supplier);
        logger.error(supplier);
        logger.fatal(supplier);
        logger.info(supplier);
        logger.trace(supplier);
        logger.warn(supplier);
        logger.log(Level.INFO, supplier);

        logger.debug(MARKER, supplier);
        logger.error(MARKER, supplier);
        logger.fatal(MARKER, supplier);
        logger.info(MARKER, supplier);
        logger.trace(MARKER, supplier);
        logger.warn(MARKER, supplier);
        logger.log(Level.INFO, MARKER, supplier);
    }

    @Test
    void testMessageSupplierWithoutThrowable() {
        final ThrowableExpectingLogger logger = new ThrowableExpectingLogger(false);
        final ThrowableMessage message = new ThrowableMessage(null);
        final MessageSupplier supplier = () -> message;

        logger.debug(supplier);
        logger.error(supplier);
        logger.fatal(supplier);
        logger.info(supplier);
        logger.trace(supplier);
        logger.warn(supplier);
        logger.log(Level.INFO, supplier);

        logger.debug(MARKER, supplier);
        logger.error(MARKER, supplier);
        logger.fatal(MARKER, supplier);
        logger.info(MARKER, supplier);
        logger.trace(MARKER, supplier);
        logger.warn(MARKER, supplier);
        logger.log(Level.INFO, MARKER, supplier);
    }

    @Test
    void testSupplierWithThrowable() {
        final ThrowableExpectingLogger logger = new ThrowableExpectingLogger(true);
        final ThrowableMessage message = new ThrowableMessage(t);
        final Supplier<Message> supplier = () -> message;

        logger.debug(supplier);
        logger.error(supplier);
        logger.fatal(supplier);
        logger.info(supplier);
        logger.trace(supplier);
        logger.warn(supplier);
        logger.log(Level.INFO, supplier);

        logger.debug(MARKER, supplier);
        logger.error(MARKER, supplier);
        logger.fatal(MARKER, supplier);
        logger.info(MARKER, supplier);
        logger.trace(MARKER, supplier);
        logger.warn(MARKER, supplier);
        logger.log(Level.INFO, MARKER, supplier);
    }

    @Test
    void testSupplierWithoutThrowable() {
        final ThrowableExpectingLogger logger = new ThrowableExpectingLogger(false);
        final ThrowableMessage message = new ThrowableMessage(null);
        final Supplier<Message> supplier = () -> message;

        logger.debug(supplier);
        logger.error(supplier);
        logger.fatal(supplier);
        logger.info(supplier);
        logger.trace(supplier);
        logger.warn(supplier);
        logger.log(Level.INFO, supplier);

        logger.debug(MARKER, supplier);
        logger.error(MARKER, supplier);
        logger.fatal(MARKER, supplier);
        logger.info(MARKER, supplier);
        logger.trace(MARKER, supplier);
        logger.warn(MARKER, supplier);
        logger.log(Level.INFO, MARKER, supplier);
    }

    @Test
    @ResourceLock("log4j2.StatusLogger")
    void testMessageThrows() {
        final ThrowableExpectingLogger logger = new ThrowableExpectingLogger(false);
        logger.error(new TestMessage(
                () -> {
                    throw new IllegalStateException("Oops!");
                },
                "Message Format"));
        final List<StatusData> statusDatalist = StatusLogger.getLogger().getStatusData();
        final StatusData mostRecent = statusDatalist.get(statusDatalist.size() - 1);
        assertEquals(Level.WARN, mostRecent.getLevel());
        assertThat(
                mostRecent.getFormattedStatus(),
                containsString("org.apache.logging.log4j.spi.AbstractLogger caught "
                        + "java.lang.IllegalStateException logging TestMessage: Message Format"));
    }

    @Test
    @ResourceLock("log4j2.StatusLogger")
    void testMessageThrowsAndNullFormat() {
        final ThrowableExpectingLogger logger = new ThrowableExpectingLogger(false);
        logger.error(new TestMessage(
                () -> {
                    throw new IllegalStateException("Oops!");
                },
                null /* format */));
        final List<StatusData> statusDatalist = StatusLogger.getLogger().getStatusData();
        final StatusData mostRecent = statusDatalist.get(statusDatalist.size() - 1);
        assertEquals(Level.WARN, mostRecent.getLevel());
        assertThat(
                mostRecent.getFormattedStatus(),
                containsString("org.apache.logging.log4j.spi.AbstractLogger caught "
                        + "java.lang.IllegalStateException logging TestMessage: "));
    }

    private static final class TestMessage implements Message {
        private final FormattedMessageSupplier formattedMessageSupplier;
        private final String format;

        TestMessage(final FormattedMessageSupplier formattedMessageSupplier, final String format) {
            this.formattedMessageSupplier = formattedMessageSupplier;
            this.format = format;
        }

        @Override
        public String getFormattedMessage() {
            return formattedMessageSupplier.getFormattedMessage();
        }

        @Override
        public String getFormat() {
            return format;
        }

        @Override
        public Object[] getParameters() {
            return Constants.EMPTY_OBJECT_ARRAY;
        }

        @Override
        public Throwable getThrowable() {
            return null;
        }

        interface FormattedMessageSupplier {
            String getFormattedMessage();
        }
    }

    private static class CountingLogger extends AbstractLogger {
        private static final long serialVersionUID = -3171452617952475480L;

        private Level currentLevel;
        private LogEvent currentEvent;
        private int charSeqCount;
        private int objectCount;

        CountingLogger() {
            super("CountingLogger", new MessageFactory2Adapter(ParameterizedMessageFactory.INSTANCE));
        }

        void setCurrentLevel(final Level currentLevel) {
            this.currentLevel = currentLevel;
        }

        void setCurrentEvent(final LogEvent currentEvent) {
            this.currentEvent = currentEvent;
        }

        int getCharSeqCount() {
            return charSeqCount;
        }

        int getObjectCount() {
            return objectCount;
        }

        @Override
        public Level getLevel() {
            return currentLevel;
        }

        @Override
        public boolean isEnabled(final Level level, final Marker marker, final Message data, final Throwable t) {
            assertEquals(level, currentLevel, "Incorrect Level. Expected " + currentLevel + ", actual " + level);
            if (marker == null) {
                if (currentEvent.markerName != null) {
                    fail("Incorrect marker. Expected " + currentEvent.markerName + ", actual is null");
                }
            } else if (currentEvent.markerName == null) {
                fail("Incorrect marker. Expected null. Actual is " + marker.getName());
            } else {
                assertEquals(
                        currentEvent.markerName,
                        marker.getName(),
                        "Incorrect marker. Expected " + currentEvent.markerName + ", actual " + marker.getName());
            }
            if (data == null) {
                if (currentEvent.data != null) {
                    fail("Incorrect message. Expected " + currentEvent.data + ", actual is null");
                }
            } else if (currentEvent.data == null) {
                fail("Incorrect message. Expected null. Actual is " + data.getFormattedMessage());
            } else {
                assertTrue(
                        data.getClass().isAssignableFrom(currentEvent.data.getClass()),
                        "Incorrect message type. Expected " + currentEvent.data + ", actual " + data);
                assertEquals(
                        currentEvent.data.getFormattedMessage(),
                        data.getFormattedMessage(),
                        "Incorrect message. Expected " + currentEvent.data.getFormattedMessage() + ", actual "
                                + data.getFormattedMessage());
            }
            if (t == null) {
                if (currentEvent.t != null) {
                    fail("Incorrect Throwable. Expected " + currentEvent.t + ", actual is null");
                }
            } else if (currentEvent.t == null) {
                fail("Incorrect Throwable. Expected null. Actual is " + t);
            } else {
                assertEquals(currentEvent.t, t, "Incorrect Throwable. Expected " + currentEvent.t + ", actual " + t);
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
        public boolean isEnabled(
                final Level level, final Marker marker, final String message, final Object p0, final Object p1) {
            return isEnabled(level, marker, new ParameterizedMessage(message, p0, p1), null);
        }

        @Override
        public boolean isEnabled(
                final Level level,
                final Marker marker,
                final String message,
                final Object p0,
                final Object p1,
                final Object p2) {
            return isEnabled(level, marker, new ParameterizedMessage(message, p0, p1, p2), null);
        }

        @Override
        public boolean isEnabled(
                final Level level,
                final Marker marker,
                final String message,
                final Object p0,
                final Object p1,
                final Object p2,
                final Object p3) {
            return isEnabled(level, marker, new ParameterizedMessage(message, p0, p1, p2, p3), null);
        }

        @Override
        public boolean isEnabled(
                final Level level,
                final Marker marker,
                final String message,
                final Object p0,
                final Object p1,
                final Object p2,
                final Object p3,
                final Object p4) {
            return isEnabled(level, marker, new ParameterizedMessage(message, p0, p1, p2, p3, p4), null);
        }

        @Override
        public boolean isEnabled(
                final Level level,
                final Marker marker,
                final String message,
                final Object p0,
                final Object p1,
                final Object p2,
                final Object p3,
                final Object p4,
                final Object p5) {
            return isEnabled(level, marker, new ParameterizedMessage(message, p0, p1, p2, p3, p4, p5), null);
        }

        @Override
        public boolean isEnabled(
                final Level level,
                final Marker marker,
                final String message,
                final Object p0,
                final Object p1,
                final Object p2,
                final Object p3,
                final Object p4,
                final Object p5,
                final Object p6) {
            return isEnabled(level, marker, new ParameterizedMessage(message, p0, p1, p2, p3, p4, p5, p6), null);
        }

        @Override
        public boolean isEnabled(
                final Level level,
                final Marker marker,
                final String message,
                final Object p0,
                final Object p1,
                final Object p2,
                final Object p3,
                final Object p4,
                final Object p5,
                final Object p6,
                final Object p7) {
            return isEnabled(level, marker, new ParameterizedMessage(message, p0, p1, p2, p3, p4, p5, p6, p7), null);
        }

        @Override
        public boolean isEnabled(
                final Level level,
                final Marker marker,
                final String message,
                final Object p0,
                final Object p1,
                final Object p2,
                final Object p3,
                final Object p4,
                final Object p5,
                final Object p6,
                final Object p7,
                final Object p8) {
            return isEnabled(
                    level, marker, new ParameterizedMessage(message, p0, p1, p2, p3, p4, p5, p6, p7, p8), null);
        }

        @Override
        public boolean isEnabled(
                final Level level,
                final Marker marker,
                final String message,
                final Object p0,
                final Object p1,
                final Object p2,
                final Object p3,
                final Object p4,
                final Object p5,
                final Object p6,
                final Object p7,
                final Object p8,
                final Object p9) {
            return isEnabled(
                    level, marker, new ParameterizedMessage(message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9), null);
        }

        @Override
        public boolean isEnabled(final Level level, final Marker marker, final String data, final Throwable t) {
            return isEnabled(level, marker, (Message) new SimpleMessage(data), t);
        }

        @Override
        public void logMessage(
                final String fqcn, final Level level, final Marker marker, final Message data, final Throwable t) {
            assertEquals(level, currentLevel, "Incorrect Level. Expected " + currentLevel + ", actual " + level);
            if (marker == null) {
                if (currentEvent.markerName != null) {
                    fail("Incorrect marker. Expected " + currentEvent.markerName + ", actual is null");
                }
            } else if (currentEvent.markerName == null) {
                fail("Incorrect marker. Expected null. Actual is " + marker.getName());
            } else {
                assertEquals(
                        currentEvent.markerName,
                        marker.getName(),
                        "Incorrect marker. Expected " + currentEvent.markerName + ", actual " + marker.getName());
            }
            if (data == null) {
                if (currentEvent.data != null) {
                    fail("Incorrect message. Expected " + currentEvent.data + ", actual is null");
                }
            } else if (currentEvent.data == null) {
                fail("Incorrect message. Expected null. Actual is " + data.getFormattedMessage());
            } else {
                assertTrue(
                        data.getClass().isAssignableFrom(currentEvent.data.getClass()),
                        "Incorrect message type. Expected " + currentEvent.data + ", actual " + data);
                assertEquals(
                        currentEvent.data.getFormattedMessage(),
                        data.getFormattedMessage(),
                        "Incorrect message. Expected " + currentEvent.data.getFormattedMessage() + ", actual "
                                + data.getFormattedMessage());
            }
            if (t == null) {
                if (currentEvent.t != null) {
                    fail("Incorrect Throwable. Expected " + currentEvent.t + ", actual is null");
                }
            } else if (currentEvent.t == null) {
                fail("Incorrect Throwable. Expected null. Actual is " + t);
            } else {
                assertEquals(currentEvent.t, t, "Incorrect Throwable. Expected " + currentEvent.t + ", actual " + t);
            }
        }
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

    private static class ThrowableExpectingLogger extends AbstractLogger {
        private static final long serialVersionUID = -7218195998038685039L;
        private final boolean expectingThrowables;

        ThrowableExpectingLogger(final boolean expectingThrowables) {
            super("ThrowableExpectingLogger", new MessageFactory2Adapter(ParameterizedMessageFactory.INSTANCE));
            this.expectingThrowables = expectingThrowables;
        }

        @Override
        public boolean isEnabled(final Level level, final Marker marker, final Message message, final Throwable t) {
            return true;
        }

        @Override
        public boolean isEnabled(
                final Level level, final Marker marker, final CharSequence message, final Throwable t) {
            return true;
        }

        @Override
        public boolean isEnabled(final Level level, final Marker marker, final Object message, final Throwable t) {
            return true;
        }

        @Override
        public boolean isEnabled(final Level level, final Marker marker, final String message, final Throwable t) {
            return true;
        }

        @Override
        public boolean isEnabled(final Level level, final Marker marker, final String message) {
            return true;
        }

        @Override
        public boolean isEnabled(final Level level, final Marker marker, final String message, final Object... params) {
            return true;
        }

        @Override
        public boolean isEnabled(final Level level, final Marker marker, final String message, final Object p0) {
            return true;
        }

        @Override
        public boolean isEnabled(
                final Level level, final Marker marker, final String message, final Object p0, final Object p1) {
            return true;
        }

        @Override
        public boolean isEnabled(
                final Level level,
                final Marker marker,
                final String message,
                final Object p0,
                final Object p1,
                final Object p2) {
            return true;
        }

        @Override
        public boolean isEnabled(
                final Level level,
                final Marker marker,
                final String message,
                final Object p0,
                final Object p1,
                final Object p2,
                final Object p3) {
            return true;
        }

        @Override
        public boolean isEnabled(
                final Level level,
                final Marker marker,
                final String message,
                final Object p0,
                final Object p1,
                final Object p2,
                final Object p3,
                final Object p4) {
            return true;
        }

        @Override
        public boolean isEnabled(
                final Level level,
                final Marker marker,
                final String message,
                final Object p0,
                final Object p1,
                final Object p2,
                final Object p3,
                final Object p4,
                final Object p5) {
            return true;
        }

        @Override
        public boolean isEnabled(
                final Level level,
                final Marker marker,
                final String message,
                final Object p0,
                final Object p1,
                final Object p2,
                final Object p3,
                final Object p4,
                final Object p5,
                final Object p6) {
            return true;
        }

        @Override
        public boolean isEnabled(
                final Level level,
                final Marker marker,
                final String message,
                final Object p0,
                final Object p1,
                final Object p2,
                final Object p3,
                final Object p4,
                final Object p5,
                final Object p6,
                final Object p7) {
            return true;
        }

        @Override
        public boolean isEnabled(
                final Level level,
                final Marker marker,
                final String message,
                final Object p0,
                final Object p1,
                final Object p2,
                final Object p3,
                final Object p4,
                final Object p5,
                final Object p6,
                final Object p7,
                final Object p8) {
            return true;
        }

        @Override
        public boolean isEnabled(
                final Level level,
                final Marker marker,
                final String message,
                final Object p0,
                final Object p1,
                final Object p2,
                final Object p3,
                final Object p4,
                final Object p5,
                final Object p6,
                final Object p7,
                final Object p8,
                final Object p9) {
            return true;
        }

        @Override
        public void logMessage(
                final String fqcn, final Level level, final Marker marker, final Message message, final Throwable t) {
            if (expectingThrowables) {
                assertNotNull(t, "Expected a Throwable but received null!");
            } else {
                assertNull(t, "Expected null but received a Throwable! " + t);
            }
            if (message != null) {
                message.getFormattedMessage();
            }
        }

        @Override
        public Level getLevel() {
            return Level.INFO;
        }
    }

    private static class ThrowableMessage implements Message {
        private static final long serialVersionUID = 1L;
        private final Throwable throwable;

        public ThrowableMessage(final Throwable throwable) {
            this.throwable = throwable;
        }

        @Override
        public String getFormattedMessage() {
            return null;
        }

        @Override
        public Object[] getParameters() {
            return Constants.EMPTY_OBJECT_ARRAY;
        }

        @Override
        public Throwable getThrowable() {
            return throwable;
        }
    }
}
