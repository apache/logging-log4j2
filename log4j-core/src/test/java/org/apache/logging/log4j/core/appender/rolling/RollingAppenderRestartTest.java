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

import org.apache.commons.io.file.PathUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.hamcrest.Matcher;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.apache.logging.log4j.core.test.hamcrest.Descriptors.that;
import static org.apache.logging.log4j.core.test.hamcrest.FileMatchers.hasName;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RollingAppenderRestartTest implements RolloverListener {

    private static final String CONFIG = "log4j-rolling-restart.xml";

    // Note that both paths are hardcoded in the configuration!
    private static final Path DIR = Paths.get("target/rolling-restart");
    private static final Path FILE = DIR.resolve("test.log");
    private static final CountDownLatch latch = new CountDownLatch(1);

    private final LoggerContextRule loggerContextRule =
            LoggerContextRule.createShutdownTimeoutLoggerContextRule(CONFIG);

    @Rule
    public RuleChain chain =
            loggerContextRule.withCleanFoldersRule(
                    false, true, 5, DIR.toAbsolutePath().toString());

    @BeforeClass
    public static void setup() throws Exception {
        tearDown();
        Files.createDirectories(DIR);
        Files.write(FILE, "Hello, world".getBytes(), StandardOpenOption.CREATE);
        FileTime newTime = FileTime.from(Instant.now().minus(2, ChronoUnit.DAYS));
        Files
                .getFileAttributeView(FILE, BasicFileAttributeView.class)
                .setTimes(newTime, newTime, newTime);
    }

    @AfterClass
    public static void tearDown() throws IOException {
        if (DIR.toFile().exists()) {
            PathUtils.deleteDirectory(DIR);
        }
    }

    @Test
    public void testAppender() throws Exception {
        final Logger logger = loggerContextRule.getLogger();
        final RollingFileAppender appender = (RollingFileAppender) loggerContextRule.getAppender("RollingFile");
        assertNotNull("No RollingFile Appender", appender);
        appender.getManager().addRolloverListener(this);
        logger.info("This is test message number 1");
        latch.await(100, TimeUnit.MILLISECONDS);
        // Delay to allow asynchronous gzip to complete.
        Thread.sleep(200);
        final Matcher<File[]> hasGzippedFile = hasItemInArray(that(hasName(that(endsWith(".gz")))));
        final File[] files = DIR.toFile().listFiles();
        assertTrue(
                "was expecting files with '.gz' suffix, found: " + Arrays.toString(files),
                hasGzippedFile.matches(files));
    }

    @Override
    public void rolloverTriggered(String fileName) {

    }

    @Override
    public void rolloverComplete(String fileName) {
        latch.countDown();
    }
}
