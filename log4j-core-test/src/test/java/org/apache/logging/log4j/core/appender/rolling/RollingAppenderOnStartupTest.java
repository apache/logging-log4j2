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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.apache.commons.io.file.PathUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 */
public class RollingAppenderOnStartupTest {

    private static final Path SOURCE = Path.of("src", "test", "resources", "__files");
    private static final Path DIR = Path.of("target", "onStartup");
    private static final String FILENAME = "onStartup.log";
    private static final String PREFIX = "This is test message number ";
    private static final String ROLLED = "onStartup-";

    @BeforeAll
    static void setUp() throws IOException {
        if (Files.isDirectory(DIR)) {
            PathUtils.deleteDirectory(DIR);
        }
        Files.createDirectory(DIR);
        final Path target = Files.copy(SOURCE.resolve(FILENAME), DIR.resolve(FILENAME), StandardCopyOption.COPY_ATTRIBUTES);
        final FileTime newTime = FileTime.from(Instant.now().minus(1, ChronoUnit.DAYS));
        Files.getFileAttributeView(target, BasicFileAttributeView.class).setTimes(newTime, newTime, newTime);
    }

    @AfterAll
    static void tearDown() throws IOException {
        PathUtils.deleteDirectory(DIR);
    }

    @Test
    @LoggerContextSource("log4j-rollOnStartup.xml")
    public void performTest(final LoggerContext loggerContext) throws Exception {
        boolean rolled = false;
        final Logger logger = loggerContext.getLogger(RollingAppenderOnStartupTest.class);
        for (int i = 3; i < 10; ++i) {
            logger.debug(PREFIX + i);
        }
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(DIR)) {
            for (final Path path : directoryStream) {
                if (path.toFile().getName().startsWith(ROLLED)) {
                    rolled = true;
                    List<String> lines = Files.readAllLines(path);
                    assertTrue(lines.size() > 0, "No messages in " + path.toFile().getName());
                    assertTrue(lines.get(0).startsWith(PREFIX + "1"), "Missing message for " + path.toFile().getName());
                }
            }
        }
        assertTrue(rolled, "File did not roll");
    }
}
