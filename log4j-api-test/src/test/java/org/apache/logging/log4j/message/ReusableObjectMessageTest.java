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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests ReusableObjectMessage.
 */
class ReusableObjectMessageTest {

    @Test
    void testSet_InitializesFormattedMessage() {
        final ReusableObjectMessage msg = new ReusableObjectMessage();
        msg.set("abc");
        assertEquals("abc", msg.getFormattedMessage());
    }

    @Test
    void testGetFormattedMessage_InitiallyNullString() {
        assertEquals("null", new ReusableObjectMessage().getFormattedMessage());
    }

    @Test
    void testGetFormattedMessage_ReturnsLatestSetString() {
        final ReusableObjectMessage msg = new ReusableObjectMessage();
        msg.set("abc");
        assertEquals("abc", msg.getFormattedMessage());
        msg.set("def");
        assertEquals("def", msg.getFormattedMessage());
        msg.set("xyz");
        assertEquals("xyz", msg.getFormattedMessage());
    }

    @Test
    void testGetFormat_InitiallyNull() {
        assertNull(new ReusableObjectMessage().getFormat());
    }

    @Test
    void testGetFormat_ReturnsLatestSetString() {
        final ReusableObjectMessage msg = new ReusableObjectMessage();
        msg.set("abc");
        assertEquals("abc", msg.getFormat());
        msg.set("def");
        assertEquals("def", msg.getFormat());
        msg.set("xyz");
        assertEquals("xyz", msg.getFormat());
    }

    @Test
    void testGetParameters_InitiallyReturnsNullObjectInLength1Array() {
        assertArrayEquals(new Object[] {null}, new ReusableObjectMessage().getParameters());
    }

    @Test
    void testGetParameters_ReturnsSetObjectInParameterArrayAfterMessageSet() {
        final ReusableObjectMessage msg = new ReusableObjectMessage();
        msg.set("abc");
        assertArrayEquals(new Object[] {"abc"}, msg.getParameters());
        msg.set("def");
        assertArrayEquals(new Object[] {"def"}, msg.getParameters());
    }

    @Test
    void testGetThrowable_InitiallyReturnsNull() {
        assertNull(new ReusableObjectMessage().getThrowable());
    }

    @Test
    void testGetThrowable_ReturnsNullAfterMessageSet() {
        final ReusableObjectMessage msg = new ReusableObjectMessage();
        msg.set("abc");
        assertNull(msg.getThrowable());
        msg.set("def");
        assertNull(msg.getThrowable());
    }

    @Test
    void testFormatTo_InitiallyWritesNull() {
        final ReusableObjectMessage msg = new ReusableObjectMessage();
        final StringBuilder sb = new StringBuilder();
        msg.formatTo(sb);
        assertEquals("null", sb.toString());
    }

    @Test
    void testFormatTo_WritesLatestSetString() {
        final ReusableObjectMessage msg = new ReusableObjectMessage();
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

    static Stream<Object> testSerializable() {
        return ObjectMessageTest.testSerializable();
    }

    @ParameterizedTest
    @MethodSource
    void testSerializable(final Object arg) {
        final ReusableObjectMessage expected = new ReusableObjectMessage();
        expected.set(arg);
        final Message actual = SerialUtil.deserialize(SerialUtil.serialize(expected));
        assertThat(actual).isInstanceOf(ObjectMessage.class);
        assertThat(actual.getFormattedMessage()).isEqualTo(expected.getFormattedMessage());
    }
}
