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
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.layout.json.template.util.JsonReader;
import org.apache.logging.log4j.layout.json.template.util.JsonWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public enum TemplateResolvers {;

    private static final TemplateResolver<?> EMPTY_ARRAY_RESOLVER =
            (final Object ignored, final JsonWriter jsonWriter) -> {
                jsonWriter.writeArrayStart();
                jsonWriter.writeArrayEnd();
            };

    private static final TemplateResolver<?> EMPTY_OBJECT_RESOLVER =
            (final Object ignored, final JsonWriter jsonWriter) -> {
                jsonWriter.writeObjectStart();
                jsonWriter.writeObjectEnd();
            };

    private static final TemplateResolver<?> NULL_RESOLVER =
            (final Object ignored, final JsonWriter jsonWriter) ->
                    jsonWriter.writeNull();

    public static <V, C extends TemplateResolverContext<V, C>> TemplateResolver<V> ofTemplate(
            final C context,
            final String template) {

        // Read the template.
        final Object node;
        try {
            node = JsonReader.read(template);
        } catch (final Exception error) {
            final String message = String.format("failed parsing template (template=%s)", template);
            throw new RuntimeException(message, error);
        }

        // Append the additional fields.
        if (context instanceof EventResolverContext) {
            final EventResolverContext eventResolverContext = (EventResolverContext) context;
            final KeyValuePair[] additionalFields = eventResolverContext.getAdditionalFields();
            if (additionalFields != null) {

                // Check that the root is an object node.
                final Map<String, Object> objectNode;
                try {
                    @SuppressWarnings("unchecked")
                    final Map<String, Object> map = (Map<String, Object>) node;
                    objectNode = map;
                } catch (final ClassCastException error) {
                    final String message = String.format(
                            "was expecting an object to merge additional fields (class=%s)",
                            node.getClass().getName());
                    throw new IllegalArgumentException(message);
                }

                // Merge additional fields.
                for (final KeyValuePair additionalField : additionalFields) {
                    objectNode.put(additionalField.getKey(), additionalField.getValue());
                }

            }
        }

        // Resolve the template.
        return ofObject(context, node);

    }

    private static <V, C extends TemplateResolverContext<V, C>> TemplateResolver<V> ofObject(
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
                .map(item -> ofObject(context, item))
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

        // Create resolver for each object field.
        final List<String> fieldNames = new ArrayList<>();
        final List<TemplateResolver<V>> fieldResolvers = new ArrayList<>();
        map.forEach((fieldName, fieldValue) -> {
            final TemplateResolver<V> fieldResolver = ofObject(context, fieldValue);
            fieldNames.add(fieldName);
            fieldResolvers.add(fieldResolver);
        });

        // Short-circuit if the object is empty.
        final int fieldCount = fieldNames.size();
        if (fieldCount == 0) {
            @SuppressWarnings("unchecked")
            final TemplateResolver<V> emptyObjectResolver =
                    (TemplateResolver<V>) EMPTY_OBJECT_RESOLVER;
            return emptyObjectResolver;
        }

        // Create a parent resolver collecting each object field resolver execution.
        return (final V value, final JsonWriter jsonWriter) -> {
            jsonWriter.writeObjectStart();
            for (int fieldIndex = 0; fieldIndex < fieldCount; fieldIndex++) {
                final String fieldName = fieldNames.get(fieldIndex);
                final TemplateResolver<V> fieldResolver = fieldResolvers.get(fieldIndex);
                if (fieldIndex > 0) {
                    jsonWriter.writeSeparator();
                }
                jsonWriter.writeObjectKey(fieldName);
                fieldResolver.resolve(value, jsonWriter);
            }
            jsonWriter.writeObjectEnd();
        };

    }

    private static <V, C extends TemplateResolverContext<V, C>> TemplateResolver<V> ofString(
            final C context,
            final String fieldValue) {

        // Try to resolve the directive as a ${json:xxx} parameter.
        final TemplateResolverRequest resolverRequest = readResolverRequest(fieldValue);
        if (resolverRequest != null) {
            final TemplateResolverFactory<V, C, ? extends TemplateResolver<V>> resolverFactory =
                    context.getResolverFactoryByName().get(resolverRequest.resolverName);
            if (resolverFactory != null) {
                return resolverFactory.create(context, resolverRequest.resolverKey);
            }
        }

        // The rest is the fallback template resolver that delegates every other
        // substitution to Log4j. This will be the case for every template value
        // that does not use directives of pattern ${json:xxx}. This
        // additionally serves as a mechanism to resolve values at runtime when
        // this layout misses certain resolvers.

        // Check if substitution needed at all. (Copied logic from
        // AbstractJacksonLayout.valueNeedsLookup() method.)
        final boolean substitutionNeeded = fieldValue.contains("${");
        if (substitutionNeeded) {
            if (EventResolverContext.class.isAssignableFrom(context.getContextClass())) {
                // Use Log4j substitutor with LogEvent.
                return (final V value, final JsonWriter jsonWriter) -> {
                    final LogEvent logEvent = (LogEvent) value;
                    final String replacedText = context.getSubstitutor().replace(logEvent, fieldValue);
                    if (replacedText == null) {
                        jsonWriter.writeNull();
                    } else {
                        jsonWriter.writeString(replacedText);
                    }
                };
            } else {
                // Use standalone Log4j substitutor.
                return (final V value, final JsonWriter jsonWriter) -> {
                    final String replacedText = context.getSubstitutor().replace(null, fieldValue);
                    if (replacedText == null) {
                        jsonWriter.writeNull();
                    } else {
                        jsonWriter.writeString(replacedText);
                    }
                };
            }
        } else {
            // Write the field value as is. (Blank value check has already been done at the top.)
            return (final V value, final JsonWriter jsonWriter) ->
                    jsonWriter.writeString(fieldValue);
        }

    }

    private static TemplateResolverRequest readResolverRequest(final String fieldValue) {

        // Bail-out if cannot spot the template signature.
        if (!fieldValue.startsWith("${json:") || !fieldValue.endsWith("}")) {
            return null;
        }

        // Try to read both resolver name and key.
        final int resolverNameStartIndex = 7;
        final int fieldNameSeparatorIndex = fieldValue.indexOf(':', resolverNameStartIndex);
        if (fieldNameSeparatorIndex < 0) {
            final int resolverNameEndIndex = fieldValue.length() - 1;
            final String resolverName = fieldValue.substring(resolverNameStartIndex, resolverNameEndIndex);
            return new TemplateResolverRequest(resolverName, null);
        } else {
            @SuppressWarnings("UnnecessaryLocalVariable")
            final int resolverNameEndIndex = fieldNameSeparatorIndex;
            final int resolverKeyStartIndex = fieldNameSeparatorIndex + 1;
            final int resolverKeyEndIndex = fieldValue.length() - 1;
            final String resolverName = fieldValue.substring(resolverNameStartIndex, resolverNameEndIndex);
            final String resolverKey = fieldValue.substring(resolverKeyStartIndex, resolverKeyEndIndex);
            return new TemplateResolverRequest(resolverName, resolverKey);
        }

    }

    private static final class TemplateResolverRequest {

        private final String resolverName;

        private final String resolverKey;

        private TemplateResolverRequest(final String resolverName, final String resolverKey) {
            this.resolverName = resolverName;
            this.resolverKey = resolverKey;
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
