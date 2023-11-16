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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.test.junit.TempLoggingDir;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Test;

@UsingStatusListener
public class RollingAppenderDirectWriteTest {

    private final Pattern FILE_PATTERN = Pattern.compile("test-\\d{4}-\\d{2}-\\d{2}T\\d{2}-\\d{2}-\\d+\\.log(\\.gz)?");
    private final Pattern LINE_PATTERN = Pattern.compile("This is test message number \\d+\\.");

    @TempLoggingDir
    private Path loggingPath;

    @Test
    @LoggerContextSource
    public void testAppender(final LoggerContext ctx) throws Exception {
        final Logger logger = ctx.getLogger(getClass());
        final int count = 100;
        for (int i = 0; i < count; ++i) {
            logger.debug("This is test message number {}.", i);
        }
        ctx.stop(500, TimeUnit.MILLISECONDS);
        int found = 0;
        try (final DirectoryStream<Path> stream = Files.newDirectoryStream(loggingPath)) {
            for (final Path file : stream) {
                final String fileName = file.getFileName().toString();
                assertThat(fileName).matches(FILE_PATTERN);
                try (final InputStream is = Files.newInputStream(file);
                        final InputStream uncompressed = fileName.endsWith(".gz") ? new GZIPInputStream(is) : is;
                        final BufferedReader reader = new BufferedReader(new InputStreamReader(uncompressed, UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        assertThat(line).matches(LINE_PATTERN);
                        ++found;
                    }
                }
            }
        }

        assertThat(found).as("Number of events.").isEqualTo(count);
    }
}
