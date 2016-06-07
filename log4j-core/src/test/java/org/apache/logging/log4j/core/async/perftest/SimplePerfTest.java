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

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.async.AsyncLogger;
import org.apache.logging.log4j.core.async.AsyncLoggerContext;
import org.apache.logging.log4j.core.async.AsyncLoggerContextSelector;
import org.apache.logging.log4j.spi.LoggerContext;

/**
 * Created by remko on 2/26/2016.
 *
 * -XX:+UnlockDiagnosticVMOptions -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintTenuringDistribution -XX:+PrintGCApplicationConcurrentTime -XX:+PrintGCApplicationStoppedTime
 */
public class SimplePerfTest {
    static final int ITERATIONS = 100000;

    public static void main(final String[] args) throws Exception {
        System.setProperty("Log4jContextSelector", AsyncLoggerContextSelector.class.getName());

        final Logger logger = LogManager.getLogger();
        if (!(logger instanceof AsyncLogger)) {
            throw new IllegalStateException();
        }
        // work around a bug in Log4j-2.5
        workAroundLog4j2_5Bug();

        logger.error("Starting...");
        System.out.println("Starting...");
        Thread.sleep(100);

        final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        final long testStartNanos = System.nanoTime();
        final long[] UPTIMES = new long[1024];
        final long[] DURATIONS = new long[1024];

        // warmup
        final long startMs = System.currentTimeMillis();
        final long end = startMs + TimeUnit.SECONDS.toMillis(10);
        int warmupCount = 0;
        do {
            runTest(logger, runtimeMXBean, UPTIMES, DURATIONS, warmupCount);
            warmupCount++;
            // Thread.sleep(1000);// drain buffer
        } while (System.currentTimeMillis() < end);

        final int COUNT = 10;
        for (int i = 0; i < COUNT; i++) {
            final int count = warmupCount + i;
            runTest(logger, runtimeMXBean, UPTIMES, DURATIONS, count);
            // Thread.sleep(1000);// drain buffer
        }
        final double testDurationNanos = System.nanoTime() - testStartNanos;
        System.out.println("Done. Calculating stats...");

        printReport("Warmup", UPTIMES, DURATIONS, 0, warmupCount);
        printReport("Test", UPTIMES, DURATIONS, warmupCount, COUNT);

        final StringBuilder sb = new StringBuilder(512);
        sb.append("Test took: ").append(testDurationNanos/(1000.0*1000.0*1000.0)).append(" sec");
        System.out.println(sb);

        final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        for (int i = 0; i < gcBeans.size(); i++) {
            final GarbageCollectorMXBean gcBean = gcBeans.get(i);
            sb.setLength(0);
            sb.append("GC[").append(gcBean.getName()).append("] ");
            sb.append(gcBean.getCollectionCount()).append(" collections, collection time=");
            sb.append(gcBean.getCollectionTime()).append(" millis.");
            System.out.println(sb);
        }
    }

    private static void printReport(final String label, final long[] UPTIMES, final long[] DURATIONS,
            final int offset, final int length) {
        final StringBuilder sb = new StringBuilder(512);
        long total = 0;
        for (int i = offset; i < offset + length; i++) {
            sb.setLength(0);
            final long opsPerSec = (1000L * 1000L * 1000L * ITERATIONS) / DURATIONS[i];
            total += opsPerSec;
            sb.append(UPTIMES[i]).append(" ");
            sb.append(label).append(": Throughput: ").append(opsPerSec).append(" ops/s");
            System.out.println(sb);
        }
        sb.setLength(0);
        sb.append("Average ").append(label).append(" throughput: ").append(total/length).append(" ops/s");
        System.out.println(sb);

        sb.setLength(0);
        System.out.println(sb.append(label).append(" ran: ").append(length).append(" iterations"));
    }

    private static void runTest(final Logger logger, final RuntimeMXBean runtimeMXBean, final long[] UPTIMES,
            final long[] DURATIONS, final int index) {
        UPTIMES[index] = runtimeMXBean.getUptime();
        final long startNanos = System.nanoTime();
        loop(logger, ITERATIONS);
        final long endNanos = System.nanoTime();
        DURATIONS[index] = endNanos - startNanos;
    }

    private static void loop(final Logger logger, final int iterations) {
//        String[] arg7 = new String[] {"arg1", "arg2","arg3", "arg4","arg5", "arg6","arg7", };
//        String[] arg2 = new String[] {"arg1", "arg2", };
//
        for (int i = 0; i < iterations; i ++) {
//        logger.error("7 arg message {} {} {} {} {} {} {}");
//        logger.error("7 arg message {} {} ");

            logger.error("simple text message");
        }
    }

    private static void workAroundLog4j2_5Bug() {
        // use reflection so we can use the same test with older versions of log4j2
        try {
            final Method setUseThreadLocals =
                    AsyncLoggerContext.class.getDeclaredMethod("setUseThreadLocals", new Class[]{boolean.class});
            final LoggerContext context = LogManager.getContext(false);
            setUseThreadLocals.invoke(context, new Object[] {Boolean.TRUE});
        } catch (final Throwable ignored) {
        }
    }
}
