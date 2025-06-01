/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.appender.rolling;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class RollingAppenderUncompressedTest {

    private static final String CONFIG = "log4j-rolling4.xml";
    private static final String DIR = "target/rolling4";

    private final Logger logger = LogManager.getLogger(RollingAppenderUncompressedTest.class.getName());

    // @RegisterExtension
    // private CleanFoldersRuleExtension cleanFolders = new CleanFoldersRuleExtension(
    //         DIR,
    //         CONFIG,
    //         RollingAppenderUncompressedTest.class.getName(),
    //         this.getClass().getClassLoader());

    @BeforeAll
    public static void setupAll() {
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
    }

    @AfterAll
    public static void cleanupAll() {
        System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        final LoggerContext ctx = LoggerContext.getContext();
        ctx.reconfigure();
        StatusLogger.getLogger().reset();
    }

    @Test
    public void testAppender() {
        for (int i = 0; i < 100; ++i) {
            logger.debug("This is test message number " + i);
        }
        final File dir = new File(DIR);
        assertTrue(dir.exists() && dir.listFiles().length > 0, "Directory not created");
        final File[] files = dir.listFiles();
        assertNotNull(files);
        boolean found = false;
        for (final File file : files) {
            final String name = file.getName();
            if (name.startsWith("test1") && name.endsWith(".log")) {
                found = true;
                break;
            }
        }
        assertTrue(found, "No archived files found");
    }
}
