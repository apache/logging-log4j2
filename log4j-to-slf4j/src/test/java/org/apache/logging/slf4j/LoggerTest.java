
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
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.theInstance;
import static org.junit.Assert.*;

/**
 *
 */
public class LoggerTest {

    private static final String CONFIG = "target/test-classes/logback-slf4j.xml";

    @ClassRule
    public static final InitialLoggerContext CTX = new InitialLoggerContext(CONFIG);

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
        logger.entry();
        logger.exit();
        assertTrue("Incorrect number of events. Expected 2, actual " + list.strList.size(), list.strList.size() == 2);
    }

    @Test
    public void simpleFlow() {
        logger.entry(CONFIG);
        logger.exit(0);
        assertTrue("Incorrect number of events. Expected 2, actual " + list.strList.size(), list.strList.size() == 2);
    }

    @Test
    public void throwing() {
        logger.throwing(new IllegalArgumentException("Test Exception"));
        assertTrue("Incorrect number of events. Expected 1, actual " + list.strList.size(), list.strList.size() == 1);
    }

    @Test
    public void catching() {
        try {
            throw new NullPointerException();
        } catch (final Exception e) {
            logger.catching(e);
        }
        assertTrue("Incorrect number of events. Expected 1, actual " + list.strList.size(), list.strList.size() == 1);
    }

    @Test
    public void debug() {
        logger.debug("Debug message");
        assertTrue("Incorrect number of events. Expected 1, actual " + list.strList.size(), list.strList.size() == 1);
    }

    @Test
    public void getLogger_String_MessageFactoryMismatch() {
        final Logger testLogger = testMessageFactoryMismatch("getLogger_String_MessageFactoryMismatch",
            StringFormatterMessageFactory.INSTANCE, ParameterizedMessageFactory.INSTANCE);
        testLogger.debug("%,d", Integer.MAX_VALUE);
        assertTrue("Incorrect number of events. Expected 1, actual " + list.strList.size(), list.strList.size() == 1);
        assertEquals(String.format("%,d", Integer.MAX_VALUE), list.strList.get(0));
    }

    @Test
    public void getLogger_String_MessageFactoryMismatchNull() {
        final Logger testLogger =  testMessageFactoryMismatch("getLogger_String_MessageFactoryMismatchNull",
            StringFormatterMessageFactory.INSTANCE, null);
        testLogger.debug("%,d", Integer.MAX_VALUE);
        assertTrue("Incorrect number of events. Expected 1, actual " + list.strList.size(), list.strList.size() == 1);
        assertEquals(String.format("%,d", Integer.MAX_VALUE), list.strList.get(0));
    }

    private Logger testMessageFactoryMismatch(final String name, final MessageFactory messageFactory1, final MessageFactory messageFactory2) {
        final Logger testLogger = LogManager.getLogger(name, messageFactory1);
        assertNotNull(testLogger);
        assertEquals(messageFactory1, testLogger.getMessageFactory());
        final Logger testLogger2 = LogManager.getLogger(name, messageFactory2);
        assertEquals(messageFactory1, testLogger2.getMessageFactory());
        return testLogger;
    }

    @Test
    public void debugObject() {
        logger.debug(new Date());
        assertTrue("Incorrect number of events. Expected 1, actual " + list.strList.size(), list.strList.size() == 1);
    }

    @Test
    public void debugWithParms() {
        logger.debug("Hello, {}", "World");
        assertTrue("Incorrect number of events. Expected 1, actual " + list.strList.size(), list.strList.size() == 1);
    }

    @Test
    public void testImpliedThrowable() {
        logger.debug("This is a test", new Throwable("Testing"));
        final List<String> msgs = list.strList;
        assertTrue("Incorrect number of messages. Expected 1, actual " + msgs.size(), msgs.size() == 1);
        final String expected = "java.lang.Throwable: Testing";
        assertTrue("Incorrect message data", msgs.get(0).contains(expected));
    }
    @Test
    public void mdc() {
        ThreadContext.put("TestYear", new Integer(2010).toString());
        logger.debug("Debug message");
        ThreadContext.clearMap();
        logger.debug("Debug message");
        assertTrue("Incorrect number of events. Expected 2, actual " + list.strList.size(), list.strList.size() == 2);
        assertTrue("Incorrect year", list.strList.get(0).startsWith("2010"));
    }
}

