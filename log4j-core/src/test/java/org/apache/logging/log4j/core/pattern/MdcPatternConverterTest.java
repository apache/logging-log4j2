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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class MdcPatternConverterTest {

    @Test
    public void testConverter() {

        final Message msg = new SimpleMessage("Hello");
        ThreadContext.put("subject", "I");
        ThreadContext.put("verb", "love");
        ThreadContext.put("object", "Log4j");
        final MdcPatternConverter converter = MdcPatternConverter.newInstance(null);
        final LogEvent event = new Log4jLogEvent("MyLogger", null, null, Level.DEBUG, msg, null);
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        final String str = sb.toString();
        final String expected = "{object=Log4j, subject=I, verb=love}";
        assertTrue("Incorrect result. Expected " + expected + ", actual " + str, str.equals(expected));
    }

    @Test
    public void testConverterWithKey() {

        final Message msg = new SimpleMessage("Hello");
        final String [] options = new String[] {"object"};
        ThreadContext.put("subject", "I");
        ThreadContext.put("verb", "love");
        ThreadContext.put("object", "Log4j");
        final MdcPatternConverter converter = MdcPatternConverter.newInstance(options);
        final LogEvent event = new Log4jLogEvent("MyLogger", null, null, Level.DEBUG, msg, null);
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        final String str = sb.toString();
        final String expected = "Log4j";
        assertEquals(expected, str);
    }

    @Test
    public void testConverterWithKeys() {

        final Message msg = new SimpleMessage("Hello");
        final String [] options = new String[] {"object, subject"};
        ThreadContext.put("subject", "I");
        ThreadContext.put("verb", "love");
        ThreadContext.put("object", "Log4j");
        final MdcPatternConverter converter = MdcPatternConverter.newInstance(options);
        final LogEvent event = new Log4jLogEvent("MyLogger", null, null, Level.DEBUG, msg, null);
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        final String str = sb.toString();
        final String expected = "{object=Log4j, subject=I}";
        assertEquals(expected, str);
    }
}

