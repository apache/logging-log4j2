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
package org.apache.logging.log4j.layout.template.json.resolver;

import org.apache.logging.log4j.layout.template.json.util.JsonWriter;

/**
 * {@link StackTraceElement} resolver.
 *
 * <h3>Configuration</h3>
 *
 * <pre>
 * config = "field" -> (
 *            "className"  |
 *            "fileName"   |
 *            "methodName" |
 *            "lineNumber" )
 * </pre>
 *
 * <h3>Examples</h3>
 *
 * Resolve the line number:
 *
 * <pre>
 * {
 *   "$resolver": "stackTraceElement",
 *   "field": "lineNumber"
 * }
 * </pre>
 */
final class StackTraceElementResolver implements TemplateResolver<StackTraceElement> {

    private static final TemplateResolver<StackTraceElement> CLASS_NAME_RESOLVER =
            (final StackTraceElement stackTraceElement, final JsonWriter jsonWriter) ->
                    jsonWriter.writeString(stackTraceElement.getClassName());

    private static final TemplateResolver<StackTraceElement> METHOD_NAME_RESOLVER =
            (final StackTraceElement stackTraceElement, final JsonWriter jsonWriter) ->
                    jsonWriter.writeString(stackTraceElement.getMethodName());

    private static final TemplateResolver<StackTraceElement> FILE_NAME_RESOLVER =
            (final StackTraceElement stackTraceElement, final JsonWriter jsonWriter) ->
                    jsonWriter.writeString(stackTraceElement.getFileName());

    private static final TemplateResolver<StackTraceElement> LINE_NUMBER_RESOLVER =
            (final StackTraceElement stackTraceElement, final JsonWriter jsonWriter) ->
                    jsonWriter.writeNumber(stackTraceElement.getLineNumber());

    private final TemplateResolver<StackTraceElement> internalResolver;

    StackTraceElementResolver(final TemplateResolverConfig config) {
        this.internalResolver = createInternalResolver(config);
    }

    static String getName() {
        return "stackTraceElement";
    }

    private TemplateResolver<StackTraceElement> createInternalResolver(
            final TemplateResolverConfig config) {
        final String fieldName = config.getString("field");
        if ("className".equals(fieldName)) {
            return CLASS_NAME_RESOLVER;
        } else if ("methodName".equals(fieldName)) {
            return METHOD_NAME_RESOLVER;
        } else if ("fileName".equals(fieldName)) {
            return FILE_NAME_RESOLVER;
        } else if ("lineNumber".equals(fieldName)) {
            return LINE_NUMBER_RESOLVER;
        }
        throw new IllegalArgumentException("unknown field: " + config);
    }

    @Override
    public void resolve(
            final StackTraceElement stackTraceElement,
            final JsonWriter jsonWriter) {
        internalResolver.resolve(stackTraceElement, jsonWriter);
    }

}
