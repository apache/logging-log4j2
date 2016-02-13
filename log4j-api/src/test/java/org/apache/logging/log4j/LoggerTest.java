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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.message.EntryMessage;
import org.apache.logging.log4j.message.JsonMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.message.ParameterizedMessageFactory;
import org.apache.logging.log4j.message.SimpleMessageFactory;
import org.apache.logging.log4j.message.StringFormatterMessageFactory;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.apache.logging.log4j.util.MessageSupplier;
import org.apache.logging.log4j.util.Strings;
import org.apache.logging.log4j.util.Supplier;
import org.junit.Before;
import org.junit.Test;
/**
 *
 */
public class LoggerTest {

    private static class TestParameterizedMessageFactory {
        // empty
    }

    private static class TestStringFormatterMessageFactory {
        // empty
    }

    TestLogger logger = (TestLogger) LogManager.getLogger("LoggerTest");
    List<String> results = logger.getEntries();

    @Test
    public void basicFlow() {
        logger.entry();
        logger.exit();
        assertEquals(2, results.size());
        assertThat("Incorrect Entry", results.get(0), equalTo("ENTRY[ FLOW ] TRACE entry"));
        assertThat("incorrect Exit", results.get(1), equalTo("EXIT[ FLOW ] TRACE exit"));

    }

    @Test
    public void flowTracingMessage() {
        logger.traceEntry(new JsonMessage(System.getProperties()));
        final Response response = new Response(-1, "Generic error");
        logger.traceExit(response,  new JsonMessage(response));
        assertEquals(2, results.size());
        assertThat("Incorrect Entry", results.get(0), startsWith("ENTRY[ FLOW ] TRACE entry"));
        assertThat("Missing entry data", results.get(0), containsString("\"java.runtime.name\":"));
        assertThat("incorrect Exit", results.get(1), startsWith("EXIT[ FLOW ] TRACE exit"));
        assertThat("Missing exit data", results.get(1), containsString("\"message\":\"Generic error\""));
    }

    @Test
    public void flowTracingString_ObjectArray1() {
        logger.traceEntry("doFoo(a={}, b={})", 1, 2);
        logger.traceExit("doFoo(a=1, b=2): {}", 3);
        assertEquals(2, results.size());
        assertThat("Incorrect Entry", results.get(0), startsWith("ENTRY[ FLOW ] TRACE entry"));
        assertThat("Missing entry data", results.get(0), containsString("doFoo(a=1, b=2)"));
        assertThat("Incorrect Exit", results.get(1), startsWith("EXIT[ FLOW ] TRACE exit"));
        assertThat("Missing exit data", results.get(1), containsString("doFoo(a=1, b=2): 3"));
    }

    @Test
    public void flowTracingString_ObjectArray2() {
        EntryMessage msg = logger.traceEntry("doFoo(a={}, b={})", 1, 2);
        logger.traceExit(3, msg);
        assertEquals(2, results.size());
        assertThat("Incorrect Entry", results.get(0), startsWith("ENTRY[ FLOW ] TRACE entry"));
        assertThat("Missing entry data", results.get(0), containsString("doFoo(a=1, b=2)"));
        assertThat("Incorrect Exit", results.get(1), startsWith("EXIT[ FLOW ] TRACE exit"));
        assertThat("Missing exit data", results.get(1), containsString("doFoo(a=1, b=2): 3"));
    }

    @Test
    public void flowTracingNoExitArgs() {
        logger.traceEntry();
        logger.traceExit();
        assertEquals(2, results.size());
        assertThat("Incorrect Entry", results.get(0), startsWith("ENTRY[ FLOW ] TRACE entry"));
        assertThat("Incorrect Exit", results.get(1), startsWith("EXIT[ FLOW ] TRACE exit"));
    }

    @Test
    public void flowTracingNoArgs() {
        final EntryMessage message = logger.traceEntry();
        logger.traceExit(message);
        assertEquals(2, results.size());
        assertThat("Incorrect Entry", results.get(0), startsWith("ENTRY[ FLOW ] TRACE entry"));
        assertThat("Incorrect Exit", results.get(1), startsWith("EXIT[ FLOW ] TRACE exit"));
    }

    @Test
    public void flowTracingString_MessageSupplierOfObjectMessages() {
        EntryMessage msg = logger.traceEntry("doFoo(a={}, b={})", new MessageSupplier() {
            @Override
            public Message get() {
                return new ObjectMessage(1);
            }
        }, new MessageSupplier() {
            @Override
            public Message get() {
                return new ObjectMessage(2);
            }
        });
        logger.traceExit(3, msg);
        assertEquals(2, results.size());
        assertThat("Incorrect Entry", results.get(0), startsWith("ENTRY[ FLOW ] TRACE entry"));
        assertThat("Missing entry data", results.get(0), containsString("doFoo(a=1, b=2)"));
        assertThat("Incorrect Exit", results.get(1), startsWith("EXIT[ FLOW ] TRACE exit"));
        assertThat("Missing exit data", results.get(1), containsString("doFoo(a=1, b=2): 3"));
    }

    @Test
    public void flowTracingString_SupplierOfObjectMessages() {
        EntryMessage msg = logger.traceEntry("doFoo(a={}, b={})", new Supplier<Message>() {
            @Override
            public Message get() {
                return new ObjectMessage(1);
            }
        }, new Supplier<Message>() {
            @Override
            public Message get() {
                return new ObjectMessage(2);
            }
        });
        logger.traceExit(3, msg);
        assertEquals(2, results.size());
        assertThat("Incorrect Entry", results.get(0), startsWith("ENTRY[ FLOW ] TRACE entry"));
        assertThat("Missing entry data", results.get(0), containsString("doFoo(a=1, b=2)"));
        assertThat("Incorrect Exit", results.get(1), startsWith("EXIT[ FLOW ] TRACE exit"));
        assertThat("Missing exit data", results.get(1), containsString("doFoo(a=1, b=2): 3"));
    }

    @Test
    public void flowTracingString_SupplierOfStrings() {
        EntryMessage msg = logger.traceEntry("doFoo(a={}, b={})", new Supplier<String>() {
            @Override
            public String get() {
                return "1";
            }
        }, new Supplier<String>() {
            @Override
            public String get() {
                return "2";
            }
        });
        logger.traceExit(3, msg);
        assertEquals(2, results.size());
        assertThat("Incorrect Entry", results.get(0), startsWith("ENTRY[ FLOW ] TRACE entry"));
        assertThat("Missing entry data", results.get(0), containsString("doFoo(a=1, b=2)"));
        assertThat("Incorrect Exit", results.get(1), startsWith("EXIT[ FLOW ] TRACE exit"));
        assertThat("Missing exit data", results.get(1), containsString("doFoo(a=1, b=2): 3"));
    }

    @Test
    public void catching() {
        try {
            throw new NullPointerException();
        } catch (final Exception e) {
            logger.catching(e);
            assertEquals(1, results.size());
            assertThat("Incorrect Catching",
                    results.get(0), startsWith("CATCHING[ EXCEPTION ] ERROR catching java.lang.NullPointerException"));
        }
    }

    @Test
    public void debug() {
        logger.debug("Debug message");
        assertEquals(1, results.size());
        assertTrue("Incorrect message", results.get(0).startsWith(" DEBUG Debug message"));
    }

    @Test
    public void debugObject() {
        logger.debug(new Date());
        assertEquals(1, results.size());
        assertTrue("Invalid length", results.get(0).length() > 7);
    }

    @Test
    public void debugWithParms() {
        logger.debug("Hello, {}", "World");
        assertEquals(1, results.size());
        assertTrue("Incorrect substitution", results.get(0).startsWith(" DEBUG Hello, World"));
    }

    @Test
    public void debugWithParmsAndThrowable() {
        logger.debug("Hello, {}", "World", new RuntimeException("Test Exception"));
        assertEquals(1, results.size());
        assertTrue("Unexpected results: " + results.get(0),
            results.get(0).startsWith(" DEBUG Hello, World java.lang.RuntimeException: Test Exception"));
    }

    @Test
    public void getFormatterLogger() {
        // The TestLogger logger was already created in an instance variable for this class.
        // The message factory is only used when the logger is created.
        final TestLogger testLogger = (TestLogger) LogManager.getFormatterLogger();
        final TestLogger altLogger = (TestLogger) LogManager.getFormatterLogger(getClass());
        assertEquals(testLogger.getName(), altLogger.getName());
        assertNotNull(testLogger);
        assertTrue(testLogger.getMessageFactory() instanceof StringFormatterMessageFactory);
        assertEquals(StringFormatterMessageFactory.INSTANCE, testLogger.getMessageFactory());
        testLogger.debug("%,d", Integer.MAX_VALUE);
        assertEquals(1, testLogger.getEntries().size());
        assertEquals(String.format(" DEBUG %,d", Integer.MAX_VALUE), testLogger.getEntries().get(0));
    }

    @Test
    public void getFormatterLogger_Class() {
        // The TestLogger logger was already created in an instance variable for this class.
        // The message factory is only used when the logger is created.
        final TestLogger testLogger = (TestLogger) LogManager.getFormatterLogger(TestStringFormatterMessageFactory.class);
        assertNotNull(testLogger);
        assertTrue(testLogger.getMessageFactory() instanceof StringFormatterMessageFactory);
        assertEquals(StringFormatterMessageFactory.INSTANCE, testLogger.getMessageFactory());
        testLogger.debug("%,d", Integer.MAX_VALUE);
        assertEquals(1, testLogger.getEntries().size());
        assertEquals(String.format(" DEBUG %,d", Integer.MAX_VALUE), testLogger.getEntries().get(0));
    }

    @Test
    public void getFormatterLogger_Object() {
        // The TestLogger logger was already created in an instance variable for this class.
        // The message factory is only used when the logger is created.
        final TestLogger testLogger = (TestLogger) LogManager.getFormatterLogger(new TestStringFormatterMessageFactory());
        assertNotNull(testLogger);
        assertTrue(testLogger.getMessageFactory() instanceof StringFormatterMessageFactory);
        assertEquals(StringFormatterMessageFactory.INSTANCE, testLogger.getMessageFactory());
        testLogger.debug("%,d", Integer.MAX_VALUE);
        assertEquals(1, testLogger.getEntries().size());
        assertEquals(String.format(" DEBUG %,d", Integer.MAX_VALUE), testLogger.getEntries().get(0));
    }

    @Test
    public void getFormatterLogger_String() {
        final StringFormatterMessageFactory messageFactory = StringFormatterMessageFactory.INSTANCE;
        final TestLogger testLogger = (TestLogger) LogManager.getFormatterLogger("getLogger_String_StringFormatterMessageFactory");
        assertNotNull(testLogger);
        assertTrue(testLogger.getMessageFactory() instanceof StringFormatterMessageFactory);
        assertEquals(messageFactory, testLogger.getMessageFactory());
        testLogger.debug("%,d", Integer.MAX_VALUE);
        assertEquals(1, testLogger.getEntries().size());
        assertEquals(String.format(" DEBUG %,d", Integer.MAX_VALUE), testLogger.getEntries().get(0));
    }

    @Test
    public void getLogger_Class_ParameterizedMessageFactory() {
        // The TestLogger logger was already created in an instance variable for this class.
        // The message factory is only used when the logger is created.
        final ParameterizedMessageFactory messageFactory = ParameterizedMessageFactory.INSTANCE;
        final TestLogger testLogger = (TestLogger) LogManager.getLogger(TestParameterizedMessageFactory.class,
                messageFactory);
        assertNotNull(testLogger);
        assertEquals(messageFactory, testLogger.getMessageFactory());
        testLogger.debug("{}", Integer.MAX_VALUE);
        assertEquals(1, testLogger.getEntries().size());
        assertEquals(" DEBUG " + Integer.MAX_VALUE, testLogger.getEntries().get(0));
    }

    @Test
    public void getLogger_Class_StringFormatterMessageFactory() {
        // The TestLogger logger was already created in an instance variable for this class.
        // The message factory is only used when the logger is created.
        final TestLogger testLogger = (TestLogger) LogManager.getLogger(TestStringFormatterMessageFactory.class,
                StringFormatterMessageFactory.INSTANCE);
        assertNotNull(testLogger);
        assertEquals(StringFormatterMessageFactory.INSTANCE, testLogger.getMessageFactory());
        testLogger.debug("%,d", Integer.MAX_VALUE);
        assertEquals(1, testLogger.getEntries().size());
        assertEquals(String.format(" DEBUG %,d", Integer.MAX_VALUE), testLogger.getEntries().get(0));
    }

    @Test
    public void getLogger_Object_ParameterizedMessageFactory() {
        // The TestLogger logger was already created in an instance variable for this class.
        // The message factory is only used when the logger is created.
        final ParameterizedMessageFactory messageFactory =  ParameterizedMessageFactory.INSTANCE;
        final TestLogger testLogger = (TestLogger) LogManager.getLogger(new TestParameterizedMessageFactory(),
                messageFactory);
        assertNotNull(testLogger);
        assertEquals(messageFactory, testLogger.getMessageFactory());
        testLogger.debug("{}", Integer.MAX_VALUE);
        assertEquals(1, testLogger.getEntries().size());
        assertEquals(" DEBUG " + Integer.MAX_VALUE, testLogger.getEntries().get(0));
    }

    @Test
    public void getLogger_Object_StringFormatterMessageFactory() {
        // The TestLogger logger was already created in an instance variable for this class.
        // The message factory is only used when the logger is created.
        final StringFormatterMessageFactory messageFactory = StringFormatterMessageFactory.INSTANCE;
        final TestLogger testLogger = (TestLogger) LogManager.getLogger(new TestStringFormatterMessageFactory(),
                messageFactory);
        assertNotNull(testLogger);
        assertEquals(messageFactory, testLogger.getMessageFactory());
        testLogger.debug("%,d", Integer.MAX_VALUE);
        assertEquals(1, testLogger.getEntries().size());
        assertEquals(String.format(" DEBUG %,d", Integer.MAX_VALUE), testLogger.getEntries().get(0));
    }

    @Test
    public void getLogger_String_MessageFactoryMismatch() {
        final StringFormatterMessageFactory messageFactory = StringFormatterMessageFactory.INSTANCE;
        final TestLogger testLogger = (TestLogger) LogManager.getLogger("getLogger_String_MessageFactoryMismatch",
                messageFactory);
        assertNotNull(testLogger);
        assertEquals(messageFactory, testLogger.getMessageFactory());
        final TestLogger testLogger2 = (TestLogger) LogManager.getLogger("getLogger_String_MessageFactoryMismatch",
                ParameterizedMessageFactory.INSTANCE);
        assertNotNull(testLogger2);
        //TODO: How to test?
        //This test context always creates new loggers, other test context impls I tried fail other tests.
        //assertEquals(messageFactory, testLogger2.getMessageFactory());
        testLogger.debug("%,d", Integer.MAX_VALUE);
        assertEquals(1, testLogger.getEntries().size());
        assertEquals(String.format(" DEBUG %,d", Integer.MAX_VALUE), testLogger.getEntries().get(0));
    }

    @Test
    public void getLogger_String_ParameterizedMessageFactory() {
        final ParameterizedMessageFactory messageFactory =  ParameterizedMessageFactory.INSTANCE;
        final TestLogger testLogger = (TestLogger) LogManager.getLogger("getLogger_String_ParameterizedMessageFactory",
                messageFactory);
        assertNotNull(testLogger);
        assertEquals(messageFactory, testLogger.getMessageFactory());
        testLogger.debug("{}", Integer.MAX_VALUE);
        assertEquals(1, testLogger.getEntries().size());
        assertEquals(" DEBUG " + Integer.MAX_VALUE, testLogger.getEntries().get(0));
    }

    @Test
    public void getLogger_String_SimpleMessageFactory() {
        final SimpleMessageFactory messageFactory = SimpleMessageFactory.INSTANCE;
        final TestLogger testLogger = (TestLogger) LogManager.getLogger("getLogger_String_StringFormatterMessageFactory",
                messageFactory);
        assertNotNull(testLogger);
        assertEquals(messageFactory, testLogger.getMessageFactory());
        testLogger.debug("{} %,d {foo}", Integer.MAX_VALUE);
        assertEquals(1, testLogger.getEntries().size());
        assertEquals(" DEBUG {} %,d {foo}", testLogger.getEntries().get(0));
    }

    @Test
    public void getLogger_String_StringFormatterMessageFactory() {
        final StringFormatterMessageFactory messageFactory = StringFormatterMessageFactory.INSTANCE;
        final TestLogger testLogger = (TestLogger) LogManager.getLogger("getLogger_String_StringFormatterMessageFactory",
                messageFactory);
        assertNotNull(testLogger);
        assertEquals(messageFactory, testLogger.getMessageFactory());
        testLogger.debug("%,d", Integer.MAX_VALUE);
        assertEquals(1, testLogger.getEntries().size());
        assertEquals(String.format(" DEBUG %,d", Integer.MAX_VALUE), testLogger.getEntries().get(0));
    }

    @Test
    public void getLoggerByClass() {
        final Logger classLogger = LogManager.getLogger(LoggerTest.class);
        assertNotNull(classLogger);
    }

    @Test
    public void getLoggerByNullClass() {
        // Returns a SimpleLogger
        assertNotNull(LogManager.getLogger((Class<?>) null));
    }

    @Test
    public void getLoggerByNullObject() {
        // Returns a SimpleLogger
        assertNotNull(LogManager.getLogger((Object) null));
    }

    @Test
    public void getLoggerByNullString() {
        // Returns a SimpleLogger
        assertNotNull(LogManager.getLogger((String) null));
    }

    @Test
    public void getLoggerByObject() {
        final Logger classLogger = LogManager.getLogger(this);
        assertNotNull(classLogger);
        assertEquals(classLogger, LogManager.getLogger(LoggerTest.class));
    }

    @Test
    public void getRootLogger() {
        assertNotNull(LogManager.getRootLogger());
        assertNotNull(LogManager.getLogger(Strings.EMPTY));
        assertNotNull(LogManager.getLogger(LogManager.ROOT_LOGGER_NAME));
        assertEquals(LogManager.getRootLogger(), LogManager.getLogger(Strings.EMPTY));
        assertEquals(LogManager.getRootLogger(), LogManager.getLogger(LogManager.ROOT_LOGGER_NAME));
    }

    @Test
    public void isAllEnabled() {
        assertTrue("Incorrect level", logger.isEnabled(Level.ALL));
    }

    @Test
    public void isDebugEnabled() {
        assertTrue("Incorrect level", logger.isDebugEnabled());
        assertTrue("Incorrect level", logger.isEnabled(Level.DEBUG));
    }

    @Test
    public void isErrorEnabled() {
        assertTrue("Incorrect level", logger.isErrorEnabled());
        assertTrue("Incorrect level", logger.isEnabled(Level.ERROR));
    }

    @Test
    public void isFatalEnabled() {
        assertTrue("Incorrect level", logger.isFatalEnabled());
        assertTrue("Incorrect level", logger.isEnabled(Level.FATAL));
    }

    @Test
    public void isInfoEnabled() {
        assertTrue("Incorrect level", logger.isInfoEnabled());
        assertTrue("Incorrect level", logger.isEnabled(Level.INFO));
    }

    @Test
    public void isOffEnabled() {
        assertTrue("Incorrect level", logger.isEnabled(Level.OFF));
    }

    @Test
    public void isTraceEnabled() {
        assertTrue("Incorrect level", logger.isTraceEnabled());
        assertTrue("Incorrect level", logger.isEnabled(Level.TRACE));
    }

    @Test
    public void isWarnEnabled() {
        assertTrue("Incorrect level", logger.isWarnEnabled());
        assertTrue("Incorrect level", logger.isEnabled(Level.WARN));
    }

    @Test
    public void mdc() {

        ThreadContext.put("TestYear", Integer.valueOf(2010).toString());
        logger.debug("Debug message");
        ThreadContext.clearMap();
        logger.debug("Debug message");
        assertEquals(2, results.size());
        assertTrue("Incorrect MDC: " + results.get(0),
            results.get(0).startsWith(" DEBUG Debug message {TestYear=2010}"));
        assertTrue("MDC not cleared?: " + results.get(1),
            results.get(1).startsWith(" DEBUG Debug message"));
    }

    @Test
    public void printf() {
        logger.printf(Level.DEBUG, "Debug message %d", 1);
        logger.printf(Level.DEBUG, MarkerManager.getMarker("Test"), "Debug message %d", 2);
        assertEquals(2, results.size());
        assertThat("Incorrect message", results.get(0), startsWith(" DEBUG Debug message 1"));
        assertThat("Incorrect message", results.get(1), startsWith("Test DEBUG Debug message 2"));
    }

    @Before
    public void setup() {
        results.clear();
    }

    @Test
    public void structuredData() {
        ThreadContext.put("loginId", "JohnDoe");
        ThreadContext.put("ipAddress", "192.168.0.120");
        ThreadContext.put("locale", Locale.US.getDisplayName());
        final StructuredDataMessage msg = new StructuredDataMessage("Audit@18060", "Transfer Complete", "Transfer");
        msg.put("ToAccount", "123456");
        msg.put("FromAccount", "123457");
        msg.put("Amount", "200.00");
        logger.info(MarkerManager.getMarker("EVENT"), msg);
        ThreadContext.clearMap();
        assertEquals(1, results.size());
        assertThat("Incorrect structured data: ", results.get(0), startsWith(
                "EVENT INFO Transfer [Audit@18060 Amount=\"200.00\" FromAccount=\"123457\" ToAccount=\"123456\"] Transfer Complete"));
    }

    @Test
    public void throwing() {
        logger.throwing(new IllegalArgumentException("Test Exception"));
        assertEquals(1, results.size());
        assertThat("Incorrect Throwing",
                results.get(0), startsWith("THROWING[ EXCEPTION ] ERROR throwing java.lang.IllegalArgumentException: Test Exception"));
    }


    private class Response {
        int status;
        String message;

        public Response(int status, String message) {
            this.status = status;
            this.message = message;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
