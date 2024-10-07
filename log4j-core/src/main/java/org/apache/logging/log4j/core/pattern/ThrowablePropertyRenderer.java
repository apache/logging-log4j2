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

import org.apache.logging.log4j.core.impl.ThrowableFormatOptions;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
enum ThrowablePropertyRenderer implements ThrowableRenderer {
    MESSAGE(ThrowableFormatOptions.MESSAGE, (buffer, throwable, lineSeparator) -> {
        final String message = throwable.getMessage();
        buffer.append(message);
        buffer.append(lineSeparator);
    }),
    LOCALIZED_MESSAGE(ThrowableFormatOptions.LOCALIZED_MESSAGE, (buffer, throwable, lineSeparator) -> {
        final String localizedMessage = throwable.getLocalizedMessage();
        buffer.append(localizedMessage);
        buffer.append(lineSeparator);
    }),
    CLASS_NAME(ThrowableFormatOptions.CLASS_NAME, ((buffer, throwable, lineSeparator) -> {
        @Nullable final StackTraceElement[] stackTraceElements = throwable.getStackTrace();
        if (stackTraceElements != null && stackTraceElements.length > 0) {
            final StackTraceElement throwingMethod = stackTraceElements[0];
            final String className = throwingMethod.getClassName();
            buffer.append(className);
            buffer.append(lineSeparator);
        }
    })),
    METHOD_NAME(ThrowableFormatOptions.METHOD_NAME, ((buffer, throwable, lineSeparator) -> {
        @Nullable final StackTraceElement[] stackTraceElements = throwable.getStackTrace();
        if (stackTraceElements != null && stackTraceElements.length > 0) {
            final StackTraceElement throwingMethod = stackTraceElements[0];
            final String methodName = throwingMethod.getMethodName();
            buffer.append(methodName);
            buffer.append(lineSeparator);
        }
    })),
    LINE_NUMBER(ThrowableFormatOptions.LINE_NUMBER, ((buffer, throwable, lineSeparator) -> {
        @Nullable final StackTraceElement[] stackTraceElements = throwable.getStackTrace();
        if (stackTraceElements != null && stackTraceElements.length > 0) {
            final StackTraceElement throwingMethod = stackTraceElements[0];
            final int lineNumber = throwingMethod.getLineNumber();
            buffer.append(lineNumber);
            buffer.append(lineSeparator);
        }
    })),
    FILE_NAME(ThrowableFormatOptions.FILE_NAME, ((buffer, throwable, lineSeparator) -> {
        @Nullable final StackTraceElement[] stackTraceElements = throwable.getStackTrace();
        if (stackTraceElements != null && stackTraceElements.length > 0) {
            final StackTraceElement throwingMethod = stackTraceElements[0];
            final String fileName = throwingMethod.getFileName();
            buffer.append(fileName);
            buffer.append(lineSeparator);
        }
    }));

    private final String name;

    private final ThrowableRenderer delegate;

    ThrowablePropertyRenderer(final String name, final ThrowableRenderer delegate) {
        this.name = name;
        this.delegate = delegate;
    }

    @Override
    public void renderThrowable(final StringBuilder buffer, final Throwable throwable, final String lineSeparator) {
        delegate.renderThrowable(buffer, throwable, lineSeparator);
    }

    @Nullable
    static ThrowablePropertyRenderer fromOptions(@Nullable final String[] options) {
        if (options != null && options.length > 0) {
            final String name = options[0];
            for (final ThrowablePropertyRenderer renderer : values()) {
                if (renderer.name.equalsIgnoreCase(name)) {
                    return renderer;
                }
            }
        }
        return null;
    }
}
