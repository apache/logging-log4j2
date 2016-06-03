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
package org.apache.logging.log4j.util;

import java.lang.reflect.Method;
import java.util.Stack;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * <em>Consider this class private.</em> Provides various methods to determine the caller class. <h3>Background</h3>
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
public final class ReflectionUtil {
    // Checkstyle Suppress: the lower-case 'u' ticks off CheckStyle...
    // CHECKSTYLE:OFF
    static final int JDK_7u25_OFFSET;
    // CHECKSTYLE:OFF
    
    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final boolean SUN_REFLECTION_SUPPORTED;
    private static final Method GET_CALLER_CLASS;
    private static final PrivateSecurityManager SECURITY_MANAGER;

    static {
        Method getCallerClass;
        int java7u25CompensationOffset = 0;
        try {
            final Class<?> sunReflectionClass = LoaderUtil.loadClass("sun.reflect.Reflection");
            getCallerClass = sunReflectionClass.getDeclaredMethod("getCallerClass", int.class);
            Object o = getCallerClass.invoke(null, 0);
            final Object test1 = getCallerClass.invoke(null, 0);
            if (o == null || o != sunReflectionClass) {
                LOGGER.warn("Unexpected return value from Reflection.getCallerClass(): {}", test1);
                getCallerClass = null;
                java7u25CompensationOffset = -1;
            } else {
                o = getCallerClass.invoke(null, 1);
                if (o == sunReflectionClass) {
                    LOGGER.warn("You are using Java 1.7.0_25 which has a broken implementation of "
                            + "Reflection.getCallerClass.");
                    LOGGER.warn("You should upgrade to at least Java 1.7.0_40 or later.");
                    LOGGER.debug("Using stack depth compensation offset of 1 due to Java 7u25.");
                    java7u25CompensationOffset = 1;
                }
            }
        } catch (final Exception | LinkageError e) {
            LOGGER.info("sun.reflect.Reflection.getCallerClass is not supported. "
                    + "ReflectionUtil.getCallerClass will be much slower due to this.", e);
            getCallerClass = null;
            java7u25CompensationOffset = -1;
        }

        SUN_REFLECTION_SUPPORTED = getCallerClass != null;
        GET_CALLER_CLASS = getCallerClass;
        JDK_7u25_OFFSET = java7u25CompensationOffset;

        PrivateSecurityManager psm;
        try {
            final SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkPermission(new RuntimePermission("createSecurityManager"));
            }
            psm = new PrivateSecurityManager();
        } catch (final SecurityException ignored) {
            LOGGER.debug("Not allowed to create SecurityManager. "
                    + "Falling back to slowest ReflectionUtil implementation.");
            psm = null;
        }
        SECURITY_MANAGER = psm;
    }

    private ReflectionUtil() {
    }

    public static boolean supportsFastReflection() {
        return SUN_REFLECTION_SUPPORTED;
    }

    // TODO: return Object.class instead of null (though it will have a null ClassLoader)
    // (MS) I believe this would work without any modifications elsewhere, but I could be wrong

    // migrated from ReflectiveCallerClassUtility
    @PerformanceSensitive
    public static Class<?> getCallerClass(final int depth) {
        if (depth < 0) {
            throw new IndexOutOfBoundsException(Integer.toString(depth));
        }
        // note that we need to add 1 to the depth value to compensate for this method, but not for the Method.invoke
        // since Reflection.getCallerClass ignores the call to Method.invoke()
        if (supportsFastReflection()) {
            try {
                return (Class<?>) GET_CALLER_CLASS.invoke(null, depth + 1 + JDK_7u25_OFFSET);
            } catch (final Exception e) {
                // theoretically this could happen if the caller class were native code
                LOGGER.error("Error in ReflectionUtil.getCallerClass({}).", depth, e);
                // TODO: return Object.class
                return null;
            }
        }
        // TODO: SecurityManager-based version?
        // slower fallback method using stack trace
        final StackTraceElement element = getEquivalentStackTraceElement(depth + 1);
        try {
            return LoaderUtil.loadClass(element.getClassName());
        } catch (final ClassNotFoundException e) {
            LOGGER.error("Could not find class in ReflectionUtil.getCallerClass({}).", depth, e);
        }
        // TODO: return Object.class
        return null;
    }

    static StackTraceElement getEquivalentStackTraceElement(final int depth) {
        // (MS) I tested the difference between using Throwable.getStackTrace() and Thread.getStackTrace(), and
        // the version using Throwable was surprisingly faster! at least on Java 1.8. See ReflectionBenchmark.
        final StackTraceElement[] elements = new Throwable().getStackTrace();
        int i = 0;
        for (final StackTraceElement element : elements) {
            if (isValid(element)) {
                if (i == depth) {
                    return element;
                }
                ++i;
            }
        }
        LOGGER.error("Could not find an appropriate StackTraceElement at index {}", depth);
        throw new IndexOutOfBoundsException(Integer.toString(depth));
    }

    private static boolean isValid(final StackTraceElement element) {
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

    // migrated from ClassLoaderContextSelector
    @PerformanceSensitive
    public static Class<?> getCallerClass(final String fqcn) {
        return getCallerClass(fqcn, Strings.EMPTY);
    }

    // migrated from Log4jLoggerFactory
    @PerformanceSensitive
    public static Class<?> getCallerClass(final String fqcn, final String pkg) {
        if (supportsFastReflection()) {
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
            return null;
        }
        if (SECURITY_MANAGER != null) {
            return SECURITY_MANAGER.getCallerClass(fqcn, pkg);
        }
        try {
            return LoaderUtil.loadClass(getCallerClassName(fqcn, pkg, new Throwable().getStackTrace()));
        } catch (final ClassNotFoundException ignored) {
            // no problem really
        }
        // TODO: return Object.class
        return null;
    }

    // added for use in LoggerAdapter implementations mainly
    @PerformanceSensitive
    public static Class<?> getCallerClass(final Class<?> anchor) {
        if (supportsFastReflection()) {
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
        if (SECURITY_MANAGER != null) {
            return SECURITY_MANAGER.getCallerClass(anchor);
        }
        try {
            return LoaderUtil.loadClass(getCallerClassName(anchor.getName(), Strings.EMPTY,
                    new Throwable().getStackTrace()));
        } catch (final ClassNotFoundException ignored) {
            // no problem really
        }
        return Object.class;
    }

    private static String getCallerClassName(final String fqcn, final String pkg, final StackTraceElement... elements) {
        boolean next = false;
        for (final StackTraceElement element : elements) {
            final String className = element.getClassName();
            if (className.equals(fqcn)) {
                next = true;
                continue;
            }
            if (next && className.startsWith(pkg)) {
                return className;
            }
        }
        return Object.class.getName();
    }

    // migrated from ThrowableProxy
    @PerformanceSensitive
    public static Stack<Class<?>> getCurrentStackTrace() {
        // benchmarks show that using the SecurityManager is much faster than looping through getCallerClass(int)
        if (SECURITY_MANAGER != null) {
            final Class<?>[] array = SECURITY_MANAGER.getClassContext();
            final Stack<Class<?>> classes = new Stack<>();
            classes.ensureCapacity(array.length);
            for (final Class<?> clazz : array) {
                classes.push(clazz);
            }
            return classes;
        }
        // slower version using getCallerClass where we cannot use a SecurityManager
        if (supportsFastReflection()) {
            final Stack<Class<?>> classes = new Stack<>();
            Class<?> clazz;
            for (int i = 1; null != (clazz = getCallerClass(i)); i++) {
                classes.push(clazz);
            }
            return classes;
        }
        return new Stack<>();
    }

    /**
     * 
     */
    static final class PrivateSecurityManager extends SecurityManager {

        @Override
        protected Class<?>[] getClassContext() {
            return super.getClassContext();
        }

        protected Class<?> getCallerClass(final String fqcn, final String pkg) {
            boolean next = false;
            for (final Class<?> clazz : getClassContext()) {
                if (fqcn.equals(clazz.getName())) {
                    next = true;
                    continue;
                }
                if (next && clazz.getName().startsWith(pkg)) {
                    return clazz;
                }
            }
            // TODO: return Object.class
            return null;
        }

        protected Class<?> getCallerClass(final Class<?> anchor) {
            boolean next = false;
            for (final Class<?> clazz : getClassContext()) {
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
    }
}
