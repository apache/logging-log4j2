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
package org.apache.logging.log4j.core.pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.core.impl.ContextDataFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.impl.ThrowableFormatOptions;
import org.apache.logging.log4j.core.util.DummyNanoClock;
import org.apache.logging.log4j.core.util.SystemNanoClock;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.StringMap;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PatternParserTest {

    static String OUTPUT_FILE = "output/PatternParser";
    static String WITNESS_FILE = "witness/PatternParser";
    LoggerContext ctx = LoggerContext.getContext();
    Logger root = ctx.getRootLogger();

    private static final String msgPattern = "%m%n";
    private final String mdcMsgPattern1 = "%m : %X%n";
    private final String mdcMsgPattern2 = "%m : %X{key1}%n";
    private final String mdcMsgPattern3 = "%m : %X{key2}%n";
    private final String mdcMsgPattern4 = "%m : %X{key3}%n";
    private final String mdcMsgPattern5 = "%m : %X{key1},%X{key2},%X{key3}%n";
    private final String deeplyNestedPattern = "%notEmpty{ %maxLen{%X{var}}{3} }";

    private static final String badPattern = "[%d{yyyyMMdd HH:mm:ss,SSS] %-5p [%c{10}] - %m%n";
    private static final String customPattern = "[%d{yyyyMMdd HH:mm:ss,SSS}] %-5p [%-25.25c{1}:%-4L] - %m%n";
    private static final String patternTruncateFromEnd = "%d; %-5p %5.-5c %m%n";
    private static final String patternTruncateFromBeginning = "%d; %-5p %5.5c %m%n";
    private static final String nestedPatternHighlight =
            "%highlight{%d{dd MMM yyyy HH:mm:ss,SSS}{GMT+0} [%t] %-5level: %msg%n%throwable}";

    private static final String KEY = "Converter";
    private PatternParser parser;

    @BeforeEach
    void setup() {
        parser = new PatternParser(KEY);
    }

    private void validateConverter(final List<PatternFormatter> formatter, final int index, final String name) {
        final PatternConverter pc = formatter.get(index).getConverter();
        assertEquals(
                pc.getName(), name, "Incorrect converter " + pc.getName() + " at index " + index + " expected " + name);
    }

    /**
     * Test the default pattern
     */
    @Test
    void defaultPattern() {
        final List<PatternFormatter> formatters = parser.parse(msgPattern);
        assertNotNull(formatters);
        assertEquals(2, formatters.size());
        validateConverter(formatters, 0, "Message");
        validateConverter(formatters, 1, "Line Sep");
    }

    /**
     * Test the custom pattern
     */
    @Test
    void testCustomPattern() {
        final List<PatternFormatter> formatters = parser.parse(customPattern);
        assertNotNull(formatters);
        final StringMap mdc = ContextDataFactory.createContextData();
        mdc.putValue("loginId", "Fred");
        final Throwable t = new Throwable();
        final StackTraceElement[] elements = t.getStackTrace();
        final Log4jLogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("org.apache.logging.log4j.PatternParserTest") //
                .setMarker(MarkerManager.getMarker("TEST")) //
                .setLoggerFqcn(Logger.class.getName()) //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage("Hello, world")) //
                .setContextData(mdc) //
                .setThreadName("Thread1") //
                .setSource(elements[0])
                .setTimeMillis(System.currentTimeMillis())
                .build();
        final StringBuilder buf = new StringBuilder();
        for (final PatternFormatter formatter : formatters) {
            formatter.format(event, buf);
        }
        final String str = buf.toString();
        final String expected = "INFO  [PatternParserTest        :101 ] - Hello, world" + Strings.LINE_SEPARATOR;
        assertTrue(str.endsWith(expected), "Expected to end with: " + expected + ". Actual: " + str);
    }

    @Test
    void testPatternTruncateFromBeginning() {
        final List<PatternFormatter> formatters = parser.parse(patternTruncateFromBeginning);
        assertNotNull(formatters);
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("org.apache.logging.log4j.PatternParserTest") //
                .setLoggerFqcn(Logger.class.getName()) //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage("Hello, world")) //
                .setThreadName("Thread1") //
                .setTimeMillis(System.currentTimeMillis()) //
                .build();
        final StringBuilder buf = new StringBuilder();
        for (final PatternFormatter formatter : formatters) {
            formatter.format(event, buf);
        }
        final String str = buf.toString();
        final String expected = "INFO  rTest Hello, world" + Strings.LINE_SEPARATOR;
        assertTrue(str.endsWith(expected), "Expected to end with: " + expected + ". Actual: " + str);
    }

    @Test
    void testPatternTruncateFromEnd() {
        final List<PatternFormatter> formatters = parser.parse(patternTruncateFromEnd);
        assertNotNull(formatters);
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("org.apache.logging.log4j.PatternParserTest") //
                .setLoggerFqcn(Logger.class.getName()) //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage("Hello, world")) //
                .setThreadName("Thread1") //
                .setTimeMillis(System.currentTimeMillis()) //
                .build();
        final StringBuilder buf = new StringBuilder();
        for (final PatternFormatter formatter : formatters) {
            formatter.format(event, buf);
        }
        final String str = buf.toString();
        final String expected = "INFO  org.a Hello, world" + Strings.LINE_SEPARATOR;
        assertTrue(str.endsWith(expected), "Expected to end with: " + expected + ". Actual: " + str);
    }

    @Test
    void testBadPattern() {
        final Calendar cal = Calendar.getInstance();
        cal.set(2001, Calendar.FEBRUARY, 3, 4, 5, 6);
        cal.set(Calendar.MILLISECOND, 789);
        final long timestamp = cal.getTimeInMillis();

        final List<PatternFormatter> formatters = parser.parse(badPattern);
        assertNotNull(formatters);
        final Throwable t = new Throwable();
        final StackTraceElement[] elements = t.getStackTrace();
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("a.b.c") //
                .setLoggerFqcn(Logger.class.getName()) //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage("Hello, world")) //
                .setThreadName("Thread1") //
                .setSource(elements[0]) //
                .setTimeMillis(timestamp) //
                .build();
        final StringBuilder buf = new StringBuilder();
        for (final PatternFormatter formatter : formatters) {
            formatter.format(event, buf);
        }
        final String str = buf.toString();

        // eats all characters until the closing '}' character
        final String expected = "[2001-02-03 04:05:06,789] - Hello, world";
        assertTrue(str.startsWith(expected), "Expected to start with: " + expected + ". Actual: " + str);
    }

    @Test
    void testNestedPatternHighlight() {
        testNestedPatternHighlight(Level.TRACE, "\u001B[30m");
        testNestedPatternHighlight(Level.DEBUG, "\u001B[36m");
        testNestedPatternHighlight(Level.INFO, "\u001B[32m");
        testNestedPatternHighlight(Level.WARN, "\u001B[33m");
        testNestedPatternHighlight(Level.ERROR, "\u001B[1;31m");
        testNestedPatternHighlight(Level.FATAL, "\u001B[1;31m");
    }

    private void testNestedPatternHighlight(final Level level, final String expectedStart) {
        final List<PatternFormatter> formatters = parser.parse(nestedPatternHighlight);
        assertNotNull(formatters);
        final Throwable t = new Throwable();
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("org.apache.logging.log4j.PatternParserTest") //
                .setMarker(MarkerManager.getMarker("TEST")) //
                .setLoggerFqcn(Logger.class.getName()) //
                .setLevel(level) //
                .setMessage(new SimpleMessage("Hello, world")) //
                .setThreadName("Thread1") //
                .setSource(/*stackTraceElement[0]*/ null) //
                .setTimeMillis(System.currentTimeMillis()) //
                .build();
        final StringBuilder buf = new StringBuilder();
        for (final PatternFormatter formatter : formatters) {
            formatter.format(event, buf);
        }
        final String str = buf.toString();
        final String expectedEnd = String.format("] %-5s: Hello, world%s\u001B[m", level, Strings.LINE_SEPARATOR);
        assertTrue(str.startsWith(expectedStart), "Expected to start with: " + expectedStart + ". Actual: " + str);
        assertTrue(str.endsWith(expectedEnd), "Expected to end with: \"" + expectedEnd + "\". Actual: \"" + str);
    }

    @Test
    void testNanoPatternShort() {
        testFirstConverter("%N", NanoTimePatternConverter.class);
    }

    @Test
    void testNanoPatternLong() {
        testFirstConverter("%nano", NanoTimePatternConverter.class);
    }

    @Test
    void testThreadNamePattern() {
        testThreadNamePattern("%thread");
    }

    @Test
    void testThreadNameFullPattern() {
        testThreadNamePattern("%threadName");
    }

    @Test
    void testThreadIdFullPattern() {
        testThreadIdPattern("%threadId");
    }

    @Test
    void testThreadIdShortPattern1() {
        testThreadIdPattern("%tid");
    }

    @Test
    void testThreadIdShortPattern2() {
        testThreadIdPattern("%T");
    }

    @Test
    void testThreadPriorityShortPattern() {
        testThreadPriorityPattern("%tp");
    }

    @Test
    void testThreadPriorityFullPattern() {
        testThreadPriorityPattern("%threadPriority");
    }

    @Test
    void testLoggerFqcnPattern() {
        testFirstConverter("%fqcn", LoggerFqcnPatternConverter.class);
    }

    @Test
    void testEndOfBatchPattern() {
        testFirstConverter("%endOfBatch", EndOfBatchPatternConverter.class);
    }

    private void testThreadIdPattern(final String pattern) {
        testFirstConverter(pattern, ThreadIdPatternConverter.class);
    }

    private void testThreadNamePattern(final String pattern) {
        testFirstConverter(pattern, ThreadNamePatternConverter.class);
    }

    private void testThreadPriorityPattern(final String pattern) {
        testFirstConverter(pattern, ThreadPriorityPatternConverter.class);
    }

    private void testFirstConverter(final String pattern, final Class<?> checkClass) {
        final List<PatternFormatter> formatters = parser.parse(pattern);
        assertNotNull(formatters);
        final String msg = formatters.toString();
        assertEquals(1, formatters.size(), msg);
        assertTrue(checkClass.isInstance(formatters.get(0).getConverter()), msg);
    }

    @Test
    void testThreadNameShortPattern() {
        testThreadNamePattern("%t");
    }

    @Test
    void testNanoPatternShortChangesConfigurationNanoClock() {
        final Configuration config = new NullConfiguration();
        assertInstanceOf(DummyNanoClock.class, config.getNanoClock());

        final PatternParser pp = new PatternParser(config, KEY, null);
        assertInstanceOf(DummyNanoClock.class, config.getNanoClock());

        pp.parse("%m");
        assertInstanceOf(DummyNanoClock.class, config.getNanoClock());

        pp.parse("%nano"); // this changes the config clock
        assertInstanceOf(SystemNanoClock.class, config.getNanoClock());
    }

    @Test
    void testNanoPatternLongChangesNanoClockFactoryMode() {
        final Configuration config = new NullConfiguration();
        assertInstanceOf(DummyNanoClock.class, config.getNanoClock());

        final PatternParser pp = new PatternParser(config, KEY, null);
        assertInstanceOf(DummyNanoClock.class, config.getNanoClock());

        pp.parse("%m");
        assertInstanceOf(DummyNanoClock.class, config.getNanoClock());

        pp.parse("%N");
        assertInstanceOf(SystemNanoClock.class, config.getNanoClock());
    }

    @Test
    void testDeeplyNestedPattern() {
        final List<PatternFormatter> formatters = parser.parse(deeplyNestedPattern);
        assertNotNull(formatters);
        assertEquals(1, formatters.size());

        final StringMap mdc = ContextDataFactory.createContextData();
        mdc.putValue("var", "1234");
        final Log4jLogEvent event = Log4jLogEvent.newBuilder() //
                .setContextData(mdc)
                .build();
        final StringBuilder buf = new StringBuilder();
        formatters.get(0).format(event, buf);
        final String expected = " 123 ";
        assertEquals(expected, buf.toString());
    }

    @Test
    void testMissingClosingBracket() {
        testFirstConverter("%d{", DatePatternConverter.class);
    }

    @Test
    void testClosingBracketButWrongPlace() {
        final List<PatternFormatter> formatters = parser.parse("}%d{");
        assertNotNull(formatters);
        assertEquals(2, formatters.size());

        validateConverter(formatters, 0, "SimpleLiteral");
        validateConverter(formatters, 1, "Date");
    }

    @Test
    void testExceptionWithFilters() {
        final List<PatternFormatter> formatters =
                parser.parse("%d{DEFAULT} - %msg - %xEx{full}{filters(org.junit,org.eclipse)}%n");
        assertNotNull(formatters);
        assertEquals(6, formatters.size());
        final PatternFormatter patternFormatter = formatters.get(4);
        final LogEventPatternConverter converter = patternFormatter.getConverter();
        assertEquals(ExtendedThrowablePatternConverter.class, converter.getClass());
        final ExtendedThrowablePatternConverter exConverter = (ExtendedThrowablePatternConverter) converter;
        final ThrowableFormatOptions options = exConverter.getOptions();
        assertTrue(options.getIgnorePackages().contains("org.junit"));
        assertTrue(options.getIgnorePackages().contains("org.eclipse"));
        assertEquals(System.lineSeparator(), options.getSeparator());
    }

    @Test
    void testExceptionWithFiltersAndSeparator() {
        final List<PatternFormatter> formatters =
                parser.parse("%d{DEFAULT} - %msg - %xEx{full}{filters(org.junit,org.eclipse)}{separator(|)}%n");
        assertNotNull(formatters);
        assertEquals(6, formatters.size());
        final PatternFormatter patternFormatter = formatters.get(4);
        final LogEventPatternConverter converter = patternFormatter.getConverter();
        assertEquals(ExtendedThrowablePatternConverter.class, converter.getClass());
        final ExtendedThrowablePatternConverter exConverter = (ExtendedThrowablePatternConverter) converter;
        final ThrowableFormatOptions options = exConverter.getOptions();
        final List<String> ignorePackages = options.getIgnorePackages();
        assertNotNull(ignorePackages);
        final String ignorePackagesString = ignorePackages.toString();
        assertTrue(ignorePackages.contains("org.junit"), ignorePackagesString);
        assertTrue(ignorePackages.contains("org.eclipse"), ignorePackagesString);
        assertEquals("|", options.getSeparator());
    }

    // LOG4J2-2564: Multiple newInstance methods.
    @Test
    void testMapPatternConverter() {
        final List<PatternFormatter> formatters = parser.parse("%K");
        assertNotNull(formatters);
        assertEquals(1, formatters.size());
        final PatternFormatter formatter = formatters.get(0);
        assertInstanceOf(MapPatternConverter.class, formatter.getConverter(), "Expected a MapPatternConverter");
    }
}
