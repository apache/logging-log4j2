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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 *
 */
class RollingAppenderOnStartupDirectTest {

    private static final String SOURCE = "src/test/resources/__files";
    private static final String DIR = "target/onStartup";
    private static final String CONFIG = "log4j-rollOnStartupDirect.xml";
    private static final String FILENAME = "onStartup.log";
    private static final String PREFIX = "This is test message number ";
    private static final String ROLLED = "onStartup-";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    private static LoggerContext loggerContext;

    @BeforeAll
    static void beforeClass() throws Exception {
        if (Files.exists(Paths.get("target/onStartup"))) {
            try (final DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(DIR))) {
                for (final Path path : directoryStream) {
                    Files.delete(path);
                }
                Files.delete(Paths.get(DIR));
            }
        }
        Files.createDirectory(new File(DIR).toPath());
        final String fileName = ROLLED + formatter.format(LocalDate.now()) + "-1.log";
        final Path target = Paths.get(DIR, fileName);
        Files.copy(Paths.get(SOURCE, FILENAME), target, StandardCopyOption.COPY_ATTRIBUTES);
        final FileTime newTime = FileTime.from(Instant.now().minus(1, ChronoUnit.DAYS));
        Files.getFileAttributeView(target, BasicFileAttributeView.class).setTimes(newTime, newTime, newTime);
    }

    @Test
    void performTest() throws Exception {
        loggerContext = Configurator.initialize("Test", CONFIG);
        final Logger logger = loggerContext.getLogger(RollingAppenderOnStartupDirectTest.class);
        for (int i = 3; i < 10; ++i) {
            logger.debug(PREFIX + i);
        }
        int fileCount = 0;
        try (final DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(DIR))) {
            for (final Path path : directoryStream) {
                ++fileCount;
                if (path.toFile().getName().startsWith(ROLLED)) {
                    final List<String> lines = Files.readAllLines(path);
                    assertFalse(
                            lines.isEmpty(), "No messages in " + path.toFile().getName());
                    assertTrue(
                            lines.get(0).startsWith(PREFIX),
                            "Missing message for " + path.toFile().getName());
                }
            }
        }
        assertEquals(2, fileCount, "File did not roll");
    }

    @AfterAll
    static void afterClass() throws Exception {
        Configurator.shutdown(loggerContext);
        try (final DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(DIR))) {
            for (final Path path : directoryStream) {
                Files.delete(path);
            }
        }
        Files.delete(Paths.get(DIR));
    }
}
