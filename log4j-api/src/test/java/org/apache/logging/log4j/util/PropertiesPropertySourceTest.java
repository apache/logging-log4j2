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
import java.util.Properties;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class PropertiesPropertySourceTest {

    private final PropertySource source = new PropertiesPropertySource(new Properties());
    private final CharSequence expected;
    private final List<? extends CharSequence> tokens;

    public PropertiesPropertySourceTest(final String expected, final List<CharSequence> tokens) {
        this.expected = expected;
        this.tokens = tokens;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] data() {
        return new Object[][]{
            {"log4j2.configurationFile", Arrays.asList("configuration", "file")},
            {"log4j2.fooBarProperty", Arrays.asList("foo", "bar", "property")},
            {"log4j2.EXACT", Collections.singletonList("EXACT")},
            {"log4j2.testPropertyName", PropertySource.Util.tokenize("Log4jTestPropertyName")},
        };
    }

    @Test
    public void testNormalFormFollowsCamelCaseConventions() throws Exception {
        assertEquals(expected, source.getNormalForm(tokens));
    }
}