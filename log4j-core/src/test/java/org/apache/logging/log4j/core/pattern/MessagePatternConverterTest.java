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
package org.apache.logging.log4j.core.pattern;

import static org.junit.Assert.assertEquals;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.Test;

/**
 *
 */
public class MessagePatternConverterTest {

    @Test
    public void testPattern() throws Exception {
        final MessagePatternConverter converter = MessagePatternConverter.newInstance(null, null);
        Message msg = new SimpleMessage("Hello!");
        LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("MyLogger") //
                .setLevel(Level.DEBUG) //
                .setMessage(msg).build();
        StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        assertEquals("Unexpected result", "Hello!", sb.toString());
        event = Log4jLogEvent.newBuilder() //
                .setLoggerName("MyLogger") //
                .setLevel(Level.DEBUG) //
                .setMessage(null).build();
        sb = new StringBuilder();
        converter.format(event, sb);
        assertEquals("Incorrect length: " + sb, 0, sb.length());
        msg = new SimpleMessage(null);
        event = Log4jLogEvent.newBuilder() //
                .setLoggerName("MyLogger") //
                .setLevel(Level.DEBUG) //
                .setMessage(msg).build();
        sb = new StringBuilder();
        converter.format(event, sb);
        assertEquals("Incorrect length: " + sb, 4, sb.length());
    }

    @Test
    public void testPatternAndParameterizedMessageDateLookup() throws Exception {
        final MessagePatternConverter converter = MessagePatternConverter.newInstance(null, null);
        final Message msg = new ParameterizedMessage("${date:now:buhu}");
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("MyLogger") //
                .setLevel(Level.DEBUG) //
                .setMessage(msg).build();
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        assertEquals("Unexpected result", "${date:now:buhu}", sb.toString());
    }

    @Test
    public void testPatternWithConfiguration() throws Exception {
        final Configuration config = new DefaultConfiguration();
        final MessagePatternConverter converter = MessagePatternConverter.newInstance(config, null);
        Message msg = new SimpleMessage("Hello!");
        LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("MyLogger") //
                .setLevel(Level.DEBUG) //
                .setMessage(msg).build();
        StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        assertEquals("Unexpected result", "Hello!", sb.toString());
        event = Log4jLogEvent.newBuilder() //
                .setLoggerName("MyLogger") //
                .setLevel(Level.DEBUG) //
                .setMessage(null).build();
        sb = new StringBuilder();
        converter.format(event, sb);
        assertEquals("Incorrect length: " + sb, 0, sb.length());
        msg = new SimpleMessage(null);
        event = Log4jLogEvent.newBuilder() //
                .setLoggerName("MyLogger") //
                .setLevel(Level.DEBUG) //
                .setMessage(msg).build();
        sb = new StringBuilder();
        converter.format(event, sb);
        assertEquals("Incorrect length: " + sb, 4, sb.length());
    }
}
