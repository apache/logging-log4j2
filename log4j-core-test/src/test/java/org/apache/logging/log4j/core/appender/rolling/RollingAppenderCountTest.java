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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

/**
 * Validate rolling with a file pattern that contains leading zeros for the increment.
 */
@Ignore("https://issues.apache.org/jira/browse/LOG4J2-3522")
public class RollingAppenderCountTest {

    private static final String SOURCE = "src/test/resources/__files";
    private static final String DIR = "target/rolling_count";
    private static final String CONFIG = "log4j-rolling-count.xml";
    private static final String FILENAME = "onStartup.log";
    private static final String TARGET = "rolling_test.log.";

    private Logger logger;

    @Rule
    public LoggerContextRule loggerContextRule;

    public RollingAppenderCountTest() {
        this.loggerContextRule = LoggerContextRule.createShutdownTimeoutLoggerContextRule(CONFIG);
    }

    @Before
    public void setUp() throws Exception {
        this.logger = this.loggerContextRule.getLogger("LogTest");
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        if (Files.exists(Paths.get(DIR))) {
            try (final DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(DIR))) {
                for (final Path path : directoryStream) {
                    Files.delete(path);
                }
                Files.delete(Paths.get(DIR));
            }
        }
        final File dir = new File(DIR);
        if (!dir.exists()) {
            Files.createDirectory(new File(DIR).toPath());
        }
        final Path target = Paths.get(DIR, TARGET + System.currentTimeMillis());
        Files.copy(Paths.get(SOURCE, FILENAME), target, StandardCopyOption.COPY_ATTRIBUTES);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        final int count = Objects.requireNonNull(new File(DIR).listFiles()).length;
        assertEquals("Expected 17 files, got " + count, 17, count);
        try (final DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(DIR))) {
            for (final Path path : directoryStream) {
                Files.delete(path);
            }
        }
        Files.delete(Paths.get(DIR));
    }

    @Test
    public void testLog() throws Exception {
        for (long i = 0; i < 60; ++i) {
            logger.info("Sequence: " + i);
            logger.debug(RandomStringUtils.randomAscii(128, 512));
            Thread.sleep(250);
        }
    }
}
