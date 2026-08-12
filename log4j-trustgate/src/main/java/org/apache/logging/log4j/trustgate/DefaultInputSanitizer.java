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

    static final String STRICT_PROPERTY = "log4j2.trustgate.strict";
    private static final String EMPTY_INPUT_RULE = "empty-input";

    private final List<ValidationRule> rules;

    public DefaultInputSanitizer() {
        this(loadRules());
    }

    DefaultInputSanitizer(final List<ValidationRule> rules) {
        this.rules = Collections.unmodifiableList(new ArrayList<>(rules));
    }

    private static List<ValidationRule> loadRules() {
        final List<ValidationRule> loaded = new ArrayList<>();
        for (final ValidationRule rule : ServiceLoader.load(ValidationRule.class)) {
            loaded.add(rule);
        }
        return loaded;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.parseBoolean(System.getProperty(STRICT_PROPERTY, "true"));
    }

    @Override
    public ValidationResult validate(final String input, final InputType type) {
        if (!isEnabled()) {
            return ValidationResult.valid();
        }
        if (input == null || input.isEmpty()) {
            return ValidationResult.rejected(EMPTY_INPUT_RULE, "Input must not be null or empty");
        }
        for (final ValidationRule rule : rules) {
            if (rule.matches(input, type)) {
                throw new TrustGateException("Input rejected by rule: " + rule.getRuleName());
            }
        }
        return ValidationResult.valid();
    }
}
