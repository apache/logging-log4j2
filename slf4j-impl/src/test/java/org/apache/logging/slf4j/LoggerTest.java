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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ListAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.XMLConfigurationFactory;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.ext.EventData;
import org.slf4j.ext.EventLogger;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class LoggerTest {

    private static final String CONFIG = "log4j-test1.xml";
    private static LoggerContext ctx;

    @BeforeClass
    public static void setupClass() {
        System.setProperty(XMLConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
        ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
    }

    @AfterClass
    public static void cleanupClass() {
        System.clearProperty(XMLConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        ctx.reconfigure();
        StatusLogger.getLogger().reset();
    }

    Logger logger = LoggerFactory.getLogger("LoggerTest");
    XLogger xlogger = XLoggerFactory.getXLogger("LoggerTest");

    @Test
    public void basicFlow() {
        xlogger.entry();
        verify("List", "o.a.l.s.LoggerTest entry MDC{}\n");
        xlogger.exit();
        verify("List", "o.a.l.s.LoggerTest exit MDC{}\n");
    }

    @Test
    public void simpleFlow() {
        xlogger.entry(CONFIG);
        verify("List", "o.a.l.s.LoggerTest entry with (log4j-test1.xml) MDC{}\n");
        xlogger.exit(0);
        verify("List", "o.a.l.s.LoggerTest exit with (0) MDC{}\n");
    }

    @Test
    public void throwing() {
        xlogger.throwing(new IllegalArgumentException("Test Exception"));
        verify("List", "o.a.l.s.LoggerTest throwing MDC{}\n");
    }

    @Test
    public void catching() {
        try {
            throw new NullPointerException();
        } catch (Exception e) {
            xlogger.catching(e);
            verify("List", "o.a.l.s.LoggerTest catching MDC{}\n");
        }
    }

    @Test
    public void debug() {
        logger.debug("Debug message");
        verify("List", "o.a.l.s.LoggerTest Debug message MDC{}\n");
    }


    @Test
    public void debugWithParms() {
        logger.debug("Hello, {}", "World");
        verify("List", "o.a.l.s.LoggerTest Hello, World MDC{}\n");
    }

    @Test
    public void mdc() {

        MDC.put("TestYear", "2010");
        logger.debug("Debug message");
        verify("List", "o.a.l.s.LoggerTest Debug message MDC{TestYear=2010}\n");
        MDC.clear();
        logger.debug("Debug message");
        verify("List", "o.a.l.s.LoggerTest Debug message MDC{}\n");
    }

    @Test
    public void testEventLogger() {
        MDC.put("loginId", "JohnDoe");
        MDC.put("ipAddress", "192.168.0.120");
        MDC.put("locale", Locale.US.getDisplayName());
        EventData data = new EventData();
        data.setEventType("Transfer");
        data.setEventId("Audit@18060");
        data.setMessage("Transfer Complete");
        data.put("ToAccount", "123456");
        data.put("FromAccount", "123457");
        data.put("Amount", "200.00");
        EventLogger.logEvent(data);
        MDC.clear();
        verify("EventLogger", "o.a.l.s.LoggerTest Transfer [Audit@18060 Amount=\"200.00\" FromAccount=\"123457\" ToAccount=\"123456\"] Transfer Complete\n");
    }

    private void verify(String name, String expected) {
        //LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Map<String, Appender> list = ctx.getConfiguration().getAppenders();
        Appender listApp = list.get(name);
        assertNotNull("Missing Appender", listApp);
        assertTrue("Not a ListAppender", listApp instanceof ListAppender);
        List<String> events = ((ListAppender) listApp).getMessages();
        assertTrue("Incorrect number of messages. Expected 1 Actual " + events.size(), events.size()== 1);
        String actual = events.get(0);
        assertEquals("Incorrect message. Expected " + expected + ". Actual " + actual, expected, actual);
        ((ListAppender) listApp).clear();
    }
}
