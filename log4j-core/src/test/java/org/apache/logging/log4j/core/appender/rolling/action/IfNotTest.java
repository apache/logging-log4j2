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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Tests the Not composite condition.
 */
public class IfNotTest {

    @Test
    public void test() {
        assertThat(new FixedCondition(true).accept(null, null, null)).isTrue();
        assertThat(IfNot.createNotCondition(new FixedCondition(true)).accept(null, null, null)).isFalse();

        assertThat(new FixedCondition(false).accept(null, null, null)).isFalse();
        assertThat(IfNot.createNotCondition(new FixedCondition(false)).accept(null, null, null)).isTrue();
    }

    @Test
    public void testEmptyIsFalse() {
        assertThatThrownBy(() -> IfNot.createNotCondition(null).accept(null, null, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testBeforeTreeWalk() {
        final CountingCondition counter = new CountingCondition(true);
        final IfNot not = IfNot.createNotCondition(counter);
        not.beforeFileTreeWalk();
        assertThat(counter.getBeforeFileTreeWalkCount()).isEqualTo(1);
    }

}
