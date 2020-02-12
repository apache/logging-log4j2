/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.layout.json.template.resolver;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;

final class StackTraceObjectResolver implements StackTraceResolver {

    private final TemplateResolver<StackTraceElement> stackTraceElementResolver;

    StackTraceObjectResolver(final TemplateResolver<StackTraceElement> stackTraceElementResolver) {
        this.stackTraceElementResolver = stackTraceElementResolver;
    }

    @Override
    public void resolve(
            final Throwable throwable,
            final JsonGenerator jsonGenerator)
            throws IOException {
        final StackTraceElement[] stackTraceElements = throwable.getStackTrace();
        if (stackTraceElements.length  == 0) {
            jsonGenerator.writeNull();
        } else {
            jsonGenerator.writeStartArray();
            // noinspection ForLoopReplaceableByForEach (avoid iterator instantiation)
            for (int stackTraceElementIndex = 0;
                 stackTraceElementIndex < stackTraceElements.length;
                 stackTraceElementIndex++) {
                final StackTraceElement stackTraceElement = stackTraceElements[stackTraceElementIndex];
                stackTraceElementResolver.resolve(stackTraceElement, jsonGenerator);
            }
            jsonGenerator.writeEndArray();
        }
    }

}
