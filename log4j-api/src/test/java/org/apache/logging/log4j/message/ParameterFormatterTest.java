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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests ParameterFormatter.
 */
public class ParameterFormatterTest {

    @Test
    public void testCountArgumentPlaceholders() throws Exception {
        assertThat(ParameterFormatter.countArgumentPlaceholders("")).isEqualTo(0);
        assertThat(ParameterFormatter.countArgumentPlaceholders("aaa")).isEqualTo(0);
        assertThat(ParameterFormatter.countArgumentPlaceholders("\\{}")).isEqualTo(0);
        assertThat(ParameterFormatter.countArgumentPlaceholders("{}")).isEqualTo(1);
        assertThat(ParameterFormatter.countArgumentPlaceholders("{}\\{}")).isEqualTo(1);
        assertThat(ParameterFormatter.countArgumentPlaceholders("{}{}")).isEqualTo(2);
        assertThat(ParameterFormatter.countArgumentPlaceholders("{}{}{}")).isEqualTo(3);
        assertThat(ParameterFormatter.countArgumentPlaceholders("{}{}{}aa{}")).isEqualTo(4);
        assertThat(ParameterFormatter.countArgumentPlaceholders("{}{}{}a{]b{}")).isEqualTo(4);
        assertThat(ParameterFormatter.countArgumentPlaceholders("{}{}{}a{}b{}")).isEqualTo(5);
    }

    @Test
    public void testFormat3StringArgs() {
        final String testMsg = "Test message {}{} {}";
        final String[] args = { "a", "b", "c" };
        final String result = ParameterFormatter.format(testMsg, args);
        assertThat(result).isEqualTo("Test message ab c");
    }

    @Test
    public void testFormatNullArgs() {
        final String testMsg = "Test message {} {} {} {} {} {}";
        final String[] args = { "a", null, "c", null, null, null };
        final String result = ParameterFormatter.format(testMsg, args);
        assertThat(result).isEqualTo("Test message a null c null null null");
    }

    @Test
    public void testFormatStringArgsIgnoresSuperfluousArgs() {
        final String testMsg = "Test message {}{} {}";
        final String[] args = { "a", "b", "c", "unnecessary", "superfluous" };
        final String result = ParameterFormatter.format(testMsg, args);
        assertThat(result).isEqualTo("Test message ab c");
    }

    @Test
    public void testFormatStringArgsWithEscape() {
        final String testMsg = "Test message \\{}{} {}";
        final String[] args = { "a", "b", "c" };
        final String result = ParameterFormatter.format(testMsg, args);
        assertThat(result).isEqualTo("Test message {}a b");
    }

    @Test
    public void testFormatStringArgsWithTrailingEscape() {
        final String testMsg = "Test message {}{} {}\\";
        final String[] args = { "a", "b", "c" };
        final String result = ParameterFormatter.format(testMsg, args);
        assertThat(result).isEqualTo("Test message ab c\\");
    }

    @Test
    public void testFormatStringArgsWithTrailingEscapedEscape() {
        final String testMsg = "Test message {}{} {}\\\\";
        final String[] args = { "a", "b", "c" };
        final String result = ParameterFormatter.format(testMsg, args);
        assertThat(result).isEqualTo("Test message ab c\\\\");
    }

    @Test
    public void testFormatStringArgsWithEscapedEscape() {
        final String testMsg = "Test message \\\\{}{} {}";
        final String[] args = { "a", "b", "c" };
        final String result = ParameterFormatter.format(testMsg, args);
        assertThat(result).isEqualTo("Test message \\ab c");
    }

    @Test
    public void testFormatMessage3StringArgs() {
        final String testMsg = "Test message {}{} {}";
        final String[] args = { "a", "b", "c" };
        final StringBuilder sb = new StringBuilder();
        ParameterFormatter.formatMessage(sb, testMsg, args, 3);
        final String result = sb.toString();
        assertThat(result).isEqualTo("Test message ab c");
    }

    @Test
    public void testFormatMessageNullArgs() {
        final String testMsg = "Test message {} {} {} {} {} {}";
        final String[] args = { "a", null, "c", null, null, null };
        final StringBuilder sb = new StringBuilder();
        ParameterFormatter.formatMessage(sb, testMsg, args, 6);
        final String result = sb.toString();
        assertThat(result).isEqualTo("Test message a null c null null null");
    }

    @Test
    public void testFormatMessageStringArgsIgnoresSuperfluousArgs() {
        final String testMsg = "Test message {}{} {}";
        final String[] args = { "a", "b", "c", "unnecessary", "superfluous" };
        final StringBuilder sb = new StringBuilder();
        ParameterFormatter.formatMessage(sb, testMsg, args, 5);
        final String result = sb.toString();
        assertThat(result).isEqualTo("Test message ab c");
    }

    @Test
    public void testFormatMessageStringArgsWithEscape() {
        final String testMsg = "Test message \\{}{} {}";
        final String[] args = { "a", "b", "c" };
        final StringBuilder sb = new StringBuilder();
        ParameterFormatter.formatMessage(sb, testMsg, args, 3);
        final String result = sb.toString();
        assertThat(result).isEqualTo("Test message {}a b");
    }

    @Test
    public void testFormatMessageStringArgsWithTrailingEscape() {
        final String testMsg = "Test message {}{} {}\\";
        final String[] args = { "a", "b", "c" };
        final StringBuilder sb = new StringBuilder();
        ParameterFormatter.formatMessage(sb, testMsg, args, 3);
        final String result = sb.toString();
        assertThat(result).isEqualTo("Test message ab c\\");
    }

    @Test
    public void testFormatMessageStringArgsWithTrailingEscapedEscape() {
        final String testMsg = "Test message {}{} {}\\\\";
        final String[] args = { "a", "b", "c" };
        final StringBuilder sb = new StringBuilder();
        ParameterFormatter.formatMessage(sb, testMsg, args, 3);
        final String result = sb.toString();
        assertThat(result).isEqualTo("Test message ab c\\\\");
    }

    @Test
    public void testFormatMessageStringArgsWithEscapedEscape() {
        final String testMsg = "Test message \\\\{}{} {}";
        final String[] args = { "a", "b", "c" };
        final StringBuilder sb = new StringBuilder();
        ParameterFormatter.formatMessage(sb, testMsg, args, 3);
        final String result = sb.toString();
        assertThat(result).isEqualTo("Test message \\ab c");
    }

    @Test
    public void testDeepToString() throws Exception {
        final List<Object> list = new ArrayList<>();
        list.add(1);
        list.add(list);
        list.add(2);
        final String actual = ParameterFormatter.deepToString(list);
        final String expected = "[1, [..." + ParameterFormatter.identityToString(list) + "...], 2]";
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testIdentityToString() throws Exception {
        final List<Object> list = new ArrayList<>();
        list.add(1);
        list.add(list);
        list.add(2);
        final String actual = ParameterFormatter.identityToString(list);
        final String expected = list.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(list));
        assertThat(actual).isEqualTo(expected);
    }
}
