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
package org.apache.logging.log4j.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;
import org.apache.logging.log4j.test.junit.Mutable;
import org.apache.logging.log4j.test.junit.SerialUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests ReusableParameterizedMessage.
 */
public class ReusableParameterizedMessageTest {

    public static ReusableParameterizedMessage set(
            final ReusableParameterizedMessage msg, final String format, final Object... params) {

        return msg.set(format, params);
    }

    @Test
    public void testNoArgs() {
        final String testMsg = "Test message {}";
        final ReusableParameterizedMessage msg = new ReusableParameterizedMessage();
        msg.set(testMsg, (Object[]) null);
        String result = msg.getFormattedMessage();
        assertEquals(testMsg, result);

        msg.set(testMsg, (Object) null);
        result = msg.getFormattedMessage();
        assertEquals("Test message null", result);

        msg.set(testMsg, null, null);
        result = msg.getFormattedMessage();
        assertEquals("Test message null", result);
    }

    @Test
    public void testFormat3StringArgs() {
        final String testMsg = "Test message {}{} {}";
        final String[] args = {"a", "b", "c"};
        final String result =
                new ReusableParameterizedMessage().set(testMsg, (Object[]) args).getFormattedMessage();
        assertEquals("Test message ab c", result);
    }

    @Test
    public void testFormatNullArgs() {
        final String testMsg = "Test message {} {} {} {} {} {}";
        final String[] args = {"a", null, "c", null, null, null};
        final String result =
                new ReusableParameterizedMessage().set(testMsg, (Object[]) args).getFormattedMessage();
        assertEquals("Test message a null c null null null", result);
    }

    @Test
    public void testFormatStringArgsIgnoresSuperfluousArgs() {
        final String testMsg = "Test message {}{} {}";
        final String[] args = {"a", "b", "c", "unnecessary", "superfluous"};
        final String result =
                new ReusableParameterizedMessage().set(testMsg, (Object[]) args).getFormattedMessage();
        assertEquals("Test message ab c", result);
    }

    @Test
    public void testFormatStringArgsWithEscape() {
        final String testMsg = "Test message \\{}{} {}";
        final String[] args = {"a", "b", "c"};
        final String result =
                new ReusableParameterizedMessage().set(testMsg, (Object[]) args).getFormattedMessage();
        assertEquals("Test message {}a b", result);
    }

    @Test
    public void testFormatStringArgsWithTrailingEscape() {
        final String testMsg = "Test message {}{} {}\\";
        final String[] args = {"a", "b", "c"};
        final String result =
                new ReusableParameterizedMessage().set(testMsg, (Object[]) args).getFormattedMessage();
        assertEquals("Test message ab c\\", result);
    }

    @Test
    public void testFormatStringArgsWithTrailingText() {
        final String testMsg = "Test message {}{} {}Text";
        final String[] args = {"a", "b", "c"};
        final String result =
                new ReusableParameterizedMessage().set(testMsg, (Object[]) args).getFormattedMessage();
        assertEquals("Test message ab cText", result);
    }

    @Test
    public void testFormatStringArgsWithTrailingEscapedEscape() {
        final String testMsg = "Test message {}{} {}\\\\";
        final String[] args = {"a", "b", "c"};
        final String result =
                new ReusableParameterizedMessage().set(testMsg, (Object[]) args).getFormattedMessage();
        assertEquals("Test message ab c\\", result);
    }

    @Test
    public void testFormatStringArgsWithEscapedEscape() {
        final String testMsg = "Test message \\\\{}{} {}";
        final Object[] args = {"a", "b", "c"};
        final String result =
                new ReusableParameterizedMessage().set(testMsg, args).getFormattedMessage();
        assertEquals("Test message \\ab c", result);
    }

    @Test
    public void testNotSafeWithMutableParams() {
        final String testMsg = "Test message {}";
        final Mutable param = new Mutable().set("abc");
        final ReusableParameterizedMessage msg = new ReusableParameterizedMessage();
        msg.set(testMsg, param);

        // modify parameter before calling msg.getFormattedMessage
        param.set("XYZ");
        final String actual = msg.getFormattedMessage();
        assertEquals("Test message XYZ", actual, "Should use current param value");

        // modify parameter after calling msg.getFormattedMessage
        param.set("000");
        final String after = msg.getFormattedMessage();
        assertEquals("Test message 000", after, "Renders again");
    }

    @Test
    public void testThrowable() {
        final String testMsg = "Test message {}";
        final ReusableParameterizedMessage msg = new ReusableParameterizedMessage();
        final Throwable EXCEPTION1 = new IllegalAccessError("#1");
        msg.set(testMsg, "msg", EXCEPTION1);
        assertSame(EXCEPTION1, msg.getThrowable());

        final Throwable EXCEPTION2 = new UnsupportedOperationException("#2");
        msg.set(testMsg, "msgs", EXCEPTION2);
        assertSame(EXCEPTION2, msg.getThrowable());
    }

    @Test
    public void testParameterConsumer() {
        final String testMsg = "Test message {}";
        final ReusableParameterizedMessage msg = new ReusableParameterizedMessage();
        final Throwable EXCEPTION1 = new IllegalAccessError("#1");
        msg.set(testMsg, "msg", EXCEPTION1);
        final List<Object> expected = new LinkedList<>();
        expected.add("msg");
        expected.add(EXCEPTION1);
        final List<Object> actual = new LinkedList<>();
        msg.forEachParameter((parameter, parameterIndex, state) -> actual.add(parameter), null);
        assertEquals(expected, actual);
    }

    static Stream<Object> testSerializable() {
        return Stream.of("World", new Object(), null);
    }

    @ParameterizedTest
    @MethodSource
    void testSerializable(final Object arg) {
        final ReusableParameterizedMessage expected = new ReusableParameterizedMessage();
        expected.set("Hello {}!", arg);
        final Message actual = SerialUtil.deserialize(SerialUtil.serialize(expected));
        assertThat(actual).isInstanceOf(ParameterizedMessage.class);
        assertThat(actual.getFormattedMessage()).isEqualTo(expected.getFormattedMessage());
    }
}
