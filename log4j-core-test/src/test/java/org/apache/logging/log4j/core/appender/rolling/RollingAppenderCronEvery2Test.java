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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.test.junit.TempLoggingDir;
import org.junit.jupiter.api.Test;

class RollingAppenderCronEvery2Test extends AbstractRollingListenerTest {

    private final CountDownLatch rollover = new CountDownLatch(3);

    @TempLoggingDir
    private static Path loggingPath;

    @Test
    @LoggerContextSource(timeout = 10)
    void testAppender(final Logger logger, @Named("RollingFile") final RollingFileManager manager) throws Exception {
        manager.addRolloverListener(this);
        assertThat(loggingPath.resolve("rollingtest.log")).exists();
        final long end = currentTimeMillis.get() + 5000;
        final Random rand = new Random(end);
        int count = 1;
        do {
            logger.debug("Log Message {}", count++);
            currentTimeMillis.addAndGet(10 * rand.nextInt(100));
        } while (currentTimeMillis.get() < end);

        rollover.await();
        assertThat(loggingPath).isNotEmptyDirectory().isDirectoryContaining("glob:**.gz");
    }

    @Override
    public void rolloverComplete(final String fileName) {
        rollover.countDown();
    }
}
