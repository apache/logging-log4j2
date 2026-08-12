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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class StrSubstitutorTrustGateTest {

    private static final String TESTKEY = "StrSubstitutorTrustGateTestKey";
    private static final String TESTVAL = "TrustGateTestValue";
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
        System.setProperty(TESTKEY, TESTVAL);
    }

    @AfterEach
    void tearDown() {
        restoreProperty(STRICTNESS_PROPERTY, previousStrictnessProperty);
        restoreProperty(STRICT_PROPERTY, previousStrictProperty);
        System.clearProperty(TESTKEY);
    }

    @Test
    void plainTextWithoutLookupPatternsIsUnchanged() {
        final StrSubstitutor subst = createSubstitutor();
        assertEquals("no variables here", subst.replace("no variables here"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"${sys:" + TESTKEY + "}", "${env:PATH}"})
    void validLookupPatternsResolve(final String pattern) {
        final StrSubstitutor subst = createSubstitutor();
        if (pattern.startsWith("${sys:")) {
            assertEquals(TESTVAL, subst.replace(pattern));
        } else {
            assertEquals(System.getenv("PATH"), subst.replace(pattern));
        }
    }

    @Test
    void nestedLookupPatternIsLeftLiteralInStrictMode() {
        final StrSubstitutor subst = createSubstitutor();
        final String attack = "${${env:USER}}";
        assertEquals(attack, subst.replace(attack));
    }

    @Test
    void escapedLookupPatternBypassesValidationAndSubstitution() {
        final StrSubstitutor subst = createSubstitutor();
        assertEquals("prefix ${literal} suffix", subst.replace("prefix $${literal} suffix"));
    }

    @Test
    void defaultValueExpressionWithSingleDepthLookupResolves() {
        final StrSubstitutor subst = createSubstitutor();
        assertEquals(TESTVAL, subst.replace("${sys:MissingKey:-${sys:" + TESTKEY + "}}"));
    }

    @Test
    void configurationStrSubstitutorInheritsTrustGateValidation() {
        final ConfigurationStrSubstitutor subst = new ConfigurationStrSubstitutor(createLookup());
        final String attack = "${${env:USER}}";
        assertEquals(attack, subst.replace(attack));
    }

    @Test
    void fixtureValidPatternsResolve() throws IOException {
        final StrSubstitutor subst = createSubstitutor();
        for (final String pattern : loadFixturePatterns("valid")) {
            if (pattern.startsWith("${sys:")) {
                assertEquals(TESTVAL, subst.replace(pattern), pattern);
            } else if (pattern.startsWith("${env:")) {
                final String key = pattern.substring("${env:".length(), pattern.length() - 1);
                assertEquals(System.getenv(key), subst.replace(pattern), pattern);
            }
        }
    }

    @Test
    void fixtureAttackPatternsRemainLiteralInStrictMode() throws IOException {
        final StrSubstitutor subst = createSubstitutor();
        for (final String pattern : loadFixturePatterns("attack")) {
            assertEquals(pattern, subst.replace(pattern), pattern);
        }
    }

    private static StrSubstitutor createSubstitutor() {
        return new StrSubstitutor(createLookup());
    }

    private static StrLookup createLookup() {
        final Map<String, String> map = new HashMap<>();
        map.put(TESTKEY, TESTVAL);
        return new Interpolator(new PropertiesLookup(map));
    }

    private static void restoreProperty(final String name, final String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }

    private static List<String> loadFixturePatterns(final String category) throws IOException {
        final String header = "category=" + category;
        boolean inCategory = false;
        final List<String> patterns = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                StrSubstitutorTrustGateTest.class.getResourceAsStream("/trustgate-strsubstitutor-patterns.txt"),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("category=")) {
                    inCategory = line.equals(header);
                    continue;
                }
                if (inCategory) {
                    patterns.add(line);
                }
            }
        }
        return patterns;
    }

    private static final class PropertiesLookup extends AbstractLookup {

        private final Map<String, String> properties;

        PropertiesLookup(final Map<String, String> properties) {
            this.properties = properties;
        }

        @Override
        public String lookup(final String key) {
            return properties.get(key);
        }

        @Override
        public String lookup(final org.apache.logging.log4j.core.LogEvent event, final String key) {
            return lookup(key);
        }
    }
}
