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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.junit.InitialLoggerContext;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 *
 */
public class ExtendedThrowableTest {
    private ListAppender app;

    @Rule
    public InitialLoggerContext init = new InitialLoggerContext("log4j-throwablefilter.xml");

    @Before
    public void setUp() throws Exception {
        this.app = (ListAppender) this.init.getContext().getConfiguration().getAppenders().get("List");
    }

    @After
    public void tearDown() throws Exception {
        this.app.clear();
    }

    @Test
    public void testException() {
        final Logger logger = this.init.getContext().getLogger("LoggerTest");
        final Throwable cause = new NullPointerException("null pointer");
        final Throwable parent = new IllegalArgumentException("IllegalArgument", cause);
        logger.error("Exception", parent);
        final List<String> msgs = app.getMessages();
        assertNotNull(msgs);
        assertEquals("Incorrect number of messages. Should be 1 is " + msgs.size(), msgs.size(), 1);
        assertTrue("No suppressed lines", msgs.get(0).contains("suppressed"));
    }
}
