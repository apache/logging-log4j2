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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 */
public class RollingAppenderDirectCronTest {

    private static final String CONFIG = "log4j-rolling-direct-cron.xml";
    private static final String DIR = "target/rolling-direct-cron";

    private final LoggerContextRule loggerContextRule = LoggerContextRule.createShutdownTimeoutLoggerContextRule(CONFIG);

    @Rule
    public RuleChain chain = loggerContextRule.withCleanFoldersRule(DIR);

    private final Pattern filePattern = Pattern.compile(".*(\\d\\d-\\d\\d-\\d\\d-\\d\\d-\\d\\d-\\d\\d).*$");

    @Test
    public void testAppender() throws Exception {
        // TODO Is there a better way to test than putting the thread to sleep all over the place?
        RollingFileAppender app = loggerContextRule.getAppender("RollingFile");
        final Logger logger = loggerContextRule.getLogger();
        logger.debug("This is test message number 1");
        RolloverDelay delay = new RolloverDelay(app.getManager());
        delay.waitForRollover();
        final File dir = new File(DIR);
        File[] files = dir.listFiles();
        assertTrue("Directory not created", dir.exists() && files != null && files.length > 0);
        delay.reset(3);

        final int MAX_TRIES = 30;
        for (int i = 0; i < MAX_TRIES; ++i) {
            logger.debug("Adding new event {}", i);
            Thread.sleep(100);
        }
        delay.waitForRollover();
    }



    private class RolloverDelay implements RolloverListener {
        private volatile CountDownLatch latch;

        public RolloverDelay(RollingFileManager manager) {
            latch = new CountDownLatch(1);
            manager.addRolloverListener(this);
        }

        public void waitForRollover() {
            try {
                if (!latch.await(3, TimeUnit.SECONDS)) {
                    fail("failed to rollover");
                }
            } catch (InterruptedException ex) {
                fail("failed to rollover");
            }
        }

        public void reset(int count) {
            latch = new CountDownLatch(count);
        }

        @Override
        public void rolloverTriggered(String fileName) {

        }

        @Override
        public void rolloverComplete(String fileName) {
            java.util.regex.Matcher matcher = filePattern.matcher(fileName);
            assertTrue("Invalid file name: " + fileName, matcher.matches());
            Path path = new File(fileName).toPath();
            try {
                List<String> lines = Files.readAllLines(path);
                assertTrue("Not enough lines in " + fileName + ":" + lines.size(), lines.size() > 0);
                assertTrue("log and file times don't match. file: " + matcher.group(1) + ", log: "
                        + lines.get(0), lines.get(0).startsWith(matcher.group(1)));
            } catch (IOException ex) {
                fail("Unable to read file " + fileName + ": " + ex.getMessage());
            }
            latch.countDown();
        }
    }
}
