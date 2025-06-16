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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.test.junit.UsingThreadContextMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@UsingThreadContextMap
class MdcPatternConverterTest {

    @BeforeEach
    void setup() {
        ThreadContext.put("subject", "I");
        ThreadContext.put("verb", "love");
        ThreadContext.put("object", "Log4j");
    }

    @Test
    void testConverter() {
        final Message msg = new SimpleMessage("Hello");
        final MdcPatternConverter converter = MdcPatternConverter.newInstance(null);
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("MyLogger") //
                .setLevel(Level.DEBUG) //
                .setMessage(msg) //
                .build();
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        final String str = sb.toString();
        final String expected = "{object=Log4j, subject=I, verb=love}";
        assertEquals(expected, str, "Incorrect result. Expected " + expected + ", actual " + str);
    }

    @Test
    void testConverterWithExistingData() {
        final Message msg = new SimpleMessage("Hello");
        final MdcPatternConverter converter = MdcPatternConverter.newInstance(null);
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("MyLogger") //
                .setLevel(Level.DEBUG) //
                .setMessage(msg) //
                .build();
        final StringBuilder sb = new StringBuilder();
        sb.append("prefix ");
        converter.format(event, sb);
        final String str = sb.toString();
        final String expected = "prefix {object=Log4j, subject=I, verb=love}";
        assertEquals(expected, str, "Incorrect result. Expected " + expected + ", actual " + str);
    }

    @Test
    void testConverterFullEmpty() {
        ThreadContext.clearMap();
        final Message msg = new SimpleMessage("Hello");
        final MdcPatternConverter converter = MdcPatternConverter.newInstance(null);
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("MyLogger") //
                .setLevel(Level.DEBUG) //
                .setMessage(msg) //
                .build();
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        final String str = sb.toString();
        final String expected = "{}";
        assertEquals(expected, str, "Incorrect result. Expected " + expected + ", actual " + str);
    }

    @Test
    void testConverterFullSingle() {
        ThreadContext.clearMap();
        ThreadContext.put("foo", "bar");
        final Message msg = new SimpleMessage("Hello");
        final MdcPatternConverter converter = MdcPatternConverter.newInstance(null);
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("MyLogger") //
                .setLevel(Level.DEBUG) //
                .setMessage(msg) //
                .build();
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        final String str = sb.toString();
        final String expected = "{foo=bar}";
        assertEquals(expected, str, "Incorrect result. Expected " + expected + ", actual " + str);
    }

    @Test
    void testConverterWithKey() {
        final Message msg = new SimpleMessage("Hello");
        final String[] options = new String[] {"object"};
        final MdcPatternConverter converter = MdcPatternConverter.newInstance(options);
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("MyLogger") //
                .setLevel(Level.DEBUG) //
                .setMessage(msg) //
                .build();
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        final String str = sb.toString();
        final String expected = "Log4j";
        assertEquals(expected, str);
    }

    @Test
    void testConverterWithKeys() {
        final Message msg = new SimpleMessage("Hello");
        final String[] options = new String[] {"object, subject"};
        final MdcPatternConverter converter = MdcPatternConverter.newInstance(options);
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("MyLogger") //
                .setLevel(Level.DEBUG) //
                .setMessage(msg) //
                .build();
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        final String str = sb.toString();
        final String expected = "{object=Log4j, subject=I}";
        assertEquals(expected, str);
    }

    @Test
    void testConverterWithKeysAndPrefix() {
        final Message msg = new SimpleMessage("Hello");
        final String[] options = new String[] {"object, subject"};
        final MdcPatternConverter converter = MdcPatternConverter.newInstance(options);
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("MyLogger") //
                .setLevel(Level.DEBUG) //
                .setMessage(msg) //
                .build();
        final StringBuilder sb = new StringBuilder();
        sb.append("prefix ");
        converter.format(event, sb);
        final String str = sb.toString();
        final String expected = "prefix {object=Log4j, subject=I}";
        assertEquals(expected, str);
    }

    @Test
    void testConverterWithKeysSinglePresent() {
        final Message msg = new SimpleMessage("Hello");
        final String[] options = new String[] {"object, notpresent"};
        final MdcPatternConverter converter = MdcPatternConverter.newInstance(options);
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("MyLogger") //
                .setLevel(Level.DEBUG) //
                .setMessage(msg) //
                .build();
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        final String str = sb.toString();
        final String expected = "{object=Log4j}";
        assertEquals(expected, str);
    }

    @Test
    void testConverterWithKeysNonePresent() {
        ThreadContext.clearMap();
        final Message msg = new SimpleMessage("Hello");
        final String[] options = new String[] {"object, subject"};
        final MdcPatternConverter converter = MdcPatternConverter.newInstance(options);
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("MyLogger") //
                .setLevel(Level.DEBUG) //
                .setMessage(msg) //
                .build();
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        final String str = sb.toString();
        final String expected = "{}";
        assertEquals(expected, str);
    }
}
