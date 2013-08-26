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
package org.apache.logging.slf4j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.helpers.Constants;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 *
 */
public class OptionalTest {

    private static final String CONFIG = "log4j-test1.xml";
    private static LoggerContext ctx;

    @BeforeClass
    public static void setupClass() {
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
        ctx = (LoggerContext) LogManager.getContext(false);
        ctx.getConfiguration();
    }

    @AfterClass
    public static void cleanupClass() {
        System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        ctx.reconfigure();
        StatusLogger.getLogger().reset();
    }

    Logger logger = LoggerFactory.getLogger("EventLogger");
    Marker marker = MarkerFactory.getMarker("EVENT");

    @Test
    public void testEventLogger() {
        logger.info(marker, "This is a test");
        MDC.clear();
        verify("EventLogger", "o.a.l.s.OptionalTest This is a test" + Constants.LINE_SEP);
    }

    private void verify(final String name, final String expected) {
        //LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Map<String, Appender> list = ctx.getConfiguration().getAppenders();
        final Appender listApp = list.get(name);
        assertNotNull("Missing Appender", listApp);
        assertTrue("Not a ListAppender", listApp instanceof ListAppender);
        final List<String> events = ((ListAppender) listApp).getMessages();
        assertTrue("Incorrect number of messages. Expected 1 Actual " + events.size(), events.size()== 1);
        final String actual = events.get(0);
        assertEquals("Incorrect message. Expected " + expected + ". Actual " + actual, expected, actual);
        ((ListAppender) listApp).clear();
    }

    @Before
    public void cleanup()
    {
        final Map<String, Appender> list = ctx.getConfiguration().getAppenders();
        final Appender listApp = list.get("List");
        ((ListAppender) listApp).clear();
        final Appender eventApp = list.get("EventLogger");
        ((ListAppender) eventApp).clear();
    }
}
