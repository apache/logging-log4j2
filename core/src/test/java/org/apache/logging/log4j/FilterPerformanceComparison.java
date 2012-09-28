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

import org.apache.logging.log4j.core.config.XMLConfigurationFactory;
import org.apache.logging.log4j.core.util.Profiler;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 *
 */
public class FilterPerformanceComparison {

    private Logger logger = LogManager.getLogger(FilterPerformanceComparison.class.getName());
    private org.slf4j.Logger logbacklogger = org.slf4j.LoggerFactory.getLogger(FilterPerformanceComparison.class);


    // How many times should we try to log:
    private static final int COUNT = 10000000;
    private static final int THREADED_COUNT = 100000;
    private static final int WARMUP = 1000;

    private static final String CONFIG = "log4j2-perf-filter.xml";
    private static final String LOGBACK_CONFIG = "logback-perf-filter.xml";

    private static final String LOGBACK_CONF = "logback.configurationFile";

    @BeforeClass
    public static void setupClass() {
        System.setProperty(XMLConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
        System.setProperty(LOGBACK_CONF, LOGBACK_CONFIG);
    }

    @AfterClass
    public static void cleanupClass() {
        System.clearProperty(XMLConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        System.clearProperty(LOGBACK_CONF);
    }

    @Test
    public void testPerformance() throws Exception {
        logback(WARMUP);
        log4j2(WARMUP);

        System.out.println("Starting Log4j 2.0");
        long result3 = log4j2(COUNT);
        System.out.println("Starting Logback");
        long result2 = logback(COUNT);

        System.out.println("###############################################");
        System.out.println("Logback: " + result2);
        System.out.println("Log4j 2.0: " + result3);
        System.out.println("###############################################");
    }

    @Test
    public void testThreads() throws Exception {
        System.out.println("Testing multithreading");
        int threadedCount = COUNT; // THREADED_COUNT * threadCount < COUNT ? COUNT / threadCount : THREADED_COUNT;
        int[] threadCounts = new int[] {1, 2, 5, 10, 20, 50};
        for (int threadCount : threadCounts) {
            System.out.println("Testing " + threadCount + " threads");
            Worker[] workers = new Worker[threadCount];
            long[] results = new long[threadCount];
            for (int i=0; i < threadCount; ++i) {
                workers[i] = new Worker(true, threadedCount, results, i);
            }
            for (int i=0; i < threadCount; ++i) {
                workers[i].start();
            }
            long total = 0;
            for (int i=0; i < threadCount; ++i) {
                workers[i].join();
                total += results[i];
            }
            long result3 = total / threadCount;
            total = 0;
            for (int i=0; i < threadCount; ++i) {
                workers[i] = new Worker(false, threadedCount, results, i);
            }
            for (int i=0; i < threadCount; ++i) {
                workers[i].start();
            }
            for (int i=0; i < threadCount; ++i) {
                workers[i].join();
                total += results[i];
            }
            long result2 = total / threadCount;
            System.out.println("###############################################");
            System.out.println("Logback: " + result2);
            System.out.println("Log4j 2.0: " + result3 );
            System.out.println("###############################################");
        }

    }

    private long logback(int loop) {
        Integer j = new Integer(2);
        long start = System.nanoTime();
        for (int i = 0; i < loop; i++) {
            logbacklogger.debug("SEE IF THIS IS LOGGED {}.", j);
        }
        return (System.nanoTime() - start) / loop;
    }


    private long log4j2(int loop) {
        Integer j = new Integer(2);
        long start = System.nanoTime();
        for (int i = 0; i < loop; i++) {
            logger.debug("SEE IF THIS IS LOGGED {}.", j);
        }
        return (System.nanoTime() - start) / loop;
    }

    private class Worker extends Thread {

        private boolean isLog4j;
        private int count;
        private long[] results;
        private int index;

        public Worker(boolean isLog4j, int count, long[] results, int index) {
            this.isLog4j = isLog4j;
            this.count = count;
            this.results = results;
            this.index = index;
        }

        @Override
        public void run() {
            results[index] = isLog4j ? log4j2(count) : logback(count);
        }
    }

}