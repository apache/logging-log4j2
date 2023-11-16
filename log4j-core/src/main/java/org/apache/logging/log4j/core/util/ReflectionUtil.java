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
package org.apache.logging.log4j.core.util;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.util.Objects;

/**
 * Utility class for performing common reflective operations.
 *
 * @since 2.1
 */
public final class ReflectionUtil {
    private ReflectionUtil() {}

    /**
     * Indicates whether or not a {@link Member} is both public and is contained in a public class.
     *
     * @param <T> type of the object whose accessibility to test
     * @param member the Member to check for public accessibility (must not be {@code null}).
     * @return {@code true} if {@code member} is public and contained in a public class.
     * @throws NullPointerException if {@code member} is {@code null}.
     */
    public static <T extends AccessibleObject & Member> boolean isAccessible(final T member) {
        Objects.requireNonNull(member, "No member provided");
        return Modifier.isPublic(member.getModifiers())
                && Modifier.isPublic(member.getDeclaringClass().getModifiers());
    }

    /**
     * Makes a {@link Member} {@link AccessibleObject#isAccessible() accessible} if the member is not public.
     *
     * @param <T> type of the object to make accessible
     * @param member the Member to make accessible (must not be {@code null}).
     * @throws NullPointerException if {@code member} is {@code null}.
     */
    public static <T extends AccessibleObject & Member> void makeAccessible(final T member) {
        if (!isAccessible(member) && !member.isAccessible()) {
            member.setAccessible(true);
        }
    }

    /**
     * Makes a {@link Field} {@link AccessibleObject#isAccessible() accessible} if it is not public or if it is final.
     *
     * <p>Note that using this method to make a {@code final} field writable will most likely not work very well due to
     * compiler optimizations and the like.</p>
     *
     * @param field the Field to make accessible (must not be {@code null}).
     * @throws NullPointerException if {@code field} is {@code null}.
     */
    public static void makeAccessible(final Field field) {
        Objects.requireNonNull(field, "No field provided");
        if ((!isAccessible(field) || Modifier.isFinal(field.getModifiers())) && !field.isAccessible()) {
            field.setAccessible(true);
        }
    }

    /**
     * Gets the value of a {@link Field}, making it accessible if required.
     *
     * @param field    the Field to obtain a value from (must not be {@code null}).
     * @param instance the instance to obtain the field value from or {@code null} only if the field is static.
     * @return the value stored by the field.
     * @throws NullPointerException if {@code field} is {@code null}, or if {@code instance} is {@code null} but
     *                              {@code field} is not {@code static}.
     * @see Field#get(Object)
     */
    public static Object getFieldValue(final Field field, final Object instance) {
        makeAccessible(field);
        if (!Modifier.isStatic(field.getModifiers())) {
            Objects.requireNonNull(instance, "No instance given for non-static field");
        }
        try {
            return field.get(instance);
        } catch (final IllegalAccessException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    /**
     * Gets the value of a static {@link Field}, making it accessible if required.
     *
     * @param field the Field to obtain a value from (must not be {@code null}).
     * @return the value stored by the static field.
     * @throws NullPointerException if {@code field} is {@code null}, or if {@code field} is not {@code static}.
     * @see Field#get(Object)
     */
    public static Object getStaticFieldValue(final Field field) {
        return getFieldValue(field, null);
    }

    /**
     * Sets the value of a {@link Field}, making it accessible if required.
     *
     * @param field    the Field to write a value to (must not be {@code null}).
     * @param instance the instance to write the value to or {@code null} only if the field is static.
     * @param value    the (possibly wrapped) value to write to the field.
     * @throws NullPointerException if {@code field} is {@code null}, or if {@code instance} is {@code null} but
     *                              {@code field} is not {@code static}.
     * @see Field#set(Object, Object)
     */
    public static void setFieldValue(final Field field, final Object instance, final Object value) {
        makeAccessible(field);
        if (!Modifier.isStatic(field.getModifiers())) {
            Objects.requireNonNull(instance, "No instance given for non-static field");
        }
        try {
            field.set(instance, value);
        } catch (final IllegalAccessException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    /**
     * Sets the value of a static {@link Field}, making it accessible if required.
     *
     * @param field the Field to write a value to (must not be {@code null}).
     * @param value the (possibly wrapped) value to write to the field.
     * @throws NullPointerException if {@code field} is {@code null}, or if {@code field} is not {@code static}.
     * @see Field#set(Object, Object)
     */
    public static void setStaticFieldValue(final Field field, final Object value) {
        setFieldValue(field, null, value);
    }

    /**
     * Gets the default (no-arg) constructor for a given class.
     *
     * @param clazz the class to find a constructor for
     * @param <T>   the type made by the constructor
     * @return the default constructor for the given class
     * @throws IllegalStateException if no default constructor can be found
     */
    public static <T> Constructor<T> getDefaultConstructor(final Class<T> clazz) {
        Objects.requireNonNull(clazz, "No class provided");
        try {
            final Constructor<T> constructor = clazz.getDeclaredConstructor();
            makeAccessible(constructor);
            return constructor;
        } catch (final NoSuchMethodException ignored) {
            try {
                final Constructor<T> constructor = clazz.getConstructor();
                makeAccessible(constructor);
                return constructor;
            } catch (final NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     * Constructs a new {@code T} object using the default constructor of its class. Any exceptions thrown by the
     * constructor will be rethrown by this method, possibly wrapped in an
     * {@link java.lang.reflect.UndeclaredThrowableException}.
     *
     * @param clazz the class to use for instantiation.
     * @param <T>   the type of the object to construct.
     * @return a new instance of T made from its default constructor.
     * @throws IllegalArgumentException if the given class is abstract, an interface, an array class, a primitive type,
     *                                  or void
     * @throws IllegalStateException    if access is denied to the constructor, or there are no default constructors
     * @throws InternalError        wrapper of the underlying exception if checked
     */
    public static <T> T instantiate(final Class<T> clazz) {
        Objects.requireNonNull(clazz, "No class provided");
        final Constructor<T> constructor = getDefaultConstructor(clazz);
        try {
            return constructor.newInstance();
        } catch (final LinkageError | InstantiationException e) {
            // LOG4J2-1051
            // On platforms like Google App Engine and Android, some JRE classes are not supported: JMX, JNDI, etc.
            throw new IllegalArgumentException(e);
        } catch (final IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (final InvocationTargetException e) {
            Throwables.rethrow(e.getCause());
            throw new InternalError("Unreachable");
        }
    }
}
