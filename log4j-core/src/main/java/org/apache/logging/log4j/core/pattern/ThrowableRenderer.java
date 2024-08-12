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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.core.util.internal.StringBuilders;
import org.apache.logging.log4j.util.Strings;

class ThrowableRenderer<C extends ThrowableRenderer.Context> {

    private static final String CAUSED_BY_CAPTION = "Caused by: ";

    private static final String SUPPRESSED_CAPTION = "Suppressed: ";

    final List<String> ignoredPackageNames;

    final String lineSeparator;

    final int maxLineCount;

    ThrowableRenderer(final List<String> ignoredPackageNames, final String lineSeparator, final int maxLineCount) {
        this.ignoredPackageNames = ignoredPackageNames;
        this.lineSeparator = lineSeparator;
        this.maxLineCount = maxLineCount;
    }

    final void renderThrowable(
            final StringBuilder buffer, final Throwable throwable, final String stackTraceElementSuffix) {
        C context = createContext(throwable);
        renderThrowable(buffer, throwable, context, stackTraceElementSuffix, "", "");
        StringBuilders.truncateAfterDelimiter(buffer, lineSeparator, maxLineCount);
    }

    @SuppressWarnings("unchecked")
    C createContext(final Throwable throwable) {
        final Map<Throwable, Context.Metadata> metadataByThrowable = Context.Metadata.ofThrowable(throwable);
        return (C) new Context(0, metadataByThrowable);
    }

    void renderThrowable(
            final StringBuilder buffer,
            final Throwable throwable,
            final C context,
            final String stackTraceElementSuffix,
            final String prefix,
            final String caption) {
        buffer.append(prefix);
        buffer.append(caption);
        renderThrowableMessage(buffer, throwable);
        buffer.append(lineSeparator);
        renderStackTraceElements(buffer, throwable, context, prefix, stackTraceElementSuffix);
        renderSuppressed(buffer, throwable.getSuppressed(), context, stackTraceElementSuffix, prefix + '\t');
        renderCause(buffer, throwable.getCause(), context, stackTraceElementSuffix, prefix);
    }

    void renderSuppressed(
            final StringBuilder buffer,
            final Throwable[] suppressedThrowables,
            final C context,
            final String stackTraceElementSuffix,
            final String prefix) {
        for (final Throwable suppressedThrowable : suppressedThrowables) {
            renderThrowable(buffer, suppressedThrowable, context, stackTraceElementSuffix, prefix, SUPPRESSED_CAPTION);
        }
    }

    void renderCause(
            final StringBuilder buffer,
            final Throwable cause,
            final C context,
            final String stackTraceElementSuffix,
            final String prefix) {
        if (cause != null) {
            renderThrowable(buffer, cause, context, stackTraceElementSuffix, prefix, CAUSED_BY_CAPTION);
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

    void renderStackTraceElements(
            final StringBuilder buffer,
            final Throwable throwable,
            final C context,
            final String prefix,
            final String stackTraceElementSuffix) {
        context.ignoredStackTraceElementCount = 0;
        final Context.Metadata metadata = context.metadataByThrowable.get(throwable);
        final StackTraceElement[] stackTraceElements = throwable.getStackTrace();
        for (int i = 0; i < metadata.stackLength; i++) {
            renderStackTraceElement(buffer, stackTraceElements[i], context, prefix, stackTraceElementSuffix);
        }
        if (context.ignoredStackTraceElementCount > 0) {
            renderSuppressedCount(
                    buffer, context.ignoredStackTraceElementCount, prefix, lineSeparator);
        }
        if (metadata.commonElementCount != 0) {
            buffer.append(prefix);
            buffer.append("\t... ");
            buffer.append(metadata.commonElementCount);
            buffer.append(" more");
            renderSuffix(buffer, stackTraceElementSuffix);
            buffer.append(lineSeparator);
        }
    }

    void renderStackTraceElement(
            final StringBuilder buffer,
            final StackTraceElement stackTraceElement,
            final C context,
            final String prefix,
            final String stackTraceElementSuffix) {

        // Short-circuit on ignored stack trace elements
        final boolean stackTraceElementIgnored = isStackTraceElementIgnored(stackTraceElement, ignoredPackageNames);
        if (stackTraceElementIgnored) {
            context.ignoredStackTraceElementCount += 1;
            return;
        }

        // Render the stack trace element
        if (context.ignoredStackTraceElementCount > 0) {
            renderSuppressedCount(
                    buffer, context.ignoredStackTraceElementCount, prefix, lineSeparator);
            context.ignoredStackTraceElementCount = 0;
        }
        buffer.append(prefix);
        buffer.append("\tat ");
        buffer.append(stackTraceElement.toString());
        renderSuffix(buffer, stackTraceElementSuffix);
        buffer.append(lineSeparator);
    }

    static boolean isStackTraceElementIgnored(final StackTraceElement element, final List<String> ignorePackages) {
        if (ignorePackages != null) {
            final String className = element.getClassName();
            for (final String ignoredPackage : ignorePackages) {
                if (className.startsWith(ignoredPackage)) {
                    return true;
                }
            }
        }
        return false;
    }

    static void renderSuppressedCount(
            final StringBuilder buffer, final int count, final String prefix, final String lineSeparator) {
        buffer.append(prefix);
        if (count == 1) {
            buffer.append("\t... ");
        } else {
            buffer.append("\t... suppressed ");
            buffer.append(count);
            buffer.append(" lines");
        }
        buffer.append(lineSeparator);
    }

    static void renderSuffix(final StringBuilder buffer, final String suffix) {
        if (Strings.isNotBlank(suffix)) {
            buffer.append(' ');
            buffer.append(suffix);
        }
    }

    static class Context {

        /**
         * Number of stack trace elements ignored.
         * <p>
         * This value will be reset per {@link Throwable} in the causal chain.
         * </p>
         */
        int ignoredStackTraceElementCount;

        /**
         * {@link Metadata} per {@link Throwable} in the causal chain
         */
        final Map<Throwable, Metadata> metadataByThrowable;

        /**
         * The canonical constructor.
         */
        Context(final int ignoredStackTraceElementCount, final Map<Throwable, Metadata> metadataByThrowable) {
            this.ignoredStackTraceElementCount = ignoredStackTraceElementCount;
            this.metadataByThrowable = metadataByThrowable;
        }

        /**
         * Invariants associated with a {@link Throwable}
         */
        static final class Metadata {

            /**
             * Number of stack trace elements shared with the parent {@link Throwable}'s stack
             */
            final int commonElementCount;

            /**
             * Number of stack trace elements exclusive to this {@link Throwable}, i.e., not in common with the parent {@link Throwable}'s stack
             */
            final int stackLength;

            private Metadata(final int commonElementCount, final int stackLength) {
                this.commonElementCount = commonElementCount;
                this.stackLength = stackLength;
            }

            static Map<Throwable, Metadata> ofThrowable(final Throwable throwable) {
                final Map<Throwable, Metadata> metadataByThrowable = new HashMap<>();
                populateMetadata(metadataByThrowable, new HashSet<>(), null, throwable);
                return metadataByThrowable;
            }

            private static void populateMetadata(
                    final Map<Throwable, Metadata> metadataByThrowable,
                    final Set<Throwable> visitedThrowables,
                    final Throwable parentThrowable,
                    final Throwable throwable) {

                // Populate metadata of the current throwable
                final StackTraceElement[] rootTrace = parentThrowable == null ? null : parentThrowable.getStackTrace();
                final Metadata metadata = populateMetadata(rootTrace, throwable.getStackTrace());
                metadataByThrowable.put(throwable, metadata);

                // Populate metadata of suppressed exceptions
                for (final Throwable suppressed : throwable.getSuppressed()) {
                    if (!visitedThrowables.contains(suppressed)) {
                        visitedThrowables.add(suppressed);
                        populateMetadata(metadataByThrowable, visitedThrowables, throwable, suppressed);
                    }
                }

                // Populate metadata of the causal chain
                final Throwable cause = throwable.getCause();
                if (cause != null && !visitedThrowables.contains(cause)) {
                    visitedThrowables.add(cause);
                    populateMetadata(metadataByThrowable, visitedThrowables, throwable, cause);
                }
            }

            private static Metadata populateMetadata(
                    final StackTraceElement[] parentTrace, final StackTraceElement[] currentTrace) {
                int commonElementCount;
                int stackLength;
                if (parentTrace != null) {
                    int parentIndex = parentTrace.length - 1;
                    int currentIndex = currentTrace.length - 1;
                    while (parentIndex >= 0
                            && currentIndex >= 0
                            && parentTrace[parentIndex].equals(currentTrace[currentIndex])) {
                        --parentIndex;
                        --currentIndex;
                    }
                    commonElementCount = currentTrace.length - 1 - currentIndex;
                    stackLength = currentIndex + 1;
                } else {
                    commonElementCount = 0;
                    stackLength = currentTrace.length;
                }
                return new Metadata(commonElementCount, stackLength);
            }
        }
    }
}
