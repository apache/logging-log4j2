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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

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
import org.apache.logging.log4j.core.time.SystemNanoClock;
import org.apache.logging.log4j.core.time.internal.DummyNanoClock;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.StringMap;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    private final String deeplyNestedPattern = "%notEmpty{ %maxLen{%X{var}}{3} }";

    private static String badPattern = "[%d{yyyyMMdd HH:mm:ss,SSS] %-5p [%c{10}] - %m%n";
    private static String customPattern = "[%d{yyyyMMdd HH:mm:ss,SSS}] %-5p [%-25.25c{1}:%-4L] - %m%n";
    private static String patternTruncateFromEnd = "%d; %-5p %5.-5c %m%n";
    private static String patternTruncateFromBeginning = "%d; %-5p %5.5c %m%n";
    private static String nestedPatternHighlight =
            "%highlight{%d{dd MMM yyyy HH:mm:ss,SSS}{GMT+0} [%t] %-5level: %msg%n%throwable}";

    private static final String KEY = "Converter";
    private PatternParser parser;

    @BeforeEach
    public void setup() {
        parser = new PatternParser(KEY);
    }

    private void validateConverter(final List<PatternFormatter> formatter, final int index, final String name) {
        final PatternConverter pc = formatter.get(index).getConverter();
        assertThat(name).describedAs("Incorrect converter " + pc.getName() + " at index " + index + " expected " + name).isEqualTo(pc.getName());
    }

    /**
     * Test the default pattern
     */
    @Test
    public void defaultPattern() {
        final List<PatternFormatter> formatters = parser.parse(msgPattern);
        assertThat(formatters).isNotNull();
        assertThat(formatters).hasSize(2);
        validateConverter(formatters, 0, "Message");
        validateConverter(formatters, 1, "Line Sep");
    }

    /**
     * Test the custom pattern
     */
    @Test
    public void testCustomPattern() {
        final List<PatternFormatter> formatters = parser.parse(customPattern);
        assertThat(formatters).isNotNull();
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
                .setTimeMillis(System.currentTimeMillis()).build();
        final StringBuilder buf = new StringBuilder();
        for (final PatternFormatter formatter : formatters) {
            formatter.format(event, buf);
        }
        final String str = buf.toString();
        final String expected = "INFO  [PatternParserTest        :99  ] - Hello, world" + Strings.LINE_SEPARATOR;
        assertTrue(str.endsWith(expected), "Expected to end with: " + expected + ". Actual: " + str);
    }

    @Test
    public void testPatternTruncateFromBeginning() {
        final List<PatternFormatter> formatters = parser.parse(patternTruncateFromBeginning);
        assertThat(formatters).isNotNull();
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
    public void testPatternTruncateFromEnd() {
        final List<PatternFormatter> formatters = parser.parse(patternTruncateFromEnd);
        assertThat(formatters).isNotNull();
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
    public void testBadPattern() {
        final Calendar cal = Calendar.getInstance();
        cal.set(2001, Calendar.FEBRUARY, 3, 4, 5, 6);
        cal.set(Calendar.MILLISECOND, 789);
        final long timestamp = cal.getTimeInMillis();

        final List<PatternFormatter> formatters = parser.parse(badPattern);
        assertThat(formatters).isNotNull();
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
        assertThat(formatters).isNotNull();
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
        assertTrue(str.startsWith(expectedStart), "Expected to start with: " + expectedStart + ". Actual: " + str);
        assertTrue(str.endsWith(expectedEnd), "Expected to end with: \"" + expectedEnd + "\". Actual: \"" + str);
    }

    @Test
    public void testNanoPatternShort() {
        testFirstConverter("%N", NanoTimePatternConverter.class);
    }

    @Test
    public void testNanoPatternLong() {
        testFirstConverter("%nano", NanoTimePatternConverter.class);
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

    @Test
    public void testLoggerFqcnPattern() {
        testFirstConverter("%fqcn", LoggerFqcnPatternConverter.class);
    }

    @Test
    public void testEndOfBatchPattern() {
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
        assertThat(formatters).isNotNull();
        final String msg = formatters.toString();
        assertThat(formatters).describedAs(msg).hasSize(1);
        assertTrue(checkClass.isInstance(formatters.get(0).getConverter()), msg);
    }

    @Test
    public void testThreadNameShortPattern() {
        testThreadNamePattern("%t");
    }

    @Test
    public void testNanoPatternShortChangesConfigurationNanoClock() {
        final Configuration config = new NullConfiguration();
        assertThat(config.getNanoClock() instanceof DummyNanoClock).isTrue();

        final PatternParser pp = new PatternParser(config, KEY, null);
        assertThat(config.getNanoClock() instanceof DummyNanoClock).isTrue();

        pp.parse("%m");
        assertThat(config.getNanoClock() instanceof DummyNanoClock).isTrue();

        pp.parse("%nano"); // this changes the config clock
        assertThat(config.getNanoClock() instanceof SystemNanoClock).isTrue();
    }

    @Test
    public void testNanoPatternLongChangesNanoClockFactoryMode() {
        final Configuration config = new NullConfiguration();
        assertThat(config.getNanoClock() instanceof DummyNanoClock).isTrue();

        final PatternParser pp = new PatternParser(config, KEY, null);
        assertThat(config.getNanoClock() instanceof DummyNanoClock).isTrue();

        pp.parse("%m");
        assertThat(config.getNanoClock() instanceof DummyNanoClock).isTrue();

        pp.parse("%N");
        assertThat(config.getNanoClock() instanceof SystemNanoClock).isTrue();
    }

    @Test
    public void testDeeplyNestedPattern() {
        final List<PatternFormatter> formatters = parser.parse(deeplyNestedPattern);
        assertThat(formatters).isNotNull();
        assertThat(formatters).hasSize(1);

        final StringMap mdc = ContextDataFactory.createContextData();
        mdc.putValue("var", "1234");
        final Log4jLogEvent event = Log4jLogEvent.newBuilder() //
            .setContextData(mdc).build();
        final StringBuilder buf = new StringBuilder();
        formatters.get(0).format(event, buf);
        final String expected = " 123 ";
        assertThat(buf.toString()).isEqualTo(expected);
    }

    @Test
    public void testMissingClosingBracket() {
        testFirstConverter("%d{", DatePatternConverter.class);
    }

    @Test
    public void testClosingBracketButWrongPlace() {
        final List<PatternFormatter> formatters = parser.parse("}%d{");
        assertThat(formatters).isNotNull();
        assertThat(formatters).hasSize(2);

        validateConverter(formatters, 0, "Literal");
        validateConverter(formatters, 1, "Date");
    }

    @Test
    public void testExceptionWithFilters() {
        final List<PatternFormatter> formatters = parser
                .parse("%d{DEFAULT} - %msg - %xEx{full}{filters(org.junit,org.eclipse)}%n");
        assertThat(formatters).isNotNull();
        assertThat(formatters).hasSize(6);
        final PatternFormatter patternFormatter = formatters.get(4);
        final LogEventPatternConverter converter = patternFormatter.getConverter();
        assertThat(converter.getClass()).isEqualTo(ExtendedThrowablePatternConverter.class);
        final ExtendedThrowablePatternConverter exConverter = (ExtendedThrowablePatternConverter) converter;
        final ThrowableFormatOptions options = exConverter.getOptions();
        assertThat(options.getIgnorePackages().contains("org.junit")).isTrue();
        assertThat(options.getIgnorePackages().contains("org.eclipse")).isTrue();
        assertThat(options.getSeparator()).isEqualTo(System.lineSeparator());
    }

    @Test
    public void testExceptionWithFiltersAndSeparator() {
        final List<PatternFormatter> formatters = parser
                .parse("%d{DEFAULT} - %msg - %xEx{full}{filters(org.junit,org.eclipse)}{separator(|)}%n");
        assertThat(formatters).isNotNull();
        assertThat(formatters).hasSize(6);
        final PatternFormatter patternFormatter = formatters.get(4);
        final LogEventPatternConverter converter = patternFormatter.getConverter();
        assertThat(converter.getClass()).isEqualTo(ExtendedThrowablePatternConverter.class);
        final ExtendedThrowablePatternConverter exConverter = (ExtendedThrowablePatternConverter) converter;
        final ThrowableFormatOptions options = exConverter.getOptions();
        final List<String> ignorePackages = options.getIgnorePackages();
        assertThat(ignorePackages).isNotNull();
        final String ignorePackagesString = ignorePackages.toString();
        assertTrue(ignorePackages.contains("org.junit"), ignorePackagesString);
        assertTrue(ignorePackages.contains("org.eclipse"), ignorePackagesString);
        assertThat(options.getSeparator()).isEqualTo("|");
    }

    // LOG4J2-2564: Multiple newInstance methods.
    @Test
    public void testMapPatternConverter() {
        final List<PatternFormatter> formatters = parser.parse("%K");
        assertThat(formatters).isNotNull();
        assertThat(formatters).hasSize(1);
        PatternFormatter formatter = formatters.get(0);
        assertTrue(formatter.getConverter() instanceof MapPatternConverter, "Expected a MapPatternConverter");
    }
}
