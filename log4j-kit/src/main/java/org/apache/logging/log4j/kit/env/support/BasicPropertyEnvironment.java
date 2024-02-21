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

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.time.Duration;
import java.time.format.DateTimeParseException;
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
    public @Nullable Boolean getBooleanProperty(final String name, final @Nullable Boolean defaultValue) {
        final String prop = getStringProperty(name);
        return prop == null ? defaultValue : Boolean.parseBoolean(prop);
    }

    @Override
    public @Nullable Charset getCharsetProperty(final String name, final @Nullable Charset defaultValue) {
        final String charsetName = getStringProperty(name);
        if (charsetName == null) {
            return defaultValue;
        }
        try {
            return Charset.forName(charsetName);
        } catch (final IllegalCharsetNameException | UnsupportedOperationException e) {
            statusLogger.warn(
                    "Unable to get Charset '{}' for property '{}', using default '{}'.",
                    charsetName,
                    name,
                    defaultValue,
                    e);
        }
        return defaultValue;
    }

    @Override
    public <T> @Nullable Class<? extends T> getClassProperty(
            final String name, final @Nullable Class<? extends T> defaultValue, final Class<T> upperBound) {
        final String className = getStringProperty(name);
        if (className == null) {
            return defaultValue;
        }
        try {
            final Class<?> clazz = getClassForName(className);
            if (upperBound.isAssignableFrom(clazz)) {
                return (Class<? extends T>) clazz;
            }
            statusLogger.warn(
                    "Unable to get Class '{}' for property '{}': class does not extend {}.",
                    className,
                    name,
                    upperBound.getName());
        } catch (final ReflectiveOperationException e) {
            statusLogger.warn(
                    "Unable to get Class '{}' for property '{}', using default '{}'.",
                    className,
                    name,
                    defaultValue,
                    e);
        }
        return defaultValue;
    }

    protected Class<?> getClassForName(final String className) throws ReflectiveOperationException {
        return Class.forName(className);
    }

    @Override
    public @Nullable Duration getDurationProperty(final String name, final @Nullable Duration defaultValue) {
        final String prop = getStringProperty(name);
        if (prop != null) {
            try {
                return Duration.parse(prop);
            } catch (final DateTimeParseException ignored) {
                statusLogger.warn(
                        "Invalid Duration value '{}' for property '{}', using default '{}'.", prop, name, defaultValue);
            }
        }
        return defaultValue;
    }

    @Override
    public @Nullable Integer getIntegerProperty(final String name, final @Nullable Integer defaultValue) {
        final String prop = getStringProperty(name);
        if (prop != null) {
            try {
                return Integer.parseInt(prop);
            } catch (final Exception ignored) {
                statusLogger.warn(
                        "Invalid integer value '{}' for property '{}', using default '{}'.", prop, name, defaultValue);
            }
        }
        return defaultValue;
    }

    @Override
    public @Nullable Long getLongProperty(final String name, final @Nullable Long defaultValue) {
        final String prop = getStringProperty(name);
        if (prop != null) {
            try {
                return Long.parseLong(prop);
            } catch (final Exception ignored) {
                statusLogger.warn(
                        "Invalid long value '{}' for property '{}', using default '{}'.", prop, name, defaultValue);
            }
        }
        return defaultValue;
    }

    @Override
    public abstract @Nullable String getStringProperty(String name);

    @Override
    public <T> T getProperty(final Class<T> propertyClass) {
        if (!propertyClass.isRecord()) {
            throw new IllegalArgumentException("Unsupported configuration properties class '" + propertyClass.getName()
                    + "': class is not a record.");
        }
        if (propertyClass.getAnnotation(Log4jProperty.class) == null) {
            throw new IllegalArgumentException("Unsupported configuration properties class '" + propertyClass.getName()
                    + "': missing '@Log4jProperty' annotation.");
        }
        return getProperty(null, propertyClass);
    }

    private <T> T getProperty(final @Nullable String parentPrefix, final Class<T> propertyClass) {
        final Log4jProperty annotation = propertyClass.getAnnotation(Log4jProperty.class);
        final String prefix = parentPrefix != null
                ? parentPrefix
                : annotation != null && annotation.name().isEmpty() ? propertyClass.getSimpleName() : annotation.name();

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
        final Object[] initArgs = new Object[parameters.length];
        for (int i = 0; i < initArgs.length; i++) {
            initArgs[i] = getProperty(prefix, parameters[i]);
        }
        try {
            return constructor.newInstance(initArgs);
        } catch (final ReflectiveOperationException e) {
            statusLogger.warn("Unable to parse configuration properties class {}.", propertyClass.getName(), e);
            return null;
        }
    }

    private Object getProperty(final String parentPrefix, final Parameter parameter) {
        if (!parameter.isNamePresent()) {
            statusLogger.warn("Missing parameter name on configuration parameter {}.", parameter);
            return null;
        }
        final String key = parentPrefix + "." + parameter.getName();
        final Class<?> type = parameter.getType();
        if (boolean.class.equals(type)) {
            return getBooleanProperty(key);
        }
        if (Class.class.equals(type)) {
            return getClassProperty(key, parameter.getAnnotatedType().getType());
        }
        if (Charset.class.equals(type)) {
            return getCharsetProperty(key);
        }
        if (Duration.class.equals(type)) {
            return getDurationProperty(key);
        }
        if (Enum.class.isAssignableFrom(type)) {
            final String prop = getStringProperty(key);
            if (prop != null) {
                try {
                    return Enum.valueOf((Class<? extends Enum>) type, prop);
                } catch (final IllegalArgumentException e) {
                    statusLogger.warn("Invalid {} value '{}' for property '{}'.", type.getSimpleName(), prop, key);
                }
            }
            return null;
        }
        if (int.class.equals(type)) {
            return getIntegerProperty(key);
        }
        if (long.class.equals(type)) {
            return getLongProperty(key);
        }
        return String.class.equals(type) ? getStringProperty(key) : getProperty(key, type);
    }

    private Object getClassProperty(final String key, final Type type) {
        Class<?> upperBound = Object.class;
        if (type instanceof final ParameterizedType parameterizedType) {
            final Type[] arguments = parameterizedType.getActualTypeArguments();
            if (arguments.length > 0) {
                upperBound = findUpperBound(arguments[0]);
            }
        }
        return getClassProperty(key, null, upperBound);
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
}
