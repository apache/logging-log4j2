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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import org.apache.logging.log4j.message.EntryMessage;
import org.apache.logging.log4j.message.JsonMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.message.ParameterizedMessageFactory;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.message.SimpleMessageFactory;
import org.apache.logging.log4j.message.StringFormatterMessageFactory;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.apache.logging.log4j.util.Strings;
import org.apache.logging.log4j.util.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

@ResourceLock("log4j2.MarkerManager")
@ResourceLock("log4j2.TestLogger")
public class LoggerTest {

    private static class TestParameterizedMessageFactory {
        // empty
    }

    private static class TestStringFormatterMessageFactory {
        // empty
    }

    private final TestLogger logger = (TestLogger) LogManager.getLogger("LoggerTest");
    private final Marker marker = MarkerManager.getMarker("test");
    private final List<String> results = logger.getEntries();

    @Test
    public void builder() {
        logger.atDebug().withLocation().log("Hello");
        logger.atError().withMarker(marker).log("Hello {}", "John");
        logger.atWarn().withThrowable(new Throwable("This is a test")).log((Message) new SimpleMessage("Log4j rocks!"));
        assertThat(results.size()).isEqualTo(3);
        assertThat(results.get(0)).describedAs("Incorrect message 1").isEqualTo(" DEBUG org.apache.logging.log4j.LoggerTest.builder(LoggerTest.java:64) Hello");
        assertThat(results.get(1)).describedAs("Incorrect message 2").isEqualTo("test ERROR Hello John");
        assertThat(results.get(2)).describedAs("Incorrect message 3").startsWith(" WARN Log4j rocks! java.lang.Throwable: This is a test");
        assertThat(results.get(2)).describedAs("Throwable incorrect in message 3").contains("at org.apache.logging.log4j.LoggerTest.builder(LoggerTest.java:66)");
    }

    @Test
    public void basicFlow() {
        logger.traceEntry();
        logger.traceExit();
        assertThat(results.size()).isEqualTo(2);
        assertThat(results.get(0)).describedAs("Incorrect Entry").isEqualTo("ENTER[ FLOW ] TRACE Enter");
        assertThat(results.get(1)).describedAs("incorrect Exit").isEqualTo("EXIT[ FLOW ] TRACE Exit");

    }

    @Test
    public void flowTracingMessage() {
        Properties props = new Properties();
        props.setProperty("foo", "bar");
        logger.traceEntry(new JsonMessage(props));
        final Response response = new Response(-1, "Generic error");
        logger.traceExit(new JsonMessage(response),  response);
        assertThat(results.size()).isEqualTo(2);
        assertThat(results.get(0)).describedAs("Incorrect Entry").startsWith("ENTER[ FLOW ] TRACE Enter");
        assertThat(results.get(0)).describedAs("Missing entry data").contains("\"foo\":\"bar\"");
        assertThat(results.get(1)).describedAs("incorrect Exit").startsWith("EXIT[ FLOW ] TRACE Exit");
        assertThat(results.get(1)).describedAs("Missing exit data").contains("\"message\":\"Generic error\"");
    }

    @Test
    public void flowTracingString_ObjectArray1() {
        logger.traceEntry("doFoo(a={}, b={})", 1, 2);
        logger.traceExit("doFoo(a=1, b=2): {}", 3);
        assertThat(results.size()).isEqualTo(2);
        assertThat(results.get(0)).describedAs("Incorrect Entry").startsWith("ENTER[ FLOW ] TRACE Enter");
        assertThat(results.get(0)).describedAs("Missing entry data").contains("doFoo(a=1, b=2)");
        assertThat(results.get(1)).describedAs("Incorrect Exit").startsWith("EXIT[ FLOW ] TRACE Exit");
        assertThat(results.get(1)).describedAs("Missing exit data").contains("doFoo(a=1, b=2): 3");
    }

    @Test
    public void flowTracingExitValueOnly() {
        logger.traceEntry("doFoo(a={}, b={})", 1, 2);
        logger.traceExit(3);
        assertThat(results.size()).isEqualTo(2);
        assertThat(results.get(0)).describedAs("Incorrect Entry").startsWith("ENTER[ FLOW ] TRACE Enter");
        assertThat(results.get(0)).describedAs("Missing entry data").contains("doFoo(a=1, b=2)");
        assertThat(results.get(1)).describedAs("Incorrect Exit").startsWith("EXIT[ FLOW ] TRACE Exit");
        assertThat(results.get(1)).describedAs("Missing exit data").contains("3");
    }

    @Test
    public void flowTracingString_ObjectArray2() {
        final EntryMessage msg = logger.traceEntry("doFoo(a={}, b={})", 1, 2);
        logger.traceExit(msg, 3);
        assertThat(results.size()).isEqualTo(2);
        assertThat(results.get(0)).describedAs("Incorrect Entry").startsWith("ENTER[ FLOW ] TRACE Enter");
        assertThat(results.get(0)).describedAs("Missing entry data").contains("doFoo(a=1, b=2)");
        assertThat(results.get(1)).describedAs("Incorrect Exit").startsWith("EXIT[ FLOW ] TRACE Exit");
        assertThat(results.get(1)).describedAs("Missing exit data").contains("doFoo(a=1, b=2): 3");
    }

    @Test
    public void flowTracingVoidReturn() {
        final EntryMessage msg = logger.traceEntry("doFoo(a={}, b={})", 1, 2);
        logger.traceExit(msg);
        assertThat(results.size()).isEqualTo(2);
        assertThat(results.get(0)).describedAs("Incorrect Entry").startsWith("ENTER[ FLOW ] TRACE Enter");
        assertThat(results.get(0)).describedAs("Missing entry data").contains("doFoo(a=1, b=2)");
        assertThat(results.get(1)).describedAs("Incorrect Exit").startsWith("EXIT[ FLOW ] TRACE Exit");
        assertThat(results.get(1)).describedAs("Missing exit data").endsWith("doFoo(a=1, b=2)");
    }

    @Test
    public void flowTracingNoExitArgs() {
        logger.traceEntry();
        logger.traceExit();
        assertThat(results.size()).isEqualTo(2);
        assertThat(results.get(0)).describedAs("Incorrect Entry").startsWith("ENTER[ FLOW ] TRACE Enter");
        assertThat(results.get(1)).describedAs("Incorrect Exit").startsWith("EXIT[ FLOW ] TRACE Exit");
    }

    @Test
    public void flowTracingNoArgs() {
        final EntryMessage message = logger.traceEntry();
        logger.traceExit(message);
        assertThat(results.size()).isEqualTo(2);
        assertThat(results.get(0)).describedAs("Incorrect Entry").startsWith("ENTER[ FLOW ] TRACE Enter");
        assertThat(results.get(1)).describedAs("Incorrect Exit").startsWith("EXIT[ FLOW ] TRACE Exit");
    }

    @Test
    public void flowTracingString_SupplierOfObjectMessages() {
        final EntryMessage msg = logger.traceEntry("doFoo(a={}, b={})", new Supplier<Message>() {
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
        logger.traceExit(msg, 3);
        assertThat(results.size()).isEqualTo(2);
        assertThat(results.get(0)).describedAs("Incorrect Entry").startsWith("ENTER[ FLOW ] TRACE Enter");
        assertThat(results.get(0)).describedAs("Missing entry data").contains("doFoo(a=1, b=2)");
        assertThat(results.get(1)).describedAs("Incorrect Exit").startsWith("EXIT[ FLOW ] TRACE Exit");
        assertThat(results.get(1)).describedAs("Missing exit data").contains("doFoo(a=1, b=2): 3");
    }

    @Test
    public void flowTracingString_SupplierOfStrings() {
        final EntryMessage msg = logger.traceEntry("doFoo(a={}, b={})", new Supplier<String>() {
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
        logger.traceExit(msg, 3);
        assertThat(results.size()).isEqualTo(2);
        assertThat(results.get(0)).describedAs("Incorrect Entry").startsWith("ENTER[ FLOW ] TRACE Enter");
        assertThat(results.get(0)).describedAs("Missing entry data").contains("doFoo(a=1, b=2)");
        assertThat(results.get(1)).describedAs("Incorrect Exit").startsWith("EXIT[ FLOW ] TRACE Exit");
        assertThat(results.get(1)).describedAs("Missing exit data").contains("doFoo(a=1, b=2): 3");
    }

    @Test
    public void catching() {
        try {
            throw new NullPointerException();
        } catch (final Exception e) {
            logger.catching(e);
            assertThat(results.size()).isEqualTo(1);
            assertThat(results.get(0)).describedAs("Incorrect Catching").startsWith("CATCHING[ EXCEPTION ] ERROR Catching java.lang.NullPointerException");
        }
    }

    @Test
    public void debug() {
        logger.debug("Debug message");
        assertThat(results.size()).isEqualTo(1);
        assertTrue(results.get(0).startsWith(" DEBUG Debug message"), "Incorrect message");
    }

    @Test
    public void debugObject() {
        logger.debug(new Date());
        assertThat(results.size()).isEqualTo(1);
        assertTrue(results.get(0).length() > 7, "Invalid length");
    }

    @Test
    public void debugWithParms() {
        logger.debug("Hello, {}", "World");
        assertThat(results.size()).isEqualTo(1);
        assertTrue(results.get(0).startsWith(" DEBUG Hello, World"), "Incorrect substitution");
    }

    @Test
    public void debugWithParmsAndThrowable() {
        logger.debug("Hello, {}", "World", new RuntimeException("Test Exception"));
        assertThat(results.size()).isEqualTo(1);
        assertTrue(
                results.get(0).startsWith(" DEBUG Hello, World java.lang.RuntimeException: Test Exception"),
                "Unexpected results: " + results.get(0));
    }

    @Test
    public void getFormatterLogger() {
        // The TestLogger logger was already created in an instance variable for this class.
        // The message factory is only used when the logger is created.
        final TestLogger testLogger = (TestLogger) LogManager.getFormatterLogger();
        final TestLogger altLogger = (TestLogger) LogManager.getFormatterLogger(getClass());
        assertThat(altLogger.getName()).isEqualTo(testLogger.getName());
        assertThat(testLogger).isNotNull();
        assertMessageFactoryInstanceOf(testLogger.getMessageFactory(), StringFormatterMessageFactory.class);
        assertEqualMessageFactory(StringFormatterMessageFactory.INSTANCE, testLogger);
        testLogger.debug("%,d", Integer.MAX_VALUE);
        assertThat(testLogger.getEntries().size()).isEqualTo(1);
        assertThat(testLogger.getEntries().get(0)).isEqualTo(String.format(" DEBUG %,d", Integer.MAX_VALUE));
    }

    @Test
    public void getFormatterLogger_Class() {
        // The TestLogger logger was already created in an instance variable for this class.
        // The message factory is only used when the logger is created.
        final TestLogger testLogger = (TestLogger) LogManager.getFormatterLogger(TestStringFormatterMessageFactory.class);
        assertThat(testLogger).isNotNull();
        assertMessageFactoryInstanceOf(testLogger.getMessageFactory(), StringFormatterMessageFactory.class);
        assertEqualMessageFactory(StringFormatterMessageFactory.INSTANCE, testLogger);
        testLogger.debug("%,d", Integer.MAX_VALUE);
        assertThat(testLogger.getEntries().size()).isEqualTo(1);
        assertThat(testLogger.getEntries().get(0)).isEqualTo(String.format(" DEBUG %,d", Integer.MAX_VALUE));
    }

    private static void assertMessageFactoryInstanceOf(MessageFactory factory, final Class<?> cls) {
        assertThat(factory.getClass().isAssignableFrom(cls)).isTrue();
    }

    @Test
    public void getFormatterLogger_Object() {
        // The TestLogger logger was already created in an instance variable for this class.
        // The message factory is only used when the logger is created.
        final TestLogger testLogger = (TestLogger) LogManager.getFormatterLogger(new TestStringFormatterMessageFactory());
        assertThat(testLogger).isNotNull();
        assertMessageFactoryInstanceOf(testLogger.getMessageFactory(), StringFormatterMessageFactory.class);
        assertEqualMessageFactory(StringFormatterMessageFactory.INSTANCE, testLogger);
        testLogger.debug("%,d", Integer.MAX_VALUE);
        assertThat(testLogger.getEntries().size()).isEqualTo(1);
        assertThat(testLogger.getEntries().get(0)).isEqualTo(String.format(" DEBUG %,d", Integer.MAX_VALUE));
    }

    @Test
    public void getFormatterLogger_String() {
        final StringFormatterMessageFactory messageFactory = StringFormatterMessageFactory.INSTANCE;
        final TestLogger testLogger = (TestLogger) LogManager.getFormatterLogger("getLogger_String_StringFormatterMessageFactory");
        assertThat(testLogger).isNotNull();
        assertMessageFactoryInstanceOf(testLogger.getMessageFactory(), StringFormatterMessageFactory.class);
        assertEqualMessageFactory(messageFactory, testLogger);
        testLogger.debug("%,d", Integer.MAX_VALUE);
        assertThat(testLogger.getEntries().size()).isEqualTo(1);
        assertThat(testLogger.getEntries().get(0)).isEqualTo(String.format(" DEBUG %,d", Integer.MAX_VALUE));
    }

    @Test
    public void getLogger_Class_ParameterizedMessageFactory() {
        // The TestLogger logger was already created in an instance variable for this class.
        // The message factory is only used when the logger is created.
        final ParameterizedMessageFactory messageFactory = ParameterizedMessageFactory.INSTANCE;
        final TestLogger testLogger = (TestLogger) LogManager.getLogger(TestParameterizedMessageFactory.class,
                messageFactory);
        assertThat(testLogger).isNotNull();
        assertEqualMessageFactory(messageFactory, testLogger);
        testLogger.debug("{}", Integer.MAX_VALUE);
        assertThat(testLogger.getEntries().size()).isEqualTo(1);
        assertThat(testLogger.getEntries().get(0)).isEqualTo(" DEBUG " + Integer.MAX_VALUE);
    }

    @Test
    public void getLogger_Class_StringFormatterMessageFactory() {
        // The TestLogger logger was already created in an instance variable for this class.
        // The message factory is only used when the logger is created.
        final TestLogger testLogger = (TestLogger) LogManager.getLogger(TestStringFormatterMessageFactory.class,
                StringFormatterMessageFactory.INSTANCE);
        assertThat(testLogger).isNotNull();
        assertEqualMessageFactory(StringFormatterMessageFactory.INSTANCE, testLogger);
        testLogger.debug("%,d", Integer.MAX_VALUE);
        assertThat(testLogger.getEntries().size()).isEqualTo(1);
        assertThat(testLogger.getEntries().get(0)).isEqualTo(String.format(" DEBUG %,d", Integer.MAX_VALUE));
    }

    @Test
    public void getLogger_Object_ParameterizedMessageFactory() {
        // The TestLogger logger was already created in an instance variable for this class.
        // The message factory is only used when the logger is created.
        final ParameterizedMessageFactory messageFactory =  ParameterizedMessageFactory.INSTANCE;
        final TestLogger testLogger = (TestLogger) LogManager.getLogger(new TestParameterizedMessageFactory(),
                messageFactory);
        assertThat(testLogger).isNotNull();
        assertEqualMessageFactory(messageFactory, testLogger);
        testLogger.debug("{}", Integer.MAX_VALUE);
        assertThat(testLogger.getEntries().size()).isEqualTo(1);
        assertThat(testLogger.getEntries().get(0)).isEqualTo(" DEBUG " + Integer.MAX_VALUE);
    }

    private void assertEqualMessageFactory(final MessageFactory messageFactory, final TestLogger testLogger) {
        MessageFactory actual = testLogger.getMessageFactory();
        assertThat(actual).isEqualTo(messageFactory);
    }

    @Test
    public void getLogger_Object_StringFormatterMessageFactory() {
        // The TestLogger logger was already created in an instance variable for this class.
        // The message factory is only used when the logger is created.
        final StringFormatterMessageFactory messageFactory = StringFormatterMessageFactory.INSTANCE;
        final TestLogger testLogger = (TestLogger) LogManager.getLogger(new TestStringFormatterMessageFactory(),
                messageFactory);
        assertThat(testLogger).isNotNull();
        assertEqualMessageFactory(messageFactory, testLogger);
        testLogger.debug("%,d", Integer.MAX_VALUE);
        assertThat(testLogger.getEntries().size()).isEqualTo(1);
        assertThat(testLogger.getEntries().get(0)).isEqualTo(String.format(" DEBUG %,d", Integer.MAX_VALUE));
    }

    @Test
    public void getLogger_String_MessageFactoryMismatch() {
        final StringFormatterMessageFactory messageFactory = StringFormatterMessageFactory.INSTANCE;
        final TestLogger testLogger = (TestLogger) LogManager.getLogger("getLogger_String_MessageFactoryMismatch",
                messageFactory);
        assertThat(testLogger).isNotNull();
        assertEqualMessageFactory(messageFactory, testLogger);
        final TestLogger testLogger2 = (TestLogger) LogManager.getLogger("getLogger_String_MessageFactoryMismatch",
                ParameterizedMessageFactory.INSTANCE);
        assertThat(testLogger2).isNotNull();
        //TODO: How to test?
        //This test context always creates new loggers, other test context impls I tried fail other tests.
        //assertEquals(messageFactory, testLogger2.getMessageFactory());
        testLogger.debug("%,d", Integer.MAX_VALUE);
        assertThat(testLogger.getEntries().size()).isEqualTo(1);
        assertThat(testLogger.getEntries().get(0)).isEqualTo(String.format(" DEBUG %,d", Integer.MAX_VALUE));
    }

    @Test
    public void getLogger_String_ParameterizedMessageFactory() {
        final ParameterizedMessageFactory messageFactory =  ParameterizedMessageFactory.INSTANCE;
        final TestLogger testLogger = (TestLogger) LogManager.getLogger("getLogger_String_ParameterizedMessageFactory",
                messageFactory);
        assertThat(testLogger).isNotNull();
        assertEqualMessageFactory(messageFactory, testLogger);
        testLogger.debug("{}", Integer.MAX_VALUE);
        assertThat(testLogger.getEntries().size()).isEqualTo(1);
        assertThat(testLogger.getEntries().get(0)).isEqualTo(" DEBUG " + Integer.MAX_VALUE);
    }

    @Test
    public void getLogger_String_SimpleMessageFactory() {
        final SimpleMessageFactory messageFactory = SimpleMessageFactory.INSTANCE;
        final TestLogger testLogger = (TestLogger) LogManager.getLogger("getLogger_String_StringFormatterMessageFactory",
                messageFactory);
        assertThat(testLogger).isNotNull();
        assertEqualMessageFactory(messageFactory, testLogger);
        testLogger.debug("{} %,d {foo}", Integer.MAX_VALUE);
        assertThat(testLogger.getEntries().size()).isEqualTo(1);
        assertThat(testLogger.getEntries().get(0)).isEqualTo(" DEBUG {} %,d {foo}");
    }

    @Test
    public void getLogger_String_StringFormatterMessageFactory() {
        final StringFormatterMessageFactory messageFactory = StringFormatterMessageFactory.INSTANCE;
        final TestLogger testLogger = (TestLogger) LogManager.getLogger("getLogger_String_StringFormatterMessageFactory",
                messageFactory);
        assertThat(testLogger).isNotNull();
        assertEqualMessageFactory(messageFactory, testLogger);
        testLogger.debug("%,d", Integer.MAX_VALUE);
        assertThat(testLogger.getEntries().size()).isEqualTo(1);
        assertThat(testLogger.getEntries().get(0)).isEqualTo(String.format(" DEBUG %,d", Integer.MAX_VALUE));
    }

    @Test
    public void getLoggerByClass() {
        final Logger classLogger = LogManager.getLogger(LoggerTest.class);
        assertThat(classLogger).isNotNull();
    }

    @Test
    public void getLoggerByNullClass() {
        // Returns a SimpleLogger
        assertThat(LogManager.getLogger((Class<?>) null)).isNotNull();
    }

    @Test
    public void getLoggerByNullObject() {
        // Returns a SimpleLogger
        assertThat(LogManager.getLogger((Object) null)).isNotNull();
    }

    @Test
    public void getLoggerByNullString() {
        // Returns a SimpleLogger
        assertThat(LogManager.getLogger((String) null)).isNotNull();
    }

    @Test
    public void getLoggerByObject() {
        final Logger classLogger = LogManager.getLogger(this);
        assertThat(classLogger).isNotNull();
        assertThat(LogManager.getLogger(LoggerTest.class)).isEqualTo(classLogger);
    }

    @Test
    public void getRootLogger() {
        assertThat(LogManager.getRootLogger()).isNotNull();
        assertThat(LogManager.getLogger(Strings.EMPTY)).isNotNull();
        assertThat(LogManager.getLogger(LogManager.ROOT_LOGGER_NAME)).isNotNull();
        assertThat(LogManager.getLogger(Strings.EMPTY)).isEqualTo(LogManager.getRootLogger());
        assertThat(LogManager.getLogger(LogManager.ROOT_LOGGER_NAME)).isEqualTo(LogManager.getRootLogger());
    }

    @Test
    public void isAllEnabled() {
        assertTrue(logger.isEnabled(Level.ALL), "Incorrect level");
    }

    @Test
    public void isDebugEnabled() {
        assertTrue(logger.isDebugEnabled(), "Incorrect level");
        assertTrue(logger.isEnabled(Level.DEBUG), "Incorrect level");
    }

    @Test
    public void isErrorEnabled() {
        assertTrue(logger.isErrorEnabled(), "Incorrect level");
        assertTrue(logger.isEnabled(Level.ERROR), "Incorrect level");
    }

    @Test
    public void isFatalEnabled() {
        assertTrue(logger.isFatalEnabled(), "Incorrect level");
        assertTrue(logger.isEnabled(Level.FATAL), "Incorrect level");
    }

    @Test
    public void isInfoEnabled() {
        assertTrue(logger.isInfoEnabled(), "Incorrect level");
        assertTrue(logger.isEnabled(Level.INFO), "Incorrect level");
    }

    @Test
    public void isOffEnabled() {
        assertTrue(logger.isEnabled(Level.OFF), "Incorrect level");
    }

    @Test
    public void isTraceEnabled() {
        assertTrue(logger.isTraceEnabled(), "Incorrect level");
        assertTrue(logger.isEnabled(Level.TRACE), "Incorrect level");
    }

    @Test
    public void isWarnEnabled() {
        assertTrue(logger.isWarnEnabled(), "Incorrect level");
        assertTrue(logger.isEnabled(Level.WARN), "Incorrect level");
    }

    @Test
    public void isAllEnabledWithMarker() {
        assertTrue(logger.isEnabled(Level.ALL, marker), "Incorrect level");
    }

    @Test
    public void isDebugEnabledWithMarker() {
        assertTrue(logger.isDebugEnabled(marker), "Incorrect level");
        assertTrue(logger.isEnabled(Level.DEBUG, marker), "Incorrect level");
    }

    @Test
    public void isErrorEnabledWithMarker() {
        assertTrue(logger.isErrorEnabled(marker), "Incorrect level");
        assertTrue(logger.isEnabled(Level.ERROR, marker), "Incorrect level");
    }

    @Test
    public void isFatalEnabledWithMarker() {
        assertTrue(logger.isFatalEnabled(marker), "Incorrect level");
        assertTrue(logger.isEnabled(Level.FATAL, marker), "Incorrect level");
    }

    @Test
    public void isInfoEnabledWithMarker() {
        assertTrue(logger.isInfoEnabled(marker), "Incorrect level");
        assertTrue(logger.isEnabled(Level.INFO, marker), "Incorrect level");
    }

    @Test
    public void isOffEnabledWithMarker() {
        assertTrue(logger.isEnabled(Level.OFF, marker), "Incorrect level");
    }

    @Test
    public void isTraceEnabledWithMarker() {
        assertTrue(logger.isTraceEnabled(marker), "Incorrect level");
        assertTrue(logger.isEnabled(Level.TRACE, marker), "Incorrect level");
    }

    @Test
    public void isWarnEnabledWithMarker() {
        assertTrue(logger.isWarnEnabled(marker), "Incorrect level");
        assertTrue(logger.isEnabled(Level.WARN, marker), "Incorrect level");
    }

    @Test
    public void mdc() {

        ThreadContext.put("TestYear", Integer.valueOf(2010).toString());
        logger.debug("Debug message");
        String testYear = ThreadContext.get("TestYear");
        assertThat(testYear).describedAs("Test Year is null").isNotNull();
        assertThat(testYear).describedAs("Incorrect test year: " + testYear).isEqualTo("2010");
        ThreadContext.clearMap();
        logger.debug("Debug message");
        assertThat(results.size()).isEqualTo(2);
        System.out.println("Log line 1: " + results.get(0));
        System.out.println("log line 2: " + results.get(1));
        assertTrue(
                results.get(0).startsWith(" DEBUG Debug message {TestYear=2010}"), "Incorrect MDC: " + results.get(0));
        assertTrue(
                results.get(1).startsWith(" DEBUG Debug message"), "MDC not cleared?: " + results.get(1));
    }

    @Test
    public void printf() {
        logger.printf(Level.DEBUG, "Debug message %d", 1);
        logger.printf(Level.DEBUG, MarkerManager.getMarker("Test"), "Debug message %d", 2);
        assertThat(results.size()).isEqualTo(2);
        assertThat(results.get(0)).describedAs("Incorrect message").startsWith(" DEBUG Debug message 1");
        assertThat(results.get(1)).describedAs("Incorrect message").startsWith("Test DEBUG Debug message 2");
    }

    @BeforeEach
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
        assertThat(results.size()).isEqualTo(1);
        assertThat(results.get(0)).describedAs("Incorrect structured data: ").startsWith("EVENT INFO Transfer [Audit@18060 Amount=\"200.00\" FromAccount=\"123457\" ToAccount=\"123456\"] Transfer Complete");
    }

    @Test
    public void throwing() {
        logger.throwing(new IllegalArgumentException("Test Exception"));
        assertThat(results.size()).isEqualTo(1);
        assertThat(results.get(0)).describedAs("Incorrect Throwing").startsWith("THROWING[ EXCEPTION ] ERROR Throwing java.lang.IllegalArgumentException: Test Exception");
    }


    private static class Response {
        int status;
        String message;

        public Response(final int status, final String message) {
            this.status = status;
            this.message = message;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(final int status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(final String message) {
            this.message = message;
        }
    }
}
