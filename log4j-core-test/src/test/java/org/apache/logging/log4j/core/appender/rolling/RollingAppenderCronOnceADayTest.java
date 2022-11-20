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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.util.CronExpression;
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.junit.CleanUpDirectories;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("flaky")
@Disabled("https://issues.apache.org/jira/browse/LOG4J2-3633")
public class RollingAppenderCronOnceADayTest extends AbstractRollingListenerTest {

    private static final int CRON_DELAY = 10;
    private static final String CONFIG = "log4j-rolling-cron-once-a-day.xml";
    private static final String CONFIG_TARGET = "log4j-rolling-cron-once-a-day-target.xml";
    private static final String TARGET = "target";
    private static final String DIR = TARGET + "/rolling-cron-once-a-day";
    private static final String FILE = DIR + "/rollingtest.log";
    private static final String TARGET_TEST_CLASSES = TARGET + "/test-classes";

    private static String cronExpression;
    private static Duration remainingTime;
    private final CountDownLatch rollover = new CountDownLatch(1);


    @BeforeAll
    public static void beforeClass() throws Exception {
        final Path src = FileSystems.getDefault().getPath(TARGET_TEST_CLASSES, CONFIG);
        String content = Files.readString(src);
        final ZonedDateTime now = ZonedDateTime.now();
        final ZonedDateTime end = now.plusSeconds(CRON_DELAY);
        remainingTime = Duration.between(now, end);
        cronExpression = String.format("%d %d %d * * ?", end.getSecond(), end.getMinute(), end.getHour());
        content = content.replace("@CRON_EXPR@", cronExpression);
        Files.writeString(Path.of(TARGET_TEST_CLASSES, CONFIG_TARGET), content);
        StatusLogger.getLogger().debug("Cron expression will be " + cronExpression + " in " + remainingTime);
    }

    @Test
    @CleanUpDirectories(DIR)
    @LoggerContextSource(value = CONFIG_TARGET, timeout = 10)
    public void testAppender(final Logger logger, @Named("RollingFile") final RollingFileManager manager) throws Exception {
        manager.addRolloverListener(this);
        final File file = new File(FILE);
        assertTrue(file.exists(), "Log file does not exist");
        logger.debug("This is test message number 1, waiting for rolling");

        final TriggeringPolicy policy = manager.getTriggeringPolicy();
        assertThat(policy).isInstanceOf(CronTriggeringPolicy.class);
        final CronExpression expression = ((CronTriggeringPolicy) policy).getCronExpression();
        assertEquals(cronExpression, expression.getCronExpression(), "Incorrect cron expression");
        logger.debug("Cron expression will be {}", expression.getCronExpression());

        // force a rollover
        final long delta = remainingTime.dividedBy(20).toMillis();
        for (int i = 1; i <= 20; ++i) {
            logger.debug("Adding first event {}", i);
            currentTimeMillis.addAndGet(delta);
        }

        rollover.await();
        final File dir = new File(DIR);
        assertThat(dir).isNotEmptyDirectory();

        for (int i = 1; i < 5; i++) {
            logger.debug("Adding some more event {}", i);
            currentTimeMillis.addAndGet(1000);
        }
        assertThat(dir).isDirectoryContaining("glob:**.gz");
        assertThat(dir.listFiles(pathname -> pathname.getName().endsWith(".gz"))).hasSize(1);
    }

    @Override
    public void rolloverComplete(final String fileName) {
        rollover.countDown();
    }
}
