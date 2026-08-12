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

import java.util.Collections;
import java.util.stream.Stream;
import org.apache.logging.log4j.trustgate.spi.InputType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class UriSchemeValidationRuleTest {

    private String previousAllowedSchemes;
    private UriSchemeValidationRule rule;

    @BeforeEach
    void setUp() {
        previousAllowedSchemes = System.getProperty(UriSchemeValidationRule.ALLOWED_SCHEMES_PROPERTY);
        System.clearProperty(UriSchemeValidationRule.ALLOWED_SCHEMES_PROPERTY);
        rule = new UriSchemeValidationRule();
    }

    @AfterEach
    void tearDown() {
        if (previousAllowedSchemes == null) {
            System.clearProperty(UriSchemeValidationRule.ALLOWED_SCHEMES_PROPERTY);
        } else {
            System.setProperty(UriSchemeValidationRule.ALLOWED_SCHEMES_PROPERTY, previousAllowedSchemes);
        }
    }

    @ParameterizedTest
    @MethodSource("allowedConfigurationValues")
    void acceptsWhitelistedConfigurationValues(final String value) {
        assertFalse(rule.matches(value, InputType.CONFIGURATION_VALUE), value);
    }

    @ParameterizedTest
    @MethodSource("rejectedConfigurationValues")
    void rejectsDisallowedConfigurationValues(final String value) {
        assertTrue(rule.matches(value, InputType.CONFIGURATION_VALUE), value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"file", "http", "https", "classpath", "classloader"})
    void acceptsWhitelistedSchemes(final String scheme) {
        assertFalse(rule.matches(scheme, InputType.URI_SCHEME));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ldap", "ftp", "jar", "jndi"})
    void rejectsDisallowedSchemes(final String scheme) {
        assertTrue(rule.matches(scheme, InputType.URI_SCHEME));
    }

    @Test
    void ignoresPlainConfigurationValueWithoutScheme() {
        assertFalse(rule.matches("rolling", InputType.CONFIGURATION_VALUE));
    }

    @Test
    void rejectsMalformedConfigurationUri() {
        assertTrue(rule.matches("http://[bad", InputType.CONFIGURATION_VALUE));
    }

    @Test
    void ignoresWrongInputType() {
        assertFalse(rule.matches("ldap://evil.example/a", InputType.LOG_MESSAGE));
    }

    @Test
    void honorsAllowedSchemesProperty() {
        System.setProperty(UriSchemeValidationRule.ALLOWED_SCHEMES_PROPERTY, "ldap,ftp");
        final UriSchemeValidationRule customRule = new UriSchemeValidationRule();
        assertFalse(customRule.matches("ldap", InputType.URI_SCHEME));
        assertTrue(customRule.matches("file", InputType.URI_SCHEME));
    }

    @Test
    void packagePrivateConstructorOverridesDefaults() {
        final UriSchemeValidationRule customRule = new UriSchemeValidationRule(Collections.singleton("custom"));
        assertFalse(customRule.matches("custom", InputType.URI_SCHEME));
        assertTrue(customRule.matches("file", InputType.URI_SCHEME));
    }

    static Stream<Arguments> allowedConfigurationValues() {
        return Stream.of(
                Arguments.of("file:///etc/passwd"),
                Arguments.of("http://config.example/app.xml"),
                Arguments.of("https://config.example/app.xml"),
                Arguments.of("classpath:log4j2.xml"),
                Arguments.of("classloader:log4j2.xml"));
    }

    static Stream<Arguments> rejectedConfigurationValues() {
        return AttackPatternFixtures.patternsFor("uri-scheme").filter(args -> {
            final String value = (String) args.get()[0];
            return value.startsWith("ldap:")
                    || value.startsWith("ldaps:")
                    || value.startsWith("jndi:")
                    || value.startsWith("ftp:")
                    || value.startsWith("sftp:")
                    || value.startsWith("jar:")
                    || value.startsWith("netdoc:")
                    || value.startsWith("data:")
                    || value.startsWith("mailto:");
        });
    }
}
