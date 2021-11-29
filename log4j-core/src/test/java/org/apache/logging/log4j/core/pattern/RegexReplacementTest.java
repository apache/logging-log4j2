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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.junit.LoggerContextSource;
import org.apache.logging.log4j.junit.Named;
import org.apache.logging.log4j.junit.UsingThreadContextMap;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.Test;

@LoggerContextSource("log4j-replace.xml")
@UsingThreadContextMap
public class RegexReplacementTest {
    private final ListAppender app;
    private final ListAppender app2;
    private final org.apache.logging.log4j.Logger logger;
    private final org.apache.logging.log4j.Logger logger2;

    private static final String EXPECTED = "/RegexReplacementTest" + Strings.LINE_SEPARATOR;

    public RegexReplacementTest(
            final LoggerContext context, @Named("List") final ListAppender app, @Named("List2") final ListAppender app2) {
        logger = context.getLogger("LoggerTest");
        logger2 = context.getLogger("ReplacementTest");
        this.app = app.clear();
        this.app2 = app2.clear();
    }

    @Test
    public void testReplacement() {
        logger.error(this.getClass().getName());
        List<String> msgs = app.getMessages();
        assertNotNull(msgs);
        assertEquals(1, msgs.size(), "Incorrect number of messages. Should be 1 is " + msgs.size());
        assertTrue(
                msgs.get(0).endsWith(EXPECTED), "Replacement failed - expected ending " + EXPECTED + " Actual " + msgs.get(0));

    }

    @Test
    public void testMessageReplacement() {
        ThreadContext.put("MyKey", "Apache");
        logger.error("This is a test for ${ctx:MyKey}");
        List<String> msgs = app.getMessages();
        assertNotNull(msgs);
        assertEquals(1, msgs.size(), "Incorrect number of messages. Should be 1 is " + msgs.size());
        assertEquals("LoggerTest This is a test for Apache" + Strings.LINE_SEPARATOR, msgs.get(0));
    }

    @Test
    public void testConverter() {
        logger2.error(this.getClass().getName());
        final List<String> msgs = app2.getMessages();
        assertNotNull(msgs);
        assertEquals(1, msgs.size(), "Incorrect number of messages. Should be 1 is " + msgs.size());
        assertTrue(
                msgs.get(0).endsWith(EXPECTED), "Replacement failed - expected ending " + EXPECTED + " Actual " + msgs.get(0));
    }
}
