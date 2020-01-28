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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.util.Strings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public enum TemplateResolvers {;

    private static final TemplateResolver<?> EMPTY_ARRAY_RESOLVER =
            (final Object ignored, final JsonGenerator jsonGenerator) -> {
                jsonGenerator.writeStartArray();
                jsonGenerator.writeEndArray();
            };

    private static final TemplateResolver<?> EMPTY_OBJECT_RESOLVER =
            (final Object ignored, final JsonGenerator jsonGenerator) -> {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeEndObject();
            };

    private static final TemplateResolver<?> NULL_NODE_RESOLVER =
            (final Object ignored, final JsonGenerator jsonGenerator) ->
                    jsonGenerator.writeNull();

    public static <V, C extends TemplateResolverContext<V, C>> TemplateResolver<V> ofTemplate(
            final C context,
            final String template) {

        // Read the template.
        ObjectNode node;
        try {
            node = context.getObjectMapper().readValue(template, ObjectNode.class);
        } catch (final IOException error) {
            final String message = String.format("failed parsing template (template=%s)", template);
            throw new RuntimeException(message, error);
        }

        // Append the additional fields.
        if (context instanceof EventResolverContext) {
            final EventResolverContext eventResolverContext = (EventResolverContext) context;
            final KeyValuePair[] additionalFields = eventResolverContext.getAdditionalFields();
            if (additionalFields != null) {
                for (final KeyValuePair additionalField : additionalFields) {
                    node.put(additionalField.getKey(), additionalField.getValue());
                }
            }
        }

        // Resolve the template.
        return ofNode(context, node);

    }

    private static <V, C extends TemplateResolverContext<V, C>> TemplateResolver<V> ofNode(
            final C context,
            final JsonNode node) {

        // Check for known types.
        final JsonNodeType nodeType = node.getNodeType();
        switch (nodeType) {
            case ARRAY: return ofArrayNode(context, node);
            case OBJECT: return ofObjectNode(context, node);
            case STRING: return ofStringNode(context, node);
        }

        // Create constant resolver for the JSON.
        return (final V ignored, final JsonGenerator jsonGenerator) ->
                jsonGenerator.writeTree(node);

    }

    private static <V, C extends TemplateResolverContext<V, C>> TemplateResolver<V> ofArrayNode(
            final C context,
            final JsonNode arrayNode) {

        // Create resolver for each children.
        final List<TemplateResolver<V>> itemResolvers = new ArrayList<>();
        for (int itemIndex = 0; itemIndex < arrayNode.size(); itemIndex++) {
            final JsonNode itemNode = arrayNode.get(itemIndex);
            final TemplateResolver<V> itemResolver = ofNode(context, itemNode);
            itemResolvers.add(itemResolver);
        }

        // Short-circuit if the array is empty.
        if (itemResolvers.isEmpty()) {
            @SuppressWarnings("unchecked")
            final TemplateResolver<V> emptyArrayResolver = (TemplateResolver<V>) EMPTY_ARRAY_RESOLVER;
            return emptyArrayResolver;
        }

        // Create a parent resolver collecting each child resolver execution.
        return (final V value, final JsonGenerator jsonGenerator) -> {
            jsonGenerator.writeStartArray();
            // noinspection ForLoopReplaceableByForEach (avoid iterator instantiation)
            for (int itemResolverIndex = 0;
                 itemResolverIndex < itemResolvers.size();
                 itemResolverIndex++) {
                final TemplateResolver<V> itemResolver = itemResolvers.get(itemResolverIndex);
                itemResolver.resolve(value, jsonGenerator);
            }
            jsonGenerator.writeEndArray();
        };

    }

    private static <V, C extends TemplateResolverContext<V, C>> TemplateResolver<V> ofObjectNode(
            final C context,
            final JsonNode srcNode) {

        // Create resolver for each object field.
        final List<String> fieldNames = new ArrayList<>();
        final List<TemplateResolver<V>> fieldResolvers = new ArrayList<>();
        final Iterator<Map.Entry<String, JsonNode>> srcNodeFieldIterator = srcNode.fields();
        while (srcNodeFieldIterator.hasNext()) {
            final Map.Entry<String, JsonNode> srcNodeField = srcNodeFieldIterator.next();
            final String fieldName = srcNodeField.getKey();
            final JsonNode fieldValue = srcNodeField.getValue();
            final TemplateResolver<V> fieldResolver = ofNode(context, fieldValue);
            fieldNames.add(fieldName);
            fieldResolvers.add(fieldResolver);
        }

        // Short-circuit if the object is empty.
        final int fieldCount = fieldNames.size();
        if (fieldCount == 0) {
            @SuppressWarnings("unchecked")
            final TemplateResolver<V> emptyObjectResolver =
                    (TemplateResolver<V>) EMPTY_OBJECT_RESOLVER;
            return emptyObjectResolver;
        }

        // Create a parent resolver collecting each object field resolver execution.
        return (final V value, final JsonGenerator jsonGenerator) -> {
            jsonGenerator.writeStartObject();
            for (int fieldIndex = 0; fieldIndex < fieldCount; fieldIndex++) {
                final String fieldName = fieldNames.get(fieldIndex);
                final TemplateResolver<V> fieldResolver = fieldResolvers.get(fieldIndex);
                jsonGenerator.writeFieldName(fieldName);
                fieldResolver.resolve(value, jsonGenerator);
            }
            jsonGenerator.writeEndObject();
        };

    }

    private static <V, C extends TemplateResolverContext<V, C>> TemplateResolver<V> ofStringNode(
            final C context,
            final JsonNode textNode) {

        // Short-circuit if content is blank and not allowed.
        final String fieldValue = textNode.asText();
        final boolean fieldValueExcluded =
                context.isBlankPropertyExclusionEnabled() &&
                        Strings.isEmpty(fieldValue);
        if (fieldValueExcluded) {
            @SuppressWarnings("unchecked")
            final TemplateResolver<V> nullNodeResolver = (TemplateResolver<V>) NULL_NODE_RESOLVER;
            return nullNodeResolver;
        }

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
        // this library misses certain resolvers.

        // Check if substitution needed at all. (Copied logic from
        // AbstractJacksonLayout.valueNeedsLookup() method.)
        final boolean substitutionNeeded = fieldValue.contains("${");
        if (substitutionNeeded) {
            if (EventResolverContext.class.isAssignableFrom(context.getContextClass())) {
                // Use Log4j substitutor with LogEvent.
                return (final V value, final JsonGenerator jsonGenerator) -> {
                    final LogEvent logEvent = (LogEvent) value;
                    final String replacedText = context.getSubstitutor().replace(logEvent, fieldValue);
                    final boolean replacedTextExcluded =
                            context.isBlankPropertyExclusionEnabled() &&
                                    Strings.isEmpty(replacedText);
                    if (replacedTextExcluded) {
                        jsonGenerator.writeNull();
                    } else {
                        jsonGenerator.writeString(replacedText);
                    }
                };
            } else {
                // Use standalone Log4j substitutor.
                return (final V value, final JsonGenerator jsonGenerator) -> {
                    final String replacedText = context.getSubstitutor().replace(null, fieldValue);
                    final boolean replacedTextExcluded =
                            context.isBlankPropertyExclusionEnabled() &&
                                    Strings.isEmpty(replacedText);
                    if (replacedTextExcluded) {
                        jsonGenerator.writeNull();
                    } else {
                        jsonGenerator.writeString(replacedText);
                    }
                };
            }
        } else {
            // Write the field value as is. (Blank value check has already been done at the top.)
            return (final V value, final JsonGenerator jsonGenerator) ->
                    jsonGenerator.writeString(fieldValue);
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

    private static class TemplateResolverRequest {

        private final String resolverName;

        private final String resolverKey;

        private TemplateResolverRequest(final String resolverName, final String resolverKey) {
            this.resolverName = resolverName;
            this.resolverKey = resolverKey;
        }

    }

}
