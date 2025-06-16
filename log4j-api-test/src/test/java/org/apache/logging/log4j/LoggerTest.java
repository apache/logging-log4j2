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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.apache.logging.log4j.spi.MessageFactory2Adapter;
import org.apache.logging.log4j.test.TestLogger;
import org.apache.logging.log4j.test.junit.Log4jStaticResources;
import org.apache.logging.log4j.test.junit.UsingThreadContextMap;
import org.apache.logging.log4j.util.Strings;
import org.apache.logging.log4j.util.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junitpioneer.jupiter.ReadsSystemProperty;

@ResourceLock(value = Log4jStaticResources.MARKER_MANAGER, mode = ResourceAccessMode.READ)
@ReadsSystemProperty
class LoggerTest {

    private static class TestParameterizedMessageFactory {
        // empty
    }

    private static class TestStringFormatterMessageFactory {
        // empty
    }

    private final TestLogger logger = (TestLogger) LogManager.getLogger(LoggerTest.class);
    private final Marker marker = MarkerManager.getMarker("test");
    private final List<String> results = logger.getEntries();

    @Test
    void builder() {
        logger.atDebug().withLocation().log("Hello");
        logger.atError().withMarker(marker).log("Hello {}", "John");
        logger.atWarn().withThrowable(new Throwable("This is a test")).log((Message) new SimpleMessage("Log4j rocks!"));
        assertEquals(3, results.size());
        assertThat(
                "Incorrect message 1",
                results.get(0),
                equalTo(" DEBUG org.apache.logging.log4j.LoggerTest.builder(LoggerTest.java:72) Hello"));
        assertThat("Incorrect message 2", results.get(1), equalTo("test ERROR Hello John"));
        assertThat(
                "Incorrect message 3",
                results.get(2),
                startsWith(" WARN Log4j rocks! java.lang.Throwable: This is a test"));
        assertThat(
                "Throwable incorrect in message 3",
                results.get(2),
                containsString("org.apache.logging.log4j.LoggerTest.builder(LoggerTest.java:74)"));
    }

    @Test
    void basicFlow() {
        logger.entry();
        logger.exit();
        assertEquals(2, results.size());
        assertThat(results.get(0)).isEqualTo("ENTER[ FLOW ] TRACE Enter");
        assertThat(results.get(1)).isEqualTo("EXIT[ FLOW ] TRACE Exit");
    }

    @Test
    void flowTracingMessage() {
        final Properties props = new Properties();
        props.setProperty("foo", "bar");
        logger.traceEntry(new JsonMessage(props));
        final Response response = new Response(-1, "Generic error");
        logger.traceExit(new JsonMessage(response), response);
        assertThat(results).hasSize(2);
        assertThat(results.get(0)).startsWith("ENTER[ FLOW ] TRACE Enter").contains("\"foo\":\"bar\"");
        assertThat(results.get(1)).startsWith("EXIT[ FLOW ] TRACE Exit").contains("\"message\":\"Generic error\"");
    }

    @Test
    void flowTracingString_ObjectArray1() {
        logger.traceEntry("doFoo(a={}, b={})", 1, 2);
        logger.traceExit("doFoo(a=1, b=2): {}", 3);
        assertThat(results).hasSize(2);
        assertThat(results.get(0)).startsWith("ENTER[ FLOW ] TRACE Enter").contains("doFoo(a=1, b=2)");
        assertThat(results.get(1)).startsWith("EXIT[ FLOW ] TRACE Exit").contains("doFoo(a=1, b=2): 3");
    }

    @Test
    void flowTracingExitValueOnly() {
        logger.traceEntry("doFoo(a={}, b={})", 1, 2);
        logger.traceExit(3);
        assertThat(results).hasSize(2);
        assertThat(results.get(0)).startsWith("ENTER[ FLOW ] TRACE Enter").contains("doFoo(a=1, b=2)");
        assertThat(results.get(1)).startsWith("EXIT[ FLOW ] TRACE Exit").contains("3");
    }

    @Test
    void flowTracingString_ObjectArray2() {
        final EntryMessage msg = logger.traceEntry("doFoo(a={}, b={})", 1, 2);
        logger.traceExit(msg, 3);
        assertThat(results).hasSize(2);
        assertThat(results.get(0)).startsWith("ENTER[ FLOW ] TRACE Enter").contains("doFoo(a=1, b=2)");
        assertThat(results.get(1)).startsWith("EXIT[ FLOW ] TRACE Exit").contains("doFoo(a=1, b=2): 3");
    }

    @Test
    void flowTracingVoidReturn() {
        final EntryMessage msg = logger.traceEntry("doFoo(a={}, b={})", 1, 2);
        logger.traceExit(msg);
        assertThat(results).hasSize(2);
        assertThat(results.get(0)).startsWith("ENTER[ FLOW ] TRACE Enter").contains("doFoo(a=1, b=2)");
        assertThat(results.get(1)).startsWith("EXIT[ FLOW ] TRACE Exit").endsWith("doFoo(a=1, b=2)");
    }

    @Test
    void flowTracingNoExitArgs() {
        logger.traceEntry();
        logger.traceExit();
        assertThat(results).hasSize(2);
        assertThat(results.get(0)).startsWith("ENTER[ FLOW ] TRACE Enter");
        assertThat(results.get(1)).startsWith("EXIT[ FLOW ] TRACE Exit");
    }

    @Test
    void flowTracingNoArgs() {
        final EntryMessage message = logger.traceEntry();
        logger.traceExit(message);
        assertThat(results).hasSize(2);
        assertThat(results.get(0)).startsWith("ENTER[ FLOW ] TRACE Enter");
        assertThat(results.get(1)).startsWith("EXIT[ FLOW ] TRACE Exit");
    }

    @Test
    void flowTracingString_SupplierOfObjectMessages() {
        final EntryMessage msg = logger.traceEntry(
                "doFoo(a={}, b={})", (Supplier<Message>) () -> new ObjectMessage(1), (Supplier<Message>)
                        () -> new ObjectMessage(2));
        logger.traceExit(msg, 3);
        assertThat(results).hasSize(2);
        assertThat(results.get(0)).startsWith("ENTER[ FLOW ] TRACE Enter").contains("doFoo(a=1, b=2)");
        assertThat(results.get(1)).startsWith("EXIT[ FLOW ] TRACE Exit").contains("doFoo(a=1, b=2): 3");
    }

    @Test
    void flowTracingString_SupplierOfStrings() {
        final EntryMessage msg =
                logger.traceEntry("doFoo(a={}, b={})", (Supplier<String>) () -> "1", (Supplier<String>) () -> "2");
        logger.traceExit(msg, 3);
        assertThat(results).hasSize(2);
        assertThat(results.get(0)).startsWith("ENTER[ FLOW ] TRACE Enter").contains("doFoo(a=1, b=2)");
        assertThat(results.get(1)).startsWith("EXIT[ FLOW ] TRACE Exit").contains("doFoo(a=1, b=2): 3");
    }

    @Test
    void flowTracingNoFormat() {
        logger.traceEntry(null, 1, "2", new ObjectMessage(3));
        logger.traceExit((String) null, 4);
        assertThat(results).hasSize(2);
        assertThat(results.get(0)).isEqualTo("ENTER[ FLOW ] TRACE Enter params(1, 2, 3)");
        assertThat(results.get(1)).isEqualTo("EXIT[ FLOW ] TRACE Exit with(4)");
    }

    @Test
    void catching() {
        try {
            throw new NullPointerException();
        } catch (final Exception e) {
            logger.catching(e);
            assertEquals(1, results.size());
            assertThat(
                    "Incorrect Catching",
                    results.get(0),
                    startsWith("CATCHING[ EXCEPTION ] ERROR Catching java.lang.NullPointerException"));
        }
    }

    @Test
    void debug() {
        logger.debug("Debug message");
        assertEquals(1, results.size());
        assertTrue(results.get(0).startsWith(" DEBUG Debug message"), "Incorrect message");
    }

    @Test
    void debugObject() {
        logger.debug(new Date());
        assertEquals(1, results.size());
        assertTrue(results.get(0).length() > 7, "Invalid length");
    }

    @Test
    void debugWithParms() {
        logger.debug("Hello, {}", "World");
        assertEquals(1, results.size());
        assertTrue(results.get(0).startsWith(" DEBUG Hello, World"), "Incorrect substitution");
    }

    @Test
    void debugWithParmsAndThrowable() {
        logger.debug("Hello, {}", "World", new RuntimeException("Test Exception"));
        assertEquals(1, results.size());
        assertTrue(
                results.get(0).startsWith(" DEBUG Hello, World java.lang.RuntimeException: Test Exception"),
                "Unexpected results: " + results.get(0));
    }

    @Test
    @ResourceLock(value = org.junit.jupiter.api.parallel.Resources.LOCALE, mode = ResourceAccessMode.READ)
    void getFormatterLogger() {
        // The TestLogger logger was already created in an instance variable for this class.
        // The message factory is only used when the logger is created.
        final TestLogger testLogger = (TestLogger) LogManager.getFormatterLogger();
        final TestLogger altLogger = (TestLogger) LogManager.getFormatterLogger(getClass());
        assertEquals(testLogger.getName(), altLogger.getName());
        assertNotNull(testLogger);
        assertMessageFactoryInstanceOf(testLogger.getMessageFactory(), StringFormatterMessageFactory.class);
        assertEqualMessageFactory(StringFormatterMessageFactory.INSTANCE, testLogger);
        testLogger.debug("%,d", Integer.MAX_VALUE);
        assertEquals(1, testLogger.getEntries().size());
        assertEquals(
                String.format(" DEBUG %,d", Integer.MAX_VALUE),
                testLogger.getEntries().get(0));
    }

    @Test
    @ResourceLock(value = org.junit.jupiter.api.parallel.Resources.LOCALE, mode = ResourceAccessMode.READ)
    void getFormatterLogger_Class() {
        // The TestLogger logger was already created in an instance variable for this class.
        // The message factory is only used when the logger is created.
        final TestLogger testLogger =
                (TestLogger) LogManager.getFormatterLogger(TestStringFormatterMessageFactory.class);
        assertNotNull(testLogger);
        assertMessageFactoryInstanceOf(testLogger.getMessageFactory(), StringFormatterMessageFactory.class);
        assertEqualMessageFactory(StringFormatterMessageFactory.INSTANCE, testLogger);
        testLogger.debug("%,d", Integer.MAX_VALUE);
        assertEquals(1, testLogger.getEntries().size());
        assertEquals(
                String.format(" DEBUG %,d", Integer.MAX_VALUE),
                testLogger.getEntries().get(0));
    }

    private static void assertMessageFactoryInstanceOf(MessageFactory factory, final Class<?> cls) {
        if (factory instanceof MessageFactory2Adapter) {
            factory = ((MessageFactory2Adapter) factory).getOriginal();
        }
        assertTrue(factory.getClass().isAssignableFrom(cls));
    }

    @Test
    @ResourceLock(value = org.junit.jupiter.api.parallel.Resources.LOCALE, mode = ResourceAccessMode.READ)
    void getFormatterLogger_Object() {
        // The TestLogger logger was already created in an instance variable for this class.
        // The message factory is only used when the logger is created.
        final TestLogger testLogger =
                (TestLogger) LogManager.getFormatterLogger(new TestStringFormatterMessageFactory());
        assertNotNull(testLogger);
        assertMessageFactoryInstanceOf(testLogger.getMessageFactory(), StringFormatterMessageFactory.class);
        assertEqualMessageFactory(StringFormatterMessageFactory.INSTANCE, testLogger);
        testLogger.debug("%,d", Integer.MAX_VALUE);
        assertEquals(1, testLogger.getEntries().size());
        assertEquals(
                String.format(" DEBUG %,d", Integer.MAX_VALUE),
                testLogger.getEntries().get(0));
    }

    @Test
    @ResourceLock(value = org.junit.jupiter.api.parallel.Resources.LOCALE, mode = ResourceAccessMode.READ)
    void getFormatterLogger_String() {
        final StringFormatterMessageFactory messageFactory = StringFormatterMessageFactory.INSTANCE;
        final TestLogger testLogger =
                (TestLogger) LogManager.getFormatterLogger("getLogger_String_StringFormatterMessageFactory");
        assertNotNull(testLogger);
        assertMessageFactoryInstanceOf(testLogger.getMessageFactory(), StringFormatterMessageFactory.class);
        assertEqualMessageFactory(messageFactory, testLogger);
        testLogger.debug("%,d", Integer.MAX_VALUE);
        assertEquals(1, testLogger.getEntries().size());
        assertEquals(
                String.format(" DEBUG %,d", Integer.MAX_VALUE),
                testLogger.getEntries().get(0));
    }

    @Test
    void getLogger_Class_ParameterizedMessageFactory() {
        // The TestLogger logger was already created in an instance variable for this class.
        // The message factory is only used when the logger is created.
        final ParameterizedMessageFactory messageFactory = ParameterizedMessageFactory.INSTANCE;
        final TestLogger testLogger =
                (TestLogger) LogManager.getLogger(TestParameterizedMessageFactory.class, messageFactory);
        assertNotNull(testLogger);
        assertEqualMessageFactory(messageFactory, testLogger);
        testLogger.debug("{}", Integer.MAX_VALUE);
        assertEquals(1, testLogger.getEntries().size());
        assertEquals(" DEBUG " + Integer.MAX_VALUE, testLogger.getEntries().get(0));
    }

    @Test
    void getLogger_Class_StringFormatterMessageFactory() {
        // The TestLogger logger was already created in an instance variable for this class.
        // The message factory is only used when the logger is created.
        final TestLogger testLogger = (TestLogger)
                LogManager.getLogger(TestStringFormatterMessageFactory.class, StringFormatterMessageFactory.INSTANCE);
        assertNotNull(testLogger);
        assertEqualMessageFactory(StringFormatterMessageFactory.INSTANCE, testLogger);
        testLogger.debug("%,d", Integer.MAX_VALUE);
        assertEquals(1, testLogger.getEntries().size());
        assertEquals(
                String.format(" DEBUG %,d", Integer.MAX_VALUE),
                testLogger.getEntries().get(0));
    }

    @Test
    void getLogger_Object_ParameterizedMessageFactory() {
        // The TestLogger logger was already created in an instance variable for this class.
        // The message factory is only used when the logger is created.
        final ParameterizedMessageFactory messageFactory = ParameterizedMessageFactory.INSTANCE;
        final TestLogger testLogger =
                (TestLogger) LogManager.getLogger(new TestParameterizedMessageFactory(), messageFactory);
        assertNotNull(testLogger);
        assertEqualMessageFactory(messageFactory, testLogger);
        testLogger.debug("{}", Integer.MAX_VALUE);
        assertEquals(1, testLogger.getEntries().size());
        assertEquals(" DEBUG " + Integer.MAX_VALUE, testLogger.getEntries().get(0));
    }

    private void assertEqualMessageFactory(final MessageFactory messageFactory, final TestLogger testLogger) {
        MessageFactory actual = testLogger.getMessageFactory();
        if (actual instanceof MessageFactory2Adapter) {
            actual = ((MessageFactory2Adapter) actual).getOriginal();
        }
        assertEquals(messageFactory, actual);
    }

    @Test
    void getLogger_Object_StringFormatterMessageFactory() {
        // The TestLogger logger was already created in an instance variable for this class.
        // The message factory is only used when the logger is created.
        final StringFormatterMessageFactory messageFactory = StringFormatterMessageFactory.INSTANCE;
        final TestLogger testLogger =
                (TestLogger) LogManager.getLogger(new TestStringFormatterMessageFactory(), messageFactory);
        assertNotNull(testLogger);
        assertEqualMessageFactory(messageFactory, testLogger);
        testLogger.debug("%,d", Integer.MAX_VALUE);
        assertEquals(1, testLogger.getEntries().size());
        assertEquals(
                String.format(" DEBUG %,d", Integer.MAX_VALUE),
                testLogger.getEntries().get(0));
    }

    @Test
    void getLogger_String_MessageFactoryMismatch() {
        final StringFormatterMessageFactory messageFactory = StringFormatterMessageFactory.INSTANCE;
        final TestLogger testLogger =
                (TestLogger) LogManager.getLogger("getLogger_String_MessageFactoryMismatch", messageFactory);
        assertNotNull(testLogger);
        assertEqualMessageFactory(messageFactory, testLogger);
        final TestLogger testLogger2 = (TestLogger)
                LogManager.getLogger("getLogger_String_MessageFactoryMismatch", ParameterizedMessageFactory.INSTANCE);
        assertNotNull(testLogger2);
        // TODO: How to test?
        // This test context always creates new loggers, other test context impls I tried fail other tests.
        // assertEquals(messageFactory, testLogger2.getMessageFactory());
        testLogger.debug("%,d", Integer.MAX_VALUE);
        assertEquals(1, testLogger.getEntries().size());
        assertEquals(
                String.format(" DEBUG %,d", Integer.MAX_VALUE),
                testLogger.getEntries().get(0));
    }

    @Test
    void getLogger_String_ParameterizedMessageFactory() {
        final ParameterizedMessageFactory messageFactory = ParameterizedMessageFactory.INSTANCE;
        final TestLogger testLogger =
                (TestLogger) LogManager.getLogger("getLogger_String_ParameterizedMessageFactory", messageFactory);
        assertNotNull(testLogger);
        assertEqualMessageFactory(messageFactory, testLogger);
        testLogger.debug("{}", Integer.MAX_VALUE);
        assertEquals(1, testLogger.getEntries().size());
        assertEquals(" DEBUG " + Integer.MAX_VALUE, testLogger.getEntries().get(0));
    }

    @Test
    void getLogger_String_SimpleMessageFactory() {
        final SimpleMessageFactory messageFactory = SimpleMessageFactory.INSTANCE;
        final TestLogger testLogger =
                (TestLogger) LogManager.getLogger("getLogger_String_StringFormatterMessageFactory", messageFactory);
        assertNotNull(testLogger);
        assertEqualMessageFactory(messageFactory, testLogger);
        testLogger.debug("{} %,d {foo}", Integer.MAX_VALUE);
        assertEquals(1, testLogger.getEntries().size());
        assertEquals(" DEBUG {} %,d {foo}", testLogger.getEntries().get(0));
    }

    @Test
    void getLogger_String_StringFormatterMessageFactory() {
        final StringFormatterMessageFactory messageFactory = StringFormatterMessageFactory.INSTANCE;
        final TestLogger testLogger =
                (TestLogger) LogManager.getLogger("getLogger_String_StringFormatterMessageFactory", messageFactory);
        assertNotNull(testLogger);
        assertEqualMessageFactory(messageFactory, testLogger);
        testLogger.debug("%,d", Integer.MAX_VALUE);
        assertEquals(1, testLogger.getEntries().size());
        assertEquals(
                String.format(" DEBUG %,d", Integer.MAX_VALUE),
                testLogger.getEntries().get(0));
    }

    @Test
    void getLoggerByClass() {
        final Logger classLogger = LogManager.getLogger(LoggerTest.class);
        assertNotNull(classLogger);
    }

    @Test
    void getLoggerByNullClass() {
        // Returns a SimpleLogger
        assertNotNull(LogManager.getLogger((Class<?>) null));
    }

    @Test
    void getLoggerByNullObject() {
        // Returns a SimpleLogger
        assertNotNull(LogManager.getLogger((Object) null));
    }

    @Test
    void getLoggerByNullString() {
        // Returns a SimpleLogger
        assertNotNull(LogManager.getLogger((String) null));
    }

    @Test
    void getLoggerByObject() {
        final Logger classLogger = LogManager.getLogger(this);
        assertNotNull(classLogger);
        assertEquals(classLogger, LogManager.getLogger(LoggerTest.class));
    }

    @Test
    void getRootLogger() {
        assertNotNull(LogManager.getRootLogger());
        assertNotNull(LogManager.getLogger(Strings.EMPTY));
        assertNotNull(LogManager.getLogger(LogManager.ROOT_LOGGER_NAME));
        assertEquals(LogManager.getRootLogger(), LogManager.getLogger(Strings.EMPTY));
        assertEquals(LogManager.getRootLogger(), LogManager.getLogger(LogManager.ROOT_LOGGER_NAME));
    }

    @Test
    void isAllEnabled() {
        assertTrue(logger.isEnabled(Level.ALL), "Incorrect level");
    }

    @Test
    void isDebugEnabled() {
        assertTrue(logger.isDebugEnabled(), "Incorrect level");
        assertTrue(logger.isEnabled(Level.DEBUG), "Incorrect level");
    }

    @Test
    void isErrorEnabled() {
        assertTrue(logger.isErrorEnabled(), "Incorrect level");
        assertTrue(logger.isEnabled(Level.ERROR), "Incorrect level");
    }

    @Test
    void isFatalEnabled() {
        assertTrue(logger.isFatalEnabled(), "Incorrect level");
        assertTrue(logger.isEnabled(Level.FATAL), "Incorrect level");
    }

    @Test
    void isInfoEnabled() {
        assertTrue(logger.isInfoEnabled(), "Incorrect level");
        assertTrue(logger.isEnabled(Level.INFO), "Incorrect level");
    }

    @Test
    void isOffEnabled() {
        assertTrue(logger.isEnabled(Level.OFF), "Incorrect level");
    }

    @Test
    void isTraceEnabled() {
        assertTrue(logger.isTraceEnabled(), "Incorrect level");
        assertTrue(logger.isEnabled(Level.TRACE), "Incorrect level");
    }

    @Test
    void isWarnEnabled() {
        assertTrue(logger.isWarnEnabled(), "Incorrect level");
        assertTrue(logger.isEnabled(Level.WARN), "Incorrect level");
    }

    @Test
    void isAllEnabledWithMarker() {
        assertTrue(logger.isEnabled(Level.ALL, marker), "Incorrect level");
    }

    @Test
    void isDebugEnabledWithMarker() {
        assertTrue(logger.isDebugEnabled(marker), "Incorrect level");
        assertTrue(logger.isEnabled(Level.DEBUG, marker), "Incorrect level");
    }

    @Test
    void isErrorEnabledWithMarker() {
        assertTrue(logger.isErrorEnabled(marker), "Incorrect level");
        assertTrue(logger.isEnabled(Level.ERROR, marker), "Incorrect level");
    }

    @Test
    void isFatalEnabledWithMarker() {
        assertTrue(logger.isFatalEnabled(marker), "Incorrect level");
        assertTrue(logger.isEnabled(Level.FATAL, marker), "Incorrect level");
    }

    @Test
    void isInfoEnabledWithMarker() {
        assertTrue(logger.isInfoEnabled(marker), "Incorrect level");
        assertTrue(logger.isEnabled(Level.INFO, marker), "Incorrect level");
    }

    @Test
    void isOffEnabledWithMarker() {
        assertTrue(logger.isEnabled(Level.OFF, marker), "Incorrect level");
    }

    @Test
    void isTraceEnabledWithMarker() {
        assertTrue(logger.isTraceEnabled(marker), "Incorrect level");
        assertTrue(logger.isEnabled(Level.TRACE, marker), "Incorrect level");
    }

    @Test
    void isWarnEnabledWithMarker() {
        assertTrue(logger.isWarnEnabled(marker), "Incorrect level");
        assertTrue(logger.isEnabled(Level.WARN, marker), "Incorrect level");
    }

    @Test
    @UsingThreadContextMap
    void mdc() {
        ThreadContext.put("TestYear", Integer.toString(2010));
        logger.debug("Debug message");
        final String testYear = ThreadContext.get("TestYear");
        assertNotNull(testYear, "Test Year is null");
        assertEquals("2010", testYear, "Incorrect test year: " + testYear);
        ThreadContext.clearMap();
        logger.debug("Debug message");
        assertEquals(2, results.size());
        System.out.println("Log line 1: " + results.get(0));
        System.out.println("log line 2: " + results.get(1));
        assertTrue(
                results.get(0).startsWith(" DEBUG Debug message {TestYear=2010}"), "Incorrect MDC: " + results.get(0));
        assertTrue(results.get(1).startsWith(" DEBUG Debug message"), "MDC not cleared?: " + results.get(1));
    }

    @Test
    void printf() {
        logger.printf(Level.DEBUG, "Debug message %d", 1);
        logger.printf(Level.DEBUG, MarkerManager.getMarker("Test"), "Debug message %d", 2);
        assertEquals(2, results.size());
        assertThat("Incorrect message", results.get(0), startsWith(" DEBUG Debug message 1"));
        assertThat("Incorrect message", results.get(1), startsWith("Test DEBUG Debug message 2"));
    }

    @BeforeEach
    void setup() {
        results.clear();
    }

    @Test
    void structuredData() {
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
        assertThat(
                "Incorrect structured data: ",
                results.get(0),
                startsWith(
                        "EVENT INFO Transfer [Audit@18060 Amount=\"200.00\" FromAccount=\"123457\" ToAccount=\"123456\"] Transfer Complete"));
    }

    @Test
    void throwing() {
        logger.throwing(new IllegalArgumentException("Test Exception"));
        assertEquals(1, results.size());
        assertThat(
                "Incorrect Throwing",
                results.get(0),
                startsWith("THROWING[ EXCEPTION ] ERROR Throwing java.lang.IllegalArgumentException: Test Exception"));
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
