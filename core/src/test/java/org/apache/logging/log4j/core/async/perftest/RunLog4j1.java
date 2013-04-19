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

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.lmax.disruptor.collections.Histogram;

public class RunLog4j1 implements IPerfTestRunner {

    @Override
    public void runThroughputTest(int lines, Histogram histogram) {
        long s1 = System.nanoTime();
        Logger logger = LogManager.getLogger(getClass());
        for (int j = 0; j < lines; j++) {
            logger.info(THROUGHPUT_MSG);
        }
        long s2 = System.nanoTime();
        long opsPerSec = (1000L * 1000L * 1000L * lines) / (s2 - s1);
        histogram.addObservation(opsPerSec);
    }

    @Override
    public void runLatencyTest(int samples, Histogram histogram,
            long nanoTimeCost, int threadCount) {
        Logger logger = LogManager.getLogger(getClass());
        for (int i = 0; i < samples; i++) {
            long s1 = System.nanoTime();
            logger.info(LATENCY_MSG);
            long s2 = System.nanoTime();
            long value = s2 - s1 - nanoTimeCost;
            if (value > 0) {
                histogram.addObservation(value);
            }
            // wait 1 microsec
            final long PAUSE_NANOS = 10000 * threadCount;
            long pauseStart = System.nanoTime();
            while (PAUSE_NANOS > (System.nanoTime() - pauseStart)) {
                // busy spin
            }
        }
    }

    @Override
    public void shutdown() {
        LogManager.shutdown();
    }

    @Override
    public void log(String finalMessage) {
        Logger logger = LogManager.getLogger(getClass());
        logger.info(finalMessage);
    }
}
