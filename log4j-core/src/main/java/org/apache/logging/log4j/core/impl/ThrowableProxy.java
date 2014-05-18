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

import java.io.Serializable;
import java.net.URL;
import java.security.CodeSource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.core.util.Throwables;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;

/**
 * Wraps a Throwable to add packaging information about each stack trace element.
 *
 * <p>
 * A proxy is used to represent a throwable that may not exist in a different class loader or JVM. When an application deserializes a
 * ThrowableProxy, the throwable may not be set, but the throwable's information is preserved in other fields of the proxy like the message
 * and stack trace.
 * </p>
 *
 * TODO: Move this class to org.apache.logging.log4j.core because it is used from LogEvent.
 * TODO: Deserialize: Try to rebuild Throwable if the target exception is in this class loader?
 */
public class ThrowableProxy implements Serializable {

    /**
     * Cached StackTracePackageElement and ClassLoader.
     * <p>
     * Consider this class private.
     * </p>
     */
    static class CacheEntry {
        private final ExtendedStackTraceElement element;
        private final ClassLoader loader;

        public CacheEntry(final ExtendedStackTraceElement element, final ClassLoader loader) {
            this.element = element;
            this.loader = loader;
        }
    }

    /**
     * Security Manager for accessing the call stack.
     */
    private static class PrivateSecurityManager extends SecurityManager {
        public Class<?>[] getClasses() {
            return this.getClassContext();
        }
    }

    private static final ThrowableProxy[] EMPTY_THROWABLE_PROXY_ARRAY = new ThrowableProxy[0];

    private static final char EOL = '\n';

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static final PrivateSecurityManager SECURITY_MANAGER;

    private static final long serialVersionUID = -2752771578252251910L;

    static {
        if (ReflectiveCallerClassUtility.isSupported()) {
            SECURITY_MANAGER = null;
        } else {
            PrivateSecurityManager securityManager;
            try {
                securityManager = new PrivateSecurityManager();
                if (securityManager.getClasses() == null) {
                    // This shouldn't happen.
                    securityManager = null;
                    LOGGER.error("Unable to obtain call stack from security manager.");
                }
            } catch (final Exception e) {
                securityManager = null;
                LOGGER.debug("Unable to install security manager.", e);
            }
            SECURITY_MANAGER = securityManager;
        }
    }

    private final ThrowableProxy causeProxy;

    private int commonElementCount;

    private final ExtendedStackTraceElement[] extendedStackTrace;

    private final String localizedMessage;

    private final String message;

    private final String name;

    private final ThrowableProxy[] suppressedProxies;

    private final transient Throwable throwable;

    /**
     * For JSON and XML IO via Jackson.
     */
    @SuppressWarnings("unused")
    private ThrowableProxy() {
        this.throwable = null;
        this.name = null;
        this.extendedStackTrace = null;
        this.causeProxy = null;
        this.message = null;
        this.localizedMessage = null;
        this.suppressedProxies = EMPTY_THROWABLE_PROXY_ARRAY;
    }

    /**
     * Constructs the wrapper for the Throwable that includes packaging data.
     *
     * @param throwable The Throwable to wrap, must not be null.
     */
    public ThrowableProxy(final Throwable throwable) {
        this.throwable = throwable;
        this.name = throwable.getClass().getName();
        this.message = throwable.getMessage();
        this.localizedMessage = throwable.getLocalizedMessage();
        final Map<String, CacheEntry> map = new HashMap<String, CacheEntry>();
        final Stack<Class<?>> stack = this.getCurrentStack();
        this.extendedStackTrace = this.toExtendedStackTrace(stack, map, null, throwable.getStackTrace());
        final Throwable throwableCause = throwable.getCause();
        this.causeProxy = throwableCause == null ? null : new ThrowableProxy(throwable, stack, map, throwableCause);
        this.suppressedProxies = this.toSuppressedProxies(throwable);
    }

    /**
     * Constructs the wrapper for a Throwable that is referenced as the cause by another Throwable.
     *
     * @param parent The Throwable referencing this Throwable.
     * @param stack The Class stack.
     * @param map The cache containing the packaging data.
     * @param cause The Throwable to wrap.
     */
    private ThrowableProxy(final Throwable parent, final Stack<Class<?>> stack, final Map<String, CacheEntry> map, final Throwable cause) {
        this.throwable = cause;
        this.name = cause.getClass().getName();
        this.message = this.throwable.getMessage();
        this.localizedMessage = this.throwable.getLocalizedMessage();
        this.extendedStackTrace = this.toExtendedStackTrace(stack, map, parent.getStackTrace(), cause.getStackTrace());
        this.causeProxy = cause.getCause() == null ? null : new ThrowableProxy(parent, stack, map, cause.getCause());
        this.suppressedProxies = this.toSuppressedProxies(cause);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final ThrowableProxy other = (ThrowableProxy) obj;
        if (this.causeProxy == null) {
            if (other.causeProxy != null) {
                return false;
            }
        } else if (!this.causeProxy.equals(other.causeProxy)) {
            return false;
        }
        if (this.commonElementCount != other.commonElementCount) {
            return false;
        }
        if (this.name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!this.name.equals(other.name)) {
            return false;
        }
        if (!Arrays.equals(this.extendedStackTrace, other.extendedStackTrace)) {
            return false;
        }
        if (!Arrays.equals(this.suppressedProxies, other.suppressedProxies)) {
            return false;
        }
        return true;
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    private void formatCause(final StringBuilder sb, final ThrowableProxy cause, final List<String> packages) {
        sb.append("Caused by: ").append(cause).append(EOL);
        this.formatElements(sb, cause.commonElementCount, cause.getThrowable().getStackTrace(), cause.extendedStackTrace, packages);
        if (cause.getCauseProxy() != null) {
            this.formatCause(sb, cause.causeProxy, packages);
        }
    }

    private void formatElements(final StringBuilder sb, final int commonCount, final StackTraceElement[] causedTrace,
            final ExtendedStackTraceElement[] packageData, final List<String> packages) {
        if (packages == null || packages.size() == 0) {
            for (int i = 0; i < packageData.length; ++i) {
                this.formatEntry(causedTrace[i], packageData[i], sb);
            }
        } else {
            int count = 0;
            for (int i = 0; i < packageData.length; ++i) {
                if (!this.isSuppressed(causedTrace[i], packages)) {
                    if (count > 0) {
                        if (count == 1) {
                            sb.append("\t....\n");
                        } else {
                            sb.append("\t... suppressed ").append(count).append(" lines\n");
                        }
                        count = 0;
                    }
                    this.formatEntry(causedTrace[i], packageData[i], sb);
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
            sb.append("\t... ").append(commonCount).append(" more").append('\n');
        }
    }

    private void formatEntry(final StackTraceElement element, final ExtendedStackTraceElement packageData, final StringBuilder sb) {
        sb.append("\tat ");
        sb.append(element);
        sb.append(' ');
        sb.append(packageData);
        sb.append('\n');
    }

    /**
     * Formats the specified Throwable.
     *
     * @param sb StringBuilder to contain the formatted Throwable.
     * @param cause The Throwable to format.
     */
    public void formatWrapper(final StringBuilder sb, final ThrowableProxy cause) {
        this.formatWrapper(sb, cause, null);
    }

    /**
     * Formats the specified Throwable.
     *
     * @param sb StringBuilder to contain the formatted Throwable.
     * @param cause The Throwable to format.
     * @param packages The List of packages to be suppressed from the trace.
     */
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void formatWrapper(final StringBuilder sb, final ThrowableProxy cause, final List<String> packages) {
        final Throwable caused = cause.getCauseProxy() != null ? cause.getCauseProxy().getThrowable() : null;
        if (caused != null) {
            this.formatWrapper(sb, cause.causeProxy);
            sb.append("Wrapped by: ");
        }
        sb.append(cause).append('\n');
        this.formatElements(sb, cause.commonElementCount, cause.getThrowable().getStackTrace(), cause.extendedStackTrace, packages);
    }

    public ThrowableProxy getCauseProxy() {
        return this.causeProxy;
    }

    /**
     * Format the Throwable that is the cause of this Throwable.
     *
     * @return The formatted Throwable that caused this Throwable.
     */
    public String getCauseStackTraceAsString() {
        return this.getCauseStackTraceAsString(null);
    }

    /**
     * Format the Throwable that is the cause of this Throwable.
     *
     * @param packages The List of packages to be suppressed from the trace.
     * @return The formatted Throwable that caused this Throwable.
     */
    public String getCauseStackTraceAsString(final List<String> packages) {
        final StringBuilder sb = new StringBuilder();
        if (this.causeProxy != null) {
            this.formatWrapper(sb, this.causeProxy);
            sb.append("Wrapped by: ");
        }
        sb.append(this.toString());
        sb.append('\n');
        this.formatElements(sb, 0, this.throwable.getStackTrace(), this.extendedStackTrace, packages);
        return sb.toString();
    }

    /**
     * Return the number of elements that are being omitted because they are common with the parent Throwable's stack trace.
     *
     * @return The number of elements omitted from the stack trace.
     */
    public int getCommonElementCount() {
        return this.commonElementCount;
    }

    /**
     * Initialize the cache by resolving everything in the current stack trace via Reflection.getCallerClass or via the SecurityManager if
     * either are available. These are the only Classes that can be trusted to be accurate.
     *
     * @return A Stack containing the current stack of Class objects.
     */
    private Stack<Class<?>> getCurrentStack() {
        if (ReflectiveCallerClassUtility.isSupported()) {
            final Stack<Class<?>> classes = new Stack<Class<?>>();
            int index = 1;
            Class<?> clazz = ReflectiveCallerClassUtility.getCaller(index);
            while (clazz != null) {
                classes.push(clazz);
                clazz = ReflectiveCallerClassUtility.getCaller(++index);
            }
            return classes;
        } else if (SECURITY_MANAGER != null) {
            final Class<?>[] array = SECURITY_MANAGER.getClasses();
            final Stack<Class<?>> classes = new Stack<Class<?>>();
            for (final Class<?> clazz : array) {
                classes.push(clazz);
            }
            return classes;
        }
        return new Stack<Class<?>>();
    }

    /**
     * Gets the stack trace including packaging information.
     *
     * @return The stack trace including packaging information.
     */
    public ExtendedStackTraceElement[] getExtendedStackTrace() {
        return this.extendedStackTrace;
    }

    /**
     * Format the stack trace including packaging information.
     *
     * @return The formatted stack trace including packaging information.
     */
    public String getExtendedStackTraceAsString() {
        return this.getExtendedStackTraceAsString(null);
    }

    /**
     * Format the stack trace including packaging information.
     *
     * @param packages List of packages to be suppressed from the trace.
     * @return The formatted stack trace including packaging information.
     */
    public String getExtendedStackTraceAsString(final List<String> packages) {
        final StringBuilder sb = new StringBuilder(this.name);
        final String msg = this.message;
        if (msg != null) {
            sb.append(": ").append(msg);
        }
        sb.append('\n');
        this.formatElements(sb, 0, this.throwable.getStackTrace(), this.extendedStackTrace, packages);
        if (this.causeProxy != null) {
            this.formatCause(sb, this.causeProxy, packages);
        }
        return sb.toString();
    }

    public String getLocalizedMessage() {
        return this.localizedMessage;
    }

    public String getMessage() {
        return this.message;
    }

    /**
     * Return the FQCN of the Throwable.
     *
     * @return The FQCN of the Throwable.
     */
    public String getName() {
        return this.name;
    }

    public StackTraceElement[] getStackTrace() {
        return this.throwable == null ? null : this.throwable.getStackTrace();
    }

    /**
     * Gets proxies for suppressed exceptions.
     *
     * @return proxies for suppressed exceptions.
     */
    public ThrowableProxy[] getSuppressedProxies() {
        return this.suppressedProxies;
    }

    /**
     * Format the suppressed Throwables.
     *
     * @return The formatted suppressed Throwables.
     */
    public String getSuppressedStackTrace() {
        final ThrowableProxy[] suppressed = this.getSuppressedProxies();
        if (suppressed == null || suppressed.length == 0) {
            return Strings.EMPTY;
        }
        final StringBuilder sb = new StringBuilder("Suppressed Stack Trace Elements:\n");
        for (final ThrowableProxy proxy : suppressed) {
            sb.append(proxy.getExtendedStackTraceAsString());
        }
        return sb.toString();
    }

    /**
     * The throwable or null if this object is deserialized from XML or JSON.
     *
     * @return The throwable or null if this object is deserialized from XML or JSON.
     */
    public Throwable getThrowable() {
        return this.throwable;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.causeProxy == null ? 0 : this.causeProxy.hashCode());
        result = prime * result + this.commonElementCount;
        result = prime * result + (this.extendedStackTrace == null ? 0 : Arrays.hashCode(this.extendedStackTrace));
        result = prime * result + (this.suppressedProxies == null ? 0 : Arrays.hashCode(this.suppressedProxies));
        result = prime * result + (this.name == null ? 0 : this.name.hashCode());
        return result;
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
     * Loads classes not located via Reflection.getCallerClass.
     *
     * @param lastLoader The ClassLoader that loaded the Class that called this Class.
     * @param className The name of the Class.
     * @return The Class object for the Class or null if it could not be located.
     */
    private Class<?> loadClass(final ClassLoader lastLoader, final String className) {
        // XXX: this is overly complicated
        Class<?> clazz;
        if (lastLoader != null) {
            try {
                clazz = Loader.initializeClass(className, lastLoader);
                if (clazz != null) {
                    return clazz;
                }
            } catch (final Exception ignore) {
                // Ignore exception.
            }
        }
        try {
            clazz = Loader.loadClass(className);
        } catch (final ClassNotFoundException ignored) {
            try {
                clazz = Loader.initializeClass(className, this.getClass().getClassLoader());
            } catch (final ClassNotFoundException ignore) {
                return null;
            }
        }
        return clazz;
    }

    /**
     * Construct the CacheEntry from the Class's information.
     *
     * @param stackTraceElement The stack trace element
     * @param callerClass The Class.
     * @param exact True if the class was obtained via Reflection.getCallerClass.
     *
     * @return The CacheEntry.
     */
    private CacheEntry resolvePackageElement(final StackTraceElement stackTraceElement, final Class<?> callerClass, final boolean exact) {
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
        return new CacheEntry(new ExtendedStackTraceElement(stackTraceElement, exact, location, version), lastLoader);
    }

    /**
     * Resolve all the stack entries in this stack trace that are not common with the parent.
     *
     * @param stack The callers Class stack.
     * @param map The cache of CacheEntry objects.
     * @param rootTrace The first stack trace resolve or null.
     * @param stackTrace The stack trace being resolved.
     * @return The StackTracePackageElement array.
     */
    ExtendedStackTraceElement[] toExtendedStackTrace(final Stack<Class<?>> stack, final Map<String, CacheEntry> map,
            final StackTraceElement[] rootTrace, final StackTraceElement[] stackTrace) {
        int stackLength;
        if (rootTrace != null) {
            int rootIndex = rootTrace.length - 1;
            int stackIndex = stackTrace.length - 1;
            while (rootIndex >= 0 && stackIndex >= 0 && rootTrace[rootIndex].equals(stackTrace[stackIndex])) {
                --rootIndex;
                --stackIndex;
            }
            this.commonElementCount = stackTrace.length - 1 - stackIndex;
            stackLength = stackIndex + 1;
        } else {
            this.commonElementCount = 0;
            stackLength = stackTrace.length;
        }
        final ExtendedStackTraceElement[] packageArray = new ExtendedStackTraceElement[stackLength];
        Class<?> clazz = stack.isEmpty() ? null : stack.peek();
        ClassLoader lastLoader = null;
        for (int i = stackLength - 1; i >= 0; --i) {
            final StackTraceElement stackTraceElement = stackTrace[i];
            final String className = stackTraceElement.getClassName();
            // The stack returned from getCurrentStack will be missing entries for java.lang.reflect.Method.invoke()
            // and its implementation. The Throwable might also contain stack entries that are no longer
            // present as those methods have returned.
            if (clazz != null && className.equals(clazz.getName())) {
                final CacheEntry entry = this.resolvePackageElement(stackTraceElement, clazz, true);
                packageArray[i] = entry.element;
                lastLoader = entry.loader;
                stack.pop();
                clazz = stack.isEmpty() ? null : stack.peek();
            } else {
                if (map.containsKey(className)) {
                    final CacheEntry entry = map.get(className);
                    packageArray[i] = entry.element;
                    if (entry.loader != null) {
                        lastLoader = entry.loader;
                    }
                } else {
                    final CacheEntry entry = this.resolvePackageElement(stackTraceElement, this.loadClass(lastLoader, className), false);
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

    @Override
    public String toString() {
        final String msg = this.message;
        return msg != null ? this.name + ": " + msg : this.name;
    }

    private ThrowableProxy[] toSuppressedProxies(final Throwable thrown) {
        try {
            final Throwable[] suppressed = Throwables.getSuppressed(thrown);
            if (suppressed == null) {
                return EMPTY_THROWABLE_PROXY_ARRAY;
            }
            final ThrowableProxy[] proxies = new ThrowableProxy[suppressed.length];
            for (int i = 0; i < suppressed.length; i++) {
                proxies[i] = new ThrowableProxy(suppressed[i]);
            }
            return proxies;
        } catch (final Exception e) {
            StatusLogger.getLogger().error(e);
        }
        return null;
    }
}
