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

import java.util.stream.Stream;
import org.apache.logging.log4j.trustgate.spi.InputType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class RecursiveLookupValidationRuleTest {

    private String previousMaxDepth;
    private RecursiveLookupValidationRule rule;

    @BeforeEach
    void setUp() {
        previousMaxDepth = System.getProperty(RecursiveLookupValidationRule.MAX_DEPTH_PROPERTY);
        System.clearProperty(RecursiveLookupValidationRule.MAX_DEPTH_PROPERTY);
        rule = new RecursiveLookupValidationRule();
    }

    @AfterEach
    void tearDown() {
        if (previousMaxDepth == null) {
            System.clearProperty(RecursiveLookupValidationRule.MAX_DEPTH_PROPERTY);
        } else {
            System.setProperty(RecursiveLookupValidationRule.MAX_DEPTH_PROPERTY, previousMaxDepth);
        }
    }

    @ParameterizedTest
    @MethodSource("recursiveAttackPatterns")
    void rejectsFixturePatterns(final String pattern) {
        assertTrue(rule.matches(pattern, InputType.LOOKUP_PATTERN), pattern);
    }

    @ParameterizedTest
    @ValueSource(strings = {"${env:USER}", "${lower:abc}", "${foo}${bar}"})
    void acceptsSingleDepthPatterns(final String pattern) {
        assertFalse(rule.matches(pattern, InputType.LOOKUP_PATTERN));
    }

    @ParameterizedTest
    @ValueSource(strings = {"${${env:USER}}", "${${lower:${env:USER}}}"})
    void rejectsNestedPatternsWithDefaultDepth(final String pattern) {
        assertTrue(rule.matches(pattern, InputType.LOOKUP_PATTERN));
    }

    @Test
    void honorsConfiguredDepth() {
        final RecursiveLookupValidationRule depthTwo = new RecursiveLookupValidationRule(2);
        assertFalse(depthTwo.matches("${${env:USER}}", InputType.LOOKUP_PATTERN));
        assertTrue(depthTwo.matches("${${${env:USER}}}", InputType.LOOKUP_PATTERN));
    }

    @Test
    void ignoresWrongInputType() {
        assertFalse(rule.matches("${${env:USER}}", InputType.LOG_MESSAGE));
    }

    @Test
    void maxNestedDepthUtility() {
        assertTrue(RecursiveLookupValidationRule.maxNestedDepth("${${x}}") > 1);
        assertFalse(RecursiveLookupValidationRule.maxNestedDepth("${a}${b}") > 1);
    }

    static Stream<Arguments> recursiveAttackPatterns() {
        return AttackPatternFixtures.patternsFor("recursive-lookup");
    }
}
