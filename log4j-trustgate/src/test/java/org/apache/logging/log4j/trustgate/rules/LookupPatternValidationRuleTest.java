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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class LookupPatternValidationRuleTest {

    private final LookupPatternValidationRule rule = new LookupPatternValidationRule();

    @ParameterizedTest
    @MethodSource("logMessageAttackPatterns")
    void rejectsFixturePatterns(final String pattern) {
        assertTrue(rule.matches(pattern, InputType.LOG_MESSAGE), pattern);
    }

    @ParameterizedTest
    @ValueSource(strings = {"plain message", "price is 100 dollars", "use $$ to escape", ""})
    void acceptsSafeMessages(final String message) {
        if (message.isEmpty()) {
            return;
        }
        assertFalse(rule.matches(message, InputType.LOG_MESSAGE));
    }

    @Test
    void detectsLookupMarkerViaIndexOf() {
        assertTrue(rule.matches("before${after", InputType.LOG_MESSAGE));
    }

    @Test
    void ignoresWrongInputType() {
        assertFalse(rule.matches("${env:USER}", InputType.LOOKUP_PATTERN));
    }

    static Stream<Arguments> logMessageAttackPatterns() {
        return AttackPatternFixtures.patternsFor("log-message");
    }
}
