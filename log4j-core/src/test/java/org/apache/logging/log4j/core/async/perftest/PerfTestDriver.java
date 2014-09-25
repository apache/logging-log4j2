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
    static enum WaitStrategy {
        Sleep, Yield, Block
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
        private final String runner;

        public Setup(final Class<?> klass, final String runner, final String name,
                final String log4jConfig, final int threadCount, final WaitStrategy wait,
                final String... systemProperties) throws IOException {
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
            final List<String> args = new ArrayList<String>();
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
            args.add(runner);
            args.add(name);
            args.add(temp.getAbsolutePath());
            args.add(String.valueOf(threadCount));
            return args;
        }

        private int getUserSpecifiedRingBufferSize() {
            try {
                return Integer.parseInt(System.getProperty("RingBufferSize",
                        "-1"));
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
            } else if (MtPerfTest.class == klass) {
                detail = threadCount + " threads";
            }
            final String target = runner.substring(runner.indexOf(".Run") + 4);
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
        private final long averageOpsPerSec;

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

    // single-threaded performance test
    private static Setup s(final String config, final String runner, final String name,
            final String... systemProperties) throws IOException {
        final WaitStrategy wait = WaitStrategy.valueOf(System.getProperty(
                "WaitStrategy", "Sleep"));
        return new Setup(PerfTest.class, runner, name, config, 1, wait,
                systemProperties);
    }

    // multi-threaded performance test
    private static Setup m(final String config, final String runner, final String name,
            final int threadCount, final String... systemProperties) throws IOException {
        final WaitStrategy wait = WaitStrategy.valueOf(System.getProperty(
                "WaitStrategy", "Sleep"));
        return new Setup(MtPerfTest.class, runner, name, config, threadCount,
                wait, systemProperties);
    }

    public static void main(final String[] args) throws Exception {
        final String ALL_ASYNC = "-DLog4jContextSelector="
                + AsyncLoggerContextSelector.class.getName();
        final String CACHEDCLOCK = "-Dlog4j.Clock=CachedClock";
        final String SYSCLOCK = "-Dlog4j.Clock=SystemClock";
        final String LOG12 = RunLog4j1.class.getName();
        final String LOG20 = RunLog4j2.class.getName();
        final String LOGBK = RunLogback.class.getName();

        final String THREADNAME = "-DAsyncLogger.ThreadNameStrategy=" //
                + System.getProperty("AsyncLogger.ThreadNameStrategy", "CACHED");

        final long start = System.nanoTime();
        final List<Setup> tests = new ArrayList<PerfTestDriver.Setup>();
        // includeLocation=false
        tests.add(s("perf3PlainNoLoc.xml", LOG20, "Loggers all async",
                ALL_ASYNC, SYSCLOCK, THREADNAME));
        tests.add(s("perf7MixedNoLoc.xml", LOG20, "Loggers mixed sync/async"));
        tests.add(s("perf-logback.xml", LOGBK, "Sync"));
        tests.add(s("perf-log4j12.xml", LOG12, "Sync"));
        tests.add(s("perf3PlainNoLoc.xml", LOG20, "Sync"));
        tests.add(s("perf-logback-async.xml", LOGBK, "Async Appender"));
        tests.add(s("perf-log4j12-async.xml", LOG12, "Async Appender"));
        tests.add(s("perf5AsyncApndNoLoc.xml", LOG20, "Async Appender"));

        // includeLocation=true
        // tests.add(s("perf6AsyncApndLoc.xml", LOG20,
        // "Async Appender includeLocation"));
        // tests.add(s("perf8MixedLoc.xml", LOG20,
        // "Mixed sync/async includeLocation"));
        // tests.add(s("perf4PlainLocation.xml", LOG20,
        // "Loggers all async includeLocation", ALL_ASYNC));
        // tests.add(s("perf4PlainLocation.xml", LOG20,
        // "Loggers all async includeLocation CachedClock", ALL_ASYNC,
        // CACHEDCLOCK));
        // tests.add(s("perf4PlainLocation.xml", LOG20,
        // "Sync includeLocation"));

        // appenders
        // tests.add(s("perf1syncFile.xml", LOG20, "FileAppender"));
        // tests.add(s("perf1syncRandomAccessFile.xml", LOG20, "RandomAccessFileAppender"));
        // tests.add(s("perf2syncRollFile.xml", LOG20, "RollFileAppender"));
        // tests.add(s("perf2syncRollRandomAccessFile.xml", LOG20,
        // "RollRandomAccessFileAppender"));

        final int MAX_THREADS = 4; // 64 takes a LONG time
        for (int i = 2; i <= MAX_THREADS; i *= 2) {
            // includeLocation = false
            tests.add(m("perf-logback.xml", LOGBK, "Sync", i));
            tests.add(m("perf-log4j12.xml", LOG12, "Sync", i));
            tests.add(m("perf3PlainNoLoc.xml", LOG20, "Sync", i));
            tests.add(m("perf-logback-async.xml", LOGBK, "Async Appender", i));
            tests.add(m("perf-log4j12-async.xml", LOG12, "Async Appender", i));
            tests.add(m("perf5AsyncApndNoLoc.xml", LOG20, "Async Appender", i));
            tests.add(m("perf3PlainNoLoc.xml", LOG20, "Loggers all async", i,
                    ALL_ASYNC, SYSCLOCK, THREADNAME));
            tests.add(m("perf7MixedNoLoc.xml", LOG20,
                    "Loggers mixed sync/async", i));

            // includeLocation=true
            // tests.add(m("perf6AsyncApndLoc.xml", LOG20,
            // "Async Appender includeLocation", i));
            // tests.add(m("perf8MixedLoc.xml", LOG20,
            // "Mixed sync/async includeLocation", i));
            // tests.add(m("perf4PlainLocation.xml", LOG20,
            // "Loggers all async includeLocation", i, ALL_ASYNC));
            // tests.add(m("perf4PlainLocation.xml", LOG20,
            // "Loggers all async includeLocation CachedClock", i,
            // ALL_ASYNC, CACHEDCLOCK));
            // tests.add(m("perf4PlainLocation.xml", LOG20,
            // "Sync includeLocation", i));

            // appenders
            // tests.add(m("perf1syncFile.xml", LOG20, "FileAppender", i));
            // tests.add(m("perf1syncRandomAccessFile.xml", LOG20, "RandomAccessFileAppender",
            // i));
            // tests.add(m("perf2syncRollFile.xml", LOG20, "RollFileAppender",
            // i));
            // tests.add(m("perf2syncRollRandomAccessFile.xml", LOG20,
            // "RollRandomAccessFileAppender", i));
        }

        final String java = args.length > 0 ? args[0] : "java";
        final int repeat = args.length > 1 ? Integer.parseInt(args[1]) : 5;
        int x = 0;
        for (final Setup config : tests) {
            System.out.print(config.description());
            final ProcessBuilder pb = config.throughputTest(java);
            pb.redirectErrorStream(true); // merge System.out and System.err
            final long t1 = System.nanoTime();
            final int count = config.threadCount >= 4 ? 3 : repeat;
            runPerfTest(count, x++, config, pb);
            System.out.printf(" took %.1f seconds%n", (System.nanoTime() - t1)
                    / (1000.0 * 1000.0 * 1000.0));

            final FileReader reader = new FileReader(config.temp);
            final CharBuffer buffer = CharBuffer.allocate(256 * 1024);
            reader.read(buffer);
            reader.close();
            config.temp.delete();
            buffer.flip();

            final String raw = buffer.toString();
            System.out.print(raw);
            final Stats stats = new Stats(raw);
            System.out.println(stats);
            System.out.println("-----");
            config.stats = stats;
        }
        new File("perftest.log").delete();
        System.out
                .printf("Done. Total duration: %.1f minutes%n",
                        (System.nanoTime() - start)
                                / (60.0 * 1000.0 * 1000.0 * 1000.0));

        printRanking(tests.toArray(new Setup[tests.size()]));
    }

    private static void printRanking(final Setup[] tests) {
        System.out.println();
        System.out.println("Ranking:");
        Arrays.sort(tests);
        for (int i = 0; i < tests.length; i++) {
            final Setup setup = tests[i];
            System.out.println((i + 1) + ". " + setup.description() + ": "
                    + setup.stats);
        }
    }

    private static void runPerfTest(final int repeat, final int setupIndex, final Setup config,
            final ProcessBuilder pb) throws IOException, InterruptedException {
        for (int i = 0; i < repeat; i++) {
            System.out.print(" (" + (i + 1) + '/' + repeat + ")...");
            final Process process = pb.start();

            final boolean[] stop = { false };
            printProcessOutput(process, stop);
            process.waitFor();
            stop[0] = true;

            final File gc = new File("gc" + setupIndex + '_' + i
                    + config.log4jConfig + ".log");
            if (gc.exists()) {
                gc.delete();
            }
            new File("gc.log").renameTo(gc);
        }
    }

    private static Thread printProcessOutput(final Process process,
            final boolean[] stop) {

        final Thread t = new Thread("OutputWriter") {
            @Override
            public void run() {
                final BufferedReader in = new BufferedReader(new InputStreamReader(
                        process.getInputStream()));
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
