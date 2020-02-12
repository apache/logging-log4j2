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
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.layout.json.template.util.JsonGenerators;
import org.apache.logging.log4j.util.IndexedStringMap;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Add Mapped Diagnostic Context (MDC).
 */
final class ContextDataResolver implements EventResolver {

    private final EventResolverContext context;

    private final String key;

    ContextDataResolver(final EventResolverContext context, final String key) {
        this.context = context;
        this.key = key;
    }

    static String getName() {
        return "mdc";
    }

    @Override
    public void resolve(
            final LogEvent logEvent,
            final JsonGenerator jsonGenerator)
            throws IOException {

        // Retrieve context data.
        final ReadOnlyStringMap contextData = logEvent.getContextData();
        if (contextData == null || contextData.isEmpty()) {
            jsonGenerator.writeNull();
            return;
        }

        // Check if key matches.
        if (key != null) {
            final Object value = contextData.getValue(key);
            final boolean valueExcluded = isValueExcluded(context, value);
            if (valueExcluded) {
                jsonGenerator.writeNull();
            } else {
                JsonGenerators.writeObject(jsonGenerator, value);
            }
            return;
        }

        // Otherwise return all context data matching the MDC key pattern.
        final Pattern keyPattern = context.getMdcKeyPattern();
        jsonGenerator.writeStartObject();
        if (contextData instanceof IndexedStringMap) {  // First, try access-by-id, which is GC free.
            resolveIndexedMap(jsonGenerator, (IndexedStringMap) contextData, keyPattern);
        } else {                                        // Otherwise, fallback to ReadOnlyStringMap#forEach().
            resolveGenericMap(jsonGenerator, contextData, keyPattern);
        }
        jsonGenerator.writeEndObject();

    }

    private void resolveIndexedMap(
            final JsonGenerator jsonGenerator,
            final IndexedStringMap contextData,
            final Pattern keyPattern) {
        for (int entryIndex = 0; entryIndex < contextData.size(); entryIndex++) {
            final String key = contextData.getKeyAt(entryIndex);
            final Object value = contextData.getValueAt(entryIndex);
            final boolean keyMatches = keyPattern == null || keyPattern.matcher(key).matches();
            resolveEntry(jsonGenerator, key, value, keyMatches);
        }
    }

    private void resolveGenericMap(
            final JsonGenerator jsonGenerator,
            final ReadOnlyStringMap contextData,
            final Pattern keyPattern) {
        contextData.forEach((final String key, final Object value) -> {
            final boolean keyMatched = keyPattern == null || keyPattern.matcher(key).matches();
            resolveEntry(jsonGenerator, key, value, keyMatched);
        });
    }

    private void resolveEntry(
            final JsonGenerator jsonGenerator,
            final String key,
            final Object value,
            final boolean keyMatched) {
        if (keyMatched) {
            final boolean valueExcluded = isValueExcluded(context, value);
            if (!valueExcluded) {
                try {
                    jsonGenerator.writeFieldName(key);
                    JsonGenerators.writeObject(jsonGenerator, value);
                } catch (IOException error) {
                    final String message = String.format(
                            "failed to append MDC field (key=%s, value=%s)", key, value);
                    throw new RuntimeException(message, error);
                }
            }
        }
    }

    private static boolean isValueExcluded(
            final EventResolverContext context,
            final Object value) {
        return context.isBlankFieldExclusionEnabled() &&
                (value == null || (value instanceof String && ((String) value).isEmpty()));
    }

}
