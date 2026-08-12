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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.logging.log4j.trustgate.spi.InputType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultInputSanitizerTest {

    private static final InputType TYPE = InputType.LOG_MESSAGE;

    private String previousStrictProperty;

    @BeforeEach
    void setUp() {
        previousStrictProperty = System.getProperty(DefaultInputSanitizer.STRICT_PROPERTY);
        System.clearProperty(DefaultInputSanitizer.STRICT_PROPERTY);
    }

    @AfterEach
    void tearDown() {
        if (previousStrictProperty == null) {
            System.clearProperty(DefaultInputSanitizer.STRICT_PROPERTY);
        } else {
            System.setProperty(DefaultInputSanitizer.STRICT_PROPERTY, previousStrictProperty);
        }
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
    void throwsWhenRuleMatches() {
        final DefaultInputSanitizer sanitizer = new DefaultInputSanitizer();
        final String forbiddenInput = "prefix" + TestValidationRule.MATCH_TOKEN + "suffix";

        final TrustGateException exception =
                assertThrows(TrustGateException.class, () -> sanitizer.validate(forbiddenInput, TYPE));

        assertTrue(exception.getMessage().contains(TestValidationRule.RULE_NAME));
    }

    @Test
    void bypassesValidationWhenStrictDisabled() {
        System.setProperty(DefaultInputSanitizer.STRICT_PROPERTY, "false");
        final DefaultInputSanitizer sanitizer = new DefaultInputSanitizer();
        final String forbiddenInput = TestValidationRule.MATCH_TOKEN;

        assertFalse(sanitizer.isEnabled());
        final ValidationResult result = sanitizer.validate(forbiddenInput, TYPE);

        assertTrue(result.isValid());
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
    void strictEnabledByDefault() {
        final DefaultInputSanitizer sanitizer = new DefaultInputSanitizer();

        assertTrue(sanitizer.isEnabled());
    }

    @Test
    void injectedRuleMatchThrows() {
        final DefaultInputSanitizer sanitizer =
                new DefaultInputSanitizer(Collections.singletonList(new TestValidationRule()));

        assertThrows(
                TrustGateException.class,
                () -> sanitizer.validate(TestValidationRule.MATCH_TOKEN, InputType.JNDI_LOOKUP));
    }
}
