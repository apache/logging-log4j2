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
package org.apache.logging.log4j.core.appender;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.Throwables;
import org.apache.logging.log4j.junit.CleanFiles;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.Strings;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests {@link FileAppender}.
 */
@RunWith(Parameterized.class)
public class FileAppenderTest {

    @Parameters(name = "createOnDemand = {0}")
    public static Boolean[] getParameters() {
        return new Boolean[] { false, true };
    }

    private static final String FILE_NAME = "target/fileAppenderTest.log";
    private static final Path PATH = Paths.get(FILE_NAME);
    private static final int THREADS = 2;

    public FileAppenderTest(final boolean createOnDemand) {
        super();
        this.createOnDemand = createOnDemand;
    }

    private final boolean createOnDemand;
    private final int threadCount = THREADS;

    @Rule
    public CleanFiles files = new CleanFiles(PATH);

    @AfterClass
    public static void cleanupClass() {
        assertTrue("Manager for " + FILE_NAME + " not removed", !AbstractManager.hasManager(FILE_NAME));
    }

    @Test
    public void testAppender() throws Exception {
        final int logEventCount = 1;
        writer(false, logEventCount, "test", createOnDemand, false);
        verifyFile(logEventCount);
    }

    @Test
    public void testLazyCreate() throws Exception {
        final Layout<String> layout = createPatternLayout();
        // @formatter:off
        final FileAppender appender = FileAppender.newBuilder()
            .withFileName(FILE_NAME)
            .withName("test")
            .withImmediateFlush(false)
            .withIgnoreExceptions(false)
            .withBufferedIo(false)
            .withBufferSize(1)
            .withLayout(layout)
            .withCreateOnDemand(createOnDemand)
            .build();
        // @formatter:on
        Assert.assertEquals(createOnDemand, appender.getManager().isCreateOnDemand());
        try {
            Assert.assertNotEquals(createOnDemand, Files.exists(PATH));
            appender.start();
            Assert.assertNotEquals(createOnDemand, Files.exists(PATH));
        } finally {
            appender.stop();
        }
        Assert.assertNotEquals(createOnDemand, Files.exists(PATH));
    }

    private static PatternLayout createPatternLayout() {
        return PatternLayout.newBuilder().withPattern(PatternLayout.SIMPLE_CONVERSION_PATTERN)
                .build();
    }

    @Test
    public void testSmallestBufferSize() throws Exception {
        final Layout<String> layout = createPatternLayout();
        // @formatter:off
        final FileAppender appender = FileAppender.newBuilder()
            .withFileName(FILE_NAME)
            .withName("test")
            .withImmediateFlush(false)
            .withIgnoreExceptions(false)
            .withBufferedIo(false)
            .withBufferSize(1)
            .withLayout(layout)
            .withCreateOnDemand(createOnDemand)
            .build();
        // @formatter:on
        try {
            appender.start();
            final File file = new File(FILE_NAME);
            assertTrue("Appender did not start", appender.isStarted());
            Assert.assertNotEquals(createOnDemand, Files.exists(PATH));
            long curLen = file.length();
            long prevLen = curLen;
            assertTrue("File length: " + curLen, curLen == 0);
            for (int i = 0; i < 100; ++i) {
                final LogEvent event = Log4jLogEvent.newBuilder().setLoggerName("TestLogger") //
                        .setLoggerFqcn(FileAppenderTest.class.getName()).setLevel(Level.INFO) //
                        .setMessage(new SimpleMessage("Test")).setThreadName(this.getClass().getSimpleName()) //
                        .setTimeMillis(System.currentTimeMillis()).build();
                try {
                    appender.append(event);
                    curLen = file.length();
                    assertTrue("File length: " + curLen, curLen > prevLen);
                    // Give up control long enough for another thread/process to occasionally do something.
                    Thread.sleep(25);
                } catch (final Exception ex) {
                    throw ex;
                }
                prevLen = curLen;
            }
        } finally {
            appender.stop();
        }
        assertFalse("Appender did not stop", appender.isStarted());
    }

    @Test
    public void testLockingAppender() throws Exception {
        final int logEventCount = 1;
        writer(true, logEventCount, "test", createOnDemand, false);
        verifyFile(logEventCount);
    }

    @Test
    public void testMultipleAppenderThreads() throws Exception {
        testMultipleLockingAppenderThreads(false, threadCount);
    }

    private void testMultipleLockingAppenderThreads(final boolean lock, final int threadCount)
            throws InterruptedException, Exception {
        final ExecutorService threadPool = Executors.newFixedThreadPool(threadCount);
        final Exception[] exceptionRef = new Exception[1];
        final int logEventCount = 100;
        final Runnable runnable = new FileWriterRunnable(lock, logEventCount, exceptionRef);
        for (int i = 0; i < threadCount; ++i) {
            threadPool.execute(runnable);
        }
        threadPool.shutdown();
        Assert.assertTrue("The thread pool has not shutdown: " + threadPool,
                threadPool.awaitTermination(10, TimeUnit.SECONDS));
        if (exceptionRef[0] != null) {
            throw exceptionRef[0];
        }
        verifyFile(threadCount * logEventCount);
    }

    @Test
    public void testMultipleLockingAppenders() throws Exception {
        testMultipleLockingAppenderThreads(true, threadCount);
    }

    @Test
    @Ignore
    public void testMultipleVMs() throws Exception {
        final String classPath = System.getProperty("java.class.path");
        final Integer logEventCount = 10;
        final int processCount = 3;
        final Process[] processes = new Process[processCount];
        final ProcessBuilder[] builders = new ProcessBuilder[processCount];
        for (int index = 0; index < processCount; ++index) {
            builders[index] = new ProcessBuilder("java", "-cp", classPath, ProcessTest.class.getName(),
                    "Process " + index, logEventCount.toString(), "true", Boolean.toString(createOnDemand));
        }
        for (int index = 0; index < processCount; ++index) {
            processes[index] = builders[index].start();
        }
        for (int index = 0; index < processCount; ++index) {
            final Process process = processes[index];
            // System.out.println("Process " + index + " exited with " + p.waitFor());
            try (final BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                }
            }
            process.destroy();
        }
        verifyFile(logEventCount * processCount);
    }

    private static void writer(final boolean locking, final int logEventCount, final String name, final boolean createOnDemand,
            final boolean concurrent) throws Exception {
        final Layout<String> layout = createPatternLayout();
        // @formatter:off
        final FileAppender appender = FileAppender.newBuilder()
            .withFileName(FILE_NAME)
            .withName("test")
            .withImmediateFlush(false)
            .withIgnoreExceptions(false)
            .withLocking(locking)
            .withBufferedIo(false)
            .withLayout(layout)
            .withCreateOnDemand(createOnDemand)
            .build();
        // @formatter:on
        Assert.assertEquals(createOnDemand, appender.getManager().isCreateOnDemand());
        try {
            appender.start();
            assertTrue("Appender did not start", appender.isStarted());
            final boolean exists = Files.exists(PATH);
            final String msg = String.format("concurrent = %s, createOnDemand = %s, file exists = %s", concurrent, createOnDemand,
                    exists);
            // If concurrent the file might have been created (or not.)
            // Can't really test createOnDemand && concurrent.
            final boolean expectFileCreated = !createOnDemand;
            if (concurrent && expectFileCreated) {
                Assert.assertTrue(msg, exists);
            } else if (expectFileCreated) {
                Assert.assertNotEquals(msg, createOnDemand, exists);
            }
            for (int i = 0; i < logEventCount; ++i) {
                final LogEvent logEvent = Log4jLogEvent.newBuilder().setLoggerName("TestLogger")
                        .setLoggerFqcn(FileAppenderTest.class.getName()).setLevel(Level.INFO)
                        .setMessage(new SimpleMessage("Test")).setThreadName(name)
                        .setTimeMillis(System.currentTimeMillis()).build();
                try {
                    appender.append(logEvent);
                    Thread.sleep(25); // Give up control long enough for another thread/process to occasionally do
                                      // something.
                } catch (final Exception ex) {
                    throw ex;
                }
            }
        } finally {
            appender.stop();
        }
        assertFalse("Appender did not stop", appender.isStarted());
    }

    private void verifyFile(final int count) throws Exception {
        // String expected = "[\\w]* \\[\\s*\\] INFO TestLogger - Test$";
        final String expected = "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3} \\[[^\\]]*\\] INFO TestLogger - Test";
        final Pattern pattern = Pattern.compile(expected);
        int lines = 0;
        try (final BufferedReader is = new BufferedReader(new InputStreamReader(new FileInputStream(FILE_NAME)))) {
            String str = Strings.EMPTY;
            while (is.ready()) {
                str = is.readLine();
                // System.out.println(str);
                ++lines;
                final Matcher matcher = pattern.matcher(str);
                assertTrue("Unexpected data: " + str, matcher.matches());
            }
        }
        Assert.assertEquals(count, lines);
    }

    public class FileWriterRunnable implements Runnable {
        private final boolean lock;
        private final int logEventCount;
        private final Exception[] exceptionRef;

        public FileWriterRunnable(final boolean lock, final int logEventCount, final Exception[] exceptionRef) {
            this.lock = lock;
            this.logEventCount = logEventCount;
            this.exceptionRef = exceptionRef;
        }

        @Override
        public void run() {
            final Thread thread = Thread.currentThread();

            try {
                writer(lock, logEventCount, thread.getName(), createOnDemand, true);
            } catch (final Exception e) {
                exceptionRef[0] = e;
                Throwables.rethrow(e);
            }
        }
    }

    public static class ProcessTest {

        public static void main(final String[] args) {

            if (args.length != 3) {
                System.out.println("Required arguments 'id', 'count' and 'lock' not provided");
                System.exit(-1);
            }
            final String id = args[0];

            final int count = Integer.parseInt(args[1]);

            if (count <= 0) {
                System.out.println("Invalid count value: " + args[1]);
                System.exit(-1);
            }
            final boolean lock = Boolean.parseBoolean(args[2]);

            final boolean createOnDemand = Boolean.parseBoolean(args[2]);

            // System.out.println("Got arguments " + id + ", " + count + ", " + lock);

            try {
                writer(lock, count, id, createOnDemand, true);
                // thread.sleep(50);

            } catch (final Exception e) {
                Throwables.rethrow(e);
            }

        }
    }
}
