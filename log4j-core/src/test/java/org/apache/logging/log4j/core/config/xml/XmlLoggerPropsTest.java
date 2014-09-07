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
package org.apache.logging.log4j.core.config.xml;

import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
public class XmlLoggerPropsTest {

    private static final String CONFIG = "log4j-loggerprops.xml";
    private static Configuration config;
    private static ListAppender listAppender;
    private static LoggerContext ctx;

    @BeforeClass
    public static void setupClass() {
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
        System.setProperty("test", "test");
        ctx = (LoggerContext) LogManager.getContext(false);
        config = ctx.getConfiguration();
        listAppender = (ListAppender) config.getAppender("List");
    }

    @AfterClass
    public static void cleanupClass() {
        System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        ctx.reconfigure();
        StatusLogger.getLogger().reset();
    }

    @Test
    public void testWithProps() {
        assertNotNull("No List Appender", listAppender);

        try {
            assertTrue("Configuration is not an XmlConfiguration", config instanceof XmlConfiguration);
            Logger logger = LogManager.getLogger(XmlLoggerPropsTest.class);
            logger.debug("Test with props");
            logger = LogManager.getLogger("tiny.bubbles");
            logger.debug("Test on root");
            final List<String> events = listAppender.getMessages();
            assertTrue("No events", events.size() > 0);
            assertTrue("Incorrect number of events", events.size() == 2);
            assertTrue("Incorrect value", events.get(0).contains("user="));
            assertTrue("Incorrect value", events.get(0).contains("phrasex=****"));
            assertTrue("Incorrect value", events.get(0).contains("test=test"));
            assertTrue("Incorrect value", events.get(0).contains("test2=test2default"));
            assertTrue("Incorrect value", events.get(0).contains("test3=Unknown"));
            assertTrue("Incorrect value", events.get(1).contains("user="));
            assertTrue("Incorrect value", events.get(1).contains("phrasex=****"));
            assertTrue("Incorrect value", events.get(1).contains("test=test"));
            assertTrue("Incorrect value", events.get(1).contains("test2=test2default"));
            assertTrue("Incorrect value", events.get(1).contains("test3=Unknown"));
        } finally {
            System.clearProperty("test");
        }
    }
}
