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

/**
 * Represents the calling context in which reflection operations {@link Injector} will be performed in. This allows for
 * changing the code location where calls to {@link AccessibleObject#setAccessible(boolean)} are performed which depends
 * on the caller class and its module.
 */
@FunctionalInterface
public interface ReflectionCallerContext {
    /**
     * Default caller context where reflection is performed from the log4j-plugins module.
     */
    ReflectionCallerContext DEFAULT = object -> object.setAccessible(true);

    /**
     * Invokes {@link AccessibleObject#setAccessible(boolean)} with a value of {@code true}.
     * Location of implementation for this method determines access rules for reflection operations in {@link Injector}.
     *
     * @param object the reflection object to be made accessible
     */
    void setAccessible(AccessibleObject object);

    /**
     * Gets the value of the given field on the given instance in this caller context.
     */
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

    /**
     * Sets the value of the given field on the given instance in this caller context.
     */
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

    /**
     * Invokes the given method on the given instance in this caller context.
     */
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

    /**
     * Invokes the given constructor in this caller context.
     */
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
