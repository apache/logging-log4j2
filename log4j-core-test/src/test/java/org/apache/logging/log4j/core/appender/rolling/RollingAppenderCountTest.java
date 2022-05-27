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

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.plugins.Named;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Validate rolling with a file pattern that contains leading zeros for the increment.
 */
@Tag("sleepy")
@Disabled("https://issues.apache.org/jira/browse/LOG4J2-3522")
public class RollingAppenderCountTest {

    private static final String SOURCE = "src/test/resources/__files";
    private static final String DIR = "target/rolling_count";
    private static final String CONFIG = "log4j-rolling-count.xml";
    private static final String FILENAME = "onStartup.log";
    private static final String TARGET = "rolling_test.log.";

    @BeforeAll
    public static void beforeClass() throws Exception {
        Path dir = Path.of(DIR);
        if (Files.exists(dir)) {
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dir)) {
                for (final Path path : directoryStream) {
                    Files.delete(path);
                }
                Files.delete(dir);
            }
        }
        if (Files.notExists(dir)) {
            Files.createDirectory(dir);
        }
        Path target = dir.resolve(TARGET + System.currentTimeMillis());
        Files.copy(Paths.get(SOURCE, FILENAME), target, StandardCopyOption.COPY_ATTRIBUTES);
    }

    @AfterAll
    public static void afterClass() throws Exception {
        int count = Objects.requireNonNull(new File(DIR).listFiles()).length;
        assertEquals(17, count, "Expected 16 files, got " + count);
        Path dir = Path.of(DIR);
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dir)) {
            for (final Path path : directoryStream) {
                Files.delete(path);
            }
        }
        Files.delete(dir);
    }

    @Test
    @LoggerContextSource(value = CONFIG, timeout = 10)
    public void testLog(final @Named("TestLog") Logger logger) throws Exception {
        for (long i = 0; i < 60; ++i) {
            logger.info("Sequence: " + i);
            logger.debug(RandomStringUtils.randomAscii(128, 512));
            Thread.sleep(250);
        }
    }
}
