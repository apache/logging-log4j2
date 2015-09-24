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
    public void testPatternSelector() throws Exception {
        org.apache.logging.log4j.Logger logger = LogManager.getLogger("TestPatternSelector");
        logger.entry();
        logger.info("Hello World");
        logger.exit();
        final ListAppender app = (ListAppender) context.getRequiredAppender("List");
        assertNotNull("No ListAppender", app);
        List<String> messages = app.getMessages();
        assertNotNull("No Messages", messages);
        assertTrue("Incorrect number of messages. Expected 3, Actual " + messages.size(), messages.size() == 3);
        final String expect = String.format("[TRACE] TestPatternSelector ====== "
                + "o.a.l.l.c.PatternSelectorTest.testPatternSelector:42 entry ======%n");
        assertEquals(expect, messages.get(0));
        assertEquals(String.format("[INFO ] TestPatternSelector Hello World%n"), messages.get(1));
    }
}
