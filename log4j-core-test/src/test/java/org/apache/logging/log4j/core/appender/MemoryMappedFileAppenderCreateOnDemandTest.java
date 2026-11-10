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
package org.apache.logging.log4j.core.appender;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests the {@code createOnDemand} attribute of {@link MemoryMappedFileAppender}.
 *
 * @see <a href="https://github.com/apache/logging-log4j2/issues/3083">#3083</a>
 */
class MemoryMappedFileAppenderCreateOnDemandTest {

    private static final String MESSAGE = "Hello Log4j";
    private static final String OLD_CONTENT = "Old content, which is longer than the new message.";

    private static MemoryMappedFileAppender createAppender(
            final Path logFile, final boolean append, final boolean createOnDemand) {
        final MemoryMappedFileAppender appender = MemoryMappedFileAppender.newBuilder()
                .setName("MemoryMappedFileAppenderCreateOnDemandTest")
                .setFileName(logFile.toString())
                .setAppend(append)
                .setCreateOnDemand(createOnDemand)
                .setLayout(PatternLayout.newBuilder().withPattern("%m").build())
                .setConfiguration(new NullConfiguration())
                .build();
        assertThat(appender).isNotNull();
        appender.start();
        return appender;
    }

    private static LogEvent createEvent() {
        return Log4jLogEvent.newBuilder()
                .setLoggerName("test")
                .setLevel(Level.INFO)
                .setMessage(new SimpleMessage(MESSAGE))
                .build();
    }

    private static String contentOf(final Path file) throws Exception {
        return new String(Files.readAllBytes(file), UTF_8);
    }

    @Test
    void fileIsNotCreatedBeforeFirstEvent(@TempDir final Path tempDir) throws Exception {
        final Path logDir = tempDir.resolve("onDemand");
        final Path logFile = logDir.resolve("test.log");

        final MemoryMappedFileAppender appender = createAppender(logFile, true, true);
        try {
            assertThat(logDir).doesNotExist();
            assertThat(logFile).doesNotExist();

            appender.append(createEvent());
            assertThat(logFile).exists();
        } finally {
            appender.stop();
        }

        assertThat(contentOf(logFile)).isEqualTo(MESSAGE);
    }

    @Test
    void fileIsNotCreatedWithoutEvents(@TempDir final Path tempDir) {
        final Path logDir = tempDir.resolve("noEvents");
        final Path logFile = logDir.resolve("test.log");

        final MemoryMappedFileAppender appender = createAppender(logFile, true, true);
        appender.stop();

        assertThat(logDir).doesNotExist();
        assertThat(logFile).doesNotExist();
    }

    @Test
    void existingFileIsAppendedTo(@TempDir final Path tempDir) throws Exception {
        final Path logFile = tempDir.resolve("append.log");
        Files.write(logFile, OLD_CONTENT.getBytes(UTF_8));

        final MemoryMappedFileAppender appender = createAppender(logFile, true, true);
        try {
            appender.append(createEvent());
        } finally {
            appender.stop();
        }

        assertThat(contentOf(logFile)).isEqualTo(OLD_CONTENT + MESSAGE);
    }

    @Test
    void withoutAppendExistingFileIsKeptUntilFirstEvent(@TempDir final Path tempDir) throws Exception {
        final Path logFile = tempDir.resolve("truncate.log");
        Files.write(logFile, OLD_CONTENT.getBytes(UTF_8));

        final MemoryMappedFileAppender appender = createAppender(logFile, false, true);
        try {
            assertThat(contentOf(logFile)).isEqualTo(OLD_CONTENT);

            appender.append(createEvent());
        } finally {
            appender.stop();
        }

        assertThat(contentOf(logFile)).isEqualTo(MESSAGE);
    }

    @Test
    void withoutAppendAndEventsExistingFileIsKept(@TempDir final Path tempDir) throws Exception {
        final Path logFile = tempDir.resolve("untouched.log");
        Files.write(logFile, OLD_CONTENT.getBytes(UTF_8));

        final MemoryMappedFileAppender appender = createAppender(logFile, false, true);
        appender.stop();

        assertThat(contentOf(logFile)).isEqualTo(OLD_CONTENT);
    }

    @Test
    void fileIsCreatedEagerlyByDefault(@TempDir final Path tempDir) {
        final Path logFile = tempDir.resolve("eager").resolve("test.log");

        final MemoryMappedFileAppender appender = createAppender(logFile, true, false);
        try {
            assertThat(logFile).exists();
        } finally {
            appender.stop();
        }
    }
}
