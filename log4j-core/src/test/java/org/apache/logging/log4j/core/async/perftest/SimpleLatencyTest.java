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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Latency test.
 * <p>
 * See <a href="https://groups.google.com/d/msg/mechanical-sympathy/0gaBXxFm4hE/O9QomwHIJAAJ">https://groups.google.com/d/msg/mechanical-sympathy/0gaBXxFm4hE/O9QomwHIJAAJ</a>:
 * </p>
 * <p>Gil Tene's rules of thumb for latency tests:</p>
 * <ol>
 * <li>DO measure max achievable throughput, but DON'T get focused on it as the main or single axis of measurement /
 * comparison.</li>
 * <li>DO measure response time / latency behaviors across a spectrum of attempted load levels (e.g. at attempted loads
 * between 2% to 100%+ of max established thoughout).</li>
 * <li>DO measure the response time / latency spectrum for each tested load (even for max throughout, for which response
 * time should linearly grow with test length, or the test is wrong). HdrHistogram is one good way to capture this
 * information.</li>
 * <li>DO make sure you are measuring response time correctly and labeling it right. If you also measure and report
 * service time, label it as such (don't call it "latency").
 * <li>DO compare response time / latency spectrum at given loads.</li>
 * <li>DO [repeatedly] sanity check and calibrate the benchmark setup to verify that it produces expected results for
 * known forced scenarios. E.g. forced pauses of known size via ^Z or SIGSTOP/SIGCONT should produce expected response
 * time percentile levels. Attempting to load at >100% than achieved throughput should result in response time / latency
 * measurements that grow with benchmark run length, while service time (if measured) should remain fairly flat well
 * past saturation.</li>
 * <li>DON'T use or report standard deviation for latency. Ever. Except if you mean it as a joke.</li>
 * <li>DON'T use average latency as a way to compare things with one another. [use median or 90%'ile instead, if what
 * you want to compare is "common case" latencies]. Consider not reporting avg. at all.</li>
 * <li>DON'T compare results of different setups or loads from short runs (< 20-30 minutes).</li>
 * <li>DON'T include process warmup behavior (e.g. 1st minute and 1st 50K messages) in compared or reported results.
 * </li>
 * </ol>
 */
public class SimpleLatencyTest {
    private static final String LATENCY_MSG = new String(new char[64]);

    public static void main(String[] args) throws Exception {
        //System.setProperty("Log4jContextSelector", AsyncLoggerContextSelector.class.getName());
        //System.setProperty("log4j.configurationFile", "perf3PlainNoLoc.xml");

        Logger logger = LogManager.getLogger();
        logger.info("Starting..."); // initializes Log4j
        Thread.sleep(100);

        final long nanoTimeCost = PerfTest.calcNanoTimeCost();
        System.out.println(nanoTimeCost);

        long maxThroughput = 1000 * 1000; // just assume... TODO make parameter
        double targetLoadLevel = 0.02; // TODO make parameter

        long targetMsgPerSec = (long) (maxThroughput * targetLoadLevel);
        long targetMsgCountPerIteration = 5 * 1000 * 1000;
        long durationMillisPerIteration = (1000 * targetMsgCountPerIteration) / targetMsgPerSec;


        final int threadCount = 1;
        List<Histogram> warmupHistograms = new ArrayList<>(threadCount);

        final int WARMUP_COUNT = 500000;
        final long interval = -1; // TODO calculate
        final IdleStrategy idleStrategy = new NoOpIdleStrategy();
        runLatencyTest(logger, WARMUP_COUNT, interval, idleStrategy, warmupHistograms, nanoTimeCost, threadCount);
        Thread.sleep(1000);

        List<Histogram> histograms = new ArrayList<>(threadCount);

        for (int i = 0; i < 30; i++) {
            final int ITERATIONS = 100 * 1000;// * 30;
            runLatencyTest(logger, ITERATIONS, interval, idleStrategy, histograms, nanoTimeCost, threadCount);

            // wait 10 microsec
            final long PAUSE_NANOS = 1000000 * threadCount;
            final long pauseStart = System.nanoTime();
            while (PAUSE_NANOS > (System.nanoTime() - pauseStart)) {
                // busy spin
                Thread.yield();
            }
        }
    }

    public static void runLatencyTest(final Logger logger, final int samples, final long interval,
            final IdleStrategy idleStrategy, final List<Histogram> histograms, final long nanoTimeCost,
            final int threadCount) {

        final CountDownLatch LATCH = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            final Histogram hist = new Histogram(TimeUnit.SECONDS.toNanos(10), 3);
            histograms.add(hist);
            final Thread t = new Thread("latencytest-" + i) {
                public void run() {
                    LATCH.countDown();
                    try {
                        LATCH.await();
                    } catch (InterruptedException e) {
                        interrupt(); // restore interrupt status
                        return;
                    }
                    for (int i = 0; i < samples; i++) {
                        final long s1 = System.nanoTime();
                        logger.info(LATENCY_MSG);
                        final long s2 = System.nanoTime();
                        final long value = s2 - s1 - nanoTimeCost;
                        if (value > 0) {
                            hist.recordValueWithExpectedInterval(value, interval);
                        }
                        while (System.nanoTime() - s2 < interval) {
                            idleStrategy.idle();
                        }
                    }
                }
            };
            t.start();
        }
    }
}
