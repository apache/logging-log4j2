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

import static org.junit.Assert.assertEquals;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.ContextDataFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.StringMap;
import org.junit.Test;

public class Log4j1MdcPatternConverterTest {

    @Test
    public void testConverter0() {
        final StringMap contextMap = ContextDataFactory.createContextData(0);
        final String expected = "{}";
        test(contextMap, expected, null);
    }

    @Test
    public void testConverter1() {
        final StringMap contextMap = ContextDataFactory.createContextData(1);
        contextMap.putValue("key1", "value1");
        final String expected = "{{key1,value1}}";
        test(contextMap, expected, null);
    }

    @Test
    public void testConverter2() {
        final StringMap contextMap = ContextDataFactory.createContextData(2);
        contextMap.putValue("key1", "value1");
        contextMap.putValue("key2", "value2");
        final String expected = "{{key1,value1}{key2,value2}}";
        test(contextMap, expected, null);
    }

    @Test
    public void testConverterWithKey() {
        final StringMap contextMap = ContextDataFactory.createContextData(2);
        contextMap.putValue("key1", "value1");
        contextMap.putValue("key2", "value2");
        final String expected = "value1";
        test(contextMap, expected, new String[] {"key1"});
    }

    private void test(final StringMap contextMap, final String expected, final String[] options) {
        final LogEvent event = Log4jLogEvent.newBuilder()
                .setLoggerName("MyLogger")
                .setLevel(Level.DEBUG)
                .setMessage(new SimpleMessage("Hello"))
                .setContextData(contextMap)
                .build();
        final StringBuilder sb = new StringBuilder();
        final Log4j1MdcPatternConverter converter = Log4j1MdcPatternConverter.newInstance(options);
        converter.format(event, sb);
        assertEquals(expected, sb.toString());
    }

}

