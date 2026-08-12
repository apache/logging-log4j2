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

import org.apache.logging.log4j.trustgate.spi.InputType;
import org.apache.logging.log4j.trustgate.spi.ValidationRule;

/**
 * Rejects lookup patterns whose nested {@code ${...}} depth exceeds the configured limit.
 */
public final class RecursiveLookupValidationRule implements ValidationRule {

    static final String MAX_DEPTH_PROPERTY = "log4j2.trustgate.maxLookupDepth";
    static final int DEFAULT_MAX_DEPTH = 1;
    private static final String RULE_NAME = "recursive-lookup";

    private final int maxDepth;

    public RecursiveLookupValidationRule() {
        this(parseMaxDepth(System.getProperty(MAX_DEPTH_PROPERTY)));
    }

    RecursiveLookupValidationRule(final int maxDepth) {
        this.maxDepth = maxDepth;
    }

    @Override
    public boolean matches(final String input, final InputType type) {
        if (type != InputType.LOOKUP_PATTERN) {
            return false;
        }
        return maxNestedDepth(input) > maxDepth;
    }

    @Override
    public String getRuleName() {
        return RULE_NAME;
    }

    static int maxNestedDepth(final String input) {
        int depth = 0;
        int maxSeen = 0;
        for (int i = 0; i < input.length() - 1; i++) {
            if (input.charAt(i) == '$' && input.charAt(i + 1) == '{') {
                depth++;
                if (depth > maxSeen) {
                    maxSeen = depth;
                }
                i++;
            } else if (input.charAt(i) == '}') {
                if (depth > 0) {
                    depth--;
                }
            }
        }
        return maxSeen;
    }

    private static int parseMaxDepth(final String propertyValue) {
        if (propertyValue == null || propertyValue.isEmpty()) {
            return DEFAULT_MAX_DEPTH;
        }
        try {
            return Integer.parseInt(propertyValue);
        } catch (final NumberFormatException ex) {
            return DEFAULT_MAX_DEPTH;
        }
    }
}
