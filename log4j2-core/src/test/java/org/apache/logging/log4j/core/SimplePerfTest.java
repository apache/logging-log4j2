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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Assert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class SimplePerfTest {

    private static org.apache.logging.log4j.Logger logger = LogManager.getLogger(SimplePerfTest.class.getName());
    private volatile Level lvl = Level.DEBUG;
    private static final int LOOP_CNT = 100000000;
    private static final int WARMUP = 1000;
    private static long maxTime;

    @BeforeClass
    public static void setupClass() {
        for (int i=0; i < WARMUP; ++i) {
            if (overhead(i, LOOP_CNT)) {
                System.out.println("help!");
            }
        }

        Timer timer = new Timer("Setup", LOOP_CNT);
        timer.start();
        for (int i=0; i < LOOP_CNT; ++i) {
            if (overhead(i, LOOP_CNT)) {
                System.out.println("help!");
            }
        }
        timer.stop();
        maxTime = timer.getElapsedNanoTime();
        System.out.println(timer.toString());
    }

    @Test
    public void debugDisabled() {
        Timer timer = new Timer("DebugDisabled", LOOP_CNT);
        timer.start();
        for (int i=0; i < LOOP_CNT; ++i) {
            logger.isDebugEnabled();
        }
        timer.stop();
        System.out.println(timer.toString());
        assertTrue("Timer exceeded max time of " + maxTime, maxTime > timer.getElapsedNanoTime());
    }

    @Test
    public void debugLogger() {
        Timer timer = new Timer("DebugLogger", LOOP_CNT);
        timer.start();
        for (int i=0; i < LOOP_CNT; ++i) {
            logger.debug("This is a test");
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
    private static boolean overhead(int i, int j) {
        for (int k=j; k < j+12; ++k) {
            if (i > k) {
                return true;
            }
            if (i == k) {
                return true;
            }
            if (i < 0) {
                return true;
            }
        }
        return false;
    }
}
