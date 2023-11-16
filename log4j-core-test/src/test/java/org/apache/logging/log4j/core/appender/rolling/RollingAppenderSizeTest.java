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
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.test.junit.TempLoggingDir;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@UsingStatusListener
public class RollingAppenderSizeTest {

    private static long DEFAULT_SHUTDOWN_MS = 500;

    private static final Pattern MESSAGE_PATTERN = Pattern.compile("This is test message numer \\d+.");

    private static final List<String> FILE_EXTENSIONS =
            Arrays.asList("gz", "zip", "bz2", "deflate", "pack200", "xz", "zst");

    static Stream<Arguments> parameters() {
        return FILE_EXTENSIONS.stream().flatMap(fileExtension -> {
            return Stream.of(Arguments.of(fileExtension, true), Arguments.of(fileExtension, false));
        });
    }

    @TempLoggingDir
    private static Path loggingPath;

    private static RollingFileAppender createRollingFileAppender(
            final String fileExtension, final boolean createOnDemand) {
        final Path folder = loggingPath.resolve(fileExtension);
        final String fileName = folder.resolve("rollingtest.log").toString();
        final String filePattern =
                folder.resolve("rollingtest-%i.log." + fileExtension).toString();
        final RollingFileAppender appender = RollingFileAppender.newBuilder()
                .setName("RollingFile")
                .withFileName(fileName)
                .withFilePattern(filePattern)
                .setLayout(PatternLayout.createDefaultLayout())
                .withPolicy(SizeBasedTriggeringPolicy.createPolicy("500"))
                .withCreateOnDemand(createOnDemand)
                .build();
        appender.start();
        return appender;
    }

    private static LogEvent createEvent(final String pattern, final Object p0) {
        return Log4jLogEvent.newBuilder()
                .setMessage(new ParameterizedMessage(pattern, p0))
                .build();
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testIsCreateOnDemand(final String fileExtension, final boolean createOnDemand) throws IOException {
        final Path extensionFolder = loggingPath.resolve(fileExtension);
        RollingFileAppender appender = null;
        try {
            appender = createRollingFileAppender(fileExtension, createOnDemand);
            final RollingFileManager manager = appender.getManager();
            assertThat(manager).isNotNull().extracting("createOnDemand").isEqualTo(createOnDemand);

        } finally {
            appender.stop(DEFAULT_SHUTDOWN_MS, TimeUnit.MILLISECONDS);
            FileUtils.deleteDirectory(extensionFolder.toFile());
        }
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testAppender(final String fileExtension, final boolean createOnDemand) throws Exception {
        final Path extensionFolder = loggingPath.resolve(fileExtension);
        RollingFileAppender appender = null;
        try {
            appender = createRollingFileAppender(fileExtension, createOnDemand);
            final Path currentLog = extensionFolder.resolve("rollingtest.log");
            if (createOnDemand) {
                assertThat(currentLog).as("file created on demand").doesNotExist();
            }
            for (int i = 0; i < 500; ++i) {
                appender.append(createEvent("This is test message numer {}.", i));
            }
            appender.stop(DEFAULT_SHUTDOWN_MS, TimeUnit.MILLISECONDS);

            assertThat(extensionFolder).isDirectoryContaining("glob:**/*." + fileExtension);

            final FileExtension ext = FileExtension.lookup(fileExtension);
            if (ext == null || FileExtension.ZIP == ext || FileExtension.PACK200 == ext) {
                return; // Apache Commons Compress cannot deflate zip? TODO test decompressing these
                // formats
            }

            for (final Path file : Files.newDirectoryStream(extensionFolder)) {
                if (file.getFileName().endsWith(fileExtension)) {
                    try (final InputStream fis = Files.newInputStream(file);
                            final InputStream in = new CompressorStreamFactory()
                                    .createCompressorInputStream(toRootLowerCase(ext.name()), fis)) {
                        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        assertThat(in).as("compressed input stream").isNotNull();
                        assertDoesNotThrow(() -> IOUtils.copy(in, baos));
                        final String text = new String(baos.toByteArray(), Charset.defaultCharset());
                        final String[] lines = text.split("[\\r\\n]+");
                        assertThat(lines)
                                .allMatch(message ->
                                        MESSAGE_PATTERN.matcher(message).matches());
                    }
                }
            }
        } finally {
            if (appender.isStarted()) {
                appender.stop(DEFAULT_SHUTDOWN_MS, TimeUnit.MILLISECONDS);
            }
            FileUtils.deleteDirectory(extensionFolder.toFile());
        }
    }
}
