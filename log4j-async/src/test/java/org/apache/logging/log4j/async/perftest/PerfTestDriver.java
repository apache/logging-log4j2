package org.apache.logging.log4j.async.perftest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PerfTestDriver {
    static enum WaitStrategy {
        Sleep, Yield, Block
    };

    static class Setup implements Comparable<Setup> {
        private Class<?> _class;
        private String _log4jConfig;
        private String _name;
        private String[] _systemProperties;
        private int _threadCount;
        private File _temp;
        public Stats _stats;
        private WaitStrategy _wait;
        private String _runner;

        public Setup(Class<?> klass, String runner, String name,
                String log4jConfig, int threadCount, WaitStrategy wait,
                String... systemProperties) throws IOException {
            _class = klass;
            _runner = runner;
            _name = name;
            _log4jConfig = log4jConfig;
            _threadCount = threadCount;
            _systemProperties = systemProperties;
            _wait = wait;
            _temp = File.createTempFile("log4jperformance", ".txt");
        }

        List<String> processArguments(String java) {
            List<String> args = new ArrayList<String>();
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

            args.add("-Dlog4j.configuration=" + _log4jConfig); // log4j1.2
            args.add("-Dlog4j.configurationFile=" + _log4jConfig); // log4j2
            args.add("-Dlogback.configurationFile=" + _log4jConfig);// logback

            int ringBufferSize = getUserSpecifiedRingBufferSize();
            if (ringBufferSize >= 128) {
                args.add("-DAsyncLoggerConfig.RingBufferSize=" + ringBufferSize);
                args.add("-DAsyncLogger.RingBufferSize=" + ringBufferSize);
            }
            args.add("-DAsyncLoggerConfig.WaitStrategy=" + _wait);
            args.add("-DAsyncLogger.WaitStrategy=" + _wait);
            if (_systemProperties != null) {
                for (String property : _systemProperties) {
                    args.add(property);
                }
            }
            args.add("-cp");
            args.add(System.getProperty("java.class.path"));
            args.add(_class.getName());
            args.add(_runner);
            args.add(_name);
            args.add(_temp.getAbsolutePath());
            args.add(String.valueOf(_threadCount));
            return args;
        }

        private int getUserSpecifiedRingBufferSize() {
            try {
                return Integer.parseInt(System.getProperty("RingBufferSize",
                        "-1"));
            } catch (Exception ignored) {
                return -1;
            }
        }

        ProcessBuilder latencyTest(String java) {
            return new ProcessBuilder(processArguments(java));
        }

        ProcessBuilder throughputTest(String java) {
            List<String> args = processArguments(java);
            args.add("-throughput");
            return new ProcessBuilder(args);
        }

        @Override
        public int compareTo(Setup other) {
            // largest ops/sec first
            return (int) Math.signum(other._stats._averageOpsPerSec
                    - _stats._averageOpsPerSec);
        }

        public String description() {
            String detail = _class.getSimpleName();
            if (PerfTest.class == _class) {
                detail = "single thread";
            } else if (MTPerfTest.class == _class) {
                detail = _threadCount + " threads";
            }
            String target = _runner.substring(_runner.indexOf(".Run") + 4);
            return target + ": " + _name + " (" + detail + ")";
        }
    }

    static class Stats {
        int _count;
        long _average;
        long _pct99;
        long _pct99_99;
        double _latencyRowCount;
        int _throughputRowCount;
        private long _averageOpsPerSec;

        // example line: avg=828 99%=1118 99.99%=5028 Count=3125
        public Stats(String raw, int repeat) {
            String[] lines = raw.split("[\\r\\n]+");
            long totalOps = 0;
            for (String line : lines) {
                if (line.startsWith("avg")) {
                    _latencyRowCount++;
                    String[] parts = line.split(" ");
                    int i = 0;
                    _average += Long.parseLong(parts[i++].split("=")[1]);
                    _pct99 += Long.parseLong(parts[i++].split("=")[1]);
                    _pct99_99 += Long.parseLong(parts[i++].split("=")[1]);
                    _count += Integer.parseInt(parts[i++].split("=")[1]);
                } else {
                    _throughputRowCount++;
                    String number = line.substring(0, line.indexOf(' '));
                    long opsPerSec = Long.parseLong(number);
                    totalOps += opsPerSec;
                }
            }
            _averageOpsPerSec = totalOps / (int) _throughputRowCount;
        }

        public String toString() {
            String fmt = "throughput: %,d ops/sec. latency(ns): avg=%.1f 99%% < %.1f 99.99%% < %.1f (%d samples)";
            return String.format(fmt, _averageOpsPerSec, //
                    _average / _latencyRowCount, // mean latency
                    _pct99 / _latencyRowCount, // 99% observations less than
                    _pct99_99 / _latencyRowCount,// 99.99% observs less than
                    _count);
        }
    }

    // single-threaded performance test
    private static Setup s(String config, String runner, String name,
            String... systemProperties) throws IOException {
        WaitStrategy wait = WaitStrategy.valueOf(System.getProperty(
                "WaitStrategy", "Block"));
        return new Setup(PerfTest.class, runner, name, config, 1, wait,
                systemProperties);
    }

    // single-threaded performance test
    private static Setup m(String config, String runner, String name,
            int threadCount, String... systemProperties) throws IOException {
        WaitStrategy wait = WaitStrategy.valueOf(System.getProperty(
                "WaitStrategy", "Block"));
        return new Setup(MTPerfTest.class, runner, name, config, threadCount,
                wait, systemProperties);
    }

    public static void main(String[] args) throws Exception {
        final String ALL_ASYNC = "-DLog4jContextSelector=org.apache.logging.log4j.async.AsyncLoggerContextSelector";
        final String CACHEDCLOCK = "-DAsyncLogger.Clock=CachedClock";
        final String SYSCLOCK = "";
        final String LOG12 = "org.apache.logging.log4j.async.perftest.RunLog4j1";
        final String LOG20 = "org.apache.logging.log4j.async.perftest.RunLog4j2";
        final String LOGBK = "org.apache.logging.log4j.async.perftest.RunLogback";

        long start = System.nanoTime();
        List<Setup> tests = new ArrayList<PerfTestDriver.Setup>();
        tests.add(s("perf-logback.xml", LOGBK, "Sync"));
        tests.add(s("perf-log4j12.xml", LOG12, "Sync"));
        tests.add(s("perf3PlainNoLoc.xml", LOG20, "Sync"));
        tests.add(s("perf-logback-async.xml", LOGBK, "Async Appender"));
        tests.add(s("perf-log4j12-async.xml", LOG12, "Async Appender"));
        tests.add(s("perf5AsyncApndNoLoc.xml", LOG20,
                "Async Appender no location"));
        tests.add(s("perf3PlainNoLoc.xml", LOG20, "All async no loc SysClock",
                ALL_ASYNC, SYSCLOCK));
        tests.add(s("perf7MixedNoLoc.xml", LOG20, "Mixed async no location"));

        for (int i = 32; i <= 64; i *= 2) {
            tests.add(m("perf-logback.xml", LOGBK, "Sync", i));
            tests.add(m("perf-log4j12.xml", LOG12, "Sync", i));
            tests.add(m("perf3PlainNoLoc.xml", LOG20, "Sync", i));
            tests.add(m("perf-logback-async.xml", LOGBK, "Async Appender", i));
            tests.add(m("perf-log4j12-async.xml", LOG12, "Async Appender", i));
            tests.add(m("perf5AsyncApndNoLoc.xml", LOG20,
                    "Async Appender no location", i));
            tests.add(m("perf3PlainNoLoc.xml", LOG20,
                    "All async no loc SysClock", i, ALL_ASYNC, SYSCLOCK));
            tests.add(m("perf7MixedNoLoc.xml", LOG20,
                    "Mixed async no location", i));
        }

        // Setup[] tests = new Setup[] { //
        // s("perf-logback.xml", LOGBK, "Sync"), //
        // s("perf-log4j12.xml", LOG12, "Sync"), //
        // s("perf3PlainNoLoc.xml", LOG20, "Sync"), //
        // s("perf-logback-async.xml", LOGBK, "Async Appender"), //
        // s("perf-log4j12-async.xml", LOG12, "Async Appender"), //
        // s("perf5AsyncApndNoLoc.xml", LOG20,
        // "Async Appender no location"),
        // s("perf3PlainNoLoc.xml", LOG20, "All async no loc SysClock",
        // ALL_ASYNC, SYSCLOCK), //
        // s("perf7MixedNoLoc.xml", LOG20, "Mixed async no location"), //
        //
        // m("perf-log4j12-async.xml", LOG12, "Async Appender", 2), //
        // m("perf-log4j12.xml", LOG12, "Sync", 2), //
        // m("perf-logback.xml", LOGBK, "Sync", 2), //
        // m("perf-logback-async.xml", LOGBK, "Async Appender", 2), //
        // m("perf3PlainNoLoc.xml", LOG20, "Sync", 2), //
        // m("perf3PlainNoLoc.xml", LOG20, "All async no loc CachedClock",
        // 2, ALL_ASYNC, CACHEDCLOCK), //
        // m("perf3PlainNoLoc.xml", LOG20, "All async no loc SysClock", 2,
        // ALL_ASYNC, SYSCLOCK), //
        //
        // m("perf-log4j12-async.xml", LOG12, "Async Appender", 4), //
        // m("perf-log4j12.xml", LOG12, "Sync", 4), //
        // m("perf-logback.xml", LOGBK, "Sync", 4), //
        // m("perf-logback-async.xml", LOGBK, "Async Appender", 4), //
        // m("perf3PlainNoLoc.xml", LOG20, "Sync", 4), //
        // m("perf3PlainNoLoc.xml", LOG20,
        // "All async no loc CachedClock",
        // 4, ALL_ASYNC, CACHEDCLOCK), //
        // m("perf3PlainNoLoc.xml", LOG20, "All async no loc SysClock",
        // 4,
        // ALL_ASYNC, SYSCLOCK), //
        //
        // m("perf-log4j12-async.xml", LOG12, "Async Appender", 8), //
        // m("perf-log4j12.xml", LOG12, "Sync", 8), //
        // m("perf-logback.xml", LOGBK, "Sync", 8), //
        // m("perf-logback-async.xml", LOGBK, "Async Appender", 8), //
        // m("perf3PlainNoLoc.xml", LOG20, "Sync", 8), //
        // m("perf3PlainNoLoc.xml", LOG20,
        // "All async no loc CachedClock",
        // 8, ALL_ASYNC, CACHEDCLOCK), //
        // m("perf3PlainNoLoc.xml", LOG20, "All async no loc SysClock",
        // 8,
        // ALL_ASYNC, SYSCLOCK), //

        // 2 threads
        // m("perf5AsyncApndNoLoc.xml", LOG20,"Async Appender no location",
        // 2), //
        // m("perf6AsyncApndLoc.xml",
        // LOG12,"Async Appender with location",
        // 2), //
        // m("perf7MixedNoLoc.xml", "Mixed async no location", 2), //
        // m("perf8MixedLoc.xml", "Mixed async with location", 2), //
        // m("perf4PlainLocation.xml",
        // "All async with location SysClock",
        // 2, ALL_ASYNC), //
        // m("perf4PlainLocation.xml",
        // "All async with location CachedClock", 2, ALL_ASYNC,
        // CACHEDCLOCK), //
        // m("perf4PlainLocation.xml", "All sync with location", 2), //
        // m("perf1syncFile.xml", "FileAppender", 2), //
        // m("perf1syncFastFile.xml", "FastFileAppender", 2), //
        // m("perf2syncRollFile.xml", "RollFileAppender", 2), //
        // m("perf2syncRollFastFile.xml", "RollFastFileAppender", 2), //

        // 4 threads
        // m("perf5AsyncApndNoLoc.xml", LOG20,"Async Appender no location",
        // 4), //
        // m("perf6AsyncApndLoc.xml", "Async Appender with location",
        // 4), //
        // m("perf7MixedNoLoc.xml", "Mixed async no location", 4), //
        // m("perf8MixedLoc.xml", "Mixed async with location", 4), //
        // m("perf4PlainLocation.xml",
        // "All async with location SysClock",
        // 4, ALL_ASYNC), //
        // m("perf4PlainLocation.xml",
        // "All async with location CachedClock", 4, ALL_ASYNC,
        // CACHEDCLOCK), //
        // m("perf4PlainLocation.xml", "All sync with location", 4), //
        // m("perf1syncFile.xml", "FileAppender", 4), //
        // m("perf1syncFastFile.xml", "FastFileAppender", 4), //
        // m("perf2syncRollFile.xml", "RollFileAppender", 4), //
        // m("perf2syncRollFastFile.xml", "RollFastFileAppender", 4), //

        // 8 threads
        // m("perf5AsyncApndNoLoc.xml", LOG20,
        // "Async Appender no location", 8), //
        // m("perf6AsyncApndLoc.xml", "Async Appender with location",
        // 8), //
        // m("perf7MixedNoLoc.xml", "Mixed async no location", 8), //
        // m("perf8MixedLoc.xml", "Mixed async with location", 8), //
        // m("perf4PlainLocation.xml",
        // "All async with location SysClock",
        // 8, ALL_ASYNC), //
        // m("perf4PlainLocation.xml",
        // "All async with location CachedClock", 8, ALL_ASYNC,
        // CACHEDCLOCK), //
        // m("perf4PlainLocation.xml", "All sync with location", 8), //
        // m("perf1syncFile.xml", "FileAppender", 8), //
        // m("perf1syncFastFile.xml", "FastFileAppender", 8), //
        // m("perf2syncRollFile.xml", "RollFileAppender", 8), //
        // m("perf2syncRollFastFile.xml", "RollFastFileAppender", 8), //

        // s("perf-log4j12-async.xml", LOG12, "Async Appender"), //
        // s("perf-log4j12.xml", LOG12, "Sync"), //
        // s("perf-logback.xml", LOGBK, "Sync"), //
        // s("perf-logback-async.xml", LOGBK, "Async Appender"), //
        // s("perf3PlainNoLoc.xml", LOG20, "Sync"), //
        // s("perf3PlainNoLoc.xml", LOG20, "All async no loc CachedClock",
        // ALL_ASYNC, CACHEDCLOCK), //
        // s("perf3PlainNoLoc.xml", LOG20, "All async no loc SysClock",
        // ALL_ASYNC, SYSCLOCK), //
        // s("perf5AsyncApndNoLoc.xml", LOG20,
        // "Async Appender no location"),
        //
        // s("perf6AsyncApndLoc.xml", "Async Appender with location"), //
        // s("perf7MixedNoLoc.xml", "Mixed async no location"), //
        // s("perf8MixedLoc.xml", "Mixed async with location"), //
        // s("perf4PlainLocation.xml", "All async with location SysClock",
        // ALL_ASYNC), //
        // s("perf4PlainLocation.xml",
        // "All async with location CachedClock", ALL_ASYNC,
        // CACHEDCLOCK), //
        // s("perf4PlainLocation.xml", "All sync with location"), //
        // s("perf1syncFile.xml", "FileAppender"), //
        // s("perf1syncFastFile.xml", "FastFileAppender"), //
        // s("perf2syncRollFile.xml", "RollFileAppender"), //
        // s("perf2syncRollFastFile.xml", "RollFastFileAppender"), //
        // };

        String java = args.length > 0 ? args[0] : "java";
        int repeat = args.length > 1 ? Integer.parseInt(args[1]) : 5;
        int x = 0;
        for (Setup config : tests) {
            System.out.print(config.description());
            ProcessBuilder pb = config.throughputTest(java);
            pb.redirectErrorStream(true); // merge System.out and System.err
            long t1 = System.nanoTime();
            // int count = config._threadCount >= 16 ? 2 : repeat;
            int count = repeat;
            runPerfTest(count, x++, config, pb);
            System.out.printf(" took %.1f seconds%n", (System.nanoTime() - t1)
                    / (1000.0 * 1000.0 * 1000.0));

            FileReader reader = new FileReader(config._temp);
            CharBuffer buffer = CharBuffer.allocate(256 * 1024);
            reader.read(buffer);
            reader.close();
            config._temp.delete();
            buffer.flip();

            String raw = buffer.toString();
            System.out.print(raw);
            Stats stats = new Stats(raw, repeat);
            System.out.println(stats);
            System.out.println("-----");
            config._stats = stats;
        }
        new File("perftest.log").delete();
        System.out
                .printf("Done. Total duration: %.1f minutes%n",
                        (System.nanoTime() - start)
                                / (60.0 * 1000.0 * 1000.0 * 1000.0));

        printRanking((Setup[]) tests.toArray(new Setup[tests.size()]));
    }

    private static void printRanking(Setup[] tests) {
        System.out.println();
        System.out.println("Ranking:");
        Arrays.sort(tests);
        for (int i = 0; i < tests.length; i++) {
            Setup setup = tests[i];
            System.out.println((i + 1) + ". " + setup.description() + ": "
                    + setup._stats);
        }
    }

    private static void runPerfTest(int repeat, int setupIndex, Setup config,
            ProcessBuilder pb) throws IOException, InterruptedException {
        for (int i = 0; i < repeat; i++) {
            System.out.print(" (" + (i + 1) + "/" + repeat + ")...");
            final Process process = pb.start();

            final boolean[] stop = { false };
            printProcessOutput(process, stop);
            process.waitFor();
            stop[0] = true;

            File gc = new File("gc" + setupIndex + "_" + i
                    + config._log4jConfig + ".log");
            if (gc.exists()) {
                gc.delete();
            }
            new File("gc.log").renameTo(gc);
        }
    }

    private static Thread printProcessOutput(final Process process,
            final boolean[] stop) {

        Thread t = new Thread("OutputWriter") {
            public void run() {
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        process.getInputStream()));
                try {
                    String line = null;
                    while (!stop[0] && (line = in.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (Exception ignored) {
                }
            }
        };
        t.start();
        return t;
    }
}
