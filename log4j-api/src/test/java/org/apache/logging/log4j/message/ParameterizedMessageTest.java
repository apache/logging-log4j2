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

import org.apache.logging.log4j.junit.Mutable;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class ParameterizedMessageTest {

    @Test
    public void testNoArgs() {
        final String testMsg = "Test message {}";
        ParameterizedMessage msg = new ParameterizedMessage(testMsg, (Object[]) null);
        String result = msg.getFormattedMessage();
        assertThat(result).isEqualTo(testMsg);
        final Object[] array = null;
        msg = new ParameterizedMessage(testMsg, array, null);
        result = msg.getFormattedMessage();
        assertThat(result).isEqualTo(testMsg);
    }

    @Test
    public void testZeroLength() {
        final String testMsg = "";
        ParameterizedMessage msg = new ParameterizedMessage(testMsg, new Object[]{"arg"});
        String result = msg.getFormattedMessage();
        assertThat(result).isEqualTo(testMsg);
        final Object[] array = null;
        msg = new ParameterizedMessage(testMsg, array, null);
        result = msg.getFormattedMessage();
        assertThat(result).isEqualTo(testMsg);
    }

    @Test
    public void testOneCharLength() {
        final String testMsg = "d";
        ParameterizedMessage msg = new ParameterizedMessage(testMsg, new Object[]{"arg"});
        String result = msg.getFormattedMessage();
        assertThat(result).isEqualTo(testMsg);
        final Object[] array = null;
        msg = new ParameterizedMessage(testMsg, array, null);
        result = msg.getFormattedMessage();
        assertThat(result).isEqualTo(testMsg);
    }

    @Test
    public void testFormat3StringArgs() {
        final String testMsg = "Test message {}{} {}";
        final String[] args = { "a", "b", "c" };
        final String result = ParameterizedMessage.format(testMsg, args);
        assertThat(result).isEqualTo("Test message ab c");
    }

    @Test
    public void testFormatNullArgs() {
        final String testMsg = "Test message {} {} {} {} {} {}";
        final String[] args = { "a", null, "c", null, null, null };
        final String result = ParameterizedMessage.format(testMsg, args);
        assertThat(result).isEqualTo("Test message a null c null null null");
    }

    @Test
    public void testFormatStringArgsIgnoresSuperfluousArgs() {
        final String testMsg = "Test message {}{} {}";
        final String[] args = { "a", "b", "c", "unnecessary", "superfluous" };
        final String result = ParameterizedMessage.format(testMsg, args);
        assertThat(result).isEqualTo("Test message ab c");
    }

    @Test
    public void testFormatStringArgsWithEscape() {
        final String testMsg = "Test message \\{}{} {}";
        final String[] args = { "a", "b", "c" };
        final String result = ParameterizedMessage.format(testMsg, args);
        assertThat(result).isEqualTo("Test message {}a b");
    }

    @Test
    public void testFormatStringArgsWithTrailingEscape() {
        final String testMsg = "Test message {}{} {}\\";
        final String[] args = { "a", "b", "c" };
        final String result = ParameterizedMessage.format(testMsg, args);
        assertThat(result).isEqualTo("Test message ab c\\");
    }

    @Test
    public void testFormatStringArgsWithTrailingText() {
        final String testMsg = "Test message {}{} {}Text";
        final String[] args = { "a", "b", "c" };
        final String result = ParameterizedMessage.format(testMsg, args);
        assertThat(result).isEqualTo("Test message ab cText");
    }

    @Test
    public void testFormatStringArgsWithTrailingEscapedEscape() {
        final String testMsg = "Test message {}{} {}\\\\";
        final String[] args = { "a", "b", "c" };
        final String result = ParameterizedMessage.format(testMsg, args);
        assertThat(result).isEqualTo("Test message ab c\\\\");
    }

    @Test
    public void testFormatStringArgsWithEscapedEscape() {
        final String testMsg = "Test message \\\\{}{} {}";
        final String[] args = { "a", "b", "c" };
        final String result = ParameterizedMessage.format(testMsg, args);
        assertThat(result).isEqualTo("Test message \\ab c");
    }

    @Test
    public void testSafeWithMutableParams() { // LOG4J2-763
        final String testMsg = "Test message {}";
        final Mutable param = new Mutable().set("abc");
        final ParameterizedMessage msg = new ParameterizedMessage(testMsg, param);

        // modify parameter before calling msg.getFormattedMessage
        param.set("XYZ");
        final String actual = msg.getFormattedMessage();
        assertThat(actual).describedAs("Should use current param value").isEqualTo("Test message XYZ");

        // modify parameter after calling msg.getFormattedMessage
        param.set("000");
        final String after = msg.getFormattedMessage();
        assertThat(after).describedAs("Should not change after rendered once").isEqualTo("Test message XYZ");
    }
}
