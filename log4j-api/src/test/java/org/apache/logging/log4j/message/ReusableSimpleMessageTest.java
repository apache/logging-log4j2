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

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests ReusableSimpleMessage.
 */
public class ReusableSimpleMessageTest {

    @Test
    public void testSet_InitializesFormattedMessage() throws Exception {
        final ReusableSimpleMessage msg = new ReusableSimpleMessage();
        msg.set("abc");
        assertEquals("abc", msg.getFormattedMessage());
    }

    @Test
    public void testGetFormattedMessage_InitiallyStringNull() throws Exception {
        assertEquals("null", new ReusableSimpleMessage().getFormattedMessage());
    }

    @Test
    public void testGetFormattedMessage_ReturnsLatestSetString() throws Exception {
        final ReusableSimpleMessage msg = new ReusableSimpleMessage();
        msg.set("abc");
        assertEquals("abc", msg.getFormattedMessage());
        msg.set("def");
        assertEquals("def", msg.getFormattedMessage());
        msg.set("xyz");
        assertEquals("xyz", msg.getFormattedMessage());
    }

    @Test
    public void testGetFormat_InitiallyStringNull() throws Exception {
        assertEquals("null", new ReusableSimpleMessage().getFormat());
    }

    @Test
    public void testGetFormat_ReturnsLatestSetString() throws Exception {
        final ReusableSimpleMessage msg = new ReusableSimpleMessage();
        msg.set("abc");
        assertEquals("abc", msg.getFormat());
        msg.set("def");
        assertEquals("def", msg.getFormat());
        msg.set("xyz");
        assertEquals("xyz", msg.getFormat());
    }

    @Test
    public void testGetParameters_InitiallyReturnsEmptyArray() throws Exception {
        assertArrayEquals(new Object[0], new ReusableSimpleMessage().getParameters());
    }

    @Test
    public void testGetParameters_ReturnsEmptyArrayAfterMessageSet() throws Exception {
        final ReusableSimpleMessage msg = new ReusableSimpleMessage();
        msg.set("abc");
        assertArrayEquals(new Object[0], msg.getParameters());
        msg.set("def");
        assertArrayEquals(new Object[0], msg.getParameters());
    }

    @Test
    public void testGetThrowable_InitiallyReturnsNull() throws Exception {
        assertNull(new ReusableSimpleMessage().getThrowable());
    }

    @Test
    public void testGetThrowable_ReturnsNullAfterMessageSet() throws Exception {
        final ReusableSimpleMessage msg = new ReusableSimpleMessage();
        msg.set("abc");
        assertNull(msg.getThrowable());
        msg.set("def");
        assertNull(msg.getThrowable());
    }

    @Test
    public void testFormatTo_InitiallyWritesNull() throws Exception {
        final ReusableSimpleMessage msg = new ReusableSimpleMessage();
        final StringBuilder sb = new StringBuilder();
        msg.formatTo(sb);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testFormatTo_WritesLatestSetString() throws Exception {
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
}