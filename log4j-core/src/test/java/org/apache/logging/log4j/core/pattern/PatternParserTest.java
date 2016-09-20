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
package org.apache.logging.log4j.core.pattern;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.util.DummyNanoClock;
import org.apache.logging.log4j.core.util.SystemNanoClock;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.Strings;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class PatternParserTest {

    static String OUTPUT_FILE   = "output/PatternParser";
    static String WITNESS_FILE  = "witness/PatternParser";
    LoggerContext ctx = LoggerContext.getContext();
    Logger root = ctx.getRootLogger();

    private static String msgPattern = "%m%n";
    private final String mdcMsgPattern1 = "%m : %X%n";
    private final String mdcMsgPattern2 = "%m : %X{key1}%n";
    private final String mdcMsgPattern3 = "%m : %X{key2}%n";
    private final String mdcMsgPattern4 = "%m : %X{key3}%n";
    private final String mdcMsgPattern5 = "%m : %X{key1},%X{key2},%X{key3}%n";

    private static String badPattern = "[%d{yyyyMMdd HH:mm:ss,SSS] %-5p [%c{10}] - %m%n";
    private static String customPattern = "[%d{yyyyMMdd HH:mm:ss,SSS}] %-5p [%-25.25c{1}:%-4L] - %m%n";
    private static String patternTruncateFromEnd = "%d; %-5p %5.-5c %m%n";
    private static String patternTruncateFromBeginning = "%d; %-5p %5.5c %m%n";
    private static String nestedPatternHighlight =
            "%highlight{%d{dd MMM yyyy HH:mm:ss,SSS}{GMT+0} [%t] %-5level: %msg%n%throwable}";

    private static final String KEY = "Converter";
    private PatternParser parser;

    @Before
    public void setup() {
        parser = new PatternParser(KEY);
    }

    private void validateConverter(final List<PatternFormatter> formatter, final int index, final String name) {
        final PatternConverter pc = formatter.get(index).getConverter();
        assertEquals("Incorrect converter " + pc.getName() + " at index " + index + " expected " + name,
            pc.getName(), name);
    }

    /**
     * Test the default pattern
     */
    @Test
    public void defaultPattern() {
        final List<PatternFormatter> formatters = parser.parse(msgPattern);
        assertNotNull(formatters);
        assertTrue(formatters.size() == 2);
        validateConverter(formatters, 0, "Message");
        validateConverter(formatters, 1, "Line Sep");
    }

    /**
     * Test the custom pattern
     */
    @Test
    public void testCustomPattern() {
        final List<PatternFormatter> formatters = parser.parse(customPattern);
        assertNotNull(formatters);
        final Map<String, String> mdc = new HashMap<>();
        mdc.put("loginId", "Fred");
        final Throwable t = new Throwable();
        final StackTraceElement[] elements = t.getStackTrace();
        final Log4jLogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("org.apache.logging.log4j.PatternParserTest") //
                .setMarker(MarkerManager.getMarker("TEST")) //
                .setLoggerFqcn(Logger.class.getName()) //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage("Hello, world")) //
                .setContextMap(mdc) //
                .setThreadName("Thread1") //
                .setSource(elements[0])
                .setTimeMillis(System.currentTimeMillis()).build();
        final StringBuilder buf = new StringBuilder();
        for (final PatternFormatter formatter : formatters) {
            formatter.format(event, buf);
        }
        final String str = buf.toString();
        final String expected = "INFO  [PatternParserTest        :100 ] - Hello, world" + Strings.LINE_SEPARATOR;
        assertTrue("Expected to end with: " + expected + ". Actual: " + str, str.endsWith(expected));
    }

    @Test
    public void testPatternTruncateFromBeginning() {
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
        assertTrue("Expected to end with: " + expected + ". Actual: " + str, str.endsWith(expected));
    }

    @Test
    public void testPatternTruncateFromEnd() {
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
        assertTrue("Expected to end with: " + expected + ". Actual: " + str, str.endsWith(expected));
    }


    @Test
    public void testBadPattern() {
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
        assertTrue("Expected to start with: " + expected + ". Actual: " + str, str.startsWith(expected));
    }

    @Test
    public void testNestedPatternHighlight() {
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
        t.getStackTrace();
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
        assertTrue("Expected to start with: " + expectedStart + ". Actual: " + str, str.startsWith(expectedStart));
        assertTrue("Expected to end with: \"" + expectedEnd + "\". Actual: \"" + str, str.endsWith(expectedEnd));
    }

    @Test
    public void testNanoPatternShort() {
        final List<PatternFormatter> formatters = parser.parse("%N");
        assertNotNull(formatters);
        assertEquals(1, formatters.size());
        assertTrue(formatters.get(0).getConverter() instanceof NanoTimePatternConverter);
    }

    @Test
    public void testNanoPatternLong() {
        final List<PatternFormatter> formatters = parser.parse("%nano");
        assertNotNull(formatters);
        assertEquals(1, formatters.size());
        assertTrue(formatters.get(0).getConverter() instanceof NanoTimePatternConverter);
    }

    @Test
    public void testThreadNamePattern() {
        testThreadNamePattern("%thread");
    }

    @Test
    public void testThreadNameFullPattern() {
        testThreadNamePattern("%threadName");
    }

    @Test
    public void testThreadIdFullPattern() {
        testThreadIdPattern("%threadId");
    }

    @Test
    public void testThreadIdShortPattern1() {
        testThreadIdPattern("%tid");
    }

    @Test
    public void testThreadIdShortPattern2() {
        testThreadIdPattern("%T");
    }

    @Test
    public void testThreadPriorityShortPattern() {
        testThreadPriorityPattern("%tp");
    }

    @Test
    public void testThreadPriorityFullPattern() {
        testThreadPriorityPattern("%threadPriority");
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
        assertEquals(msg, 1, formatters.size());
        assertTrue(msg, checkClass.isInstance(formatters.get(0).getConverter()));
    }

    @Test
    public void testThreadNameShortPattern() {
        testThreadNamePattern("%t");
    }

    @Test
    public void testNanoPatternShortChangesConfigurationNanoClock() {
        final Configuration config = new NullConfiguration();
        assertTrue(config.getNanoClock() instanceof DummyNanoClock);

        final PatternParser pp = new PatternParser(config, KEY, null);
        assertTrue(config.getNanoClock() instanceof DummyNanoClock);

        pp.parse("%m");
        assertTrue(config.getNanoClock() instanceof DummyNanoClock);

        pp.parse("%nano"); // this changes the config clock
        assertTrue(config.getNanoClock() instanceof SystemNanoClock);
    }

    @Test
    public void testNanoPatternLongChangesNanoClockFactoryMode() {
        final Configuration config = new NullConfiguration();
        assertTrue(config.getNanoClock() instanceof DummyNanoClock);

        final PatternParser pp = new PatternParser(config, KEY, null);
        assertTrue(config.getNanoClock() instanceof DummyNanoClock);

        pp.parse("%m");
        assertTrue(config.getNanoClock() instanceof DummyNanoClock);

        pp.parse("%N");
        assertTrue(config.getNanoClock() instanceof SystemNanoClock);
    }
}
