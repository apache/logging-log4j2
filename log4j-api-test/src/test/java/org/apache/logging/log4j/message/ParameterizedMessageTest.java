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

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.test.ListStatusListener;
import org.apache.logging.log4j.test.junit.Mutable;
import org.apache.logging.log4j.test.junit.SerialUtil;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@UsingStatusListener
class ParameterizedMessageTest {

    final ListStatusListener statusListener;

    ParameterizedMessageTest(ListStatusListener statusListener) {
        this.statusListener = statusListener;
    }

    @Test
    void testNoArgs() {
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
    void testZeroLength() {
        final String testMsg = "";
        ParameterizedMessage msg = new ParameterizedMessage(testMsg, new Object[] {"arg"});
        String result = msg.getFormattedMessage();
        assertThat(result).isEqualTo(testMsg);
        final Object[] array = null;
        msg = new ParameterizedMessage(testMsg, array, null);
        result = msg.getFormattedMessage();
        assertThat(result).isEqualTo(testMsg);
    }

    @Test
    void testOneCharLength() {
        final String testMsg = "d";
        ParameterizedMessage msg = new ParameterizedMessage(testMsg, new Object[] {"arg"});
        String result = msg.getFormattedMessage();
        assertThat(result).isEqualTo(testMsg);
        final Object[] array = null;
        msg = new ParameterizedMessage(testMsg, array, null);
        result = msg.getFormattedMessage();
        assertThat(result).isEqualTo(testMsg);
    }

    @Test
    void testFormat3StringArgs() {
        final String testMsg = "Test message {}{} {}";
        final String[] args = {"a", "b", "c"};
        final String result = ParameterizedMessage.format(testMsg, args);
        assertThat(result).isEqualTo("Test message ab c");
    }

    @Test
    void testFormatNullArgs() {
        final String testMsg = "Test message {} {} {} {} {} {}";
        final String[] args = {"a", null, "c", null, null, null};
        final String result = ParameterizedMessage.format(testMsg, args);
        assertThat(result).isEqualTo("Test message a null c null null null");
    }

    @Test
    void testFormatStringArgsIgnoresSuperfluousArgs() {
        final String testMsg = "Test message {}{} {}";
        final String[] args = {"a", "b", "c", "unnecessary", "superfluous"};
        final String result = ParameterizedMessage.format(testMsg, args);
        assertThat(result).isEqualTo("Test message ab c");
    }

    @Test
    void testFormatStringArgsWithEscape() {
        final String testMsg = "Test message \\{}{} {}";
        final String[] args = {"a", "b", "c"};
        final String result = ParameterizedMessage.format(testMsg, args);
        assertThat(result).isEqualTo("Test message {}a b");
    }

    @Test
    void testFormatStringArgsWithTrailingEscape() {
        final String testMsg = "Test message {}{} {}\\";
        final String[] args = {"a", "b", "c"};
        final String result = ParameterizedMessage.format(testMsg, args);
        assertThat(result).isEqualTo("Test message ab c\\");
    }

    @Test
    void testFormatStringArgsWithTrailingText() {
        final String testMsg = "Test message {}{} {}Text";
        final String[] args = {"a", "b", "c"};
        final String result = ParameterizedMessage.format(testMsg, args);
        assertThat(result).isEqualTo("Test message ab cText");
    }

    @Test
    void testFormatStringArgsWithTrailingEscapedEscape() {
        final String testMsg = "Test message {}{} {}\\\\";
        final String[] args = {"a", "b", "c"};
        final String result = ParameterizedMessage.format(testMsg, args);
        assertThat(result).isEqualTo("Test message ab c\\");
    }

    @Test
    void testFormatStringArgsWithEscapedEscape() {
        final String testMsg = "Test message \\\\{}{} {}";
        final String[] args = {"a", "b", "c"};
        final String result = ParameterizedMessage.format(testMsg, args);
        assertThat(result).isEqualTo("Test message \\ab c");
    }

    @Test
    void testSafeWithMutableParams() { // LOG4J2-763
        final String testMsg = "Test message {}";
        final Mutable param = new Mutable().set("abc");
        final ParameterizedMessage msg = new ParameterizedMessage(testMsg, param);

        // modify parameter before calling msg.getFormattedMessage
        param.set("XYZ");
        final String actual = msg.getFormattedMessage();
        assertThat(actual).isEqualTo("Test message XYZ").as("Should use current param value");

        // modify parameter after calling msg.getFormattedMessage
        param.set("000");
        final String after = msg.getFormattedMessage();
        assertThat(after).isEqualTo("Test message XYZ").as("Should not change after rendered once");
    }

    static Stream<Object> testSerializable() {
        @SuppressWarnings("EqualsHashCode")
        class NonSerializable {
            @Override
            public boolean equals(final Object other) {
                return other instanceof NonSerializable; // a very lenient equals()
            }
        }
        return Stream.of(
                "World",
                new NonSerializable(),
                new BigDecimal("123.456"),
                // LOG4J2-3680
                new RuntimeException(),
                null);
    }

    @ParameterizedTest
    @MethodSource
    void testSerializable(final Object arg) {
        final Message expected = new ParameterizedMessage("Hello {}!", arg);
        final Message actual = SerialUtil.deserialize(SerialUtil.serialize(expected));
        assertThat(actual).isInstanceOf(ParameterizedMessage.class);
        assertThat(actual.getFormattedMessage()).isEqualTo(expected.getFormattedMessage());
    }

    static Stream<Object[]> testCasesForInsufficientFormatArgs() {
        return Stream.of(new Object[] {1, "foo {}"}, new Object[] {2, "bar {}{}"});
    }

    @ParameterizedTest
    @MethodSource("testCasesForInsufficientFormatArgs")
    void formatTo_should_fail_on_insufficient_args(final int placeholderCount, final String pattern) {
        final int argCount = placeholderCount - 1;
        verifyFormattingFailureOnInsufficientArgs(placeholderCount, pattern, argCount, () -> {
            final ParameterizedMessage message = new ParameterizedMessage(pattern, new Object[argCount]);
            final StringBuilder buffer = new StringBuilder();
            message.formatTo(buffer);
            return buffer.toString();
        });
    }

    @ParameterizedTest
    @MethodSource("testCasesForInsufficientFormatArgs")
    void format_should_fail_on_insufficient_args(final int placeholderCount, final String pattern) {
        final int argCount = placeholderCount - 1;
        verifyFormattingFailureOnInsufficientArgs(
                placeholderCount, pattern, argCount, () -> ParameterizedMessage.format(pattern, new Object[argCount]));
    }

    private void verifyFormattingFailureOnInsufficientArgs(
            final int placeholderCount,
            final String pattern,
            final int argCount,
            final Supplier<String> formattedMessageSupplier) {

        // Verify the formatted message
        final String formattedMessage = formattedMessageSupplier.get();
        assertThat(formattedMessage).isEqualTo(pattern);

        // Verify the logged failure
        final List<StatusData> statusDataList = statusListener.getStatusData().collect(Collectors.toList());
        assertThat(statusDataList).hasSize(1);
        final StatusData statusData = statusDataList.get(0);
        assertThat(statusData.getLevel()).isEqualTo(Level.ERROR);
        assertThat(statusData.getMessage().getFormattedMessage()).isEqualTo("Unable to format msg: %s", pattern);
        assertThat(statusData.getThrowable())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "found %d argument placeholders, but provided %d for pattern `%s`",
                        placeholderCount, argCount, pattern);
    }
}
