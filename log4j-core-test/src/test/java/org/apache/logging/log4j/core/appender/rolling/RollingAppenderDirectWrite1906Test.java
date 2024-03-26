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
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.test.junit.TempLoggingDir;
import org.junit.jupiter.api.Test;

class RollingAppenderDirectWrite1906Test implements RolloverListener {

    private final CountDownLatch rollover = new CountDownLatch(2);

    @TempLoggingDir
    private static Path loggingPath;

    @Test
    @LoggerContextSource(timeout = 10)
    void testAppender(final LoggerContext context, @Named("RollingFile") final RollingFileManager manager)
            throws Exception {
        manager.addRolloverListener(this);
        final var logger = context.getLogger(getClass());
        final int count = 100;
        for (int i = 0; i < count; ++i) {
            logger.debug("This is test message number " + i);
            Thread.sleep(50);
        }
        rollover.await();
        assertThat(loggingPath).isNotEmptyDirectory();
        assertThat(loggingPath).isDirectoryContaining("glob:**.log");

        try (final Stream<Path> files = Files.list(loggingPath)) {
            final AtomicInteger found = new AtomicInteger();
            assertThat(files).allSatisfy(file -> {
                final String expected = file.getFileName().toString();
                try (final Stream<String> stream = Files.lines(file)) {
                    final List<String> lines = stream.map(
                                    line -> String.format("rollingfile.%s.log", line.substring(0, line.indexOf(' '))))
                            .collect(Collectors.toList());
                    found.addAndGet(lines.size());
                    assertThat(lines).allSatisfy(actual -> assertThat(actual).isEqualTo(expected));
                }
            });
            assertEquals(
                    count,
                    found.get(),
                    "Incorrect number of events read. Expected " + count + ", Actual " + found.get());
        }
    }

    @Override
    public void rolloverTriggered(final String fileName) {}

    @Override
    public void rolloverComplete(final String fileName) {
        rollover.countDown();
    }
}
