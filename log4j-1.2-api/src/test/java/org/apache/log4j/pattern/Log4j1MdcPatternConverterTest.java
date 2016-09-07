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
package org.apache.log4j.pattern;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class Log4j1MdcPatternConverterTest {

    @Test
    public void testConverter0() {
        final Map<String, String> contextMap = new HashMap<>(0);
        final String expected = "{}";
        test(contextMap, expected, null);
    }

    @Test
    public void testConverter1() {
        final Map<String, String> contextMap = new HashMap<>(1);
        contextMap.put("key1", "value1");
        final String expected = "{{key1,value1}}";
        test(contextMap, expected, null);
    }

    @Test
    public void testConverter2() {
        final Map<String, String> contextMap = new HashMap<>(2);
        contextMap.put("key1", "value1");
        contextMap.put("key2", "value2");
        final String expected = "{{key1,value1}{key2,value2}}";
        test(contextMap, expected, null);
    }

    @Test
    public void testConverterWithKey() {
        final Map<String, String> contextMap = new HashMap<>(2);
        contextMap.put("key1", "value1");
        contextMap.put("key2", "value2");
        final String expected = "value1";
        test(contextMap, expected, new String[] {"key1"});
    }

    private void test(final Map<String, String> contextMap, final String expected, final String[] options) {
        final LogEvent event = Log4jLogEvent.newBuilder()
                .setLoggerName("MyLogger")
                .setLevel(Level.DEBUG)
                .setMessage(new SimpleMessage("Hello"))
                .setContextMap(contextMap)
                .build();
        final StringBuilder sb = new StringBuilder();
        final Log4j1MdcPatternConverter converter = Log4j1MdcPatternConverter.newInstance(options);
        converter.format(event, sb);
        assertEquals(expected, sb.toString());
    }

}

