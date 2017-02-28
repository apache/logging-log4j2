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
package org.apache.logging.log4j.core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.categories.PerformanceTests;
import org.apache.logging.log4j.core.util.Timer;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 *
 */
@Category(PerformanceTests.class)
public class ThreadedPerfTest {

    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(ThreadedPerfTest.class.getName());
    private volatile Level lvl = Level.DEBUG;
    private static final int LOOP_CNT = 10000000;
    private static final int THREADS = 10;

    @Test
    public void debugDisabled() {
        final Timer timer = new Timer("DebugDisabled", LOOP_CNT * THREADS);
        final Runnable runnable = new DebugDisabledRunnable();
        final ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        timer.start();
        for (int i=0; i < THREADS; ++i) {
            pool.execute(runnable);
        }
        pool.shutdown();
        timer.stop();
        System.out.println(timer.toString());
    }

    @Test
    public void debugLogger() {
        final Timer timer = new Timer("DebugLogger", LOOP_CNT * THREADS);
        final Runnable runnable = new DebugLoggerRunnable();
        final ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        timer.start();
        for (int i=0; i < THREADS; ++i) {
            pool.execute(runnable);
        }
        pool.shutdown();
        timer.stop();
        System.out.println(timer.toString());
    }

    public static class DebugDisabledRunnable implements Runnable {
        @Override
        public void run() {
            for (int i=0; i < LOOP_CNT; ++i) {
                logger.isDebugEnabled();
            }
        }
    }

     public static class DebugLoggerRunnable implements Runnable {
        @Override
        public void run() {
            for (int i=0; i < LOOP_CNT; ++i) {
                logger.debug("This is a test");
            }
        }
    }
}
