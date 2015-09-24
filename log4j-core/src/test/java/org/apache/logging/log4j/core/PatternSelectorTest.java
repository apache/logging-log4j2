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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;

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
        org.apache.logging.log4j.Logger logger = LogManager.getLogger("TestMarkerPatternSelector");
        logger.entry();
        logger.info("Hello World");
        logger.exit();
        final ListAppender app = (ListAppender) context.getRequiredAppender("List");
        assertNotNull("No ListAppender", app);
        List<String> messages = app.getMessages();
        assertNotNull("No Messages", messages);
        assertTrue("Incorrect number of messages. Expected 3, Actual " + messages.size(), messages.size() == 3);
        final String expect = String.format("[TRACE] TestMarkerPatternSelector ====== "
                + "o.a.l.l.c.PatternSelectorTest.testMarkerPatternSelector:42 entry ======%n");
        assertEquals(expect, messages.get(0));
        assertEquals("[INFO ] TestMarkerPatternSelector Hello World\n", messages.get(1));
        app.clear();
    }

    @Test
    public void testScriptPatternSelector() throws Exception {
        org.apache.logging.log4j.Logger logger = LogManager.getLogger("TestScriptPatternSelector");
        org.apache.logging.log4j.Logger logger2 = LogManager.getLogger("NoLocation");
        logger.entry();
        logger.info("Hello World");
        logger2.info("No location information");
        logger.exit();
        final ListAppender app = (ListAppender) context.getRequiredAppender("List2");
        assertNotNull("No ListAppender", app);
        List<String> messages = app.getMessages();
        assertNotNull("No Messages", messages);
        assertTrue("Incorrect number of messages. Expected 4, Actual " + messages.size(), messages.size() == 4);
        String expect = "[TRACE] TestScriptPatternSelector ====== " +
                "o.a.l.l.c.PatternSelectorTest.testScriptPatternSelector:61 entry ======\n";
        assertEquals(expect, messages.get(0));
        expect = "[INFO ] TestScriptPatternSelector o.a.l.l.c.PatternSelectorTest.testScriptPatternSelector.62 " +
                "Hello World\n";
        assertEquals(expect, messages.get(1));
        assertEquals("[INFO ] NoLocation No location information\n", messages.get(2));
        app.clear();
    }

    @Test
    public void testJavaScriptPatternSelector() throws Exception {
        org.apache.logging.log4j.Logger logger = LogManager.getLogger("TestJavaScriptPatternSelector");
        org.apache.logging.log4j.Logger logger2 = LogManager.getLogger("JavascriptNoLocation");
        logger.entry();
        logger.info("Hello World");
        logger2.info("No location information");
        logger.exit();
        final ListAppender app = (ListAppender) context.getRequiredAppender("List3");
        assertNotNull("No ListAppender", app);
        List<String> messages = app.getMessages();
        assertNotNull("No Messages", messages);
        assertTrue("Incorrect number of messages. Expected 4, Actual " + messages.size(), messages.size() == 4);
        String expect = "[TRACE] TestJavaScriptPatternSelector ====== " +
                "o.a.l.l.c.PatternSelectorTest.testJavaScriptPatternSelector:84 entry ======\n";
        assertEquals(expect, messages.get(0));
        expect = "[INFO ] TestJavaScriptPatternSelector " +
                "o.a.l.l.c.PatternSelectorTest.testJavaScriptPatternSelector.85 Hello World\n";
        assertEquals(expect, messages.get(1));
        assertEquals("[INFO ] JavascriptNoLocation No location information\n", messages.get(2));
        app.clear();
    }
}
