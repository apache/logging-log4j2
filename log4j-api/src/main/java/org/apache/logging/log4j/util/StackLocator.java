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

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Predicate;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * <em>Consider this class private.</em> Provides various methods to determine the caller class.
 *
 * <h2>Background</h2>
 * <p>
 * This method, available only in the Oracle/Sun/OpenJDK implementations of the Java Virtual Machine, is a much more
 * efficient mechanism for determining the {@link Class} of the caller of a particular method. When it is not available,
 * a {@link SecurityManager} is the second-best option. When this is also not possible, the {@code StackTraceElement[]}
 * returned by {@link Throwable#getStackTrace()} must be used, and its {@code String} class name converted to a
 * {@code Class} using the slow {@link Class#forName} (which can add an extra microsecond or more for each invocation
 * depending on the runtime ClassLoader hierarchy).
 * </p>
 * <p>
 * During Java 8 development, the {@code sun.reflect.Reflection.getCallerClass(int)} was removed from OpenJDK, and this
 * change was back-ported to Java 7 in version 1.7.0_25 which changed the behavior of the call and caused it to be off
 * by one stack frame. This turned out to be beneficial for the survival of this API as the change broke hundreds of
 * libraries and frameworks relying on the API which brought much more attention to the intended API removal.
 * </p>
 * <p>
 * After much community backlash, the JDK team agreed to restore {@code getCallerClass(int)} and keep its existing
 * behavior for the rest of Java 7. However, the method is deprecated in Java 8, and current Java 9 development has not
 * addressed this API. Therefore, the functionality of this class cannot be relied upon for all future versions of Java.
 * It does, however, work just fine in Sun JDK 1.6, OpenJDK 1.6, Oracle/OpenJDK 1.7, and Oracle/OpenJDK 1.8. Other Java
 * environments may fall back to using {@link Throwable#getStackTrace()} which is significantly slower due to
 * examination of every virtual frame of execution.
 * </p>
 */
@InternalApi
public final class StackLocator {

    private static final Logger LOGGER = StatusLogger.getLogger();

    /** TODO Consider removing now that we require Java 8. */
    static final int JDK_7U25_OFFSET;

    private static final Method GET_CALLER_CLASS_METHOD;

    private static final StackLocator INSTANCE;

    /** TODO: Use Object.class. */
    private static final Class<?> DEFAULT_CALLER_CLASS = null;

    static {
        Method getCallerClassMethod;
        int java7u25CompensationOffset = 0;
        try {
            // Do not use `LoaderUtil` here, since it causes a cycle of dependencies:
            // LoaderUtil -> PropertiesUtil -> ServiceLoaderUtil -> StackLocator
            final Class<?> sunReflectionClass =
                    Class.forName("sun.reflect.Reflection", true, ClassLoader.getSystemClassLoader());
            getCallerClassMethod = sunReflectionClass.getDeclaredMethod("getCallerClass", int.class);
            Object o = getCallerClassMethod.invoke(null, 0);
            getCallerClassMethod.invoke(null, 0);
            if (o == null || o != sunReflectionClass) {
                getCallerClassMethod = null;
                java7u25CompensationOffset = -1;
            } else {
                o = getCallerClassMethod.invoke(null, 1);
                if (o == sunReflectionClass) {
                    LOGGER.warn(
                            "Unexpected result from `sun.reflect.Reflection.getCallerClass(int)`, adjusting offset for future calls.");
                    java7u25CompensationOffset = 1;
                }
            }
        } catch (final Exception | LinkageError e) {
            // We can not use `Constants` here, since they depend on `PropertiesUtil`.
            LOGGER.warn(
                    System.getProperty("java.version", "").startsWith("1.8")
                            ? "`sun.reflect.Reflection.getCallerClass(int)` is not supported. This will impact location-based features."
                            : "Runtime environment or build system does not support multi-release JARs. This will impact location-based features.");
            getCallerClassMethod = null;
            java7u25CompensationOffset = -1;
        }

        GET_CALLER_CLASS_METHOD = getCallerClassMethod;
        JDK_7U25_OFFSET = java7u25CompensationOffset;
        INSTANCE = new StackLocator();
    }

    /**
     * Gets the singleton instance.
     *
     * @return the singleton instance.
     */
    public static StackLocator getInstance() {
        return INSTANCE;
    }

    private StackLocator() {}

    // TODO: return Object.class instead of null (though it will have a null ClassLoader)
    // (MS) I believe this would work without any modifications elsewhere, but I could be wrong

    @PerformanceSensitive
    public Class<?> getCallerClass(final Class<?> sentinelClass, final Predicate<Class<?>> callerPredicate) {
        if (sentinelClass == null) {
            throw new IllegalArgumentException("sentinelClass cannot be null");
        }
        if (callerPredicate == null) {
            throw new IllegalArgumentException("callerPredicate cannot be null");
        }
        boolean foundSentinel = false;
        Class<?> clazz;
        for (int i = 2; null != (clazz = getCallerClass(i)); i++) {
            if (sentinelClass.equals(clazz)) {
                foundSentinel = true;
            } else if (foundSentinel && callerPredicate.test(clazz)) {
                return clazz;
            }
        }
        return DEFAULT_CALLER_CLASS;
    }

    /**
     * Gets the Class of the method that called <em>this</em> method at the location up the call stack by the given stack
     * frame depth.
     * <p>
     * This method returns {@code null} if:
     * </p>
     * <ul>
     * <li>{@code sun.reflect.Reflection.getCallerClass(int)} is not present.</li>
     * <li>An exception is caught calling {@code sun.reflect.Reflection.getCallerClass(int)}.</li>
     * </ul>
     *
     * @param depth The stack frame count to walk.
     * @return A class or null.
     * @throws IndexOutOfBoundsException if depth is negative.
     */
    // migrated from ReflectiveCallerClassUtility
    @PerformanceSensitive
    public Class<?> getCallerClass(final int depth) {
        if (depth < 0) {
            throw new IndexOutOfBoundsException(Integer.toString(depth));
        }
        if (GET_CALLER_CLASS_METHOD == null) {
            return DEFAULT_CALLER_CLASS;
        }
        // note that we need to add 1 to the depth value to compensate for this method, but not for the Method.invoke
        // since Reflection.getCallerClass ignores the call to Method.invoke()
        try {
            return (Class<?>) GET_CALLER_CLASS_METHOD.invoke(null, depth + 1 + JDK_7U25_OFFSET);
        } catch (final Exception e) {
            // theoretically this could happen if the caller class were native code
            // TODO: return Object.class
            return DEFAULT_CALLER_CLASS;
        }
    }

    // migrated from Log4jLoggerFactory
    @PerformanceSensitive
    public Class<?> getCallerClass(final String fqcn, final String pkg) {
        boolean next = false;
        Class<?> clazz;
        for (int i = 2; null != (clazz = getCallerClass(i)); i++) {
            if (fqcn.equals(clazz.getName())) {
                next = true;
                continue;
            }
            if (next && clazz.getName().startsWith(pkg)) {
                return clazz;
            }
        }
        // TODO: return Object.class
        return DEFAULT_CALLER_CLASS;
    }

    // added for use in LoggerAdapter implementations mainly
    @PerformanceSensitive
    public Class<?> getCallerClass(final Class<?> anchor) {
        boolean next = false;
        Class<?> clazz;
        for (int i = 2; null != (clazz = getCallerClass(i)); i++) {
            if (anchor.equals(clazz)) {
                next = true;
                continue;
            }
            if (next) {
                return clazz;
            }
        }
        return Object.class;
    }

    // migrated from ThrowableProxy
    @PerformanceSensitive
    public Deque<Class<?>> getCurrentStackTrace() {
        // benchmarks show that using the SecurityManager is much faster than looping through getCallerClass(int)
        if (PrivateSecurityManagerStackTraceUtil.isEnabled()) {
            return PrivateSecurityManagerStackTraceUtil.getCurrentStackTrace();
        }
        // slower version using getCallerClass where we cannot use a SecurityManager
        final Deque<Class<?>> classes = new ArrayDeque<>();
        Class<?> clazz;
        for (int i = 1; null != (clazz = getCallerClass(i)); i++) {
            classes.addLast(clazz);
        }
        return classes;
    }

    public StackTraceElement calcLocation(final String fqcnOfLogger) {
        if (fqcnOfLogger == null) {
            return null;
        }
        // LOG4J2-1029 new Throwable().getStackTrace is faster than Thread.currentThread().getStackTrace().
        final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        boolean found = false;
        for (int i = 0; i < stackTrace.length; i++) {
            final String className = stackTrace[i].getClassName();
            if (fqcnOfLogger.equals(className)) {
                found = true;
                continue;
            }
            if (found && !fqcnOfLogger.equals(className)) {
                return stackTrace[i];
            }
        }
        return null;
    }

    public StackTraceElement getStackTraceElement(final int depth) {
        // (MS) I tested the difference between using Throwable.getStackTrace() and Thread.getStackTrace(), and
        // the version using Throwable was surprisingly faster! at least on Java 1.8. See ReflectionBenchmark.
        int i = 0;
        for (final StackTraceElement element : new Throwable().getStackTrace()) {
            if (isValid(element)) {
                if (i == depth) {
                    return element;
                }
                ++i;
            }
        }
        throw new IndexOutOfBoundsException(Integer.toString(depth));
    }

    private boolean isValid(final StackTraceElement element) {
        // ignore native methods (oftentimes are repeated frames)
        if (element.isNativeMethod()) {
            return false;
        }
        final String cn = element.getClassName();
        // ignore OpenJDK internal classes involved with reflective invocation
        if (cn.startsWith("sun.reflect.")) {
            return false;
        }
        final String mn = element.getMethodName();
        // ignore use of reflection including:
        // Method.invoke
        // InvocationHandler.invoke
        // Constructor.newInstance
        if (cn.startsWith("java.lang.reflect.") && (mn.equals("invoke") || mn.equals("newInstance"))) {
            return false;
        }
        // ignore use of Java 1.9+ reflection classes
        if (cn.startsWith("jdk.internal.reflect.")) {
            return false;
        }
        // ignore Class.newInstance
        if (cn.equals("java.lang.Class") && mn.equals("newInstance")) {
            return false;
        }
        // ignore use of Java 1.7+ MethodHandle.invokeFoo() methods
        if (cn.equals("java.lang.invoke.MethodHandle") && mn.startsWith("invoke")) {
            return false;
        }
        // any others?
        return true;
    }
}
