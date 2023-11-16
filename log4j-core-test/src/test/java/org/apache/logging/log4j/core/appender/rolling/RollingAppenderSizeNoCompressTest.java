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

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.junit.TempLoggingDir;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Test;

/**
 * LOG4J2-1804.
 */
@UsingStatusListener
public class RollingAppenderSizeNoCompressTest {

    private static final Logger LOGGER = StatusLogger.getLogger();

    @TempLoggingDir
    private static Path loggingPath;

    @Test
    @LoggerContextSource
    public void testAppender(final LoggerContext context) throws Exception {
        final Logger logger = context.getLogger(getClass());
        final List<String> messages = new ArrayList<>();
        for (int i = 0; i < 1000; ++i) {
            final String message = "This is test message number " + i;
            messages.add(message);
            logger.debug(message);
        }
        if (!context.stop(30, TimeUnit.SECONDS)) {
            LOGGER.error("Could not stop cleanly logger context {}.", context);
        }
        final List<Path> files = StreamSupport.stream(
                        Files.newDirectoryStream(loggingPath).spliterator(), false)
                .collect(Collectors.toList());
        for (final Path file : files) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Files.copy(file, baos);
            final String text = new String(baos.toByteArray(), Charset.defaultCharset());
            final String[] lines = text.split("[\\r\\n]+");
            for (final String line : lines) {
                messages.remove(line);
            }
        }
        assertThat(messages).as("Lost messages").isEmpty();
        assertThat(files).as("Log files").hasSizeGreaterThan(31);
    }
}
