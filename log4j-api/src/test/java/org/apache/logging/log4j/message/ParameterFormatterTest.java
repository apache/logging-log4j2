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
package org.apache.logging.log4j.message;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests ParameterFormatter.
 */
public class ParameterFormatterTest {

    @Test
    public void testCountArgumentPlaceholders() throws Exception {
        assertEquals(0, ParameterFormatter.countArgumentPlaceholders(""));
        assertEquals(0, ParameterFormatter.countArgumentPlaceholders("aaa"));
        assertEquals(0, ParameterFormatter.countArgumentPlaceholders("\\{}"));
        assertEquals(1, ParameterFormatter.countArgumentPlaceholders("{}"));
        assertEquals(1, ParameterFormatter.countArgumentPlaceholders("{}\\{}"));
        assertEquals(2, ParameterFormatter.countArgumentPlaceholders("{}{}"));
        assertEquals(3, ParameterFormatter.countArgumentPlaceholders("{}{}{}"));
        assertEquals(4, ParameterFormatter.countArgumentPlaceholders("{}{}{}aa{}"));
        assertEquals(4, ParameterFormatter.countArgumentPlaceholders("{}{}{}a{]b{}"));
        assertEquals(5, ParameterFormatter.countArgumentPlaceholders("{}{}{}a{}b{}"));
    }

    @Test
    public void testFormat3StringArgs() {
        final String testMsg = "Test message {}{} {}";
        final String[] args = { "a", "b", "c" };
        final String result = ParameterFormatter.format(testMsg, args);
        assertEquals("Test message ab c", result);
    }

    @Test
    public void testFormatNullArgs() {
        final String testMsg = "Test message {} {} {} {} {} {}";
        final String[] args = { "a", null, "c", null, null, null };
        final String result = ParameterFormatter.format(testMsg, args);
        assertEquals("Test message a null c null null null", result);
    }

    @Test
    public void testFormatStringArgsIgnoresSuperfluousArgs() {
        final String testMsg = "Test message {}{} {}";
        final String[] args = { "a", "b", "c", "unnecessary", "superfluous" };
        final String result = ParameterFormatter.format(testMsg, args);
        assertEquals("Test message ab c", result);
    }

    @Test
    public void testFormatStringArgsWithEscape() {
        final String testMsg = "Test message \\{}{} {}";
        final String[] args = { "a", "b", "c" };
        final String result = ParameterFormatter.format(testMsg, args);
        assertEquals("Test message {}a b", result);
    }

    @Test
    public void testFormatStringArgsWithTrailingEscape() {
        final String testMsg = "Test message {}{} {}\\";
        final String[] args = { "a", "b", "c" };
        final String result = ParameterFormatter.format(testMsg, args);
        assertEquals("Test message ab c\\", result);
    }

    @Test
    public void testFormatStringArgsWithTrailingEscapedEscape() {
        final String testMsg = "Test message {}{} {}\\\\";
        final String[] args = { "a", "b", "c" };
        final String result = ParameterFormatter.format(testMsg, args);
        assertEquals("Test message ab c\\\\", result);
    }

    @Test
    public void testFormatStringArgsWithEscapedEscape() {
        final String testMsg = "Test message \\\\{}{} {}";
        final String[] args = { "a", "b", "c" };
        final String result = ParameterFormatter.format(testMsg, args);
        assertEquals("Test message \\ab c", result);
    }

    @Test
    public void testFormatMessage3StringArgs() {
        final String testMsg = "Test message {}{} {}";
        final String[] args = { "a", "b", "c" };
        final StringBuilder sb = new StringBuilder();
        ParameterFormatter.formatMessage(sb, testMsg, args, 3);
        final String result = sb.toString();
        assertEquals("Test message ab c", result);
    }

    @Test
    public void testFormatMessageNullArgs() {
        final String testMsg = "Test message {} {} {} {} {} {}";
        final String[] args = { "a", null, "c", null, null, null };
        final StringBuilder sb = new StringBuilder();
        ParameterFormatter.formatMessage(sb, testMsg, args, 6);
        final String result = sb.toString();
        assertEquals("Test message a null c null null null", result);
    }

    @Test
    public void testFormatMessageStringArgsIgnoresSuperfluousArgs() {
        final String testMsg = "Test message {}{} {}";
        final String[] args = { "a", "b", "c", "unnecessary", "superfluous" };
        final StringBuilder sb = new StringBuilder();
        ParameterFormatter.formatMessage(sb, testMsg, args, 5);
        final String result = sb.toString();
        assertEquals("Test message ab c", result);
    }

    @Test
    public void testFormatMessageStringArgsWithEscape() {
        final String testMsg = "Test message \\{}{} {}";
        final String[] args = { "a", "b", "c" };
        final StringBuilder sb = new StringBuilder();
        ParameterFormatter.formatMessage(sb, testMsg, args, 3);
        final String result = sb.toString();
        assertEquals("Test message {}a b", result);
    }

    @Test
    public void testFormatMessageStringArgsWithTrailingEscape() {
        final String testMsg = "Test message {}{} {}\\";
        final String[] args = { "a", "b", "c" };
        final StringBuilder sb = new StringBuilder();
        ParameterFormatter.formatMessage(sb, testMsg, args, 3);
        final String result = sb.toString();
        assertEquals("Test message ab c\\", result);
    }

    @Test
    public void testFormatMessageStringArgsWithTrailingEscapedEscape() {
        final String testMsg = "Test message {}{} {}\\\\";
        final String[] args = { "a", "b", "c" };
        final StringBuilder sb = new StringBuilder();
        ParameterFormatter.formatMessage(sb, testMsg, args, 3);
        final String result = sb.toString();
        assertEquals("Test message ab c\\\\", result);
    }

    @Test
    public void testFormatMessageStringArgsWithEscapedEscape() {
        final String testMsg = "Test message \\\\{}{} {}";
        final String[] args = { "a", "b", "c" };
        final StringBuilder sb = new StringBuilder();
        ParameterFormatter.formatMessage(sb, testMsg, args, 3);
        final String result = sb.toString();
        assertEquals("Test message \\ab c", result);
    }

    @Test
    public void testDeepToString() throws Exception {
        final List<Object> list = new ArrayList<>();
        list.add(1);
        list.add(list);
        list.add(2);
        final String actual = ParameterFormatter.deepToString(list);
        final String expected = "[1, [..." + ParameterFormatter.identityToString(list) + "...], 2]";
        assertEquals(expected, actual);
    }

    @Test
    public void testIdentityToString() throws Exception {
        final List<Object> list = new ArrayList<>();
        list.add(1);
        list.add(list);
        list.add(2);
        final String actual = ParameterFormatter.identityToString(list);
        final String expected = list.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(list));
        assertEquals(expected, actual);
    }
}