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
package org.apache.logging.log4j.jcl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.helpers.Constants;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
public class LoggerTest {

    private static final String CONFIG = "log4j-test1.xml";

    @BeforeClass
    public static void setupClass() {
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        ctx.getConfiguration();
    }

    @AfterClass
    public static void cleanupClass() {
        System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        ctx.reconfigure();
        StatusLogger.getLogger().reset();
    }

    Log logger = LogFactory.getLog("LoggerTest");

    @Test
    public void testLog() {
        logger.debug("Test message");
        verify("List", "o.a.l.l.j.LoggerTest Test message MDC{}" + Constants.LINE_SEP);
        logger.debug("Exception: " , new NullPointerException("Test"));
        verify("List", "o.a.l.l.j.LoggerTest Exception:  MDC{}" + Constants.LINE_SEP);
        logger.info("Info Message");
        verify("List", "o.a.l.l.j.LoggerTest Info Message MDC{}" + Constants.LINE_SEP);
        logger.info("Info Message {}");
        verify("List", "o.a.l.l.j.LoggerTest Info Message {} MDC{}" + Constants.LINE_SEP);
    }

    private void verify(final String name, final String expected) {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
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

}
