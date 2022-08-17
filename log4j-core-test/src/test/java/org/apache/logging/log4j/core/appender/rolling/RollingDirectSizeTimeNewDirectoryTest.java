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
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.time.Clock;
import org.apache.logging.log4j.plugins.Factory;
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.test.junit.CleanUpDirectories;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This test attempts to validate that logging rolls when the file size exceeds 5KB or every second.
 * When the file rolls by time it should create a new directory. When rolling by size it should
 * create multiple files per directory.
 */
@Disabled("https://issues.apache.org/jira/browse/LOG4J2-3449")
public class RollingDirectSizeTimeNewDirectoryTest implements RolloverListener {

    private static final String CONFIG = "log4j-rolling-size-time-new-directory.xml";

    // Note that the path is hardcoded in the configuration!
    private static final String DIR = "target/rolling-size-time-new-directory";

    private final Map<String, AtomicInteger> rolloverFiles = new HashMap<>();
    private final AtomicLong currentTimeMillis = new AtomicLong(System.currentTimeMillis());
    private final Phaser phaser = new Phaser(1);

    @Factory
    Clock clock() {
        return currentTimeMillis::get;
    }

    @Test
    @CleanUpDirectories(DIR)
    @LoggerContextSource(value = CONFIG, timeout = 15)
    public void streamClosedError(final LoggerContext context, @Named("RollingFile") final RollingFileAppender appender) throws Exception {
        appender.getManager().addRolloverListener(this);
        final Logger logger = context.getLogger(RollingDirectSizeTimeNewDirectoryTest.class);

        for (int i = 0; i < 1000; i++) {
            currentTimeMillis.incrementAndGet();
            logger.info("nHq6p9kgfvWfjzDRYbZp");
        }
        currentTimeMillis.addAndGet(500);
        for (int i = 0; i < 1000; i++) {
            currentTimeMillis.incrementAndGet();
            logger.info("nHq6p9kgfvWfjzDRYbZp");
        }

        phaser.arriveAndAwaitAdvance();

        assertTrue(rolloverFiles.size() > 1, "A time based rollover did not occur");
        int maxFiles = Collections.max(rolloverFiles.values(), Comparator.comparing(AtomicInteger::get)).get();
        assertTrue(maxFiles > 1, "No size based rollovers occurred");
    }

    @Override
    public void rolloverTriggered(String fileName) {
        phaser.register();
    }

    @Override
    public void rolloverComplete(String fileName) {
        File file = new File(fileName);
        String logDir = file.getParentFile().getName();
        AtomicInteger fileCount = rolloverFiles.computeIfAbsent(logDir, k -> new AtomicInteger(0));
        fileCount.incrementAndGet();
        phaser.arriveAndDeregister();
    }
}


