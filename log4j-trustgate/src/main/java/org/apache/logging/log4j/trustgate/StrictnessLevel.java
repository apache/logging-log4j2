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

/**
 * Configurable enforcement level for TrustGate input validation.
 */
public enum StrictnessLevel {

    /** Fail-closed: reject input that matches a validation rule. */
    STRICT,

    /** Log a warning and allow input that would be rejected in strict mode. */
    PERMISSIVE,

    /** Bypass all validation. */
    DISABLED;

    /**
     * Parses a strictness level from its string representation.
     *
     * @param value the configured value
     * @return the matching level, or {@link #STRICT} when {@code value} is {@code null}, empty,
     *     blank, or unrecognized
     */
    public static StrictnessLevel fromString(final String value) {
        if (value == null) {
            return STRICT;
        }
        final String normalized = value.trim();
        if (normalized.isEmpty()) {
            return STRICT;
        }
        for (final StrictnessLevel level : values()) {
            if (level.name().equalsIgnoreCase(normalized)) {
                return level;
            }
        }
        return STRICT;
    }
}
