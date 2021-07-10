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
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link ReadOnlyStringMap} resolver.
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
 * multiAccess   = [ pattern ] , [ replacement ] , [ flatten ] , [ stringified ]
 * pattern       = "pattern" -> string
 * replacement   = "replacement" -> string
 * flatten       = "flatten" -> ( boolean | flattenConfig )
 * flattenConfig = [ flattenPrefix ]
 * flattenPrefix = "prefix" -> string
 * </pre>
 *
 * Note that <tt>singleAccess</tt> resolves a single field, whilst
 * <tt>multiAccess</tt> resolves a multitude of fields. If <tt>flatten</tt>
 * is provided, <tt>multiAccess</tt> merges the fields with the parent,
 * otherwise creates a new JSON object containing the values.
 * <p>
 * Enabling <tt>stringified</tt> flag converts each value to its string
 * representation.
 * <p>
 * Regex provided in the <tt>pattern</tt> is used to match against the keys.
 * If provided, <tt>replacement</tt> will be used to replace the matched keys.
 * These two are effectively equivalent to
 * <tt>Pattern.compile(pattern).matcher(key).matches()</tt> and
 * <tt>Pattern.compile(pattern).matcher(key).replaceAll(replacement)</tt> calls.
 *
 * <h3>Garbage Footprint</h3>
 *
 * <tt>stringified</tt> allocates a new <tt>String</tt> for values that are not
 * of type <tt>String</tt>.
 * <p>
 * <tt>pattern</tt> and <tt>replacement</tt> incur pattern matcher allocation
 * costs.
 * <p>
 * Writing certain non-primitive values (e.g., <tt>BigDecimal</tt>,
 * <tt>Set</tt>, etc.) to JSON generates garbage, though most (e.g.,
 * <tt>int</tt>, <tt>long</tt>, <tt>String</tt>, <tt>List</tt>,
 * <tt>boolean[]</tt>, etc.) don't.
 *
 * <h3>Examples</h3>
 *
 * <tt>"$resolver"</tt> is left out in the following examples, since it is to be
 * defined by the actual resolver, e.g., {@link MapResolver},
 * {@link ThreadContextDataResolver}.
 * <p>
 * Resolve the value of the field keyed with <tt>user:role</tt>:
 *
 * <pre>
 * {
 *   "$resolver": "…",
 *   "key": "user:role"
 * }
 * </pre>
 *
 * Resolve the string representation of the <tt>user:rank</tt> field value:
 *
 * <pre>
 * {
 *   "$resolver": "…",
 *   "key": "user:rank",
 *   "stringified": true
 * }
 * </pre>
 *
 * Resolve all fields into an object:
 *
 * <pre>
 * {
 *   "$resolver": "…"
 * }
 * </pre>
 *
 * Resolve all fields into an object such that values are converted to
 * string:
 *
 * <pre>
 * {
 *   "$resolver": "…",
 *   "stringified": true
 * }
 * </pre>
 *
 * Resolve all fields whose keys match with the <tt>user:(role|rank)</tt> regex
 * into an object:
 *
 * <pre>
 * {
 *   "$resolver": "…",
 *   "pattern": "user:(role|rank)"
 * }
 * </pre>
 *
 * Resolve all fields whose keys match with the <tt>user:(role|rank)</tt> regex
 * into an object after removing the <tt>user:</tt> prefix in the key:
 *
 * <pre>
 * {
 *   "$resolver": "…",
 *   "pattern": "user:(role|rank)",
 *   "replacement": "$1"
 * }
 * </pre>
 *
 * Merge all fields whose keys are matching with the
 * <tt>user:(role|rank)</tt> regex into the parent:
 *
 * <pre>
 * {
 *   "$resolver": "…",
 *   "flatten": true,
 *   "pattern": "user:(role|rank)"
 * }
 * </pre>
 *
 * After converting the corresponding field values to string, merge all fields
 * to parent such that keys are prefixed with <tt>_</tt>:
 *
 * <pre>
 * {
 *   "$resolver": "…",
 *   "stringified": true,
 *   "flatten": {
 *     "prefix": "_"
 *   }
 * }
 * </pre>
 *
 * @see MapResolver
 * @see ThreadContextDataResolver
 */
class ReadOnlyStringMapResolver implements EventResolver {

    private final EventResolver internalResolver;

    ReadOnlyStringMapResolver(
            final EventResolverContext context,
            final TemplateResolverConfig config,
            final Function<LogEvent, ReadOnlyStringMap> mapAccessor) {
        this.internalResolver = createResolver(context, config, mapAccessor);
    }

    private static EventResolver createResolver(
            final EventResolverContext context,
            final TemplateResolverConfig config,
            final Function<LogEvent, ReadOnlyStringMap> mapAccessor) {
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
        final String prefix = config.getString(new String[] {"flatten", "prefix"});
        final String key = config.getString("key");
        if (key != null && flatten) {
            throw new IllegalArgumentException(
                    "key and flatten options cannot be combined: " + config);
        }
        final String pattern = config.getString("pattern");
        if (pattern != null && key != null) {
            throw new IllegalArgumentException(
                    "pattern and key options cannot be combined: " + config);
        }
        final String replacement = config.getString("replacement");
        if (pattern == null && replacement != null) {
            throw new IllegalArgumentException(
                    "replacement cannot be provided without a pattern: " + config);
        }
        final boolean stringified = config.getBoolean("stringified", false);
        if (key != null) {
            return createKeyResolver(key, stringified, mapAccessor);
        } else {
            final RecyclerFactory recyclerFactory = context.getRecyclerFactory();
            return createResolver(
                    recyclerFactory,
                    flatten,
                    prefix,
                    pattern,
                    replacement,
                    stringified,
                    mapAccessor);
        }
    }

    private static EventResolver createKeyResolver(
            final String key,
            final boolean stringified,
            final Function<LogEvent, ReadOnlyStringMap> mapAccessor) {
        return new EventResolver() {

            @Override
            public boolean isResolvable(final LogEvent logEvent) {
                final ReadOnlyStringMap map = mapAccessor.apply(logEvent);
                return map != null && map.containsKey(key);
            }

            @Override
            public void resolve(final LogEvent logEvent, final JsonWriter jsonWriter) {
                final ReadOnlyStringMap map = mapAccessor.apply(logEvent);
                final Object value = map == null ? null : map.getValue(key);
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
            final String replacement,
            final boolean stringified,
            final Function<LogEvent, ReadOnlyStringMap> mapAccessor) {

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
                    loopContext.replacement = replacement;
                    loopContext.stringified = stringified;
                    return loopContext;
                });

        // Create the resolver.
        return createResolver(flatten, loopContextRecycler, mapAccessor);

    }

    private static EventResolver createResolver(
            final boolean flatten,
            final Recycler<LoopContext> loopContextRecycler,
            final Function<LogEvent, ReadOnlyStringMap> mapAccessor) {
        return new EventResolver() {

            @Override
            public boolean isFlattening() {
                return flatten;
            }

            @Override
            public boolean isResolvable(final LogEvent logEvent) {
                final ReadOnlyStringMap map = mapAccessor.apply(logEvent);
                return map != null && !map.isEmpty();
            }

            @Override
            public void resolve(final LogEvent value, final JsonWriter jsonWriter) {
                resolve(value, jsonWriter, false);
            }

            @Override
            public void resolve(
                    final LogEvent logEvent,
                    final JsonWriter jsonWriter,
                    final boolean succeedingEntry) {

                // Retrieve the map.
                final ReadOnlyStringMap map = mapAccessor.apply(logEvent);
                if (map == null || map.isEmpty()) {
                    if (!flatten) {
                        jsonWriter.writeNull();
                    }
                    return;
                }

                // Resolve the map.
                if (!flatten) {
                    jsonWriter.writeObjectStart();
                }
                final LoopContext loopContext = loopContextRecycler.acquire();
                loopContext.jsonWriter = jsonWriter;
                loopContext.initJsonWriterStringBuilderLength = jsonWriter.getStringBuilder().length();
                loopContext.succeedingEntry = flatten && succeedingEntry;
                try {
                    map.forEach(LoopMethod.INSTANCE, loopContext);
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

        private String replacement;

        private boolean stringified;

        private JsonWriter jsonWriter;

        private int initJsonWriterStringBuilderLength;

        private boolean succeedingEntry;

    }

    private enum LoopMethod implements TriConsumer<String, Object, LoopContext> {

        INSTANCE;

        @Override
        public void accept(
                final String key,
                final Object value,
                final LoopContext loopContext) {
            final Matcher matcher = loopContext.pattern != null
                    ? loopContext.pattern.matcher(key)
                    : null;
            final boolean keyMatched = matcher == null || matcher.matches();
            if (keyMatched) {
                final String replacedKey =
                        matcher != null && loopContext.replacement != null
                                ? matcher.replaceAll(loopContext.replacement)
                                : key;
                final boolean succeedingEntry =
                        loopContext.succeedingEntry ||
                                loopContext.initJsonWriterStringBuilderLength <
                                        loopContext.jsonWriter.getStringBuilder().length();
                if (succeedingEntry) {
                    loopContext.jsonWriter.writeSeparator();
                }
                if (loopContext.prefix == null) {
                    loopContext.jsonWriter.writeObjectKey(replacedKey);
                } else {
                    loopContext.prefixedKey.setLength(loopContext.prefix.length());
                    loopContext.prefixedKey.append(replacedKey);
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

    @Override
    public boolean isFlattening() {
        return internalResolver.isFlattening();
    }

    @Override
    public boolean isResolvable(final LogEvent logEvent) {
        return internalResolver.isResolvable(logEvent);
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
