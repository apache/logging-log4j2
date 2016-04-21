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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.HdrHistogram.Histogram;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.async.AsyncLoggerContextSelector;
import org.apache.logging.log4j.core.async.DefaultAsyncEventRouter;
import org.apache.logging.log4j.core.async.EventRoute;

/**
 * Latency test.
 */
// -DAsyncLogger.WaitStrategy=busywait
//-XX:+UnlockDiagnosticVMOptions -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintTenuringDistribution -XX:+PrintGCApplicationConcurrentTime -XX:+PrintGCApplicationStoppedTime
public class SimpleLatencyTest {
    private static final String LATENCY_MSG = new String(new char[64]);

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Please specify thread count and interval (us)");
            return;
        }
        final int threadCount = Integer.parseInt(args[0]);
        final int intervalMicros = Integer.parseInt(args[1]);

        System.setProperty("log4j2.AsyncEventRouter", PrintingDefaultAsyncEventRouter.class.getName());
        System.setProperty("AsyncLogger.RingBufferSize", String.valueOf(256 * 1024));
        System.setProperty("Log4jContextSelector", AsyncLoggerContextSelector.class.getName());
        System.setProperty("log4j.configurationFile", "perf3PlainNoLoc.xml");

        Logger logger = LogManager.getLogger();
        logger.info("Starting..."); // initializes Log4j
        Thread.sleep(100);

        final long nanoTimeCost = PerfTest.calcNanoTimeCost();
        System.out.println("nanoTimeCost=" + nanoTimeCost);

        final long interval = TimeUnit.MICROSECONDS.toNanos(intervalMicros);// * threadCount;
        System.out.printf("%d threads, interval is %d nanos%n", threadCount, interval);

        final long WARMUP_DURATION_MILLIS = TimeUnit.MINUTES.toMillis(1);
        List<Histogram> warmupHistograms = new ArrayList<>(threadCount);

        final int WARMUP_COUNT = 50000 / threadCount;
        final IdleStrategy idleStrategy = new YieldIdleStrategy();
        runLatencyTest(logger, WARMUP_DURATION_MILLIS, WARMUP_COUNT, interval, idleStrategy, warmupHistograms, nanoTimeCost, threadCount);
        System.out.println("Warmup done.");
        Thread.sleep(1000);

        long start = System.currentTimeMillis();
        List<Histogram> histograms = new ArrayList<>(threadCount);

        final long TEST_DURATION_MILLIS = TimeUnit.MINUTES.toMillis(4);
        final int COUNT = (5000 * 1000) / threadCount;
        runLatencyTest(logger, TEST_DURATION_MILLIS, COUNT, interval, idleStrategy, histograms, nanoTimeCost, threadCount);
        long end = System.currentTimeMillis();

        final Histogram result = new Histogram(TimeUnit.SECONDS.toNanos(10), 3);
        for (Histogram hist : histograms) {
            result.add(hist);
        }
        result.outputPercentileDistribution(System.out, 1000.0);
        System.out.println("Test duration: " + (end - start) / 1000.0 + " seconds");
    }

    public static void runLatencyTest(final Logger logger, final long durationMillis, final int samples,
            final long intervalNanos,
            final IdleStrategy idleStrategy, final List<Histogram> histograms, final long nanoTimeCost,
            final int threadCount) throws InterruptedException {

        Thread[] threads = new Thread[threadCount];
        final CountDownLatch LATCH = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            final Histogram hist = new Histogram(TimeUnit.SECONDS.toNanos(10), 3);
            histograms.add(hist);
            threads[i] = new Thread("latencytest-" + i) {
                @Override
                public void run() {
                    LATCH.countDown();
                    try {
                        LATCH.await();
                    } catch (InterruptedException e) {
                        interrupt(); // restore interrupt status
                        return;
                    }
                    long start = System.currentTimeMillis();
                    do {
                        runLatencyTest(samples, logger, nanoTimeCost, hist, intervalNanos, idleStrategy);
                    } while (System.currentTimeMillis() - start < durationMillis);
                }
            };
            threads[i].start();
        }
        for (int i = 0; i < threadCount; i++) {
            threads[i].join();
        }
    }

    private static void runLatencyTest(int samples, Logger logger, long nanoTimeCost, Histogram hist, long intervalNanos, IdleStrategy idleStrategy) {
        for (int i = 0; i < samples; i++) {
            final long s1 = System.nanoTime();
            logger.info(LATENCY_MSG);
            final long s2 = System.nanoTime();
            final long value = s2 - s1 - nanoTimeCost;
            if (value > 0) {
                hist.recordValueWithExpectedInterval(value, intervalNanos);
            }
            while (System.nanoTime() - s2 < intervalNanos) {
                idleStrategy.idle();
            }
        }
    }

    public static class PrintingDefaultAsyncEventRouter extends DefaultAsyncEventRouter {
        @Override
        public EventRoute getRoute(long backgroundThreadId, Level level) {
            System.out.println("RINGBUFFER FULL!");
            return super.getRoute(backgroundThreadId, level);
        }
    }

}
