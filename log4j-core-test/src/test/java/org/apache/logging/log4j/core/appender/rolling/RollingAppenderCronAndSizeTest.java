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

import static org.apache.logging.log4j.core.test.hamcrest.Descriptors.that;
import static org.apache.logging.log4j.core.test.hamcrest.FileMatchers.hasName;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.Random;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 * LOG4J2-1804.
 */
public class RollingAppenderCronAndSizeTest {

    private static final String CONFIG = "log4j-rolling-cron-and-size.xml";

    private static final String DIR = "target/rolling-cron-size";

    public static LoggerContextRule loggerContextRule =
            LoggerContextRule.createShutdownTimeoutLoggerContextRule(CONFIG);

    @Rule
    public RuleChain chain = loggerContextRule.withCleanFoldersRule(DIR);

    private Logger logger;

    @Before
    public void setUp() throws Exception {
        this.logger = loggerContextRule.getLogger(RollingAppenderCronAndSizeTest.class.getName());
    }

    @Test
    public void testAppender() throws Exception {
        final Random rand = new Random();
        for (int j = 0; j < 100; ++j) {
            final int count = rand.nextInt(100);
            for (int i = 0; i < count; ++i) {
                logger.debug("This is test message number " + i);
            }
            Thread.sleep(rand.nextInt(50));
        }
        Thread.sleep(50);
        final File dir = new File(DIR);
        assertTrue("Directory not created", dir.exists() && dir.listFiles().length > 0);
        final File[] files = dir.listFiles();
        Arrays.sort(files);
        assertNotNull(files);
        assertThat(files, hasItemInArray(that(hasName(that(endsWith(".log"))))));
        final int found = 0;
        int fileCounter = 0;
        String previous = "";
        for (final File file : files) {
            final String actual = file.getName();
            final StringBuilder padding = new StringBuilder();
            final String length = Long.toString(file.length());
            for (int i = length.length(); i < 10; ++i) {
                padding.append(" ");
            }
            final String[] fileParts = actual.split("_|\\.");
            fileCounter = previous.equals(fileParts[1]) ? ++fileCounter : 1;
            previous = fileParts[1];
            assertEquals(
                    "Incorrect file name. Expected counter value of " + fileCounter + " in " + actual,
                    Integer.toString(fileCounter),
                    fileParts[2]);
        }
    }
}
