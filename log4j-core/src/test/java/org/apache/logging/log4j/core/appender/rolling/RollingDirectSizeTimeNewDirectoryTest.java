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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import java.io.File;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.sort;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests
 */
public class RollingDirectSizeTimeNewDirectoryTest {
    private static final String CONFIG = "log4j-rolling-size-time-new-directory.xml";

    private static final String DIR = "target/rolling-size-time-new-directory";

    public static LoggerContextRule loggerContextRule = LoggerContextRule.createShutdownTimeoutLoggerContextRule(CONFIG);

    @Rule
    public RuleChain chain = loggerContextRule.withCleanFoldersRule(DIR);

    private Logger logger;

    @Before
    public void setUp() throws Exception {
        this.logger = loggerContextRule.getLogger(RollingDirectSizeTimeNewDirectoryTest.class.getName());
    }


    @Test
    public void streamClosedError() throws Exception {
        for (int i = 0; i < 1000; i++) {
            logger.info("nHq6p9kgfvWfjzDRYbZp");
        }
        Thread.sleep(1500);
        for (int i = 0; i < 1000; i++) {
            logger.info("nHq6p9kgfvWfjzDRYbZp");
        }

        File tempDirectoryAsFile = new File(DIR);
        File[] loggingFolders = tempDirectoryAsFile.listFiles();
        assertNotNull(loggingFolders);
        // Check if two folders were created
        assertTrue("Not enough directories created", loggingFolders.length >= 2);
        for (File dir : loggingFolders) {
            File[] files = dir.listFiles();
            assertTrue("No files in directory " + dir.toString(), files != null && files.length > 0);
            if (files.length < 3) {
                System.out.println("Only " + files.length + " files created in " + dir.toString());
                for (File logFile : files) {
                    System.out.println("File name: " + logFile.getName() + " size: " + logFile.length());
                }
                fail();
            }

        }
    }
}
