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
package org.apache.logging.log4j.core.lookup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link PropertiesLookup}.
 */
public class PropertiesLookupTest {

    @Test
    public void testLookupContextProperty() {
        final StrLookup propertiesLookup =
                new PropertiesLookup(Property.EMPTY_ARRAY, Collections.singletonMap("A", "1"));
        assertEquals("1", propertiesLookup.lookup("A"));
        final LookupResult lookupResult = propertiesLookup.evaluate("A");
        assertEquals("1", lookupResult.value());
        assertFalse(lookupResult.isLookupEvaluationAllowedInValue());
    }

    @Test
    public void testLookupConfigProperty() {
        final StrLookup propertiesLookup =
                new PropertiesLookup(new Property[] {Property.createProperty("A", "1")}, Collections.emptyMap());
        assertEquals("1", propertiesLookup.lookup("A"));
        final LookupResult lookupResult = propertiesLookup.evaluate("A");
        assertEquals("1", lookupResult.value());
        assertTrue(lookupResult.isLookupEvaluationAllowedInValue());
    }

    @Test
    public void testConfigPropertiesPreferredOverContextProperties() {
        final StrLookup propertiesLookup = new PropertiesLookup(
                new Property[] {Property.createProperty("A", "1")}, Collections.singletonMap("A", "2"));
        assertEquals("1", propertiesLookup.lookup("A"));
        final LookupResult lookupResult = propertiesLookup.evaluate("A");
        assertEquals("1", lookupResult.value());
        assertTrue(lookupResult.isLookupEvaluationAllowedInValue());
    }

    @Test
    public void testEvaluateResultsSupportRecursiveEvaluation() {
        final PropertiesLookup lookup = new PropertiesLookup(Collections.singletonMap("key", "value"));
        assertFalse(lookup.evaluate("key").isLookupEvaluationAllowedInValue());
    }

    @Test
    public void testEvaluateReturnsNullWhenKeyIsNotFound() {
        final PropertiesLookup lookup = new PropertiesLookup(Collections.emptyMap());
        assertNull(lookup.evaluate("key"));
    }

    @Test
    public void testEvaluateReturnsNullWhenKeyIsNull() {
        final PropertiesLookup lookup = new PropertiesLookup(Collections.emptyMap());
        assertNull(lookup.evaluate(null));
    }

    @Test
    public void testContextPropertiesAreMutable() {
        final Map<String, String> contextProperties = new HashMap<>();
        final PropertiesLookup lookup = new PropertiesLookup(Property.EMPTY_ARRAY, contextProperties);
        assertNull(lookup.evaluate("key"));
        contextProperties.put("key", "value");
        final LookupResult result = lookup.evaluate("key");
        assertEquals("value", result.value());
        assertFalse(result.isLookupEvaluationAllowedInValue());
    }
}
