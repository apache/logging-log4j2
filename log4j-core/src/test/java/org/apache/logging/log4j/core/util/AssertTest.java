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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.*;

/**
 *
 */
@RunWith(Parameterized.class)
public class AssertTest {

    private final Object value;
    private final boolean isEmpty;

    @Parameterized.Parameters
    public static Object[][] data() {
        return new Object[][]{
            // value, isEmpty
            {null, true},
            {"", true},
            {new Object[0], true},
            {new ArrayList<>(), true},
            {new HashMap<>(), true},
            {0, false},
            {1, false},
            {false, false},
            {true, false},
            {new Object[]{null}, false},
            {Collections.singletonList(null), false},
            {Collections.singletonMap("", null), false},
            {"null", false}
        };
    }

    public AssertTest(final Object value, final boolean isEmpty) {
        this.value = value;
        this.isEmpty = isEmpty;
    }

    @Test
    public void isEmpty() throws Exception {
        assertEquals(isEmpty, Assert.isEmpty(value));
    }

}