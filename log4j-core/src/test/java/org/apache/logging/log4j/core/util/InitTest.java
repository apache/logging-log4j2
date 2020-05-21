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
package org.apache.logging.log4j.core.util;

import static org.junit.Assert.assertTrue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Timer;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test initialization.
 */
@Ignore
public class InitTest {

    private static final String KEY = InitTest.class.getSimpleName() + ".threshold";

    @Test
    public void initTest() {
        Timer timer = new Timer("Log4j Initialization");
        timer.start();
        Logger logger = LogManager.getLogger();
        timer.stop();
        long elapsed = timer.getElapsedNanoTime();
        System.out.println(timer.toString());
        long threshold = Long.getLong(KEY, 1_000_000_000);
        assertTrue(
                String.format("Initialization time exceeded %s %,d; elapsed %,d nanoseconds", KEY, threshold, elapsed),
                elapsed < threshold);
    }
}
