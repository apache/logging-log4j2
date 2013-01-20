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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.helpers.Loader;
import org.apache.logging.log4j.status.StatusLogger;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.security.CodeSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Wraps a Throwable to add packaging information about each stack trace element.
 */
public class ThrowableProxy extends Throwable {

    private static final long serialVersionUID = -2752771578252251910L;

    private static Method getCallerClass;

    private static PrivateSecurityManager securityManager;

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static Method getSuppressed;
    private static Method addSuppressed;

    private final ThrowableProxy proxyCause;
    private int commonElementCount;

    private final String name;

    private final StackTracePackageElement[] callerPackageData;


    static {
        setupCallerCheck();
        versionCheck();
    }

    /**
     * Construct the wrapper for the Throwable that includes packaging data.
     * @param throwable The Throwable to wrap.
     */
    public ThrowableProxy(final Throwable throwable) {
        super(throwable.getMessage(), null);
        this.name = throwable.getClass().getName();
        final Map<String, CacheEntry> map = new HashMap<String, CacheEntry>();
        final Stack<Class<?>> stack = getCurrentStack();
        super.setStackTrace(throwable.getStackTrace());
        callerPackageData = resolvePackageData(stack, map, null, throwable.getStackTrace());
        this.proxyCause = throwable.getCause() == null ? null :
            new ThrowableProxy(throwable, stack, map, throwable.getCause());
        setSuppressed(throwable);
    }

    /**
     * Constructs the wrapper for a Throwable that is referenced as the cause by another
     * Throwable.
     * @param parent The Throwable referencing this Throwable.
     * @param stack The Class stack.
     * @param map The cache containing the packaging data.
     * @param cause The Throwable to wrap.
     */
    private ThrowableProxy(final Throwable parent, final Stack<Class<?>> stack, final Map<String, CacheEntry> map,
                           final Throwable cause) {
        super(cause.getMessage(), null);
        this.name = cause.getClass().getName();
        super.setStackTrace(cause.getStackTrace());
        callerPackageData = resolvePackageData(stack, map, parent.getStackTrace(), cause.getStackTrace());
        this.proxyCause = cause.getCause() == null ? null :
            new ThrowableProxy(parent, stack, map, cause.getCause());
        setSuppressed(cause);
    }


    @Override
    public void setStackTrace(final StackTraceElement[] stackTraceElements) {
        throw new UnsupportedOperationException("Cannot set the stack trace on a ThrowableProxy");
    }

    @Override
    public Throwable getCause() {
        return proxyCause;
    }

    @Override
    public Throwable initCause(final Throwable throwable) {
        throw new IllegalStateException("Cannot set the cause on a ThrowableProxy");
    }

    @Override
    public String toString() {
        final String msg = getMessage();
        return msg != null ? name + ": " + msg : name;
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }

    /*
    @Override
    public StackTraceElement[] getStackTrace() {
        return callerData;
    } */

    /**
     * Format the Throwable that is the cause of this Throwable.
     * @return The formatted Throwable that caused this Throwable.
     */
    public String getRootCauseStackTrace() {
        return getRootCauseStackTrace(null);
    }

    /**
     * Format the Throwable that is the cause of this Throwable.
     * @param packages The List of packages to be suppressed from the trace.
     * @return The formatted Throwable that caused this Throwable.
     */
    public String getRootCauseStackTrace(final List<String> packages) {
        final StringBuilder sb = new StringBuilder();
        if (proxyCause != null) {
            formatWrapper(sb, proxyCause);
            sb.append("Wrapped by: ");
        }
        sb.append(toString());
        sb.append("\n");
        formatElements(sb, 0, getStackTrace(), callerPackageData, packages);
        return sb.toString();
    }

    /**
     * Formats the specified Throwable.
     * @param sb StringBuilder to contain the formatted Throwable.
     * @param cause The Throwable to format.
     */
    public void formatWrapper(final StringBuilder sb, final ThrowableProxy cause) {
        formatWrapper(sb, cause, null);
    }

    /**
     * Formats the specified Throwable.
     * @param sb StringBuilder to contain the formatted Throwable.
     * @param cause The Throwable to format.
     * @param packages The List of packages to be suppressed from the trace.
     */
    public void formatWrapper(final StringBuilder sb, final ThrowableProxy cause, final List<String> packages) {
        final Throwable caused = cause.getCause();
        if (caused != null) {
            formatWrapper(sb, cause.proxyCause);
            sb.append("Wrapped by: ");
        }
        sb.append(cause).append("\n");
        formatElements(sb, cause.commonElementCount, cause.getStackTrace(), cause.callerPackageData, packages);
    }

    /**
     * Format the stack trace including packaging information.
     * @return The formatted stack trace including packaging information.
     */
    public String getExtendedStackTrace() {
        return getExtendedStackTrace(null);
    }

    /**
     * Format the stack trace including packaging information.
     * @param packages List of packages to be suppressed from the trace.
     * @return The formatted stack trace including packaging information.
     */
    public String getExtendedStackTrace(final List<String> packages) {
        final StringBuilder sb = new StringBuilder(name);
        final String msg = getMessage();
        if (msg != null) {
            sb.append(": ").append(getMessage());
        }
        sb.append("\n");
        formatElements(sb, 0, getStackTrace(), callerPackageData, packages);
        if (proxyCause != null) {
            formatCause(sb, proxyCause, packages);
        }
        return sb.toString();
    }

    /**
     * Format the suppressed Throwables.
     * @return The formatted suppressed Throwables.
     */
    public String getSuppressedStackTrace() {
        final ThrowableProxy[] suppressed = getSuppressedProxies();
        if (suppressed == null || suppressed.length == 0) {
            return "";
        }
        final StringBuilder sb = new StringBuilder("Suppressed Stack Trace Elements:\n");
        for (final ThrowableProxy proxy : suppressed) {
            sb.append(proxy.getExtendedStackTrace());
        }
        return sb.toString();
    }

    private void formatCause(final StringBuilder sb, final ThrowableProxy cause, final List<String> packages) {
        sb.append("Caused by: ").append(cause).append("\n");
        formatElements(sb, cause.commonElementCount, cause.getStackTrace(), cause.callerPackageData, packages);
        if (cause.getCause() != null) {
            formatCause(sb, cause.proxyCause, packages);
        }
    }

    private void formatElements(final StringBuilder sb, final int commonCount, final StackTraceElement[] causedTrace,
                                final StackTracePackageElement[] packageData, final List<String> packages) {
        if (packages == null || packages.size() == 0) {
            for (int i = 0; i < packageData.length; ++i) {
                formatEntry(causedTrace[i], packageData[i], sb);
            }
        } else {
            int count = 0;
            for (int i = 0; i < packageData.length; ++i) {
                if (!isSuppressed(causedTrace[i], packages)) {
                    if (count > 0) {
                        if (count == 1) {
                            sb.append("\t....\n");
                        } else {
                            sb.append("\t... suppressed ").append(count).append(" lines\n");
                        }
                        count = 0;
                    }
                    formatEntry(causedTrace[i], packageData[i], sb);
                } else {
                    ++count;
                }
            }
            if (count > 0) {
                if (count == 1) {
                    sb.append("\t...\n");
                } else {
                    sb.append("\t... suppressed ").append(count).append(" lines\n");
                }
            }
        }
        if (commonCount != 0) {
            sb.append("\t... ").append(commonCount).append(" more").append("\n");
        }
    }

    private void formatEntry(final StackTraceElement element, final StackTracePackageElement packageData,
                             final StringBuilder sb) {
        sb.append("\tat ");
        sb.append(element);
        sb.append(" ");
        sb.append(packageData);
        sb.append("\n");
    }

    private boolean isSuppressed(final StackTraceElement element, final List<String> packages) {
        final String className = element.getClassName();
        for (final String pkg : packages) {
            if (className.startsWith(pkg)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Initialize the cache by resolving everything in the current stack trace via Reflection.getCallerClass
     * or via the SecurityManager if either are available. These are the only Classes that can be trusted
     * to be accurate.
     * @return A Deque containing the current stack of Class objects.
     */
    private Stack<Class<?>> getCurrentStack() {
        if (getCallerClass != null) {
            final Stack<Class<?>> classes = new Stack<Class<?>>();
            int index = 2;
            Class<?> clazz = getCallerClass(index);
            while (clazz != null) {
                classes.push(clazz);
                clazz = getCallerClass(++index);
            }
            return classes;
        } else if (securityManager != null) {
            final Class<?>[] array = securityManager.getClasses();
            final Stack<Class<?>> classes = new Stack<Class<?>>();
            for (final Class<?> clazz : array) {
                classes.push(clazz);
            }
            return classes;
        }
        return new Stack<Class<?>>();
    }

    /**
     * Resolve all the stack entries in this stack trace that are not common with the parent.
     * @param stack The callers Class stack.
     * @param map The cache of CacheEntry objects.
     * @param rootTrace The first stack trace resolve or null.
     * @param stackTrace The stack trace being resolved.
     * @return The StackTracePackageElement array.
     */
    private StackTracePackageElement[] resolvePackageData(final Stack<Class<?>> stack, final Map<String,
                                                          CacheEntry> map,
                                                          final StackTraceElement[] rootTrace,
                                                          final StackTraceElement[] stackTrace) {
        int stackLength;
        if (rootTrace != null) {
            int rootIndex = rootTrace.length - 1;
            int stackIndex = stackTrace.length - 1;
            while (rootIndex >= 0 && stackIndex >= 0 && rootTrace[rootIndex].equals(stackTrace[stackIndex])) {
                --rootIndex;
                --stackIndex;
            }
            commonElementCount = stackTrace.length - 1 - stackIndex;
            stackLength = stackIndex + 1;
        } else {
            commonElementCount = 0;
            stackLength = stackTrace.length;
        }
        final StackTracePackageElement[] packageArray = new StackTracePackageElement[stackLength];
        Class<?> clazz = stack.peek();
        ClassLoader lastLoader = null;
        for (int i = stackLength - 1; i >= 0; --i) {
            final String className = stackTrace[i].getClassName();
            // The stack returned from getCurrentStack will be missing entries for  java.lang.reflect.Method.invoke()
            // and its implementation. The Throwable might also contain stack entries that are no longer
            // present as those methods have returned.
            if (className.equals(clazz.getName())) {
                final CacheEntry entry = resolvePackageElement(clazz, true);
                packageArray[i] = entry.element;
                lastLoader = entry.loader;
                stack.pop();
                clazz = stack.peek();
            } else {
                if (map.containsKey(className)) {
                    final CacheEntry entry = map.get(className);
                    packageArray[i] = entry.element;
                    if (entry.loader != null) {
                        lastLoader = entry.loader;
                    }
                } else {
                    final CacheEntry entry = resolvePackageElement(loadClass(lastLoader, className), false);
                    packageArray[i] = entry.element;
                    map.put(className, entry);
                    if (entry.loader != null) {
                        lastLoader = entry.loader;
                    }
                }
            }
        }
        return packageArray;
    }


    /**
     * Construct the CacheEntry from the Class's information.
     * @param callerClass The Class.
     * @param exact True if the class was obtained via Reflection.getCallerClass.
     * @return The CacheEntry.
     */
    private CacheEntry resolvePackageElement(final Class<?> callerClass, final boolean exact) {
        String location = "?";
        String version = "?";
        ClassLoader lastLoader = null;
        if (callerClass != null) {
            try {
                final CodeSource source = callerClass.getProtectionDomain().getCodeSource();
                if (source != null) {
                    final URL locationURL = source.getLocation();
                    if (locationURL != null) {
                        final String str = locationURL.toString().replace('\\', '/');
                        int index = str.lastIndexOf("/");
                        if (index >= 0 && index == str.length() - 1) {
                            index = str.lastIndexOf("/", index - 1);
                            location = str.substring(index + 1);
                        } else {
                            location = str.substring(index + 1);
                        }
                    }
                }
            } catch (final Exception ex) {
                // Ignore the exception.
            }
            final Package pkg = callerClass.getPackage();
            if (pkg != null) {
                final String ver = pkg.getImplementationVersion();
                if (ver != null) {
                    version = ver;
                }
            }
            lastLoader = callerClass.getClassLoader();
        }
        return new CacheEntry(new StackTracePackageElement(location, version, exact), lastLoader);
    }

    /**
     * Invoke Reflection.getCallerClass via reflection. This is slightly slower than calling the method
     * directly but removes any dependency on Sun's JDK being present at compile time. The difference
     * can be measured by running the ReflectionComparison test.
     * @param index The index into the stack trace.
     * @return The Class at the specified stack entry.
     */
    private Class<?> getCallerClass(final int index) {
        if (getCallerClass != null) {
            try {
                final Object[] params = new Object[]{index};
                return (Class<?>) getCallerClass.invoke(null, params);
            } catch (final Exception ex) {
                // logger.debug("Unable to determine caller class via Sun Reflection", ex);
            }
        }
        return null;
    }

    /**
     * Loads classes not located via Reflection.getCallerClass.
     * @param lastLoader The ClassLoader that loaded the Class that called this Class.
     * @param className The name of the Class.
     * @return The Class object for the Class or null if it could not be located.
     */
    private Class<?> loadClass(final ClassLoader lastLoader, final String className) {
        Class<?> clazz;
        if (lastLoader != null) {
            try {
                clazz = lastLoader.loadClass(className);
                if (clazz != null) {
                    return clazz;
                }
            } catch (final Exception ex) {
                // Ignore exception.
            }
        }
        try {
            clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (final ClassNotFoundException e) {
            try {
                clazz = Class.forName(className);
            } catch (final ClassNotFoundException e1) {
                try {
                    clazz = getClass().getClassLoader().loadClass(className);
                } catch (final ClassNotFoundException e2) {
                    return null;
                }
            }
        }
        return clazz;
    }

    private static void versionCheck() {
        final Method[] methods = Throwable.class.getMethods();
        for (final Method method : methods) {
            if (method.getName().equals("getSuppressed")) {
                getSuppressed = method;
            } else if (method.getName().equals("addSuppressed")) {
                addSuppressed = method;
            }
        }
    }

    /**
     * Determine if Reflection.getCallerClass is available.
     */
    private static void setupCallerCheck() {
        try {
            final ClassLoader loader = Loader.getClassLoader();
            // Use wildcard to avoid compile-time reference.
            final Class<?> clazz = loader.loadClass("sun.reflect.Reflection");
            final Method[] methods = clazz.getMethods();
            for (final Method method : methods) {
                final int modifier = method.getModifiers();
                if (method.getName().equals("getCallerClass") && Modifier.isStatic(modifier)) {
                    getCallerClass = method;
                    return;
                }
            }
        } catch (final ClassNotFoundException cnfe) {
            LOGGER.debug("sun.reflect.Reflection is not installed");
        }

        try {
            final PrivateSecurityManager mgr = new PrivateSecurityManager();
            if (mgr.getClasses() != null) {
                securityManager = mgr;
            } else {
                // This shouldn't happen.
                LOGGER.error("Unable to obtain call stack from security manager");
            }
        } catch (final Exception ex) {
            LOGGER.debug("Unable to install security manager", ex);
        }
    }

    private ThrowableProxy[] getSuppressedProxies() {
        if (getSuppressed != null) {
            try {
                return (ThrowableProxy[]) getSuppressed.invoke(this, null);
            } catch (final Exception ex) {
                return null;
            }
        }
        return null;
    }

    private void setSuppressed(final Throwable throwable) {
        if (getSuppressed != null && addSuppressed != null) {
            try {
                final Throwable[] array = (Throwable[]) getSuppressed.invoke(throwable, null);
                for (final Throwable t : array) {
                    addSuppressed.invoke(this, new ThrowableProxy(t));
                }
            } catch (final Exception ex) {
                //
            }
        }
    }

    /**
     * Cached StackTracePackageElement and the ClassLoader.
     */
    private class CacheEntry {
        private final StackTracePackageElement element;
        private final ClassLoader loader;

        public CacheEntry(final StackTracePackageElement element, final ClassLoader loader) {
            this.element = element;
            this.loader = loader;
        }
    }

    /**
     * Security Manager for accessing the call stack.
     */
    private static class PrivateSecurityManager extends SecurityManager {
        public Class<?>[] getClasses() {
            return getClassContext();
        }
    }
}
