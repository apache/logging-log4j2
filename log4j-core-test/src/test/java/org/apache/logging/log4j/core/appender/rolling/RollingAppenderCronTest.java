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

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.CountDownLatch;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.util.CronExpression;
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.test.junit.TempLoggingDir;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;

class RollingAppenderCronTest extends AbstractRollingListenerTest {

    private static final Path CONFIG;

    static {
        try {
            final Path config =
                    Path.of(requireNonNull(RollingAppenderCronTest.class.getResource("RollingAppenderCronTest.old.xml"))
                            .toURI());
            CONFIG = config.resolveSibling("RollingAppenderCronTest.xml");
        } catch (final URISyntaxException e) {
            throw new TestAbortedException("Unable to compute configuration location.", e);
        }
    }

    private final CountDownLatch rollover = new CountDownLatch(2);
    private final CountDownLatch reconfigured = new CountDownLatch(1);

    @TempLoggingDir
    private static Path loggingPath;

    @BeforeAll
    static void beforeEach() throws Exception {
        final Path src =
                Path.of(requireNonNull(RollingAppenderCronTest.class.getResource("RollingAppenderCronTest.old.xml"))
                        .toURI());
        Files.copy(src, CONFIG, REPLACE_EXISTING);
        // Set modification time to the start of the epoch, in order for modification detection to work on every OS.
        Files.setLastModifiedTime(CONFIG, FileTime.fromMillis(0L));
    }

    @AfterAll
    static void cleanUp() throws Exception {
        Files.deleteIfExists(CONFIG);
    }

    @AfterAll
    static void cleanUp() throws Exception {
        Files.deleteIfExists(CONFIG);
    }

    @Test
    @LoggerContextSource(timeout = 10)
    public void testAppender(final LoggerContext context, @Named("RollingFile") final RollingFileManager manager)
            throws Exception {
        manager.addRolloverListener(this);
        final Logger logger = context.getLogger(getClass());
        assertThat(loggingPath.resolve("rollingtest.log")).exists();
        logger.debug("This is test message number 1");
        rollover.await();
        assertThat(loggingPath).isNotEmptyDirectory().isDirectoryContaining("glob:**.gz");

        final Path src =
                Path.of(requireNonNull(RollingAppenderCronTest.class.getResource("RollingAppenderCronTest.new.xml"))
                        .toURI());
        context.addConfigurationStartedListener(ignored -> reconfigured.countDown());
        Files.copy(src, CONFIG, REPLACE_EXISTING);

        // force a reconfiguration
        for (int i = 0; i < 20; ++i) {
            logger.debug("Adding new event {}", i);
        }
        reconfigured.await();
        final RollingFileAppender appender = context.getConfiguration().getAppender("RollingFile");
        final TriggeringPolicy policy = appender.getManager().getTriggeringPolicy();
        assertThat(policy).isNotNull();
        assertThat(policy).isInstanceOf(CronTriggeringPolicy.class);
        final CronExpression expression = ((CronTriggeringPolicy) policy).getCronExpression();
        assertEquals("* * * ? * *", expression.getCronExpression(), "Incorrect triggering policy");
    }

    @Override
    public void rolloverComplete(final String fileName) {
        rollover.countDown();
    }
}
