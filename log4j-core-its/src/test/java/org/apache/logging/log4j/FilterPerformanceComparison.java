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
package org.apache.logging.log4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.categories.PerformanceTests;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.MDC;

/**
 *
 */
@Category(PerformanceTests.class)
public class FilterPerformanceComparison {

    private final Logger logger = LogManager.getLogger(FilterPerformanceComparison.class.getName());
    private final org.slf4j.Logger logbacklogger = org.slf4j.LoggerFactory.getLogger(FilterPerformanceComparison.class);


    // How many times should we try to log:
    private static final int COUNT = 10000000;
    private static final int THREADED_COUNT = 100000;
    private static final int WARMUP = 1000;

    private static final String CONFIG = "log4j2-perf-filter.xml";
    private static final String LOGBACK_CONFIG = "logback-perf-filter.xml";

    private static final String LOGBACK_CONF = "logback.configurationFile";

    @BeforeClass
    public static void setupClass() {
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
        System.setProperty(LOGBACK_CONF, LOGBACK_CONFIG);
    }

    @AfterClass
    public static void cleanupClass() {
        System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        System.clearProperty(LOGBACK_CONF);
    }

    @After
    public void after() {
        ThreadContext.clearAll();
    }

    @Test
    public void testPerformanceEmptyContext() throws Exception {
        testPerformance(Collections.<String, String>emptyMap());
    }

    @Test
    public void testPerformanceNonEmptyContext() throws Exception {
        testPerformance(createNonEmptyContextData());
    }

    private Map<String, String> createNonEmptyContextData() {
        final Map<String, String> context = new HashMap<>();
        context.put("user0", "Apache");
        context.put("user1", "Apache");
        return context;
    }

    private static void putContextData(final Map<String, String> contextData) {
        ThreadContext.putAll(contextData);
        for (final Map.Entry<String, String> entry : contextData.entrySet()) {
            MDC.put(entry.getKey(), entry.getValue());
        }
    }

    private void testPerformance(final Map<String, String> contextData) throws Exception {
        putContextData(contextData);
        Target.LOGBACK.timedLoop(logger, logbacklogger, WARMUP);
        Target.LOG4J2.timedLoop(logger, logbacklogger, WARMUP);

        System.out.println("Single-threaded Log4j 2.0, "
                + (contextData.isEmpty() ? "EMPTY context" : "NON-EMPTY context"));

        final long result3 = Target.LOG4J2.timedLoop(logger, logbacklogger, COUNT);
        System.out.println("Single-threaded Logback, "
                + (contextData.isEmpty() ? "EMPTY context" : "NON-EMPTY context"));

        final long result2 = Target.LOGBACK.timedLoop(logger, logbacklogger, COUNT);

        System.out.println("###############################################");
        System.out.println("Logback: " + result2);
        System.out.println("Log4j 2.0: " + result3);
        System.out.println("###############################################");
    }

    @Test
    public void testThreadsEmptyContext() throws Exception {
        testThreads(Collections.<String, String>emptyMap());
    }

    @Test
    public void testThreadsNonEmptyContext() throws Exception {
        testThreads(createNonEmptyContextData());
    }

    private void testThreads(final Map<String, String> contextData) throws Exception {
        System.out.println("Testing multithreading");
        final int threadedCount = COUNT; // THREADED_COUNT * threadCount < COUNT ? COUNT / threadCount : THREADED_COUNT;
        final int[] threadCounts = new int[] {1, 2, 5, 10, 20, 50};
        for (final int threadCount : threadCounts) {
            System.out.println("Testing " + threadCount + " threads, "
                    + (contextData.isEmpty() ? "EMPTY context" : "NON-EMPTY context"));
            final Worker[] workers = new Worker[threadCount];
            final long[] results = new long[threadCount];
            for (int i=0; i < threadCount; ++i) {
                workers[i] = new Worker(Target.LOG4J2, threadedCount, results, i, contextData);
            }
            for (int i=0; i < threadCount; ++i) {
                workers[i].start();
            }
            long total = 0;
            for (int i=0; i < threadCount; ++i) {
                workers[i].join();
                total += results[i];
            }
            final long result3 = total / threadCount;
            total = 0;
            for (int i=0; i < threadCount; ++i) {
                workers[i] = new Worker(Target.LOGBACK, threadedCount, results, i, contextData);
            }
            for (int i=0; i < threadCount; ++i) {
                workers[i].start();
            }
            for (int i=0; i < threadCount; ++i) {
                workers[i].join();
                total += results[i];
            }
            final long result2 = total / threadCount;
            System.out.println("###############################################");
            System.out.println("Logback: " + result2);
            System.out.println("Log4j 2.0: " + result3 );
            System.out.println("###############################################");
        }
    }

    private enum Target {
        LOGBACK {
            @Override
            long timedLoop(final Logger logger, final org.slf4j.Logger logbacklogger, final int loop) {
                final Integer j = Integer.valueOf(2);
                final long start = System.nanoTime();
                for (int i = 0; i < loop; i++) {
                    logbacklogger.debug("SEE IF THIS IS LOGGED {}.", j);
                }
                return (System.nanoTime() - start) / loop;
            }
        },

        LOG4J2 {
            @Override
            long timedLoop(final Logger logger, final org.slf4j.Logger logbacklogger, final int loop) {
                final Integer j = Integer.valueOf(2);
                final long start = System.nanoTime();
                for (int i = 0; i < loop; i++) {
                    logger.debug("SEE IF THIS IS LOGGED {}.", j);
                }
                return (System.nanoTime() - start) / loop;
            }
        };
        abstract long timedLoop(final Logger logger, final org.slf4j.Logger logbacklogger, final int loop);
    }

    private class Worker extends Thread {

        private final Target target;
        private final int count;
        private final long[] results;
        private final int index;
        private final Map<String, String> contextData;

        public Worker(final Target target, final int count, final long[] results, final int index,
                final Map<String, String> contextData) {
            this.target = target;
            this.count = count;
            this.results = results;
            this.index = index;
            this.contextData = contextData;
        }

        @Override
        public void run() {
            putContextData(contextData);
            results[index] = target.timedLoop(logger, logbacklogger, count);
        }
    }

}