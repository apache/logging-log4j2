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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import org.apache.logging.log4j.junit.CleanFiles;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.Strings;
import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class FileAppenderTest {

    private static final String FILENAME = "target/fileAppenderTest.log";
    private static final int THREADS = 2;

    @Rule
    public CleanFiles files = new CleanFiles(FILENAME);

    @AfterClass
    public static void cleanupClass() {
        assertTrue("Manager for " + FILENAME + " not removed", !AbstractManager.hasManager(FILENAME));
    }

    @Test
    public void testAppender() throws Exception {
        writer(false, 1, "test");
        verifyFile(1);
    }

    @Test
    public void testSmallestBufferSize() throws Exception {
        final Layout<String> layout = PatternLayout.newBuilder().withPattern(PatternLayout.SIMPLE_CONVERSION_PATTERN).build();
        final String bufferSizeStr = "1";
        final FileAppender appender = FileAppender.createAppender(FILENAME, "true", "false", "test", "false", "false",
                "false", bufferSizeStr, layout, null, "false", null, null);
        appender.start();
        final File file = new File(FILENAME);
        assertTrue("Appender did not start", appender.isStarted());
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
                Thread.sleep(25); // Give up control long enough for another thread/process to occasionally do
                                  // something.
            } catch (final Exception ex) {
                throw ex;
            }
            prevLen = curLen;
        }
        appender.stop();
        assertFalse("Appender did not stop", appender.isStarted());
    }

    @Test
    public void testLockingAppender() throws Exception {
        writer(true, 1, "test");
        verifyFile(1);
    }

    @Test
    public void testMultipleAppenders() throws Exception {
        final ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        final Exception[] error = new Exception[1];
        final int count = 100;
        final Runnable runnable = new FileWriterRunnable(false, count, error);
        for (int i = 0; i < THREADS; ++i) {
            pool.execute(runnable);
        }
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);
        if (error[0] != null) {
            throw error[0];
        }
        verifyFile(THREADS * count);
    }

    @Test
    public void testMultipleLockedAppenders() throws Exception {
        final ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        final Exception[] error = new Exception[1];
        final int count = 100;
        final Runnable runnable = new FileWriterRunnable(true, count, error);
        for (int i = 0; i < THREADS; ++i) {
            pool.execute(runnable);
        }
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);
        if (error[0] != null) {
            throw error[0];
        }
        verifyFile(THREADS * count);
    }

    @Test
    @Ignore
    public void testMultipleVMs() throws Exception {

        final String classPath = System.getProperty("java.class.path");
        final Integer count = 10;
        final int processeCount = 3;
        final Process[] processes = new Process[processeCount];
        final ProcessBuilder[] builders = new ProcessBuilder[processeCount];
        for (int index = 0; index < processeCount; ++index) {
            builders[index] = new ProcessBuilder("java", "-cp", classPath, ProcessTest.class.getName(), "Process "
                    + index, count.toString(), "true");
        }
        for (int index = 0; index < processeCount; ++index) {
            processes[index] = builders[index].start();
        }
        for (int index = 0; index < processeCount; ++index) {
            final Process process = processes[index];
            // System.out.println("Process " + index + " exited with " + p.waitFor());
            final InputStream is = process.getInputStream();
            final InputStreamReader isr = new InputStreamReader(is);
            final BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
            is.close();
            process.destroy();
        }
        verifyFile(count * processeCount);
    }

    private static void writer(final boolean lock, final int count, final String name) throws Exception {
        final Layout<String> layout = PatternLayout.newBuilder().withPattern(PatternLayout.SIMPLE_CONVERSION_PATTERN).build();
        final FileAppender app = FileAppender.createAppender(FILENAME, "true", Boolean.toString(lock), "test", "false",
                "false", "false", null, layout, null, "false", null, null);
        app.start();
        assertTrue("Appender did not start", app.isStarted());
        for (int i = 0; i < count; ++i) {
            final LogEvent event = Log4jLogEvent.newBuilder().setLoggerName("TestLogger")
                    .setLoggerFqcn(FileAppenderTest.class.getName()).setLevel(Level.INFO)
                    .setMessage(new SimpleMessage("Test")).setThreadName(name).setTimeMillis(System.currentTimeMillis())
                    .build();
            try {
                app.append(event);
                Thread.sleep(25); // Give up control long enough for another thread/process to occasionally do
                                  // something.
            } catch (final Exception ex) {
                throw ex;
            }
        }
        app.stop();
        assertFalse("Appender did not stop", app.isStarted());
    }

    private void verifyFile(final int count) throws Exception {
        // String expected = "[\\w]* \\[\\s*\\] INFO TestLogger - Test$";
        final String expected = "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3} \\[[^\\]]*\\] INFO TestLogger - Test";
        final Pattern pattern = Pattern.compile(expected);
        final FileInputStream fis = new FileInputStream(FILENAME);
        final BufferedReader is = new BufferedReader(new InputStreamReader(fis));
        int counter = 0;
        String str = Strings.EMPTY;
        while (is.ready()) {
            str = is.readLine();
            // System.out.println(str);
            ++counter;
            final Matcher matcher = pattern.matcher(str);
            assertTrue("Bad data: " + str, matcher.matches());
        }
        fis.close();
        assertTrue("Incorrect count: was " + counter + " should be " + count, count == counter);
        fis.close();

    }

    public class FileWriterRunnable implements Runnable {
        private final boolean lock;
        private final int count;
        private final Exception[] error;

        public FileWriterRunnable(final boolean lock, final int count, final Exception[] error) {
            this.lock = lock;
            this.count = count;
            this.error = error;
        }

        @Override
        public void run() {
            final Thread thread = Thread.currentThread();

            try {
                writer(lock, count, thread.getName());

            } catch (final Exception ex) {
                error[0] = ex;
                throw new RuntimeException(ex);
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

            // System.out.println("Got arguments " + id + ", " + count + ", " + lock);

            try {
                writer(lock, count, id);
                // thread.sleep(50);

            } catch (final Exception ex) {
                throw new RuntimeException(ex);
            }

        }
    }
}
