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

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.core.async.ArrayBlockingQueueFactory;
import org.apache.logging.log4j.core.async.BlockingQueueFactory;
import org.apache.logging.log4j.core.async.DisruptorBlockingQueueFactory;
import org.apache.logging.log4j.core.async.LinkedTransferQueueFactory;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.*;

/**
 *
 */
@RunWith(Parameterized.class)
public class AsyncAppenderTest {

    @Parameterized.Parameters
    public static Object[] data() {
        return new String[]{
            // default async config uses array blocking queue
            "log4j-asynch.xml",
            // override default blocking queue implementations
            "BlockingQueueFactory-ArrayBlockingQueue.xml",
            "BlockingQueueFactory-DisruptorBlockingQueue.xml",
            "BlockingQueueFactory-LinkedTransferQueue.xml"
        };
    }

    public AsyncAppenderTest(final String configFileName) {
        context = new LoggerContextRule(configFileName);
    }

    @Rule
    public LoggerContextRule context;

    private ListAppender listAppender;

    @Before
    public void before() throws Exception {
        listAppender = context.getListAppender("List");
    }

    @After
    public void after() {
        listAppender.clear();
    }

    @Test
    public void rewriteTest() throws Exception {
        final Logger logger = LogManager.getLogger(AsyncAppender.class);
        logger.error("This is a test");
        logger.warn("Hello world!");
        Thread.sleep(100);
        final List<String> list = listAppender.getMessages();
        assertNotNull("No events generated", list);
        assertTrue("Incorrect number of events. Expected 2, got " + list.size(), list.size() == 2);
        String msg = list.get(0);
        String expected = AsyncAppenderTest.class.getName() + " rewriteTest This is a test";
        assertTrue("Expected " + expected + ", Actual " + msg, expected.equals(msg));
        msg = list.get(1);
        expected = AsyncAppenderTest.class.getName() + " rewriteTest Hello world!";
        assertTrue("Expected " + expected + ", Actual " + msg, expected.equals(msg));
    }

    @Test
    public void testException() throws Exception {
        final Logger logger = LogManager.getLogger(AsyncAppender.class);
        final Exception parent = new IllegalStateException("Test");
        final Throwable child = new LoggingException("This is a test", parent);
        logger.error("This is a test", child);
        Thread.sleep(100);
        final List<String> list = listAppender.getMessages();
        assertNotNull("No events generated", list);
        assertTrue("Incorrect number of events. Expected 1, got " + list.size(), list.size() == 1);
        final String msg = list.get(0);
        assertTrue("No parent exception", msg.contains("java.lang.IllegalStateException"));
    }
}
