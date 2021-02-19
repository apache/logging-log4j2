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

import org.junit.jupiter.api.Test;

/**
 * Tests ReusableObjectMessage.
 */
public class ReusableObjectMessageTest {

    @Test
    public void testSet_InitializesFormattedMessage() throws Exception {
        final ReusableObjectMessage msg = new ReusableObjectMessage();
        msg.set("abc");
        assertThat(msg.getFormattedMessage()).isEqualTo("abc");
    }

    @Test
    public void testGetFormattedMessage_InitiallyNullString() throws Exception {
        assertThat(new ReusableObjectMessage().getFormattedMessage()).isEqualTo("null");
    }

    @Test
    public void testGetFormattedMessage_ReturnsLatestSetString() throws Exception {
        final ReusableObjectMessage msg = new ReusableObjectMessage();
        msg.set("abc");
        assertThat(msg.getFormattedMessage()).isEqualTo("abc");
        msg.set("def");
        assertThat(msg.getFormattedMessage()).isEqualTo("def");
        msg.set("xyz");
        assertThat(msg.getFormattedMessage()).isEqualTo("xyz");
    }

    @Test
    public void testGetFormat_InitiallyNull() throws Exception {
        assertThat(new ReusableObjectMessage().getFormat()).isNull();
    }

    @Test
    public void testGetFormat_ReturnsLatestSetString() throws Exception {
        final ReusableObjectMessage msg = new ReusableObjectMessage();
        msg.set("abc");
        assertThat(msg.getFormat()).isEqualTo("abc");
        msg.set("def");
        assertThat(msg.getFormat()).isEqualTo("def");
        msg.set("xyz");
        assertThat(msg.getFormat()).isEqualTo("xyz");
    }

    @Test
    public void testGetParameters_InitiallyReturnsNullObjectInLength1Array() throws Exception {
        assertThat(new ReusableObjectMessage().getParameters()).isEqualTo(new Object[]{null});
    }

    @Test
    public void testGetParameters_ReturnsSetObjectInParameterArrayAfterMessageSet() throws Exception {
        final ReusableObjectMessage msg = new ReusableObjectMessage();
        msg.set("abc");
        assertThat(msg.getParameters()).isEqualTo(new Object[]{"abc"});
        msg.set("def");
        assertThat(msg.getParameters()).isEqualTo(new Object[]{"def"});
    }

    @Test
    public void testGetThrowable_InitiallyReturnsNull() throws Exception {
        assertThat(new ReusableObjectMessage().getThrowable()).isNull();
    }

    @Test
    public void testGetThrowable_ReturnsNullAfterMessageSet() throws Exception {
        final ReusableObjectMessage msg = new ReusableObjectMessage();
        msg.set("abc");
        assertThat(msg.getThrowable()).isNull();
        msg.set("def");
        assertThat(msg.getThrowable()).isNull();
    }

    @Test
    public void testFormatTo_InitiallyWritesNull() throws Exception {
        final ReusableObjectMessage msg = new ReusableObjectMessage();
        final StringBuilder sb = new StringBuilder();
        msg.formatTo(sb);
        assertThat(sb.toString()).isEqualTo("null");
    }

    @Test
    public void testFormatTo_WritesLatestSetString() throws Exception {
        final ReusableObjectMessage msg = new ReusableObjectMessage();
        final StringBuilder sb = new StringBuilder();
        msg.formatTo(sb);
        assertThat(sb.toString()).isEqualTo("null");
        sb.setLength(0);
        msg.set("abc");
        msg.formatTo(sb);
        assertThat(sb.toString()).isEqualTo("abc");
        sb.setLength(0);
        msg.set("def");
        msg.formatTo(sb);
        assertThat(sb.toString()).isEqualTo("def");
        sb.setLength(0);
        msg.set("xyz");
        msg.formatTo(sb);
        assertThat(sb.toString()).isEqualTo("xyz");
    }
}
