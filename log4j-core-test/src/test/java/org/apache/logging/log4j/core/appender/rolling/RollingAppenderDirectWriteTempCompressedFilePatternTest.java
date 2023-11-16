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

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.test.junit.TempLoggingDir;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * LOG4J2-1766.
 */
@UsingStatusListener
@DisabledOnOs(value = OS.MAC, disabledReason = "FileWatcher is not fast enough on macOS for this test")
public class RollingAppenderDirectWriteTempCompressedFilePatternTest {

    private final String PATTERN = "test-\\d{4}-\\d{2}-\\d{2}T\\d{2}-\\d{2}-\\d+\\.log\\.gz";
    private final Pattern FILE_PATTERN = Pattern.compile(PATTERN);
    private final Pattern TMP_PATTERN = Pattern.compile(PATTERN + "\\.tmp");

    @TempLoggingDir
    private Path loggingPath;

    @Test
    @LoggerContextSource
    public void testAppender(final LoggerContext ctx) throws Exception {
        final Logger logger = ctx.getLogger(getClass());
        try (final WatchService watcher = FileSystems.getDefault().newWatchService()) {
            WatchKey key = loggingPath.register(watcher, StandardWatchEventKinds.ENTRY_CREATE);

            for (int i = 0; i < 100; ++i) {
                logger.debug("This is test message number {}.", i);
            }
            ctx.stop(500, TimeUnit.MILLISECONDS);

            int temporaryFilesCreated = 0;
            int compressedFiles = 0;
            key = watcher.take();

            for (final WatchEvent<?> event : key.pollEvents()) {
                final WatchEvent<Path> ev = (WatchEvent<Path>) event;
                final String filename = ev.context().getFileName().toString();
                if (TMP_PATTERN.matcher(filename).matches()) {
                    temporaryFilesCreated++;
                }
                if (FILE_PATTERN.matcher(filename).matches()) {
                    compressedFiles++;
                }
            }
            assertThat(temporaryFilesCreated)
                    .as("Temporary files created.")
                    .isGreaterThan(0)
                    .isEqualTo(compressedFiles);
        }
    }
}
