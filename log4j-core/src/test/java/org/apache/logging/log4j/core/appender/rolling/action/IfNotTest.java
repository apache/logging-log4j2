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

import org.apache.logging.log4j.core.appender.rolling.action.IfNot;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests the Not composite condition.
 */
public class IfNotTest {

    @Test
    public void test() {
        assertTrue(new FixedCondition(true).accept(null, null, null));
        assertFalse(IfNot.createNotCondition(new FixedCondition(true)).accept(null, null, null));

        assertFalse(new FixedCondition(false).accept(null, null, null));
        assertTrue(IfNot.createNotCondition(new FixedCondition(false)).accept(null, null, null));
    }

    @Test(expected = NullPointerException.class)
    public void testEmptyIsFalse() {
        assertFalse(IfNot.createNotCondition(null).accept(null, null, null));
    }

    @Test
    public void testBeforeTreeWalk() {
        final CountingCondition counter = new CountingCondition(true);
        final IfNot not = IfNot.createNotCondition(counter);
        not.beforeFileTreeWalk();
        assertEquals(1, counter.getBeforeFileTreeWalkCount());
    }

}
