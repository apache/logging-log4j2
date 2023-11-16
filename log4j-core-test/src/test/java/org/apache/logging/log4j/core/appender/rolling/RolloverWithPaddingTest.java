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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.test.junit.TempLoggingDir;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Test;

/**
 * Tests that zero-padding in rolled files works correctly.
 */
@UsingStatusListener
public class RolloverWithPaddingTest {

    private static final String[] EXPECTED_FILES = {
        "rollingtest.log", "test-001.log", "test-002.log", "test-003.log", "test-004.log", "test-005.log"
    };
    private static final byte[] NOT_EMPTY_CONTENT = "Not empty".getBytes();

    @TempLoggingDir
    private Path loggingPath;

    @Test
    @LoggerContextSource
    public void testPadding(final LoggerContext context) throws Exception {
        final Logger logger = context.getLogger(getClass());
        for (int i = 0; i < 10; ++i) {
            // 30 chars per message: each message triggers a rollover
            logger.fatal("This is a test message number " + i); // 30 chars:
        }

        assertThat(loggingPath).isDirectory();
        final List<String> actual = sortedLogFiles(loggingPath);
        assertThat(actual).containsExactly(EXPECTED_FILES);
    }

    @Test
    @LoggerContextSource
    public void testOldFileDeleted(final LoggerContext context) throws Exception {
        final Logger logger = context.getLogger(getClass());
        // Prepare directory
        for (int i = 1; i <= 5; i++) {
            final Path file = loggingPath.resolve("test-00" + i + ".log");
            if (i == 1) {
                assertDoesNotThrow(() -> Files.deleteIfExists(file));
            } else {
                assertDoesNotThrow(() -> {
                    try (final OutputStream os = Files.newOutputStream(file)) {
                        os.write(NOT_EMPTY_CONTENT);
                    }
                });
            }
        }

        for (int i = 0; i < 10; ++i) {
            // 30 chars per message: each message triggers a rollover
            logger.fatal("This is a test message number " + i); // 30 chars:
        }
        final List<String> actual = sortedLogFiles(loggingPath);
        assertThat(actual).containsExactly(EXPECTED_FILES);
    }

    private static List<String> sortedLogFiles(final Path loggingPath) throws IOException {
        try (final DirectoryStream<Path> stream = Files.newDirectoryStream(loggingPath)) {
            return StreamSupport.stream(stream.spliterator(), false)
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .collect(Collectors.toList());
        }
    }
}
