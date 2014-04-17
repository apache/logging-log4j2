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

import java.io.File;
import java.io.Serializable;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.xml.XMLConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.junit.InitialLoggerContext;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class CustomConfigurationTest {

    public static final String LOG_FILE = "target/test.log";

    @Rule
    public InitialLoggerContext init = new InitialLoggerContext("log4j-props.xml");

    @Before
    public void setUp() throws Exception {
        new File(LOG_FILE).delete();
    }

    @Test
    public void testConfig() {
        // don't bother using "error" since that's the default; try another level
        System.setProperty("log4j.level", "info");
        final LoggerContext ctx = this.init.getContext();
        ctx.reconfigure();
        final Configuration config = ctx.getConfiguration();
        assertTrue("Configuration is not an XMLConfiguration", config instanceof XMLConfiguration);
        assertSame(StatusLogger.getLogger().getLevel(), Level.INFO);
        Layout<? extends Serializable> layout = PatternLayout.createLayout(PatternLayout.SIMPLE_CONVERSION_PATTERN, config, null,
            null,null, null, null, null);
        Appender appender = FileAppender.createAppender(LOG_FILE, "false", "false", "File", "true",
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
        Logger logger = ctx.getLogger(CustomConfigurationTest.class.getName());
        logger.info("This is a test");
        final File file = new File(LOG_FILE);
        assertTrue("log file not created", file.exists());
        assertTrue("No data logged", file.length() > 0);

    }
}
