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
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.hamcrest.Matcher;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.apache.logging.log4j.hamcrest.Descriptors.that;
import static org.apache.logging.log4j.hamcrest.FileMatchers.hasName;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class RollingAppenderRestartTest {

    private static final String CONFIG = "log4j-rolling-restart.xml";
    private static final String DIR = "target/rolling-restart";

    private final LoggerContextRule loggerContextRule = LoggerContextRule.createShutdownTimeoutLoggerContextRule(CONFIG);

    @Rule
    public RuleChain chain = loggerContextRule.withCleanFoldersRule(false, true, 5, DIR);

    @BeforeClass
    public static void setup() throws Exception {
        File file = new File("target/rolling-restart/test.log");
        Files.createDirectories(file.toPath().getParent());
        Files.write(file.toPath(), "Hello, world".getBytes(), StandardOpenOption.CREATE);
        FileTime newTime = FileTime.from(Instant.now().minus(2, ChronoUnit.DAYS));
        Files.getFileAttributeView(file.toPath(), BasicFileAttributeView.class).setTimes(newTime, newTime, newTime);
    }

    @Test
    public void testAppender() throws Exception {
        final Logger logger = loggerContextRule.getLogger();
        logger.info("This is test message number 1");

        final File dir = new File(DIR);

        final Matcher<File[]> hasGzippedFile = hasItemInArray(that(hasName(that(endsWith(".gz")))));
        final File[] files = dir.listFiles();
        assertTrue("No gzipped files found", hasGzippedFile.matches(files));
    }
}
