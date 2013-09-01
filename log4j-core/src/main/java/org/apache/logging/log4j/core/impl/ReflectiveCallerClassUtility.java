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
package org.apache.logging.log4j.core.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.helpers.Loader;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Utility class that handles the instability of the Sun/OpenJDK {@code sun.reflect.Reflection.getCallerClass(int)}
 * method.<br>
 * <br>
 * <strong>Background:</strong> This method, available only in the Oracle/Sun/OpenJDK implementations of the Java
 * Virtual Machine, is a much more efficient mechanism for determining the {@link Class} of the caller of a particular
 * method. When it is not available, a {@link SecurityManager} is the second-best option. When this is also not
 * possible, the {@code StackTraceElement[]} returned by {@link Thread#getStackTrace()} must be used, and its
 * {@code String} class name converted to a {@code Class} using the slow {@link Class#forName}.<br>
 * <br>
 * As of Java 8, the {@code getCallerClass(int)} method has been removed from Oracle/OpenJDK and is no longer usable. A
 * back-port of the feature that resulted in this change was made in Java 7u25, but the {@code getCallerClass(int)} was
 * left around for that version and deprecated, with the intention of being removed in 7u40. By coincidence, the change
 * actually broke {@code getCallerClass(int)} (the return value was inadvertently offset by 1 stack frame). This was
 * actually a good thing, because it made the hundreds of libraries and frameworks relying on this method aware of what
 * the JDK developers were up to.<br>
 * <br>
 * After much community backlash, the JDK team agreed to restore {@code getCallerClass(int)} and keep its existing
 * behavior for the rest of Java 7. However, the method will still not be available in Java 8, and so backup options
 * must be used. This class:<br>
 * <ul>
 *     <li>Uses {@code getCallerClass(int)} the traditional way when possible.</li>
 *     <li>Uses {@code getCallerClass(int)} with an adjusted offset in Oracle/OpenJDK 7u25.</li>
 *     <li>Returns null otherwise. (Currently, it is the caller's responsibility to use the backup mechanisms.)</li>
 * </ul>
 * <br>
 * <strong>IMPORTANT NOTE:</strong> This class should not be relied upon. It is considered an internal class and could
 * change at any time, breaking your code if you use it. Specifically, as a possible public API replacement for
 * {@code getCallerClass(int)} develops in Java 8, this class is very likely to change or even go away.
 */
public final class ReflectiveCallerClassUtility {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static final boolean GET_CALLER_CLASS_SUPPORTED;

    private static final Method GET_CALLER_CLASS_METHOD;

    static final int JAVA_7U25_COMPENSATION_OFFSET;

    static {
        Method getCallerClass = null;
        int java7u25CompensationOffset = 0;

        try {
            final ClassLoader loader = Loader.getClassLoader();
            // Use wildcard to avoid compile-time reference.
            final Class<?> clazz = loader.loadClass("sun.reflect.Reflection");
            final Method[] methods = clazz.getMethods();
            for (final Method method : methods) {
                final int modifier = method.getModifiers();
                final Class<?>[] parameterTypes = method.getParameterTypes();
                if (method.getName().equals("getCallerClass") && Modifier.isStatic(modifier) &&
                        parameterTypes.length == 1 && parameterTypes[0] == int.class) {
                    getCallerClass = method;
                    break;
                }
            }

            if (getCallerClass == null) {
                LOGGER.info("sun.reflect.Reflection#getCallerClass does not exist.");
            } else {
                Object o = getCallerClass.invoke(null, 0);
                if (o == null || o != clazz) {
                    getCallerClass = null;
                    LOGGER.warn("sun.reflect.Reflection#getCallerClass returned unexpected value of [{}] and is " +
                            "unusable. Will fall back to another option.", o);
                } else {
                    o = getCallerClass.invoke(null, 1);
                    if (o == clazz) {
                        java7u25CompensationOffset = 1;
                        LOGGER.warn("sun.reflect.Reflection#getCallerClass is broken in Java 7u25. " +
                                "You should upgrade to 7u40. Using alternate stack offset to compensate.");
                    }
                }
            }
        } catch (final ClassNotFoundException e) {
            LOGGER.info("sun.reflect.Reflection is not installed.");
        } catch (final IllegalAccessException e) {
            LOGGER.info("sun.reflect.Reflection#getCallerClass is not accessible.");
        } catch (final InvocationTargetException e) {
            LOGGER.info("sun.reflect.Reflection#getCallerClass is not supported.");
        }

        if (getCallerClass == null) {
            GET_CALLER_CLASS_SUPPORTED = false;
            GET_CALLER_CLASS_METHOD = null;
            JAVA_7U25_COMPENSATION_OFFSET = -1;
        } else {
            GET_CALLER_CLASS_SUPPORTED = true;
            GET_CALLER_CLASS_METHOD = getCallerClass;
            JAVA_7U25_COMPENSATION_OFFSET = java7u25CompensationOffset;
        }
    }

    private ReflectiveCallerClassUtility() {

    }

    /**
     * Indicates whether {@code getCallerClass(int)} can be used on this JVM.
     *
     * @return {@code true} if it can be used. If {@code false}, {@link #getCaller} should not be called. Use a backup
     *         mechanism instead.
     */
    public static boolean isSupported() {
        return GET_CALLER_CLASS_SUPPORTED;
    }

    /**
     * Reflectively calls {@code getCallerClass(int)}, compensating for the additional frame on the stack, and
     * compensating for the Java 7u25 bug if necessary. You should check with {@link #isSupported} before using this
     * method.
     *
     * @param depth The depth of the caller to retrieve.
     * @return the caller class, or {@code null} if {@code getCallerClass(int)} is not supported.
     */
    public static Class<?> getCaller(int depth) {
        if (!GET_CALLER_CLASS_SUPPORTED) {
            return null;
        }

        try {
            return (Class<?>) GET_CALLER_CLASS_METHOD.invoke(null, depth + 1 + JAVA_7U25_COMPENSATION_OFFSET);
        } catch (IllegalAccessException ignore) {
            LOGGER.warn("Should not have failed to call getCallerClass.");
        } catch (InvocationTargetException ignore) {
            LOGGER.warn("Should not have failed to call getCallerClass.");
        }
        return null;
    }
}
