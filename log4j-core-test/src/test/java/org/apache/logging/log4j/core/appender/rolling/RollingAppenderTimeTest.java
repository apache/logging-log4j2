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
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.test.junit.CleanUpDirectories;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 */
public class RollingAppenderTimeTest extends AbstractRollingListenerTest {

    private static final String CONFIG = "log4j-rolling2.xml";
    private static final String DIR = "target/rolling2";
    private final CountDownLatch rollover = new CountDownLatch(1);

    @Test
    @CleanUpDirectories(DIR)
    @LoggerContextSource(value = CONFIG, timeout = 10)
    public void testAppender(final LoggerContext context, @Named("RollingFile") final RollingFileManager manager) throws Exception {
        manager.addRolloverListener(this);
        final Logger logger = context.getLogger(getClass());
        logger.debug("This is test message number 1");
        currentTimeMillis.addAndGet(1500);
        // Trigger the rollover
        for (int i = 0; i < 16; ++i) {
            logger.debug("This is test message number {}", i + 1);
            currentTimeMillis.addAndGet(100);
        }
        rollover.await();
        final File dir = new File(DIR);
        assertThat(dir).isNotEmptyDirectory();
        assertThat(dir).isDirectoryContaining("glob:**.gz");
    }

    @Override
    public void rolloverComplete(final String fileName) {
        rollover.countDown();
    }
}
