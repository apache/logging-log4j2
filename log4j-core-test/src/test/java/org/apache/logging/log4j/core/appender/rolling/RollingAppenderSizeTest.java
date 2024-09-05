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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.test.junit.TempLoggingDir;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@UsingStatusListener
@NullMarked
class RollingAppenderSizeTest {

    private static final long DEFAULT_SHUTDOWN_MS = 500;

    private static final String MESSAGE = "This is test message number {}.";
    private static final Pattern MESSAGE_PATTERN = Pattern.compile("This is test message number \\d+.");

    @TempLoggingDir
    private static Path loggingPath;

    private static RollingFileAppender createRollingFileAppender(
            final Path perTestFolder, final boolean createOnDemand) {
        final String fileName = perTestFolder.resolve("rollingtest.log").toString();
        final String filePattern =
                perTestFolder.resolve("rollingtest-%i.log.gz").toString();
        final RollingFileAppender appender = RollingFileAppender.newBuilder()
                .setName("RollingFile")
                .setFileName(fileName)
                .setFilePattern(filePattern)
                .setLayout(PatternLayout.createDefaultLayout())
                .setPolicy(SizeBasedTriggeringPolicy.createPolicy("500"))
                .setCreateOnDemand(createOnDemand)
                .build();
        appender.start();
        return appender;
    }

    private static LogEvent createEvent(final Integer number) {
        return Log4jLogEvent.newBuilder()
                .setMessage(new ParameterizedMessage(MESSAGE, number))
                .build();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testIsCreateOnDemand(final boolean createOnDemand) {
        Path perTestFolder = loggingPath.resolve(Boolean.toString(createOnDemand));
        RollingFileAppender appender = null;
        try {
            appender = createRollingFileAppender(perTestFolder, createOnDemand);
            final RollingFileManager manager = appender.getManager();
            assertThat(manager).isNotNull().extracting("createOnDemand").isEqualTo(createOnDemand);

        } finally {
            if (appender != null && appender.isStarted()) {
                appender.stop(DEFAULT_SHUTDOWN_MS, TimeUnit.MILLISECONDS);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testAppender(final boolean createOnDemand) throws IOException {
        Path perTestFolder = loggingPath.resolve(Boolean.toString(createOnDemand));
        RollingFileAppender appender = null;
        try {
            appender = createRollingFileAppender(perTestFolder, createOnDemand);
            final Path currentLog = perTestFolder.resolve("rollingtest.log");
            if (createOnDemand) {
                assertThat(currentLog).as("file created on demand").doesNotExist();
            }
            for (int i = 0; i < 500; ++i) {
                appender.append(createEvent(i));
            }
            appender.stop(DEFAULT_SHUTDOWN_MS, TimeUnit.MILLISECONDS);

            assertThat(perTestFolder).isDirectoryContaining("glob:**/*.gz");

            try (DirectoryStream<Path> files = Files.newDirectoryStream(perTestFolder)) {
                assertThat(files).allSatisfy(file -> {
                    if (file.getFileName().endsWith(".gz")) {
                        try (final InputStream fileInput = Files.newInputStream(file);
                                final InputStream input = new GZIPInputStream(fileInput)) {
                            List<String> lines = IOUtils.readLines(input, Charset.defaultCharset());
                            assertThat(lines)
                                    .allMatch(m -> MESSAGE_PATTERN.matcher(m).matches());
                        }
                    }
                });
            }
        } finally {
            if (appender != null && appender.isStarted()) {
                appender.stop(DEFAULT_SHUTDOWN_MS, TimeUnit.MILLISECONDS);
            }
        }
    }
}
