
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
package org.apache.logging.slf4j;

import java.util.Date;
import java.util.List;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.testUtil.StringListAppender;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.ParameterizedMessageFactory;
import org.apache.logging.log4j.message.StringFormatterMessageFactory;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.spi.MessageFactory2Adapter;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 *
 */
public class LoggerTest {

    private static final String CONFIG = "target/test-classes/logback-slf4j.xml";

    @ClassRule
    public static final LoggerContextRule CTX = new LoggerContextRule(CONFIG);

    private Logger logger;
    private StringListAppender<ILoggingEvent> list;

    @Before
    public void setUp() throws Exception {
        final org.slf4j.Logger slf4jLogger = CTX.getLogger();
        logger = LogManager.getLogger();
        assertThat(slf4jLogger, is(theInstance(((SLF4JLogger) logger).getLogger())));
        final ch.qos.logback.classic.Logger rootLogger = CTX.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
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
        logger.entry(CONFIG);
        logger.exit(0);
        assertThat(list.strList, hasSize(2));
    }

    @Test
    public void simpleFlow() {
        logger.entry(CONFIG);
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
        final Logger testLogger = testMessageFactoryMismatch("getLogger_String_MessageFactoryMismatch",
            StringFormatterMessageFactory.INSTANCE, ParameterizedMessageFactory.INSTANCE);
        testLogger.debug("%,d", Integer.MAX_VALUE);
        assertThat(list.strList, hasSize(1));
        assertThat(list.strList, hasItem(String.format("%,d", Integer.MAX_VALUE)));
    }

    @Test
    public void getLogger_String_MessageFactoryMismatchNull() {
        final Logger testLogger =  testMessageFactoryMismatch("getLogger_String_MessageFactoryMismatchNull",
            StringFormatterMessageFactory.INSTANCE, null);
        testLogger.debug("%,d", Integer.MAX_VALUE);
        assertThat(list.strList, hasSize(1));
        assertThat(list.strList, hasItem(String.format("%,d", Integer.MAX_VALUE)));
    }

    private Logger testMessageFactoryMismatch(final String name, final MessageFactory messageFactory1, final MessageFactory messageFactory2) {
        final Logger testLogger = LogManager.getLogger(name, messageFactory1);
        assertThat(testLogger, is(notNullValue()));
        checkMessageFactory(messageFactory1, testLogger);
        final Logger testLogger2 = LogManager.getLogger(name, messageFactory2);
        checkMessageFactory(messageFactory1, testLogger2);
        return testLogger;
    }

    private static void checkMessageFactory(final MessageFactory messageFactory1, final Logger testLogger1) {
        if (messageFactory1 == null) {
            assertEquals(AbstractLogger.DEFAULT_MESSAGE_FACTORY_CLASS, testLogger1.getMessageFactory().getClass());
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
}

