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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.Logger;

import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class PatternParserTest {

    static String OUTPUT_FILE   = "output/PatternParser";
    static String WITNESS_FILE  = "witness/PatternParser";
    LoggerContext ctx = (LoggerContext) LogManager.getContext();
    Logger root = ctx.getLogger("");

    private static String msgPattern = "%m%n";
    private String mdcMsgPattern1 = "%m : %X%n";
    private String mdcMsgPattern2 = "%m : %X{key1}%n";
    private String mdcMsgPattern3 = "%m : %X{key2}%n";
    private String mdcMsgPattern4 = "%m : %X{key3}%n";
    private String mdcMsgPattern5 = "%m : %X{key1},%X{key2},%X{key3}%n";

    private static String customPattern = "[%d{yyyyMMdd HH:mm:ss,SSS}] %-5p [%-25.25c{1}:%-4L] - %m%n";
    private static String nestedPattern =
        "%highlight{%d{dd MMM yyyy HH:mm:ss,SSS}{GMT+0} [%t] %-5level: %msg%n%throwable}";

    private static final String LINE_SEP = System.getProperty("line.separator");

    private static final String KEY = "Converter";
    private PatternParser parser;

    @Before
    public void setup() {
        parser = new PatternParser(KEY);
    }

    private void validateConverter(List<PatternFormatter> formatter, int index, String name) {
        PatternConverter pc = formatter.get(index).getConverter();
        assertEquals("Incorrect converter " + pc.getName() + " at index " + index + " expected " + name,
            pc.getName(), name);
    }

    /**
     * Test the default pattern
     */
    @Test
    public void defaultPattern() {
        List<PatternFormatter> formatters = parser.parse(msgPattern);
        assertNotNull(formatters);
        assertTrue(formatters.size() == 2);
        validateConverter(formatters, 0, "Message");
        validateConverter(formatters, 1, "Line Sep");
    }

    /**
     * Test the custome pattern
     */
    @Test
    public void testCustomPattern() {
        List<PatternFormatter> formatters = parser.parse(customPattern);
        assertNotNull(formatters);
        Map<String, String> mdc = new HashMap<String, String>();
        mdc.put("loginId", "Fred");
        Throwable t = new Throwable();
        StackTraceElement[] elements = t.getStackTrace();
        LogEvent event = new Log4jLogEvent("org.apache.logging.log4j.PatternParserTest", MarkerManager.getMarker("TEST"),
            Logger.class.getName(), Level.INFO, new SimpleMessage("Hello, world"), null,
            mdc, null, "Thread1", elements[0], System.currentTimeMillis());
        StringBuilder buf = new StringBuilder();
        for (PatternFormatter formatter : formatters) {
            formatter.format(event, buf);
        }
        String str = buf.toString();
        String expected = "INFO  [PatternParserTest        :97  ] - Hello, world" + LINE_SEP;
        assertTrue("Expected to end with: " + expected + ". Actual: " + str, str.endsWith(expected));
    }

    @Test
    public void testNestedPattern() {
        List<PatternFormatter> formatters = parser.parse(nestedPattern);
        assertNotNull(formatters);
        Throwable t = new Throwable();
        StackTraceElement[] elements = t.getStackTrace();
        LogEvent event = new Log4jLogEvent("org.apache.logging.log4j.PatternParserTest", MarkerManager.getMarker("TEST"),
            Logger.class.getName(), Level.INFO, new SimpleMessage("Hello, world"), null,
            null, null, "Thread1", elements[0], System.currentTimeMillis());
        StringBuilder buf = new StringBuilder();
        for (PatternFormatter formatter : formatters) {
            formatter.format(event, buf);
        }
        String str = buf.toString();
        String expected = "] INFO : Hello, world\n\u001B[m";
        assertTrue(" Expected to end with: " + expected + ". Actual: " + str, str.endsWith(expected));
    }

}
