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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.junit.JndiRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * JndiLookupTest
 */
public class JndiLookupTest {

    private static final String TEST_CONTEXT_RESOURCE_NAME = "logging/context-name";
    private static final String TEST_CONTEXT_NAME = "app-1";
    private static final String TEST_INTEGRAL_NAME = "int-value";
    private static final int TEST_INTEGRAL_VALUE = 42;
    private static final String TEST_STRINGS_NAME = "string-collection";
    private static final Collection<String> TEST_STRINGS_COLLECTION = Arrays.asList("one", "two", "three");

    @Rule
    public JndiRule jndiRule = new JndiRule(createBindings());

    private Map<String, Object> createBindings() {
        final Map<String, Object> map = new HashMap<>();
        map.put(JndiLookup.CONTAINER_JNDI_RESOURCE_PATH_PREFIX + TEST_CONTEXT_RESOURCE_NAME, TEST_CONTEXT_NAME);
        map.put(JndiLookup.CONTAINER_JNDI_RESOURCE_PATH_PREFIX + TEST_INTEGRAL_NAME, TEST_INTEGRAL_VALUE);
        map.put(JndiLookup.CONTAINER_JNDI_RESOURCE_PATH_PREFIX + TEST_STRINGS_NAME, TEST_STRINGS_COLLECTION);
        return map;
    }

    @Test
    public void testLookup() {
        final StrLookup lookup = new JndiLookup();

        String contextName = lookup.lookup(TEST_CONTEXT_RESOURCE_NAME);
        assertEquals(TEST_CONTEXT_NAME, contextName);

        contextName = lookup.lookup(JndiLookup.CONTAINER_JNDI_RESOURCE_PATH_PREFIX + TEST_CONTEXT_RESOURCE_NAME);
        assertEquals(TEST_CONTEXT_NAME, contextName);

        final String nonExistingResource = lookup.lookup("logging/non-existing-resource");
        assertNull(nonExistingResource);
    }

    @Test
    public void testNonStringLookup() throws Exception {
        // LOG4J2-1310
        final StrLookup lookup = new JndiLookup();
        final String integralValue = lookup.lookup(TEST_INTEGRAL_NAME);
        assertEquals(String.valueOf(TEST_INTEGRAL_VALUE), integralValue);
        final String collectionValue = lookup.lookup(TEST_STRINGS_NAME);
        assertEquals(String.valueOf(TEST_STRINGS_COLLECTION), collectionValue);
    }
}
