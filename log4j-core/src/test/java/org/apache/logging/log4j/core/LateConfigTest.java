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
package org.apache.logging.log4j.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.XMLConfiguration;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class LateConfigTest {

    private static final String CONFIG = "target/test-classes/log4j-test1.xml";
    private static Configuration config;
    private static ListAppender app;
    private static LoggerContext ctx;

    private final org.apache.logging.log4j.Logger logger = LogManager.getLogger("LoggerTest");

    @BeforeClass
    public static void setupClass() {
        ctx = (LoggerContext) LogManager.getContext(false);
    }

    @Test
    public void testReconfiguration() throws Exception {
        final Configuration cfg = ctx.getConfiguration();
        assertNotNull("No configuration", cfg);
        assertTrue("Not set to default configuration", cfg instanceof DefaultConfiguration);
        final File file = new File(CONFIG);
        LoggerContext loggerContext = (LoggerContext) LogManager.getContext(null, false, file.toURI());
        assertNotNull("No Logger Context", loggerContext);
        final Configuration newConfig = loggerContext.getConfiguration();
        assertTrue("Configuration not reset", cfg != newConfig);
        assertTrue("Reconfiguration failed", newConfig instanceof XMLConfiguration);
        ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration sameConfig = ctx.getConfiguration();
        assertTrue("Configuration should not have been reset", newConfig == sameConfig);
    }
}

