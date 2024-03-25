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

    /**
     * In this test cases, constructed the following scenarios: <br>
     * <p>
     * 1. The arguments contains an exception, and the count of placeholder is equal to arguments include exception. <br>
     * 2. The arguments contains an exception, and the count of placeholder is equal to arguments except exception.<br>
     * All of these should not logged in status logger.
     * </p>
     *
     * @return Streams
     */
    static Stream<Object[]> testCasesWithExceptionArgsButNoWarn() {
        return Stream.of(
                new Object[] {
                    "with exception {} {}",
                    new Object[] {"a", new RuntimeException()},
                    "with exception a java.lang.RuntimeException"
                },
                new Object[] {
                    "with exception {} {}", new Object[] {"a", "b", new RuntimeException()}, "with exception a b"
                });
    }

    @ParameterizedTest
    @MethodSource("testCasesWithExceptionArgsButNoWarn")
    void formatToWithExceptionButNoWarn(final String pattern, final Object[] args, final String expected) {
        final ParameterizedMessage message = new ParameterizedMessage(pattern, args);
        final StringBuilder buffer = new StringBuilder();
        message.formatTo(buffer);
        assertThat(buffer.toString()).isEqualTo(expected);
        final List<StatusData> statusDataList = statusListener.getStatusData().collect(Collectors.toList());
        assertThat(statusDataList).hasSize(0);
    }

    @ParameterizedTest
    @MethodSource("testCasesWithExceptionArgsButNoWarn")
    void formatWithExceptionButNoWarn(final String pattern, final Object[] args, final String expected) {
        final String message = ParameterizedMessage.format(pattern, args);
        assertThat(message).isEqualTo(expected);
        final List<StatusData> statusDataList = statusListener.getStatusData().collect(Collectors.toList());
        assertThat(statusDataList).hasSize(0);
    }

    /**
     * In this test cases, constructed the following scenarios: <br>
     * <p>
     * 1. The placeholders are greater than the count of arguments. <br>
     * 2. The placeholders are less than the count of arguments. <br>
     * 3. The arguments contains an exception, and the placeholder is greater than normal arguments. <br>
     * 4. The arguments contains an exception, and the placeholder is less than the arguments.<br>
     * All of these should logged in status logger with WARN level.
     * </p>
     *
     * @return streams
     */
    static Stream<Object[]> testCasesForInsufficientFormatArgs() {
        return Stream.of(
                new Object[] {"more {} {}", 2, new Object[] {"a"}, "more a {}"},
                new Object[] {"more {} {} {}", 3, new Object[] {"a"}, "more a {} {}"},
                new Object[] {"less {}", 1, new Object[] {"a", "b"}, "less a"},
                new Object[] {"less {} {}", 2, new Object[] {"a", "b", "c"}, "less a b"},
                new Object[] {
                    "more throwable {} {} {}",
                    3,
                    new Object[] {"a", new RuntimeException()},
                    "more throwable a java.lang.RuntimeException {}"
                },
                new Object[] {
                    "less throwable {}", 1, new Object[] {"a", "b", new RuntimeException()}, "less throwable a"
                });
    }

    @ParameterizedTest
    @MethodSource("testCasesForInsufficientFormatArgs")
    void formatToShouldWarnOnInsufficientArgs(
            final String pattern, final int placeholderCount, final Object[] args, final String expected) {
        final int argCount = args == null ? 0 : args.length;
        verifyFormattingFailureOnInsufficientArgs(pattern, placeholderCount, argCount, expected, () -> {
            final ParameterizedMessage message = new ParameterizedMessage(pattern, args);
            final StringBuilder buffer = new StringBuilder();
            message.formatTo(buffer);
            return buffer.toString();
        });
    }

    @ParameterizedTest
    @MethodSource("testCasesForInsufficientFormatArgs")
    void formatShouldWarnOnInsufficientArgs(
            final String pattern, final int placeholderCount, final Object[] args, final String expected) {
        final int argCount = args == null ? 0 : args.length;
        verifyFormattingFailureOnInsufficientArgs(
                pattern, placeholderCount, argCount, expected, () -> ParameterizedMessage.format(pattern, args));
    }

    private void verifyFormattingFailureOnInsufficientArgs(
            final String pattern,
            final int placeholderCount,
            final int argCount,
            final String expected,
            final Supplier<String> formattedMessageSupplier) {

        // Verify the formatted message
        final String formattedMessage = formattedMessageSupplier.get();
        assertThat(formattedMessage).isEqualTo(expected);

        // Verify the status logger warn
        final List<StatusData> statusDataList = statusListener.getStatusData().collect(Collectors.toList());
        assertThat(statusDataList).hasSize(1);
        final StatusData statusData = statusDataList.get(0);
        assertThat(statusData.getLevel()).isEqualTo(Level.WARN);
        assertThat(statusData.getMessage().getFormattedMessage())
                .isEqualTo(
                        "found %d argument placeholders, but provided %d for pattern `%s`",
                        placeholderCount, argCount, pattern);
        assertThat(statusData.getThrowable()).isNull();
    }
}
