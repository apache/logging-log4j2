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

import java.io.File;
import java.util.concurrent.TimeUnit;

import com.lmax.disruptor.collections.Histogram;

public class MultiThreadPerfTest extends PerfTest {

    public static void main(final String[] args) throws Exception {
        new MultiThreadPerfTest().doMain(args);
    }

    @Override
    public void runTestAndPrintResult(final IPerfTestRunner runner,
            final String name, final int threadCount, final String resultFile)
            throws Exception {

        // ThreadContext.put("aKey", "mdcVal");
        PerfTest.println("Warming up the JVM...");
        final long t1 = System.nanoTime();

        // warmup at least 2 rounds and at most 1 minute
        final Histogram warmupHist = PerfTest.createHistogram();
        final long stop = System.nanoTime() + TimeUnit.MINUTES.toNanos(1);
        final Runnable run1 = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 10; i++) {
                    final int LINES = PerfTest.throughput ? 50000 : 200000;
                    runTest(runner, LINES, null, warmupHist, 2);
                    if (i > 0 && System.nanoTime() - stop >= 0) {
                        return;
                    }
                }
            }
        };
        final Thread thread1 = new Thread(run1);
        final Thread thread2 = new Thread(run1);
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        PerfTest.printf("Warmup complete in %.1f seconds%n",
                (System.nanoTime() - t1) / (1000.0 * 1000.0 * 1000.0));
        PerfTest.println("Waiting 10 seconds for buffers to drain warmup data...");
        Thread.sleep(10000);
        new File("perftest.log").delete();
        new File("perftest.log").createNewFile();

        PerfTest.println("Starting the main test...");
        PerfTest.throughput = false;
        multiThreadedTestRun(runner, name, threadCount, resultFile);

        Thread.sleep(1000);
        PerfTest.throughput = true;
        multiThreadedTestRun(runner, name, threadCount, resultFile);
    }

    private void multiThreadedTestRun(final IPerfTestRunner runner,
            final String name, final int threadCount, final String resultFile)
            throws Exception {

        final Histogram[] histograms = new Histogram[threadCount];
        for (int i = 0; i < histograms.length; i++) {
            histograms[i] = PerfTest.createHistogram();
        }
        final int LINES = 256 * 1024;

        final Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threads.length; i++) {
            final Histogram histogram = histograms[i];
            threads[i] = new Thread() {
                @Override
                public void run() {
//                    int latencyCount = threadCount >= 16 ? 1000000 : 5000000;
                    final int latencyCount = 5000000;
                    final int count = PerfTest.throughput ? LINES / threadCount
                            : latencyCount;
                    runTest(runner, count, "end", histogram, threadCount);
                }
            };
        }
        for (final Thread thread : threads) {
            thread.start();
        }
        for (final Thread thread : threads) {
            thread.join();
        }

        for (final Histogram histogram : histograms) {
            PerfTest.reportResult(resultFile, name, histogram);
        }
}
}