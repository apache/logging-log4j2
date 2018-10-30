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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.junit.ThreadContextMapRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.apache.logging.log4j.util.Strings;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 *
 */
public class RegexReplacementTest {
    private static final String CONFIG = "log4j-replace.xml";
    private static ListAppender app;
    private static ListAppender app2;

    private static final String EXPECTED = "/RegexReplacementTest" + Strings.LINE_SEPARATOR;

    @ClassRule
    public static LoggerContextRule context = new LoggerContextRule(CONFIG);

    @Rule
    public final ThreadContextMapRule threadContextRule = new ThreadContextMapRule(); 

    @Before
    public void setUp() throws Exception {
        app = context.getListAppender("List").clear();
        app2 = context.getListAppender("List2").clear();
    }

    org.apache.logging.log4j.Logger logger = context.getLogger("LoggerTest");
    org.apache.logging.log4j.Logger logger2 = context.getLogger("ReplacementTest");

    @Test
    public void testReplacement() {
        logger.error(this.getClass().getName());
        List<String> msgs = app.getMessages();
        assertNotNull(msgs);
        assertEquals("Incorrect number of messages. Should be 1 is " + msgs.size(), 1, msgs.size());
        assertTrue("Replacement failed - expected ending " + EXPECTED + " Actual " + msgs.get(0),
            msgs.get(0).endsWith(EXPECTED));
        app.clear();
        ThreadContext.put("MyKey", "Apache");
        logger.error("This is a test for ${ctx:MyKey}");
        msgs = app.getMessages();
        assertNotNull(msgs);
        assertEquals("Incorrect number of messages. Should be 1 is " + msgs.size(), 1, msgs.size());
        assertEquals("LoggerTest This is a test for Apache" + Strings.LINE_SEPARATOR, msgs.get(0));
    }
     @Test
    public void testConverter() {
        logger2.error(this.getClass().getName());
        final List<String> msgs = app2.getMessages();
        assertNotNull(msgs);
         assertEquals("Incorrect number of messages. Should be 1 is " + msgs.size(), 1, msgs.size());
         assertTrue("Replacement failed - expected ending " + EXPECTED + " Actual " + msgs.get(0),
             msgs.get(0).endsWith(EXPECTED));
    }
}
