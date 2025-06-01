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

import static org.apache.logging.log4j.core.test.hamcrest.Descriptors.that;
import static org.apache.logging.log4j.core.test.hamcrest.FileMatchers.hasName;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.file.PathUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.test.junit.CleanFoldersRuleExtension;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class RollingAppenderRestartTest {

    private static final String CONFIG = "log4j-rolling-restart.xml";

    // Note that both paths are hardcoded in the configuration!
    private static final Path DIR = Paths.get("target/rolling-restart");
    private static final Path FILE = DIR.resolve("test.log");

    @RegisterExtension
    CleanFoldersRuleExtension extension = new CleanFoldersRuleExtension(
            DIR.toAbsolutePath().toString(),
            CONFIG,
            RollingAppenderRestartTest.class.getName(),
            this.getClass().getClassLoader(),
            false,
            true,
            5);

    @BeforeAll
    public static void setup() throws Exception {
        tearDown();
        Files.createDirectories(DIR);
        Files.write(FILE, "Hello, world".getBytes(), StandardOpenOption.CREATE);
        final FileTime newTime = FileTime.from(Instant.now().minus(2, ChronoUnit.DAYS));
        final BasicFileAttributeView attrs = Files.getFileAttributeView(FILE, BasicFileAttributeView.class);
        attrs.setTimes(newTime, newTime, newTime);
        /*
         * POSIX does not define a file creation timestamp.
         * Depending on the system `creationTime` might be:
         *  * 0,
         *  * the last modification time
         *  * or the time the file was actually created.
         *
         * This test fails if the latter occurs, since the file is created after the JVM.
         */
        final FileTime creationTime = attrs.readAttributes().creationTime();
        assumeTrue(creationTime.equals(newTime) || creationTime.toMillis() == 0L);
    }

    @AfterAll
    public static void tearDown() throws IOException {
        if (Files.exists(DIR)) {
            PathUtils.deleteDirectory(DIR);
        }
    }

    @Test
    public void testAppender(final LoggerContext loggerContext) throws Exception {
        final Logger logger = loggerContext.getLogger(RollingAppenderRestartTest.class.getName());
        logger.info("This is test message number 1");
        // The GZ compression takes place asynchronously.
        // Make sure it's done before validating.
        Thread.yield();
        final String name = "RollingFile";
        final RollingFileAppender appender = loggerContext.getConfiguration().getAppender(name);
        assertNotNull(appender, name);
        if (appender.getManager().getSemaphore().tryAcquire(5, TimeUnit.SECONDS)) {
            // If we are in here, either the rollover is done or has not taken place yet.
            validate();
        } else {
            fail("Rolling over is taking too long.");
        }
    }

    private void validate() {
        final Matcher<File[]> hasGzippedFile = hasItemInArray(that(hasName(that(endsWith(".gz")))));
        final File[] files = DIR.toFile().listFiles();
        Arrays.sort(files);
        assertTrue(
                hasGzippedFile.matches(files),
                "was expecting files with '.gz' suffix, found: " + Arrays.toString(files));
    }
}
