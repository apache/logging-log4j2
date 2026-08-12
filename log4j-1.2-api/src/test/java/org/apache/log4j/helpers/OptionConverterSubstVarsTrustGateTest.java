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
package org.apache.log4j.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OptionConverterSubstVarsTrustGateTest {

    private static final String STRICTNESS_PROPERTY = "log4j2.trustgate.strictness";
    private static final String STRICT_PROPERTY = "log4j2.trustgate.strict";

    private String previousStrictnessProperty;
    private String previousStrictProperty;

    @BeforeEach
    void setUp() {
        previousStrictnessProperty = System.getProperty(STRICTNESS_PROPERTY);
        previousStrictProperty = System.getProperty(STRICT_PROPERTY);
        System.clearProperty(STRICTNESS_PROPERTY);
        System.clearProperty(STRICT_PROPERTY);
    }

    @AfterEach
    void tearDown() {
        restoreProperty(STRICTNESS_PROPERTY, previousStrictnessProperty);
        restoreProperty(STRICT_PROPERTY, previousStrictProperty);
    }

    @Test
    void substVarsResolvesValidVariable() {
        final Properties props = new Properties();
        props.setProperty("home", "/tmp/home");
        assertEquals("/tmp/home", OptionConverter.substVars("${home}", props));
    }

    @Test
    void substVarsRejectsNestedVariableExpression() {
        final Properties props = new Properties();
        props.setProperty("outer", "${inner}");
        props.setProperty("inner", "value");
        final String result = OptionConverter.substVars("prefix-${${outer}}-suffix", props);
        assertEquals("prefix-${${outer}}-suffix", result);
    }

    @Test
    void substVarsPreservesCycleDetection() {
        final Properties props = new Properties();
        props.setProperty("a", "${b}");
        props.setProperty("b", "${a}");
        final String result = OptionConverter.substVars("${a}", props);
        assertTrue(result.contains("${") || result.isEmpty());
    }

    private static void restoreProperty(final String name, final String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }
}
