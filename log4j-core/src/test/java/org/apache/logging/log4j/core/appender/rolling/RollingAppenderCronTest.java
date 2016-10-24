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

import static org.apache.logging.log4j.hamcrest.Descriptors.that;
import static org.apache.logging.log4j.hamcrest.FileMatchers.hasName;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.util.CronExpression;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 *
 */
public class RollingAppenderCronTest {

    private static final String CONFIG = "log4j-rolling-cron.xml";
    private static final String DIR = "target/rolling-cron";
    private static final String FILE = "target/rolling-cron/rollingtest.log";

    private final LoggerContextRule loggerContextRule = LoggerContextRule.createShutdownTimeoutLoggerContextRule(CONFIG);

    @Rule
    public RuleChain chain = loggerContextRule.withCleanFoldersRule(DIR);

    @Test
    public void testAppender() throws Exception {
        // TODO Is there a better way to test than putting the thread to sleep all over the place?
        final Logger logger = loggerContextRule.getLogger();
        final File file = new File(FILE);
        assertTrue("Log file does not exist", file.exists());
        logger.debug("This is test message number 1");
        Thread.sleep(2500);
        final File dir = new File(DIR);
        assertTrue("Directory not created", dir.exists() && dir.listFiles().length > 0);

        final int MAX_TRIES = 20;
        final Matcher<File[]> hasGzippedFile = hasItemInArray(that(hasName(that(endsWith(".gz")))));
        boolean succeeded = false;
        for (int i = 0; i < MAX_TRIES; i++) {
            final File[] files = dir.listFiles();
            if (hasGzippedFile.matches(files)) {
                succeeded = true;
                break;
            }
            logger.debug("Sleeping #" + i);
            Thread.sleep(100); // Allow time for rollover to complete
        }
        if (!succeeded) {
            final File[] files = dir.listFiles();
            for (final File dirFile : files) {
                logger.error("Found file: " + dirFile.getPath());
            }
            fail("No compressed files found");
        }
        final Path src = FileSystems.getDefault().getPath("target/test-classes/log4j-rolling-cron2.xml");
        try (final OutputStream os = new FileOutputStream("target/test-classes/log4j-rolling-cron.xml")) {
            Files.copy(src, os);
        }
        Thread.sleep(5000);
        // force a reconfiguration
        for (int i = 0; i < MAX_TRIES; ++i) {
            logger.debug("Adding new event {}", i);
        }
        Thread.sleep(1000);
        final RollingFileAppender app = (RollingFileAppender) loggerContextRule.getLoggerContext().getConfiguration().getAppender("RollingFile");
        final TriggeringPolicy policy = app.getManager().getTriggeringPolicy();
        assertNotNull("No triggering policy", policy);
        assertTrue("Incorrect policy type", policy instanceof CronTriggeringPolicy);
        final CronExpression expression = ((CronTriggeringPolicy) policy).getCronExpression();
        assertTrue("Incorrect triggering policy", expression.getCronExpression().equals("* * * ? * *"));

    }

}
