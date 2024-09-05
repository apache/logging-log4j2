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

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.junit.TempLoggingDir;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * LOG4J2-1766.
 */
@UsingStatusListener // Disables status logger unless an error occurs
class RollingAppenderTempCompressedFilePatternTest {

    private static final Logger LOGGER = StatusLogger.getLogger();

    @TempLoggingDir
    private static Path loggingPath;

    @Test
    @DisabledOnOs(value = OS.MAC, disabledReason = "FileWatcher isn't fast enough to work properly.")
    @LoggerContextSource
    void testAppender(final LoggerContext context) throws Exception {
        final Logger logger = context.getLogger(getClass());
        final Path logsDir = loggingPath.resolve("logs");
        final Path tmpDir = loggingPath.resolve("tmp");
        Files.createDirectories(tmpDir);
        try (final WatchService watcher = FileSystems.getDefault().newWatchService()) {
            tmpDir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE);

            final List<String> messages = new ArrayList<>();
            for (int i = 0; i < 500; ++i) {
                final String message = "This is test message number " + i;
                messages.add(message);
                logger.debug(message);
            }
            if (!context.stop(30, TimeUnit.SECONDS)) {
                LOGGER.error(
                        "Could not stop logger context {} cleanly in {}.",
                        context.getName(),
                        getClass().getSimpleName());
            }

            AtomicInteger fileCount = new AtomicInteger();
            AtomicInteger gzippedFileCount = new AtomicInteger();
            try (final DirectoryStream<Path> files = Files.newDirectoryStream(logsDir)) {
                files.forEach(file -> assertDoesNotThrow(() -> {
                    fileCount.incrementAndGet();
                    final boolean isGzipped = file.getFileName().toString().endsWith(".gz");
                    if (isGzipped) {
                        gzippedFileCount.incrementAndGet();
                        try (InputStream fileInput = Files.newInputStream(file);
                                InputStream input = new GZIPInputStream(fileInput)) {
                            IOUtils.readLines(input, Charset.defaultCharset()).forEach(messages::remove);
                        }
                    } else {
                        try (Stream<String> lines = Files.lines(file, Charset.defaultCharset())) {
                            lines.forEach(messages::remove);
                        }
                    }
                }));
            }
            assertThat(messages).as("Lost messages").isEmpty();
            assertThat(fileCount).as("Log file file count").hasValueGreaterThan(16);
            assertThat(gzippedFileCount).as("Compressed log file count").hasValueGreaterThan(16);

            int temporaryFileCount = 0;
            WatchKey key = watcher.take();

            for (final WatchEvent<?> event : key.pollEvents()) {
                final Path filename = (Path) event.context();
                if (filename.toString().endsWith(".tmp")) {
                    temporaryFileCount++;
                }
            }
            assertThat(temporaryFileCount)
                    .as("Temporary file count")
                    .isGreaterThan(0)
                    .isEqualTo(gzippedFileCount.get());
        }
    }
}
