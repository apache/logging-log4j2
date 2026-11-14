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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.RollingRandomAccessFileAppender;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.FileUtils;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.test.ListStatusListener;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@UsingStatusListener
class RollingRandomAccessFileAppenderCreateOnDemandTest {

    private static final String MESSAGE = "Hello Log4j";
    private static final String OLD_CONTENT = "Old content, which is longer than the new message.";

    private static RollingRandomAccessFileAppender.Builder<?> newBuilder(
            final Path logFile, final boolean append, final boolean createOnDemand) {
        return RollingRandomAccessFileAppender.newBuilder()
                .setName("RollingRandomAccessFileAppenderCreateOnDemandTest")
                .setFileName(logFile.toString())
                .setFilePattern(logFile + ".%d{yyyy-MM-dd}")
                .setPolicy(NoOpTriggeringPolicy.INSTANCE)
                .setAppend(append)
                .setCreateOnDemand(createOnDemand)
                .setLayout(PatternLayout.newBuilder().withPattern("%m").build())
                .setConfiguration(new NullConfiguration());
    }

    private static RollingRandomAccessFileAppender createAppender(
            final Path logFile, final boolean append, final boolean createOnDemand) {
        final RollingRandomAccessFileAppender appender =
                newBuilder(logFile, append, createOnDemand).build();
        assertThat(appender).isNotNull();
        appender.start();
        return appender;
    }

    private static LogEvent createEvent(final String message) {
        return Log4jLogEvent.newBuilder()
                .setLoggerName("test")
                .setLevel(Level.INFO)
                .setMessage(new SimpleMessage(message))
                // the appender flushes the manager at the end of a batch
                .setEndOfBatch(true)
                .build();
    }

    private static String contentOf(final Path file) throws Exception {
        return new String(Files.readAllBytes(file), UTF_8);
    }

    private static void assertNoStatusWarnings(final ListStatusListener listener) {
        assertThat(listener.findStatusData(Level.WARN))
                .extracting(data -> data.getMessage().getFormattedMessage())
                .isEmpty();
    }

    @Test
    void fileIsNotCreatedBeforeFirstEvent(@TempDir final Path tempDir, final ListStatusListener listener)
            throws Exception {
        final Path logDir = tempDir.resolve("onDemand");
        final Path logFile = logDir.resolve("test.log");

        final RollingRandomAccessFileAppender appender = createAppender(logFile, true, true);
        try {
            assertThat(logDir).doesNotExist();
            assertThat(logFile).doesNotExist();

            appender.append(createEvent(MESSAGE));
            assertThat(logFile).exists();
        } finally {
            appender.stop();
        }

        assertThat(contentOf(logFile)).isEqualTo(MESSAGE);
        assertNoStatusWarnings(listener);
    }

    @Test
    void fileIsNotCreatedWithoutEvents(@TempDir final Path tempDir, final ListStatusListener listener) {
        final Path logDir = tempDir.resolve("noEvents");
        final Path logFile = logDir.resolve("test.log");

        final RollingRandomAccessFileAppender appender = createAppender(logFile, true, true);
        appender.stop();

        assertThat(logDir).doesNotExist();
        assertThat(logFile).doesNotExist();
        assertNoStatusWarnings(listener);
    }

    @Test
    void fileIsNotCreatedByEmptyEvent(@TempDir final Path tempDir, final ListStatusListener listener) {
        final Path logDir = tempDir.resolve("emptyEvent");
        final Path logFile = logDir.resolve("test.log");

        final RollingRandomAccessFileAppender appender = createAppender(logFile, true, true);
        try {
            // Nothing is written, but the manager is flushed.
            appender.append(createEvent(""));
        } finally {
            appender.stop();
        }

        assertThat(logDir).doesNotExist();
        assertThat(logFile).doesNotExist();
        assertNoStatusWarnings(listener);
    }

    @Test
    void existingFileIsAppendedTo(@TempDir final Path tempDir, final ListStatusListener listener) throws Exception {
        final Path logFile = tempDir.resolve("append.log");
        Files.write(logFile, OLD_CONTENT.getBytes(UTF_8));

        final RollingRandomAccessFileAppender appender = createAppender(logFile, true, true);
        try {
            appender.append(createEvent(MESSAGE));
        } finally {
            appender.stop();
        }

        assertThat(contentOf(logFile)).isEqualTo(OLD_CONTENT + MESSAGE);
        assertNoStatusWarnings(listener);
    }

    @Test
    void withoutAppendExistingFileIsKeptUntilFirstEvent(@TempDir final Path tempDir, final ListStatusListener listener)
            throws Exception {
        final Path logFile = tempDir.resolve("truncate.log");
        Files.write(logFile, OLD_CONTENT.getBytes(UTF_8));

        final RollingRandomAccessFileAppender appender = createAppender(logFile, false, true);
        try {
            assertThat(contentOf(logFile)).isEqualTo(OLD_CONTENT);

            appender.append(createEvent(MESSAGE));
        } finally {
            appender.stop();
        }

        assertThat(contentOf(logFile)).isEqualTo(MESSAGE);
        assertNoStatusWarnings(listener);
    }

    @Test
    void withoutAppendAndEventsExistingFileIsKept(@TempDir final Path tempDir, final ListStatusListener listener)
            throws Exception {
        final Path logFile = tempDir.resolve("untouched.log");
        Files.write(logFile, OLD_CONTENT.getBytes(UTF_8));

        final RollingRandomAccessFileAppender appender = createAppender(logFile, false, true);
        appender.stop();

        assertThat(contentOf(logFile)).isEqualTo(OLD_CONTENT);
        assertNoStatusWarnings(listener);
    }

    @Test
    void filePermissionsAreAppliedOnFirstEvent(@TempDir final Path tempDir, final ListStatusListener listener)
            throws Exception {
        assumeTrue(FileUtils.isFilePosixAttributeViewSupported());
        // The parent directory exists, so only `createOnDemand` prevents the creation of the file.
        final Path logFile = tempDir.resolve("permissions.log");

        final RollingRandomAccessFileAppender appender =
                newBuilder(logFile, true, true).setFilePermissions("rw-r-----").build();
        assertThat(appender).isNotNull();
        appender.start();
        try {
            assertThat(logFile).doesNotExist();

            appender.append(createEvent(MESSAGE));
        } finally {
            appender.stop();
        }

        assertThat(contentOf(logFile)).isEqualTo(MESSAGE);
        assertThat(PosixFilePermissions.toString(Files.getPosixFilePermissions(logFile)))
                .isEqualTo("rw-r-----");
        assertNoStatusWarnings(listener);
    }

    @Test
    void fileIsCreatedEagerlyByDefault(@TempDir final Path tempDir, final ListStatusListener listener) {
        final Path logFile = tempDir.resolve("eager").resolve("test.log");

        final RollingRandomAccessFileAppender appender = createAppender(logFile, true, false);
        try {
            assertThat(logFile).exists();
        } finally {
            appender.stop();
        }
        assertNoStatusWarnings(listener);
    }
}
