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
package org.apache.logging.log4j.core.async;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class AsyncLoggersWithAsyncAppenderTest {
    private static Configuration config;
    private static ListAppender listAppender;
    private static LoggerContext ctx;

    @BeforeClass
    public static void beforeClass() {
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY,
                "AsyncLoggersWithAsyncAppenderTest.xml");
        System.setProperty(Constants.LOG4J_CONTEXT_SELECTOR, AsyncLoggerContextSelector.class.getName());
        ctx = LoggerContext.getContext(false);
        config = ctx.getConfiguration();
        listAppender = (ListAppender) config.getAppender("List");
    }

    @AfterClass
    public static void cleanupClass() {
        System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        System.clearProperty(Constants.LOG4J_CONTEXT_SELECTOR);
        ctx.reconfigure();
        StatusLogger.getLogger().reset();
    }

    @Test
    public void testLoggingWorks() throws Exception {        
        final Logger logger = LogManager.getLogger();
        logger.error("This is a test");
        logger.warn("Hello world!");
        Thread.sleep(100);
        final List<String> list = listAppender.getMessages();
        assertNotNull("No events generated", list);
        assertTrue("Incorrect number of events. Expected 2, got " + list.size(), list.size() == 2);
        String msg = list.get(0);
        String expected = getClass().getName() + " This is a test";
        assertTrue("Expected " + expected + ", Actual " + msg, expected.equals(msg));
        msg = list.get(1);
        expected = getClass().getName() + " Hello world!";
        assertTrue("Expected " + expected + ", Actual " + msg, expected.equals(msg));
    }
}
