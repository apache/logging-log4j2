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
package org.apache.logging.log4j.core.layout;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Test for LOG4J2-1769, kind of.
 *
 * @since 2.8
 */
@Tag("concurrency")
public class ConcurrentLoggingWithGelfLayoutTest {
    private static final Path PATH = Paths.get("target", "test-gelf-layout.log");

    @AfterAll
    static void after() throws IOException {
        // on Windows, this will need to happen after the LoggerContext is stopped
        Files.deleteIfExists(PATH);
    }

    @Test
    @LoggerContextSource("log4j2-gelf-layout.xml")
    public void testConcurrentLogging(final LoggerContext context) throws Throwable {
        final Logger log = context.getLogger(ConcurrentLoggingWithGelfLayoutTest.class);
        final int threadCount = Runtime.getRuntime().availableProcessors() * 2;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final List<Throwable> thrown = Collections.synchronizedList(new ArrayList<>());

        for (int x = 0; x < threadCount; x++) {
            final Thread t = new Thread(() -> {
                log.info(latch.getCount());
                try {
                    for (int i = 0; i < 64; i++) {
                        log.info("First message.");
                        log.info("Second message.");
                    }
                } finally {
                    latch.countDown();
                }
            });

            // Appender is configured with ignoreExceptions="false";
            // any exceptions are propagated to the caller, so we can catch them here.
            t.setUncaughtExceptionHandler((t1, e) -> thrown.add(e));
            t.start();
        }

        latch.await();

        // if any error occurred, fail this test
        if (!thrown.isEmpty()) {
            throw thrown.get(0);
        }

        // simple test to ensure content is not corrupted
        if (Files.exists(PATH)) {
            try (final Stream<String> lines = Files.lines(PATH, Charset.defaultCharset())) {
                lines.forEach(line -> assertThat(
                        line,
                        both(startsWith("{\"version\":\"1.1\",\"host\":\"myself\",\"timestamp\":"))
                                .and(endsWith("\"}"))));
            }
        }
    }
}
