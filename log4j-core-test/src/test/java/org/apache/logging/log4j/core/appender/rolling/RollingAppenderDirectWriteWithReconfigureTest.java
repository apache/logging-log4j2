/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache license, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the license for the specific language
 * governing permissions and limitations under the license.
 */
package org.apache.logging.log4j.core.appender.rolling;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.test.junit.CleanUpDirectories;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URI;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 */
public class RollingAppenderDirectWriteWithReconfigureTest extends AbstractRollingListenerTest {

    private static final String CONFIG = "log4j-rolling-direct-reconfigure.xml";

    private static final String DIR = "target/rolling-direct-reconfigure";

    private final CountDownLatch rollover = new CountDownLatch(1);

    @Test
    @CleanUpDirectories(DIR)
    @LoggerContextSource(value = CONFIG, timeout = 10)
    public void testRollingFileAppenderWithReconfigure(final LoggerContext context) throws Exception {
        final var logger = context.getLogger(getClass());
        logger.debug("Before reconfigure");

        context.setConfigLocation(new URI(CONFIG));
        context.reconfigure();
        Configuration config = context.getConfiguration();
        final RollingFileAppender appender = config.getAppender("RollingFile");
        appender.getManager().addRolloverListener(this);
        logger.debug("Force a rollover");
        rollover.await();
        final File dir = new File(DIR);
        assertThat(dir).isNotEmptyDirectory();
        assertThat(dir.listFiles()).hasSize(2);
    }

    @Override
    public void rolloverComplete(final String fileName) {
        rollover.countDown();
    }
}
