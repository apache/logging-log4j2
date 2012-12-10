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
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class LevelPatternConverterTest {


    @Test
    public void testLevel() {
        final Message msg = new SimpleMessage("Hello");
        LogEvent event = new Log4jLogEvent("MyLogger", null, null, Level.DEBUG, msg, null);
        final StringBuilder sb = new StringBuilder();
        LevelPatternConverter converter = LevelPatternConverter.newInstance(null);
        converter.format(event, sb);
        assertEquals(Level.DEBUG.toString(), sb.toString());
        final String[] opts = new String[] {"WARN=Warning, DEBUG=Debug, ERROR=Error, TRACE=Trace, INFO=Info"};
        converter = LevelPatternConverter.newInstance(opts);
        sb.setLength(0);
        converter.format(event, sb);
        assertEquals("Debug", sb.toString());
        event = new Log4jLogEvent("MyLogger", null, null, Level.WARN, msg, null);
        sb.setLength(0);
        converter.format(event, sb);
        assertEquals("Warning", sb.toString());
    }
}
