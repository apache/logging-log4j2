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
package org.apache.logging.log4j.util;

import java.util.Deque;
import java.util.NoSuchElementException;
import java.util.function.Predicate;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * <em>Consider this class private.</em> Provides various methods to determine the caller class.
 */
@InternalApi
public final class StackLocatorUtil {
    private static StackLocator stackLocator = null;
    private static volatile boolean errorLogged;

    static {
        stackLocator = StackLocator.getInstance();
    }

    private StackLocatorUtil() {}

    // TODO: return Object.class instead of null (though it will have a null ClassLoader)
    // (MS) I believe this would work without any modifications elsewhere, but I could be wrong

    // migrated from ReflectiveCallerClassUtility
    @PerformanceSensitive
    public static Class<?> getCallerClass(final int depth) {
        return stackLocator.getCallerClass(depth + 1);
    }

    public static StackTraceElement getStackTraceElement(final int depth) {
        return stackLocator.getStackTraceElement(depth + 1);
    }

    /**
     * Equivalent to {@link #getCallerClass(String, String)} with an empty {@code pkg}.
     */
    // migrated from ClassLoaderContextSelector
    @PerformanceSensitive
    public static Class<?> getCallerClass(final String fqcn) {
        return getCallerClass(fqcn, Strings.EMPTY);
    }

    /**
     * Search for a calling class.
     *
     * @param fqcn Root class name whose caller to search for.
     * @param pkg Package name prefix that must be matched after the {@code fqcn} has been found.
     * @return The caller class that was matched, or null if one could not be located.
     */
    @PerformanceSensitive
    public static Class<?> getCallerClass(final String fqcn, final String pkg) {
        return stackLocator.getCallerClass(fqcn, pkg);
    }

    /**
     * Gets the ClassLoader of the class that called <em>this</em> method at the location up the call stack by the given
     * stack frame depth.
     * <p>
     * This method returns {@code null} if:
     * </p>
     * <ul>
     * <li>{@code sun.reflect.Reflection.getCallerClass(int)} is not present.</li>
     * <li>An exception is caught calling {@code sun.reflect.Reflection.getCallerClass(int)}.</li>
     * <li>Some Class implementations may use null to represent the bootstrap class loader.</li>
     * </ul>
     *
     * @param depth The stack frame count to walk.
     * @return A class or null.
     * @throws IndexOutOfBoundsException if depth is negative.
     */
    @PerformanceSensitive
    public static ClassLoader getCallerClassLoader(final int depth) {
        final Class<?> callerClass = stackLocator.getCallerClass(depth + 1);
        return callerClass != null ? callerClass.getClassLoader() : null;
    }

    /**
     * Search for a calling class.
     *
     * @param sentinelClass Sentinel class at which to begin searching
     * @param callerPredicate Predicate checked after the sentinelClass is found
     * @return the first matching class after <code>sentinelClass</code> is found.
     */
    @PerformanceSensitive
    public static Class<?> getCallerClass(final Class<?> sentinelClass, final Predicate<Class<?>> callerPredicate) {
        return stackLocator.getCallerClass(sentinelClass, callerPredicate);
    }

    // added for use in LoggerAdapter implementations mainly
    @PerformanceSensitive
    public static Class<?> getCallerClass(final Class<?> anchor) {
        return stackLocator.getCallerClass(anchor);
    }

    // migrated from ThrowableProxy
    @PerformanceSensitive
    public static Deque<Class<?>> getCurrentStackTrace() {
        return stackLocator.getCurrentStackTrace();
    }

    public static StackTraceElement calcLocation(final String fqcnOfLogger) {
        try {
            return stackLocator.calcLocation(fqcnOfLogger);
        } catch (NoSuchElementException ex) {
            if (!errorLogged) {
                errorLogged = true;
                StatusLogger.getLogger().warn("Unable to locate stack trace element for {}", fqcnOfLogger, ex);
            }
            return null;
        }
    }
}
