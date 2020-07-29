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

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Tests MainLookup.
 */
public class MainLookupTest {

    @Test
    public void testMainArgs(){
        MainMapLookup.setMainArguments("--file", "foo.txt", "--verbose", "-x", "bar");
        String str ="${key} ${main:-1} ${main:0} ${main:1} ${main:2} ${main:3} ${main:4} ${main:\\--file} ${main:foo.txt} ${main:\\--verbose} ${main:\\-x} ${main:bar} ${main:\\--quiet:-true}";
        Map<String, String> properties =  new HashMap<String, String>();
        properties.put("key", "value");
        properties.put("bar", "default_bar_value");
        Interpolator lookup = new Interpolator(properties);
        StrSubstitutor substitutor = new StrSubstitutor(lookup);
        String replacedValue = substitutor.replace(null, str);
        String[] values = replacedValue.split(" ");
        assertEquals("Item 0 is incorrect ", "value", values[0]);
        assertEquals("Item 1 is incorrect ", "1", values[1]);
        assertEquals("Item 2 is incorrect", "--file", values[2]);
        assertEquals("Item 3 is incorrect", "foo.txt", values[3]);
        assertEquals("Item 4 is incorrect", "--verbose", values[4]);
        assertEquals("Item 5 is incorrect", "-x", values[5]);
        assertEquals("Iten 6 is incorrect", "bar", values[6]);
        assertEquals("Item 7 is incorrect", "foo.txt", values[7]);
        assertEquals("Item 8 is incorrect", "--verbose", values[8]);
        assertEquals("Item 9 is incorrect", "-x", values[9]);
        assertEquals("Item 10 is incorrect", "bar", values[10]);
        assertEquals("Item 11 is incorrect", "default_bar_value", values[11]);
        assertEquals("Item 12 is incorrect", "true", values[12]);
    }
}
