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
package org.apache.logging.log4j.core.layout;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.junit.LoggerContextSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.*;

/**
 * Test for LOG4J2-1769, kind of.
 *
 * @since 2.8
 */
@Tag("concurrency")
@LoggerContextSource("log4j2-gelf-layout.xml")
public class ConcurrentLoggingWithGelfLayoutTest {
    private static final Path PATH = Paths.get("target", "test-gelf-layout.log");

    @AfterAll
    static void after() throws IOException {
        Files.deleteIfExists(PATH);
    }

    @Test
    public void testConcurrentLogging(final LoggerContext context) throws Throwable {
        final Logger log = context.getLogger(ConcurrentLoggingWithGelfLayoutTest.class.getName());
        final Set<Thread> threads = Collections.synchronizedSet(new HashSet<>());
        final List<Throwable> thrown = Collections.synchronizedList(new ArrayList<>());

        for (int x = 0; x < Runtime.getRuntime().availableProcessors() * 2; x++) {
            final Thread t = new LoggingThread(threads, log);
            threads.add(t);

            // Appender is configured with ignoreExceptions="false";
            // any exceptions are propagated to the caller, so we can catch them here.
            t.setUncaughtExceptionHandler((t1, e) -> thrown.add(e));
            t.start();
        }

        while (!threads.isEmpty()) {
            log.info("not done going to sleep...");
            Thread.sleep(10);
        }

        // if any error occurred, fail this test
        if (!thrown.isEmpty()) {
            throw thrown.get(0);
        }

        // simple test to ensure content is not corrupted
        if (Files.exists(PATH)) {
            final List<String> lines = Files.readAllLines(PATH, Charset.defaultCharset());
            for (final String line : lines) {
                assertThat(line, startsWith("{\"version\":\"1.1\",\"host\":\"myself\",\"timestamp\":"));
                assertThat(line, endsWith("\"}"));
            }
        }
    }

    private static class LoggingThread extends Thread {
        private final Set<Thread> threads;
        private final Logger log;

        LoggingThread(final Set<Thread> threads, final Logger log) {
            this.threads = threads;
            this.log = log;
        }

        @Override
        public void run() {
            log.info(threads.size());
            try {
                for (int i = 0; i < 64; i++) {
                    log.info("First message.");
                    log.info("Second message.");
                }
            } finally {
                threads.remove(this);
            }
        }
    }
}
