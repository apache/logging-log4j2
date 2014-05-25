/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.logging.log4j.core.config.plugins.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.util.Assert;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Collection of basic TypeConverter implementations. May be used to register additional TypeConverters or find
 * registered TypeConverters.
 */
public final class TypeConverters {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private final Map<Class<?>, TypeConverter<?>> registry =
        new ConcurrentHashMap<Class<?>, TypeConverter<?>>();

    private static final class Holder {
        private static final TypeConverters INSTANCE = new TypeConverters();
    }

    /**
     * Constructs default TypeConverter registry. Used solely by singleton instance.
     */
    private TypeConverters() {
        registry.put(String.class, new StringConverter());
        registry.put(Boolean.class, new BooleanConverter());
        registry.put(Integer.class, new IntegerConverter());
        registry.put(Long.class, new LongConverter());
        registry.put(Float.class, new FloatConverter());
        registry.put(Double.class, new DoubleConverter());
        registry.put(Pattern.class, new PatternConverter());
        registry.put(Level.class, new LevelConverter());
        registry.put(Filter.Result.class, new FilterResultConverter());
    }

    /**
     * Locates a TypeConverter for a specified class.
     *
     * @param clazz the class to get a TypeConverter for
     * @return the associated TypeConverter for that class or {@code null} if none could be found
     */
    public static TypeConverter<?> findTypeConverter(final Class<?> clazz) {
        // TODO: what to do if there's no converter?
        // Idea 1: use reflection to see if the class has a static "valueOf" method and use that
        // Idea 2: reflect on class's declared methods to see if any methods look suitable (probably too complex)
        return Holder.INSTANCE.registry.get(clazz);
    }

    /**
     * Registers a TypeConverter for a specified class. This will overwrite any existing TypeConverter that may be
     * registered for the class.
     *
     * @param clazz the class to register the TypeConverter for
     * @param converter the TypeConverter to register
     */
    public static void registerTypeConverter(final Class<?> clazz, final TypeConverter<?> converter) {
        Holder.INSTANCE.registry.put(clazz, converter);
    }

    /**
     * Converts a String to a given class if a TypeConverter is available for that class. Falls back to the provided
     * default value if the conversion is unsuccessful. However, if the default value is <em>also</em> invalid, then
     * {@code null} is returned (along with a nasty status log message).
     *
     * @param s     the string to convert
     * @param clazz the class to try to convert the string to
     * @param defaultValue the fallback string to use if the conversion is unsuccessful
     * @return the converted object which may be {@code null} if the string is invalid for the given type
     * @throws NullPointerException if {@code clazz} is {@code null}
     * @throws IllegalArgumentException if no TypeConverter exists for the given class
     */
    public static Object convert(final String s, final Class<?> clazz, final String defaultValue) {
        final TypeConverter<?> converter = findTypeConverter(
            Assert.requireNonNull(clazz, "No class specified to convert to."));
        if (converter == null) {
            throw new IllegalArgumentException("No type converter found for class: " + clazz.getName());
        }
        if (s == null) {
            LOGGER.debug("Null string given to convert. Using default [{}].", defaultValue);
            return parseDefaultValue(converter, defaultValue);
        }
        try {
            return converter.convert(s);
        } catch (final Exception e) {
            LOGGER.warn("Error while converting string [{}] to type [{}]. Using default value [{}].", s, clazz,
                defaultValue, e);
            return parseDefaultValue(converter, defaultValue);
        }
    }

    private static Object parseDefaultValue(final TypeConverter<?> converter, final String defaultValue) {
        if (defaultValue == null) {
            return null;
        }
        try {
            return converter.convert(defaultValue);
        } catch (final Exception e) {
            LOGGER.debug("Can't parse default value [{}] for type [{}].", defaultValue, converter.getClass(), e);
            return null;
        }
    }

    /**
     * Trivial identity converter.
     */
    private static class StringConverter implements TypeConverter<String> {
        @Override
        public String convert(final String s) {
            return s;
        }
    }

    /**
     * Parses strings into booleans.
     */
    private static class BooleanConverter implements TypeConverter<Boolean> {
        @Override
        public Boolean convert(final String s) {
            return Boolean.parseBoolean(s);
        }
    }

    /**
     * Parses strings into integers.
     */
    private static class IntegerConverter implements TypeConverter<Integer> {
        @Override
        public Integer convert(final String s) {
            return Integer.parseInt(s);
        }
    }

    /**
     * Parses strings into longs.
     */
    private static class LongConverter implements TypeConverter<Long> {
        @Override
        public Long convert(final String s) {
            return Long.parseLong(s);
        }
    }

    /**
     * Parses strings into floats.
     */
    private static class FloatConverter implements TypeConverter<Float> {
        @Override
        public Float convert(final String s) {
            return Float.parseFloat(s);
        }
    }

    /**
     * Parses strings into doubles.
     */
    private static class DoubleConverter implements TypeConverter<Double> {
        @Override
        public Double convert(final String s) {
            return Double.parseDouble(s);
        }
    }

    /**
     * Parses strings into regular expression Patterns.
     */
    private static class PatternConverter implements TypeConverter<Pattern> {
        @Override
        public Pattern convert(final String s) {
            return Pattern.compile(s);
        }
    }

    /**
     * Parses strings into Log4j Levels. Returns {@code null} for invalid level names.
     */
    private static class LevelConverter implements TypeConverter<Level> {
        @Override
        public Level convert(final String s) {
            return Level.valueOf(s);
        }
    }

    /**
     * Parses strings into Filter Results. Returns {@code null} for invalid result names.
     */
    private static class FilterResultConverter implements TypeConverter<Filter.Result> {
        @Override
        public Filter.Result convert(final String s) {
            return Filter.Result.valueOf(s.toUpperCase());
        }
    }
}
