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
package org.apache.logging.log4j.trustgate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.logging.log4j.trustgate.spi.InputSanitizer;
import org.apache.logging.log4j.trustgate.spi.InputType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultInputSanitizerTest {

    private static final InputType TYPE = InputType.LOG_MESSAGE;

    private String previousStrictnessProperty;
    private String previousStrictProperty;
    private PrintStream previousErr;
    private ByteArrayOutputStream capturedErr;

    @BeforeEach
    void setUp() {
        previousStrictnessProperty = System.getProperty(DefaultInputSanitizer.STRICTNESS_PROPERTY);
        previousStrictProperty = System.getProperty(DefaultInputSanitizer.STRICT_PROPERTY);
        System.clearProperty(DefaultInputSanitizer.STRICTNESS_PROPERTY);
        System.clearProperty(DefaultInputSanitizer.STRICT_PROPERTY);
        capturedErr = new ByteArrayOutputStream();
        previousErr = System.err;
        System.setErr(new PrintStream(capturedErr, true));
    }

    @AfterEach
    void tearDown() {
        System.setErr(previousErr);
        restoreProperty(DefaultInputSanitizer.STRICTNESS_PROPERTY, previousStrictnessProperty);
        restoreProperty(DefaultInputSanitizer.STRICT_PROPERTY, previousStrictProperty);
    }

    private static void restoreProperty(final String propertyName, final String previousValue) {
        if (previousValue == null) {
            System.clearProperty(propertyName);
        } else {
            System.setProperty(propertyName, previousValue);
        }
    }

    @Test
    void getInstanceReturnsServiceLoaderImplementation() {
        final InputSanitizer sanitizer = DefaultInputSanitizer.getInstance();

        assertTrue(sanitizer instanceof DefaultInputSanitizer);
        assertTrue(sanitizer.isEnabled());
    }

    @Test
    void rejectsNullInput() {
        final DefaultInputSanitizer sanitizer = new DefaultInputSanitizer();

        final ValidationResult result = sanitizer.validate(null, TYPE);

        assertFalse(result.isValid());
        assertEquals("empty-input", result.getRuleName());
        assertEquals("Input must not be null or empty", result.getRejectionReason());
    }

    @Test
    void rejectsEmptyInput() {
        final DefaultInputSanitizer sanitizer = new DefaultInputSanitizer();

        final ValidationResult result = sanitizer.validate("", TYPE);

        assertFalse(result.isValid());
        assertEquals("empty-input", result.getRuleName());
        assertEquals("Input must not be null or empty", result.getRejectionReason());
    }

    @Test
    void acceptsValidInput() {
        final DefaultInputSanitizer sanitizer = new DefaultInputSanitizer();

        final ValidationResult result = sanitizer.validate("safe message", TYPE);

        assertTrue(result.isValid());
    }

    @Test
    void throwsWhenRuleMatchesInStrictMode() {
        final DefaultInputSanitizer sanitizer = new DefaultInputSanitizer();
        final String forbiddenInput = "prefix" + TestValidationRule.MATCH_TOKEN + "suffix";

        final TrustGateException exception =
                assertThrows(TrustGateException.class, () -> sanitizer.validate(forbiddenInput, TYPE));

        assertTrue(exception.getMessage().contains(TestValidationRule.RULE_NAME));
    }

    @Test
    void bypassesValidationWhenDisabledViaLegacyStrictProperty() {
        System.setProperty(DefaultInputSanitizer.STRICT_PROPERTY, "false");
        final DefaultInputSanitizer sanitizer = new DefaultInputSanitizer();
        final String forbiddenInput = TestValidationRule.MATCH_TOKEN;

        assertFalse(sanitizer.isEnabled());
        final ValidationResult result = sanitizer.validate(forbiddenInput, TYPE);

        assertTrue(result.isValid());
    }

    @Test
    void bypassesValidationWhenDisabledViaStrictnessProperty() {
        System.setProperty(DefaultInputSanitizer.STRICTNESS_PROPERTY, "disabled");
        final DefaultInputSanitizer sanitizer = new DefaultInputSanitizer();
        final String forbiddenInput = TestValidationRule.MATCH_TOKEN;

        assertFalse(sanitizer.isEnabled());
        assertTrue(sanitizer.validate(forbiddenInput, TYPE).isValid());
        assertTrue(sanitizer.validate(null, TYPE).isValid());
    }

    @Test
    void permissiveModeLogsWarningAndAllowsInput() {
        System.setProperty(DefaultInputSanitizer.STRICTNESS_PROPERTY, "permissive");
        final DefaultInputSanitizer sanitizer = new DefaultInputSanitizer();
        final String forbiddenInput = TestValidationRule.MATCH_TOKEN;

        assertTrue(sanitizer.isEnabled());
        final ValidationResult result = sanitizer.validate(forbiddenInput, TYPE);

        assertTrue(result.isValid());
        final String errOutput = capturedErr.toString();
        assertTrue(errOutput.contains("[TrustGate PERMISSIVE] Validation rule " + TestValidationRule.RULE_NAME
                + " would reject input of type " + TYPE + ": Input rejected by rule: " + TestValidationRule.RULE_NAME));
    }

    @Test
    void strictEnabledByDefault() {
        final DefaultInputSanitizer sanitizer = new DefaultInputSanitizer();

        assertTrue(sanitizer.isEnabled());
        assertEquals(StrictnessLevel.STRICT, DefaultInputSanitizer.resolveStrictnessLevel());
    }

    @Test
    void legacyStrictTrueMapsToStrictMode() {
        System.setProperty(DefaultInputSanitizer.STRICT_PROPERTY, "true");

        assertEquals(StrictnessLevel.STRICT, DefaultInputSanitizer.resolveStrictnessLevel());
    }

    @Test
    void legacyStrictFalseMapsToDisabledMode() {
        System.setProperty(DefaultInputSanitizer.STRICT_PROPERTY, "false");

        assertEquals(StrictnessLevel.DISABLED, DefaultInputSanitizer.resolveStrictnessLevel());
    }

    @Test
    void strictnessPropertyTakesPrecedenceOverLegacyStrictProperty() {
        System.setProperty(DefaultInputSanitizer.STRICTNESS_PROPERTY, "permissive");
        System.setProperty(DefaultInputSanitizer.STRICT_PROPERTY, "false");

        assertEquals(StrictnessLevel.PERMISSIVE, DefaultInputSanitizer.resolveStrictnessLevel());
        final DefaultInputSanitizer sanitizer = new DefaultInputSanitizer();

        assertTrue(sanitizer.isEnabled());
        assertTrue(sanitizer.validate(TestValidationRule.MATCH_TOKEN, TYPE).isValid());
    }

    @Test
    void invalidStrictnessPropertyDefaultsToStrict() {
        System.setProperty(DefaultInputSanitizer.STRICTNESS_PROPERTY, "medium");

        assertEquals(StrictnessLevel.STRICT, DefaultInputSanitizer.resolveStrictnessLevel());
        final String errOutput = capturedErr.toString();
        assertTrue(errOutput.contains("Unrecognized strictness value 'medium', defaulting to STRICT"));
    }

    @Test
    void supportsConcurrentAccess() throws Exception {
        final DefaultInputSanitizer sanitizer = new DefaultInputSanitizer();
        final ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            final List<Callable<ValidationResult>> tasks = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                tasks.add(() ->
                        sanitizer.validate("safe-" + Thread.currentThread().getId(), TYPE));
            }
            for (final Future<ValidationResult> future : executor.invokeAll(tasks)) {
                assertTrue(future.get().isValid());
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void injectedRuleMatchThrowsInStrictMode() {
        final DefaultInputSanitizer sanitizer =
                new DefaultInputSanitizer(Collections.singletonList(new TestValidationRule()), StrictnessLevel.STRICT);

        assertThrows(
                TrustGateException.class,
                () -> sanitizer.validate(TestValidationRule.MATCH_TOKEN, InputType.JNDI_LOOKUP));
    }

    @Test
    void injectedRuleMatchAllowedInPermissiveMode() {
        final DefaultInputSanitizer sanitizer = new DefaultInputSanitizer(
                Collections.singletonList(new TestValidationRule()), StrictnessLevel.PERMISSIVE);

        assertTrue(sanitizer
                .validate(TestValidationRule.MATCH_TOKEN, InputType.JNDI_LOOKUP)
                .isValid());
        final String errOutput = capturedErr.toString();
        assertTrue(errOutput.contains("[TrustGate PERMISSIVE]"));
    }

    @Test
    void injectedRuleMatchSkippedInDisabledMode() {
        final DefaultInputSanitizer sanitizer = new DefaultInputSanitizer(
                Collections.singletonList(new TestValidationRule()), StrictnessLevel.DISABLED);

        assertFalse(sanitizer.isEnabled());
        assertTrue(sanitizer
                .validate(TestValidationRule.MATCH_TOKEN, InputType.JNDI_LOOKUP)
                .isValid());
        assertTrue(capturedErr.toString().isEmpty());
    }
}
