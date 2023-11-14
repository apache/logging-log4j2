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
package org.apache.logging.log4j.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PropertySourceTokenizerTest {

    public static Object[][] data() {
        return new Object[][]{
            {"log4j.simple", Arrays.asList("log4j", "simple")},
            {"log4j_simple", Arrays.asList("log4j", "simple")},
            {"log4j-simple", Collections.singletonList("log4j-simple")},
            {"log4j/simple", Arrays.asList("log4j", "simple")},
            {"log4j2.simple", Arrays.asList("log4j2", "simple")},
            {"Log4jSimple", Collections.singletonList("Log4jSimple")},
            {"LOG4J_simple", Arrays.asList("LOG4J", "simple")},
            {"org.apache.logging.log4j.simple", Arrays.asList("org", "apache", "logging", "log4j", "simple")},
            {"log4j.simpleProperty", Arrays.asList("log4j", "simpleProperty")},
            {"log4j.simple_property", Arrays.asList("log4j", "simple", "property")},
            {"LOG4J_simple_property", Arrays.asList("LOG4J", "simple", "property")},
            {"LOG4J_SIMPLE_PROPERTY", Arrays.asList("LOG4J", "SIMPLE", "PROPERTY")},
            {"log4j2-dashed-propertyName", Collections.singletonList("log4j2-dashed-propertyName")},
            {"Log4jProperty_with.all-the/separators", Arrays.asList("Log4jProperty", "with", "all-the", "separators")},
            {"org.apache.logging.log4j.config.property", Arrays.asList("org", "apache", "logging", "log4j", "config", "property")},
            {"level", Collections.singletonList("level")},
            {"user.home", Arrays.asList("user", "home")},
            {"CATALINA_BASE", Arrays.asList("CATALINA", "BASE")}
        };
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testTokenize(final CharSequence value, final List<CharSequence> expectedTokens) {
        final List<CharSequence> tokens = PropertySource.Util.tokenize(value);
        assertEquals(expectedTokens, tokens);
    }
}
