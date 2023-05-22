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
package org.apache.logging.log4j.plugins.internal.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.plugins.Factory;
import org.apache.logging.log4j.plugins.FactoryType;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.QualifierType;
import org.apache.logging.log4j.plugins.di.AmbiguousInjectConstructorException;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.plugins.di.NotInjectableException;
import org.apache.logging.log4j.plugins.di.spi.DependencyChain;
import org.apache.logging.log4j.plugins.di.spi.ResolvableKey;
import org.apache.logging.log4j.plugins.util.AnnotationUtil;
import org.apache.logging.log4j.util.Cast;
import org.apache.logging.log4j.util.InternalApi;

/**
 * Utility methods.
 */
@InternalApi
public final class BeanUtils {
    private BeanUtils() {
    }

    public static String decapitalize(final String string) {
        if (string.isEmpty()) {
            return string;
        }
        final char[] chars = string.toCharArray();
        if (chars.length >= 2 && Character.isUpperCase(chars[0]) && Character.isUpperCase(chars[1])) {
            return string;
        }
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }

    public static Executable getInjectableFactory(final Class<?> clazz) {
        return findStaticFactoryMethod(clazz)
                .or(() -> findInjectableConstructor(clazz))
                .orElseThrow(() -> new NotInjectableException(clazz));
    }

    public static Executable getInjectableFactory(final ResolvableKey<?> resolvableKey) {
        final Class<?> rawType = resolvableKey.getKey().getRawType();
        return findStaticFactoryMethod(rawType)
                .or(() -> findInjectableConstructor(rawType))
                .orElseThrow(() -> new NotInjectableException(resolvableKey));
    }

    private static Optional<Executable> findStaticFactoryMethod(final Class<?> clazz) {
        return Stream.of(clazz.getDeclaredMethods())
                .filter(method -> Modifier.isStatic(method.getModifiers()) &&
                        AnnotationUtil.isMetaAnnotationPresent(method, FactoryType.class))
                .min(Comparator.comparingInt(Method::getParameterCount).thenComparing(Method::getReturnType, (c1, c2) -> {
                    if (c1.equals(c2)) {
                        return 0;
                    } else if (Supplier.class.isAssignableFrom(c1)) {
                        return -1;
                    } else if (Supplier.class.isAssignableFrom(c2)) {
                        return 1;
                    } else {
                        return c1.getName().compareTo(c2.getName());
                    }
                }))
                .map(Executable.class::cast);
    }

    public static <T> Constructor<T> getInjectableConstructor(final Key<T> key, final DependencyChain chain) {
        return findInjectableConstructor(key.getRawType())
                .orElseThrow(() -> new NotInjectableException(key, chain));
    }

    public static List<Field> getInjectableFields(final Class<?> clazz) {
        final List<Field> fields = new ArrayList<>();
        for (Class<?> cls = clazz; cls != Object.class; cls = cls.getSuperclass()) {
            for (final Field field : cls.getDeclaredFields()) {
                if (isInjectable(field)) {
                    fields.add(field);
                }
            }
        }
        return fields;
    }

    public static List<Method> getInjectableMethods(final Class<?> clazz) {
        final List<Method> methods = new ArrayList<>();
        for (Class<?> cls = clazz; cls != Object.class; cls = cls.getSuperclass()) {
            for (final Method method : cls.getDeclaredMethods()) {
                if (isInjectable(method)) {
                    methods.add(method);
                }
            }
        }
        return methods;
    }

    /**
     * Checks the given class to see if it is {@linkplain Inject injectable}. An injectable class can be explicitly
     * injectable with a single {@link Inject}-annotated constructor, implicitly injectable with a no-args
     * constructor, or provided by a static {@link Factory}-annotated method in the class.
     *
     * @param clazz class to check support for injection
     * @return whether the class is injectable
     */
    public static boolean isInjectable(final Class<?> clazz) {
        return findInjectableConstructor(clazz).isPresent() || findStaticFactoryMethod(clazz).isPresent();
    }

    public static boolean isInjectable(final Field field) {
        return field.isAnnotationPresent(Inject.class) || AnnotationUtil.isMetaAnnotationPresent(field, QualifierType.class);
    }

    public static boolean isInjectable(final Method method) {
        if (method.isAnnotationPresent(Inject.class)) {
            return true;
        }
        if (!AnnotationUtil.isMetaAnnotationPresent(method, FactoryType.class)) {
            for (final Parameter parameter : method.getParameters()) {
                if (AnnotationUtil.isMetaAnnotationPresent(parameter, QualifierType.class)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static <T> Optional<Constructor<T>> findInjectableConstructor(final Class<T> clazz) {
        final List<Constructor<?>> constructors = Stream.of(clazz.getDeclaredConstructors())
                .filter(constructor -> constructor.isAnnotationPresent(Inject.class))
                .collect(Collectors.toList());
        final int size = constructors.size();
        if (size > 1) {
            throw new AmbiguousInjectConstructorException(clazz);
        }
        if (size == 1) {
            return Optional.of(Cast.cast(constructors.get(0)));
        }
        try {
            return Optional.of(clazz.getDeclaredConstructor());
        } catch (final NoSuchMethodException ignored) {
        }
        try {
            return Optional.of(clazz.getConstructor());
        } catch (final NoSuchMethodException ignored) {
        }
        return Optional.empty();
    }
}
