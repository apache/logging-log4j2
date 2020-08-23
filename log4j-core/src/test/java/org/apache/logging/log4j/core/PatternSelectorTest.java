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
package org.apache.logging.log4j.core;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.junit.Named;
import org.apache.logging.log4j.junit.LoggerContextSource;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@LoggerContextSource("log4j-patternSelector.xml")
public class PatternSelectorTest {

    @Test
    public void testMarkerPatternSelector(@Named("List") final ListAppender app) {
        final org.apache.logging.log4j.Logger logger = LogManager.getLogger("TestMarkerPatternSelector");
        logger.traceEntry();
        logger.info("Hello World");
        logger.traceExit();
        final List<String> messages = app.getMessages();
        assertNotNull(messages, "No Messages");
        assertEquals(3, messages.size(),
                "Incorrect number of messages. Expected 3, Actual " + messages.size() + ": " + messages);
        final String expect = String.format("[TRACE] TestMarkerPatternSelector ====== "
                + "o.a.l.l.c.PatternSelectorTest.testMarkerPatternSelector:36 Enter ======%n");
        assertEquals(expect, messages.get(0));
        assertEquals("[INFO ] TestMarkerPatternSelector Hello World" + Strings.LINE_SEPARATOR, messages.get(1));
        app.clear();
    }

    @Test
    public void testScriptPatternSelector(@Named("List2") final ListAppender app) {
        final org.apache.logging.log4j.Logger logger = LogManager.getLogger("TestScriptPatternSelector");
        final org.apache.logging.log4j.Logger logger2 = LogManager.getLogger("NoLocation");
        logger.traceEntry();
        logger.info("Hello World");
        logger2.info("No location information");
        logger.traceExit();
        final List<String> messages = app.getMessages();
        assertNotNull(messages, "No Messages");
        assertEquals(4, messages.size(),
                "Incorrect number of messages. Expected 4, Actual " + messages.size() + ": " + messages);
        String expect = "[TRACE] TestScriptPatternSelector ====== " +
                "o.a.l.l.c.PatternSelectorTest.testScriptPatternSelector:54 Enter ======" + Strings.LINE_SEPARATOR;
        assertEquals(expect, messages.get(0));
        expect = "[INFO ] TestScriptPatternSelector o.a.l.l.c.PatternSelectorTest.testScriptPatternSelector.55 " +
                "Hello World" + Strings.LINE_SEPARATOR;
        assertEquals(expect, messages.get(1));
        assertEquals("[INFO ] NoLocation No location information" + Strings.LINE_SEPARATOR, messages.get(2));
        app.clear();
    }

    @Test
    public void testJavaScriptPatternSelector(@Named("List3") final ListAppender app) {
        final org.apache.logging.log4j.Logger logger = LogManager.getLogger("TestJavaScriptPatternSelector");
        final org.apache.logging.log4j.Logger logger2 = LogManager.getLogger("JavascriptNoLocation");
        logger.traceEntry();
        logger.info("Hello World");
        logger2.info("No location information");
        logger.traceExit();
        final List<String> messages = app.getMessages();
        assertNotNull(messages, "No Messages");
        assertEquals(4, messages.size(),
                "Incorrect number of messages. Expected 4, Actual " + messages.size() + ": " + messages);
        String expect = "[TRACE] TestJavaScriptPatternSelector ====== " +
                "o.a.l.l.c.PatternSelectorTest.testJavaScriptPatternSelector:76 Enter ======" + Strings.LINE_SEPARATOR;
        assertEquals(expect, messages.get(0));
        expect = "[INFO ] TestJavaScriptPatternSelector " +
                "o.a.l.l.c.PatternSelectorTest.testJavaScriptPatternSelector.77 Hello World" + Strings.LINE_SEPARATOR;
        assertEquals(expect, messages.get(1));
        assertEquals("[INFO ] JavascriptNoLocation No location information" + Strings.LINE_SEPARATOR, messages.get(2));
        app.clear();
    }
}
