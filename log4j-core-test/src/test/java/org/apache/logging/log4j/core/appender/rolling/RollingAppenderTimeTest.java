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
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.time.Clock;
import org.apache.logging.log4j.plugins.Factory;
import org.apache.logging.log4j.test.junit.CleanUpDirectories;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

import static org.apache.logging.log4j.core.test.hamcrest.Descriptors.that;
import static org.apache.logging.log4j.core.test.hamcrest.FileMatchers.hasName;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 *
 */
public class RollingAppenderTimeTest {

    private static final String CONFIG = "log4j-rolling2.xml";
    private static final String DIR = "target/rolling2";

    private final AtomicLong currentTimeMillis = new AtomicLong(System.currentTimeMillis());

    @Factory
    Clock clock() {
        return currentTimeMillis::get;
    }

    @Test
    @CleanUpDirectories(DIR)
    @LoggerContextSource(value = CONFIG, timeout = 10)
    public void testAppender(final LoggerContext context) throws Exception {
        final Logger logger = context.getLogger(getClass());
        logger.debug("This is test message number 1");
        currentTimeMillis.addAndGet(1500);
        // Trigger the rollover
        for (int i = 0; i < 16; ++i) {
            logger.debug("This is test message number " + i + 1);
        }
        final File dir = new File(DIR);
        assertTrue(dir.exists() && dir.listFiles().length > 0, "Directory not created");

        final int MAX_TRIES = 20;
        final Matcher<File[]> hasGzippedFile = hasItemInArray(that(hasName(that(endsWith(".gz")))));
        for (int i = 0; i < MAX_TRIES; i++) {
            final File[] files = dir.listFiles();
            if (hasGzippedFile.matches(files)) {
                return; // test succeeded
            }
            logger.debug("Adding additional event " + i);
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(100)); // Allow time for rollover to complete
        }
        fail("No compressed files found");
    }
}
