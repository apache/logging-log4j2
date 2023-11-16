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

import static org.apache.logging.log4j.util.Strings.toRootLowerCase;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
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
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
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
@UsingStatusListener
public class RollingAppenderTempCompressedFilePatternTest {

    private static Logger LOGGER = StatusLogger.getLogger();

    @TempLoggingDir
    private static Path loggingPath;

    @Test
    @DisabledOnOs(value = OS.MAC, disabledReason = "FileWatcher isn't fast enough to work properly.")
    @LoggerContextSource
    public void testAppender(final LoggerContext context) throws Exception {
        final Logger logger = context.getLogger(getClass());
        final Path logsDir = loggingPath.resolve("logs");
        final Path tmpDir = loggingPath.resolve("tmp");
        Files.createDirectories(tmpDir);
        try (final WatchService watcher = FileSystems.getDefault().newWatchService()) {
            WatchKey key = tmpDir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE);

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

            int gzippedFiles = 0;
            final List<Path> files = StreamSupport.stream(
                            Files.newDirectoryStream(logsDir).spliterator(), false)
                    .collect(Collectors.toList());
            for (final Path file : files) {
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                final FileExtension ext =
                        FileExtension.lookupForFile(file.getFileName().toString());
                if (ext != null) {
                    gzippedFiles++;
                }
                try (final InputStream fis = Files.newInputStream(file);
                        final InputStream in = ext != null
                                ? new CompressorStreamFactory()
                                        .createCompressorInputStream(toRootLowerCase(ext.name()), fis)
                                : fis) {
                    assertThat(in).as("compressed input stream").isNotNull();
                    assertDoesNotThrow(() -> IOUtils.copy(in, baos));
                }
                final String text = new String(baos.toByteArray(), Charset.defaultCharset());
                final String[] lines = text.split("[\\r\\n]+");
                for (final String line : lines) {
                    messages.remove(line);
                }
            }
            assertThat(messages).as("Lost messages").isEmpty();
            assertThat(files).as("Log files").hasSizeGreaterThan(16);
            assertThat(gzippedFiles).as("Compressed log file count").isGreaterThan(16);

            int temporaryFilesCreated = 0;
            key = watcher.take();

            for (final WatchEvent<?> event : key.pollEvents()) {
                final WatchEvent<Path> ev = (WatchEvent<Path>) event;
                final Path filename = ev.context();
                if (filename.toString().endsWith(".tmp")) {
                    temporaryFilesCreated++;
                }
            }
            assertThat(temporaryFilesCreated)
                    .as("Temporary files created")
                    .isGreaterThan(0)
                    .isEqualTo(gzippedFiles);
        }
    }
}
