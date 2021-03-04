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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import static org.junit.Assert.assertTrue;

/**
 * This test attempts to validate that logging rolls when the file size exceeds 5KB or every second.
 * When the file rolls by time it should create a new directory. When rolling by size it should
 * create multiple files per directory.
 */
public class RollingDirectSizeTimeNewDirectoryTest implements RolloverListener {

    private static final String CONFIG = "log4j-rolling-size-time-new-directory.xml";

    // Note that the path is hardcoded in the configuration!
    private static final String DIR = "target/rolling-size-time-new-directory";

    public static LoggerContextRule loggerContextRule =
            LoggerContextRule.createShutdownTimeoutLoggerContextRule(CONFIG);

    @Rule
    public RuleChain chain = loggerContextRule.withCleanFoldersRule(DIR);

    private Map<String, AtomicInteger> rolloverFiles = new HashMap<>();

    @Test
    public void streamClosedError() throws Exception {
        ((RollingFileAppender) loggerContextRule.getAppender("RollingFile")).getManager()
                .addRolloverListener(this);
        final Logger logger = loggerContextRule.getLogger(RollingDirectSizeTimeNewDirectoryTest.class);

        for (int i = 0; i < 1000; i++) {
            logger.info("nHq6p9kgfvWfjzDRYbZp");
        }
        Thread.sleep(1500);
        for (int i = 0; i < 1000; i++) {
            logger.info("nHq6p9kgfvWfjzDRYbZp");
        }

        assertTrue("A time based rollover did not occur", rolloverFiles.size() > 1);
        int maxFiles = Collections.max(rolloverFiles.values(), Comparator.comparing(AtomicInteger::get)).get();
        assertTrue("No size based rollovers occurred", maxFiles > 1);
    }

    @Override
    public void rolloverTriggered(String fileName) {

    }

    @Override
    public void rolloverComplete(String fileName) {
        File file = new File(fileName);
        String logDir = file.getParentFile().getName();
        AtomicInteger fileCount = rolloverFiles.computeIfAbsent(logDir, k -> new AtomicInteger(0));
        fileCount.incrementAndGet();
    }
}

