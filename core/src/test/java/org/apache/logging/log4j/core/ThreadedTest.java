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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.XMLConfigurationFactory;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 */
public class ThreadedTest {
    private static final String DIR = "target/threaded";
    private static final String CONFIG = "log4j-threaded.xml";
    private Logger logger = LogManager.getLogger(ThreadedTest.class.getName());
    private volatile Level lvl = Level.DEBUG;
    private static final int LOOP_CNT = 25;
    private static final int THREADS = 4;
    private static int counter = 0;

    @BeforeClass
    public static void setupClass() {
        deleteDir();
        System.setProperty(XMLConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
        LoggerContext ctx = (LoggerContext) LogManager.getContext();
        Configuration config = ctx.getConfiguration();
    }

    @AfterClass
    public static void cleanupClass() {
        deleteDir();
        System.clearProperty(XMLConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        LoggerContext ctx = (LoggerContext) LogManager.getContext();
        ctx.reconfigure();
        StatusLogger.getLogger().reset();
    }

    @Test
    public void testDeadlock() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS * 2);
        State state = new State();
        for (int count=0; count < THREADS; ++count) {
            pool.execute(new LoggingRunnable(state));
            pool.execute(new StateSettingRunnable(state));
        }
        Thread.sleep(250);
        pool.shutdown();
        System.out.println("Counter = " + counter);
    }

    public class LoggingRunnable implements Runnable {
        private State state;

        public LoggingRunnable(State state) {
            this.state = state;
        }
        public void run() {
            for (int i=0; i < LOOP_CNT; ++i) {
                logger.debug(state);
            }
        }
    }
    public class StateSettingRunnable implements Runnable {
        private State state;

        public StateSettingRunnable(State state) {
            this.state = state;
        }
        public void run() {
            for (int i=0; i < LOOP_CNT*4; ++i) {
                Thread.yield();
                state.setState();
            }
        }
    }

    private static void deleteDir() {
        File dir = new File(DIR);
        if (dir.exists()) {
            File[] files = dir.listFiles();
            for (File file : files) {
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