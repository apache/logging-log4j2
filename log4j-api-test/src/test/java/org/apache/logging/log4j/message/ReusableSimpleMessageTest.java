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
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.stream.Stream;
import org.apache.logging.log4j.test.junit.SerialUtil;
import org.apache.logging.log4j.util.Constants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests ReusableSimpleMessage.
 */
class ReusableSimpleMessageTest {

    @Test
    void testSet_InitializesFormattedMessage() {
        final ReusableSimpleMessage msg = new ReusableSimpleMessage();
        msg.set("abc");
        assertEquals("abc", msg.getFormattedMessage());
    }

    @Test
    void testGetFormattedMessage_InitiallyStringNull() {
        assertEquals("null", new ReusableSimpleMessage().getFormattedMessage());
    }

    @Test
    void testGetFormattedMessage_ReturnsLatestSetString() {
        final ReusableSimpleMessage msg = new ReusableSimpleMessage();
        msg.set("abc");
        assertEquals("abc", msg.getFormattedMessage());
        msg.set("def");
        assertEquals("def", msg.getFormattedMessage());
        msg.set("xyz");
        assertEquals("xyz", msg.getFormattedMessage());
    }

    @Test
    void testGetFormat_InitiallyStringNull() {
        assertNull(new ReusableSimpleMessage().getFormat());
    }

    @Test
    void testGetFormat_ReturnsLatestSetString() {
        final ReusableSimpleMessage msg = new ReusableSimpleMessage();
        msg.set("abc");
        assertEquals("abc", msg.getFormat());
        msg.set("def");
        assertEquals("def", msg.getFormat());
        msg.set("xyz");
        assertEquals("xyz", msg.getFormat());
    }

    @Test
    void testGetParameters_InitiallyReturnsEmptyArray() {
        assertArrayEquals(Constants.EMPTY_OBJECT_ARRAY, new ReusableSimpleMessage().getParameters());
    }

    @Test
    void testGetParameters_ReturnsEmptyArrayAfterMessageSet() {
        final ReusableSimpleMessage msg = new ReusableSimpleMessage();
        msg.set("abc");
        assertArrayEquals(Constants.EMPTY_OBJECT_ARRAY, msg.getParameters());
        msg.set("def");
        assertArrayEquals(Constants.EMPTY_OBJECT_ARRAY, msg.getParameters());
    }

    @Test
    void testGetThrowable_InitiallyReturnsNull() {
        assertNull(new ReusableSimpleMessage().getThrowable());
    }

    @Test
    void testGetThrowable_ReturnsNullAfterMessageSet() {
        final ReusableSimpleMessage msg = new ReusableSimpleMessage();
        msg.set("abc");
        assertNull(msg.getThrowable());
        msg.set("def");
        assertNull(msg.getThrowable());
    }

    @Test
    void testFormatTo_InitiallyWritesNull() {
        final ReusableSimpleMessage msg = new ReusableSimpleMessage();
        final StringBuilder sb = new StringBuilder();
        msg.formatTo(sb);
        assertEquals("null", sb.toString());
    }

    @Test
    void testFormatTo_WritesLatestSetString() {
        final ReusableSimpleMessage msg = new ReusableSimpleMessage();
        final StringBuilder sb = new StringBuilder();
        msg.formatTo(sb);
        assertEquals("null", sb.toString());
        sb.setLength(0);
        msg.set("abc");
        msg.formatTo(sb);
        assertEquals("abc", sb.toString());
        sb.setLength(0);
        msg.set("def");
        msg.formatTo(sb);
        assertEquals("def", sb.toString());
        sb.setLength(0);
        msg.set("xyz");
        msg.formatTo(sb);
        assertEquals("xyz", sb.toString());
    }

    static Stream<CharSequence> testSerializable() {
        return SimpleMessageTest.testSerializable();
    }

    @ParameterizedTest
    @MethodSource
    void testSerializable(final CharSequence arg) {
        final ReusableSimpleMessage expected = new ReusableSimpleMessage();
        expected.set(arg);
        final Message actual = SerialUtil.deserialize(SerialUtil.serialize(expected));
        assertThat(actual).isInstanceOf(SimpleMessage.class);
        assertThat(actual.getFormattedMessage()).isEqualTo(expected.getFormattedMessage());
    }
}
