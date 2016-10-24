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

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 *
 */
public class RollingAppenderDeleteScriptTest {

    private static final String CONFIG = "log4j-rolling-with-custom-delete-script.xml";
    private static final String DIR = "target/rolling-with-delete-script/test";

    private final LoggerContextRule loggerContextRule = LoggerContextRule.createShutdownTimeoutLoggerContextRule(CONFIG);

    @Rule
    public RuleChain chain = loggerContextRule.withCleanFoldersRule(DIR);

    @Test
    public void testAppender() throws Exception {
        final Logger logger = loggerContextRule.getLogger();
        // Trigger the rollover
        for (int i = 0; i < 10; ++i) {
            // 30 chars per message: each message triggers a rollover
            logger.debug("This is a test message number " + i); // 30 chars:
        }
        Thread.sleep(100); // Allow time for rollover to complete

        final File dir = new File(DIR);
        assertTrue("Dir " + DIR + " should exist", dir.exists());
        assertTrue("Dir " + DIR + " should contain files", dir.listFiles().length > 0);

        final File[] files = dir.listFiles();
        for (final File file : files) {
            System.out.println(file);
        }
        for (final File file : files) {
            assertTrue(file.getName() + " starts with 'test-'", file.getName().startsWith("test-"));
            assertTrue(file.getName() + " ends with '.log'", file.getName().endsWith(".log"));
            final String strIndex = file.getName().substring(5, file.getName().indexOf('.'));
            final int index = Integer.parseInt(strIndex);
            assertTrue(file + " should have odd index", index % 2 == 1);
        }
    }
}
