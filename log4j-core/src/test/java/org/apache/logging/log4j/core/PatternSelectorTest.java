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
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.apache.logging.log4j.util.Strings;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.*;
/**
 *
 */
public class PatternSelectorTest {


    private static final String CONFIG = "log4j-patternSelector.xml";

    @ClassRule
    public static LoggerContextRule context = new LoggerContextRule(CONFIG);

    @Test
    public void testMarkerPatternSelector() throws Exception {
        final org.apache.logging.log4j.Logger logger = LogManager.getLogger("TestMarkerPatternSelector");
        logger.traceEntry();
        logger.info("Hello World");
        logger.traceExit();
        final ListAppender app = (ListAppender) context.getRequiredAppender("List");
        assertNotNull("No ListAppender", app);
        final List<String> messages = app.getMessages();
        assertNotNull("No Messages", messages);
        assertTrue("Incorrect number of messages. Expected 3, Actual " + messages.size() + ": " + messages, messages.size() == 3);
        final String expect = String.format("[TRACE] TestMarkerPatternSelector ====== "
                + "o.a.l.l.c.PatternSelectorTest.testMarkerPatternSelector:43 Enter ======%n");
        assertEquals(expect, messages.get(0));
        assertEquals("[INFO ] TestMarkerPatternSelector Hello World" + Strings.LINE_SEPARATOR, messages.get(1));
        app.clear();
    }

    @Test
    public void testScriptPatternSelector() throws Exception {
        final org.apache.logging.log4j.Logger logger = LogManager.getLogger("TestScriptPatternSelector");
        final org.apache.logging.log4j.Logger logger2 = LogManager.getLogger("NoLocation");
        logger.traceEntry();
        logger.info("Hello World");
        logger2.info("No location information");
        logger.traceExit();
        final ListAppender app = (ListAppender) context.getRequiredAppender("List2");
        assertNotNull("No ListAppender", app);
        final List<String> messages = app.getMessages();
        assertNotNull("No Messages", messages);
        assertTrue("Incorrect number of messages. Expected 4, Actual " + messages.size() + ": " + messages, messages.size() == 4);
        String expect = "[TRACE] TestScriptPatternSelector ====== " +
                "o.a.l.l.c.PatternSelectorTest.testScriptPatternSelector:62 Enter ======" + Strings.LINE_SEPARATOR;
        assertEquals(expect, messages.get(0));
        expect = "[INFO ] TestScriptPatternSelector o.a.l.l.c.PatternSelectorTest.testScriptPatternSelector.63 " +
                "Hello World" + Strings.LINE_SEPARATOR;
        assertEquals(expect, messages.get(1));
        assertEquals("[INFO ] NoLocation No location information" + Strings.LINE_SEPARATOR, messages.get(2));
        app.clear();
    }

    @Test
    public void testJavaScriptPatternSelector() throws Exception {
        final org.apache.logging.log4j.Logger logger = LogManager.getLogger("TestJavaScriptPatternSelector");
        final org.apache.logging.log4j.Logger logger2 = LogManager.getLogger("JavascriptNoLocation");
        logger.traceEntry();
        logger.info("Hello World");
        logger2.info("No location information");
        logger.traceExit();
        final ListAppender app = (ListAppender) context.getRequiredAppender("List3");
        assertNotNull("No ListAppender", app);
        final List<String> messages = app.getMessages();
        assertNotNull("No Messages", messages);
        assertTrue("Incorrect number of messages. Expected 4, Actual " + messages.size() + ": " + messages, messages.size() == 4);
        String expect = "[TRACE] TestJavaScriptPatternSelector ====== " +
                "o.a.l.l.c.PatternSelectorTest.testJavaScriptPatternSelector:85 Enter ======" + Strings.LINE_SEPARATOR;
        assertEquals(expect, messages.get(0));
        expect = "[INFO ] TestJavaScriptPatternSelector " +
                "o.a.l.l.c.PatternSelectorTest.testJavaScriptPatternSelector.86 Hello World" + Strings.LINE_SEPARATOR;
        assertEquals(expect, messages.get(1));
        assertEquals("[INFO ] JavascriptNoLocation No location information" + Strings.LINE_SEPARATOR, messages.get(2));
        app.clear();
    }
}
