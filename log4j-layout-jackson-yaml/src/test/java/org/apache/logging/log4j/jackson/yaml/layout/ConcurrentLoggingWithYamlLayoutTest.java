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
package org.apache.logging.log4j.jackson.yaml.layout;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.test.junit.CleanUpFiles;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Like the test for LOG4J2-1769.
 */
public class ConcurrentLoggingWithYamlLayoutTest {

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

    private static final String PATH = "target/test-yaml-layout.log";

    @Test
    @CleanUpFiles(PATH)
    @LoggerContextSource("log4j2-yaml-layout.xml")
    public void testConcurrentLogging(final LoggerContext context) throws Throwable {
        final Logger log = context.getLogger(ConcurrentLoggingWithYamlLayoutTest.class);
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
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10));
        }

        // if any error occurred, fail this test
        if (!thrown.isEmpty()) {
            throw thrown.get(0);
        }

        // simple test to ensure content is not corrupted
        if (new File(PATH).exists()) {
            final List<String> lines = Files.readAllLines(new File(PATH).toPath(), Charset.defaultCharset());
            assertThat(lines.get(0), startsWith("---"));
            // TODO more
        }
    }
}
