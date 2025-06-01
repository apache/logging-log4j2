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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.test.junit.CleanFoldersRuleExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * This test attempts to validate that logging rolls when the file size exceeds 5KB or every second.
 * When the file rolls by time it should create a new directory. When rolling by size it should
 * create multiple files per directory.
 */
public class RollingDirectSizeTimeNewDirectoryTest implements RolloverListener {

    private static final String CONFIG = "log4j-rolling-size-time-new-directory.xml";

    // Note that the path is hardcoded in the configuration!
    private static final String DIR = "target/rolling-size-time-new-directory";

    @RegisterExtension
    CleanFoldersRuleExtension extension = new CleanFoldersRuleExtension(
            DIR,
            CONFIG,
            RollingDirectSizeTimeNewDirectoryTest.class.getName(),
            this.getClass().getClassLoader());

    private final Map<String, AtomicInteger> rolloverFiles = new HashMap<>();

    @Test
    public void streamClosedError(final LoggerContext loggerContext) throws Exception {
        ((RollingFileAppender) loggerContext.getConfiguration().getAppender("RollingFile"))
                .getManager()
                .addRolloverListener(this);
        final Logger logger = loggerContext.getLogger(RollingDirectSizeTimeNewDirectoryTest.class);

        for (int i = 0; i < 1000; i++) {
            logger.info("nHq6p9kgfvWfjzDRYbZp");
        }
        Thread.sleep(1500);
        for (int i = 0; i < 1000; i++) {
            logger.info("nHq6p9kgfvWfjzDRYbZp");
        }

        assertTrue(rolloverFiles.size() > 1, "A time based rollover did not occur");
        final int maxFiles = Collections.max(rolloverFiles.values(), Comparator.comparing(AtomicInteger::get))
                .get();
        assertTrue(maxFiles > 1, "No size based rollovers occurred");
    }

    @Override
    public void rolloverTriggered(final String fileName) {}

    @Override
    public void rolloverComplete(final String fileName) {
        final File file = new File(fileName);
        final String logDir = file.getParentFile().getName();
        final AtomicInteger fileCount = rolloverFiles.computeIfAbsent(logDir, k -> new AtomicInteger(0));
        fileCount.incrementAndGet();
    }
}
