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

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.StackLocatorUtil;

/**
 * {@link ThrowableStackTraceRenderer} variant where the rendered {@link StackTraceElement}s are enriched with the enclosing JAR file and its version information, if available.
 */
final class ThrowableExtendedStackTraceRenderer
        extends ThrowableStackTraceRenderer<ThrowableExtendedStackTraceRenderer.ExtendedContext> {

    ThrowableExtendedStackTraceRenderer(final List<String> ignoredPackageNames, final int maxLineCount) {
        super(ignoredPackageNames, maxLineCount);
    }

    @Override
    ExtendedContext createContext(final Throwable throwable) {
        return ExtendedContext.ofThrowable(throwable);
    }

    @Override
    void renderStackTraceElement(
            final StringBuilder buffer,
            final StackTraceElement stackTraceElement,
            final ExtendedContext context,
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
        final ClassResourceInfo classResourceInfo =
                context.classResourceInfoByName.get(stackTraceElement.getClassName());
        if (classResourceInfo != null) {
            buffer.append(' ');
            buffer.append(classResourceInfo);
        }
        buffer.append(lineSeparator);
    }

    static final class ExtendedContext extends ThrowableStackTraceRenderer.Context {

        private final Map<String, ClassResourceInfo> classResourceInfoByName;

        private ExtendedContext(
                final int ignoredStackTraceElementCount,
                final Map<Throwable, Metadata> metadataByThrowable,
                final Map<String, ClassResourceInfo> classResourceInfoByName) {
            super(ignoredStackTraceElementCount, metadataByThrowable);
            this.classResourceInfoByName = classResourceInfoByName;
        }

        private static ExtendedContext ofThrowable(final Throwable throwable) {
            final Map<Throwable, Metadata> metadataByThrowable = Metadata.ofThrowable(throwable);
            final Map<String, ClassResourceInfo> classResourceInfoByName =
                    createClassResourceInfoByName(throwable, metadataByThrowable);
            return new ExtendedContext(0, metadataByThrowable, classResourceInfoByName);
        }

        private static Map<String, ClassResourceInfo> createClassResourceInfoByName(
                final Throwable rootThrowable, final Map<Throwable, Metadata> metadataByThrowable) {

            // Stack trace elements of a `Throwable` only contain the class name.
            // But we need the associated `Class<?>` to extract its resource information, i.e., JAR file and version.
            // We are capturing the current stack to find suitable class loaders.
            // We will use this as a bootstrap to go from a class name in a stack trace to a `Class<?>`.
            final Map<String, ClassResourceInfo> classResourceInfoByName =
                    StackLocatorUtil.getCurrentStackTrace().stream()
                            .collect(Collectors.toMap(
                                    Class::getName,
                                    clazz -> new ClassResourceInfo(clazz, true),
                                    (classResourceInfo1, classResourceInfo2) -> classResourceInfo1));

            // Walk over the causal chain
            final Set<Throwable> visitedThrowables = new HashSet<>();
            final Queue<Throwable> pendingThrowables = new ArrayDeque<>(Collections.singleton(rootThrowable));
            Throwable throwable;
            while ((throwable = pendingThrowables.poll()) != null && visitedThrowables.add(throwable)) {

                // Add the cause to the processing queue
                final Throwable cause = throwable.getCause();
                if (cause != null) {
                    pendingThrowables.offer(cause);
                }

                // Short-circuit if there are no associated metadata
                final Metadata metadata = metadataByThrowable.get(throwable);
                if (metadata == null) {
                    continue;
                }

                ClassLoader lastLoader = null;
                final StackTraceElement[] stackTraceElements = throwable.getStackTrace();
                for (int throwableStackIndex = metadata.stackLength - 1;
                        throwableStackIndex >= 0;
                        --throwableStackIndex) {

                    // Skip if the current class name is either known, or already visited and is unknown
                    final StackTraceElement stackTraceElement = stackTraceElements[throwableStackIndex];
                    final String stackTraceElementClassName = stackTraceElement.getClassName();
                    ClassResourceInfo classResourceInfo = classResourceInfoByName.get(stackTraceElementClassName);
                    if (classResourceInfo != null) {
                        if (classResourceInfo.clazz != null) {
                            lastLoader = classResourceInfo.clazz.getClassLoader();
                        }
                        continue;
                    }

                    // Try to determine the stack trace element class, and register the result to the lookup table
                    final Class<?> stackTraceElementClass = loadClass(lastLoader, stackTraceElementClassName);
                    classResourceInfo = stackTraceElementClass != null
                            ? new ClassResourceInfo(stackTraceElementClass, false)
                            : ClassResourceInfo.UNKNOWN;
                    classResourceInfoByName.put(stackTraceElementClassName, classResourceInfo);
                }
            }
            return classResourceInfoByName;
        }

        @FunctionalInterface
        private interface ThrowingSupplier<V> {

            V supply() throws Exception;
        }

        private static Class<?> loadClass(final ClassLoader loader, final String className) {
            return Stream.<ThrowingSupplier<Class<?>>>of(
                            // 1. Try the passed class loader
                            () -> loader != null ? loader.loadClass(className) : null,
                            // 2. Try the `LoaderUtil` magic
                            () -> LoaderUtil.loadClass(className),
                            // 3. Try the current class loader
                            () -> ThrowableExtendedStackTraceRenderer.class
                                    .getClassLoader()
                                    .loadClass(className))
                    .map(provider -> {
                        try {
                            final Class<?> clazz = provider.supply();
                            if (clazz != null) {
                                return clazz;
                            }
                        } catch (final Exception ignored) {
                            // Do nothing
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        }
    }
}
