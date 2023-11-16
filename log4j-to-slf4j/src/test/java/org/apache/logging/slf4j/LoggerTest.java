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
package org.apache.logging.slf4j;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.testUtil.StringListAppender;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Date;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.ParameterizedMessageFactory;
import org.apache.logging.log4j.message.StringFormatterMessageFactory;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.spi.MessageFactory2Adapter;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

@UsingStatusListener
@LoggerContextSource
public class LoggerTest {

    private static final Object OBJ = new Object();
    // Log4j objects
    private Logger logger;
    // Logback objects
    private LoggerContext context;
    private StringListAppender<ILoggingEvent> list;

    @BeforeEach
    public void setUp() throws Exception {
        final org.slf4j.Logger slf4jLogger = context.getLogger(getClass());
        logger = LogManager.getLogger();
        assertThat(slf4jLogger, is(theInstance(((SLF4JLogger) logger).getLogger())));
        final ch.qos.logback.classic.Logger rootLogger = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        rootLogger.detachAppender("console");
        list = TestUtil.getListAppender(rootLogger, "LIST");
        assertThat(list, is(notNullValue()));
        assertThat(list.strList, is(notNullValue()));
        list.strList.clear();
    }

    @Test
    public void basicFlow() {
        logger.traceEntry();
        logger.traceExit();
        assertThat(list.strList, hasSize(2));
    }

    @Test
    public void basicFlowDepreacted() {
        logger.entry();
        logger.exit();
        assertThat(list.strList, hasSize(2));
    }

    @Test
    public void simpleFlowDeprecated() {
        logger.entry(OBJ);
        logger.exit(0);
        assertThat(list.strList, hasSize(2));
    }

    @Test
    public void simpleFlow() {
        logger.entry(OBJ);
        logger.traceExit(0);
        assertThat(list.strList, hasSize(2));
    }

    @Test
    public void throwing() {
        logger.throwing(new IllegalArgumentException("Test Exception"));
        assertThat(list.strList, hasSize(1));
    }

    @Test
    public void catching() {
        try {
            throw new NullPointerException();
        } catch (final Exception e) {
            logger.catching(e);
        }
        assertThat(list.strList, hasSize(1));
    }

    @Test
    public void debug() {
        logger.debug("Debug message");
        assertThat(list.strList, hasSize(1));
    }

    @Test
    public void getLogger_String_MessageFactoryMismatch() {
        final Logger testLogger = testMessageFactoryMismatch(
                "getLogger_String_MessageFactoryMismatch",
                StringFormatterMessageFactory.INSTANCE,
                ParameterizedMessageFactory.INSTANCE);
        testLogger.debug("%,d", Integer.MAX_VALUE);
        assertThat(list.strList, hasSize(1));
        assertThat(list.strList, hasItem(String.format("%,d", Integer.MAX_VALUE)));
    }

    @Test
    public void getLogger_String_MessageFactoryMismatchNull() {
        final Logger testLogger = testMessageFactoryMismatch(
                "getLogger_String_MessageFactoryMismatchNull", StringFormatterMessageFactory.INSTANCE, null);
        testLogger.debug("%,d", Integer.MAX_VALUE);
        assertThat(list.strList, hasSize(1));
        assertThat(list.strList, hasItem(String.format("%,d", Integer.MAX_VALUE)));
    }

    private Logger testMessageFactoryMismatch(
            final String name, final MessageFactory messageFactory1, final MessageFactory messageFactory2) {
        final Logger testLogger = LogManager.getLogger(name, messageFactory1);
        assertThat(testLogger, is(notNullValue()));
        checkMessageFactory(messageFactory1, testLogger);
        final Logger testLogger2 = LogManager.getLogger(name, messageFactory2);
        checkMessageFactory(messageFactory2, testLogger2);
        return testLogger;
    }

    private static void checkMessageFactory(final MessageFactory messageFactory1, final Logger testLogger1) {
        if (messageFactory1 == null) {
            assertEquals(
                    AbstractLogger.DEFAULT_MESSAGE_FACTORY_CLASS,
                    testLogger1.getMessageFactory().getClass());
        } else {
            MessageFactory actual = testLogger1.getMessageFactory();
            if (actual instanceof MessageFactory2Adapter) {
                actual = ((MessageFactory2Adapter) actual).getOriginal();
            }
            assertEquals(messageFactory1, actual);
        }
    }

    @Test
    public void debugObject() {
        logger.debug(new Date());
        assertThat(list.strList, hasSize(1));
    }

    @Test
    public void debugWithParms() {
        logger.debug("Hello, {}", "World");
        assertThat(list.strList, hasSize(1));
        final String message = list.strList.get(0);
        assertEquals("Hello, World", message);
    }

    @Test
    public void paramIncludesSubstitutionMarker_locationAware() {
        logger.info("Hello, {}", "foo {} bar");
        assertThat(list.strList, hasSize(1));
        final String message = list.strList.get(0);
        assertEquals("Hello, foo {} bar", message);
    }

    @Test
    public void paramIncludesSubstitutionMarker_nonLocationAware() {
        final org.slf4j.Logger slf4jLogger = context.getLogger(getClass());
        final Logger nonLocationAwareLogger =
                new SLF4JLogger(slf4jLogger.getName(), (org.slf4j.Logger) Proxy.newProxyInstance(
                        getClass().getClassLoader(), new Class<?>[] {org.slf4j.Logger.class}, (proxy, method, args) -> {
                            try {
                                return method.invoke(slf4jLogger, args);
                            } catch (InvocationTargetException e) {
                                throw e.getCause();
                            }
                        }));
        nonLocationAwareLogger.info("Hello, {}", "foo {} bar");
        assertThat(list.strList, hasSize(1));
        final String message = list.strList.get(0);
        assertEquals("Hello, foo {} bar", message);
    }

    @Test
    public void testImpliedThrowable() {
        logger.debug("This is a test", new Throwable("Testing"));
        final List<String> msgs = list.strList;
        assertThat(msgs, hasSize(1));
        final String expected = "java.lang.Throwable: Testing";
        assertTrue("Incorrect message data", msgs.get(0).contains(expected));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mdc() {
        ThreadContext.put("TestYear", Integer.toString(2010));
        logger.debug("Debug message");
        ThreadContext.clearMap();
        logger.debug("Debug message");
        assertThat(list.strList, hasSize(2));
        assertTrue("Incorrect year", list.strList.get(0).startsWith("2010"));
    }

    @Test
    public void mdcNullBackedIsEmpty() {
        assertNull("Setup wrong", MDC.getCopyOfContextMap());
        assertTrue(ThreadContext.isEmpty());
    }

    @Test
    public void mdcNullBackedContainsKey() {
        assertNull("Setup wrong", MDC.getCopyOfContextMap());
        assertFalse(ThreadContext.containsKey("something"));
    }

    @Test
    public void mdcNullBackedContainsNullKey() {
        assertNull("Setup wrong", MDC.getCopyOfContextMap());
        assertFalse(ThreadContext.containsKey(null));
    }

    @Test
    public void mdcContainsNullKey() {
        try {
            ThreadContext.put("some", "thing");
            assertNotNull("Setup wrong", MDC.getCopyOfContextMap());
            assertFalse(ThreadContext.containsKey(null));
        } finally {
            ThreadContext.clearMap();
        }
    }

    @Test
    public void mdcCannotContainNullKey() {
        try {
            ThreadContext.put(null, "something");
            fail("should throw");
        } catch (IllegalArgumentException | NullPointerException e) {
            // expected
        }
    }
}
