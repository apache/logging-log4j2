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
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.test.junit.CleanUpDirectories;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 *
 */
@Tag("sleepy")
public class RollingAppenderCronEvery2DirectTest {

    private static final String CONFIG = "log4j-rolling-cron-every2-direct.xml";
    private static final String DIR = "target/rolling-cron-every2Direct";
    private static final int LOOP_COUNT = 100;

    @Test
    @CleanUpDirectories(DIR)
    @LoggerContextSource(value = CONFIG, timeout = 10)
    public void testAppender(final Logger logger) throws Exception {
        // TODO Is there a better way to test than putting the thread to sleep all over the place?
        final long end = System.currentTimeMillis() + 5000;
        final Random rand = new SecureRandom();
        rand.setSeed(end);
        int count = 1;
        do {
            logger.debug("Log Message {}", count++);
            Thread.sleep(10 * rand.nextInt(100));
        } while (System.currentTimeMillis() < end);
        final Path dir = Path.of(DIR);
        assertTrue(Files.exists(dir));
        try (Stream<Path> stream = Files.list(dir)) {
            assertTrue(stream.findAny().isPresent(), "Directory not created");
        }

        final int MAX_TRIES = 20;
        boolean succeeded = false;
        for (int i = 0; i < MAX_TRIES; i++) {
            try (Stream<Path> stream = Files.list(dir)) {
                if (stream.anyMatch(path -> path.toString().endsWith(".gz"))) {
                    succeeded = true;
                    break;
                }
            }
            logger.debug("Sleeping #{}", i);
            Thread.sleep(100); // Allow time for rollover to complete
        }
        if (!succeeded) {
            try (Stream<Path> stream = Files.list(dir)) {
                final List<String> fileNames = stream.map(Path::getFileName).map(Path::toString).collect(Collectors.toList());
                fail("No compressed files found; found: " + fileNames);
            }
        }
    }

}
