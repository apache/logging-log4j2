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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.UnknownFormatConversionException;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Singleton;
import org.apache.logging.log4j.plugins.util.TypeUtil;
import org.apache.logging.log4j.spi.RecyclerFactories;
import org.apache.logging.log4j.spi.RecyclerFactory;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.EnglishEnums;

@Singleton
public class TypeConverterFactory {
    private static final Logger LOGGER = StatusLogger.getLogger();
    private final Map<Type, TypeConverter<?>> typeConverters = new ConcurrentHashMap<>();

    @Inject
    public TypeConverterFactory(@TypeConverters List<TypeConverter<?>> typeConverters) {
        typeConverters.forEach(converter -> registerTypeConverter(getTypeConverterSupportedType(converter.getClass()), converter));
        registerTypeConverter(Boolean.class, Boolean::valueOf);
        registerTypeAlias(Boolean.class, Boolean.TYPE);
        registerTypeConverter(Byte.class, Byte::valueOf);
        registerTypeAlias(Byte.class, Byte.TYPE);
        registerTypeConverter(Character.class, s -> {
            if (s.length() != 1) {
                throw new IllegalArgumentException("Character string must be of length 1: " + s);
            }
            return s.toCharArray()[0];
        });
        registerTypeAlias(Character.class, Character.TYPE);
        registerTypeConverter(Double.class, Double::valueOf);
        registerTypeAlias(Double.class, Double.TYPE);
        registerTypeConverter(Float.class, Float::valueOf);
        registerTypeAlias(Float.class, Float.TYPE);
        registerTypeConverter(Integer.class, Integer::valueOf);
        registerTypeAlias(Integer.class, Integer.TYPE);
        registerTypeConverter(Long.class, Long::valueOf);
        registerTypeAlias(Long.class, Long.TYPE);
        registerTypeConverter(RecyclerFactory.class, RecyclerFactories::ofSpec);
        registerTypeConverter(Short.class, Short::valueOf);
        registerTypeAlias(Short.class, Short.TYPE);
        registerTypeConverter(String.class, s -> s);
    }

    public TypeConverter<?> getTypeConverter(final Type type) {
        final TypeConverter<?> primary = typeConverters.get(type);
        // cached type converters
        if (primary != null) {
            return primary;
        }
        // dynamic enum support
        if (type instanceof Class<?>) {
            final Class<?> clazz = (Class<?>) type;
            if (clazz.isEnum()) {
                return registerTypeConverter(type, s -> EnglishEnums.valueOf(clazz.asSubclass(Enum.class), s));
            }
        }
        // look for compatible converters
        for (final Map.Entry<Type, TypeConverter<?>> entry : typeConverters.entrySet()) {
            final Type key = entry.getKey();
            if (TypeUtil.isAssignable(type, key)) {
                LOGGER.debug("Found compatible TypeConverter<{}> for type [{}].", key, type);
                final TypeConverter<?> value = entry.getValue();
                return registerTypeConverter(type, value);
            }
        }
        throw new UnknownFormatConversionException(type.toString());
    }

    private void registerTypeAlias(final Type knownType, final Type aliasType) {
        final TypeConverter<?> converter = typeConverters.get(knownType);
        if (converter != null) {
            typeConverters.put(aliasType, converter);
        } else {
            LOGGER.error("Cannot locate type converter for {}", knownType);
        }
    }

    private TypeConverter<?> registerTypeConverter(final Type type, final TypeConverter<?> converter) {
        final TypeConverter<?> conflictingConverter = typeConverters.get(type);
        if (conflictingConverter != null) {
            final boolean overridable;
            if (converter instanceof Comparable) {
                @SuppressWarnings("unchecked") final Comparable<TypeConverter<?>> comparableConverter =
                        (Comparable<TypeConverter<?>>) converter;
                overridable = comparableConverter.compareTo(conflictingConverter) < 0;
            } else if (conflictingConverter instanceof Comparable) {
                @SuppressWarnings("unchecked") final Comparable<TypeConverter<?>> comparableConflictingConverter =
                        (Comparable<TypeConverter<?>>) conflictingConverter;
                overridable = comparableConflictingConverter.compareTo(converter) > 0;
            } else {
                overridable = false;
            }
            if (overridable) {
                LOGGER.debug(
                        "Replacing TypeConverter [{}] for type [{}] with [{}] after comparison.",
                        conflictingConverter, type, converter);
                typeConverters.put(type, converter);
                return converter;
            } else {
                LOGGER.warn(
                        "Ignoring TypeConverter [{}] for type [{}] that conflicts with [{}], since they are not comparable.",
                        converter, type, conflictingConverter);
                return conflictingConverter;
            }
        } else {
            typeConverters.put(type, converter);
            return converter;
        }
    }

    private static Type getTypeConverterSupportedType(final Class<?> clazz) {
        for (final Type type : clazz.getGenericInterfaces()) {
            if (type instanceof ParameterizedType) {
                final ParameterizedType parameterizedType = (ParameterizedType) type;
                if (parameterizedType.getRawType() == TypeConverter.class) {
                    return parameterizedType.getActualTypeArguments()[0];
                }
            }
        }
        return Void.TYPE;
    }
}
