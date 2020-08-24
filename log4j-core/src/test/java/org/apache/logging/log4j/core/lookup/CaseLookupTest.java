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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CaseLookupTest {


    @Test
    public void testLookup() {
        final String testStr = "JabberWocky";
        final String lower = "jabberwocky";
        final String upper = "JABBERWOCKY";
        StrLookup lookup = new LowerLookup();
        String value = lookup.lookup(null, testStr);
        assertNotNull(value);
        assertEquals(lower, value);
        lookup = new UpperLookup();
        value = lookup.lookup(null, testStr);
        assertNotNull(value);
        assertEquals(upper, value);
    }
}
