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
package org.apache.logging.log4j.core.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class XMLLoggerPropsTest {

    private static final String CONFIG = "log4j-loggerprops.xml";
    private static Configuration config;
    private static ListAppender app;
    private static LoggerContext ctx;

    @BeforeClass
    public static void setupClass() {
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
        System.setProperty("test", "test");
        ctx = (LoggerContext) LogManager.getContext(false);
        config = ctx.getConfiguration();
        for (final Map.Entry<String, Appender> entry : config.getAppenders().entrySet()) {
            if (entry.getKey().equals("List")) {
                app = (ListAppender) entry.getValue();
                break;
            }
        }
    }

    @AfterClass
    public static void cleanupClass() {
        System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        ctx.reconfigure();
        StatusLogger.getLogger().reset();
    }

    @Test
    public void testWithProps() {
        assertNotNull("No List Appender", app);

        try {
            assertTrue("Configuration is not an XMLConfiguration", config instanceof XMLConfiguration);
            Logger logger = LogManager.getLogger(XMLLoggerPropsTest.class);
            logger.debug("Test with props");
            logger = LogManager.getLogger("tiny.bubbles");
            logger.debug("Test on root");
            final List<String> events = app.getMessages();
            assertTrue("No events", events.size() > 0);
            assertTrue("Incorrect number of events", events.size() == 2);
            assertTrue("Incorrect value", events.get(0).contains("test=test"));
        } finally {
            System.clearProperty("test");
        }
    }
}
