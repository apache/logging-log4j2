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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;

/**
 * Collection of basic TypeConverter implementations.
 */
public final class TypeConverters {

    private final Map<Class<?>, TypeConverter<?>> registry =
        new ConcurrentHashMap<Class<?>, TypeConverter<?>>();

    private static final class Holder {
        private static final TypeConverters INSTANCE = new TypeConverters();
    }

    private TypeConverters() {
        registry.put(String.class, new StringConverter());
        registry.put(Boolean.class, new BooleanConverter());
        registry.put(Integer.class, new IntegerConverter());
        registry.put(Long.class, new LongConverter());
        registry.put(Float.class, new FloatConverter());
        registry.put(Double.class, new DoubleConverter());
        registry.put(Level.class, new LevelConverter());
        registry.put(Filter.Result.class, new FilterResultConverter());
    }

    /**
     * Converts a String to a given class if a TypeConverter is available for that class.
     *
     * @param s     the string to convert
     * @param clazz the class to try to convert the string to
     * @return the converted object which may be {@code null} if the string is invalid for the given type
     */
    public static Object convert(final String s, final Class<?> clazz) {
        final TypeConverter<?> converter = Holder.INSTANCE.registry.get(clazz);
        // TODO: what to do if there's no converter?
        // Idea 1: use reflection to see if the class has a static "valueOf" method and use that
        // Idea 2: reflect on class's declared methods to see if any methods look suitable (probably too complex)
        if (converter == null) {
            throw new IllegalArgumentException("No type converter found for class: " + clazz.getName());
        }
        return converter.convert(s);
    }

    /**
     * Trivial identity converter.
     */
    private static class StringConverter implements TypeConverter<String> {
        @Override
        public String convert(String s) {
            return s;
        }
    }

    /**
     * Parses strings into booleans.
     */
    private static class BooleanConverter implements TypeConverter<Boolean> {
        @Override
        public Boolean convert(String s) {
            return Boolean.parseBoolean(s);
        }
    }

    /**
     * Parses strings into integers.
     */
    private static class IntegerConverter implements TypeConverter<Integer> {
        @Override
        public Integer convert(String s) {
            return Integer.parseInt(s);
        }
    }

    /**
     * Parses strings into longs.
     */
    private static class LongConverter implements TypeConverter<Long> {
        @Override
        public Long convert(String s) {
            return Long.parseLong(s);
        }
    }

    /**
     * Parses strings into floats.
     */
    private static class FloatConverter implements TypeConverter<Float> {
        @Override
        public Float convert(String s) {
            return Float.parseFloat(s);
        }
    }

    /**
     * Parses strings into doubles.
     */
    private static class DoubleConverter implements TypeConverter<Double> {
        @Override
        public Double convert(String s) {
            return Double.parseDouble(s);
        }
    }

    /**
     * Parses strings into Log4j Levels. Returns {@code null} for invalid level names.
     */
    private static class LevelConverter implements TypeConverter<Level> {
        @Override
        public Level convert(String s) {
            return Level.toLevel(s, null);
        }
    }

    /**
     * Parses strings into Filter Results. Returns {@code null} for invalid result names.
     */
    private static class FilterResultConverter implements TypeConverter<Filter.Result> {
        @Override
        public Filter.Result convert(String s) {
            return Filter.Result.toResult(s, null);
        }
    }
}
