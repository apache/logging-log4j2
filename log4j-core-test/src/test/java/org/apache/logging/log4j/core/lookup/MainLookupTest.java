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

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests MainLookup.
 */
public class MainLookupTest {

    @Test
    public void testMainArgs() {
        MainMapLookup.setMainArguments("--file", "foo.txt", "--verbose", "-x", "bar");
        final String str =
                "${key} ${main:-1} ${main:0} ${main:1} ${main:2} ${main:3} ${main:4} ${main:\\--file} ${main:foo.txt} ${main:\\--verbose} ${main:\\-x} ${main:bar} ${main:\\--quiet:-true}";
        final Map<String, String> properties = new HashMap<>();
        properties.put("key", "value");
        properties.put("bar", "default_bar_value");
        final Interpolator lookup = new Interpolator(properties);
        final StrSubstitutor substitutor = new StrSubstitutor(lookup);
        final String replacedValue = substitutor.replace(null, str);
        final String[] values = replacedValue.split(" ");
        assertEquals("value", values[0], "Item 0 is incorrect ");
        assertEquals("1", values[1], "Item 1 is incorrect ");
        assertEquals("--file", values[2], "Item 2 is incorrect");
        assertEquals("foo.txt", values[3], "Item 3 is incorrect");
        assertEquals("--verbose", values[4], "Item 4 is incorrect");
        assertEquals("-x", values[5], "Item 5 is incorrect");
        assertEquals("bar", values[6], "Iten 6 is incorrect");
        assertEquals("foo.txt", values[7], "Item 7 is incorrect");
        assertEquals("--verbose", values[8], "Item 8 is incorrect");
        assertEquals("-x", values[9], "Item 9 is incorrect");
        assertEquals("bar", values[10], "Item 10 is incorrect");
        assertEquals("default_bar_value", values[11], "Item 11 is incorrect");
        assertEquals("true", values[12], "Item 12 is incorrect");
    }
}
