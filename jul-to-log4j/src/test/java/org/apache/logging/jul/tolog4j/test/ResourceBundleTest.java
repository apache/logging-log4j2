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
package org.apache.logging.jul.tolog4j.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.logging.jul.tolog4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test methods that accept a resource bundle
 */
public class ResourceBundleTest {

    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("ResourceBundleTest");
    private static final String SOURCE_CLASS = "SourceClass";
    private static final String SOURCE_METHOD = "sourceMethod";

    private static final String[] EXPECTED_MESSAGES = {"Hello!", "Hello Log4j and JUL!"};
    private static final String[] EXPECTED_CLASS_NAMES = {ResourceBundleTest.class.getName(), SOURCE_CLASS};
    private static final String[] EXPECTED_METHOD_NAMES = {"testCorrectMessageAndLocation", SOURCE_METHOD};

    @Rule
    public final LoggerContextRule ctx = new LoggerContextRule("ResourceBundleTest.xml");

    @BeforeClass
    public static void setUpClass() {
        System.setProperty("java.util.logging.manager", LogManager.class.getName());
    }

    @AfterClass
    public static void tearDownClass() {
        System.clearProperty("java.util.logging.manager");
    }

    @Test
    public void testCorrectMessageAndLocation() {
        ListAppender appender = ctx.getListAppender("LIST").clear();
        Logger logger = Logger.getLogger(ResourceBundleTest.class.getName());

        Throwable thrown = new RuntimeException();
        logger.logrb(Level.INFO, BUNDLE, "msg_1", thrown);
        logger.logrb(Level.INFO, BUNDLE, "msg_2", "Log4j", "JUL");
        logger.logrb(Level.INFO, SOURCE_CLASS, SOURCE_METHOD, BUNDLE, "msg_1", thrown);
        logger.logrb(Level.INFO, SOURCE_CLASS, SOURCE_METHOD, BUNDLE, "msg_2", "Log4j", "JUL");

        LogEvent[] logEvents = appender.getEvents().toArray(LogEvent[]::new);
        for (int idx = 0; idx < logEvents.length; ++idx) {
            assertEquals(
                    String.format("Message of event %d", idx),
                    EXPECTED_MESSAGES[idx % 2],
                    logEvents[idx].getMessage().getFormattedMessage());
            assertEquals(
                    String.format("Source class of event %d", idx),
                    EXPECTED_CLASS_NAMES[idx / 2],
                    logEvents[idx].getSource().getClassName());
            assertEquals(
                    String.format("Source method of event %d", idx),
                    EXPECTED_METHOD_NAMES[idx / 2],
                    logEvents[idx].getSource().getMethodName());
            assertSame(
                    String.format("Exception of event %d", idx),
                    idx % 2 == 0 ? thrown : null,
                    logEvents[idx].getThrown());
        }
    }
}
