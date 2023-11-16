/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

public class PatternResolverDoesNotEvaluateThreadContextTest {

    private static final String CONFIG = "log4j2-pattern-layout-with-context.xml";
    private static final String PARAMETER = "user";
    private ListAppender listAppender;

    @ClassRule
    public static LoggerContextRule context = new LoggerContextRule(CONFIG);

    @Before
    public void before() {
        listAppender = context.getRequiredAppender("list", ListAppender.class);
        listAppender.clear();
    }

    @Test
    public void testNoUserSet() {
        final Logger logger = context.getLogger(getClass());
        logger.info("This is a test");
        final List<String> messages = listAppender.getMessages();
        assertTrue(messages != null && messages.size() > 0, "No messages returned");
        final String message = messages.get(0);
        assertEquals(
                "INFO org.apache.logging.log4j.core."
                        + "PatternResolverDoesNotEvaluateThreadContextTest ${ctx:user} This is a test",
                message);
    }

    @Test
    public void testMessageIsNotLookedUp() {
        final Logger logger = context.getLogger(getClass());
        logger.info("This is a ${upper:test}");
        final List<String> messages = listAppender.getMessages();
        assertTrue(messages != null && messages.size() > 0, "No messages returned");
        final String message = messages.get(0);
        assertEquals(
                "INFO org.apache.logging.log4j.core."
                        + "PatternResolverDoesNotEvaluateThreadContextTest ${ctx:user} This is a ${upper:test}",
                message);
    }

    @Test
    public void testUser() {
        final Logger logger = context.getLogger(getClass());
        ThreadContext.put(PARAMETER, "123");
        try {
            logger.info("This is a test");
        } finally {
            ThreadContext.remove(PARAMETER);
        }
        final List<String> messages = listAppender.getMessages();
        assertTrue(messages != null && messages.size() > 0, "No messages returned");
        final String message = messages.get(0);
        assertEquals(
                "INFO org.apache.logging.log4j.core."
                        + "PatternResolverDoesNotEvaluateThreadContextTest 123 This is a test",
                message);
    }

    @Test
    public void testUserIsLookup() {
        final Logger logger = context.getLogger(getClass());
        ThreadContext.put(PARAMETER, "${java:version}");
        try {
            logger.info("This is a test");
        } finally {
            ThreadContext.remove(PARAMETER);
        }
        final List<String> messages = listAppender.getMessages();
        assertTrue(messages != null && messages.size() > 0, "No messages returned");
        final String message = messages.get(0);
        assertEquals(
                "INFO org.apache.logging.log4j.core."
                        + "PatternResolverDoesNotEvaluateThreadContextTest ${java:version} This is a test",
                message);
    }

    @Test
    public void testUserHasLookup() {
        final Logger logger = context.getLogger(getClass());
        ThreadContext.put(PARAMETER, "user${java:version}name");
        try {
            logger.info("This is a test");
        } finally {
            ThreadContext.remove(PARAMETER);
        }
        final List<String> messages = listAppender.getMessages();
        assertTrue(messages != null && messages.size() > 0, "No messages returned");
        final String message = messages.get(0);
        assertEquals(
                "INFO org.apache.logging.log4j.core."
                        + "PatternResolverDoesNotEvaluateThreadContextTest user${java:version}name This is a test",
                message);
    }
}
