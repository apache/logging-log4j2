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

class RootThrowableRenderer extends ThrowableRenderer<RootThrowableRenderer.RootContext> {

    static final String WRAPPED_BY_LABEL = "Wrapped by: ";

    RootThrowableRenderer(List<String> ignoredPackageNames, String lineSeparator, int maxLineCount) {
        super(ignoredPackageNames, lineSeparator, maxLineCount);
    }

    @Override
    RootContext createContext(final Throwable throwable) {
        final Map<Throwable, RootContext.ThrowableMetadata> map = new HashMap<>();
        final Set<Throwable> visited = new HashSet<>(1);
        buildMap(null, throwable, map, visited);
        return new RootContext(map);
    }

    @Override
    void renderThrowable(
            final StringBuilder buffer,
            final Throwable throwable,
            final RootContext context,
            final String stackTraceElementSuffix) {
        formatWrapper(buffer, throwable, context, stackTraceElementSuffix);
        StringBuilders.truncateAfterDelimiter(buffer, lineSeparator, maxLineCount);
    }

    private void formatWrapper(
            final StringBuilder buffer,
            final Throwable throwable,
            final RootContext context,
            final String stackTraceElementSuffix) {
        final Throwable cause = throwable.getCause();
        if (cause != null) {
            formatWrapper(buffer, throwable.getCause(), context, stackTraceElementSuffix);
            buffer.append(WRAPPED_BY_LABEL);
            appendSuffix(buffer, stackTraceElementSuffix);
        }
        renderThrowableMessage(buffer, throwable);
        appendSuffix(buffer, stackTraceElementSuffix);
        appendLineSeparator(buffer, lineSeparator);
        formatElements(buffer, throwable, context, stackTraceElementSuffix);
    }

    private void formatElements(
            final StringBuilder buffer,
            final Throwable throwable,
            final RootContext context,
            final String stackTraceElementSuffix) {
        context.ignoredStackTraceElementCount = 0;
        final RootContext.ThrowableMetadata metadata = context.throwableMetadataMap.get(throwable);
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

    private void buildMap(
            final Throwable rootThrowable,
            final Throwable throwable,
            final Map<Throwable, RootContext.ThrowableMetadata> map,
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

    private RootContext.ThrowableMetadata getMetadata(
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
        return new RootContext.ThrowableMetadata(commonElementCount, stackLength);
    }

    static class RootContext extends ThrowableRenderer.Context {
        public RootContext(Map<Throwable, ThrowableMetadata> throwableMetadataMap) {
            this.throwableMetadataMap = throwableMetadataMap;
        }

        Map<Throwable, ThrowableMetadata> throwableMetadataMap;

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
