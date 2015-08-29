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

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.categories.PerformanceTests;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 *
 */
@Category(PerformanceTests.class)
public class ThreadedTest {
    private static final String DIR = "target/threaded";
    private static final String CONFIG = "log4j-threaded.xml";
    private static final int LOOP_CNT = 25;
    private static final int THREADS = 4;
    private static final AtomicInteger counter = new AtomicInteger(0);
    private static final LoggerContextRule context = new LoggerContextRule(CONFIG);

    private final Logger logger = context.getLogger(ThreadedTest.class.getName());
    private volatile Level lvl = Level.DEBUG;

    // this would look pretty sweet with lambdas
    @ClassRule
    public static RuleChain chain = RuleChain.outerRule(new TestRule() {
        @Override
        public Statement apply(final Statement base, final Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    deleteDir();
                    try {
                        base.evaluate();
                    } finally {
                        deleteDir();
                    }
                }
            };
        }
    }).around(context);

    @Test
    public void testDeadlock() throws Exception {
        final ExecutorService pool = Executors.newFixedThreadPool(THREADS * 2);
        final State state = new State();
        for (int count=0; count < THREADS; ++count) {
            pool.execute(new LoggingRunnable(state));
            pool.execute(new StateSettingRunnable(state));
        }
        Thread.sleep(250);
        pool.shutdown();
        System.out.println("Counter = " + counter);
    }

    public class LoggingRunnable implements Runnable {
        private final State state;

        public LoggingRunnable(final State state) {
            this.state = state;
        }
        @Override
        public void run() {
            for (int i=0; i < LOOP_CNT; ++i) {
                logger.debug(state);
            }
        }
    }
    public class StateSettingRunnable implements Runnable {
        private final State state;

        public StateSettingRunnable(final State state) {
            this.state = state;
        }
        @Override
        public void run() {
            for (int i=0; i < LOOP_CNT*4; ++i) {
                Thread.yield();
                state.setState();
            }
        }
    }

    private static void deleteDir() {
        final File dir = new File(DIR);
        if (dir.exists()) {
            final File[] files = dir.listFiles();
            for (final File file : files) {
                file.delete();
            }
            dir.delete();
        }
    }

    class State {

        synchronized void setState() {
            // Something takes a long time here
            logger.debug("hello world");
        }

        synchronized Object getState() {
            return counter.incrementAndGet();
        }

        @Override
        public String toString() {
            return "state=" + getState();
        }
    }
}
