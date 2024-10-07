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
import java.util.Map;
import java.util.function.Function;
import org.apache.logging.log4j.core.impl.ThrowableFormatOptions;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A factory of {@link ThrowableRenderer} implementations for extracting certain properties from a {@link Throwable}.
 */
@NullMarked
class ThrowablePropertyRendererFactory {

    private static final ThrowableRenderer MESSAGE_RENDERER = (buffer, throwable, lineSeparator) -> {
        final String message = throwable.getMessage();
        buffer.append(message);
    };

    private static final ThrowableRenderer LOCALIZED_MESSAGE_RENDERER = (buffer, throwable, lineSeparator) -> {
        final String localizedMessage = throwable.getLocalizedMessage();
        buffer.append(localizedMessage);
    };

    private static final Function<Throwable, @Nullable StackTraceElement> THROWING_METHOD_EXTRACTOR = throwable -> {
        @Nullable final StackTraceElement[] stackTraceElements = throwable.getStackTrace();
        return (stackTraceElements != null && stackTraceElements.length > 0) ? stackTraceElements[0] : null;
    };

    static final ThrowablePropertyRendererFactory INSTANCE =
            new ThrowablePropertyRendererFactory(THROWING_METHOD_EXTRACTOR);

    private final Map<String, ThrowableRenderer> rendererByPropertyName;

    ThrowablePropertyRendererFactory(final Function<Throwable, @Nullable StackTraceElement> throwingMethodExtractor) {
        this.rendererByPropertyName = createRendererByPropertyName(throwingMethodExtractor);
    }

    private static Map<String, ThrowableRenderer> createRendererByPropertyName(
            final Function<Throwable, @Nullable StackTraceElement> throwingMethodExtractor) {
        final Map<String, ThrowableRenderer> map = new HashMap<>();
        map.put(ThrowableFormatOptions.MESSAGE, MESSAGE_RENDERER);
        map.put(ThrowableFormatOptions.LOCALIZED_MESSAGE, LOCALIZED_MESSAGE_RENDERER);
        map.put(ThrowableFormatOptions.CLASS_NAME, createClassNameRenderer(throwingMethodExtractor));
        map.put(ThrowableFormatOptions.METHOD_NAME, createMethodNameRenderer(throwingMethodExtractor));
        map.put(ThrowableFormatOptions.LINE_NUMBER, createLineNumberRenderer(throwingMethodExtractor));
        map.put(ThrowableFormatOptions.FILE_NAME, createFileNameRenderer(throwingMethodExtractor));
        return map;
    }

    private static ThrowableRenderer createClassNameRenderer(
            final Function<Throwable, @Nullable StackTraceElement> throwingMethodExtractor) {
        return (buffer, throwable, lineSeparator) -> {
            @Nullable final StackTraceElement throwingMethod = throwingMethodExtractor.apply(throwable);
            if (throwingMethod != null) {
                final String className = throwingMethod.getClassName();
                buffer.append(className);
            }
        };
    }

    private static ThrowableRenderer createMethodNameRenderer(
            final Function<Throwable, @Nullable StackTraceElement> throwingMethodExtractor) {
        return (buffer, throwable, lineSeparator) -> {
            @Nullable final StackTraceElement throwingMethod = throwingMethodExtractor.apply(throwable);
            if (throwingMethod != null) {
                final String methodName = throwingMethod.getMethodName();
                buffer.append(methodName);
            }
        };
    }

    private static ThrowableRenderer createLineNumberRenderer(
            final Function<Throwable, @Nullable StackTraceElement> throwingMethodExtractor) {
        return (buffer, throwable, lineSeparator) -> {
            @Nullable final StackTraceElement throwingMethod = throwingMethodExtractor.apply(throwable);
            if (throwingMethod != null) {
                final int lineNumber = throwingMethod.getLineNumber();
                buffer.append(lineNumber);
            }
        };
    }

    private static ThrowableRenderer createFileNameRenderer(
            final Function<Throwable, @Nullable StackTraceElement> throwingMethodExtractor) {
        return (buffer, throwable, lineSeparator) -> {
            @Nullable final StackTraceElement throwingMethod = throwingMethodExtractor.apply(throwable);
            if (throwingMethod != null) {
                final String fileName = throwingMethod.getFileName();
                buffer.append(fileName);
            }
        };
    }

    @Nullable
    final ThrowableRenderer createPropertyRenderer(@Nullable final String[] options) {
        if (options != null && options.length > 0) {
            final String propertyName = options[0];
            return rendererByPropertyName.get(propertyName);
        }
        return null;
    }
}
