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
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@FunctionalInterface
public interface ReflectionCallerContext {
    ReflectionCallerContext DEFAULT = object -> object.setAccessible(true);

    void setAccessible(AccessibleObject object);

    default Object get(final Field field, final Object instance) {
        if (!field.canAccess(instance)) {
            setAccessible(field);
        }
        try {
            return field.get(instance);
        } catch (final IllegalAccessException e) {
            throw new InaccessibleObjectException(e.getMessage());
        }
    }

    default void set(final Field field, final Object instance, final Object value) {
        if (!field.canAccess(instance)) {
            setAccessible(field);
        }
        try {
            field.set(instance, value);
        } catch (final IllegalAccessException e) {
            throw new InaccessibleObjectException(e.getMessage());
        }
    }

    default Object invoke(final Method method, final Object instance, final Object... args) throws InvocationTargetException {
        if (!method.canAccess(instance)) {
            setAccessible(method);
        }
        try {
            return method.invoke(instance, args);
        } catch (final IllegalAccessException e) {
            throw new InaccessibleObjectException(e.getMessage());
        }
    }

    default <T> T construct(final Constructor<T> constructor, final Object... args)
            throws InvocationTargetException, InstantiationException {
        if (!constructor.canAccess(null)) {
            setAccessible(constructor);
        }
        try {
            return constructor.newInstance(args);
        } catch (final IllegalAccessException e) {
            throw new InaccessibleObjectException(e.getMessage());
        }
    }
}
