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
package org.apache.logging.log4j.core.pattern;

import java.util.*;

import org.apache.logging.log4j.core.util.internal.StringBuilders;
import org.apache.logging.log4j.util.Strings;

class ThrowableRenderer<C extends ThrowableRenderer.Context> {
    static final String CAUSED_BY_LABEL = "Caused by: ";
    protected final List<String> ignoredPackageNames;
    protected final String lineSeparator;
    protected final int maxLineCount;

    ThrowableRenderer(final List<String> ignoredPackageNames, final String lineSeparator, final int maxLineCount) {
        this.ignoredPackageNames = ignoredPackageNames;
        this.lineSeparator = lineSeparator;
        this.maxLineCount = maxLineCount;
    }

    final void renderThrowable(
            final StringBuilder buffer, final Throwable throwable, final String stackTraceElementSuffix) {
        C context = createContext(throwable);
        renderThrowable(buffer, throwable, context, stackTraceElementSuffix);
    }

    @SuppressWarnings("unchecked")
    C createContext(final Throwable throwable) {
        final Map<Throwable, Context.ThrowableMetadata> map = new HashMap<>();
        final Set<Throwable> visited = new HashSet<>(1);
        buildMap(null, throwable, map, visited);
        return (C) new Context(map);
    }

    void renderThrowable(
            final StringBuilder buffer,
            final Throwable throwable,
            final C context,
            final String stackTraceElementSuffix) {
        format(buffer, throwable, context, stackTraceElementSuffix);
        StringBuilders.truncateAfterDelimiter(buffer, lineSeparator, maxLineCount);
    }

    void renderStackTraceElement(
            final StringBuilder buffer,
            final StackTraceElement stackTraceElement,
            final C context,
            final String stackTraceElementSuffix) {
        if (!ignoreElement(stackTraceElement, ignoredPackageNames)) {
            if (context.ignoredStackTraceElementCount > 0) {
                appendSuppressedCount(
                        buffer, context.ignoredStackTraceElementCount, stackTraceElementSuffix, lineSeparator);
                context.ignoredStackTraceElementCount = 0;
            }
            appendEntry(stackTraceElement, buffer, stackTraceElementSuffix, lineSeparator);
        } else {
            context.ignoredStackTraceElementCount += 1;
        }
    }

    static void renderThrowableMessage(final StringBuilder buffer, final Throwable throwable) {
        final String message = throwable.getMessage();
        buffer.append(throwable.getClass().getName());
        if (message != null) {
            buffer.append(": ");
            buffer.append(message);
        }
    }

    void format(
            final StringBuilder buffer,
            final Throwable throwable,
            final C context,
            final String stackTraceElementSuffix) {
        renderThrowableMessage(buffer, throwable);
        appendSuffix(buffer, stackTraceElementSuffix);
        appendLineSeparator(buffer, lineSeparator);
        formatElements(buffer, throwable, context, stackTraceElementSuffix);
        final Throwable cause = throwable.getCause();
        if (cause != null) {
            buffer.append(CAUSED_BY_LABEL);
            appendSuffix(buffer, stackTraceElementSuffix);
            format(buffer, throwable.getCause(), context, stackTraceElementSuffix);
        }
    }

    void formatElements(
            final StringBuilder buffer,
            final Throwable throwable,
            final C context,
            final String stackTraceElementSuffix) {
        context.ignoredStackTraceElementCount = 0;
        final Context.ThrowableMetadata metadata = context.throwableMetadataMap.get(throwable);
        final StackTraceElement[] stackTraceElements = throwable.getStackTrace();
        for (int i = 0; i < metadata.stackLength; i++) {
            renderStackTraceElement(buffer, stackTraceElements[i], context, stackTraceElementSuffix);
        }
        if (context.ignoredStackTraceElementCount > 0) {
            appendSuppressedCount(
                    buffer, context.ignoredStackTraceElementCount, stackTraceElementSuffix, lineSeparator);
        }
        if (metadata.commonElementCount != 0) {
            buffer.append("\t... ");
            buffer.append(metadata.commonElementCount);
            buffer.append(" more");
            appendSuffix(buffer, stackTraceElementSuffix);
            appendLineSeparator(buffer, lineSeparator);
        }
    }

    private static boolean ignoreElement(final StackTraceElement element, final List<String> ignorePackages) {
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

    static void appendSuppressedCount(
            final StringBuilder buffer, final int count, final String suffix, final String lineSeparator) {
        if (count == 1) {
            buffer.append("\t... ");
        } else {
            buffer.append("\t... suppressed ");
            buffer.append(count);
            buffer.append(" lines");
        }
        appendSuffix(buffer, suffix);
        buffer.append(lineSeparator);
    }

    private static void appendEntry(
            final StackTraceElement stackTraceElement,
            final StringBuilder buffer,
            final String suffix,
            final String lineSeparator) {
        buffer.append("\tat ");
        buffer.append(stackTraceElement.toString());
        appendSuffix(buffer, suffix);
        buffer.append(lineSeparator);
    }

    static void appendSuffix(final StringBuilder buffer, final String suffix) {
        if (Strings.isNotBlank(suffix)) {
            buffer.append(' ');
            buffer.append(suffix);
        }
    }

    static void appendLineSeparator(final StringBuilder buffer, final String lineSeparator) {
        buffer.append(lineSeparator);
    }

    private void buildMap(
            final Throwable rootThrowable,
            final Throwable throwable,
            final Map<Throwable, Context.ThrowableMetadata> map,
            final Set<Throwable> visited) {
        map.put(
                throwable,
                getMetadata(rootThrowable == null ? null : rootThrowable.getStackTrace(), throwable.getStackTrace()));

        Throwable throwableCause = throwable.getCause();
        if (throwableCause != null && !visited.contains(throwableCause)) {
            visited.add(throwableCause);
            buildMap(throwable, throwable.getCause(), map, visited);
        }
    }

    private Context.ThrowableMetadata getMetadata(
            final StackTraceElement[] rootTrace, final StackTraceElement[] currentTrace) {
        int commonElementCount;
        int stackLength;
        if (rootTrace != null) {
            int rootIndex = rootTrace.length - 1;
            int stackIndex = currentTrace.length - 1;
            while (rootIndex >= 0 && stackIndex >= 0 && rootTrace[rootIndex].equals(currentTrace[stackIndex])) {
                --rootIndex;
                --stackIndex;
            }
            commonElementCount = currentTrace.length - 1 - stackIndex;
            stackLength = stackIndex + 1;
        } else {
            commonElementCount = 0;
            stackLength = currentTrace.length;
        }
        return new Context.ThrowableMetadata(commonElementCount, stackLength);
    }

    static class Context {
        int ignoredStackTraceElementCount;
        Map<Throwable, ThrowableMetadata> throwableMetadataMap;

        public Context(Map<Throwable, ThrowableMetadata> throwableMetadataMap) {
            this.ignoredStackTraceElementCount = 0;
            this.throwableMetadataMap = throwableMetadataMap;
        }

        static class ThrowableMetadata {
            public ThrowableMetadata(int commonElementCount, int stackLength) {
                this.commonElementCount = commonElementCount;
                this.stackLength = stackLength;
            }

            int commonElementCount;
            int stackLength;
        }
    }
}
