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
package org.apache.logging.log4j.plugins.convert;

import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Cast;

/**
 * Interface for doing automatic String conversion to a specific type.
 *
 * @param <T> Converts Strings into the given type {@code T}.
 * @since 3.0.0 Moved to {@code log4j-plugins}.
 */
@FunctionalInterface
public interface TypeConverter<T> {

    /**
     * Converts a String to a given type.
     *
     * @param s the String to convert. Cannot be {@code null}.
     * @return the converted object.
     * @throws Exception thrown when a conversion error occurs
     */
    T convert(String s) throws Exception;

    default T convert(final String string, final Object defaultValue) {
        return convert(string, defaultValue, false);
    }

    default T convert(final String string, final Object defaultValue, final boolean sensitive) {
        if (string != null) {
            try {
                return convert(string);
            } catch (final Exception e) {
                StatusLogger.getLogger().warn("Unable to convert string [{}]. Using default value [{}].",
                        sensitive ? "-redacted-" : string, defaultValue, e);
            }
        }
        if (defaultValue == null) {
            return null;
        }
        if (!(defaultValue instanceof String)) {
            return Cast.cast(defaultValue);
        }
        try {
            return convert((String) defaultValue);
        } catch (final Exception e) {
            StatusLogger.getLogger().debug("Unable to parse default value [{}].", defaultValue, e);
            return null;
        }
    }
}
