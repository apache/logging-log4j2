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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.test.junit.CleanUpDirectories;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests
 */
public class RollingNewDirectoryTest implements RolloverListener {
    private static final String CONFIG = "log4j-rolling-new-directory.xml";

    private static final String DIR = "target/rolling-new-directory";
    private final CountDownLatch rollover = new CountDownLatch(3);

    @Test
    @CleanUpDirectories(DIR)
    @LoggerContextSource(value = CONFIG, timeout = 10)
    public void streamClosedError(final Logger logger, @Named("RollingFile") final RollingFileManager manager) throws Exception {
        manager.addRolloverListener(this);
        for (int i = 0; i <= 10; ++i) {
            logger.info("AAA");
            Thread.sleep(300);
        }
        try {
            if (!rollover.await(5, TimeUnit.SECONDS)) {
                fail("Timer expired before rollover");
            }
        } catch (InterruptedException ie) {
            fail("Thread was interrupted");
        }
        final File dir = new File(DIR);
        assertThat(dir).isNotEmptyDirectory();
        assertThat(dir.listFiles()).hasSizeGreaterThan(2);
    }

    @Override
    public void rolloverTriggered(final String fileName) {
    }

    @Override
    public void rolloverComplete(final String fileName) {
        rollover.countDown();
    }
}
