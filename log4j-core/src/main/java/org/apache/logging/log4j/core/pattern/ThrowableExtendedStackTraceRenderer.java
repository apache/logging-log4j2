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
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
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
            classResourceInfo.render(buffer);
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
            // But we need the associated `Class` to extract its resource information, i.e., JAR file and version.
            // We are capturing the current stack to find suitable class loaders.
            // We will use this as a bootstrap to go from a class name in a stack trace to a `Class`.
            final Deque<Class<?>> executionStackTrace = StackLocatorUtil.getCurrentStackTrace();

            // Mapping a class name to a `ClassResourceInfo` is an expensive operation.
            // Next to `ClassResourceInfo` allocation, it requires extraction of the associated `Class`.
            // We will use this lookup table to speed things up.
            final Map<String, ClassResourceInfo> classResourceInfoByName = new HashMap<>();

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

                Class<?> executionStackTraceElementClass =
                        executionStackTrace.isEmpty() ? null : executionStackTrace.peekLast();
                ClassLoader lastLoader = null;
                final StackTraceElement[] stackTraceElements = throwable.getStackTrace();
                for (int throwableStackIndex = metadata.stackLength - 1;
                        throwableStackIndex >= 0;
                        --throwableStackIndex) {

                    // Get the exception's stack trace element
                    final StackTraceElement throwableStackTraceElement = stackTraceElements[throwableStackIndex];
                    final String throwableStackTraceElementClassName = throwableStackTraceElement.getClassName();

                    // Skip if the current class name is already registered
                    ClassResourceInfo classResourceInfo =
                            classResourceInfoByName.get(throwableStackTraceElementClassName);
                    if (classResourceInfo != null) {
                        if (classResourceInfo.clazz != null) {
                            lastLoader = classResourceInfo.clazz.getClassLoader();
                        }
                    }

                    // See if we get a match from the execution stack trace
                    else if (executionStackTraceElementClass != null
                            && throwableStackTraceElementClassName.equals(executionStackTraceElementClass.getName())) {
                        classResourceInfo = new ClassResourceInfo(executionStackTraceElementClass, true);
                        classResourceInfoByName.put(throwableStackTraceElementClassName, classResourceInfo);
                        lastLoader = classResourceInfo.clazz.getClassLoader();
                        executionStackTrace.pollLast();
                        executionStackTraceElementClass = executionStackTrace.peekLast();
                    }

                    // We don't know this class name, try to load it using the last found loader
                    else {
                        final Class<?> stackTraceElementClass =
                                loadClass(lastLoader, throwableStackTraceElementClassName);
                        classResourceInfo = stackTraceElementClass != null
                                ? new ClassResourceInfo(stackTraceElementClass, false)
                                : ClassResourceInfo.UNKNOWN;
                        classResourceInfoByName.put(throwableStackTraceElementClassName, classResourceInfo);
                    }
                }
            }
            return classResourceInfoByName;
        }

        private static Class<?> loadClass(final ClassLoader loader, final String className) {
            for (final ClassLoadingStrategy strategy : CLASS_LOADING_STRATEGIES) {
                try {
                    final Class<?> clazz = strategy.run(loader, className);
                    if (clazz != null) {
                        return clazz;
                    }
                } catch (final Exception ignored) {
                    // Do nothing
                }
            }
            return null;
        }
    }

    private static final ClassLoadingStrategy[] CLASS_LOADING_STRATEGIES = {
        // 1. Try the passed class loader
        (loader, className) -> loader != null ? loader.loadClass(className) : null,
        // 2. Try the `LoaderUtil` magic
        (loader, className) -> LoaderUtil.loadClass(className),
        // 3. Try the current class loader
        (loader, className) ->
                ThrowableExtendedStackTraceRenderer.class.getClassLoader().loadClass(className)
    };

    private interface ClassLoadingStrategy {

        Class<?> run(final ClassLoader loader, final String className) throws Exception;
    }
}
