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
package org.apache.logging.log4j.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class PropertySourceCamelCaseTest {

    private final CharSequence expected;
    private final List<String> tokens;

    public PropertySourceCamelCaseTest(final CharSequence expected, final List<String> tokens) {
        this.expected = expected;
        this.tokens = tokens;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] data() {
        return new Object[][]{
            {"", Collections.singletonList("")},
            {"foo", Collections.singletonList("foo")},
            {"fooBar", Arrays.asList("foo", "bar")},
            {"oneTwoThree", Arrays.asList("one", "two", "three")},
        };
    }

    @Test
    public void testJoinAsCamelCase() throws Exception {
        assertEquals(expected, PropertySource.Util.joinAsCamelCase(tokens));
    }
}