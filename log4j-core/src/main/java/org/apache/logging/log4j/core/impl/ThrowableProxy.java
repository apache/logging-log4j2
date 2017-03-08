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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.logging.log4j.core.pattern.PlainTextRenderer;
import org.apache.logging.log4j.core.pattern.TextRenderer;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.ReflectionUtil;
import org.apache.logging.log4j.util.Strings;

/**
 * Wraps a Throwable to add packaging information about each stack trace element.
 *
 * <p>
 * A proxy is used to represent a throwable that may not exist in a different class loader or JVM. When an application
 * deserializes a ThrowableProxy, the throwable may not be set, but the throwable's information is preserved in other
 * fields of the proxy like the message and stack trace.
 * </p>
 *
 * <p>
 * TODO: Move this class to org.apache.logging.log4j.core because it is used from LogEvent.
 * </p>
 * <p>
 * TODO: Deserialize: Try to rebuild Throwable if the target exception is in this class loader?
 * </p>
 */
public class ThrowableProxy implements Serializable {

    private static final String TAB = "\t";
    private static final String CAUSED_BY_LABEL = "Caused by: ";
    private static final String SUPPRESSED_LABEL = "Suppressed: ";
    private static final String WRAPPED_BY_LABEL = "Wrapped by: ";

    /**
     * Cached StackTracePackageElement and ClassLoader.
     * <p>
     * Consider this class private.
     * </p>
     */
    static class CacheEntry {
        private final ExtendedClassInfo element;
        private final ClassLoader loader;

        public CacheEntry(final ExtendedClassInfo element, final ClassLoader loader) {
            this.element = element;
            this.loader = loader;
        }
    }

    private static final ThrowableProxy[] EMPTY_THROWABLE_PROXY_ARRAY = new ThrowableProxy[0];

    private static final char EOL = '\n';

    private static final String EOL_STR = String.valueOf(EOL);

    private static final long serialVersionUID = -2752771578252251910L;

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
        this(throwable, null);
    }

    /**
     * Constructs the wrapper for the Throwable that includes packaging data.
     *
     * @param throwable The Throwable to wrap, must not be null.
     * @param visited   The set of visited suppressed exceptions.
     */
    private ThrowableProxy(final Throwable throwable, final Set<Throwable> visited) {
        this.throwable = throwable;
        this.name = throwable.getClass().getName();
        this.message = throwable.getMessage();
        this.localizedMessage = throwable.getLocalizedMessage();
        final Map<String, CacheEntry> map = new HashMap<>();
        final Stack<Class<?>> stack = ReflectionUtil.getCurrentStackTrace();
        this.extendedStackTrace = this.toExtendedStackTrace(stack, map, null, throwable.getStackTrace());
        final Throwable throwableCause = throwable.getCause();
        final Set<Throwable> causeVisited = new HashSet<>(1);
        this.causeProxy = throwableCause == null ? null : new ThrowableProxy(throwable, stack, map, throwableCause,
            visited, causeVisited);
        this.suppressedProxies = this.toSuppressedProxies(throwable, visited);
    }

    /**
     * Constructs the wrapper for a Throwable that is referenced as the cause by another Throwable.
     *
     * @param parent            The Throwable referencing this Throwable.
     * @param stack             The Class stack.
     * @param map               The cache containing the packaging data.
     * @param cause             The Throwable to wrap.
     * @param suppressedVisited TODO
     * @param causeVisited      TODO
     */
    private ThrowableProxy(final Throwable parent, final Stack<Class<?>> stack, final Map<String, CacheEntry> map,
                           final Throwable cause, final Set<Throwable> suppressedVisited,
                           final Set<Throwable> causeVisited) {
        causeVisited.add(cause);
        this.throwable = cause;
        this.name = cause.getClass().getName();
        this.message = this.throwable.getMessage();
        this.localizedMessage = this.throwable.getLocalizedMessage();
        this.extendedStackTrace = this.toExtendedStackTrace(stack, map, parent.getStackTrace(), cause.getStackTrace());
        final Throwable causeCause = cause.getCause();
        this.causeProxy = causeCause == null || causeVisited.contains(causeCause) ? null : new ThrowableProxy(parent,
            stack, map, causeCause, suppressedVisited, causeVisited);
        this.suppressedProxies = this.toSuppressedProxies(cause, suppressedVisited);
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

    private void formatCause(final StringBuilder sb, final String prefix, final ThrowableProxy cause,
                             final List<String> ignorePackages, final TextRenderer textRenderer, final String suffix) {
        formatThrowableProxy(sb, prefix, CAUSED_BY_LABEL, cause, ignorePackages, textRenderer, suffix);
    }

    private void formatThrowableProxy(final StringBuilder sb, final String prefix, final String causeLabel,
                                      final ThrowableProxy throwableProxy, final List<String> ignorePackages,
                                      final TextRenderer textRenderer, final String suffix) {
        if (throwableProxy == null) {
            return;
        }
        textRenderer.render(prefix, sb, "Prefix");
        textRenderer.render(causeLabel, sb, "CauseLabel");
        throwableProxy.renderOn(sb, textRenderer);
        renderSuffix(suffix, sb, textRenderer);
        textRenderer.render(EOL_STR, sb, "Text");
        this.formatElements(sb, prefix, throwableProxy.commonElementCount,
            throwableProxy.getStackTrace(), throwableProxy.extendedStackTrace, ignorePackages, textRenderer, suffix);
        this.formatSuppressed(sb, prefix + TAB, throwableProxy.suppressedProxies, ignorePackages, textRenderer, suffix);
        this.formatCause(sb, prefix, throwableProxy.causeProxy, ignorePackages, textRenderer, suffix);
    }

    void renderOn(final StringBuilder output, final TextRenderer textRenderer) {
        final String msg = this.message;
        textRenderer.render(this.name, output, "Name");
        if (msg != null) {
            textRenderer.render(": ", output, "NameMessageSeparator");
            textRenderer.render(msg, output, "Message");
        }
    }

    private void formatSuppressed(final StringBuilder sb, final String prefix, final ThrowableProxy[] suppressedProxies,
                                  final List<String> ignorePackages, final TextRenderer textRenderer, final String suffix) {
        if (suppressedProxies == null) {
            return;
        }
        for (final ThrowableProxy suppressedProxy : suppressedProxies) {
            formatThrowableProxy(sb, prefix, SUPPRESSED_LABEL, suppressedProxy, ignorePackages, textRenderer, suffix);
        }
    }

    private void formatElements(final StringBuilder sb, final String prefix, final int commonCount,
                                final StackTraceElement[] causedTrace, final ExtendedStackTraceElement[] extStackTrace,
                                final List<String> ignorePackages, final TextRenderer textRenderer, final String suffix) {
        if (ignorePackages == null || ignorePackages.isEmpty()) {
            for (final ExtendedStackTraceElement element : extStackTrace) {
                this.formatEntry(element, sb, prefix, textRenderer, suffix);
            }
        } else {
            int count = 0;
            for (int i = 0; i < extStackTrace.length; ++i) {
                if (!this.ignoreElement(causedTrace[i], ignorePackages)) {
                    if (count > 0) {
                        appendSuppressedCount(sb, prefix, count, textRenderer, suffix);
                        count = 0;
                    }
                    this.formatEntry(extStackTrace[i], sb, prefix, textRenderer, suffix);
                } else {
                    ++count;
                }
            }
            if (count > 0) {
                appendSuppressedCount(sb, prefix, count, textRenderer, suffix);
            }
        }
        if (commonCount != 0) {
            textRenderer.render(prefix, sb, "Prefix");
            textRenderer.render("\t... ", sb, "More");
            textRenderer.render(Integer.toString(commonCount), sb, "More");
            textRenderer.render(" more", sb, "More");
            renderSuffix(suffix, sb, textRenderer);
            textRenderer.render(EOL_STR, sb, "Text");
        }
    }

    private void renderSuffix(final String suffix, final StringBuilder sb, final TextRenderer textRenderer) {
        if (!suffix.isEmpty()) {
            textRenderer.render(" ", sb, "Suffix");
            textRenderer.render(suffix, sb, "Suffix");
        }
    }

    private void appendSuppressedCount(final StringBuilder sb, final String prefix, final int count,
                                       final TextRenderer textRenderer, final String suffix) {
        textRenderer.render(prefix, sb, "Prefix");
        if (count == 1) {
            textRenderer.render("\t... ", sb, "Suppressed");
        } else {
            textRenderer.render("\t... suppressed ", sb, "Suppressed");
            textRenderer.render(Integer.toString(count), sb, "Suppressed");
            textRenderer.render(" lines", sb, "Suppressed");
        }
        renderSuffix(suffix, sb, textRenderer);
        textRenderer.render(EOL_STR, sb, "Text");
    }

    private void formatEntry(final ExtendedStackTraceElement extStackTraceElement, final StringBuilder sb,
                             final String prefix, final TextRenderer textRenderer, final String suffix) {
        textRenderer.render(prefix, sb, "Prefix");
        textRenderer.render("\tat ", sb, "At");
        extStackTraceElement.renderOn(sb, textRenderer);
        renderSuffix(suffix, sb, textRenderer);
        textRenderer.render(EOL_STR, sb, "Text");
    }

    /**
     * Formats the specified Throwable.
     *  @param sb    StringBuilder to contain the formatted Throwable.
     * @param cause The Throwable to format.
     * @param suffix
     */
    public void formatWrapper(final StringBuilder sb, final ThrowableProxy cause, final String suffix) {
        this.formatWrapper(sb, cause, null, PlainTextRenderer.getInstance(), suffix);
    }

    /**
     * Formats the specified Throwable.
     *  @param sb             StringBuilder to contain the formatted Throwable.
     * @param cause          The Throwable to format.
     * @param ignorePackages The List of packages to be suppressed from the trace.
     * @param suffix
     */
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void formatWrapper(final StringBuilder sb, final ThrowableProxy cause, final List<String> ignorePackages, final String suffix) {
        this.formatWrapper(sb, cause, ignorePackages, PlainTextRenderer.getInstance(), suffix);
    }

    /**
     * Formats the specified Throwable.
     *  @param sb             StringBuilder to contain the formatted Throwable.
     * @param cause          The Throwable to format.
     * @param ignorePackages The List of packages to be suppressed from the trace.
     * @param textRenderer   The text render
     * @param suffix
     */
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void formatWrapper(final StringBuilder sb, final ThrowableProxy cause, final List<String> ignorePackages,
                              final TextRenderer textRenderer, final String suffix) {
        final Throwable caused = cause.getCauseProxy() != null ? cause.getCauseProxy().getThrowable() : null;
        if (caused != null) {
            this.formatWrapper(sb, cause.causeProxy, ignorePackages, textRenderer, suffix);
            sb.append(WRAPPED_BY_LABEL);
            renderSuffix(suffix, sb, textRenderer);
        }
        cause.renderOn(sb, textRenderer);
        renderSuffix(suffix, sb, textRenderer);
        textRenderer.render(EOL_STR, sb, "Text");
        this.formatElements(sb, Strings.EMPTY, cause.commonElementCount,
            cause.getThrowable().getStackTrace(), cause.extendedStackTrace, ignorePackages, textRenderer, suffix);
    }

    public ThrowableProxy getCauseProxy() {
        return this.causeProxy;
    }

    /**
     * Formats the Throwable that is the cause of this Throwable.
     *
     * @return The formatted Throwable that caused this Throwable.
     * @param suffix
     */
    public String getCauseStackTraceAsString(final String suffix) {
        return this.getCauseStackTraceAsString(null, PlainTextRenderer.getInstance(), suffix);
    }

    /**
     * Formats the Throwable that is the cause of this Throwable.
     *
     * @param packages The List of packages to be suppressed from the trace.
     * @param suffix
     * @return The formatted Throwable that caused this Throwable.
     */
    public String getCauseStackTraceAsString(final List<String> packages, final String suffix) {
        return getCauseStackTraceAsString(packages, PlainTextRenderer.getInstance(), suffix);
    }

    /**
     * Formats the Throwable that is the cause of this Throwable.
     *
     * @param ignorePackages The List of packages to be suppressed from the trace.
     * @param textRenderer   the text renderer
     * @param suffix
     * @return The formatted Throwable that caused this Throwable.
     */
    public String getCauseStackTraceAsString(final List<String> ignorePackages, final TextRenderer textRenderer, final String suffix) {
        final StringBuilder sb = new StringBuilder();
        if (this.causeProxy != null) {
            this.formatWrapper(sb, this.causeProxy, ignorePackages, textRenderer, suffix);
            sb.append(WRAPPED_BY_LABEL);
            renderSuffix(suffix, sb, textRenderer);
        }
        this.renderOn(sb, textRenderer);
        renderSuffix(suffix, sb, textRenderer);
        textRenderer.render(EOL_STR, sb, "Text");
        this.formatElements(sb, Strings.EMPTY, 0, this.throwable.getStackTrace(), this.extendedStackTrace,
            ignorePackages, textRenderer, suffix);
        return sb.toString();
    }

    /**
     * Returns the number of elements that are being omitted because they are common with the parent Throwable's stack
     * trace.
     *
     * @return The number of elements omitted from the stack trace.
     */
    public int getCommonElementCount() {
        return this.commonElementCount;
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
     * @param suffix
     */
    public String getExtendedStackTraceAsString(final String suffix) {
        return this.getExtendedStackTraceAsString(null, PlainTextRenderer.getInstance(), suffix);
    }

    /**
     * Format the stack trace including packaging information.
     *
     * @param ignorePackages List of packages to be ignored in the trace.
     * @param suffix
     * @return The formatted stack trace including packaging information.
     */
    public String getExtendedStackTraceAsString(final List<String> ignorePackages, final String suffix) {
        return getExtendedStackTraceAsString(ignorePackages, PlainTextRenderer.getInstance(), suffix);
    }

    /**
     * Format the stack trace including packaging information.
     *
     * @param ignorePackages List of packages to be ignored in the trace.
     * @param textRenderer   The message renderer
     * @param suffix
     * @return The formatted stack trace including packaging information.
     */
    public String getExtendedStackTraceAsString(final List<String> ignorePackages, final TextRenderer textRenderer, final String suffix) {
        final StringBuilder sb = new StringBuilder(1024);
        textRenderer.render(name, sb, "Name");
        textRenderer.render(": ", sb, "NameMessageSeparator");
        textRenderer.render(this.message, sb, "Message");
        renderSuffix(suffix, sb, textRenderer);
        textRenderer.render(EOL_STR, sb, "Text");
        final StackTraceElement[] causedTrace = this.throwable != null ? this.throwable.getStackTrace() : null;
        this.formatElements(sb, Strings.EMPTY, 0, causedTrace, this.extendedStackTrace, ignorePackages, textRenderer, suffix);
        this.formatSuppressed(sb, TAB, this.suppressedProxies, ignorePackages, textRenderer, suffix);
        this.formatCause(sb, Strings.EMPTY, this.causeProxy, ignorePackages, textRenderer, suffix);
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
     * @param suffix
     */
    public String getSuppressedStackTrace(final String suffix) {
        final ThrowableProxy[] suppressed = this.getSuppressedProxies();
        if (suppressed == null || suppressed.length == 0) {
            return Strings.EMPTY;
        }
        final StringBuilder sb = new StringBuilder("Suppressed Stack Trace Elements:").append(EOL);
        for (final ThrowableProxy proxy : suppressed) {
            sb.append(proxy.getExtendedStackTraceAsString(suffix));
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

    private boolean ignoreElement(final StackTraceElement element, final List<String> ignorePackages) {
        if (ignorePackages != null) {
            final String className = element.getClassName();
            for (final String pkg : ignorePackages) {
                if (className.startsWith(pkg)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Loads classes not located via Reflection.getCallerClass.
     *
     * @param lastLoader The ClassLoader that loaded the Class that called this Class.
     * @param className  The name of the Class.
     * @return The Class object for the Class or null if it could not be located.
     */
    private Class<?> loadClass(final ClassLoader lastLoader, final String className) {
        // XXX: this is overly complicated
        Class<?> clazz;
        if (lastLoader != null) {
            try {
                clazz = lastLoader.loadClass(className);
                if (clazz != null) {
                    return clazz;
                }
            } catch (final Throwable ignore) {
                // Ignore exception.
            }
        }
        try {
            clazz = LoaderUtil.loadClass(className);
        } catch (final ClassNotFoundException | NoClassDefFoundError e) {
            return loadClass(className);
        } catch (final SecurityException e) {
            return null;
        }
        return clazz;
    }

    private Class<?> loadClass(final String className) {
        try {
            return Loader.loadClass(className, this.getClass().getClassLoader());
        } catch (final ClassNotFoundException | NoClassDefFoundError | SecurityException e) {
            return null;
        }
    }

    /**
     * Construct the CacheEntry from the Class's information.
     *
     * @param stackTraceElement The stack trace element
     * @param callerClass       The Class.
     * @param exact             True if the class was obtained via Reflection.getCallerClass.
     * @return The CacheEntry.
     */
    private CacheEntry toCacheEntry(final StackTraceElement stackTraceElement, final Class<?> callerClass,
                                    final boolean exact) {
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
            try {
                lastLoader = callerClass.getClassLoader();
            } catch (final SecurityException e) {
                lastLoader = null;
            }
        }
        return new CacheEntry(new ExtendedClassInfo(exact, location, version), lastLoader);
    }

    /**
     * Resolve all the stack entries in this stack trace that are not common with the parent.
     *
     * @param stack      The callers Class stack.
     * @param map        The cache of CacheEntry objects.
     * @param rootTrace  The first stack trace resolve or null.
     * @param stackTrace The stack trace being resolved.
     * @return The StackTracePackageElement array.
     */
    ExtendedStackTraceElement[] toExtendedStackTrace(final Stack<Class<?>> stack, final Map<String, CacheEntry> map,
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
            this.commonElementCount = stackTrace.length - 1 - stackIndex;
            stackLength = stackIndex + 1;
        } else {
            this.commonElementCount = 0;
            stackLength = stackTrace.length;
        }
        final ExtendedStackTraceElement[] extStackTrace = new ExtendedStackTraceElement[stackLength];
        Class<?> clazz = stack.isEmpty() ? null : stack.peek();
        ClassLoader lastLoader = null;
        for (int i = stackLength - 1; i >= 0; --i) {
            final StackTraceElement stackTraceElement = stackTrace[i];
            final String className = stackTraceElement.getClassName();
            // The stack returned from getCurrentStack may be missing entries for java.lang.reflect.Method.invoke()
            // and its implementation. The Throwable might also contain stack entries that are no longer
            // present as those methods have returned.
            ExtendedClassInfo extClassInfo;
            if (clazz != null && className.equals(clazz.getName())) {
                final CacheEntry entry = this.toCacheEntry(stackTraceElement, clazz, true);
                extClassInfo = entry.element;
                lastLoader = entry.loader;
                stack.pop();
                clazz = stack.isEmpty() ? null : stack.peek();
            } else {
                final CacheEntry cacheEntry = map.get(className);
                if (cacheEntry != null) {
                    final CacheEntry entry = cacheEntry;
                    extClassInfo = entry.element;
                    if (entry.loader != null) {
                        lastLoader = entry.loader;
                    }
                } else {
                    final CacheEntry entry = this.toCacheEntry(stackTraceElement,
                        this.loadClass(lastLoader, className), false);
                    extClassInfo = entry.element;
                    map.put(stackTraceElement.toString(), entry);
                    if (entry.loader != null) {
                        lastLoader = entry.loader;
                    }
                }
            }
            extStackTrace[i] = new ExtendedStackTraceElement(stackTraceElement, extClassInfo);
        }
        return extStackTrace;
    }

    @Override
    public String toString() {
        final String msg = this.message;
        return msg != null ? this.name + ": " + msg : this.name;
    }

    private ThrowableProxy[] toSuppressedProxies(final Throwable thrown, Set<Throwable> suppressedVisited) {
        try {
            final Throwable[] suppressed = thrown.getSuppressed();
            if (suppressed == null) {
                return EMPTY_THROWABLE_PROXY_ARRAY;
            }
            final List<ThrowableProxy> proxies = new ArrayList<>(suppressed.length);
            if (suppressedVisited == null) {
                suppressedVisited = new HashSet<>(proxies.size());
            }
            for (int i = 0; i < suppressed.length; i++) {
                final Throwable candidate = suppressed[i];
                if (!suppressedVisited.contains(candidate)) {
                    suppressedVisited.add(candidate);
                    proxies.add(new ThrowableProxy(candidate, suppressedVisited));
                }
            }
            return proxies.toArray(new ThrowableProxy[proxies.size()]);
        } catch (final Exception e) {
            StatusLogger.getLogger().error(e);
        }
        return null;
    }
}
