package org.apache.logging.log4j.core.pattern;/*
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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the HighlightConverter.
 */
public class HighlightConverterTest {

    @Test
    public void testAnsiEmpty() {
        final String[] options = {"", PatternParser.NO_CONSOLE_NO_ANSI + "=false, " + PatternParser.DISABLE_ANSI + "=false"};
        final HighlightConverter converter = HighlightConverter.newInstance(null, options);
        assertNotNull(converter);

        final LogEvent event = Log4jLogEvent.newBuilder().setLevel(Level.INFO).setLoggerName("a.b.c").setMessage(
                new SimpleMessage("message in a bottle")).build();
        final StringBuilder buffer = new StringBuilder();
        converter.format(event, buffer);
        assertEquals("", buffer.toString());
    }

    @Test
    public void testAnsiNonEmpty() {
        final String[] options = {"%-5level: %msg", PatternParser.NO_CONSOLE_NO_ANSI + "=false, " + PatternParser.DISABLE_ANSI + "=false"};
        final HighlightConverter converter = HighlightConverter.newInstance(null, options);
        assertNotNull(converter);

        final LogEvent event = Log4jLogEvent.newBuilder().setLevel(Level.INFO).setLoggerName("a.b.c").setMessage(
                new SimpleMessage("message in a bottle")).build();
        final StringBuilder buffer = new StringBuilder();
        converter.format(event, buffer);
        assertEquals("\u001B[32mINFO : message in a bottle\u001B[m", buffer.toString());
    }

    @Test
    public void testLevelNamesBad() {
        final String colorName = "red";
        final String[] options = { "%-5level: %msg", PatternParser.NO_CONSOLE_NO_ANSI + "=false, "
                + PatternParser.DISABLE_ANSI + "=false, " + "BAD_LEVEL_A=" + colorName + ", BAD_LEVEL_B=" + colorName };
        final HighlightConverter converter = HighlightConverter.newInstance(null, options);
        assertNotNull(converter);
        assertNotNull(converter.getLevelStyle(Level.TRACE));
        assertNotNull(converter.getLevelStyle(Level.DEBUG));
    }

    @Test
    public void testLevelNamesGood() {
        final String colorName = "red";
        final String[] options = { "%-5level: %msg", PatternParser.NO_CONSOLE_NO_ANSI + "=false, "
                + PatternParser.DISABLE_ANSI + "=false, " + "DEBUG=" + colorName + ", TRACE=" + colorName };
        final HighlightConverter converter = HighlightConverter.newInstance(null, options);
        assertNotNull(converter);
        assertEquals(AnsiEscape.createSequence(colorName), converter.getLevelStyle(Level.TRACE));
        assertEquals(AnsiEscape.createSequence(colorName), converter.getLevelStyle(Level.DEBUG));
    }

    @Test
    public void testLevelNamesUnknown() {
        final String colorName = "blue";
        final String[] options = { "%level", PatternParser.NO_CONSOLE_NO_ANSI + "=false, " + PatternParser.DISABLE_ANSI
                + "=false, " + "DEBUG=" + colorName + ", CUSTOM1=" + colorName };
        final HighlightConverter converter = HighlightConverter.newInstance(null, options);
        assertNotNull(converter);
        assertNotNull(converter.getLevelStyle(Level.INFO));
        assertNotNull(converter.getLevelStyle(Level.DEBUG));
        assertNotNull(converter.getLevelStyle(Level.forName("CUSTOM1", 412)));
        assertNull(converter.getLevelStyle(Level.forName("CUSTOM2", 512)));

        assertArrayEquals(new byte[] { 27, '[', '3', '4', 'm', 'D', 'E', 'B', 'U', 'G', 27, '[', 'm' },
                toFormattedCharSeq(converter, Level.DEBUG).toString().getBytes());
        assertArrayEquals(new byte[] { 27, '[', '3', '2', 'm', 'I', 'N', 'F', 'O', 27, '[', 'm' },
                toFormattedCharSeq(converter, Level.INFO).toString().getBytes());
        assertArrayEquals(new byte[] { 27, '[', '3', '4', 'm', 'C', 'U', 'S', 'T', 'O', 'M', '1', 27, '[', 'm' },
                toFormattedCharSeq(converter, Level.forName("CUSTOM1", 412)).toString().getBytes());
        assertArrayEquals(new byte[] { 'C', 'U', 'S', 'T', 'O', 'M', '2' },
                toFormattedCharSeq(converter, Level.forName("CUSTOM2", 512)).toString().getBytes());
    }    

    @Test
    public void testLevelNamesNone() {
        final String[] options = { "%-5level: %msg",
                PatternParser.NO_CONSOLE_NO_ANSI + "=false, " + PatternParser.DISABLE_ANSI + "=false" };
        final HighlightConverter converter = HighlightConverter.newInstance(null, options);
        assertNotNull(converter);
        assertNotNull(converter.getLevelStyle(Level.TRACE));
        assertNotNull(converter.getLevelStyle(Level.DEBUG));
    }

    @Test
    public void testNoAnsiEmpty() {
        final String[] options = {"", PatternParser.DISABLE_ANSI + "=true"};
        final HighlightConverter converter = HighlightConverter.newInstance(null, options);
        assertNotNull(converter);

        final LogEvent event = Log4jLogEvent.newBuilder().setLevel(Level.INFO).setLoggerName("a.b.c").setMessage(
                new SimpleMessage("message in a bottle")).build();
        final StringBuilder buffer = new StringBuilder();
        converter.format(event, buffer);
        assertEquals("", buffer.toString());
    }

    @Test
    public void testNoAnsiNonEmpty() {
        final String[] options = {"%-5level: %msg", PatternParser.DISABLE_ANSI + "=true"};
        final HighlightConverter converter = HighlightConverter.newInstance(null, options);
        assertNotNull(converter);

        final LogEvent event = Log4jLogEvent.newBuilder().setLevel(Level.INFO).setLoggerName("a.b.c").setMessage(
                new SimpleMessage("message in a bottle")).build();
        final StringBuilder buffer = new StringBuilder();
        converter.format(event, buffer);
        assertEquals("INFO : message in a bottle", buffer.toString());
    }

    private CharSequence toFormattedCharSeq(final HighlightConverter converter, final Level level) {
        final StringBuilder sb = new StringBuilder();
        converter.format(Log4jLogEvent.newBuilder().setLevel(level).build(), sb);
        return sb;
    }
}
