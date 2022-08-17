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
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.test.junit.CleanUpDirectories;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 */
public class RollingAppenderCronEvery2DirectTest extends AbstractRollingListenerTest {

    private static final String CONFIG = "log4j-rolling-cron-every2-direct.xml";
    private static final String DIR = "target/rolling-cron-every2Direct";
    private final CountDownLatch rollover = new CountDownLatch(2);

    @Test
    @CleanUpDirectories(DIR)
    @LoggerContextSource(value = CONFIG, timeout = 10)
    public void testAppender(final Logger logger, @Named("RollingFile") final RollingFileManager manager) throws Exception {
        manager.addRolloverListener(this);
        final long end = currentTimeMillis.get() + 5000;
        final Random rand = new Random(end);
        int count = 1;
        do {
            logger.debug("Log Message {}", count++);
            currentTimeMillis.addAndGet(10 * rand.nextInt(100));
        } while (currentTimeMillis.get() < end);

        rollover.await();
        final Path dir = Path.of(DIR);
        assertThat(dir).isNotEmptyDirectory();
        assertThat(dir).isDirectoryContaining("glob:**.gz");
    }

    @Override
    public void rolloverComplete(final String fileName) {
        rollover.countDown();
    }
}
