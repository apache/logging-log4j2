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
package org.apache.logging.log4j.plugins.di.spi;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;
import org.apache.logging.log4j.lang.NonNull;
import org.apache.logging.log4j.lang.NullUnmarked;

/**
 * Provides reflection operations using the calling context of another class. This is useful for centralizing
 * which module to open oneself to instead of this Log4j Plugins module.
 */
@FunctionalInterface
@NullUnmarked
public interface ReflectionAgent {

    /**
     * Invokes {@link AccessibleObject#setAccessible(boolean)} from a class contained in a module that will be
     * used to perform reflective operations.
     *
     * @param object the object to make accessible (never null)
     * @throws InaccessibleObjectException if this cannot access the provided object
     * @throws SecurityException if the request is denied by the security manager
     */
    void makeAccessible(final @NonNull AccessibleObject object);

    private <M extends AccessibleObject & Member> void makeAccessible(final M member, final Object instance) {
        Objects.requireNonNull(member, "member cannot be null");
        final boolean isStatic = Modifier.isStatic(member.getModifiers());
        if (!member.canAccess(isStatic ? null : instance)) {
            try {
                makeAccessible(member);
            } catch (final InaccessibleObjectException | SecurityException e) {
                throw new ReflectionException("Unable to make member accessible", e);
            }
        }
    }

    default Object getFieldValue(final @NonNull Field field, final Object instance) {
        makeAccessible(field, instance);
        try {
            return field.get(instance);
        } catch (final IllegalAccessException | ExceptionInInitializerError e) {
            throw new ReflectionException("Unable to get field value from " + field, e);
        }
    }

    default void setFieldValue(final @NonNull Field field, final Object instance, final Object value) {
        makeAccessible(field, instance);
        try {
            field.set(instance, value);
        } catch (final IllegalAccessException | ExceptionInInitializerError e) {
            throw new ReflectionException("Unable to set field value on " + field, e);
        }
    }

    default Object invokeMethod(final @NonNull Method method, final Object instance, final Object... args) {
        makeAccessible(method, instance);
        try {
            return method.invoke(instance, args);
        } catch (final InvocationTargetException e) {
            throw new ReflectionException(e.getMessage(), e.getCause());
        } catch (final IllegalAccessException | ExceptionInInitializerError e) {
            throw new ReflectionException("Unable to invoke method " + method, e);
        }
    }

    default <T> @NonNull T newInstance(final @NonNull Constructor<T> constructor, final Object... args) {
        makeAccessible(constructor, null);
        try {
            return constructor.newInstance(args);
        } catch (final InvocationTargetException e) {
            throw new ReflectionException(e.getMessage(), e.getCause());
        } catch (final IllegalAccessException | InstantiationException | ExceptionInInitializerError e) {
            throw new ReflectionException("Unable to invoke constructor " + constructor, e);
        }
    }

    default <T> @NonNull T newInstance(final @NonNull Class<T> clazz) {
        return newInstance(getDefaultConstructor(clazz));
    }

    private <T> @NonNull Constructor<T> getDefaultConstructor(final @NonNull Class<T> clazz) {
        try {
            return clazz.getDeclaredConstructor();
        } catch (final NoSuchMethodException | SecurityException e) {
            try {
                return clazz.getConstructor();
            } catch (final NoSuchMethodException | SecurityException ex) {
                ex.addSuppressed(e);
                throw new ReflectionException("Unable to find default constructor for " + clazz, ex);
            }
        }
    }
}
