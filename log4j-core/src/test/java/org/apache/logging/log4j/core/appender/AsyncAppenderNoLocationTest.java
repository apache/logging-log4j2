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
package org.apache.logging.log4j.core.appender;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.apache.logging.log4j.LogManager;
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
public class AsyncAppenderNoLocationTest {
    private ListAppender app;

    @Rule
    public InitialLoggerContext init = new InitialLoggerContext("log4j-asynch-no-location.xml");

    @Before
    public void setUp() throws Exception {
        this.app = (ListAppender) this.init.getAppender("List");
    }

    @After
    public void after() {
        app.clear();
    }

    @Test
    public void testNoLocation() throws Exception {
        final Logger logger = LogManager.getLogger(AsyncAppender.class);
        logger.error("This is a test");
        logger.warn("Hello world!");
        Thread.sleep(100);
        final List<String> list = app.getMessages();
        assertNotNull("No events generated", list);
        assertEquals("Incorrect number of events. Expected 2, got " + list.size(), list.size(), 2);
        String msg = list.get(0);
        String expected = "?  This is a test";
        assertEquals("Expected " + expected + ", Actual " + msg, expected, msg);
        msg = list.get(1);
        expected = "?  Hello world!";
        assertEquals("Expected " + expected + ", Actual " + msg, expected, msg);
    }
}
