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

class JndiSchemeValidationRuleTest {

    private final JndiSchemeValidationRule rule = new JndiSchemeValidationRule();

    @ParameterizedTest
    @MethodSource("jndiAttackPatterns")
    void rejectsFixturePatterns(final String pattern) {
        if (pattern.startsWith("java:") || !pattern.contains("://")) {
            assertFalse(rule.matches(pattern, InputType.JNDI_LOOKUP), pattern);
            return;
        }
        assertTrue(rule.matches(pattern, InputType.JNDI_LOOKUP), pattern);
    }

    @ParameterizedTest
    @ValueSource(strings = {"java:comp/env/jdbc/datasource", "comp/env/jdbc/datasource", "java:/module/env"})
    void acceptsJavaOrSchemelessLookups(final String input) {
        assertFalse(rule.matches(input, InputType.JNDI_LOOKUP));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ldap://evil.example/a", "dns://evil.example/a", "rmi://evil.example/a"})
    void rejectsNonJavaSchemes(final String input) {
        assertTrue(rule.matches(input, InputType.JNDI_LOOKUP));
    }

    @Test
    void ignoresWrongInputType() {
        assertFalse(rule.matches("ldap://evil.example/a", InputType.LOG_MESSAGE));
    }

    @Test
    void rejectsMalformedUri() {
        assertTrue(rule.matches("ldap:[invalid", InputType.JNDI_LOOKUP));
    }

    @Test
    void hasStableRuleName() {
        assertTrue(rule.getRuleName().contains("jndi"));
    }

    static Stream<Arguments> jndiAttackPatterns() {
        return AttackPatternFixtures.patternsFor("jndi");
    }
}
