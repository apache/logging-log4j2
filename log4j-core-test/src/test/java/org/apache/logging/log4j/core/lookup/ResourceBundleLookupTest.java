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
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ResourceBundleLookupTest {

    @Test
    void testLookup() {
        final StrLookup lookup = new ResourceBundleLookup();
        lookup.lookup("org.apache.logging.log4j.core.lookup.resource-bundle_en:KeyA");
        assertEquals("ValueA", lookup.lookup("org.apache.logging.log4j.core.lookup.resource-bundle:KeyA"));
    }

    @Test
    void testLookupWithLocale() {
        final StrLookup lookup = new ResourceBundleLookup();
        lookup.lookup("org.apache.logging.log4j.core.lookup.resource-bundle:KeyA");
        assertEquals("ValueA", lookup.lookup("org.apache.logging.log4j.core.lookup.resource-bundle:KeyA"));
    }

    @Test
    void testMissingKey() {
        final StrLookup lookup = new ResourceBundleLookup();
        assertNull(lookup.lookup("org.apache.logging.log4j.core.lookup.resource-bundle:KeyUnkown"));
    }

    @Test
    void testBadFormatBundleOnly() {
        final StrLookup lookup = new ResourceBundleLookup();
        assertNull(lookup.lookup("X"));
    }
}
