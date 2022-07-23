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

package org.apache.logging.log4j.plugins.di;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

@FunctionalInterface
public interface ReflectionAccessor {
    void makeAccessible(final AccessibleObject object);

    default <M extends AccessibleObject & Member> void makeAccessible(final M member, final Object instance) {
        final boolean isStatic = Modifier.isStatic(member.getModifiers());
        if (!member.canAccess(isStatic ? null : instance)) {
            makeAccessible(member);
        }
    }

    default Object getFieldValue(final Field field, final Object instance) {
        makeAccessible(field, instance);
        try {
            return field.get(instance);
        } catch (final IllegalAccessException e) {
            throw errorFrom(e);
        }
    }

    default void setFieldValue(final Field field, final Object instance, final Object value) {
        makeAccessible(field, instance);
        try {
            field.set(instance, value);
        } catch (final IllegalAccessException e) {
            throw errorFrom(e);
        }
    }

    default Object invokeMethod(final Method method, final Object instance, final Object... args) {
        makeAccessible(method, instance);
        try {
            return method.invoke(instance, args);
        } catch (final InvocationTargetException e) {
            throw new InjectException(e.getMessage(), e.getCause());
        } catch (final IllegalAccessException e) {
            throw errorFrom(e);
        }
    }

    default <T> T newInstance(final Constructor<T> constructor, final Object[] args) {
        makeAccessible(constructor, null);
        try {
            return constructor.newInstance(args);
        } catch (final InvocationTargetException e) {
            throw new InjectException(e.getMessage(), e.getCause());
        } catch (final IllegalAccessException e) {
            throw errorFrom(e);
        } catch (final InstantiationException e) {
            throw new InjectException(e.getMessage(), e);
        }
    }

    private static IllegalAccessError errorFrom(final IllegalAccessException e) {
        final IllegalAccessError error = new IllegalAccessError(e.getMessage());
        error.initCause(e);
        return error;
    }
}
