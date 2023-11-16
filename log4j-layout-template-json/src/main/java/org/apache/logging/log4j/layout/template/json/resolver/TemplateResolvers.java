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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.logging.log4j.layout.template.json.util.JsonReader;
import org.apache.logging.log4j.layout.template.json.util.JsonWriter;

/**
 * Main class for compiling {@link TemplateResolver}s from a template.
 */
public final class TemplateResolvers {

    private TemplateResolvers() {}

    private static final String RESOLVER_FIELD_NAME = "$resolver";

    private abstract static class UnresolvableTemplateResolver implements TemplateResolver<Object> {

        @Override
        public final boolean isResolvable() {
            return false;
        }

        @Override
        public final boolean isResolvable(final Object value) {
            return false;
        }
    }

    private static final TemplateResolver<?> EMPTY_ARRAY_RESOLVER = new EmptyArrayResolver();

    private static final class EmptyArrayResolver extends UnresolvableTemplateResolver {

        @Override
        public void resolve(final Object value, final JsonWriter jsonWriter) {
            jsonWriter.writeArrayStart();
            jsonWriter.writeArrayEnd();
        }
    }

    private static final TemplateResolver<?> EMPTY_OBJECT_RESOLVER = new EmptyObjectResolver();

    private static final class EmptyObjectResolver extends UnresolvableTemplateResolver {

        @Override
        public void resolve(final Object value, final JsonWriter jsonWriter) {
            jsonWriter.writeObjectStart();
            jsonWriter.writeObjectEnd();
        }
    }

    private static final TemplateResolver<?> NULL_RESOLVER = new NullResolver();

    private static final class NullResolver extends UnresolvableTemplateResolver {

        @Override
        public void resolve(final Object value, final JsonWriter jsonWriter) {
            jsonWriter.writeNull();
        }
    }

    public static <V, C extends TemplateResolverContext<V, C>> TemplateResolver<V> ofTemplate(
            final C context, final String template) {

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
        final List<? extends TemplateResolverInterceptor<V, C>> interceptors = context.getResolverInterceptors();
        // noinspection ForLoopReplaceableByForEach
        for (int interceptorIndex = 0; interceptorIndex < interceptors.size(); interceptorIndex++) {
            final TemplateResolverInterceptor<V, C> interceptor = interceptors.get(interceptorIndex);
            node = interceptor.processTemplateBeforeResolverInjection(context, node);
        }

        // Resolve the template.
        return ofObject(context, node);
    }

    static <V, C extends TemplateResolverContext<V, C>> TemplateResolver<V> ofObject(
            final C context, final Object object) {
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
                    "invalid JSON node type (class=%s)", object.getClass().getName());
            throw new IllegalArgumentException(message);
        }
    }

    private static <V, C extends TemplateResolverContext<V, C>> TemplateResolver<V> ofList(
            final C context, final List<Object> list) {

        // Create resolver for each child.
        final List<TemplateResolver<V>> itemResolvers = list.stream()
                .map(item -> {
                    final TemplateResolver<V> itemResolver = ofObject(context, item);
                    if (itemResolver.isFlattening()) {
                        throw new IllegalArgumentException("flattening resolvers are not allowed in lists");
                    }
                    return itemResolver;
                })
                .collect(Collectors.toList());

        // Short-circuit if the array is empty.
        if (itemResolvers.isEmpty()) {
            @SuppressWarnings("unchecked")
            final TemplateResolver<V> emptyArrayResolver = (TemplateResolver<V>) EMPTY_ARRAY_RESOLVER;
            return emptyArrayResolver;
        }

        // Create a parent resolver collecting each child resolver execution.
        return new ArrayResolver<>(itemResolvers);
    }

    private static final class ArrayResolver<V> implements TemplateResolver<V> {

        private final List<TemplateResolver<V>> itemResolvers;

        private ArrayResolver(final List<TemplateResolver<V>> itemResolvers) {
            this.itemResolvers = itemResolvers;
        }

        @Override
        public void resolve(final V value, final JsonWriter jsonWriter) {
            jsonWriter.writeArrayStart();
            for (int itemResolverIndex = 0; itemResolverIndex < itemResolvers.size(); itemResolverIndex++) {
                if (itemResolverIndex > 0) {
                    jsonWriter.writeSeparator();
                }
                final TemplateResolver<V> itemResolver = itemResolvers.get(itemResolverIndex);
                itemResolver.resolve(value, jsonWriter);
            }
            jsonWriter.writeArrayEnd();
        }
    }

    private static <V, C extends TemplateResolverContext<V, C>> TemplateResolver<V> ofMap(
            final C context, final Map<String, Object> map) {

        // Check if this is a resolver request.
        if (map.containsKey(RESOLVER_FIELD_NAME)) {
            return ofResolver(context, map);
        }

        // Collect field resolver contexts.
        final List<FieldResolverContext<V>> fieldResolverContexts = populateFieldResolverMethods(context, map);

        // Short-circuit if the object is empty.
        final int fieldCount = fieldResolverContexts.size();
        if (fieldCount == 0) {
            @SuppressWarnings("unchecked")
            final TemplateResolver<V> emptyObjectResolver = (TemplateResolver<V>) EMPTY_OBJECT_RESOLVER;
            return emptyObjectResolver;
        }

        // Create the resolver.
        return new MapResolver<>(fieldResolverContexts);
    }

    private static <V, C extends TemplateResolverContext<V, C>>
            List<FieldResolverContext<V>> populateFieldResolverMethods(final C context, final Map<String, Object> map) {

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

        // Prepare field names to avoid escape and truncation costs at runtime.
        final List<String> fieldPrefixes = fieldNames.stream()
                .map(fieldName -> {
                    try (final JsonWriter jsonWriter = context.getJsonWriter()) {
                        jsonWriter.writeString(fieldName);
                        jsonWriter.getStringBuilder().append(':');
                        return jsonWriter.getStringBuilder().toString();
                    }
                })
                .collect(Collectors.toList());

        // Collect field resolver contexts.
        final int fieldCount = fieldNames.size();
        return IntStream.range(0, fieldCount)
                .mapToObj(fieldIndex -> {
                    final TemplateResolver<V> fieldResolver = fieldResolvers.get(fieldIndex);
                    final FieldResolverMethod<V> fieldResolverMethod;
                    final boolean flattening = fieldResolver.isFlattening();
                    if (flattening) {
                        fieldResolverMethod = new FlatteningFieldResolverMethod<>(fieldResolver);
                    } else {
                        final String fieldPrefix = fieldPrefixes.get(fieldIndex);
                        fieldResolverMethod = new PrefixedFieldResolverMethod<>(fieldPrefix, fieldResolver);
                    }
                    return new FieldResolverContext<>(fieldResolver, fieldResolverMethod);
                })
                .collect(Collectors.toList());
    }

    private static final class FieldResolverContext<V> {

        private final TemplateResolver<V> resolver;

        private final FieldResolverMethod<V> resolverMethod;

        private FieldResolverContext(final TemplateResolver<V> resolver, final FieldResolverMethod<V> resolverMethod) {
            this.resolver = resolver;
            this.resolverMethod = resolverMethod;
        }
    }

    @FunctionalInterface
    private interface FieldResolverMethod<V> {

        boolean resolve(V value, JsonWriter jsonWriter, boolean succeedingEntry);
    }

    private static final class FlatteningFieldResolverMethod<V> implements FieldResolverMethod<V> {

        private final TemplateResolver<V> fieldResolver;

        private FlatteningFieldResolverMethod(final TemplateResolver<V> fieldResolver) {
            this.fieldResolver = fieldResolver;
        }

        @Override
        public boolean resolve(final V value, final JsonWriter jsonWriter, final boolean succeedingEntry) {
            final boolean resolvable = fieldResolver.isResolvable(value);
            if (!resolvable) {
                return false;
            }
            final StringBuilder jsonWriterStringBuilder = jsonWriter.getStringBuilder();
            final int initLength = jsonWriterStringBuilder.length();
            fieldResolver.resolve(value, jsonWriter, succeedingEntry);
            return jsonWriterStringBuilder.length() > initLength;
        }
    }

    private static final class PrefixedFieldResolverMethod<V> implements FieldResolverMethod<V> {

        private final String fieldPrefix;

        private final TemplateResolver<V> fieldResolver;

        private PrefixedFieldResolverMethod(final String fieldPrefix, final TemplateResolver<V> fieldResolver) {
            this.fieldPrefix = fieldPrefix;
            this.fieldResolver = fieldResolver;
        }

        @Override
        public boolean resolve(final V value, final JsonWriter jsonWriter, final boolean succeedingEntry) {
            final boolean resolvable = fieldResolver.isResolvable(value);
            if (!resolvable) {
                return false;
            }
            if (succeedingEntry) {
                jsonWriter.writeSeparator();
            }
            jsonWriter.writeRawString(fieldPrefix);
            fieldResolver.resolve(value, jsonWriter, succeedingEntry);
            return true;
        }
    }

    private static final class MapResolver<V> implements TemplateResolver<V> {

        private final List<FieldResolverContext<V>> fieldResolverContexts;

        private MapResolver(final List<FieldResolverContext<V>> fieldResolverContexts) {
            this.fieldResolverContexts = fieldResolverContexts;
        }

        @Override
        public boolean isResolvable() {
            // We have already excluded unresolvable ones while collecting
            // the resolvers; it is safe to return true here.
            return true;
        }

        /**
         * The parent resolver checking if each child is resolvable given
         * the passed {@code value}.
         *
         * This is an optimization to skip the rendering of a parent if all
         * its children are not resolvable for the given {@code value}.
         */
        @Override
        public boolean isResolvable(final V value) {
            final int fieldCount = fieldResolverContexts.size();
            // noinspection ForLoopReplaceableByForEach (avoid iterator instantiation)
            for (int fieldIndex = 0; fieldIndex < fieldCount; fieldIndex++) {
                final TemplateResolver<V> fieldResolver = fieldResolverContexts.get(fieldIndex).resolver;
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
            jsonWriter.writeObjectStart();
            final int fieldCount = fieldResolverContexts.size();
            for (int resolvedFieldCount = 0, fieldIndex = 0; fieldIndex < fieldCount; fieldIndex++) {
                final FieldResolverContext<V> fieldResolverContext = fieldResolverContexts.get(fieldIndex);
                final boolean resolvable = fieldResolverContext.resolver.isResolvable(value);
                if (!resolvable) {
                    continue;
                }
                final boolean succeedingEntry = resolvedFieldCount > 0;
                final boolean resolved =
                        fieldResolverContext.resolverMethod.resolve(value, jsonWriter, succeedingEntry);
                if (resolved) {
                    resolvedFieldCount++;
                }
            }
            jsonWriter.writeObjectEnd();
        }
    }

    private static <V, C extends TemplateResolverContext<V, C>> TemplateResolver<V> ofResolver(
            final C context, final Map<String, Object> configMap) {

        // Check arguments.
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(configMap, "configMap");

        // Extract the resolver name.
        final Object resolverNameObject = configMap.get(RESOLVER_FIELD_NAME);
        if (!(resolverNameObject instanceof String)) {
            throw new IllegalArgumentException("invalid resolver name: " + resolverNameObject);
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
            final C context, final String fieldValue) {

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
                    final TemplateResolver<V> resolver = (TemplateResolver<V>) NULL_RESOLVER;
                    return resolver;
                } else {
                    // Prepare the escaped replacement first.
                    final String escapedReplacedText =
                            contextJsonWriter.use(() -> contextJsonWriter.writeString(replacedText));
                    // Create a resolver dedicated to the escaped replacement.
                    return new RawStringResolver<>(escapedReplacedText);
                }
            }

            // Otherwise, the unstable substitutor needs to be invoked always at
            // runtime.
            else {
                return new SubstitutingStringResolver<>(substitutor, fieldValue);
            }

        }

        // Write the field value as is.
        else {
            final String escapedFieldValue = contextJsonWriter.use(() -> contextJsonWriter.writeString(fieldValue));
            return new RawStringResolver<>(escapedFieldValue);
        }
    }

    private static final class SubstitutingStringResolver<V> implements TemplateResolver<V> {

        private final TemplateResolverStringSubstitutor<V> substitutor;

        private final String string;

        private SubstitutingStringResolver(
                final TemplateResolverStringSubstitutor<V> substitutor, final String string) {
            this.substitutor = substitutor;
            this.string = string;
        }

        @Override
        public void resolve(final V value, final JsonWriter jsonWriter) {
            final String replacedString = substitutor.replace(value, string);
            jsonWriter.writeString(replacedString);
        }
    }

    private static final class RawStringResolver<V> implements TemplateResolver<V> {

        private final String rawString;

        private RawStringResolver(final String rawString) {
            this.rawString = rawString;
        }

        @Override
        public void resolve(final V ignored, final JsonWriter jsonWriter) {
            jsonWriter.writeRawString(rawString);
        }
    }

    private static <V> TemplateResolver<V> ofNumber(final Number number) {
        return new NumberResolver<>(number);
    }

    private static final class NumberResolver<V> implements TemplateResolver<V> {

        private final String numberString;

        private NumberResolver(final Number number) {
            this.numberString = String.valueOf(number);
        }

        @Override
        public void resolve(final V ignored, final JsonWriter jsonWriter) {
            jsonWriter.writeRawString(numberString);
        }
    }

    private static <V> TemplateResolver<V> ofBoolean(final boolean value) {
        return new BooleanResolver<>(value);
    }

    private static final class BooleanResolver<V> implements TemplateResolver<V> {

        private final boolean value;

        private BooleanResolver(final boolean value) {
            this.value = value;
        }

        @Override
        public void resolve(final V ignored, final JsonWriter jsonWriter) {
            jsonWriter.writeBoolean(value);
        }
    }
}
