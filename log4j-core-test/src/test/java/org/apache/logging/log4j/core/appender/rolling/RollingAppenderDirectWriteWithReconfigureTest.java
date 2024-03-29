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

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.test.junit.TempLoggingDir;
import org.junit.jupiter.api.Test;

class RollingAppenderDirectWriteWithReconfigureTest extends AbstractRollingListenerTest {

    private static final String CONFIG =
            "org/apache/logging/log4j/core/appender/rolling/RollingAppenderDirectWriteWithReconfigureTest.xml";

    private final CountDownLatch rollover = new CountDownLatch(1);

    @TempLoggingDir
    private static Path loggingPath;

    @Test
    @LoggerContextSource(timeout = 10)
    void testRollingFileAppenderWithReconfigure(final LoggerContext context) throws Exception {
        final var logger = context.getLogger(getClass());
        logger.debug("Before reconfigure");

        context.setConfigLocation(new URI(CONFIG));
        context.reconfigure();
        final Configuration config = context.getConfiguration();
        final RollingFileAppender appender = config.getAppender("RollingFile");
        appender.getManager().addRolloverListener(this);
        logger.debug("Force a rollover");
        rollover.await();
        assertThat(loggingPath).isNotEmptyDirectory();
        try (final Stream<Path> files = Files.list(loggingPath)) {
            assertThat(files).as("check log file count").hasSize(2);
        }
    }

    @Override
    public void rolloverComplete(final String fileName) {
        rollover.countDown();
    }
}
