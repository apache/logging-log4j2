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
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.helpers.Constants;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertTrue;

/**
 *
 */
public class CustomConfigurationTest {

    private static final String CONFIG = "log4j-props.xml";

    @BeforeClass
    public static void setupClass() {
    }

    @AfterClass
    public static void cleanupClass() {
        System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        ctx.reconfigure();
        StatusLogger.getLogger().reset();
    }

    @Test
    public void testConfig() {
        File file = new File("target/test.log");
        file.delete();
        System.setProperty("log4j.level", "error");
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        ctx.reconfigure();
        final Configuration config = ctx.getConfiguration();
        assertTrue("Configuration is not an XMLConfiguration", config instanceof XMLConfiguration);
        Layout layout = PatternLayout.createLayout(PatternLayout.SIMPLE_CONVERSION_PATTERN, config, null,
            null,null, null);
        Appender appender = FileAppender.createAppender("target/test.log", "false", "false", "File", "true",
            "false", "false", "4000", layout, null, "false", null, config);
        appender.start();
        config.addAppender(appender);
        AppenderRef ref = AppenderRef.createAppenderRef("File", null, null);
        AppenderRef[] refs = new AppenderRef[] {ref};

        LoggerConfig loggerConfig = LoggerConfig.createLogger("false", "info", "org.apache.logging.log4j",
            "true", refs, null, config, null );
        loggerConfig.addAppender(appender, null, null);
        config.addLogger("org.apache.logging.log4j", loggerConfig);
        ctx.updateLoggers();
        Logger logger = LogManager.getLogger("org.apache.logging.log4j.core.config.CustomConfigurationTest");
        logger.info("This is a test");
        file = new File("target/test.log");
        assertTrue("log file not created", file.exists());
        assertTrue("No data logged", file.length() > 0);

    }
}
