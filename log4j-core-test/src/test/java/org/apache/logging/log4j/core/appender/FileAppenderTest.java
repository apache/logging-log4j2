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
package org.apache.logging.log4j.core.appender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.Throwables;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.test.junit.CleanUpFiles;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests {@link FileAppender}.
 */
@CleanUpFiles(FileAppenderTest.FILE_NAME)
@Tag("sleepy")
public class FileAppenderTest {

    static final String FILE_NAME = "target/fileAppenderTest.log";
    private static final Path PATH = Paths.get(FILE_NAME);
    private static final int THREADS = 2;

    @AfterAll
    public static void cleanupClass() {
        assertFalse(AbstractManager.hasManager(FILE_NAME), "Manager for " + FILE_NAME + " not removed");
    }

    private final Configuration configuration = new DefaultConfiguration();

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testAppender(final boolean createOnDemand) throws Exception {
        final int logEventCount = 1;
        writer(false, logEventCount, "test", createOnDemand, false, configuration);
        verifyFile(logEventCount);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testLazyCreate(final boolean createOnDemand) throws Exception {
        final Layout layout = createPatternLayout(configuration);
        // @formatter:off
        final FileAppender appender = FileAppender.newBuilder()
                .setFileName(FILE_NAME)
                .setName("test")
                .setImmediateFlush(false)
                .setIgnoreExceptions(false)
                .setBufferedIo(false)
                .setBufferSize(1)
                .setLayout(layout)
                .setCreateOnDemand(createOnDemand)
                .build();
        // @formatter:on
        assertEquals(createOnDemand, appender.getManager().isCreateOnDemand());
        try {
            assertNotEquals(createOnDemand, Files.exists(PATH));
            appender.start();
            assertNotEquals(createOnDemand, Files.exists(PATH));
        } finally {
            appender.stop();
        }
        assertNotEquals(createOnDemand, Files.exists(PATH));
    }

    private static PatternLayout createPatternLayout(final Configuration configuration) {
        return PatternLayout.newBuilder()
                .setConfiguration(configuration)
                .setPattern(PatternLayout.SIMPLE_CONVERSION_PATTERN)
                .build();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testSmallestBufferSize(final boolean createOnDemand) throws Exception {
        final Layout layout = createPatternLayout(configuration);
        // @formatter:off
        final FileAppender appender = FileAppender.newBuilder()
                .setConfiguration(configuration)
                .setFileName(FILE_NAME)
                .setName("test")
                .setImmediateFlush(false)
                .setIgnoreExceptions(false)
                .setBufferedIo(false)
                .setBufferSize(1)
                .setLayout(layout)
                .setCreateOnDemand(createOnDemand)
                .build();
        // @formatter:on
        try {
            appender.start();
            final File file = new File(FILE_NAME);
            assertTrue(appender.isStarted(), "Appender did not start");
            assertNotEquals(createOnDemand, Files.exists(PATH));
            long curLen = file.length();
            long prevLen = curLen;
            assertEquals(0, curLen, "File length: " + curLen);
            for (int i = 0; i < 100; ++i) {
                // @formatter:off
                final LogEvent event = Log4jLogEvent.newBuilder()
                        .setLoggerName("TestLogger")
                        .setLoggerFqcn(FileAppenderTest.class.getName())
                        .setLevel(Level.INFO)
                        .setMessage(new SimpleMessage("Test"))
                        .setThreadName(this.getClass().getSimpleName())
                        .setTimeMillis(System.currentTimeMillis())
                        .build();
                // @formatter:on
                appender.append(event);
                curLen = file.length();
                assertTrue(curLen > prevLen, "File length: " + curLen);
                // Give up control long enough for another thread/process to occasionally do something.
                Thread.sleep(25);
                prevLen = curLen;
            }
        } finally {
            appender.stop();
        }
        assertFalse(appender.isStarted(), "Appender did not stop");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testLockingAppender(final boolean createOnDemand) throws Exception {
        final int logEventCount = 1;
        writer(true, logEventCount, "test", createOnDemand, false, configuration);
        verifyFile(logEventCount);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testMultipleAppenderThreads(final boolean createOnDemand) throws Exception {
        testMultipleLockingAppenderThreads(false, THREADS, createOnDemand);
    }

    private void testMultipleLockingAppenderThreads(
            final boolean lock, final int threadCount, final boolean createOnDemand)
            throws InterruptedException, Exception {
        final ExecutorService threadPool = Executors.newFixedThreadPool(threadCount);
        final AtomicReference<Throwable> throwableRef = new AtomicReference<>();
        final int logEventCount = 100;
        final Runnable runnable =
                new FileWriterRunnable(createOnDemand, lock, logEventCount, throwableRef, configuration);
        for (int i = 0; i < threadCount; ++i) {
            threadPool.execute(runnable);
        }
        threadPool.shutdown();
        boolean stopped = false;
        for (int i = 0; i < 20; i++) {
            // intentional assignment
            if (stopped = threadPool.awaitTermination(1, TimeUnit.SECONDS)) {
                break;
            }
        }
        if (throwableRef.get() != null) {
            Throwables.rethrow(throwableRef.get());
        }
        assertTrue(stopped, "The thread pool has not shutdown: " + threadPool);
        verifyFile(threadCount * logEventCount);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testMultipleLockingAppenders(final boolean createOnDemand) throws Exception {
        testMultipleLockingAppenderThreads(true, THREADS, createOnDemand);
    }

    private static void writer(
            final boolean locking,
            final int logEventCount,
            final String name,
            final boolean createOnDemand,
            final boolean concurrent,
            final Configuration configuration)
            throws Exception {
        final Layout layout = createPatternLayout(configuration);
        // @formatter:off
        final FileAppender appender = FileAppender.newBuilder()
                .setConfiguration(configuration)
                .setFileName(FILE_NAME)
                .setName("test")
                .setImmediateFlush(false)
                .setIgnoreExceptions(false)
                .setLocking(locking)
                .setBufferedIo(false)
                .setLayout(layout)
                .setCreateOnDemand(createOnDemand)
                .build();
        // @formatter:on
        assertEquals(createOnDemand, appender.getManager().isCreateOnDemand());
        try {
            appender.start();
            assertTrue(appender.isStarted(), "Appender did not start");
            final boolean exists = Files.exists(PATH);
            final String msg = String.format(
                    "concurrent = %s, createOnDemand = %s, file exists = %s", concurrent, createOnDemand, exists);
            // If concurrent the file might have been created (or not.)
            // Can't really test createOnDemand && concurrent.
            final boolean expectFileCreated = !createOnDemand;
            if (concurrent && expectFileCreated) {
                assertTrue(exists, msg);
            } else if (expectFileCreated) {
                assertNotEquals(createOnDemand, exists, msg);
            }
            for (int i = 0; i < logEventCount; ++i) {
                // @formatter:off
                final LogEvent logEvent = Log4jLogEvent.newBuilder()
                        .setLoggerName("TestLogger")
                        .setLoggerFqcn(FileAppenderTest.class.getName())
                        .setLevel(Level.INFO)
                        .setMessage(new SimpleMessage("Test"))
                        .setThreadName(name)
                        .setTimeMillis(System.currentTimeMillis())
                        .build();
                // @formatter:on
                appender.append(logEvent);
                Thread.sleep(
                        25); // Give up control long enough for another thread/process to occasionally do something.
            }
        } finally {
            appender.stop();
        }
        assertFalse(appender.isStarted(), "Appender did not stop");
    }

    private void verifyFile(final int count) throws Exception {
        // String expected = "[\\w]* \\[\\s*\\] INFO TestLogger - Test$";
        final String expected =
                "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3} \\[[^\\]]*\\] INFO TestLogger - Test";
        final Pattern pattern = Pattern.compile(expected);
        int lines = 0;
        try (final BufferedReader is = new BufferedReader(new InputStreamReader(new FileInputStream(FILE_NAME)))) {
            String str;
            while (is.ready()) {
                str = is.readLine();
                // System.out.println(str);
                ++lines;
                final Matcher matcher = pattern.matcher(str);
                assertTrue(matcher.matches(), "Unexpected data: " + str);
            }
        }
        assertEquals(count, lines);
    }

    public static class FileWriterRunnable implements Runnable {
        private final boolean createOnDemand;
        private final boolean lock;
        private final int logEventCount;
        private final AtomicReference<Throwable> throwableRef;
        private final Configuration configuration;

        public FileWriterRunnable(
                final boolean createOnDemand,
                final boolean lock,
                final int logEventCount,
                final AtomicReference<Throwable> throwableRef,
                final Configuration configuration) {
            this.createOnDemand = createOnDemand;
            this.lock = lock;
            this.logEventCount = logEventCount;
            this.throwableRef = throwableRef;
            this.configuration = configuration;
        }

        @Override
        public void run() {
            final Thread thread = Thread.currentThread();

            try {
                writer(lock, logEventCount, thread.getName(), createOnDemand, true, configuration);
            } catch (final Throwable e) {
                throwableRef.set(e);
            }
        }
    }
}
