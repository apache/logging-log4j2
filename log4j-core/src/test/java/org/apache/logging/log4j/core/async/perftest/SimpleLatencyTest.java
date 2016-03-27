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

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.concurrent.TimeUnit;

import com.lmax.disruptor.collections.Histogram;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.async.AsyncLogger;
import org.apache.logging.log4j.core.async.AsyncLoggerContextSelector;

/**
 *
 */
public class SimpleLatencyTest {
    private static final String LATENCY_MSG = new String(new char[64]);

    public static void main(String[] args) throws Exception {
        System.setProperty("Log4jContextSelector", AsyncLoggerContextSelector.class.getName());
        System.setProperty("log4j.configurationFile", "perf3PlainNoLoc.xml");
        System.setProperty("log4j2.enable.threadlocals", "true");
        System.setProperty("AsyncLogger.WaitStrategy", "Block");

        Logger logger = LogManager.getLogger();
        if (!(logger instanceof AsyncLogger)) {
            throw new IllegalStateException();
        }
        logger.info("Starting...");
        Thread.sleep(100);

        System.out.println(PerfTest.calcNanoTimeCost());
        final long nanoTimeCost = PerfTest.calcNanoTimeCost();
        final Histogram warmupHistogram = PerfTest.createHistogram();
        final Histogram histogram = PerfTest.createHistogram();
        final int threadCount = 1;

        final int WARMUP_COUNT = 500000;
        runLatencyTest(logger, WARMUP_COUNT, warmupHistogram, nanoTimeCost, threadCount);
        Thread.sleep(1000);

        for (int i = 0 ; i < 30; i++) {
            final int ITERATIONS = 100 * 1000;// * 30;
            runLatencyTest(logger, ITERATIONS, histogram, nanoTimeCost, threadCount);

            // wait 10 microsec
            final long PAUSE_NANOS = 1000000 * threadCount;
            final long pauseStart = System.nanoTime();
            while (PAUSE_NANOS > (System.nanoTime() - pauseStart)) {
                // busy spin
                Thread.yield();
            }

        }

        System.out.println(histogram);
    }

    public static void runLatencyTest(final Logger logger, final int samples, final Histogram histogram,
            final long nanoTimeCost, final int threadCount) {
        for (int i = 0; i < samples; i++) {
            final long s1 = System.nanoTime();
            logger.info(LATENCY_MSG);
            final long s2 = System.nanoTime();
            final long value = s2 - s1 - nanoTimeCost;
            if (value > 0) {
                histogram.addObservation(value);
            }
        }
    }
}
