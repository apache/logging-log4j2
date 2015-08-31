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

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class ExtendedThrowableTest {
    private ListAppender app;

    @ClassRule
    public static LoggerContextRule context = new LoggerContextRule("log4j-throwablefilter.xml");

    @Before
    public void setUp() throws Exception {
        this.app = context.getListAppender("List").clear();
    }

    @Test
    public void testException() {
        final Logger logger = context.getLogger("LoggerTest");
        final Throwable cause = new NullPointerException("null pointer");
        final Throwable parent = new IllegalArgumentException("IllegalArgument", cause);
        logger.error("Exception", parent);
        final List<String> msgs = app.getMessages();
        assertNotNull(msgs);
        assertEquals("Incorrect number of messages. Should be 1 is " + msgs.size(), 1, msgs.size());
        assertTrue("No suppressed lines", msgs.get(0).contains("suppressed"));
    }
}
