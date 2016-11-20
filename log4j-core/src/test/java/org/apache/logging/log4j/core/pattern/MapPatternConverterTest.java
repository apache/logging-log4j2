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
import org.apache.logging.log4j.message.MapMessage;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class MapPatternConverterTest {

    @Test
    public void testConverter() {

        final MapMessage msg = new MapMessage();
        msg.put("subject", "I");
        msg.put("verb", "love");
        msg.put("object", "Log4j");
        final MapPatternConverter converter = MapPatternConverter.newInstance(null);
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("MyLogger") //
                .setLevel(Level.DEBUG) //
                .setMessage(msg) //
                .build();
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        final String str = sb.toString();
        String expected = "subject=I";
        assertTrue("Missing or incorrect subject. Expected " + expected + ", actual " + str, str.contains(expected));
        expected = "verb=love";
        assertTrue("Missing or incorrect verb", str.contains(expected));
        expected = "object=Log4j";
        assertTrue("Missing or incorrect object", str.contains(expected));

        assertEquals("{object=Log4j, subject=I, verb=love}", str);
    }

    @Test
    public void testConverterWithKey() {

        final MapMessage msg = new MapMessage();
        msg.put("subject", "I");
        msg.put("verb", "love");
        msg.put("object", "Log4j");
        final MapPatternConverter converter = MapPatternConverter.newInstance(new String[] {"object"});
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
}
