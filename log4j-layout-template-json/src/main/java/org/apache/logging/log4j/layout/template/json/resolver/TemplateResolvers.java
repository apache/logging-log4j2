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

import org.apache.logging.log4j.layout.template.json.util.JsonReader;
import org.apache.logging.log4j.layout.template.json.util.JsonWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Main class for compiling {@link TemplateResolver}s from a template.
 */
public final class TemplateResolvers {

    private TemplateResolvers() {}

    private static final String RESOLVER_FIELD_NAME = "$resolver";

    private static abstract class UnresolvableTemplateResolver
            implements TemplateResolver<Object> {

        @Override
        public final boolean isResolvable() {
            return false;
        }

        @Override
        public final boolean isResolvable(Object value) {
            return false;
        }

    }

    private static final TemplateResolver<?> EMPTY_ARRAY_RESOLVER =
            new UnresolvableTemplateResolver() {
                @Override
                public void resolve(final Object value, final JsonWriter jsonWriter) {
                    jsonWriter.writeArrayStart();
                    jsonWriter.writeArrayEnd();
                }
            };

    private static final TemplateResolver<?> EMPTY_OBJECT_RESOLVER =
            new UnresolvableTemplateResolver() {
                @Override
                public void resolve(final Object value, final JsonWriter jsonWriter) {
                    jsonWriter.writeObjectStart();
                    jsonWriter.writeObjectEnd();
                }
            };

    private static final TemplateResolver<?> NULL_RESOLVER =
            new UnresolvableTemplateResolver() {
                @Override
                public void resolve(final Object value, final JsonWriter jsonWriter) {
                    jsonWriter.writeNull();
                }
            };

    public static <V, C extends TemplateResolverContext<V, C>> TemplateResolver<V> ofTemplate(
            final C context,
            final String template) {

        // Check arguments.
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(template, "template");

        // Read the template.
        Object node;
        try {
            node = JsonReader.read(template);
        } catch (final Exception error) {
            final String message = String.format("failed parsing template (template=%s)", template);
            throw new RuntimeException(message, error);
        }

        // Perform contextual interception.
        final List<? extends TemplateResolverInterceptor<V, C>> interceptors =
                context.getResolverInterceptors();
        // noinspection ForLoopReplaceableByForEach
        for (int interceptorIndex = 0;
             interceptorIndex < interceptors.size();
             interceptorIndex++) {
            final TemplateResolverInterceptor<V, C> interceptor =
                    interceptors.get(interceptorIndex);
            node = interceptor.processTemplateBeforeResolverInjection(context, node);
        }

        // Resolve the template.
        return ofObject(context, node);

    }

    static <V, C extends TemplateResolverContext<V, C>> TemplateResolver<V> ofObject(
            final C context,
            final Object object) {
        if (object == null) {
            @SuppressWarnings("unchecked")
            final TemplateResolver<V> nullResolver = (TemplateResolver<V>) NULL_RESOLVER;
            return nullResolver;
        } else if (object instanceof List) {
            @SuppressWarnings("unchecked")
            final List<Object> list = (List<Object>) object;
            return ofList(context, list);
        } else if (object instanceof Map) {
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) object;
            return ofMap(context, map);
        } else if (object instanceof String) {
            final String string = (String) object;
            return ofString(context, string);
        } else if (object instanceof Number) {
            final Number number = (Number) object;
            return ofNumber(number);
        } else if (object instanceof Boolean) {
            final boolean value = (boolean) object;
            return ofBoolean(value);
        } else {
            final String message = String.format(
                    "invalid JSON node type (class=%s)",
                    object.getClass().getName());
            throw new IllegalArgumentException(message);
        }
    }

    private static <V, C extends TemplateResolverContext<V, C>> TemplateResolver<V> ofList(
            final C context,
            final List<Object> list) {

        // Create resolver for each children.
        final List<TemplateResolver<V>> itemResolvers = list
                .stream()
                .map(item -> {
                    final TemplateResolver<V> itemResolver = ofObject(context, item);
                    if (itemResolver.isFlattening()) {
                        throw new IllegalArgumentException(
                                "flattening resolvers are not allowed in lists");
                    }
                    return itemResolver;
                })
                .collect(Collectors.toList());

        // Short-circuit if the array is empty.
        if (itemResolvers.isEmpty()) {
            @SuppressWarnings("unchecked")
            final TemplateResolver<V> emptyArrayResolver =
                    (TemplateResolver<V>) EMPTY_ARRAY_RESOLVER;
            return emptyArrayResolver;
        }

        // Create a parent resolver collecting each child resolver execution.
        return (final V value, final JsonWriter jsonWriter) -> {
            jsonWriter.writeArrayStart();
            for (int itemResolverIndex = 0;
                 itemResolverIndex < itemResolvers.size();
                 itemResolverIndex++) {
                if (itemResolverIndex > 0) {
                    jsonWriter.writeSeparator();
                }
                final TemplateResolver<V> itemResolver = itemResolvers.get(itemResolverIndex);
                itemResolver.resolve(value, jsonWriter);
            }
            jsonWriter.writeArrayEnd();
        };

    }

    private static <V, C extends TemplateResolverContext<V, C>> TemplateResolver<V> ofMap(
            final C context,
            final Map<String, Object> map) {

        // Check if this is a resolver request.
        if (map.containsKey(RESOLVER_FIELD_NAME)) {
            return ofResolver(context, map);
        }

        // Create resolver for each object field.
        final List<String> fieldNames = new ArrayList<>();
        final List<TemplateResolver<V>> fieldResolvers = new ArrayList<>();
        map.forEach((fieldName, fieldValue) -> {
            final TemplateResolver<V> fieldResolver = ofObject(context, fieldValue);
            final boolean resolvable = fieldResolver.isResolvable();
            if (resolvable) {
                fieldNames.add(fieldName);
                fieldResolvers.add(fieldResolver);
            }
        });

        // Short-circuit if the object is empty.
        final int fieldCount = fieldNames.size();
        if (fieldCount == 0) {
            @SuppressWarnings("unchecked")
            final TemplateResolver<V> emptyObjectResolver =
                    (TemplateResolver<V>) EMPTY_OBJECT_RESOLVER;
            return emptyObjectResolver;
        }

        // Prepare field names to avoid escape and truncation costs at runtime.
        final List<String> fieldPrefixes = fieldNames
                .stream()
                .map(fieldName -> {
                    try (JsonWriter jsonWriter = context.getJsonWriter()) {
                        jsonWriter.writeString(fieldName);
                        jsonWriter.getStringBuilder().append(':');
                        return jsonWriter.getStringBuilder().toString();
                    }
                })
                .collect(Collectors.toList());

        return new TemplateResolver<V>() {

            @Override
            public boolean isResolvable() {
                // We have already excluded unresolvable ones while collecting
                // the resolvers. Hence it is safe to return true here.
                return true;
            }

            /**
             * The parent resolver checking if each child is resolvable given
             * the passed {@code value}.
             *
             * This is an optimization to skip the rendering of a parent if all
             * its children are not resolvable given the passed {@code value}.
             */
            @Override
            public boolean isResolvable(final V value) {
                for (int fieldIndex = 0; fieldIndex < fieldCount; fieldIndex++) {
                    final TemplateResolver<V> fieldResolver = fieldResolvers.get(fieldIndex);
                    final boolean resolvable = fieldResolver.isResolvable(value);
                    if (resolvable) {
                        return true;
                    }
                }
                return false;
            }

            /**
             * The parent resolver combining all child resolver executions.
              */
            @Override
            public void resolve(final V value, final JsonWriter jsonWriter) {
                final StringBuilder jsonWriterStringBuilder = jsonWriter.getStringBuilder();
                jsonWriter.writeObjectStart();
                for (int resolvedFieldCount = 0, fieldIndex = 0; fieldIndex < fieldCount; fieldIndex++) {
                    final TemplateResolver<V> fieldResolver = fieldResolvers.get(fieldIndex);
                    final boolean resolvable = fieldResolver.isResolvable(value);
                    if (!resolvable) {
                        continue;
                    }
                    final boolean succeedingEntry = resolvedFieldCount > 0;
                    final boolean flattening = fieldResolver.isFlattening();
                    if (flattening) {
                        final int initLength = jsonWriterStringBuilder.length();
                        fieldResolver.resolve(value, jsonWriter, succeedingEntry);
                        final boolean resolved = jsonWriterStringBuilder.length() > initLength;
                        if (resolved) {
                            resolvedFieldCount++;
                        }
                    } else {
                        if (succeedingEntry) {
                            jsonWriter.writeSeparator();
                        }
                        final String fieldPrefix = fieldPrefixes.get(fieldIndex);
                        jsonWriter.writeRawString(fieldPrefix);
                        fieldResolver.resolve(value, jsonWriter, succeedingEntry);
                        resolvedFieldCount++;
                    }
                }
                jsonWriter.writeObjectEnd();
            }

        };

    }

    private static <V, C extends TemplateResolverContext<V, C>> TemplateResolver<V> ofResolver(
            final C context,
            final Map<String, Object> configMap) {

        // Check arguments.
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(configMap, "configMap");

        // Extract the resolver name.
        final Object resolverNameObject = configMap.get(RESOLVER_FIELD_NAME);
        if (!(resolverNameObject instanceof String)) {
            throw new IllegalArgumentException(
                    "invalid resolver name: " + resolverNameObject);
        }
        final String resolverName = (String) resolverNameObject;

        // Retrieve the resolver.
        final TemplateResolverFactory<V, C> resolverFactory =
                context.getResolverFactoryByName().get(resolverName);
        if (resolverFactory == null) {
            throw new IllegalArgumentException("unknown resolver: " + resolverName);
        }
        final TemplateResolverConfig resolverConfig = new TemplateResolverConfig(configMap);
        return resolverFactory.create(context, resolverConfig);

    }

    private static <V, C extends TemplateResolverContext<V, C>> TemplateResolver<V> ofString(
            final C context,
            final String fieldValue) {

        // Check if substitution is needed.
        final boolean substitutionNeeded = fieldValue.contains("${");
        final JsonWriter contextJsonWriter = context.getJsonWriter();
        if (substitutionNeeded) {
            final TemplateResolverStringSubstitutor<V> substitutor = context.getSubstitutor();

            // If the substitutor is stable, we can get the replacement right
            // away and avoid runtime substitution.
            if (substitutor.isStable()) {
                final String replacedText = substitutor.replace(null, fieldValue);
                if (replacedText == null) {
                    @SuppressWarnings("unchecked")
                    final TemplateResolver<V> resolver =
                            (TemplateResolver<V>) NULL_RESOLVER;
                    return resolver;
                } else {
                    // Prepare the escaped replacement first.
                    final String escapedReplacedText =
                            contextJsonWriter.use(() ->
                                    contextJsonWriter.writeString(replacedText));
                    // Create a resolver dedicated to the escaped replacement.
                    return (final V value, final JsonWriter jsonWriter) ->
                            jsonWriter.writeRawString(escapedReplacedText);
                }
            }

            // Otherwise, the unstable substitutor needs to be invoked always at
            // runtime.
            else {
                return (final V value, final JsonWriter jsonWriter) -> {
                    final String replacedText = substitutor.replace(value, fieldValue);
                    jsonWriter.writeString(replacedText);
                };
            }

        }

        // Write the field value as is.
        else {
            final String escapedFieldValue =
                    contextJsonWriter.use(() ->
                            contextJsonWriter.writeString(fieldValue));
            return (final V value, final JsonWriter jsonWriter) ->
                    jsonWriter.writeRawString(escapedFieldValue);
        }

    }

    private static <V> TemplateResolver<V> ofNumber(final Number number) {
        final String numberString = String.valueOf(number);
        return (final V ignored, final JsonWriter jsonWriter) ->
                jsonWriter.writeRawString(numberString);
    }

    private static <V> TemplateResolver<V> ofBoolean(final boolean value) {
        return (final V ignored, final JsonWriter jsonWriter) ->
                jsonWriter.writeBoolean(value);
    }

}
