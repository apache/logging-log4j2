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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

public class PathConditionTest {

    private static final PathCondition[] EMPTY_FIXTURE = {};
    private static final PathCondition[] NULL_FIXTURE = null;

    @Test
    public void testCopy() {
        assertArrayEquals(EMPTY_FIXTURE, PathCondition.copy(NULL_FIXTURE));
        assertArrayEquals(EMPTY_FIXTURE, PathCondition.copy(EMPTY_FIXTURE));
        assertArrayEquals(EMPTY_FIXTURE, PathCondition.copy(PathCondition.EMPTY_ARRAY));
        assertSame(PathCondition.EMPTY_ARRAY, PathCondition.copy(PathCondition.EMPTY_ARRAY));
        assertSame(PathCondition.EMPTY_ARRAY, PathCondition.copy(NULL_FIXTURE));
        assertSame(PathCondition.EMPTY_ARRAY, PathCondition.copy(EMPTY_FIXTURE));
        //
        final CountingCondition cc = new CountingCondition(true);
        assertNotSame(cc, PathCondition.copy(cc));
    }
}
