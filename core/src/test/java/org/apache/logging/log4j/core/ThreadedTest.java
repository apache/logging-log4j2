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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
public class ThreadedTest {
    private static final String DIR = "target/threaded";
    private static final String CONFIG = "log4j-threaded.xml";
    private final Logger logger = LogManager.getLogger(ThreadedTest.class.getName());
    private volatile Level lvl = Level.DEBUG;
    private static final int LOOP_CNT = 25;
    private static final int THREADS = 4;
    private static int counter = 0;

    @BeforeClass
    public static void setupClass() {
        deleteDir();
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
        final LoggerContext ctx = (LoggerContext) LogManager.getContext();
        final Configuration config = ctx.getConfiguration();
    }

    @AfterClass
    public static void cleanupClass() {
        deleteDir();
        System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        final LoggerContext ctx = (LoggerContext) LogManager.getContext();
        ctx.reconfigure();
        StatusLogger.getLogger().reset();
    }

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
            ++counter;
            return counter;
        }

        @Override
        public String toString() {
            return "state=" + getState();
        }
    }
}