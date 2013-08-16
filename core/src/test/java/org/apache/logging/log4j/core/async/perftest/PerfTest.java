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

import java.io.FileWriter;
import java.io.IOException;

import com.lmax.disruptor.collections.Histogram;

public class PerfTest {

    private static final String LINE100 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890!\"#$%&'()-=^~|\\@`[]{};:+*,.<>/?_123456";
    public static final String LINE500 = LINE100 + LINE100 + LINE100 + LINE100
            + LINE100;

    static boolean verbose = false;
    static boolean throughput;

    // determine how long it takes to call System.nanoTime() (on average)
    static long calcNanoTimeCost() {
        final long iterations = 10000000;
        final long start = System.nanoTime();
        long finish = start;

        for (int i = 0; i < iterations; i++) {
            finish = System.nanoTime();
        }

        if (finish <= start) {
            throw new IllegalStateException();
        }

        finish = System.nanoTime();
        return (finish - start) / iterations;
    }

    static Histogram createHistogram() {
        final long[] intervals = new long[31];
        long intervalUpperBound = 1L;
        for (int i = 0, size = intervals.length - 1; i < size; i++) {
            intervalUpperBound *= 2;
            intervals[i] = intervalUpperBound;
        }

        intervals[intervals.length - 1] = Long.MAX_VALUE;
        return new Histogram(intervals);
    }

    public static void main(final String[] args) throws Exception {
        new PerfTest().doMain(args);
    }

    public void doMain(final String[] args) throws Exception {
        final String runnerClass = args[0];
        final IPerfTestRunner runner = (IPerfTestRunner) Class.forName(runnerClass)
                .newInstance();
        final String name = args[1];
        final String resultFile = args.length > 2 ? args[2] : null;
        for (final String arg : args) {
            if (verbose && throughput) {
               break;
            }
            if ("-verbose".equalsIgnoreCase(arg)) {
                verbose = true;
            }
            if ("-throughput".equalsIgnoreCase(arg)) {
                throughput = true;
            }
        }
        final int threadCount = args.length > 2 ? Integer.parseInt(args[3]) : 3;
        printf("Starting %s %s (%d)...%n", getClass().getSimpleName(), name,
                threadCount);
        runTestAndPrintResult(runner, name, threadCount, resultFile);
        runner.shutdown();
        System.exit(0);
    }

    public void runTestAndPrintResult(final IPerfTestRunner runner,
            final String name, final int threadCount, final String resultFile)
            throws Exception {
        final Histogram warmupHist = createHistogram();

        // ThreadContext.put("aKey", "mdcVal");
        println("Warming up the JVM...");
        final long t1 = System.nanoTime();

        // warmup at least 2 rounds and at most 1 minute
        final long stop = System.currentTimeMillis() + (60 * 1000);
        for (int i = 0; i < 10; i++) {
            final int LINES = throughput ? 50000 : 200000;
            runTest(runner, LINES, null, warmupHist, 1);
            if (i > 0 && System.currentTimeMillis() >= stop) {
                return;
            }
        }

        printf("Warmup complete in %.1f seconds%n", (System.nanoTime() - t1)
                / (1000.0 * 1000.0 * 1000.0));
        println("Waiting 10 seconds for buffers to drain warmup data...");
        Thread.sleep(10000);

        println("Starting the main test...");
        // test
        throughput = false;
        runSingleThreadedTest(runner, name, resultFile);

        Thread.sleep(1000);

        throughput = true;
        runSingleThreadedTest(runner, name, resultFile);
    }

    private int runSingleThreadedTest(final IPerfTestRunner runner, final String name,
            final String resultFile) throws IOException {
        final Histogram latency = createHistogram();
        final int LINES = throughput ? 50000 : 5000000;
        runTest(runner, LINES, "end", latency, 1);
        reportResult(resultFile, name, latency);
        return LINES;
    }

    static void reportResult(final String file, final String name, final Histogram histogram)
            throws IOException {
        final String result = createSamplingReport(name, histogram);
        println(result);

        if (file != null) {
            final FileWriter writer = new FileWriter(file, true);
            writer.write(result);
            writer.write(System.getProperty("line.separator"));
            writer.close();
        }
    }

    static void printf(final String msg, final Object... objects) {
        if (verbose) {
            System.out.printf(msg, objects);
        }
    }

    static void println(final String msg) {
        if (verbose) {
            System.out.println(msg);
        }
    }

    static String createSamplingReport(final String name, final Histogram histogram) {
        final Histogram data = histogram;
        if (throughput) {
            return data.getMax() + " operations/second";
        }
        final String result = String.format(
                "avg=%.0f 99%%=%d 99.99%%=%d sampleCount=%d", data.getMean(), //
                data.getTwoNinesUpperBound(), //
                data.getFourNinesUpperBound(), //
                data.getCount() //
                );
        return result;
    }

    public void runTest(final IPerfTestRunner runner, final int lines, final String finalMessage,
            final Histogram histogram, final int threadCount) {
        if (throughput) {
            runner.runThroughputTest(lines, histogram);
        } else {
            final long nanoTimeCost = calcNanoTimeCost();
            runner.runLatencyTest(lines, histogram, nanoTimeCost, threadCount);
        }
        if (finalMessage != null) {
            runner.log(finalMessage);
        }
    }
}
