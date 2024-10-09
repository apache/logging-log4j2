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
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * {@link ThrowableStackTraceRenderer} variant where the stack trace causal chain is processed in reverse order.
 */
final class ThrowableInvertedStackTraceRenderer
        extends ThrowableStackTraceRenderer<ThrowableStackTraceRenderer.Context> {

    private static final String WRAPPED_BY_CAPTION = "Wrapped by: ";

    ThrowableInvertedStackTraceRenderer(final List<String> ignoredPackageNames, final int maxLineCount) {
        super(ignoredPackageNames, maxLineCount);
    }

    @Override
    void renderThrowable(
            final StringBuilder buffer,
            final Throwable throwable,
            final Context context,
            final Set<Throwable> visitedThrowables,
            final String lineSeparator) {
        renderThrowable(buffer, throwable, context, visitedThrowables, "", lineSeparator, false);
    }

    private void renderThrowable(
            final StringBuilder buffer,
            final Throwable throwable,
            final Context context,
            final Set<Throwable> visitedThrowables,
            final String prefix,
            final String lineSeparator,
            boolean lineCapacityAcquired) {
        final boolean circular = !visitedThrowables.add(throwable);
        if (circular) {
            if (!lineCapacityAcquired) {
                acquireLineCapacity(context);
            }
            buffer.append("[CIRCULAR REFERENCE: ");
            renderThrowableMessage(buffer, throwable);
            buffer.append(']');
            buffer.append(lineSeparator);
        } else {
            lineCapacityAcquired = renderCause(
                    buffer,
                    throwable.getCause(),
                    context,
                    visitedThrowables,
                    prefix,
                    lineSeparator,
                    lineCapacityAcquired);
            if (!lineCapacityAcquired) {
                acquireLineCapacity(context);
            }
            renderThrowableMessage(buffer, throwable);
            buffer.append(lineSeparator);
            renderStackTraceElements(buffer, throwable, context, prefix, lineSeparator);
            renderSuppressed(
                    buffer, throwable.getSuppressed(), context, visitedThrowables, prefix + '\t', lineSeparator);
        }
    }

    /**
     * @return {@code true}, if line capacity is acquired; {@code false}, otherwise
     */
    private boolean renderCause(
            final StringBuilder buffer,
            @Nullable final Throwable cause,
            final Context context,
            final Set<Throwable> visitedThrowables,
            final String prefix,
            final String lineSeparator,
            final boolean lineCapacityAcquired) {
        if (cause != null) {
            renderThrowable(buffer, cause, context, visitedThrowables, prefix, lineSeparator, lineCapacityAcquired);
            acquireLineCapacity(context);
            buffer.append(prefix);
            buffer.append(WRAPPED_BY_CAPTION);
            return true;
        }
        return lineCapacityAcquired;
    }

    @Override
    void renderSuppressed(
            final StringBuilder buffer,
            final Throwable[] suppressedThrowables,
            final Context context,
            final Set<Throwable> visitedThrowables,
            final String prefix,
            final String lineSeparator) {
        if (suppressedThrowables.length > 0) {
            acquireLineCapacity(context);
            buffer.append(prefix);
            buffer.append(SUPPRESSED_CAPTION);
            for (int suppressedThrowableIndex = 0;
                    suppressedThrowableIndex < suppressedThrowables.length;
                    suppressedThrowableIndex++) {
                final Throwable suppressedThrowable = suppressedThrowables[suppressedThrowableIndex];
                final boolean lineCapacityAcquired = suppressedThrowableIndex == 0;
                renderThrowable(
                        buffer,
                        suppressedThrowable,
                        context,
                        visitedThrowables,
                        prefix,
                        lineSeparator,
                        lineCapacityAcquired);
            }
        }
    }
}
