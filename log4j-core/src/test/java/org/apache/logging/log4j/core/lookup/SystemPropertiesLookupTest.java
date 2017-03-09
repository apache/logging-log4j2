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
package org.apache.logging.log4j.core.lookup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 *
 */
public class SystemPropertiesLookupTest {

    private final StrLookup lookup = new SystemPropertiesLookup();

    @Test
    public void returnsNullForNonExistingProperty() {
        System.clearProperty("log4j.testkey");

        String value = lookup.lookup("log4j.testkey");

        assertNull(value);
    }

    @Test
    public void returnsPropertyByKey() {
        try {
            System.setProperty("log4j.testkey", "testvalue");

            String value = lookup.lookup("log4j.testkey");

            assertEquals("testvalue", value);
        } finally {
            System.clearProperty("log4j.testkey");
        }
    }

    @Test
    public void returnsFirstExistingPropertyBy2ElementExpression() {
        try {
            System.setProperty("log4j.testkey", "testvalue");

            String value = lookup.lookup("log4j.testkey|default");

            assertEquals("testvalue", value);
        } finally {
            System.clearProperty("log4j.testkey");
        }
    }

    @Test
    public void returnsFirstExistingPropertyBy3ElementExpression() {
        try {
            System.clearProperty("log4j.testkey1");
            System.setProperty("log4j.testkey2", "testvalue2");

            String value = lookup.lookup("log4j.testkey1|log4j.testkey2|default");

            assertEquals("testvalue2", value);
        } finally {
            System.clearProperty("log4j.testkey2");
        }
    }

    @Test
    public void returnsDefaultValueByExpressionIfNoPropertiesExist() {
        System.clearProperty("log4j.testkey1");
        System.clearProperty("log4j.testkey2");

        String value = lookup.lookup("log4j.testkey1|log4j.testkey2|default");

        assertEquals("default", value);
    }
}
