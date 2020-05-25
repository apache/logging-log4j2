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

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.layout.json.template.util.JsonWriter;
import org.apache.logging.log4j.layout.json.template.util.Recycler;
import org.apache.logging.log4j.layout.json.template.util.StringParameterParser;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Add Mapped Diagnostic Context (MDC).
 */
final class ContextDataResolver implements EventResolver {

    private enum Param {;

        private static final String FLATTEN = "flatten";

        private static final String PATTERN = "pattern";

        private static final String KEY = "key";

        private static final String STRINGIFY = "stringify";

    }

    private static final Set<String> PARAMS =
            new LinkedHashSet<>(Arrays.asList(
                    Param.FLATTEN,
                    Param.PATTERN,
                    Param.KEY,
                    Param.STRINGIFY));

    private final EventResolver internalResolver;

    ContextDataResolver(final EventResolverContext context, final String spec) {
        this.internalResolver = createResolver(context, spec);
    }

    private static EventResolver createResolver(
            final EventResolverContext context,
            final String spec) {
        final Map<String, StringParameterParser.Value> params = StringParameterParser.parse(spec, PARAMS);
        final StringParameterParser.Value keyValue = params.get(Param.KEY);
        if (keyValue != null) {
            if (params.size() != 1) {
                throw new IllegalArgumentException(
                        "MDC key access doesn't take arguments: " + spec);
            }
            if (keyValue instanceof StringParameterParser.NullValue) {
                throw new IllegalArgumentException("missing MDC key: " + spec);
            }
            final String key = keyValue.toString();
            return createKeyResolver(key);
        } else {
            return createResolver(context, spec, params);
        }
    }

    private static EventResolver createKeyResolver(final String key) {
        return new EventResolver() {

            @Override
            public boolean isResolvable(final LogEvent logEvent) {
                final ReadOnlyStringMap contextData = logEvent.getContextData();
                return contextData != null && contextData.containsKey(key);
            }

            @Override
            public void resolve(final LogEvent logEvent, final JsonWriter jsonWriter) {
                final ReadOnlyStringMap contextData = logEvent.getContextData();
                final Object value = contextData == null ? null : contextData.getValue(key);
                jsonWriter.writeValue(value);
            }

        };
    }

    private static EventResolver createResolver(
            final EventResolverContext context,
            final String spec,
            final Map<String, StringParameterParser.Value> params) {

        // Read the flatten prefix.
        final StringParameterParser.Value flattenValue = params.get(Param.FLATTEN);
        final boolean flatten;
        final String prefix;
        if (flattenValue != null) {
            flatten = true;
            prefix = flattenValue.toString();
        } else {
            flatten = false;
            prefix = null;
        }

        // Read the pattern.
        final StringParameterParser.Value patternValue = params.get(Param.PATTERN);
        final Pattern pattern;
        if (patternValue == null) {
            pattern = null;
        } else if (patternValue instanceof StringParameterParser.NullValue) {
            throw new IllegalArgumentException("missing MDC pattern: " + spec);
        } else {
            pattern = Pattern.compile(patternValue.toString());
        }

        // Read the stringify flag.
        final StringParameterParser.Value stringifyValue = params.get(Param.STRINGIFY);
        final boolean stringify;
        if (stringifyValue == null) {
            stringify = false;
        } else if (!(stringifyValue instanceof StringParameterParser.NullValue)) {
            throw new IllegalArgumentException(
                    "MDC stringify directive doesn't take parameters: " + spec);
        } else {
            stringify = true;
        }

        // Create the recycler for the loop context.
        final Recycler<LoopContext> loopContextRecycler =
                context.getRecyclerFactory().create(() -> {
                    final LoopContext loopContext = new LoopContext();
                    if (prefix != null) {
                        loopContext.prefix = prefix;
                        loopContext.prefixedKey = new StringBuilder(prefix);
                    }
                    loopContext.pattern = pattern;
                    loopContext.stringify = stringify;
                    return loopContext;
                });

        // Create the resolver.
        return createResolver(flatten, loopContextRecycler);

    }

    private static EventResolver createResolver(
            final boolean flatten,
            final Recycler<LoopContext> loopContextRecycler) {
        return new EventResolver() {

            @Override
            public boolean isFlattening() {
                return flatten;
            }

            @Override
            public boolean isResolvable(final LogEvent logEvent) {
                final ReadOnlyStringMap contextData = logEvent.getContextData();
                return contextData != null && !contextData.isEmpty();
            }

            @Override
            public void resolve(final LogEvent value, final JsonWriter jsonWriter) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void resolve(
                    final LogEvent logEvent,
                    final JsonWriter jsonWriter,
                    final boolean succeedingEntry) {

                // Retrieve the context data.
                final ReadOnlyStringMap contextData = logEvent.getContextData();
                if (contextData == null || contextData.isEmpty()) {
                    if (!flatten) {
                        jsonWriter.writeNull();
                    }
                    return;
                }

                // Resolve the context data.
                if (!flatten) {
                    jsonWriter.writeObjectStart();
                }
                final LoopContext loopContext = loopContextRecycler.acquire();
                loopContext.jsonWriter = jsonWriter;
                loopContext.initJsonWriterStringBuilderLength = jsonWriter.getStringBuilder().length();
                loopContext.succeedingEntry = flatten && succeedingEntry;
                try {
                    contextData.forEach(LoopMethod.INSTANCE, loopContext);
                } finally {
                    loopContextRecycler.release(loopContext);
                }
                if (!flatten) {
                    jsonWriter.writeObjectEnd();
                }

            }

        };
    }

    private static final class LoopContext {

        private String prefix;

        private StringBuilder prefixedKey;

        private Pattern pattern;

        private boolean stringify;

        private JsonWriter jsonWriter;

        private int initJsonWriterStringBuilderLength;

        private boolean succeedingEntry;

    }

    private static final class LoopMethod implements TriConsumer<String, Object, LoopContext> {

        private static final LoopMethod INSTANCE = new LoopMethod();

        @Override
        public void accept(
                final String key,
                final Object value,
                final LoopContext loopContext) {
            final boolean keyMatched =
                    loopContext.pattern == null ||
                            loopContext.pattern.matcher(key).matches();
            if (keyMatched) {
                final boolean succeedingEntry =
                        loopContext.succeedingEntry ||
                                loopContext.initJsonWriterStringBuilderLength <
                                        loopContext.jsonWriter.getStringBuilder().length();
                if (succeedingEntry) {
                    loopContext.jsonWriter.writeSeparator();
                }
                if (loopContext.prefix == null) {
                    loopContext.jsonWriter.writeObjectKey(key);
                } else {
                    loopContext.prefixedKey.setLength(loopContext.prefix.length());
                    loopContext.prefixedKey.append(key);
                    loopContext.jsonWriter.writeObjectKey(loopContext.prefixedKey);
                }
                if (loopContext.stringify && !(value instanceof String)) {
                    final String valueString = String.valueOf(value);
                    loopContext.jsonWriter.writeString(valueString);
                } else {
                    loopContext.jsonWriter.writeValue(value);
                }
            }
        }

    }

    static String getName() {
        return "mdc";
    }

    @Override
    public boolean isFlattening() {
        return internalResolver.isFlattening();
    }

    @Override
    public boolean isResolvable(final LogEvent logEvent) {
        final ReadOnlyStringMap contextData = logEvent.getContextData();
        return contextData != null && !contextData.isEmpty();
    }

    @Override
    public void resolve(
            final LogEvent logEvent,
            final JsonWriter jsonWriter) {
        internalResolver.resolve(logEvent, jsonWriter);
    }

    @Override
    public void resolve(
            final LogEvent logEvent,
            final JsonWriter jsonWriter,
            final boolean succeedingEntry) {
        internalResolver.resolve(logEvent, jsonWriter, succeedingEntry);
    }

}
