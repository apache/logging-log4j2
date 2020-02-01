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
package org.apache.logging.log4j.jackson.json.template.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.util.Strings;

import java.io.IOException;

final class SourceResolver implements EventResolver {

    private static final EventResolver NULL_RESOLVER =
            (final LogEvent value, final JsonGenerator jsonGenerator) ->
                    jsonGenerator.writeNull();

    private final EventResolver internalResolver;

    SourceResolver(final EventResolverContext context, final String key) {
        this.internalResolver = createInternalResolver(context, key);
    }

    private EventResolver createInternalResolver(
            final EventResolverContext context,
            final String key) {
        if (!context.isLocationInfoEnabled()) {
            return NULL_RESOLVER;
        }
        switch (key) {
            case "className": return createClassNameResolver(context);
            case "fileName": return createFileNameResolver(context);
            case "lineNumber": return createLineNumberResolver();
            case "methodName": return createMethodNameResolver(context);
        }
        throw new IllegalArgumentException("unknown key: " + key);
    }

    private static EventResolver createClassNameResolver(final EventResolverContext context) {
        return (final LogEvent logEvent, final JsonGenerator jsonGenerator) -> {
            final StackTraceElement logEventSource = logEvent.getSource();
            if (logEventSource != null) {
                final String sourceClassName = logEventSource.getClassName();
                final boolean sourceClassNameExcluded =
                        context.isBlankFieldExclusionEnabled() &&
                                Strings.isBlank(sourceClassName);
                if (!sourceClassNameExcluded) {
                    jsonGenerator.writeString(sourceClassName);
                    return;
                }
            }
            jsonGenerator.writeNull();
        };
    }

    private static EventResolver createFileNameResolver(final EventResolverContext context) {
        return (final LogEvent logEvent, final JsonGenerator jsonGenerator) -> {
            final StackTraceElement logEventSource = logEvent.getSource();
            if (logEventSource != null) {
                final String sourceFileName = logEventSource.getFileName();
                final boolean sourceFileNameExcluded =
                        context.isBlankFieldExclusionEnabled() &&
                                Strings.isBlank(sourceFileName);
                if (!sourceFileNameExcluded) {
                    jsonGenerator.writeString(sourceFileName);
                    return;
                }
            }
            jsonGenerator.writeNull();
        };
    }

    private static EventResolver createLineNumberResolver() {
        return (final LogEvent logEvent, final JsonGenerator jsonGenerator) -> {
            final StackTraceElement logEventSource = logEvent.getSource();
            if (logEventSource == null) {
                jsonGenerator.writeNull();
            } else {
                final int sourceLineNumber = logEventSource.getLineNumber();
                jsonGenerator.writeNumber(sourceLineNumber);
            }
        };
    }

    private static EventResolver createMethodNameResolver(final EventResolverContext context) {
        return (final LogEvent logEvent, final JsonGenerator jsonGenerator) -> {
            final StackTraceElement logEventSource = logEvent.getSource();
            if (logEventSource != null) {
                final String sourceMethodName = logEventSource.getMethodName();
                final boolean sourceMethodNameExcluded =
                        context.isBlankFieldExclusionEnabled() &&
                                Strings.isBlank(sourceMethodName);
                if (!sourceMethodNameExcluded) {
                    jsonGenerator.writeString(sourceMethodName);
                    return;
                }
            }
            jsonGenerator.writeNull();
        };
    }

    static String getName() {
        return "source";
    }

    @Override
    public void resolve(
            final LogEvent logEvent,
            final JsonGenerator jsonGenerator)
            throws IOException {
        internalResolver.resolve(logEvent, jsonGenerator);
    }

}
