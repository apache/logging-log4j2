/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SystemClockTest {

    @Test
    void testLessThan2Millis() {
        final long millis1 = new SystemClock().currentTimeMillis();
        final long sysMillis = System.currentTimeMillis();

        final long diff = sysMillis - millis1;

        assertTrue(diff <= 1, "diff too large: " + diff);
    }

    @Test
    void testAfterWaitStillLessThan2Millis() throws Exception {
        Thread.sleep(100);
        final long millis1 = new SystemClock().currentTimeMillis();
        final long sysMillis = System.currentTimeMillis();

        final long diff = sysMillis - millis1;

        assertTrue(diff <= 1, "diff too large: " + diff);
    }
}
