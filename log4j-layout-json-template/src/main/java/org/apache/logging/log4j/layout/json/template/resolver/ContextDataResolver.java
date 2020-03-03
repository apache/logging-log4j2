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
import org.apache.logging.log4j.util.IndexedStringMap;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

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
            final JsonWriter jsonWriter) {

        // Retrieve context data.
        final ReadOnlyStringMap contextData = logEvent.getContextData();
        if (contextData == null || contextData.isEmpty()) {
            jsonWriter.writeNull();
            return;
        }

        // Check if key matches.
        if (key != null) {
            final Object value = contextData.getValue(key);
            jsonWriter.writeValue(value);
            return;
        }

        // Otherwise return all context data matching the MDC key pattern.
        final Pattern keyPattern = context.getMdcKeyPattern();
        jsonWriter.writeObjectStart();
        if (contextData instanceof IndexedStringMap) {  // First, try access-by-id, which is garbage-free.
            resolveIndexedMap(jsonWriter, (IndexedStringMap) contextData, keyPattern);
        } else {                                        // Otherwise, fallback to ReadOnlyStringMap#forEach().
            resolveGenericMap(jsonWriter, contextData, keyPattern);
        }
        jsonWriter.writeObjectEnd();

    }

    private void resolveIndexedMap(
            final JsonWriter jsonWriter,
            final IndexedStringMap contextData,
            final Pattern keyPattern) {
        final boolean[] firstEntry = {true};
        for (int entryIndex = 0; entryIndex < contextData.size(); entryIndex++) {
            final String key = contextData.getKeyAt(entryIndex);
            final Object value = contextData.getValueAt(entryIndex);
            final boolean keyMatched =
                    keyPattern == null || keyPattern.matcher(key).matches();
            resolveEntry(jsonWriter, firstEntry, key, value, keyMatched);
        }
    }

    private void resolveGenericMap(
            final JsonWriter jsonWriter,
            final ReadOnlyStringMap contextData,
            final Pattern keyPattern) {
        final boolean[] firstEntry = {true};
        contextData.forEach((final String key, final Object value) -> {
            final boolean keyMatched =
                    keyPattern == null || keyPattern.matcher(key).matches();
            resolveEntry(jsonWriter, firstEntry, key, value, keyMatched);
        });
    }

    private void resolveEntry(
            final JsonWriter jsonWriter,
            final boolean[] firstEntry,
            final String key,
            final Object value,
            final boolean keyMatched) {
        if (keyMatched) {
            if (firstEntry[0]) {
                firstEntry[0] = false;
            } else {
                jsonWriter.writeSeparator();
            }
            jsonWriter.writeObjectKey(key);
            jsonWriter.writeValue(value);
        }
    }

}
