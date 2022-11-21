/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.util;

/**
 * Describes a property value representing some sort of boolean. Presence or absence of properties can be treated
 * as an added dimension to collapse into a boolean value.
 *
 * @since 3.0.0
 */
public enum BooleanProperty {
    /** Represents the absence of a property value. */
    ABSENT {
        @Override
        public boolean orElse(final boolean defaultValue) {
            return defaultValue;
        }

        @Override
        public boolean orElse(final boolean defaultValueIfAbsent, final boolean defaultValueIfPresent) {
            return defaultValueIfAbsent;
        }
    },

    /** Represents a non-empty property value that doesn't case-insensitively match the string {@code true}. */
    FALSE {
        @Override
        public boolean orElse(final boolean defaultValue) {
            return false;
        }

        @Override
        public boolean orElse(final boolean defaultValueIfAbsent, final boolean defaultValueIfPresent) {
            return false;
        }
    },

    /** Represents an empty property value. */
    PRESENT {
        @Override
        public boolean orElse(final boolean defaultValue) {
            return defaultValue;
        }

        @Override
        public boolean orElse(final boolean defaultValueIfAbsent, final boolean defaultValueIfPresent) {
            return defaultValueIfPresent;
        }
    },

    /** Represents a non-empty property value that case-insensitively matches the string {@code true}. */
    TRUE {
        @Override
        public boolean orElse(final boolean defaultValue) {
            return true;
        }

        @Override
        public boolean orElse(final boolean defaultValueIfAbsent, final boolean defaultValueIfPresent) {
            return true;
        }
    };

    /**
     * Converts this property into a boolean using the given default value when this is {@link #ABSENT} or
     * {@link #PRESENT}.
     */
    public abstract boolean orElse(final boolean defaultValue);

    /**
     * Converts this property into a boolean using the given default value when this is {@link #ABSENT} and a
     * given default value when this is {@link #PRESENT}.
     */
    public abstract boolean orElse(final boolean defaultValueIfAbsent, final boolean defaultValueIfPresent);

    public static BooleanProperty parse(final String value) {
        if (value == null) {
            return ABSENT;
        }
        if (value.isEmpty()) {
            return PRESENT;
        }
        return "true".equalsIgnoreCase(value) ? TRUE : FALSE;
    }
}
