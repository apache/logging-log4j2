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
package org.apache.logging.log4j.core.config;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.message.ThreadDumpMessage;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class ReconfigurationDeadlockTest {

    @Rule
    public LoggerContextRule init = new LoggerContextRule("reconfiguration-deadlock.xml");
    private static final int THREAD_COUNT = 5;
    private static final boolean[] finished = new boolean[THREAD_COUNT];
    private static LoggerThread[] threads = new LoggerThread[THREAD_COUNT];

    @Test
    public void testReconfig() throws InterruptedException {

        final Updater updater = new Updater();
        for (int i = 0; i < THREAD_COUNT; ++i) {
            threads[i] = new LoggerThread(i);
            threads[i].setDaemon(true);
        }
        for (int i = 0; i < THREAD_COUNT; ++i) {

            threads[i].start();
        }
        updater.setDaemon(true);
        updater.start();
        Thread.sleep(100);
        boolean stillWaiting = true;
        for (int i = 0; i < 200; ++i) {
            int index = 0;
            for (; index < THREAD_COUNT; ++index) {
                if (!finished[index]) {
                    break;
                }
            }
            if (index == THREAD_COUNT) {
                stillWaiting = false;
                break;
            }
            Thread.sleep(100);
        }
        updater.shutdown = true;
        if (stillWaiting) {
            final ThreadDumpMessage message = new ThreadDumpMessage("Waiting");
            System.err.print(message.getFormattedMessage());
        }
        for (int i = 0; i < THREAD_COUNT; ++i) {
            if (threads[i].isAlive()) {
                threads[i].interrupt();
            }
        }
        assertFalse("loggerThread didn't finish", stillWaiting);

    }

    private class LoggerThread extends Thread {

        private final Logger logger = LogManager.getRootLogger();
        private final int index;

        public LoggerThread(final int i) {
            index = i;
        }
        @Override
        public void run() {
            int i = 0;
            try {
                for (i=0; i < 30; ++i) {
                    logger.error("Thread: " + index + ", Test: " + i++);
                }
            } catch (final Exception ie) {
                return;
            }
            finished[index] = true;
        }
    }

    private class Updater extends Thread {

        public volatile boolean shutdown = false;

        @Override
        public void run() {
            while (!shutdown) {
                try {
                    Thread.sleep(1000);
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                }
                // for running from IDE
                final File file = new File("target/test-classes/reconfiguration-deadlock.xml");
                if (file.exists()) {
                    file.setLastModified(System.currentTimeMillis());
                }
            }
        }
    }

}
