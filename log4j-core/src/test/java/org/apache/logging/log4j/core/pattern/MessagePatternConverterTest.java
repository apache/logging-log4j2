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

import static org.junit.Assert.assertTrue;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.Message;
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
        LogEvent event = new Log4jLogEvent("MyLogger", null, null, Level.DEBUG, msg, null);
        StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        assertTrue("Unexpected result", "Hello!".equals(sb.toString()));
        event = new Log4jLogEvent("MyLogger", null, null, Level.DEBUG, null, null);
        sb = new StringBuilder();
        converter.format(event, sb);
        assertTrue("Incorrect length: " + sb.length(), sb.length() == 0);
        msg = new SimpleMessage(null);
        event = new Log4jLogEvent("MyLogger", null, null, Level.DEBUG, msg, null);
        sb = new StringBuilder();
        converter.format(event, sb);
        assertTrue("Incorrect length: " + sb.length(), sb.length() == 4);

    }

    @Test
    public void testPatternWithConfiguration() throws Exception {
        final Configuration config = new DefaultConfiguration();
        final MessagePatternConverter converter = MessagePatternConverter.newInstance(config, null);
        Message msg = new SimpleMessage("Hello!");
        LogEvent event = new Log4jLogEvent("MyLogger", null, null, Level.DEBUG, msg, null);
        StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        assertTrue("Unexpected result", "Hello!".equals(sb.toString()));
        event = new Log4jLogEvent("MyLogger", null, null, Level.DEBUG, null, null);
        sb = new StringBuilder();
        converter.format(event, sb);
        assertTrue("Incorrect length: " + sb.length(), sb.length() == 0);
        msg = new SimpleMessage(null);
        event = new Log4jLogEvent("MyLogger", null, null, Level.DEBUG, msg, null);
        sb = new StringBuilder();
        converter.format(event, sb);
        assertTrue("Incorrect length: " + sb.length(), sb.length() == 4);
    }
}
