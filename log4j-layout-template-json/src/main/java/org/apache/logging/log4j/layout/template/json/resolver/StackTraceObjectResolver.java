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
package org.apache.logging.log4j.layout.template.json.resolver;

import org.apache.logging.log4j.layout.template.json.util.JsonWriter;

/**
 * Exception stack trace to JSON object resolver used by {@link ExceptionResolver}.
 */
final class StackTraceObjectResolver implements StackTraceResolver {

    private final TemplateResolver<StackTraceElement> stackTraceElementResolver;

    StackTraceObjectResolver(final TemplateResolver<StackTraceElement> stackTraceElementResolver) {
        this.stackTraceElementResolver = stackTraceElementResolver;
    }

    @Override
    public void resolve(final Throwable throwable, final JsonWriter jsonWriter) {
        // Following check against the stacktrace element count is not
        // implemented in isResolvable(), since Throwable#getStackTrace() incurs
        // a significant cloning cost.
        final StackTraceElement[] stackTraceElements = throwable.getStackTrace();
        if (stackTraceElements.length == 0) {
            jsonWriter.writeNull();
        } else {
            jsonWriter.writeArrayStart();
            for (int stackTraceElementIndex = 0;
                    stackTraceElementIndex < stackTraceElements.length;
                    stackTraceElementIndex++) {
                if (stackTraceElementIndex > 0) {
                    jsonWriter.writeSeparator();
                }
                final StackTraceElement stackTraceElement = stackTraceElements[stackTraceElementIndex];
                stackTraceElementResolver.resolve(stackTraceElement, jsonWriter);
            }
            jsonWriter.writeArrayEnd();
        }
    }
}
