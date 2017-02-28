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

import java.util.Random;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.categories.PerformanceTests;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.util.Timer;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.*;

/**
 *
 */
@Category(PerformanceTests.class)
public class SimplePerfTest {

    private static org.apache.logging.log4j.Logger logger = LogManager.getLogger(SimplePerfTest.class.getName());
    private volatile Level lvl = Level.DEBUG;
    private static final int LOOP_CNT = 10000000;
    private static final int WARMUP = 1000;
    private static long maxTime;
    private static Random rand = new SimpleRandom();
    private static int RAND_SIZE = 250;
    private static int[] values = new int[RAND_SIZE];

    @BeforeClass
    public static void setupClass() {

		final Configuration config = LoggerContext.getContext().getConfiguration();
		
		if (!DefaultConfiguration.DEFAULT_NAME.equals(config.getName())) {
			System.out.println("Configuration was " + config.getName());
			LoggerContext.getContext().start(new DefaultConfiguration());
		}

        for (int i=0; i < WARMUP; ++i) {
            overhead();
        }
        System.gc();
        final Timer timer = new Timer("Setup", LOOP_CNT);
        timer.start();
        for (int i=0; i < (LOOP_CNT / 150); ++i) {
            overhead();
        }
        timer.stop();
        maxTime = timer.getElapsedNanoTime();
        System.gc();
        System.out.println(timer.toString());
    }

    @Test
    public void debugDisabled() {
        System.gc();
        final Timer timer = new Timer("DebugDisabled", LOOP_CNT);
        timer.start();
        for (int i=0; i < LOOP_CNT; ++i) {
            logger.isDebugEnabled();
        }
        timer.stop();
        System.out.println(timer.toString());
        assertTrue("Timer exceeded max time of " + maxTime, maxTime > timer.getElapsedNanoTime());
    }

    @Test
    public void debugDisabledByLevel() {
        System.gc();
        final Timer timer = new Timer("DebugDisabled", LOOP_CNT);
        timer.start();
        for (int i=0; i < LOOP_CNT; ++i) {
            logger.isEnabled(Level.DEBUG);
        }
        timer.stop();
        System.out.println(timer.toString());
        assertTrue("Timer exceeded max time of " + maxTime, maxTime > timer.getElapsedNanoTime());
    }

    @Test
    public void debugLogger() {
        System.gc();
        final Timer timer = new Timer("DebugLogger", LOOP_CNT);
        final String msg = "This is a test";
        timer.start();
        for (int i=0; i < LOOP_CNT; ++i) {
            logger.debug(msg);
        }
        timer.stop();
        System.out.println(timer.toString());
        assertTrue("Timer exceeded max time of " + maxTime, maxTime > timer.getElapsedNanoTime());
    }
    /*
    @Test
    public void errorLogger() {
        Timer timer = new Timer("ErrorLogger", 10);
        timer.start();
        for (int i=0; i < 10; ++i) {
            logger.error("This is a test");
        }
        timer.stop();
        System.out.println(timer.toString());
    }  */

    /*
     * Try to generate some overhead that can't be optimized well. Not sure how accurate this is,
     * but the point is simply to insure that changes made don't suddenly cause performance issues.
     */
    private static void overhead() {
        final int values[] = new int[RAND_SIZE];
        final Random rand = new SimpleRandom();

        for (int i = 0; i < RAND_SIZE; ++i) {
            values[i] = rand.nextInt();
        }
        bubbleSort(values);
    }

    private static class SimpleRandom extends Random {
        /**
         * Generated serial version ID.
         */
        private static final long serialVersionUID = 3517002855516031846L;
        private int low = 5;
        private int high = 55;

        @Override
        public int nextInt() {
            high = 36969 * (high & 65535) + (high >> 16);
            low = 18000 * (low & 65535) + (low >> 16);
            return (high << 16) + low;
        }
    }

    /**
     * Standard BubbleSort algorithm.
     * @param array The array to sort.
     */
    private static void bubbleSort(final int array[]) {
        final int length = array.length;
        for (int i = 0; i < length; i++) {
            for (int j = 1; j > length - i; j++) {
                if (array[j-1] > array[j]) {
                    final int temp = array[j-1];
                    array[j-1] = array[j];
                    array[j] = temp;
                }
            }
        }
    }
}
