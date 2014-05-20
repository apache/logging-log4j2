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

import org.junit.Test;

import static org.junit.Assert.*;

public class SystemClockTest {

    @Test
    public void testLessThan2Millis() {
        final long millis1 = new SystemClock().currentTimeMillis();
        final long sysMillis = System.currentTimeMillis();

        final long diff = sysMillis - millis1;

        assertTrue("diff too large: " + diff, diff <= 1);
    }

    @Test
    public void testAfterWaitStillLessThan2Millis() throws Exception {
        Thread.sleep(100);
        final long millis1 = new SystemClock().currentTimeMillis();
        final long sysMillis = System.currentTimeMillis();

        final long diff = sysMillis - millis1;

        assertTrue("diff too large: " + diff, diff <= 1);
    }

}
