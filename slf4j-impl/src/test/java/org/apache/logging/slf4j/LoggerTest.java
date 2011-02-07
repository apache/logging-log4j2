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

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.XMLConfigurationFactory;
import org.apache.logging.log4j.internal.StatusLogger;
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
import org.slf4j.helpers.Log4JLoggerFactory;

import java.util.Locale;


/**
 *
 */
public class LoggerTest {

    private static final String CONFIG = "log4j-test1.xml";

    @BeforeClass
    public static void setupClass() {
        System.setProperty(XMLConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
        LoggerContext ctx = Log4JLoggerFactory.getContext();
        Configuration config = ctx.getConfiguration();
    }

    @AfterClass
    public static void cleanupClass() {
        System.clearProperty(XMLConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        LoggerContext ctx = Log4JLoggerFactory.getContext();
        ctx.reconfigure();
        StatusLogger.getLogger().reset();
    }

    Logger logger = LoggerFactory.getLogger("LoggerTest");
    XLogger xlogger = XLoggerFactory.getXLogger("LoggerTest");

    @Test
    public void basicFlow() {
        xlogger.entry();
        xlogger.exit();
    }

    @Test
    public void simpleFlow() {
        xlogger.entry(CONFIG);
        xlogger.exit(0);
    }

    @Test
    public void throwing() {
        xlogger.throwing(new IllegalArgumentException("Test Exception"));
    }

    @Test
    public void catching() {
        try {
            throw new NullPointerException();
        } catch (Exception e) {
            xlogger.catching(e);
        }
    }

    @Test
    public void debug() {
        logger.debug("Debug message");
    }


    @Test
    public void debugWithParms() {
        logger.debug("Hello, {}", "World");
    }

    @Test
    public void mdc() {

        MDC.put("TestYear", "2010");
        logger.debug("Debug message");
        MDC.clear();
        logger.debug("Debug message");
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
    }
}
