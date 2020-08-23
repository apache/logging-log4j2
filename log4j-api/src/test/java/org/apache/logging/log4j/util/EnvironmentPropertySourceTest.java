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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EnvironmentPropertySourceTest {

    private final PropertySource source = new EnvironmentPropertySource();

    public static Object[][] data() {
        return new Object[][]{
            {"LOG4J_CONFIGURATION_FILE", Arrays.asList("configuration", "file")},
            {"LOG4J_FOO_BAR_PROPERTY", Arrays.asList("foo", "bar", "property")},
            {"LOG4J_EXACT", Collections.singletonList("EXACT")},
            {"LOG4J_TEST_PROPERTY_NAME", PropertySource.Util.tokenize("Log4jTestPropertyName")},
        };
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testNormalFormFollowsEnvironmentVariableConventions(CharSequence expected, List<? extends CharSequence> tokens) {
        assertEquals(expected, source.getNormalForm(tokens));
    }
}