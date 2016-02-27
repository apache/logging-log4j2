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

import java.util.concurrent.TimeUnit;

import org.apache.kafka.common.metrics.stats.Count;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.async.AsyncLogger;
import org.apache.logging.log4j.core.async.AsyncLoggerContextSelector;

/**
 * Created by remko on 2/26/2016.
 */
public class SimplePerfTest {
    public static void main(String[] args) throws Exception {
        System.setProperty("Log4jContextSelector", AsyncLoggerContextSelector.class.getName());

        Logger logger = LogManager.getLogger();
        if (!(logger instanceof AsyncLogger)) {
            throw new IllegalStateException();
        }
        logger.info("Starting...");
        Thread.sleep(100);

        // warmup
        final int ITERATIONS = 100000;
        long startMs = System.currentTimeMillis();
        long end = startMs + TimeUnit.SECONDS.toMillis(10);
        long total = 0;
        int count = 0;
        do {
            long startNanos = System.nanoTime();
            loop(logger, ITERATIONS);
            long endNanos = System.nanoTime();
            long durationNanos = endNanos - startNanos;
            final long opsPerSec = (1000L * 1000L * 1000L * ITERATIONS) / durationNanos;
            System.out.printf("Warmup: Throughput: %,d ops/s%n", opsPerSec);
            total += opsPerSec;
            count++;
            Thread.sleep(1000);// drain buffer
        } while (System.currentTimeMillis() < end);
        System.out.printf("Average warmup throughput: %,d ops/s%n", total/count);

        final int COUNT = 10;
        final long[] durationNanos = new long[10];
        for (int i = 0; i < COUNT; i++) {
            final long startNanos = System.nanoTime();
            loop(logger, ITERATIONS);
            long endNanos = System.nanoTime();
            durationNanos[i] = endNanos - startNanos;
            Thread.sleep(1000);// drain buffer
        }
        total = 0;
        for (int i = 0; i < COUNT; i++) {
            final long opsPerSec = (1000L * 1000L * 1000L * ITERATIONS) / durationNanos[i];
            System.out.printf("Throughput: %,d ops/s%n", opsPerSec);
            total += opsPerSec;
        }
        System.out.printf("Average throughput: %,d ops/s%n", total/COUNT);
    }

    private static void loop(final Logger logger, final int iterations) {
//        String[] arg7 = new String[] {"arg1", "arg2","arg3", "arg4","arg5", "arg6","arg7", };
//        String[] arg2 = new String[] {"arg1", "arg2", };
//
        for (int i = 0; i < iterations; i ++) {
//        logger.info("7 arg message {} {} {} {} {} {} {}");
//        logger.info("7 arg message {} {} ");

            logger.info("simple text message");
        }
    }
}
