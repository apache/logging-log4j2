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
package org.apache.logging.log4j.core.appender.rolling.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests the IfAccumulatedFileCount class.
 */
class IfAccumulatedFileCountTest {

    @Test
    void testGetThresholdCount() {
        assertEquals(123, IfAccumulatedFileCount.createFileCountCondition(123).getThresholdCount());
        assertEquals(456, IfAccumulatedFileCount.createFileCountCondition(456).getThresholdCount());
    }

    @Test
    void testAccept() {
        final int[] counts = {3, 5, 9};
        for (final int count : counts) {
            final IfAccumulatedFileCount condition = IfAccumulatedFileCount.createFileCountCondition(count);
            for (int i = 0; i < count; i++) {
                assertFalse(condition.accept(null, null, null));
                // exact match: does not accept
            }
            // accept when threshold is exceeded
            assertTrue(condition.accept(null, null, null));
            assertTrue(condition.accept(null, null, null));
        }
    }

    @Test
    void testAcceptCallsNestedConditionsOnlyIfPathAccepted() {
        final CountingCondition counter = new CountingCondition(true);
        final IfAccumulatedFileCount condition = IfAccumulatedFileCount.createFileCountCondition(3, counter);

        for (int i = 1; i < 10; i++) {
            if (i <= 3) {
                assertFalse(condition.accept(null, null, null), "i=" + i);
                assertEquals(0, counter.getAcceptCount());
            } else {
                assertTrue(condition.accept(null, null, null));
                assertEquals(i - 3, counter.getAcceptCount());
            }
        }
    }

    @Test
    void testBeforeTreeWalk() {
        final CountingCondition counter = new CountingCondition(true);
        final IfAccumulatedFileCount filter =
                IfAccumulatedFileCount.createFileCountCondition(30, counter, counter, counter);
        filter.beforeFileTreeWalk();
        assertEquals(3, counter.getBeforeFileTreeWalkCount());
    }
}
