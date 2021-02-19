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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Tests the IfAccumulatedFileCount class.
 */
public class IfAccumulatedFileCountTest {

    @Test
    public void testGetThresholdCount() {
        assertThat(IfAccumulatedFileCount.createFileCountCondition(123).getThresholdCount()).isEqualTo(123);
        assertThat(IfAccumulatedFileCount.createFileCountCondition(456).getThresholdCount()).isEqualTo(456);
    }

    @Test
    public void testAccept() {
        final int[] counts = {3, 5, 9};
        for (final int count : counts) {
            final IfAccumulatedFileCount condition = IfAccumulatedFileCount.createFileCountCondition(count);
            for (int i = 0; i < count; i++) {
                assertThat(condition.accept(null, null, null)).isFalse();
                // exact match: does not accept
            }
            // accept when threshold is exceeded
            assertThat(condition.accept(null, null, null)).isTrue();
            assertThat(condition.accept(null, null, null)).isTrue();
        }
    }

    @Test
    public void testAcceptCallsNestedConditionsOnlyIfPathAccepted() {
        final CountingCondition counter = new CountingCondition(true);
        final IfAccumulatedFileCount condition = IfAccumulatedFileCount.createFileCountCondition(3, counter);

        for (int i = 1; i < 10; i++) {
            if (i <= 3) {
                assertFalse(condition.accept(null, null, null), "i=" + i);
                assertThat(counter.getAcceptCount()).isEqualTo(0);
            } else {
                assertThat(condition.accept(null, null, null)).isTrue();
                assertThat(counter.getAcceptCount()).isEqualTo(i - 3);
            }
        }
    }

    @Test
    public void testBeforeTreeWalk() {
        final CountingCondition counter = new CountingCondition(true);
        final IfAccumulatedFileCount filter = IfAccumulatedFileCount.createFileCountCondition(30, counter, counter,
                counter);
        filter.beforeFileTreeWalk();
        assertThat(counter.getBeforeFileTreeWalkCount()).isEqualTo(3);
    }

}
