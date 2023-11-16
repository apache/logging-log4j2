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
package org.apache.logging.log4j.core.util;

import java.util.Collection;
import java.util.Map;

/**
 * Utility class providing common validation logic.
 */
public final class Assert {
    private Assert() {}

    /**
     * Checks if an object has empty semantics. The following scenarios are considered empty:
     * <ul>
     * <li>{@code null}</li>
     * <li>empty {@link CharSequence}</li>
     * <li>empty array</li>
     * <li>empty {@link Iterable}</li>
     * <li>empty {@link Map}</li>
     * </ul>
     *
     * @param o value to check for emptiness
     * @return true if the value is empty, false otherwise
     * @since 2.8
     */
    public static boolean isEmpty(final Object o) {
        if (o == null) {
            return true;
        }
        if (o instanceof CharSequence) {
            return ((CharSequence) o).length() == 0;
        }
        if (o.getClass().isArray()) {
            return ((Object[]) o).length == 0;
        }
        if (o instanceof Collection) {
            return ((Collection<?>) o).isEmpty();
        }
        if (o instanceof Map) {
            return ((Map<?, ?>) o).isEmpty();
        }
        return false;
    }

    /**
     * Opposite of {@link #isEmpty(Object)}.
     *
     * @param o value to check for non-emptiness
     * @return true if the value is non-empty, false otherwise
     * @since 2.8
     */
    public static boolean isNonEmpty(final Object o) {
        return !isEmpty(o);
    }

    /**
     * Checks a value for emptiness and throws an IllegalArgumentException if it's empty.
     *
     * @param value value to check for emptiness
     * @param <T>   type of value
     * @return the provided value if non-empty
     * @since 2.8
     */
    public static <T> T requireNonEmpty(final T value) {
        return requireNonEmpty(value, "");
    }

    /**
     * Checks a value for emptiness and throws an IllegalArgumentException if it's empty.
     *
     * @param value   value to check for emptiness
     * @param message message to provide in exception
     * @param <T>     type of value
     * @return the provided value if non-empty
     * @since 2.8
     */
    public static <T> T requireNonEmpty(final T value, final String message) {
        if (isEmpty(value)) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    public static int valueIsAtLeast(final int value, final int minValue) {
        if (value < minValue) {
            throw new IllegalArgumentException("Value should be at least " + minValue + " but was " + value);
        }
        return value;
    }
}
