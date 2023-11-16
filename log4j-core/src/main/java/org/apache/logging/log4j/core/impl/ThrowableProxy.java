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
package org.apache.logging.log4j.core.impl;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.logging.log4j.core.pattern.PlainTextRenderer;
import org.apache.logging.log4j.core.pattern.TextRenderer;
import org.apache.logging.log4j.util.Chars;
import org.apache.logging.log4j.util.StackLocatorUtil;
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

    private static final char EOL = Chars.LF;

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

    static final ThrowableProxy[] EMPTY_ARRAY = {};

    /**
     * For JSON and XML IO via Jackson.
     */
    @SuppressWarnings("unused")
    ThrowableProxy() {
        this.throwable = null;
        this.name = null;
        this.extendedStackTrace = ExtendedStackTraceElement.EMPTY_ARRAY;
        this.causeProxy = null;
        this.message = null;
        this.localizedMessage = null;
        this.suppressedProxies = ThrowableProxy.EMPTY_ARRAY;
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
    ThrowableProxy(final Throwable throwable, final Set<Throwable> visited) {
        this.throwable = throwable;
        this.name = throwable.getClass().getName();
        this.message = throwable.getMessage();
        this.localizedMessage = throwable.getLocalizedMessage();
        final Map<String, ThrowableProxyHelper.CacheEntry> map = new HashMap<>();
        final Deque<Class<?>> stack = StackLocatorUtil.getCurrentStackTrace();
        this.extendedStackTrace =
                ThrowableProxyHelper.toExtendedStackTrace(this, stack, map, null, throwable.getStackTrace());
        final Throwable throwableCause = throwable.getCause();
        final Set<Throwable> causeVisited = new HashSet<>(1);
        this.causeProxy = throwableCause == null
                ? null
                : new ThrowableProxy(throwable, stack, map, throwableCause, visited, causeVisited);
        this.suppressedProxies = ThrowableProxyHelper.toSuppressedProxies(throwable, visited);
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
    private ThrowableProxy(
            final Throwable parent,
            final Deque<Class<?>> stack,
            final Map<String, ThrowableProxyHelper.CacheEntry> map,
            final Throwable cause,
            final Set<Throwable> suppressedVisited,
            final Set<Throwable> causeVisited) {
        causeVisited.add(cause);
        this.throwable = cause;
        this.name = cause.getClass().getName();
        this.message = this.throwable.getMessage();
        this.localizedMessage = this.throwable.getLocalizedMessage();
        this.extendedStackTrace = ThrowableProxyHelper.toExtendedStackTrace(
                this, stack, map, parent.getStackTrace(), cause.getStackTrace());
        final Throwable causeCause = cause.getCause();
        this.causeProxy = causeCause == null || causeVisited.contains(causeCause)
                ? null
                : new ThrowableProxy(parent, stack, map, causeCause, suppressedVisited, causeVisited);
        this.suppressedProxies = ThrowableProxyHelper.toSuppressedProxies(cause, suppressedVisited);
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
        if (!Objects.equals(this.causeProxy, other.causeProxy)) {
            return false;
        }
        if (this.commonElementCount != other.commonElementCount) {
            return false;
        }
        if (!Objects.equals(this.name, other.name)) {
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

    /**
     * Formats the specified Throwable.
     *  @param sb    StringBuilder to contain the formatted Throwable.
     * @param cause The Throwable to format.
     * @param suffix Append this to the end of each stack frame.
     */
    public void formatWrapper(final StringBuilder sb, final ThrowableProxy cause, final String suffix) {
        this.formatWrapper(sb, cause, null, PlainTextRenderer.getInstance(), suffix);
    }

    /**
     * Formats the specified Throwable.
     *  @param sb             StringBuilder to contain the formatted Throwable.
     * @param cause          The Throwable to format.
     * @param ignorePackages The List of packages to be suppressed from the trace.
     * @param suffix Append this to the end of each stack frame.
     */
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void formatWrapper(
            final StringBuilder sb,
            final ThrowableProxy cause,
            final List<String> ignorePackages,
            final String suffix) {
        this.formatWrapper(sb, cause, ignorePackages, PlainTextRenderer.getInstance(), suffix);
    }

    /**
     * Formats the specified Throwable.
     * @param sb StringBuilder to contain the formatted Throwable.
     * @param cause The Throwable to format.
     * @param ignorePackages The List of packages to be suppressed from the stack trace.
     * @param textRenderer The text renderer.
     * @param suffix Append this to the end of each stack frame.
     */
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void formatWrapper(
            final StringBuilder sb,
            final ThrowableProxy cause,
            final List<String> ignorePackages,
            final TextRenderer textRenderer,
            final String suffix) {
        formatWrapper(sb, cause, ignorePackages, textRenderer, suffix, EOL_STR);
    }

    /**
     * Formats the specified Throwable.
     * @param sb StringBuilder to contain the formatted Throwable.
     * @param cause The Throwable to format.
     * @param ignorePackages The List of packages to be suppressed from the stack trace.
     * @param textRenderer The text renderer.
     * @param suffix Append this to the end of each stack frame.
     * @param lineSeparator The end-of-line separator.
     */
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void formatWrapper(
            final StringBuilder sb,
            final ThrowableProxy cause,
            final List<String> ignorePackages,
            final TextRenderer textRenderer,
            final String suffix,
            final String lineSeparator) {
        ThrowableProxyRenderer.formatWrapper(sb, cause, ignorePackages, textRenderer, suffix, lineSeparator);
    }

    public ThrowableProxy getCauseProxy() {
        return this.causeProxy;
    }

    /**
     * Formats the Throwable that is the cause of this Throwable.
     *
     * @return The formatted Throwable that caused this Throwable.
     * @param suffix Append this to the end of each stack frame.
     */
    public String getCauseStackTraceAsString(final String suffix) {
        return this.getCauseStackTraceAsString(null, PlainTextRenderer.getInstance(), suffix, EOL_STR);
    }

    /**
     * Formats the Throwable that is the cause of this Throwable.
     *
     * @param packages The List of packages to be suppressed from the trace.
     * @param suffix Append this to the end of each stack frame.
     * @return The formatted Throwable that caused this Throwable.
     */
    public String getCauseStackTraceAsString(final List<String> packages, final String suffix) {
        return getCauseStackTraceAsString(packages, PlainTextRenderer.getInstance(), suffix, EOL_STR);
    }

    /**
     * Formats the Throwable that is the cause of this Throwable.
     *
     * @param ignorePackages The List of packages to be suppressed from the trace.
     * @param textRenderer The text renderer.
     * @param suffix Append this to the end of each stack frame.
     * @return The formatted Throwable that caused this Throwable.
     */
    public String getCauseStackTraceAsString(
            final List<String> ignorePackages, final TextRenderer textRenderer, final String suffix) {
        return getCauseStackTraceAsString(ignorePackages, textRenderer, suffix, EOL_STR);
    }

    /**
     * Formats the Throwable that is the cause of this Throwable.
     *
     * @param ignorePackages The List of packages to be suppressed from the stack trace.
     * @param textRenderer The text renderer.
     * @param suffix Append this to the end of each stack frame.
     * @param lineSeparator The end-of-line separator.
     * @return The formatted Throwable that caused this Throwable.
     */
    public String getCauseStackTraceAsString(
            final List<String> ignorePackages,
            final TextRenderer textRenderer,
            final String suffix,
            final String lineSeparator) {
        final StringBuilder sb = new StringBuilder();
        ThrowableProxyRenderer.formatCauseStackTrace(this, sb, ignorePackages, textRenderer, suffix, lineSeparator);
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
     * Set the value of {@link ThrowableProxy#commonElementCount}.
     *
     * Method is package-private, to be used internally for initialization.
     *
     * @param value New value of commonElementCount.
     */
    void setCommonElementCount(final int value) {
        this.commonElementCount = value;
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
     * Formats the stack trace including packaging information.
     *
     * @return The formatted stack trace including packaging information.
     */
    public String getExtendedStackTraceAsString() {
        return this.getExtendedStackTraceAsString(null, PlainTextRenderer.getInstance(), Strings.EMPTY, EOL_STR);
    }

    /**
     * Formats the stack trace including packaging information.
     *
     * @return The formatted stack trace including packaging information.
     * @param suffix Append this to the end of each stack frame.
     */
    public String getExtendedStackTraceAsString(final String suffix) {
        return this.getExtendedStackTraceAsString(null, PlainTextRenderer.getInstance(), suffix, EOL_STR);
    }

    /**
     * Formats the stack trace including packaging information.
     *
     * @param ignorePackages List of packages to be ignored in the trace.
     * @param suffix Append this to the end of each stack frame.
     * @return The formatted stack trace including packaging information.
     */
    public String getExtendedStackTraceAsString(final List<String> ignorePackages, final String suffix) {
        return getExtendedStackTraceAsString(ignorePackages, PlainTextRenderer.getInstance(), suffix, EOL_STR);
    }

    /**
     * Formats the stack trace including packaging information.
     *
     * @param ignorePackages List of packages to be ignored in the trace.
     * @param textRenderer The message renderer.
     * @param suffix Append this to the end of each stack frame.
     * @return The formatted stack trace including packaging information.
     */
    public String getExtendedStackTraceAsString(
            final List<String> ignorePackages, final TextRenderer textRenderer, final String suffix) {
        return getExtendedStackTraceAsString(ignorePackages, textRenderer, suffix, EOL_STR);
    }

    /**
     * Formats the stack trace including packaging information.
     *
     * @param ignorePackages List of packages to be ignored in the trace.
     * @param textRenderer The message renderer.
     * @param suffix Append this to the end of each stack frame.
     * @param lineSeparator The end-of-line separator.
     * @return The formatted stack trace including packaging information.
     */
    public String getExtendedStackTraceAsString(
            final List<String> ignorePackages,
            final TextRenderer textRenderer,
            final String suffix,
            final String lineSeparator) {
        final StringBuilder sb = new StringBuilder(1024);
        formatExtendedStackTraceTo(sb, ignorePackages, textRenderer, suffix, lineSeparator);
        return sb.toString();
    }

    /**
     * Formats the stack trace including packaging information.
     *
     * @param sb Destination.
     * @param ignorePackages List of packages to be ignored in the trace.
     * @param textRenderer The message renderer.
     * @param suffix Append this to the end of each stack frame.
     * @param lineSeparator The end-of-line separator.
     */
    public void formatExtendedStackTraceTo(
            final StringBuilder sb,
            final List<String> ignorePackages,
            final TextRenderer textRenderer,
            final String suffix,
            final String lineSeparator) {
        ThrowableProxyRenderer.formatExtendedStackTraceTo(
                this, sb, ignorePackages, textRenderer, suffix, lineSeparator);
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
     * Formats the suppressed Throwables.
     *
     * @return The formatted suppressed Throwables.
     * @param suffix Append this to the end of each stack frame.
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

    @Override
    public String toString() {
        final String msg = this.message;
        return msg != null ? this.name + ": " + msg : this.name;
    }
}
