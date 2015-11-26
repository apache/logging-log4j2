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

package org.apache.logging.log4j.core.appender.rolling.action;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests the IfAccumulatedFileCount class.
 */
public class IfAccumulatedFileCountTest {

    @Test
    public void testGetThresholdCount() {
        assertEquals(123, IfAccumulatedFileCount.createFileCountCondition(123).getThresholdCount());
        assertEquals(456, IfAccumulatedFileCount.createFileCountCondition(456).getThresholdCount());
    }

    @Test
    public void testAccept() {
        int[] counts = {3, 5, 9};
        for (int count: counts) {
            IfAccumulatedFileCount condition = IfAccumulatedFileCount.createFileCountCondition(count);
            for (int i = 0; i < count; i++) {
                assertFalse(condition.accept(null, null, null));
            }
            assertTrue(condition.accept(null, null, null));
            assertTrue(condition.accept(null, null, null));
        }
    }

}
