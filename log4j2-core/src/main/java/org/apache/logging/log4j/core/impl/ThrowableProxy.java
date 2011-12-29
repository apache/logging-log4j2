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

import org.apache.logging.log4j.core.helpers.Loader;
import org.apache.logging.log4j.status.StatusLogger;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.security.CodeSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Wraps a Throwable to add packaging information about each stack trace element.
 */
public class ThrowableProxy extends Throwable {

    private static final long serialVersionUID = -2752771578252251910L;

    private static Method getCallerClass;

    private static PrivateSecurityManager securityManager;

    private static final org.apache.logging.log4j.Logger LOGGER = StatusLogger.getLogger();

    private static Method getSuppressed;

    private final Throwable throwable;
    private final ThrowableProxy cause;
    private final ThrowableProxy[] suppressed;
    private int commonElementCount;

    private final StackTracePackageElement[] callerPackageData;


    static {
        setupCallerCheck();
        versionCheck();
    }

    /**
     * Construct the wrapper for the Throwable that includes packaging data.
     * @param throwable The Throwable to wrap.
     */
    public ThrowableProxy(Throwable throwable) {
        this.throwable = throwable;
        Map<String, CacheEntry> map = new HashMap<String, CacheEntry>();
        Stack<Class> stack = getCurrentStack();
        callerPackageData = resolvePackageData(stack, map, null, throwable.getStackTrace());
        this.cause = (throwable.getCause() == null) ? null :
            new ThrowableProxy(throwable, stack, map, throwable.getCause());
        suppressed = getSuppressed(throwable);
    }

    /**
     * Constructs the wrapper for a Throwable that is referenced as the cause by another
     * Throwable.
     * @param parent The Throwable referencing this Throwable.
     * @param stack The Class stack.
     * @param map The cache containing the packaging data.
     * @param cause The Throwable to wrap.
     */
    private ThrowableProxy(Throwable parent, Stack<Class> stack, Map<String, CacheEntry> map, Throwable cause) {
        this.throwable = cause;
        callerPackageData = resolvePackageData(stack, map, parent.getStackTrace(), cause.getStackTrace());
        this.cause = (throwable.getCause() == null) ? null :
            new ThrowableProxy(parent, stack, map, throwable.getCause());
        suppressed = getSuppressed(throwable);
    }


    @Override
    public void setStackTrace(StackTraceElement[] stackTraceElements) {
        throw new UnsupportedOperationException("Cannot set the stack trace on a ThrowableProxy");
    }

    @Override
    public String getMessage() {
        return throwable.getMessage();
    }

    @Override
    public String getLocalizedMessage() {
        return throwable.getLocalizedMessage();
    }

    @Override
    public Throwable getCause() {
        return cause;
    }

    /**
     * Added in Java 7.
     * @param exception A Throwable that was suppressed.
     */
    public void addSuppressed(Throwable exception) {
        throw new UnsupportedOperationException("Cannot add suppressed exceptions to a ThrowableProxy");
    }

    /**
     * Added in Java 7.
     * @return Any suppressed exceptions.
     */
    public Throwable[] getSuppressed() {
        return suppressed;
    }

    @Override
    public Throwable initCause(Throwable throwable) {
        throw new IllegalStateException("Cannot set the cause on a ThrowableProxy");
    }

    @Override
    public String toString() {
        return throwable.toString();
    }

    @Override
    public void printStackTrace() {
        throwable.printStackTrace();
    }

    @Override
    public void printStackTrace(PrintStream printStream) {
        throwable.printStackTrace(printStream);
    }

    @Override
    public void printStackTrace(PrintWriter printWriter) {
        throwable.printStackTrace(printWriter);
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        return throwable.getStackTrace();
    }

    /**
     * Format the Throwable that is the cause of this Throwable.
     * @return The formatted Throwable that caused this Throwable.
     */
    public String getRootCauseStackTrace() {
        StringBuilder sb = new StringBuilder();
        if (cause != null) {
            formatWrapper(sb, cause);
            sb.append("Wrapped by: ");
        }
        sb.append(throwable.toString());
        sb.append("\n");
        formatElements(sb, 0, throwable.getStackTrace(), callerPackageData);
        return sb.toString();
    }

    /**
     * Formats the specified Throwable.
     * @param sb StringBuilder to contain the formatted Throwable.
     * @param cause The Throwable to format.
     */
    public void formatWrapper(StringBuilder sb, ThrowableProxy cause) {
        Throwable caused = cause.getCause();
        if (caused != null) {
            formatWrapper(sb, cause.cause);
            sb.append("Wrapped by: ");
        }
        sb.append(cause).append("\n");
        formatElements(sb, cause.commonElementCount, cause.getStackTrace(), cause.callerPackageData);
    }

    /**
     * Format the stack trace including packaging information.
     * @return The formatted stack trace including packaging information.
     */
    public String getExtendedStackTrace() {
        StringBuilder sb = new StringBuilder(throwable.toString());
        sb.append("\n");
        formatElements(sb, 0, throwable.getStackTrace(), callerPackageData);
        if (cause != null) {
            formatCause(sb, cause);
        }
        return sb.toString();
    }

    /**
     * Format the suppressed Throwables.
     * @return The formatted suppressed Throwables.
     */
    public String getSuppressedStackTrace() {
        if (suppressed == null || suppressed.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder("Suppressed Stack Trace Elements:\n");
        for (ThrowableProxy proxy : suppressed) {
            sb.append(proxy.getExtendedStackTrace());
        }
        return sb.toString();
    }

    private void formatCause(StringBuilder sb, ThrowableProxy cause) {
        sb.append("Caused by: ").append(cause).append("\n");
        formatElements(sb, cause.commonElementCount, cause.getStackTrace(), cause.callerPackageData);
        if (cause.getCause() != null) {
            formatCause(sb, cause.cause);
        }
    }

    private void formatElements(StringBuilder sb, int commonCount, StackTraceElement[] causedTrace,
                                StackTracePackageElement[] packageData) {
        for (int i = 0; i < packageData.length; ++i) {
            sb.append("\tat ");
            sb.append(causedTrace[i]);
            sb.append(" ");
            sb.append(packageData[i]);
            sb.append("\n");
        }
        if (commonCount != 0) {
            sb.append("\t... ").append(commonCount).append(" more").append("\n");
        }
    }

    /**
     * Initialize the cache by resolving everything in the current stack trace via Reflection.getCallerClass
     * or via the SecurityManager if either are available. These are the only Classes that can be trusted
     * to be accurate.
     * @return A Deque containing the current stack of Class objects.
     */
    private Stack<Class> getCurrentStack() {
        if (getCallerClass != null) {
            Stack<Class> classes = new Stack<Class>();
            int index = 2;
            Class clazz = getCallerClass(index);
            while (clazz != null) {
                classes.push(clazz);
                clazz = getCallerClass(++index);
            }
            return classes;
        } else if (securityManager != null) {
            Class[] array = securityManager.getClasses();
            Stack<Class> classes = new Stack<Class>();
            for (Class clazz : array) {
                classes.push(clazz);
            }
            return classes;
        }
        return new Stack<Class>();
    }

    /**
     * Resolve all the stack entries in this stack trace that are not common with the parent.
     * @param stack The callers Class stack.
     * @param map The cache of CacheEntry objects.
     * @param rootTrace The first stack trace resolve or null.
     * @param stackTrace The stack trace being resolved.
     * @return The StackTracePackageElement array.
     */
    private StackTracePackageElement[] resolvePackageData(Stack<Class> stack, Map<String, CacheEntry> map,
                                                          StackTraceElement[] rootTrace,
                                                          StackTraceElement[] stackTrace) {
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
        StackTracePackageElement[] packageArray = new StackTracePackageElement[stackLength];
        Class clazz = stack.peek();
        ClassLoader lastLoader = null;
        for (int i = stackLength - 1; i >= 0; --i) {
            String className = stackTrace[i].getClassName();
            // The stack returned from getCurrentStack will be missing entries for  java.lang.reflect.Method.invoke()
            // and its implementation. The Throwable might also contain stack entries that are no longer
            // present as those methods have returned.
            if (className.equals(clazz.getName())) {
                CacheEntry entry = resolvePackageElement(clazz, true);
                packageArray[i] = entry.element;
                lastLoader = entry.loader;
                stack.pop();
                clazz = stack.peek();
            } else {
                if (map.containsKey(className)) {
                    CacheEntry entry = map.get(className);
                    packageArray[i] = entry.element;
                    if (entry.loader != null) {
                        lastLoader = entry.loader;
                    }
                } else {
                    CacheEntry entry = resolvePackageElement(loadClass(lastLoader, className), false);
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
    private CacheEntry resolvePackageElement(Class callerClass, boolean exact) {
        String location = "?";
        String version = "?";
        ClassLoader lastLoader = null;
        if (callerClass != null) {
            try {
                CodeSource source = callerClass.getProtectionDomain().getCodeSource();
                if (source != null) {
                    URL locationURL = source.getLocation();
                    if (locationURL != null) {
                        String str = locationURL.toString().replace('\\', '/');
                        int index = str.lastIndexOf("/");
                        if (index >= 0 && index == str.length() - 1) {
                            index = str.lastIndexOf("/", index - 1);
                            location = str.substring(index + 1);
                        } else {
                            location = str.substring(index + 1);
                        }
                    }
                }
            } catch (Exception ex) {
                // Ignore the exception.
            }
            Package pkg = callerClass.getPackage();
            if (pkg != null) {
                String ver = pkg.getImplementationVersion();
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
    private Class getCallerClass(int index) {
        if (getCallerClass != null) {
            try {
                Object[] params = new Object[]{index};
                return (Class) getCallerClass.invoke(null, params);
            } catch (Exception ex) {
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
    private Class loadClass(ClassLoader lastLoader, String className) {
        Class clazz;
        if (lastLoader != null) {
            try {
                clazz = lastLoader.loadClass(className);
                if (clazz != null) {
                    return clazz;
                }
            } catch (Exception ex) {
                // Ignore exception.
            }
        }
        try {
            clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e1) {
                try {
                    clazz = getClass().getClassLoader().loadClass(className);
                } catch (ClassNotFoundException e2) {
                    return null;
                }
            }
        }
        return clazz;
    }

    private static void versionCheck() {
        Method[] methods = Throwable.class.getMethods();
        for (Method method : methods) {
            if (method.getName().equals("getSuppressed")) {
                getSuppressed = method;
            }
        }
    }

    /**
     * Determine if Reflection.getCallerClass is available.
     */
    private static void setupCallerCheck() {
        try {
            ClassLoader loader = Loader.getClassLoader();
            Class clazz = loader.loadClass("sun.reflect.Reflection");
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                int modifier = method.getModifiers();
                if (method.getName().equals("getCallerClass") && Modifier.isStatic(modifier)) {
                    getCallerClass = method;
                    return;
                }
            }
        } catch (ClassNotFoundException cnfe) {
            LOGGER.debug("sun.reflect.Reflection is not installed");
        }

        try {
            PrivateSecurityManager mgr = new PrivateSecurityManager();
            if (mgr.getClasses() != null) {
                securityManager = mgr;
            } else {
                // This shouldn't happen.
                LOGGER.error("Unable to obtain call stack from security manager");
            }
        } catch (Exception ex) {
            LOGGER.debug("Unable to install security manager", ex);
        }
    }

    private ThrowableProxy[] getSuppressed(Throwable throwable) {
        ThrowableProxy[] supp = null;
        if (getSuppressed != null) {
            try {
                Throwable[] array = (Throwable[]) getSuppressed.invoke(throwable, null);
                supp = new ThrowableProxy[array.length];
                int i = 0;
                for (Throwable t : array) {
                    supp[i] = new ThrowableProxy(t);
                    ++i;
                }
            } catch (Exception ex) {
                //
            }
        }
        return supp;
    }

    /**
     * Cached StackTracePackageElement and the ClassLoader.
     */
    private class CacheEntry {
        private StackTracePackageElement element;
        private ClassLoader loader;

        public CacheEntry(StackTracePackageElement element, ClassLoader loader) {
            this.element = element;
            this.loader = loader;
        }
    }

    /**
     * Security Manager for accessing the call stack.
     */
    private static class PrivateSecurityManager extends SecurityManager {
        public Class[] getClasses() {
            return getClassContext();
        }
    }
}
