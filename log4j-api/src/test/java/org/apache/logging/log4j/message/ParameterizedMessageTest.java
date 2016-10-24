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

import org.apache.logging.log4j.junit.Mutable;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class ParameterizedMessageTest {

    @Test
    public void testNoArgs() {
        final String testMsg = "Test message {}";
        ParameterizedMessage msg = new ParameterizedMessage(testMsg, null);
        String result = msg.getFormattedMessage();
        assertEquals(testMsg, result);
        final Object[] array = null;
        msg = new ParameterizedMessage(testMsg, array, null);
        result = msg.getFormattedMessage();
        assertEquals(testMsg, result);
    }

    @Test
    public void testZeroLength() {
        final String testMsg = "";
        ParameterizedMessage msg = new ParameterizedMessage(testMsg, new String[]{"arg"});
        String result = msg.getFormattedMessage();
        assertEquals(testMsg, result);
        final Object[] array = null;
        msg = new ParameterizedMessage(testMsg, array, null);
        result = msg.getFormattedMessage();
        assertEquals(testMsg, result);
    }

    @Test
    public void testOneCharLength() {
        final String testMsg = "d";
        ParameterizedMessage msg = new ParameterizedMessage(testMsg, new String[]{"arg"});
        String result = msg.getFormattedMessage();
        assertEquals(testMsg, result);
        final Object[] array = null;
        msg = new ParameterizedMessage(testMsg, array, null);
        result = msg.getFormattedMessage();
        assertEquals(testMsg, result);
    }

    @Test
    public void testFormat3StringArgs() {
        final String testMsg = "Test message {}{} {}";
        final String[] args = { "a", "b", "c" };
        final String result = ParameterizedMessage.format(testMsg, args);
        assertEquals("Test message ab c", result);
    }

    @Test
    public void testFormatNullArgs() {
        final String testMsg = "Test message {} {} {} {} {} {}";
        final String[] args = { "a", null, "c", null, null, null };
        final String result = ParameterizedMessage.format(testMsg, args);
        assertEquals("Test message a null c null null null", result);
    }

    @Test
    public void testFormatStringArgsIgnoresSuperfluousArgs() {
        final String testMsg = "Test message {}{} {}";
        final String[] args = { "a", "b", "c", "unnecessary", "superfluous" };
        final String result = ParameterizedMessage.format(testMsg, args);
        assertEquals("Test message ab c", result);
    }

    @Test
    public void testFormatStringArgsWithEscape() {
        final String testMsg = "Test message \\{}{} {}";
        final String[] args = { "a", "b", "c" };
        final String result = ParameterizedMessage.format(testMsg, args);
        assertEquals("Test message {}a b", result);
    }

    @Test
    public void testFormatStringArgsWithTrailingEscape() {
        final String testMsg = "Test message {}{} {}\\";
        final String[] args = { "a", "b", "c" };
        final String result = ParameterizedMessage.format(testMsg, args);
        assertEquals("Test message ab c\\", result);
    }

    @Test
    public void testFormatStringArgsWithTrailingText() {
        final String testMsg = "Test message {}{} {}Text";
        final String[] args = { "a", "b", "c" };
        final String result = ParameterizedMessage.format(testMsg, args);
        assertEquals("Test message ab cText", result);
    }

    @Test
    public void testFormatStringArgsWithTrailingEscapedEscape() {
        final String testMsg = "Test message {}{} {}\\\\";
        final String[] args = { "a", "b", "c" };
        final String result = ParameterizedMessage.format(testMsg, args);
        assertEquals("Test message ab c\\\\", result);
    }

    @Test
    public void testFormatStringArgsWithEscapedEscape() {
        final String testMsg = "Test message \\\\{}{} {}";
        final String[] args = { "a", "b", "c" };
        final String result = ParameterizedMessage.format(testMsg, args);
        assertEquals("Test message \\ab c", result);
    }

    @Test
    public void testSafeWithMutableParams() { // LOG4J2-763
        final String testMsg = "Test message {}";
        final Mutable param = new Mutable().set("abc");
        final ParameterizedMessage msg = new ParameterizedMessage(testMsg, param);

        // modify parameter before calling msg.getFormattedMessage
        param.set("XYZ");
        final String actual = msg.getFormattedMessage();
        assertEquals("Should use current param value", "Test message XYZ", actual);

        // modify parameter after calling msg.getFormattedMessage
        param.set("000");
        final String after = msg.getFormattedMessage();
        assertEquals("Should not change after rendered once", "Test message XYZ", after);
    }
}
