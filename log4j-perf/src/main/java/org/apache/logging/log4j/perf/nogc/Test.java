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
package org.apache.logging.log4j.perf.nogc;

import java.util.Date;

import static org.apache.logging.log4j.util.Unbox.*;

/**
 * Tests the classic Log4j2 components.
 * <p>
 * Run CLASSIC test (varargs, ParameterizedMessage, PatternLayout(%m)):
 * java -Xms64M -Xmx64M -cp log4j-perf/target/benchmarks.jar -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCApplicationStoppedTime org.apache.logging.log4j.perf.nogc.Test Classic
 *
 * Run NOGC test (unrolled varargs, StringBuilderMessage, NoGcLayout(%m)):
 * java -Xms64M -Xmx64M -cp log4j-perf/target/benchmarks.jar -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCApplicationStoppedTime org.apache.logging.log4j.perf.nogc.Test NoGC
 *
 * </p>
 */
public class Test {
    private static final int COUNT = 10;
    private static final int REPETITIONS = 1000000;
    static long[] startTime = new long[COUNT];
    static long[] duration = new long[COUNT];
    static long[] checksums = new long[COUNT];

    public static void main(final String[] args) throws Exception {
        if ("Classic".equalsIgnoreCase(args[0])) {
            final ClassicLogger logger = new ClassicLogger();
            runTestSuite(logger);
        } else if ("NoGC".equalsIgnoreCase(args[0])) {
            final NoGcLogger logger = new NoGcLogger();
            runTestSuite(logger);
        } else {
            throw new IllegalArgumentException("Specify either Classic or NoGC");
//            ClassicLogger classic = new ClassicLogger();
//            NoGcLogger nogc = new NoGcLogger();
//            doTestRunBoth(classic, nogc, REPETITIONS, 0);
        }
        reportResults(args[0]);
    }

    private static void runTestSuite(final ClassicLogger logger) throws Exception {
        for (int i = 0; i < 10; i++) {
            doTestRun(logger, REPETITIONS, i);
            Thread.sleep(100);
        }
    }

    private static void runTestSuite(final NoGcLogger logger) throws Exception {
        for (int i = 0; i < 10; i++) {
            doTestRun(logger, REPETITIONS, i);
            Thread.sleep(100);
        }
    }

    private static void doTestRun(final NoGcLogger logger, final int repetitions, final int n) {
        startTime[n] = System.currentTimeMillis();
        final long start = System.nanoTime();
        for (int i = 0; i < repetitions; i++) {
            logger.log("Test message str={}, double={}, int={}, obj={}", "abc", box(i / 2.5), box(i), "XYX");
            //logger.log("Test message str={}, double={}, int={}, obj={}", "abc", (i / 2.5), (i), logger);
        }
        duration[n] = System.nanoTime() - start;
        checksums[n] = logger.appender.checksum;
        logger.appender.checksum = 0;
    }

    private static void doTestRun(final ClassicLogger logger, final int repetitions, final int n) {
        startTime[n] = System.currentTimeMillis();
        final long start = System.nanoTime();
        for (int i = 0; i < repetitions; i++) {
            logger.log("Test message str={}, double={}, int={}, obj={}", "abc", i / 2.5, i, "XYX");
        }
        duration[n] = System.nanoTime() - start;
        checksums[n] = logger.appender.checksum;
        logger.appender.checksum = 0;
    }

    private static void doTestRunBoth(final ClassicLogger classic, final NoGcLogger nogc, final int repetitions, final int n) {
        startTime[n] = System.currentTimeMillis();
        final long start = System.nanoTime();
        for (int i = 0; i < repetitions; i++) {
            classic.log("Test message str={}, double={}, int={}, obj={}", "abc", i / 2.5, i, "XYX");
            nogc.log("Test message str={}, double={}, int={}, obj={}", "abc", box(i / 2.5), box(i), "XYX");

            if (classic.appender.checksum != nogc.appender.checksum) {
                throw new IllegalStateException();
            }
        }
        duration[n] = System.nanoTime() - start;
    }

    private static void reportResults(final String type) {
        for (int i = 0; i < COUNT; i++) {
            System.out.printf("%s[%d] (%3$tF %3$tT.%3$tL) took %4$,d ns. CHECK=%5$s%n", type, i,
                    new Date(startTime[i]), duration[i], Long.toHexString(checksums[i]));
        }
    }
}
