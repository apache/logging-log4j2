/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.core.appender.rolling;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusListener;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.junit.CleanUpDirectories;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 */
public class RollingAppenderDirectWrite1906Test implements RolloverListener {

    private static final String CONFIG = "log4j-rolling-direct-1906.xml";

    private static final String DIR = "target/rolling-direct-1906";
    private final CountDownLatch rollover = new CountDownLatch(2);

    @BeforeAll
    static void beforeAll() {
        StatusLogger.getLogger().registerListener(new NoopStatusListener());
    }

    @Test
    @CleanUpDirectories(DIR)
    @LoggerContextSource(value = CONFIG, timeout = 10)
    public void testAppender(final LoggerContext context, @Named("RollingFile") final RollingFileManager manager) throws Exception {
        manager.addRolloverListener(this);
        final var logger = context.getLogger(getClass());
        int count = 100;
        for (int i = 0; i < count; ++i) {
            logger.debug("This is test message number " + i);
            Thread.sleep(50);
        }
        rollover.await();
        final Path dir = Path.of(DIR);
        assertThat(dir).isNotEmptyDirectory();
        assertThat(dir).isDirectoryContaining("glob:**.log");

        try (final Stream<Path> files = Files.list(dir)) {
            final AtomicInteger found = new AtomicInteger();
            assertThat(files).allSatisfy(file -> {
                final String expected = file.getFileName().toString();
                try (final Stream<String> stream = Files.lines(file)) {
                    final List<String> lines = stream
                            .map(line -> String.format("rollingfile.%s.log", line.substring(0, line.indexOf(' '))))
                            .collect(Collectors.toList());
                    found.addAndGet(lines.size());
                    assertThat(lines).allSatisfy(actual -> assertThat(actual).isEqualTo(expected));
                }
            });
            assertEquals(count, found.get(), "Incorrect number of events read. Expected " + count + ", Actual " + found.get());
        }
    }

    @Override
    public void rolloverTriggered(String fileName) {

    }

    @Override
    public void rolloverComplete(final String fileName) {
        rollover.countDown();
    }

    private static class NoopStatusListener implements StatusListener {
        @Override
        public void log(StatusData data) {

        }

        @Override
        public Level getStatusLevel() {
            return Level.TRACE;
        }

        @Override
        public void close() throws IOException {

        }
    }
}
