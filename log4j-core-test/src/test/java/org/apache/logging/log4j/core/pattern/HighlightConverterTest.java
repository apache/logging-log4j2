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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.test.ListStatusListener;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests the HighlightConverter.
 */
public class HighlightConverterTest {

    @Test
    public void testAnsiEmpty() {
        final String[] options = {
            "", PatternParser.NO_CONSOLE_NO_ANSI + "=false, " + PatternParser.DISABLE_ANSI + "=false"
        };
        final HighlightConverter converter = HighlightConverter.newInstance(null, options);
        assertNotNull(converter);

        final LogEvent event = Log4jLogEvent.newBuilder()
                .setLevel(Level.INFO)
                .setLoggerName("a.b.c")
                .setMessage(new SimpleMessage("message in a bottle"))
                .build();
        final StringBuilder buffer = new StringBuilder();
        converter.format(event, buffer);
        assertEquals("", buffer.toString());
    }

    @Test
    public void testAnsiNonEmpty() {
        final String[] options = {
            "%-5level: %msg", PatternParser.NO_CONSOLE_NO_ANSI + "=false, " + PatternParser.DISABLE_ANSI + "=false"
        };
        final HighlightConverter converter = HighlightConverter.newInstance(null, options);
        assertNotNull(converter);

        final LogEvent event = Log4jLogEvent.newBuilder()
                .setLevel(Level.INFO)
                .setLoggerName("a.b.c")
                .setMessage(new SimpleMessage("message in a bottle"))
                .build();
        final StringBuilder buffer = new StringBuilder();
        converter.format(event, buffer);
        assertEquals("\u001B[32mINFO : message in a bottle\u001B[m", buffer.toString());
    }

    @Test
    public void testLevelNamesBad() {
        final String colorName = "red";
        final String[] options = {
            "%-5level: %msg",
            PatternParser.NO_CONSOLE_NO_ANSI + "=false, " + PatternParser.DISABLE_ANSI + "=false, " + "BAD_LEVEL_A="
                    + colorName + ", BAD_LEVEL_B=" + colorName
        };
        final HighlightConverter converter = HighlightConverter.newInstance(null, options);
        assertNotNull(converter);
        assertNotNull(converter.getLevelStyle(Level.TRACE));
        assertNotNull(converter.getLevelStyle(Level.DEBUG));
    }

    @Test
    public void testLevelNamesGood() {
        final String colorName = "red";
        final String[] options = {
            "%-5level: %msg",
            PatternParser.NO_CONSOLE_NO_ANSI + "=false, " + PatternParser.DISABLE_ANSI + "=false, " + "DEBUG="
                    + colorName + ", TRACE=" + colorName
        };
        final HighlightConverter converter = HighlightConverter.newInstance(null, options);
        assertNotNull(converter);
        assertEquals(AnsiEscape.createSequence(colorName), converter.getLevelStyle(Level.TRACE));
        assertEquals(AnsiEscape.createSequence(colorName), converter.getLevelStyle(Level.DEBUG));
    }

    static Stream<Arguments> colors() {
        return Stream.of(
                Arguments.of("bright red", "\u001B[1;31m"),
                Arguments.of("red bright", "\u001B[31;1m"),
                Arguments.of("bright_red", "\u001B[91m"),
                Arguments.of("#1cd42b", "\u001B[38;2;28;212;43m"),
                Arguments.of("fg_bright_red bg_bright_blue bold", "\u001B[91;104;1m"),
                Arguments.of("FG_#1cd42b BG_#000000", "\u001B[38;2;28;212;43;48;2;0;0;0m"));
    }

    @ParameterizedTest
    @MethodSource("colors")
    public void testColors(final String colorName, final String escape) {
        final String[] options = {
            "%-5level: %msg",
            PatternParser.NO_CONSOLE_NO_ANSI + "=false, " + PatternParser.DISABLE_ANSI + "=false, " + "INFO="
                    + colorName
        };
        final HighlightConverter converter = HighlightConverter.newInstance(null, options);
        assertNotNull(converter);

        final LogEvent event = Log4jLogEvent.newBuilder().setLevel(Level.INFO).build();
        final StringBuilder buffer = new StringBuilder();
        converter.format(event, buffer);
        assertEquals(escape + "INFO : \u001B[m", buffer.toString());
    }

    @Test
    public void testLevelNamesUnknown() {
        final String colorName = "blue";
        final String[] options = {
            "%level",
            PatternParser.NO_CONSOLE_NO_ANSI + "=false, " + PatternParser.DISABLE_ANSI + "=false, " + "DEBUG="
                    + colorName + ", CUSTOM1=" + colorName
        };
        final HighlightConverter converter = HighlightConverter.newInstance(null, options);
        assertNotNull(converter);
        assertNotNull(converter.getLevelStyle(Level.INFO));
        assertNotNull(converter.getLevelStyle(Level.DEBUG));
        assertNotNull(converter.getLevelStyle(Level.forName("CUSTOM1", 412)));
        assertNull(converter.getLevelStyle(Level.forName("CUSTOM2", 512)));

        assertArrayEquals(
                new byte[] {27, '[', '3', '4', 'm', 'D', 'E', 'B', 'U', 'G', 27, '[', 'm'},
                toFormattedCharSeq(converter, Level.DEBUG).toString().getBytes());
        assertArrayEquals(
                new byte[] {27, '[', '3', '2', 'm', 'I', 'N', 'F', 'O', 27, '[', 'm'},
                toFormattedCharSeq(converter, Level.INFO).toString().getBytes());
        assertArrayEquals(
                new byte[] {27, '[', '3', '4', 'm', 'C', 'U', 'S', 'T', 'O', 'M', '1', 27, '[', 'm'},
                toFormattedCharSeq(converter, Level.forName("CUSTOM1", 412))
                        .toString()
                        .getBytes());
        assertArrayEquals(
                new byte[] {'C', 'U', 'S', 'T', 'O', 'M', '2'},
                toFormattedCharSeq(converter, Level.forName("CUSTOM2", 512))
                        .toString()
                        .getBytes());
    }

    @Test
    public void testLevelNamesNone() {
        final String[] options = {
            "%-5level: %msg", PatternParser.NO_CONSOLE_NO_ANSI + "=false, " + PatternParser.DISABLE_ANSI + "=false"
        };
        final HighlightConverter converter = HighlightConverter.newInstance(null, options);
        assertNotNull(converter);
        assertNotNull(converter.getLevelStyle(Level.TRACE));
        assertNotNull(converter.getLevelStyle(Level.DEBUG));
    }

    @Test
    @UsingStatusListener
    public void testNoAnsiEmpty(final ListStatusListener listener) {
        final String[] options = {"", PatternParser.DISABLE_ANSI + "=true"};
        final HighlightConverter converter = HighlightConverter.newInstance(null, options);
        assertNotNull(converter);
        assertThat(listener.findStatusData(Level.WARN)).isEmpty();

        final LogEvent event = Log4jLogEvent.newBuilder()
                .setLevel(Level.INFO)
                .setLoggerName("a.b.c")
                .setMessage(new SimpleMessage("message in a bottle"))
                .build();
        final StringBuilder buffer = new StringBuilder();
        converter.format(event, buffer);
        assertEquals("", buffer.toString());
    }

    @Test
    @UsingStatusListener
    public void testNoAnsiNonEmpty(final ListStatusListener listener) {
        final String[] options = {"%-5level: %msg", PatternParser.DISABLE_ANSI + "=true"};
        final HighlightConverter converter = HighlightConverter.newInstance(null, options);
        assertNotNull(converter);
        assertThat(listener.findStatusData(Level.WARN)).isEmpty();

        final LogEvent event = Log4jLogEvent.newBuilder()
                .setLevel(Level.INFO)
                .setLoggerName("a.b.c")
                .setMessage(new SimpleMessage("message in a bottle"))
                .build();
        final StringBuilder buffer = new StringBuilder();
        converter.format(event, buffer);
        assertEquals("INFO : message in a bottle", buffer.toString());
    }

    /*
    This test ensure that a keyvalue pair separated by = sign must be provided in order to configure the highlighting style
     */
    @Test
    @UsingStatusListener
    public void testBadStyleOption(final ListStatusListener listener) {
        String defaultWarnColor = "yellow";
        String defaultInfoColor = "green";
        final String[] options = {
            "%5level",
            PatternParser.NO_CONSOLE_NO_ANSI + "=false, " + PatternParser.DISABLE_ANSI + "=false, " + "LOGBACK"
        };
        final HighlightConverter converter = HighlightConverter.newInstance(null, options);
        assertNotNull(converter);

        // As the default highlighting WARN color is Yellow while the LOGBACK color is Red
        assertEquals(AnsiEscape.createSequence(defaultWarnColor), converter.getLevelStyle(Level.WARN));
        // As the default highlighting INFO color is Green while the LOGBACK color is Blue
        assertEquals(AnsiEscape.createSequence(defaultInfoColor), converter.getLevelStyle(Level.INFO));
        assertThat(listener.findStatusData(Level.WARN)).hasSize(1);
    }

    private CharSequence toFormattedCharSeq(final HighlightConverter converter, final Level level) {
        final StringBuilder sb = new StringBuilder();
        converter.format(Log4jLogEvent.newBuilder().setLevel(level).build(), sb);
        return sb;
    }
}
