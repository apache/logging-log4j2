/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.layout.template.json.resolver;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.layout.template.json.util.JsonWriter;
import org.apache.logging.log4j.layout.template.json.util.Recycler;
import org.apache.logging.log4j.layout.template.json.util.RecyclerFactory;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Mapped Diagnostic Context (MDC), aka. Thread Context Data, resolver.
 *
 * <h3>Configuration</h3>
 *
 * <pre>
 * config        = singleAccess | multiAccess
 *
 * singleAccess  = key , [ stringified ]
 * key           = "key" -> string
 * stringified   = "stringified" -> boolean
 *
 * multiAccess   = [ pattern ] , [ flatten ] , [ stringified ]
 * pattern       = "pattern" -> string
 * flatten       = "flatten" -> ( boolean | flattenConfig )
 * flattenConfig = [ flattenPrefix ]
 * flattenPrefix = "prefix" -> string
 * </pre>
 *
 * Note that <tt>singleAccess</tt> resolves the MDC value as is, whilst
 * <tt>multiAccess</tt> resolves a multitude of MDC values. If <tt>flatten</tt>
 * is provided, <tt>multiAccess</tt> merges the values with the parent,
 * otherwise creates a new JSON object containing the values.
 *
 * <h3>Examples</h3>
 *
 * Resolve the <tt>userRole</tt> MDC value:
 *
 * <pre>
 * {
 *   "$resolver": "mdc",
 *   "key": "userRole"
 * }
 * </pre>
 *
 * Resolve the string representation of the <tt>userRank</tt> MDC value:
 *
 * <pre>
 * {
 *   "$resolver": "mdc",
 *   "key": "userRank",
 *   "stringified": true
 * }
 * </pre>
 *
 * Resolve all MDC entries into an object:
 *
 * <pre>
 * {
 *   "$resolver": "mdc"
 * }
 * </pre>
 *
 * Resolve all MDC entries into an object such that values are converted to
 * string:
 *
 * <pre>
 * {
 *   "$resolver": "mdc",
 *   "stringified": true
 * }
 * </pre>
 *
 * Merge all MDC entries whose keys are matching with the
 * <tt>user(Role|Rank)</tt> regex into the parent:
 *
 * <pre>
 * {
 *   "$resolver": "mdc",
 *   "flatten": true,
 *   "pattern": "user(Role|Rank)"
 * }
 * </pre>
 *
 * After converting the corresponding entries to string, merge all MDC entries
 * to parent such that keys are prefixed with <tt>_</tt>:
 *
 * <pre>
 * {
 *   "$resolver": "mdc",
 *   "stringified": true,
 *   "flatten": {
 *     "prefix": "_"
 *   }
 * }
 * </pre>
 */
final class ThreadContextDataResolver implements EventResolver {

    private final EventResolver internalResolver;

    ThreadContextDataResolver(
            final EventResolverContext context,
            final TemplateResolverConfig config) {
        this.internalResolver = createResolver(context, config);
    }

    private static EventResolver createResolver(
            final EventResolverContext context,
            final TemplateResolverConfig config) {
        final Object flattenObject = config.getObject("flatten");
        final boolean flatten;
        if (flattenObject == null) {
            flatten = false;
        } else if (flattenObject instanceof Boolean) {
            flatten = (boolean) flattenObject;
        } else if (flattenObject instanceof Map) {
            flatten = true;
        } else {
            throw new IllegalArgumentException("invalid flatten option: " + config);
        }
        final String key = config.getString("key");
        final String prefix = config.getString(new String[] {"flatten", "prefix"});
        final String pattern = config.getString("pattern");
        final boolean stringified = config.getBoolean("stringified", false);
        if (key != null) {
            if (flatten) {
                throw new IllegalArgumentException(
                        "both key and flatten options cannot be supplied: " + config);
            }
            return createKeyResolver(key, stringified);
        } else {
            final RecyclerFactory recyclerFactory = context.getRecyclerFactory();
            return createResolver(recyclerFactory, flatten, prefix, pattern, stringified);
        }
    }

    private static EventResolver createKeyResolver(
            final String key,
            final boolean stringified) {
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
                if (stringified) {
                    final String valueString = String.valueOf(value);
                    jsonWriter.writeString(valueString);
                } else {
                    jsonWriter.writeValue(value);
                }
            }

        };
    }

    private static EventResolver createResolver(
            final RecyclerFactory recyclerFactory,
            final boolean flatten,
            final String prefix,
            final String pattern,
            final boolean stringified) {

        // Compile the pattern.
        final Pattern compiledPattern =
                pattern == null
                        ? null
                        : Pattern.compile(pattern);

        // Create the recycler for the loop context.
        final Recycler<LoopContext> loopContextRecycler =
                recyclerFactory.create(() -> {
                    final LoopContext loopContext = new LoopContext();
                    if (prefix != null) {
                        loopContext.prefix = prefix;
                        loopContext.prefixedKey = new StringBuilder(prefix);
                    }
                    loopContext.pattern = compiledPattern;
                    loopContext.stringified = stringified;
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

        private boolean stringified;

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
                if (loopContext.stringified && !(value instanceof String)) {
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
