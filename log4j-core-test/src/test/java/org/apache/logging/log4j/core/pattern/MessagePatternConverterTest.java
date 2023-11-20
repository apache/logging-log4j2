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
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.builder.impl.DefaultConfigurationBuilder;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.message.StringMapMessage;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.junit.jupiter.api.Test;

public class MessagePatternConverterTest {

    @Test
    public void testPattern() {
        final MessagePatternConverter converter = MessagePatternConverter.newInstance(null, null);
        Message msg = new SimpleMessage("Hello!");
        LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("MyLogger") //
                .setLevel(Level.DEBUG) //
                .setMessage(msg)
                .build();
        StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        assertEquals("Hello!", sb.toString(), "Unexpected result");
        event = Log4jLogEvent.newBuilder() //
                .setLoggerName("MyLogger") //
                .setLevel(Level.DEBUG) //
                .setMessage(null)
                .build();
        sb = new StringBuilder();
        converter.format(event, sb);
        assertEquals(0, sb.length(), "Incorrect length: " + sb);
        msg = new SimpleMessage(null);
        event = Log4jLogEvent.newBuilder() //
                .setLoggerName("MyLogger") //
                .setLevel(Level.DEBUG) //
                .setMessage(msg)
                .build();
        sb = new StringBuilder();
        converter.format(event, sb);
        assertEquals(4, sb.length(), "Incorrect length: " + sb);
    }

    @Test
    public void testPatternAndParameterizedMessageDateLookup() {
        final MessagePatternConverter converter = MessagePatternConverter.newInstance(null, null);
        final Message msg = new ParameterizedMessage("${date:now:buhu}");
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("MyLogger") //
                .setLevel(Level.DEBUG) //
                .setMessage(msg)
                .build();
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        assertEquals("${date:now:buhu}", sb.toString(), "Unexpected result");
    }

    @Test
    public void testDefaultDisabledLookup() {
        final Configuration config =
                new DefaultConfigurationBuilder().addProperty("foo", "bar").build(true);
        final MessagePatternConverter converter = MessagePatternConverter.newInstance(config, null);
        final Message msg = new ParameterizedMessage("${foo}");
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("MyLogger") //
                .setLevel(Level.DEBUG) //
                .setMessage(msg)
                .build();
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        assertEquals("${foo}", sb.toString(), "Unexpected result");
    }

    @Test
    public void testDisabledLookup() {
        final Configuration config =
                new DefaultConfigurationBuilder().addProperty("foo", "bar").build(true);
        final MessagePatternConverter converter =
                MessagePatternConverter.newInstance(config, new String[] {"nolookups"});
        final Message msg = new ParameterizedMessage("${foo}");
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("MyLogger") //
                .setLevel(Level.DEBUG) //
                .setMessage(msg)
                .build();
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        assertEquals("${foo}", sb.toString(), "Expected the raw pattern string without lookup");
    }

    @Test
    public void testLookup() {
        final Configuration config =
                new DefaultConfigurationBuilder().addProperty("foo", "bar").build(true);
        final MessagePatternConverter converter = MessagePatternConverter.newInstance(config, new String[] {"lookups"});
        final Message msg = new ParameterizedMessage("${foo}");
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("MyLogger") //
                .setLevel(Level.DEBUG) //
                .setMessage(msg)
                .build();
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        assertEquals("${foo}", sb.toString(), "Unexpected result");
    }

    @Test
    public void testPatternWithConfiguration() {
        final Configuration config = new DefaultConfiguration();
        final MessagePatternConverter converter = MessagePatternConverter.newInstance(config, null);
        Message msg = new SimpleMessage("Hello!");
        LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("MyLogger") //
                .setLevel(Level.DEBUG) //
                .setMessage(msg)
                .build();
        StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        assertEquals("Hello!", sb.toString(), "Unexpected result");
        event = Log4jLogEvent.newBuilder() //
                .setLoggerName("MyLogger") //
                .setLevel(Level.DEBUG) //
                .setMessage(null)
                .build();
        sb = new StringBuilder();
        converter.format(event, sb);
        assertEquals(0, sb.length(), "Incorrect length: " + sb);
        msg = new SimpleMessage(null);
        event = Log4jLogEvent.newBuilder() //
                .setLoggerName("MyLogger") //
                .setLevel(Level.DEBUG) //
                .setMessage(msg)
                .build();
        sb = new StringBuilder();
        converter.format(event, sb);
        assertEquals(4, sb.length(), "Incorrect length: " + sb);
    }

    @Test
    public void testMapMessageFormatJson() {
        final MessagePatternConverter converter = MessagePatternConverter.newInstance(null, new String[] {"json"});
        final Message msg = new StringMapMessage().with("key", "val");
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("MyLogger") //
                .setLevel(Level.DEBUG) //
                .setMessage(msg)
                .build();
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        assertEquals("{\"key\":\"val\"}", sb.toString(), "Unexpected result");
    }

    @Test
    public void testMapMessageFormatXml() {
        final MessagePatternConverter converter = MessagePatternConverter.newInstance(null, new String[] {"xml"});
        final Message msg = new StringMapMessage().with("key", "val");
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("MyLogger") //
                .setLevel(Level.DEBUG) //
                .setMessage(msg)
                .build();
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        assertEquals("<Map>\n  <Entry key=\"key\">val</Entry>\n</Map>", sb.toString(), "Unexpected result");
    }

    @Test
    public void testMapMessageFormatDefault() {
        final MessagePatternConverter converter = MessagePatternConverter.newInstance(null, null);
        final Message msg = new StringMapMessage().with("key", "val");
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("MyLogger") //
                .setLevel(Level.DEBUG) //
                .setMessage(msg)
                .build();
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        assertEquals("key=\"val\"", sb.toString(), "Unexpected result");
    }

    @Test
    public void testStructuredDataFormatFull() {
        final MessagePatternConverter converter = MessagePatternConverter.newInstance(null, new String[] {"FULL"});
        final Message msg = new StructuredDataMessage("id", "message", "type").with("key", "val");
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("MyLogger") //
                .setLevel(Level.DEBUG) //
                .setMessage(msg)
                .build();
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        assertEquals("type [id key=\"val\"] message", sb.toString(), "Unexpected result");
    }
}
