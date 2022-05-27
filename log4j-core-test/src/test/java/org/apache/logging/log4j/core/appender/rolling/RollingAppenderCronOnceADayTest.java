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
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.util.CronExpression;
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.junit.CleanUpDirectories;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Calendar;

import static org.apache.logging.log4j.core.test.hamcrest.Descriptors.that;
import static org.apache.logging.log4j.core.test.hamcrest.FileMatchers.hasName;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This test currently takes about a minute to run.
 */
@Tag("sleepy")
public class RollingAppenderCronOnceADayTest {

    private static final int CRON_DELAY = 10;
    private static final String CONFIG = "log4j-rolling-cron-once-a-day.xml";
    private static final String CONFIG_TARGET = "log4j-rolling-cron-once-a-day-target.xml";
    private static final String TARGET = "target";
    private static final String DIR = TARGET + "/rolling-cron-once-a-day";
    private static final String FILE = DIR + "/rollingtest.log";
    private static final String TARGET_TEST_CLASSES = TARGET + "/test-classes";

    private static String cronExpression;
    private static long remainingTime;

    @BeforeAll
    public static void beforeClass() throws Exception {
      final Path src = FileSystems.getDefault().getPath(TARGET_TEST_CLASSES, CONFIG);
      String content = Files.readString(src);
      final Calendar cal = Calendar.getInstance();
      cal.add(Calendar.SECOND, CRON_DELAY);
      remainingTime = cal.getTimeInMillis() - System.currentTimeMillis();
      cronExpression =  String.format("%d %d %d * * ?",
          cal.get(Calendar.SECOND),
          cal.get(Calendar.MINUTE),
          cal.get(Calendar.HOUR_OF_DAY));
      content = content.replace("@CRON_EXPR@", cronExpression);
      Files.write(FileSystems.getDefault()
            .getPath(TARGET_TEST_CLASSES, CONFIG_TARGET), content.getBytes(StandardCharsets.UTF_8));
      StatusLogger.getLogger().debug("Cron expression will be " + cronExpression + " in " + remainingTime + "ms");
    }

    @Test
    @CleanUpDirectories(DIR)
    @LoggerContextSource(value = CONFIG_TARGET, timeout = 10)
    public void testAppender(final Logger logger, @Named("RollingFile") final RollingFileAppender app) throws Exception {
        // TODO Is there a better way to test than putting the thread to sleep all over the place?
        final File file = new File(FILE);
        assertTrue(file.exists(), "Log file does not exist");
        logger.debug("This is test message number 1, waiting for rolling");

        final TriggeringPolicy policy = app.getManager().getTriggeringPolicy();
        assertNotNull(policy, "No triggering policy");
        assertTrue(policy instanceof CronTriggeringPolicy, "Incorrect policy type");
        final CronExpression expression = ((CronTriggeringPolicy) policy).getCronExpression();
        assertEquals(cronExpression, expression.getCronExpression(), "Incorrect cron expresion");
        logger.debug("Cron expression will be {}", expression.getCronExpression());

        // force a reconfiguration
        for (int i = 1; i <= 20; ++i) {
            logger.debug("Adding first event {}", i);
        }

        Thread.sleep(remainingTime);
        final File dir = new File(DIR);
        assertTrue(dir.exists() && dir.listFiles().length > 0, "Directory not created");

        for (int i = 1; i < 5; i++) {
          logger.debug("Adding some more event {}", i);
          Thread.sleep(1000);
        }
        final Matcher<File> hasGzippedFile = hasName(that(endsWith(".gz")));
        int count = 0;
        final File[] files = dir.listFiles();
        assertNotNull(files);
        for (final File generatedFile : files) {
          if (hasGzippedFile.matches(generatedFile)) {
              count++;
          }
        }

        assertNotEquals(0, count, "No compressed files found");
        assertEquals(1, count, "Multiple files found");
    }

}
