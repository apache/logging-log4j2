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
package org.apache.logging.log4j.core.appender.rolling;

import static org.junit.Assert.*;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
public class RollingAppenderTimeTest {

    private static final String CONFIG = "log4j-rolling2.xml";
    private static final String DIR = "target/rolling2";

    org.apache.logging.log4j.Logger logger = LogManager.getLogger(RollingAppenderTimeTest.class.getName());

    @BeforeClass
    public static void setupClass() {
        deleteDir();
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
        final LoggerContext ctx = (LoggerContext) LogManager.getContext();
        final Configuration config = ctx.getConfiguration();
    }

    @AfterClass
    public static void cleanupClass() {
        //deleteDir();
        System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        final LoggerContext ctx = (LoggerContext) LogManager.getContext();
        ctx.reconfigure();
        StatusLogger.getLogger().reset();
    }

    @Test
    public void testAppender() throws Exception {
        logger.debug("This is test message number 1");
        Thread.sleep(1500);
        // Trigger the rollover
        for (int i = 0; i < 16; ++i) {
            logger.debug("This is test message number " + i + 1);
        }
        final File dir = new File(DIR);
        assertTrue("Directory not created", dir.exists() && dir.listFiles().length > 0);

        final int MAX_TRIES = 10;
        for (int i = 0; i < MAX_TRIES; i++) {
            final File[] files = dir.listFiles();
            assertTrue("No files created", files.length > 0);
            for (final File file : files) {
                if (file.getName().endsWith(".gz")) {
                    return; // test succeeded
                }
            }
            Thread.sleep(50); // Allow time for rollover to complete
        }
        fail("No compressed files found");
    }

    private static void deleteDir() {
        final File dir = new File(DIR);
        if (dir.exists()) {
            final File[] files = dir.listFiles();
            for (final File file : files) {
                file.delete();
            }
            dir.delete();
        }
    }
}
