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
package org.apache.logging.log4j.core.impl;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the ContextDataFactory class.
 */
@SetSystemProperty(key = Log4jProperties.THREAD_CONTEXT_DATA_CLASS_NAME, value = "org.apache.logging.log4j.core.impl.FactoryTestStringMap")
public class ContextDataFactoryPropertySetTest {

    @Test
    public void noArgReturnsSpecifiedImplIfPropertySpecified() throws Exception {
        assertTrue(ContextDataFactory.createContextData() instanceof FactoryTestStringMap);
    }

    @Test
    public void intArgReturnsSpecifiedImplIfPropertySpecified() throws Exception {
        assertTrue(ContextDataFactory.createContextData(2) instanceof FactoryTestStringMap);
    }

    @Test
    public void intArgSetsCapacityIfPropertySpecified() throws Exception {
        final FactoryTestStringMap actual = (FactoryTestStringMap) ContextDataFactory.createContextData(2);
        assertEquals(2, actual.initialCapacity);
    }
}
