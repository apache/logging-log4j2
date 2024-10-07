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

import static org.apache.logging.log4j.util.Strings.LINE_SEPARATOR;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * {@link ThrowableRenderer} implementation for rendering stack traces.
 *
 * @param <C> the context type
 */
@NullMarked
class ThrowableStackTraceRenderer<C extends ThrowableStackTraceRenderer.Context> implements ThrowableRenderer {

    private static final RuntimeException MAX_LINE_COUNT_EXCEEDED = new RuntimeException("max-line-count-exceeded");

    private static final String CAUSED_BY_CAPTION = "Caused by: ";

    static final String SUPPRESSED_CAPTION = "Suppressed: ";

    final List<String> ignoredPackageNames;

    final int maxLineCount;

    ThrowableStackTraceRenderer(final List<String> ignoredPackageNames, final int maxLineCount) {
        this.ignoredPackageNames = ignoredPackageNames;
        this.maxLineCount = maxLineCount;
    }

    @Override
    public final void renderThrowable(
            final StringBuilder buffer, final Throwable throwable, final String lineSeparator) {
        if (maxLineCount > 0) {
            try {
                C context = createContext(throwable);
                ensureNewlineSuffix(buffer);
                renderThrowable(buffer, throwable, context, new HashSet<>(), lineSeparator);
            } catch (final Exception error) {
                if (error != MAX_LINE_COUNT_EXCEEDED) {
                    throw error;
                }
            }
        }
    }

    private static void ensureNewlineSuffix(final StringBuilder buffer) {
        final int bufferLength = buffer.length();
        if (bufferLength > 0 && buffer.charAt(bufferLength - 1) != '\n') {
            buffer.append(LINE_SEPARATOR);
        }
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
            final Set<Throwable> visitedThrowables,
            final String lineSeparator) {
        renderThrowable(buffer, throwable, context, visitedThrowables, "", lineSeparator, "");
    }

    private void renderThrowable(
            final StringBuilder buffer,
            final Throwable throwable,
            final C context,
            final Set<Throwable> visitedThrowables,
            final String prefix,
            final String lineSeparator,
            final String caption) {
        acquireLineCapacity(context);
        final boolean circular = !visitedThrowables.add(throwable);
        buffer.append(prefix);
        buffer.append(caption);
        if (circular) {
            buffer.append("[CIRCULAR REFERENCE: ");
            renderThrowableMessage(buffer, throwable);
            buffer.append(']');
            buffer.append(lineSeparator);
        } else {
            renderThrowableMessage(buffer, throwable);
            buffer.append(lineSeparator);
            renderStackTraceElements(buffer, throwable, context, prefix, lineSeparator);
            renderSuppressed(
                    buffer, throwable.getSuppressed(), context, visitedThrowables, prefix + '\t', lineSeparator);
            renderCause(buffer, throwable.getCause(), context, visitedThrowables, prefix, lineSeparator);
        }
    }

    void acquireLineCapacity(final C context) {
        if (context.lineCount < maxLineCount) {
            context.lineCount++;
        } else {
            throw MAX_LINE_COUNT_EXCEEDED;
        }
    }

    void renderSuppressed(
            final StringBuilder buffer,
            final Throwable[] suppressedThrowables,
            final C context,
            final Set<Throwable> visitedThrowables,
            final String prefix,
            final String lineSeparator) {
        for (final Throwable suppressedThrowable : suppressedThrowables) {
            renderThrowable(
                    buffer, suppressedThrowable, context, visitedThrowables, prefix, lineSeparator, SUPPRESSED_CAPTION);
        }
    }

    private void renderCause(
            final StringBuilder buffer,
            @Nullable final Throwable cause,
            final C context,
            final Set<Throwable> visitedThrowables,
            final String prefix,
            final String lineSeparator) {
        if (cause != null) {
            renderThrowable(buffer, cause, context, visitedThrowables, prefix, lineSeparator, CAUSED_BY_CAPTION);
        }
    }

    static void renderThrowableMessage(final StringBuilder buffer, final Throwable throwable) {
        final String message = throwable.getLocalizedMessage();
        buffer.append(throwable.getClass().getName());
        if (message != null) {
            buffer.append(": ");
            buffer.append(message);
        }
    }

    final void renderStackTraceElements(
            final StringBuilder buffer,
            final Throwable throwable,
            final C context,
            final String prefix,
            final String lineSeparator) {
        context.ignoredStackTraceElementCount = 0;
        final Context.Metadata metadata = context.metadataByThrowable.get(throwable);
        final StackTraceElement[] stackTraceElements = throwable.getStackTrace();
        for (int i = 0; i < metadata.stackLength; i++) {
            renderStackTraceElement(buffer, stackTraceElements[i], context, prefix, lineSeparator);
        }
        if (context.ignoredStackTraceElementCount > 0) {
            renderSuppressedCount(buffer, context, prefix, lineSeparator);
        }
        if (metadata.commonElementCount != 0) {
            acquireLineCapacity(context);
            buffer.append(prefix);
            buffer.append("\t... ");
            buffer.append(metadata.commonElementCount);
            buffer.append(" more");
            buffer.append(lineSeparator);
        }
    }

    void renderStackTraceElement(
            final StringBuilder buffer,
            final StackTraceElement stackTraceElement,
            final C context,
            final String prefix,
            final String lineSeparator) {

        // Short-circuit on ignored stack trace elements
        final boolean stackTraceElementIgnored = isStackTraceElementIgnored(stackTraceElement);
        if (stackTraceElementIgnored) {
            context.ignoredStackTraceElementCount += 1;
            return;
        }

        // Render the suppressed stack trace element count
        if (context.ignoredStackTraceElementCount > 0) {
            renderSuppressedCount(buffer, context, prefix, lineSeparator);
            context.ignoredStackTraceElementCount = 0;
        }

        // Render the stack trace element
        acquireLineCapacity(context);
        buffer.append(prefix);
        buffer.append("\tat ");
        buffer.append(stackTraceElement);
        buffer.append(lineSeparator);
    }

    boolean isStackTraceElementIgnored(final StackTraceElement element) {
        if (ignoredPackageNames != null) {
            final String className = element.getClassName();
            for (final String ignoredPackageName : ignoredPackageNames) {
                if (className.startsWith(ignoredPackageName)) {
                    return true;
                }
            }
        }
        return false;
    }

    void renderSuppressedCount(
            final StringBuilder buffer, final C context, final String prefix, final String lineSeparator) {
        acquireLineCapacity(context);
        buffer.append(prefix);
        if (context.ignoredStackTraceElementCount == 1) {
            buffer.append("\t...");
        } else {
            buffer.append("\t... suppressed ");
            buffer.append(context.ignoredStackTraceElementCount);
            buffer.append(" lines");
        }
        buffer.append(lineSeparator);
    }

    static class Context {

        /**
         * Number of lines consumed from the {@link Throwable} causal chain so far.
         */
        int lineCount = 0;

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
                    @Nullable final Throwable parentThrowable,
                    final Throwable throwable) {

                // Populate metadata of the current throwable
                @Nullable
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
                @Nullable final Throwable cause = throwable.getCause();
                if (cause != null && !visitedThrowables.contains(cause)) {
                    visitedThrowables.add(cause);
                    populateMetadata(metadataByThrowable, visitedThrowables, throwable, cause);
                }
            }

            private static Metadata populateMetadata(
                    @Nullable final StackTraceElement[] parentTrace, final StackTraceElement[] currentTrace) {
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
