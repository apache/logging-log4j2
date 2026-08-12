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
package org.apache.logging.log4j.trustgate.rules;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.logging.log4j.trustgate.spi.InputType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class PropertyKeyValidationRuleTest {

    private final PropertyKeyValidationRule rule = new PropertyKeyValidationRule();

    @ParameterizedTest
    @MethodSource("safePropertyKeys")
    void acceptsSafeKeys(final String key) {
        assertFalse(rule.matches(key, InputType.PROPERTY_KEY), key);
    }

    @ParameterizedTest
    @MethodSource("unsafePropertyKeys")
    void rejectsUnsafeKeys(final String key) {
        assertTrue(rule.matches(key, InputType.PROPERTY_KEY), key);
    }

    @ParameterizedTest
    @ValueSource(strings = {"log4j2.enableJndi", "spring.profiles.active", "appender_console"})
    void acceptsCommonKeys(final String key) {
        assertFalse(rule.matches(key, InputType.PROPERTY_KEY));
    }

    @Test
    void ignoresWrongInputType() {
        assertFalse(rule.matches("log4j2%enableJndi", InputType.CONFIGURATION_VALUE));
    }

    @Test
    void fixtureContainsMinimumPatternCount() {
        final Map<String, List<String>> patterns = AttackPatternFixtures.patternsByCategory();
        int total = 0;
        for (final List<String> values : patterns.values()) {
            total += values.size();
        }
        assertTrue(total >= 50, "expected at least 50 attack patterns, found " + total);
    }

    static Stream<Arguments> safePropertyKeys() {
        return AttackPatternFixtures.patternsFor("property-key").filter(args -> {
            final String key = (String) args.get()[0];
            return key.chars().allMatch(ch -> Character.isLetterOrDigit(ch) || ch == '.' || ch == '-' || ch == '_');
        });
    }

    static Stream<Arguments> unsafePropertyKeys() {
        return AttackPatternFixtures.patternsFor("property-key").filter(args -> {
            final String key = (String) args.get()[0];
            return key.chars().anyMatch(ch -> !(Character.isLetterOrDigit(ch) || ch == '.' || ch == '-' || ch == '_'));
        });
    }
}
