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
package org.apache.logging.log4j.core.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.lookup.MainMapLookup;
import org.apache.logging.log4j.core.test.BasicConfigurationFactory;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.test.junit.UsingAnyThreadContext;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@UsingAnyThreadContext
class PatternLayoutTest {
    public static class FauxLogger {
        public String formatEvent(final LogEvent event, final Layout<?> layout) {
            return new String(layout.toByteArray(event));
        }
    }

    static ConfigurationFactory cf = new BasicConfigurationFactory();
    static String msgPattern = "%m%n";
    static String OUTPUT_FILE = "target/output/PatternParser";
    static final String regexPattern = "%replace{%logger %msg}{\\.}{/}";

    static String WITNESS_FILE = "witness/PatternParser";

    public static void cleanupClass() {
        ConfigurationFactory.removeConfigurationFactory(cf);
    }

    @BeforeAll
    static void setupClass() {
        ConfigurationFactory.setConfigurationFactory(cf);
        final LoggerContext ctx = LoggerContext.getContext();
        ctx.reconfigure();
    }

    LoggerContext ctx = LoggerContext.getContext();

    Logger root = ctx.getRootLogger();

    private static class Destination implements ByteBufferDestination {
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[2048]);

        @Override
        public ByteBuffer getByteBuffer() {
            return byteBuffer;
        }

        @Override
        public ByteBuffer drain(final ByteBuffer buf) {
            throw new IllegalStateException("Unexpected message larger than 2048 bytes");
        }

        @Override
        public void writeBytes(final ByteBuffer data) {
            byteBuffer.put(data);
        }

        @Override
        public void writeBytes(final byte[] data, final int offset, final int length) {
            byteBuffer.put(data, offset, length);
        }
    }

    private void assertToByteArray(final String expectedStr, final PatternLayout layout, final LogEvent event) {
        final byte[] result = layout.toByteArray(event);
        assertEquals(expectedStr, new String(result));
    }

    private void assertEncode(final String expectedStr, final PatternLayout layout, final LogEvent event) {
        final Destination destination = new Destination();
        layout.encode(event, destination);
        final ByteBuffer byteBuffer = destination.getByteBuffer();
        byteBuffer.flip(); // set limit to position, position back to zero
        assertEquals(
                expectedStr,
                new String(
                        byteBuffer.array(), byteBuffer.arrayOffset() + byteBuffer.position(), byteBuffer.remaining()));
    }

    @Test
    void testEqualsEmptyMarker() {
        // replace "[]" with the empty string
        final PatternLayout layout = PatternLayout.newBuilder()
                .withPattern("[%logger]%equals{[%marker]}{[]}{} %msg")
                .withConfiguration(ctx.getConfiguration())
                .build();
        // Not empty marker
        final LogEvent event1 = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName())
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO) //
                .setMarker(MarkerManager.getMarker("TestMarker")) //
                .setMessage(new SimpleMessage("Hello, world!"))
                .build();
        assertToByteArray(
                "[org.apache.logging.log4j.core.layout.PatternLayoutTest][TestMarker] Hello, world!", layout, event1);
        assertEncode(
                "[org.apache.logging.log4j.core.layout.PatternLayoutTest][TestMarker] Hello, world!", layout, event1);
        // empty marker
        final LogEvent event2 = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName())
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage("Hello, world!"))
                .build();
        assertToByteArray("[org.apache.logging.log4j.core.layout.PatternLayoutTest] Hello, world!", layout, event2);
        assertEncode("[org.apache.logging.log4j.core.layout.PatternLayoutTest] Hello, world!", layout, event2);
    }

    @Test
    void testHeaderFooterJavaLookup() {
        // % does not work here.
        final String pattern = "%d{UNIX} MyApp%n${java:version}%n${java:runtime}%n${java:vm}%n${java:os}%n${java:hw}";
        final PatternLayout layout = PatternLayout.newBuilder()
                .withConfiguration(ctx.getConfiguration())
                .withHeader("Header: " + pattern)
                .withFooter("Footer: " + pattern)
                .build();
        final byte[] header = layout.getHeader();
        assertNotNull(header, "No header");
        final String headerStr = new String(header);
        assertTrue(headerStr.contains("Header: "), headerStr);
        assertTrue(headerStr.contains("Java version "), headerStr);
        assertTrue(headerStr.contains("(build "), headerStr);
        assertTrue(headerStr.contains(" from "), headerStr);
        assertTrue(headerStr.contains(" architecture: "), headerStr);
        assertFalse(headerStr.contains("%d{UNIX}"), headerStr);
        //
        final byte[] footer = layout.getFooter();
        assertNotNull(footer, "No footer");
        final String footerStr = new String(footer);
        assertTrue(footerStr.contains("Footer: "), footerStr);
        assertTrue(footerStr.contains("Java version "), footerStr);
        assertTrue(footerStr.contains("(build "), footerStr);
        assertTrue(footerStr.contains(" from "), footerStr);
        assertTrue(footerStr.contains(" architecture: "), footerStr);
        assertFalse(footerStr.contains("%d{UNIX}"), footerStr);
    }

    /**
     * Tests LOG4J2-962.
     */
    @Test
    void testHeaderFooterMainLookup() {
        MainMapLookup.setMainArguments("value0", "value1", "value2");
        final PatternLayout layout = PatternLayout.newBuilder()
                .withConfiguration(ctx.getConfiguration())
                .withHeader("${main:0}")
                .withFooter("${main:2}")
                .build();
        final byte[] header = layout.getHeader();
        assertNotNull(header, "No header");
        final String headerStr = new String(header);
        assertTrue(headerStr.contains("value0"), headerStr);
        //
        final byte[] footer = layout.getFooter();
        assertNotNull(footer, "No footer");
        final String footerStr = new String(footer);
        assertTrue(footerStr.contains("value2"), footerStr);
    }

    @Test
    void testHeaderFooterThreadContext() {
        final PatternLayout layout = PatternLayout.newBuilder()
                .withPattern("%d{UNIX} %m")
                .withConfiguration(ctx.getConfiguration())
                .withHeader("${ctx:header}")
                .withFooter("${ctx:footer}")
                .build();
        ThreadContext.put("header", "Hello world Header");
        ThreadContext.put("footer", "Hello world Footer");
        final byte[] header = layout.getHeader();
        assertNotNull(header, "No header");
        assertEquals(
                "Hello world Header",
                new String(header),
                "expected \"Hello world Header\", actual " + Strings.dquote(new String(header)));
    }

    private void testMdcPattern(final String patternStr, final String expectedStr, final boolean useThreadContext) {
        final PatternLayout layout = PatternLayout.newBuilder()
                .withPattern(patternStr)
                .withConfiguration(ctx.getConfiguration())
                .build();
        if (useThreadContext) {
            ThreadContext.put("key1", "value1");
            ThreadContext.put("key2", "value2");
        }
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName())
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage("Hello"))
                .build();
        assertToByteArray(expectedStr, layout, event);
        assertEncode(expectedStr, layout, event);
    }

    @Test
    void testMdcPattern0() throws Exception {
        testMdcPattern("%m : %X", "Hello : {key1=value1, key2=value2}", true);
    }

    @Test
    void testMdcPattern1() throws Exception {
        testMdcPattern("%m : %X", "Hello : {}", false);
    }

    @Test
    void testMdcPattern2() throws Exception {
        testMdcPattern("%m : %X{key1}", "Hello : value1", true);
    }

    @Test
    void testMdcPattern3() throws Exception {
        testMdcPattern("%m : %X{key2}", "Hello : value2", true);
    }

    @Test
    void testMdcPattern4() throws Exception {
        testMdcPattern("%m : %X{key3}", "Hello : ", true);
    }

    @Test
    void testMdcPattern5() throws Exception {
        testMdcPattern("%m : %X{key1}, %X{key2}, %X{key3}", "Hello : value1, value2, ", true);
    }

    @Test
    void testPatternSelector() {
        final PatternMatch[] patterns = new PatternMatch[1];
        patterns[0] = new PatternMatch("FLOW", "%d %-5p [%t]: ====== %C{1}.%M:%L %m ======%n");
        final PatternSelector selector = MarkerPatternSelector.createSelector(
                patterns, "%d %-5p [%t]: %m%n", true, true, ctx.getConfiguration());
        final PatternLayout layout = PatternLayout.newBuilder()
                .withPatternSelector(selector)
                .withConfiguration(ctx.getConfiguration())
                .build();
        final LogEvent event1 = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName())
                .setLoggerFqcn("org.apache.logging.log4j.core.layout.PatternLayoutTest$FauxLogger")
                .setMarker(MarkerManager.getMarker("FLOW"))
                .setLevel(Level.TRACE) //
                .setIncludeLocation(true)
                .setMessage(new SimpleMessage("entry"))
                .build();
        final String result1 = new FauxLogger().formatEvent(event1, layout);
        final String expectPattern1 =
                String.format(".*====== PatternLayoutTest.testPatternSelector:\\d+ entry ======%n");
        assertTrue(result1.matches(expectPattern1), "Unexpected result: " + result1);
        final LogEvent event2 = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName())
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage("Hello, world 1!"))
                .build();
        final String result2 = new String(layout.toByteArray(event2));
        final String expectSuffix2 = String.format("Hello, world 1!%n");
        assertTrue(result2.endsWith(expectSuffix2), "Unexpected result: " + result2);
    }

    @Test
    void testRegex() {
        final PatternLayout layout = PatternLayout.newBuilder()
                .withPattern(regexPattern)
                .withConfiguration(ctx.getConfiguration())
                .build();
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName())
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage("Hello, world!"))
                .build();
        assertToByteArray("org/apache/logging/log4j/core/layout/PatternLayoutTest Hello, world!", layout, event);
        assertEncode("org/apache/logging/log4j/core/layout/PatternLayoutTest Hello, world!", layout, event);
    }

    @Test
    void testRegexEmptyMarker() {
        // replace "[]" with the empty string
        final PatternLayout layout = PatternLayout.newBuilder()
                .withPattern("[%logger]%replace{[%marker]}{\\[\\]}{} %msg")
                .withConfiguration(ctx.getConfiguration())
                .build();
        // Not empty marker
        final LogEvent event1 = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName())
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO) //
                .setMarker(MarkerManager.getMarker("TestMarker")) //
                .setMessage(new SimpleMessage("Hello, world!"))
                .build();
        assertToByteArray(
                "[org.apache.logging.log4j.core.layout.PatternLayoutTest][TestMarker] Hello, world!", layout, event1);
        assertEncode(
                "[org.apache.logging.log4j.core.layout.PatternLayoutTest][TestMarker] Hello, world!", layout, event1);

        // empty marker
        final LogEvent event2 = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName())
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage("Hello, world!"))
                .build();
        assertToByteArray("[org.apache.logging.log4j.core.layout.PatternLayoutTest] Hello, world!", layout, event2);
        assertEncode("[org.apache.logging.log4j.core.layout.PatternLayoutTest] Hello, world!", layout, event2);
    }

    @Test
    void testEqualsMarkerWithMessageSubstitution() {
        // replace "[]" with the empty string
        final PatternLayout layout = PatternLayout.newBuilder()
                .withPattern("[%logger]%equals{[%marker]}{[]}{[%msg]}")
                .withConfiguration(ctx.getConfiguration())
                .build();
        // Not empty marker
        final LogEvent event1 = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName())
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO) //
                .setMarker(MarkerManager.getMarker("TestMarker"))
                .setMessage(new SimpleMessage("Hello, world!"))
                .build();
        final byte[] result1 = layout.toByteArray(event1);
        assertEquals("[org.apache.logging.log4j.core.layout.PatternLayoutTest][TestMarker]", new String(result1));
        // empty marker
        final LogEvent event2 = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName())
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO)
                .setMessage(new SimpleMessage("Hello, world!"))
                .build();
        final byte[] result2 = layout.toByteArray(event2);
        assertEquals("[org.apache.logging.log4j.core.layout.PatternLayoutTest][Hello, world!]", new String(result2));
    }

    @Test
    void testSpecialChars() {
        final PatternLayout layout = PatternLayout.newBuilder()
                .withPattern("\\\\%level\\t%msg\\n\\t%logger\\r\\n\\f")
                .withConfiguration(ctx.getConfiguration())
                .build();
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName())
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage("Hello, world!"))
                .build();
        assertToByteArray(
                "\\INFO\tHello, world!\n" + "\torg.apache.logging.log4j.core.layout.PatternLayoutTest\r\n" + "\f",
                layout,
                event);
        assertEncode(
                "\\INFO\tHello, world!\n" + "\torg.apache.logging.log4j.core.layout.PatternLayoutTest\r\n" + "\f",
                layout,
                event);
    }

    @Test
    void testUnixTime() {
        final PatternLayout layout = PatternLayout.newBuilder()
                .withPattern("%d{UNIX} %m")
                .withConfiguration(ctx.getConfiguration())
                .build();
        final LogEvent event1 = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName())
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage("Hello, world 1!"))
                .build();
        final byte[] result1 = layout.toByteArray(event1);
        assertEquals(event1.getTimeMillis() / 1000 + " Hello, world 1!", new String(result1));
        // System.out.println("event1=" + event1.getTimeMillis() / 1000);
        final LogEvent event2 = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName())
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage("Hello, world 2!"))
                .build();
        final byte[] result2 = layout.toByteArray(event2);
        assertEquals(event2.getTimeMillis() / 1000 + " Hello, world 2!", new String(result2));
        // System.out.println("event2=" + event2.getTimeMillis() / 1000);
    }

    @SuppressWarnings("unused")
    private void testUnixTime(final String pattern) {
        final PatternLayout layout = PatternLayout.newBuilder()
                .withPattern(pattern + " %m")
                .withConfiguration(ctx.getConfiguration())
                .build();
        final LogEvent event1 = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName())
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage("Hello, world 1!"))
                .build();
        final byte[] result1 = layout.toByteArray(event1);
        assertEquals(event1.getTimeMillis() + " Hello, world 1!", new String(result1));
        // System.out.println("event1=" + event1.getMillis());
        final LogEvent event2 = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName())
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage("Hello, world 2!"))
                .build();
        final byte[] result2 = layout.toByteArray(event2);
        assertEquals(event2.getTimeMillis() + " Hello, world 2!", new String(result2));
        // System.out.println("event2=" + event2.getMillis());
    }

    @Test
    void testUnixTimeMillis() {
        final PatternLayout layout = PatternLayout.newBuilder()
                .withPattern("%d{UNIX_MILLIS} %m")
                .withConfiguration(ctx.getConfiguration())
                .build();
        final LogEvent event1 = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName())
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage("Hello, world 1!"))
                .build();
        final byte[] result1 = layout.toByteArray(event1);
        assertEquals(event1.getTimeMillis() + " Hello, world 1!", new String(result1));
        // System.out.println("event1=" + event1.getTimeMillis());
        final LogEvent event2 = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName())
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage("Hello, world 2!"))
                .build();
        final byte[] result2 = layout.toByteArray(event2);
        assertEquals(event2.getTimeMillis() + " Hello, world 2!", new String(result2));
        // System.out.println("event2=" + event2.getTimeMillis());
    }

    @Test
    void testUsePlatformDefaultIfNoCharset() {
        final PatternLayout layout = PatternLayout.newBuilder()
                .withPattern("%m")
                .withConfiguration(ctx.getConfiguration())
                .build();
        assertEquals(Charset.defaultCharset(), layout.getCharset());
    }

    @Test
    void testUseSpecifiedCharsetIfExists() {
        final PatternLayout layout = PatternLayout.newBuilder()
                .withPattern("%m")
                .withConfiguration(ctx.getConfiguration())
                .withCharset(StandardCharsets.UTF_8)
                .build();
        assertEquals(StandardCharsets.UTF_8, layout.getCharset());
    }

    @Test
    void testLoggerNameTruncationByRetainingPartsFromEnd() {
        {
            final PatternLayout layout = PatternLayout.newBuilder()
                    .withPattern("%c{1} %m")
                    .withConfiguration(ctx.getConfiguration())
                    .build();
            final LogEvent event1 = Log4jLogEvent.newBuilder()
                    .setLoggerName(this.getClass().getName())
                    .setLoggerFqcn("org.apache.logging.log4j.core.Logger")
                    .setLevel(Level.INFO)
                    .setMessage(new SimpleMessage("Hello, world 1!"))
                    .build();
            final String result1 = layout.toSerializable(event1);
            assertEquals(
                    this.getClass()
                                    .getName()
                                    .substring(this.getClass().getName().lastIndexOf(".") + 1) + " Hello, world 1!",
                    new String(result1));
        }
        {
            final PatternLayout layout = PatternLayout.newBuilder()
                    .withPattern("%c{2} %m")
                    .withConfiguration(ctx.getConfiguration())
                    .build();
            final LogEvent event1 = Log4jLogEvent.newBuilder()
                    .setLoggerName(this.getClass().getName())
                    .setLoggerFqcn("org.apache.logging.log4j.core.Logger")
                    .setLevel(Level.INFO)
                    .setMessage(new SimpleMessage("Hello, world 1!"))
                    .build();
            final String result1 = layout.toSerializable(event1);
            String name = this.getClass()
                    .getName()
                    .substring(0, this.getClass().getName().lastIndexOf("."));
            name = name.substring(0, name.lastIndexOf("."));
            assertEquals(
                    this.getClass().getName().substring(name.length() + 1) + " Hello, world 1!", new String(result1));
        }
        {
            final PatternLayout layout = PatternLayout.newBuilder()
                    .withPattern("%c{20} %m")
                    .withConfiguration(ctx.getConfiguration())
                    .build();
            final LogEvent event1 = Log4jLogEvent.newBuilder()
                    .setLoggerName(this.getClass().getName())
                    .setLoggerFqcn("org.apache.logging.log4j.core.Logger")
                    .setLevel(Level.INFO)
                    .setMessage(new SimpleMessage("Hello, world 1!"))
                    .build();
            final String result1 = layout.toSerializable(event1);
            assertEquals(this.getClass().getName() + " Hello, world 1!", new String(result1));
        }
    }

    @Test
    void testCallersFqcnTruncationByRetainingPartsFromEnd() {
        {
            final PatternLayout layout = PatternLayout.newBuilder()
                    .withPattern("%C{1} %m")
                    .withConfiguration(ctx.getConfiguration())
                    .build();
            final LogEvent event1 = Log4jLogEvent.newBuilder()
                    .setLoggerName(this.getClass().getName())
                    .setLoggerFqcn("org.apache.logging.log4j.core.Logger")
                    .setLevel(Level.INFO)
                    .setMessage(new SimpleMessage("Hello, world 1!"))
                    .setSource(new StackTraceElement(
                            this.getClass().getName(),
                            "testCallersFqcnTruncationByRetainingPartsFromEnd",
                            this.getClass().getCanonicalName() + ".java",
                            440))
                    .build();
            final String result1 = layout.toSerializable(event1);
            assertEquals(
                    this.getClass()
                                    .getName()
                                    .substring(this.getClass().getName().lastIndexOf(".") + 1) + " Hello, world 1!",
                    new String(result1));
        }
        {
            final PatternLayout layout = PatternLayout.newBuilder()
                    .withPattern("%C{2} %m")
                    .withConfiguration(ctx.getConfiguration())
                    .build();
            final LogEvent event1 = Log4jLogEvent.newBuilder()
                    .setLoggerName(this.getClass().getName())
                    .setLoggerFqcn("org.apache.logging.log4j.core.Logger")
                    .setLevel(Level.INFO)
                    .setMessage(new SimpleMessage("Hello, world 1!"))
                    .setSource(new StackTraceElement(
                            this.getClass().getName(),
                            "testCallersFqcnTruncationByRetainingPartsFromEnd",
                            this.getClass().getCanonicalName() + ".java",
                            440))
                    .build();
            final String result1 = layout.toSerializable(event1);
            String name = this.getClass()
                    .getName()
                    .substring(0, this.getClass().getName().lastIndexOf("."));
            name = name.substring(0, name.lastIndexOf("."));
            assertEquals(
                    this.getClass().getName().substring(name.length() + 1) + " Hello, world 1!", new String(result1));
        }
        {
            final PatternLayout layout = PatternLayout.newBuilder()
                    .withPattern("%C{20} %m")
                    .withConfiguration(ctx.getConfiguration())
                    .build();
            final LogEvent event1 = Log4jLogEvent.newBuilder()
                    .setLoggerName(this.getClass().getName())
                    .setLoggerFqcn("org.apache.logging.log4j.core.Logger")
                    .setLevel(Level.INFO)
                    .setMessage(new SimpleMessage("Hello, world 1!"))
                    .setSource(new StackTraceElement(
                            this.getClass().getName(),
                            "testCallersFqcnTruncationByRetainingPartsFromEnd",
                            this.getClass().getCanonicalName() + ".java",
                            440))
                    .build();
            final String result1 = layout.toSerializable(event1);
            assertEquals(this.getClass().getName() + " Hello, world 1!", new String(result1));
        }
        {
            final PatternLayout layout = PatternLayout.newBuilder()
                    .withPattern("%class{1} %m")
                    .withConfiguration(ctx.getConfiguration())
                    .build();
            final LogEvent event1 = Log4jLogEvent.newBuilder()
                    .setLoggerName(this.getClass().getName())
                    .setLoggerFqcn("org.apache.logging.log4j.core.Logger")
                    .setLevel(Level.INFO)
                    .setMessage(new SimpleMessage("Hello, world 1!"))
                    .setSource(new StackTraceElement(
                            this.getClass().getName(),
                            "testCallersFqcnTruncationByRetainingPartsFromEnd",
                            this.getClass().getCanonicalName() + ".java",
                            440))
                    .build();
            final String result1 = layout.toSerializable(event1);
            assertEquals(
                    this.getClass()
                                    .getName()
                                    .substring(this.getClass().getName().lastIndexOf(".") + 1) + " Hello, world 1!",
                    new String(result1));
        }
    }

    @Test
    void testLoggerNameTruncationByDroppingPartsFromFront() {
        {
            final PatternLayout layout = PatternLayout.newBuilder()
                    .withPattern("%c{-1} %m")
                    .withConfiguration(ctx.getConfiguration())
                    .build();
            final LogEvent event1 = Log4jLogEvent.newBuilder()
                    .setLoggerName(this.getClass().getName())
                    .setLoggerFqcn("org.apache.logging.log4j.core.Logger")
                    .setLevel(Level.INFO)
                    .setMessage(new SimpleMessage("Hello, world 1!"))
                    .build();
            final String result1 = layout.toSerializable(event1);
            final String name = this.getClass()
                    .getName()
                    .substring(this.getClass().getName().indexOf(".") + 1);
            assertEquals(name + " Hello, world 1!", new String(result1));
        }
        {
            final PatternLayout layout = PatternLayout.newBuilder()
                    .withPattern("%c{-3} %m")
                    .withConfiguration(ctx.getConfiguration())
                    .build();
            final LogEvent event1 = Log4jLogEvent.newBuilder()
                    .setLoggerName(this.getClass().getName())
                    .setLoggerFqcn("org.apache.logging.log4j.core.Logger")
                    .setLevel(Level.INFO)
                    .setMessage(new SimpleMessage("Hello, world 1!"))
                    .build();
            final String result1 = layout.toSerializable(event1);
            String name = this.getClass()
                    .getName()
                    .substring(this.getClass().getName().indexOf(".") + 1);
            name = name.substring(name.indexOf(".") + 1);
            name = name.substring(name.indexOf(".") + 1);
            assertEquals(name + " Hello, world 1!", new String(result1));
        }
        {
            final PatternLayout layout = PatternLayout.newBuilder()
                    .withPattern("%logger{-3} %m")
                    .withConfiguration(ctx.getConfiguration())
                    .build();
            final LogEvent event1 = Log4jLogEvent.newBuilder()
                    .setLoggerName(this.getClass().getName())
                    .setLoggerFqcn("org.apache.logging.log4j.core.Logger")
                    .setLevel(Level.INFO)
                    .setMessage(new SimpleMessage("Hello, world 1!"))
                    .build();
            final String result1 = layout.toSerializable(event1);
            String name = this.getClass()
                    .getName()
                    .substring(this.getClass().getName().indexOf(".") + 1);
            name = name.substring(name.indexOf(".") + 1);
            name = name.substring(name.indexOf(".") + 1);
            assertEquals(name + " Hello, world 1!", new String(result1));
        }
        {
            final PatternLayout layout = PatternLayout.newBuilder()
                    .withPattern("%c{-20} %m")
                    .withConfiguration(ctx.getConfiguration())
                    .build();
            final LogEvent event1 = Log4jLogEvent.newBuilder()
                    .setLoggerName(this.getClass().getName())
                    .setLoggerFqcn("org.apache.logging.log4j.core.Logger")
                    .setLevel(Level.INFO)
                    .setMessage(new SimpleMessage("Hello, world 1!"))
                    .build();
            final String result1 = layout.toSerializable(event1);
            assertEquals(this.getClass().getName() + " Hello, world 1!", new String(result1));
        }
    }

    @Test
    void testCallersFqcnTruncationByDroppingPartsFromFront() {
        {
            final PatternLayout layout = PatternLayout.newBuilder()
                    .withPattern("%C{-1} %m")
                    .withConfiguration(ctx.getConfiguration())
                    .build();
            final LogEvent event1 = Log4jLogEvent.newBuilder()
                    .setLoggerName(this.getClass().getName())
                    .setLoggerFqcn("org.apache.logging.log4j.core.Logger")
                    .setLevel(Level.INFO)
                    .setMessage(new SimpleMessage("Hello, world 1!"))
                    .setSource(new StackTraceElement(
                            this.getClass().getName(),
                            "testCallersFqcnTruncationByDroppingPartsFromFront",
                            this.getClass().getCanonicalName() + ".java",
                            546))
                    .build();
            final String result1 = layout.toSerializable(event1);
            final String name = this.getClass()
                    .getName()
                    .substring(this.getClass().getName().indexOf(".") + 1);
            assertEquals(name + " Hello, world 1!", new String(result1));
        }
        {
            final PatternLayout layout = PatternLayout.newBuilder()
                    .withPattern("%C{-3} %m")
                    .withConfiguration(ctx.getConfiguration())
                    .build();
            final LogEvent event1 = Log4jLogEvent.newBuilder()
                    .setLoggerName(this.getClass().getName())
                    .setLoggerFqcn("org.apache.logging.log4j.core.Logger")
                    .setLevel(Level.INFO)
                    .setMessage(new SimpleMessage("Hello, world 1!"))
                    .setSource(new StackTraceElement(
                            this.getClass().getName(),
                            "testCallersFqcnTruncationByDroppingPartsFromFront",
                            this.getClass().getCanonicalName() + ".java",
                            546))
                    .build();
            final String result1 = layout.toSerializable(event1);
            String name = this.getClass()
                    .getName()
                    .substring(this.getClass().getName().indexOf(".") + 1);
            name = name.substring(name.indexOf(".") + 1);
            name = name.substring(name.indexOf(".") + 1);
            assertEquals(name + " Hello, world 1!", new String(result1));
        }
        {
            final PatternLayout layout = PatternLayout.newBuilder()
                    .withPattern("%class{-3} %m")
                    .withConfiguration(ctx.getConfiguration())
                    .build();
            final LogEvent event1 = Log4jLogEvent.newBuilder()
                    .setLoggerName(this.getClass().getName())
                    .setLoggerFqcn("org.apache.logging.log4j.core.Logger")
                    .setLevel(Level.INFO)
                    .setMessage(new SimpleMessage("Hello, world 1!"))
                    .setSource(new StackTraceElement(
                            this.getClass().getName(),
                            "testCallersFqcnTruncationByDroppingPartsFromFront",
                            this.getClass().getCanonicalName() + ".java",
                            546))
                    .build();
            final String result1 = layout.toSerializable(event1);
            String name = this.getClass()
                    .getName()
                    .substring(this.getClass().getName().indexOf(".") + 1);
            name = name.substring(name.indexOf(".") + 1);
            name = name.substring(name.indexOf(".") + 1);
            assertEquals(name + " Hello, world 1!", new String(result1));
        }
        {
            final PatternLayout layout = PatternLayout.newBuilder()
                    .withPattern("%C{-20} %m")
                    .withConfiguration(ctx.getConfiguration())
                    .build();
            final LogEvent event1 = Log4jLogEvent.newBuilder()
                    .setLoggerName(this.getClass().getName())
                    .setLoggerFqcn("org.apache.logging.log4j.core.Logger")
                    .setLevel(Level.INFO)
                    .setMessage(new SimpleMessage("Hello, world 1!"))
                    .setSource(new StackTraceElement(
                            this.getClass().getName(),
                            "testCallersFqcnTruncationByDroppingPartsFromFront",
                            this.getClass().getCanonicalName() + ".java",
                            546))
                    .build();
            final String result1 = layout.toSerializable(event1);
            assertEquals(this.getClass().getName() + " Hello, world 1!", new String(result1));
        }
    }
}
