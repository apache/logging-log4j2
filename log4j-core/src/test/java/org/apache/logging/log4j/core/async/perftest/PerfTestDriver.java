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
package org.apache.logging.log4j.core.async.perftest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.core.async.AsyncLoggerContextSelector;

/**
 * Runs a sequence of performance tests.
 */
public class PerfTestDriver {
    private static final String DEFAULT_WAIT_STRATEGY = "Block";

    static enum WaitStrategy {
        Sleep, Yield, Block;

        public static WaitStrategy get() {
            return WaitStrategy.valueOf(System.getProperty("WaitStrategy", DEFAULT_WAIT_STRATEGY));
        }
    }

    /**
     * Defines the setup for a java process running a performance test.
     */
    static class Setup implements Comparable<Setup> {
        private final Class<?> klass;
        private final String log4jConfig;
        private final String name;
        private final String[] systemProperties;
        private final int threadCount;
        private final File temp;
        public Stats stats;
        private final WaitStrategy wait;
        private final Runner runner;

        public Setup(final Class<?> klass, final Runner runner, final String name, final String log4jConfig,
                final int threadCount, final WaitStrategy wait, final String... systemProperties) throws IOException {
            this.klass = klass;
            this.runner = runner;
            this.name = name;
            this.log4jConfig = log4jConfig;
            this.threadCount = threadCount;
            this.systemProperties = systemProperties;
            this.wait = wait;
            this.temp = File.createTempFile("log4jperformance", ".txt");
        }

        List<String> processArguments(final String java) {
            final List<String> args = new ArrayList<>();
            args.add(java);
            args.add("-server");
            args.add("-Xms1g");
            args.add("-Xmx1g");

            // args.add("-XX:+UseParallelOldGC");
            // args.add("-Xloggc:gc.log");
            // args.add("-XX:+PrintGCTimeStamps");
            // args.add("-XX:+PrintGCDetails");
            // args.add("-XX:+PrintGCDateStamps");
            // args.add("-XX:+PrintGCApplicationStoppedTime");
            // args.add("-XX:+PrintGCApplicationConcurrentTime");
            // args.add("-XX:+PrintSafepointStatistics");

            args.add("-Dlog4j.configuration=" + log4jConfig); // log4j 1.2
            args.add("-Dlog4j.configurationFile=" + log4jConfig); // log4j 2
            args.add("-Dlogback.configurationFile=" + log4jConfig);// logback

            final int ringBufferSize = getUserSpecifiedRingBufferSize();
            if (ringBufferSize >= 128) {
                args.add("-DAsyncLoggerConfig.RingBufferSize=" + ringBufferSize);
                args.add("-DAsyncLogger.RingBufferSize=" + ringBufferSize);
            }
            args.add("-DAsyncLoggerConfig.WaitStrategy=" + wait);
            args.add("-DAsyncLogger.WaitStrategy=" + wait);
            if (systemProperties != null) {
                Collections.addAll(args, systemProperties);
            }
            args.add("-cp");
            args.add(System.getProperty("java.class.path"));
            args.add(klass.getName());
            args.add(runner.implementationClass.getName());
            args.add(name);
            args.add(temp.getAbsolutePath());
            args.add(String.valueOf(threadCount));
            return args;
        }

        private int getUserSpecifiedRingBufferSize() {
            try {
                return Integer.parseInt(System.getProperty("RingBufferSize", "-1"));
            } catch (final Exception ignored) {
                return -1;
            }
        }

        ProcessBuilder latencyTest(final String java) {
            return new ProcessBuilder(processArguments(java));
        }

        ProcessBuilder throughputTest(final String java) {
            final List<String> args = processArguments(java);
            args.add("-throughput");
            return new ProcessBuilder(args);
        }

        @Override
        public int compareTo(final Setup other) {
            // largest ops/sec first
            return Long.signum(other.stats.averageOpsPerSec - stats.averageOpsPerSec);
        }

        public String description() {
            String detail = klass.getSimpleName();
            if (PerfTest.class == klass) {
                detail = "single thread";
            } else if (MultiThreadPerfTest.class == klass) {
                detail = threadCount + " threads";
            }
            final String target = runner.name();
            return target + ": " + name + " (" + detail + ')';
        }
    }

    /**
     * Results of a performance test.
     */
    static class Stats {
        int count;
        long average;
        long pct99;
        long pct99_99;
        double latencyRowCount;
        int throughputRowCount;
        private final long averageOpsPerSec; // Do not make final. Compile fails on Java 6.

        // example line: avg=828 99%=1118 99.99%=5028 Count=3125
        public Stats(final String raw) {
            final String[] lines = raw.split("[\\r\\n]+");
            long totalOps = 0;
            for (final String line : lines) {
                if (line.startsWith("avg")) {
                    latencyRowCount++;
                    final String[] parts = line.split(" ");
                    int i = 0;
                    average += Long.parseLong(parts[i++].split("=")[1]);
                    pct99 += Long.parseLong(parts[i++].split("=")[1]);
                    pct99_99 += Long.parseLong(parts[i++].split("=")[1]);
                    count += Integer.parseInt(parts[i].split("=")[1]);
                } else {
                    throughputRowCount++;
                    final String number = line.substring(0, line.indexOf(' '));
                    final long opsPerSec = Long.parseLong(number);
                    totalOps += opsPerSec;
                }
            }
            averageOpsPerSec = totalOps / throughputRowCount;
        }

        @Override
        public String toString() {
            final String fmt = "throughput: %,d ops/sec. latency(ns): avg=%.1f 99%% < %.1f 99.99%% < %.1f (%d samples)";
            return String.format(fmt, averageOpsPerSec, //
                    average / latencyRowCount, // mean latency
                    pct99 / latencyRowCount, // 99% observations less than
                    pct99_99 / latencyRowCount,// 99.99% observs less than
                    count);
        }
    }

    static enum Runner {
        Log4j12(RunLog4j1.class), //
        Log4j2(RunLog4j2.class), //
        Logback(RunLogback.class);

        private final Class<? extends IPerfTestRunner> implementationClass;

        private Runner(final Class<? extends IPerfTestRunner> cls) {
            this.implementationClass = cls;
        }
    }

    public static void main(final String[] args) throws Exception {
        final long start = System.nanoTime();
        
        final List<Setup> tests = selectTests();
        runPerfTests(args, tests);
        
        System.out.printf("Done. Total duration: %.1f minutes%n", (System.nanoTime() - start)
                / (60.0 * 1000.0 * 1000.0 * 1000.0));

        printRanking(tests.toArray(new Setup[tests.size()]));
    }

    private static List<Setup> selectTests() throws IOException {
        final List<Setup> tests = new ArrayList<>();
        
        // final String CACHEDCLOCK = "-Dlog4j.Clock=CachedClock";
        final String SYSCLOCK = "-Dlog4j.Clock=SystemClock";
        final String ALL_ASYNC = "-DLog4jContextSelector=" + AsyncLoggerContextSelector.class.getName();

        final String THREADNAME = "-DAsyncLogger.ThreadNameStrategy=" //
                + System.getProperty("AsyncLogger.ThreadNameStrategy", "CACHED");

        // includeLocation=false
        add(tests, 1, "perf3PlainNoLoc.xml", Runner.Log4j2, "Loggers all async", ALL_ASYNC, SYSCLOCK, THREADNAME);
        add(tests, 1, "perf7MixedNoLoc.xml", Runner.Log4j2, "Loggers mixed sync/async");
        add(tests, 1, "perf-logback.xml", Runner.Logback, "Sync");
        add(tests, 1, "perf-log4j12.xml", Runner.Log4j12, "Sync");
        add(tests, 1, "perf3PlainNoLoc.xml", Runner.Log4j2, "Sync");
        add(tests, 1, "perf-logback-async.xml", Runner.Logback, "Async Appender");
        add(tests, 1, "perf-log4j12-async.xml", Runner.Log4j12, "Async Appender");
        add(tests, 1, "perf5AsyncApndNoLoc.xml", Runner.Log4j2, "Async Appender");

        // includeLocation=true
        // add(tests, 1, "perf6AsyncApndLoc.xml", Runner.Log4j2, "Async Appender includeLocation");
        // add(tests, 1, "perf8MixedLoc.xml", Runner.Log4j2, "Mixed sync/async includeLocation");
        // add(tests, 1, "perf4PlainLocation.xml", Runner.Log4j2, "Loggers all async includeLocation", ALL_ASYNC);
        // add(tests, 1, "perf4PlainLocation.xml", Runner.Log4j2, "Loggers all async includeLocation CachedClock", ALL_ASYNC, CACHEDCLOCK);
        // add(tests, 1, "perf4PlainLocation.xml", Runner.Log4j2, "Sync includeLocation");

        // appenders
        // add(tests, 1, "perf1syncFile.xml", Runner.Log4j2, "FileAppender");
        // add(tests, 1, "perf1syncRandomAccessFile.xml", Runner.Log4j2, "RandomAccessFileAppender");
        // add(tests, 1, "perf2syncRollFile.xml", Runner.Log4j2, "RollFileAppender");
        // add(tests, 1, "perf2syncRollRandomAccessFile.xml", Runner.Log4j2, "RollRandomAccessFileAppender");

        final int MAX_THREADS = 4; // 64 takes a LONG time
        for (int i = 2; i <= MAX_THREADS; i *= 2) {
            // includeLocation = false
            add(tests, i, "perf-logback.xml", Runner.Logback, "Sync");
            add(tests, i, "perf-log4j12.xml", Runner.Log4j12, "Sync");
            add(tests, i, "perf3PlainNoLoc.xml", Runner.Log4j2, "Sync");
            add(tests, i, "perf-logback-async.xml", Runner.Logback, "Async Appender");
            add(tests, i, "perf-log4j12-async.xml", Runner.Log4j12, "Async Appender");
            add(tests, i, "perf5AsyncApndNoLoc.xml", Runner.Log4j2, "Async Appender");
            add(tests, i, "perf3PlainNoLoc.xml", Runner.Log4j2, "Loggers all async", ALL_ASYNC, SYSCLOCK, THREADNAME);
            add(tests, i, "perf7MixedNoLoc.xml", Runner.Log4j2, "Loggers mixed sync/async");

            // includeLocation=true
            // add(tests, i, "perf6AsyncApndLoc.xml", Runner.Log4j2, "Async Appender includeLocation");
            // add(tests, i, "perf8MixedLoc.xml", Runner.Log4j2, "Mixed sync/async includeLocation");
            // add(tests, i, "perf4PlainLocation.xml", Runner.Log4j2, "Loggers all async includeLocation", ALL_ASYNC));
            // add(tests, i, "perf4PlainLocation.xml", Runner.Log4j2, "Loggers all async includeLocation CachedClock", ALL_ASYNC, CACHEDCLOCK));
            // add(tests, i, "perf4PlainLocation.xml", Runner.Log4j2, "Sync includeLocation");

            // appenders
            // add(tests, i, "perf1syncFile.xml", Runner.Log4j2, "FileAppender");
            // add(tests, i, "perf1syncRandomAccessFile.xml", Runner.Log4j2, "RandomAccessFileAppender");
            // add(tests, i, "perf2syncRollFile.xml", Runner.Log4j2, "RollFileAppender");
            // add(tests, i, "perf2syncRollRandomAccessFile.xml", Runner.Log4j2, "RollRandomAccessFileAppender");
        }
        return tests;
    }

    private static void add(final List<Setup> tests, final int threadCount, final String config, final Runner runner, final String name,
            final String... systemProperties) throws IOException {
        final WaitStrategy wait = WaitStrategy.get();
        final Class<?> perfTest = threadCount == 1 ? PerfTest.class : MultiThreadPerfTest.class;
        final Setup setup = new Setup(perfTest, runner, name, config, threadCount, wait, systemProperties);
        tests.add(setup);
    }

    private static void runPerfTests(final String[] args, final List<Setup> tests) throws IOException,
            InterruptedException, FileNotFoundException {
        final String java = args.length > 0 ? args[0] : "java";
        final int repeat = args.length > 1 ? Integer.parseInt(args[1]) : 5;
        int x = 0;
        for (final Setup setup : tests) {
            System.out.print(setup.description());
            final ProcessBuilder pb = setup.throughputTest(java);
            pb.redirectErrorStream(true); // merge System.out and System.err
            final long t1 = System.nanoTime();
            final int count = setup.threadCount >= 4 ? 3 : repeat;
            runPerfTest(count, x++, setup, pb);
            System.out.printf(" took %.1f seconds%n", (System.nanoTime() - t1) / (1000.0 * 1000.0 * 1000.0));

            final FileReader reader = new FileReader(setup.temp);
            final CharBuffer buffer = CharBuffer.allocate(256 * 1024);
            reader.read(buffer);
            reader.close();
            setup.temp.delete();
            buffer.flip();

            final String raw = buffer.toString();
            System.out.print(raw);
            final Stats stats = new Stats(raw);
            System.out.println(stats);
            System.out.println("-----");
            setup.stats = stats;
        }
        new File("perftest.log").delete();
    }

    private static void printRanking(final Setup[] tests) {
        System.out.println();
        System.out.println("Ranking:");
        Arrays.sort(tests);
        for (int i = 0; i < tests.length; i++) {
            final Setup setup = tests[i];
            System.out.println((i + 1) + ". " + setup.description() + ": " + setup.stats);
        }
    }

    private static void runPerfTest(final int repeat, final int setupIndex, final Setup config, final ProcessBuilder pb)
            throws IOException, InterruptedException {
        for (int i = 0; i < repeat; i++) {
            System.out.print(" (" + (i + 1) + '/' + repeat + ")...");
            final Process process = pb.start();

            final boolean[] stop = {false};
            printProcessOutput(process, stop);
            process.waitFor();
            stop[0] = true;

            final File gc = new File("gc" + setupIndex + '_' + i + config.log4jConfig + ".log");
            if (gc.exists()) {
                gc.delete();
            }
            new File("gc.log").renameTo(gc);
        }
    }

    private static Thread printProcessOutput(final Process process, final boolean[] stop) {

        final Thread t = new Thread("OutputWriter") {
            @Override
            public void run() {
                final BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
                try {
                    String line = null;
                    while (!stop[0] && (line = in.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (final Exception ignored) {
                }
            }
        };
        t.start();
        return t;
    }
}
