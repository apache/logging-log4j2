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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import org.apache.logging.log4j.trustgate.spi.InputSanitizer;
import org.apache.logging.log4j.trustgate.spi.InputType;
import org.apache.logging.log4j.trustgate.spi.ValidationRule;

/**
 * Default fail-closed {@link InputSanitizer} implementation.
 */
public final class DefaultInputSanitizer implements InputSanitizer {

    static final String STRICTNESS_PROPERTY = "log4j2.trustgate.strictness";
    static final String STRICT_PROPERTY = "log4j2.trustgate.strict";
    private static final String EMPTY_INPUT_RULE = "empty-input";

    private final List<ValidationRule> rules;
    private final StrictnessLevel strictnessLevel;

    public DefaultInputSanitizer() {
        this(loadRules(), resolveStrictnessLevel());
    }

    DefaultInputSanitizer(final List<ValidationRule> rules) {
        this(rules, resolveStrictnessLevel());
    }

    DefaultInputSanitizer(final List<ValidationRule> rules, final StrictnessLevel strictnessLevel) {
        this.rules = Collections.unmodifiableList(new ArrayList<>(rules));
        this.strictnessLevel = strictnessLevel;
    }

    private static List<ValidationRule> loadRules() {
        final List<ValidationRule> loaded = new ArrayList<>();
        for (final ValidationRule rule : ServiceLoader.load(ValidationRule.class)) {
            loaded.add(rule);
        }
        return loaded;
    }

    static StrictnessLevel resolveStrictnessLevel() {
        final PropertyReadResult strictnessProperty = readProperty(STRICTNESS_PROPERTY);
        if (strictnessProperty.securityDenied) {
            return StrictnessLevel.STRICT;
        }
        if (strictnessProperty.value != null) {
            final StrictnessLevel parsed = StrictnessLevel.fromString(strictnessProperty.value);
            if (parsed == StrictnessLevel.STRICT && !isRecognizedStrictnessValue(strictnessProperty.value)) {
                System.err.println("[TrustGate] Unrecognized strictness value '"
                        + strictnessProperty.value
                        + "', defaulting to STRICT");
            }
            return parsed;
        }

        final PropertyReadResult legacyStrictProperty = readProperty(STRICT_PROPERTY);
        if (legacyStrictProperty.securityDenied) {
            return StrictnessLevel.STRICT;
        }
        if (legacyStrictProperty.value != null) {
            return Boolean.parseBoolean(legacyStrictProperty.value) ? StrictnessLevel.STRICT : StrictnessLevel.DISABLED;
        }

        return StrictnessLevel.STRICT;
    }

    private static boolean isRecognizedStrictnessValue(final String value) {
        final String normalized = value.trim();
        if (normalized.isEmpty()) {
            return false;
        }
        for (final StrictnessLevel level : StrictnessLevel.values()) {
            if (level.name().equalsIgnoreCase(normalized)) {
                return true;
            }
        }
        return false;
    }

    private static PropertyReadResult readProperty(final String propertyName) {
        try {
            return new PropertyReadResult(System.getProperty(propertyName), false);
        } catch (final SecurityException ex) {
            System.err.println(
                    "[TrustGate] Unable to read system property '" + propertyName + "', defaulting to STRICT");
            return new PropertyReadResult(null, true);
        }
    }

    private static final class PropertyReadResult {
        private final String value;
        private final boolean securityDenied;

        private PropertyReadResult(final String value, final boolean securityDenied) {
            this.value = value;
            this.securityDenied = securityDenied;
        }
    }

    @Override
    public boolean isEnabled() {
        return strictnessLevel != StrictnessLevel.DISABLED;
    }

    @Override
    public ValidationResult validate(final String input, final InputType type) {
        if (strictnessLevel == StrictnessLevel.DISABLED) {
            return ValidationResult.valid();
        }
        if (input == null || input.isEmpty()) {
            return ValidationResult.rejected(EMPTY_INPUT_RULE, "Input must not be null or empty");
        }
        for (final ValidationRule rule : rules) {
            if (rule.matches(input, type)) {
                final String reason = "Input rejected by rule: " + rule.getRuleName();
                if (strictnessLevel == StrictnessLevel.PERMISSIVE) {
                    System.err.println("[TrustGate PERMISSIVE] Validation rule "
                            + rule.getRuleName()
                            + " would reject input of type "
                            + type
                            + ": "
                            + reason);
                    return ValidationResult.valid();
                }
                throw new TrustGateException(reason);
            }
        }
        return ValidationResult.valid();
    }
}
