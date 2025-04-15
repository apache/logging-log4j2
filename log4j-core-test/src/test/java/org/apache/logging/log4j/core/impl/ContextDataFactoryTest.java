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
package org.apache.logging.log4j.core.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.lang.reflect.Field;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.junit.jupiter.api.Test;

/**
 * Tests the ContextDataFactory class.
 */
class ContextDataFactoryTest {
    @Test
    void noArgReturnsSortedArrayStringMapIfNoPropertySpecified() {
        assertInstanceOf(SortedArrayStringMap.class, ContextDataFactory.createContextData());
    }

    @Test
    void intArgReturnsSortedArrayStringMapIfNoPropertySpecified() {
        assertInstanceOf(SortedArrayStringMap.class, ContextDataFactory.createContextData(2));
    }

    @Test
    void intArgSetsCapacityIfNoPropertySpecified() throws Exception {
        final SortedArrayStringMap actual = (SortedArrayStringMap) ContextDataFactory.createContextData(2);
        final Field thresholdField = SortedArrayStringMap.class.getDeclaredField("threshold");
        thresholdField.setAccessible(true);
        assertEquals(2, thresholdField.getInt(actual));
    }
}
