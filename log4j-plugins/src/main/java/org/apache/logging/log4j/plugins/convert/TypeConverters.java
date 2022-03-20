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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.util.TypeUtil;
import org.apache.logging.log4j.status.StatusLogger;

import java.lang.reflect.Type;

/**
 * Collection of basic TypeConverter implementations. May be used to register additional TypeConverters or find
 * registered TypeConverters.
 *
 * @since 2.1 Moved to the {@code convert} package.
 */
public final class TypeConverters {

    /**
     * The {@link Plugin#category() Plugin Category} to use for {@link TypeConverter} plugins.
     *
     * @since 2.1
     */
    public static final String CATEGORY = "TypeConverter";

    /**
     * Converts a String to a given class if a TypeConverter is available for that class. Falls back to the provided
     * default value if the conversion is unsuccessful. However, if the default value is <em>also</em> invalid, then
     * {@code null} is returned (along with a nasty status log message).
     *
     * @param s
     *        the string to convert
     * @param clazz
     *        the class to try to convert the string to
     * @param defaultValue
     *        the fallback object to use if the conversion is unsuccessful
     * @param <T> The type of the clazz parameter.
     * @return the converted object which may be {@code null} if the string is invalid for the given type
     * @throws NullPointerException
     *         if {@code clazz} is {@code null}
     * @throws IllegalArgumentException
     *         if no TypeConverter exists for the given class
     */
    public static <T> T convert(final String s, final Class<? extends T> clazz, final Object defaultValue) {
        return convert(s, clazz, defaultValue, false);
    }

    public static <T> T convert(final String s, final Type targetType, final Object defaultValue, final boolean sensitive) {
        final TypeConverter<T> converter =
                TypeUtil.cast(TypeConverterRegistry.getInstance().findCompatibleConverter(targetType));
        if (s == null) {
            return parseDefaultValue(converter, defaultValue);
        }
        try {
            return converter.convert(s);
        } catch (final Exception e) {
            LOGGER.warn("Error while converting string [{}] to type [{}]. Using default value [{}].",
                    sensitive ? "-redacted-" : s, targetType, defaultValue, e);
            return parseDefaultValue(converter, defaultValue);
        }
    }

    private static <T> T parseDefaultValue(final TypeConverter<T> converter, final Object defaultValue) {
        if (defaultValue == null) {
            return null;
        }
        if (!(defaultValue instanceof String)) {
            return TypeUtil.cast(defaultValue);
        }
        try {
            return converter.convert((String) defaultValue);
        } catch (final Exception e) {
            LOGGER.debug("Can't parse default value [{}] for type [{}].", defaultValue, converter.getClass(), e);
            return null;
        }
    }

    private static final Logger LOGGER = StatusLogger.getLogger();

}
