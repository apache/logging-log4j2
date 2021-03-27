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
package org.apache.logging.log4j.core.impl;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageContentFormatter;
import org.apache.logging.log4j.message.ReusableMessageFactory;
import org.apache.logging.log4j.message.ReusableObjectMessage;
import org.apache.logging.log4j.util.StringBuilders;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests the MutableLogEvent class when background message formatting is enabled.
 */
public class BackgroundFormattingMutableLogEventTest {

    @BeforeClass
    public static void beforeClass() {
        System.setProperty("log4j.format.msg.async", "true");
    }

    @AfterClass
    public static void afterClass() {
        System.clearProperty("log4j.format.msg.async");
    }

    private class TestReusableMessage extends ReusableObjectMessage {
        @Override
        public MessageContentFormatter getMessageContentFormatter() {
            return (formatString, parameters, parameterCount, buffer) -> StringBuilders.appendValue(buffer, "foo");
        }
    }

    @Test
    public void testWithMessageContentFormatter() {
        TestReusableMessage message = new TestReusableMessage();
        final Log4jLogEvent source = Log4jLogEvent.newBuilder()
                .setEndOfBatch(true) //
                .setIncludeLocation(true) //
                .setLevel(Level.FATAL) //
                .setLoggerFqcn("a.b.c.d.e") //
                .setLoggerName("my name is Logger") //
                .setMarker(MarkerManager.getMarker("on your marks")) //
                .setMessage(message) //
                .setNanoTime(1234567) //
                .setSource(new StackTraceElement("myclass", "mymethod", "myfile", 123)) //
                .setThreadId(100).setThreadName("threadname").setThreadPriority(10) //
                .setThrown(new RuntimeException("run")) //
                .setTimeMillis(987654321)
                .build();
        final MutableLogEvent mutable = new MutableLogEvent();
        mutable.initFrom(source);
        // does not format message in setMessage, so this should be null
        assertNull(mutable.getFormat());
        assertEquals("foo", mutable.getFormattedMessage());
    }

    @Test
    public void testWithoutMessageContentFormatter() {
        Message message = ReusableMessageFactory.INSTANCE.newMessage("test");
        final Log4jLogEvent source = Log4jLogEvent.newBuilder()
                .setEndOfBatch(true) //
                .setIncludeLocation(true) //
                .setLevel(Level.FATAL) //
                .setLoggerFqcn("a.b.c.d.e") //
                .setLoggerName("my name is Logger") //
                .setMarker(MarkerManager.getMarker("on your marks")) //
                .setMessage(message) //
                .setNanoTime(1234567) //
                .setSource(new StackTraceElement("myclass", "mymethod", "myfile", 123)) //
                .setThreadId(100).setThreadName("threadname").setThreadPriority(10) //
                .setThrown(new RuntimeException("run")) //
                .setTimeMillis(987654321)
                .build();
        final MutableLogEvent mutable = new MutableLogEvent();
        mutable.initFrom(source);
        assertEquals("test", mutable.getFormat());
        assertEquals("test", mutable.getFormattedMessage());
    }
}
