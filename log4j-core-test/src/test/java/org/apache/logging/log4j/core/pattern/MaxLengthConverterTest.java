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
package org.apache.logging.log4j.core.pattern;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.Test;

public class MaxLengthConverterTest {

    private static final MaxLengthConverter converter = MaxLengthConverter.newInstance(null, new String[] {"%m", "10"});

    @Test
    public void testUnderMaxLength() {
        final Message message = new SimpleMessage("0123456789");
        final LogEvent event = Log4jLogEvent.newBuilder()
                .setLoggerName("MyLogger")
                .setLevel(Level.DEBUG)
                .setMessage(message)
                .build();
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        assertEquals("0123456789", sb.toString());
    }

    @Test
    public void testOverMaxLength() {
        final Message message = new SimpleMessage("01234567890123456789");
        final LogEvent event = Log4jLogEvent.newBuilder()
                .setLoggerName("MyLogger")
                .setLevel(Level.DEBUG)
                .setMessage(message)
                .build();
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        assertEquals("0123456789", sb.toString());
    }

    @Test
    public void testOverMaxLength21WithEllipsis() {
        final Message message = new SimpleMessage("012345678901234567890123456789");
        final LogEvent event = Log4jLogEvent.newBuilder()
                .setLoggerName("MyLogger")
                .setLevel(Level.DEBUG)
                .setMessage(message)
                .build();
        final StringBuilder sb = new StringBuilder();
        MaxLengthConverter.newInstance(null, new String[] {"%m", "21"}).format(event, sb);
        assertEquals("012345678901234567890...", sb.toString());
    }
}
