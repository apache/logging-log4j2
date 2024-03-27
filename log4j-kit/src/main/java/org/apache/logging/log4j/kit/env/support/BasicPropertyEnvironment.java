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
package org.apache.logging.log4j.kit.env.support;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.IllformedLocaleException;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.kit.env.Log4jProperty;
import org.apache.logging.log4j.kit.env.PropertyEnvironment;
import org.jspecify.annotations.Nullable;

/**
 * An implementation of {@link PropertyEnvironment} that only uses basic Java functions.
 * <p>
 * Conversion problems are logged using a status logger.
 * </p>
 */
public abstract class BasicPropertyEnvironment implements PropertyEnvironment {

    private final Logger statusLogger;

    protected BasicPropertyEnvironment(final Logger statusLogger) {
        this.statusLogger = statusLogger;
    }

    @Override
    public Boolean getBooleanProperty(final String name, final Boolean defaultValue) {
        return getObjectPropertyWithTypedDefault(name, this::toBoolean, defaultValue);
    }

    @Override
    public Charset getCharsetProperty(final String name, final Charset defaultValue) {
        return getObjectPropertyWithTypedDefault(name, this::toCharset, defaultValue);
    }

    @Override
    public <T> @Nullable Class<? extends T> getClassProperty(final String name, final Class<T> upperBound) {
        return getClassProperty(name, null, upperBound);
    }

    @Override
    public <T> Class<? extends T> getClassProperty(
            final String name, final Class<? extends T> defaultValue, final Class<T> upperBound) {
        return getObjectPropertyWithTypedDefault(name, className -> toClass(className, upperBound), defaultValue);
    }

    @Override
    public Duration getDurationProperty(final String name, final Duration defaultValue) {
        return getObjectPropertyWithTypedDefault(name, this::toDuration, defaultValue);
    }

    @Override
    public Integer getIntegerProperty(final String name, final Integer defaultValue) {
        return getObjectPropertyWithTypedDefault(name, this::toInteger, defaultValue);
    }

    @Override
    public Long getLongProperty(final String name, final Long defaultValue) {
        return getObjectPropertyWithTypedDefault(name, this::toLong, defaultValue);
    }

    @Override
    public abstract @Nullable String getStringProperty(String name);

    @Override
    public <T> T getProperty(final Class<T> propertyClass) {
        if (!propertyClass.isAnnotationPresent(Log4jProperty.class)) {
            throw new IllegalArgumentException("Unsupported configuration properties class '" + propertyClass.getName()
                    + "': missing '@Log4jProperty' annotation.");
        }
        return getRecordProperty(null, propertyClass);
    }

    protected Class<?> getClassForName(final String className) throws ReflectiveOperationException {
        return Class.forName(className);
    }

    protected Boolean toBoolean(final String value) {
        return Boolean.valueOf(value);
    }

    protected @Nullable Charset toCharset(final String value) {
        try {
            return Log4jProperty.SYSTEM.equals(value) ? Charset.defaultCharset() : Charset.forName(value);
        } catch (final IllegalCharsetNameException | UnsupportedOperationException e) {
            statusLogger.warn("Invalid Charset value '{}': {}", value, e.getMessage(), e);
        }
        return null;
    }

    protected @Nullable Duration toDuration(final CharSequence value) {
        try {
            return Duration.parse(value);
        } catch (final DateTimeParseException e) {
            statusLogger.warn("Invalid Duration value '{}': {}", value, e.getMessage(), e);
        }
        return null;
    }

    protected char[] toCharArray(final String value) {
        return value.toCharArray();
    }

    @SuppressWarnings("unchecked")
    protected <T> @Nullable Class<? extends T> toClass(final String className, final Class<T> upperBound) {
        try {
            final Class<?> clazz = getClassForName(className);
            if (upperBound.isAssignableFrom(clazz)) {
                return (Class<? extends T>) clazz;
            }
            statusLogger.warn("Invalid Class value '{}': class does not extend {}.", className, upperBound.getName());
        } catch (final ReflectiveOperationException e) {
            statusLogger.warn("Invalid Class value '{}': {}", className, e.getMessage(), e);
        }
        return null;
    }

    protected <T extends Enum<T>> @Nullable T toEnum(final String value, final Class<T> enumClass) {
        try {
            return Enum.valueOf(enumClass, value);
        } catch (final IllegalArgumentException e) {
            statusLogger.warn("Invalid enum value '{}' of type {}.", value, enumClass.getName(), e);
        }
        return null;
    }

    protected @Nullable Integer toInteger(final String value) {
        try {
            return Integer.valueOf(value);
        } catch (final NumberFormatException e) {
            statusLogger.warn("Invalid integer value '{}': {}.", value, e.getMessage(), e);
        }
        return null;
    }

    protected @Nullable Locale toLocale(final String value) {
        try {
            if (Log4jProperty.SYSTEM.equals(value)) {
                return Locale.getDefault();
            }
            // Allows the usage of POSIX locale names like 'pl_PL'
            final String languageTag = value.replace('_', '-');
            return new Locale.Builder().setLanguageTag(languageTag).build();
        } catch (final IllformedLocaleException e) {
            statusLogger.warn("Invalid locale value '{}': {}.", value, e.getMessage(), e);
        }
        return null;
    }

    protected @Nullable Long toLong(final String value) {
        try {
            return Long.valueOf(value);
        } catch (final NumberFormatException e) {
            statusLogger.warn("Invalid long value '{}': {}.", value, e.getMessage(), e);
        }
        return null;
    }

    protected @Nullable Path toPath(final String value) {
        try {
            return Paths.get(value);
        } catch (final InvalidPathException e) {
            statusLogger.warn("Invalid path value {}: {}.", value, e.getMessage(), e);
        }
        return null;
    }

    protected @Nullable Level toLevel(final String value) {
        return Level.toLevel(value, null);
    }

    protected @Nullable TimeZone toTimeZone(final String value) {
        final ZoneId zoneId = toZoneId(value);
        return zoneId != null ? TimeZone.getTimeZone(zoneId) : null;
    }

    protected @Nullable ZoneId toZoneId(final String value) {
        try {
            return Log4jProperty.SYSTEM.equals(value) ? ZoneId.systemDefault() : ZoneId.of(value);
        } catch (final DateTimeException e) {
            statusLogger.warn("Invalid timezone id value '{}': {}.", value, e.getMessage(), e);
        }
        return null;
    }

    private <T> T getRecordProperty(final @Nullable String parentPrefix, final Class<T> propertyClass) {
        if (!propertyClass.isRecord()) {
            throw new IllegalArgumentException("Unsupported configuration properties class '" + propertyClass.getName()
                    + "': class is not a record.");
        }
        final String prefix =
                parentPrefix != null ? parentPrefix : getPropertyName(propertyClass, propertyClass::getSimpleName);

        @SuppressWarnings("unchecked")
        final Constructor<T>[] constructors = (Constructor<T>[]) propertyClass.getDeclaredConstructors();
        if (constructors.length == 0) {
            throw new IllegalArgumentException("Unsupported configuration properties class '" + propertyClass.getName()
                    + "': missing public constructor.");
        } else if (constructors.length > 1) {
            throw new IllegalArgumentException("Unsupported configuration properties class '" + propertyClass.getName()
                    + "': more than one constructor found.");
        }
        final Constructor<T> constructor = constructors[0];

        final Parameter[] parameters = constructor.getParameters();
        final @Nullable Object[] initArgs = new Object[parameters.length];
        for (int i = 0; i < initArgs.length; i++) {
            final String name = prefix + "." + getPropertyName(parameters[i], parameters[i]::getName);
            final String defaultValue = getPropertyDefaultAsString(parameters[i]);
            initArgs[i] = getObjectProperty(name, parameters[i].getParameterizedType(), defaultValue);
        }
        try {
            return constructor.newInstance(initArgs);
        } catch (final ReflectiveOperationException e) {
            throw new IllegalArgumentException(
                    "Unable to parse configuration properties class " + propertyClass.getName() + ": " + e.getMessage(),
                    e);
        }
    }

    private @Nullable Object getObjectProperty(
            final String name, final Type type, final @Nullable String defaultValue) {
        if (type instanceof final ParameterizedType parameterizedType
                && parameterizedType.getRawType().equals(Class.class)) {
            final Type[] arguments = parameterizedType.getActualTypeArguments();
            final Class<?> upperBound = arguments.length > 0 ? findUpperBound(arguments[0]) : Object.class;
            return getObjectPropertyWithStringDefault(name, defaultValue, className -> toClass(className, upperBound));
        }
        if (type instanceof final Class<?> clazz) {
            if (clazz.isRecord()) {
                return getRecordProperty(name, clazz);
            }
            if (char[].class.equals(clazz)) {
                return getObjectPropertyWithStringDefault(name, defaultValue, this::toCharArray);
            }
            if (boolean.class.equals(clazz)) {
                return getObjectPropertyWithStringDefault(
                        name, Objects.toString(defaultValue, "false"), this::toBoolean);
            }
            if (Boolean.class.equals(clazz)) {
                return getObjectPropertyWithStringDefault(name, defaultValue, this::toBoolean);
            }
            if (Charset.class.equals(clazz)) {
                return getObjectPropertyWithStringDefault(name, defaultValue, this::toCharset);
            }
            if (Duration.class.equals(clazz)) {
                return getObjectPropertyWithStringDefault(name, defaultValue, this::toDuration);
            }
            if (Enum.class.isAssignableFrom(clazz)) {
                return getObjectPropertyWithStringDefault(
                        name, defaultValue, value -> toEnum(value, (Class<? extends Enum>) clazz));
            }
            if (int.class.equals(clazz)) {
                return getObjectPropertyWithStringDefault(name, Objects.toString(defaultValue, "0"), this::toInteger);
            }
            if (Integer.class.equals(clazz)) {
                return getObjectPropertyWithStringDefault(name, defaultValue, this::toInteger);
            }
            if (Locale.class.equals(clazz)) {
                return getObjectPropertyWithStringDefault(name, defaultValue, this::toLocale);
            }
            if (long.class.equals(clazz)) {
                return getObjectPropertyWithStringDefault(name, Objects.toString(defaultValue, "0"), this::toLong);
            }
            if (Long.class.equals(clazz)) {
                return getObjectPropertyWithStringDefault(name, defaultValue, this::toLong);
            }
            if (Level.class.equals(clazz)) {
                return getObjectPropertyWithStringDefault(name, defaultValue, this::toLevel);
            }
            if (Path.class.equals(clazz)) {
                return getObjectPropertyWithStringDefault(name, defaultValue, this::toPath);
            }
            if (TimeZone.class.equals(clazz)) {
                return getObjectPropertyWithStringDefault(name, defaultValue, this::toTimeZone);
            }
            if (ZoneId.class.equals(clazz)) {
                return getObjectPropertyWithStringDefault(name, defaultValue, this::toZoneId);
            }
            if (String.class.equals(clazz)) {
                return getObjectPropertyWithStringDefault(name, defaultValue, x -> x);
            }
        }
        throw new IllegalArgumentException("Unsupported property of type '" + type.getTypeName() + "'");
    }

    private Class<?> findUpperBound(final Type type) {
        final Type[] bounds;
        if (type instanceof final TypeVariable<?> typeVariable) {
            bounds = typeVariable.getBounds();
        } else if (type instanceof final WildcardType wildcardType) {
            bounds = wildcardType.getUpperBounds();
        } else {
            bounds = new Type[0];
        }
        return bounds.length > 0 && bounds[0] instanceof final Class<?> clazz ? clazz : Object.class;
    }

    private String getPropertyName(final AnnotatedElement element, final Supplier<String> fallback) {
        if (element.isAnnotationPresent(Log4jProperty.class)) {
            final String specifiedName =
                    element.getAnnotation(Log4jProperty.class).name();
            if (!specifiedName.isEmpty()) {
                return specifiedName;
            }
        }
        return fallback.get();
    }

    private @Nullable String getPropertyDefaultAsString(final AnnotatedElement parameter) {
        if (parameter.isAnnotationPresent(Log4jProperty.class)) {
            final String defaultValue =
                    parameter.getAnnotation(Log4jProperty.class).defaultValue();
            if (!defaultValue.isEmpty()) {
                return defaultValue;
            }
        }
        return null;
    }

    private <T> @Nullable Object getObjectPropertyWithStringDefault(
            final String name, final @Nullable String defaultValue, final Function<? super String, ?> converter) {
        final String prop = getStringProperty(name);
        if (prop != null) {
            final @Nullable Object value = converter.apply(prop);
            if (value != null) {
                return value;
            }
        }
        return defaultValue != null ? converter.apply(defaultValue) : null;
    }

    private <T> T getObjectPropertyWithTypedDefault(
            final String name, final Function<? super String, ? extends @Nullable T> converter, final T defaultValue) {
        final String prop = getStringProperty(name);
        if (prop != null) {
            final @Nullable T value = converter.apply(prop);
            if (value != null) {
                return value;
            }
        }
        return defaultValue;
    }
}
