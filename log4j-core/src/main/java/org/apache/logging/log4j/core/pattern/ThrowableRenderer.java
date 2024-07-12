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

import java.util.List;
import org.apache.logging.log4j.core.util.internal.StringBuilders;
import org.apache.logging.log4j.util.Strings;

public class ThrowableRenderer<C> {
    protected final List<String> ignoredPackageNames;
    protected final String stackTraceElementSuffix;
    protected final String lineSeparator;
    protected final int maxLineCount;
    private int ignoredCount;

    ThrowableRenderer(
            final List<String> ignoredPackageNames,
            final String stackTraceElementSuffix,
            final String lineSeparator,
            final int maxLineCount) {
        this.ignoredPackageNames = ignoredPackageNames;
        this.stackTraceElementSuffix = stackTraceElementSuffix;
        this.lineSeparator = lineSeparator;
        this.maxLineCount = maxLineCount;
    }

    final void renderThrowable(final StringBuilder buffer, final Throwable throwable) {
        C context = createContext(throwable);
        renderThrowable(buffer, throwable, context);
    }

    C createContext(final Throwable throwable) {
        return null;
    }

    void renderThrowable(final StringBuilder buffer, final Throwable throwable, final C context) {
        renderThrowableMessage(buffer, throwable);
        appendSuffix(buffer, stackTraceElementSuffix);
        appendLineSeparator(buffer, lineSeparator);

        ignoredCount = 0;
        final StackTraceElement[] stackTraceElements = throwable.getStackTrace();
        for (final StackTraceElement element : stackTraceElements) {
            renderStackTraceElement(buffer, element, context);
        }
        if (ignoredCount > 0) {
            appendSuppressedCount(buffer, ignoredCount, stackTraceElementSuffix, lineSeparator);
        }
        StringBuilders.truncateAfterDelimiter(buffer, lineSeparator, maxLineCount);
    }

    void renderStackTraceElement(
            final StringBuilder buffer, final StackTraceElement stackTraceElement, final C context) {
        if (!ignoreElement(stackTraceElement, ignoredPackageNames)) {
            if (ignoredCount > 0) {
                appendSuppressedCount(buffer, ignoredCount, stackTraceElementSuffix, lineSeparator);
                ignoredCount = 0;
            }
            appendEntry(stackTraceElement, buffer, stackTraceElementSuffix, lineSeparator);
        } else {
            ++ignoredCount;
        }
    }

    protected static void renderThrowableMessage(final StringBuilder buffer, final Throwable throwable) {
        final String message = throwable.getMessage();
        buffer.append(throwable.getClass().getName());
        if (message != null) {
            buffer.append(": ");
            buffer.append(message);
        }
    }

    protected static boolean ignoreElement(final StackTraceElement element, final List<String> ignorePackages) {
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

    protected static void appendSuppressedCount(
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

    protected static void appendEntry(
            final StackTraceElement stackTraceElement,
            final StringBuilder buffer,
            final String suffix,
            final String lineSeparator) {
        buffer.append(stackTraceElement.toString());
        appendSuffix(buffer, suffix);
        buffer.append(lineSeparator);
    }

    protected static void appendSuffix(final StringBuilder buffer, final String suffix) {
        if (Strings.isNotBlank(suffix)) {
            buffer.append(' ');
            buffer.append(suffix);
        }
    }

    protected static void appendLineSeparator(final StringBuilder buffer, final String lineSeparator) {
        buffer.append(lineSeparator);
    }
}
