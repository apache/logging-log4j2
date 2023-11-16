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

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.layout.template.json.util.JsonWriter;

/**
 * A {@link Marker} resolver.
 *
 * <h3>Configuration</h3>
 *
 * <pre>
 * config = "field" -> ( "name" | "parents" )
 * </pre>
 *
 * <h3>Examples</h3>
 *
 * Resolve the marker name:
 *
 * <pre>
 * {
 *   "$resolver": "marker",
 *   "field": "name"
 * }
 * </pre>
 *
 * Resolve the names of the marker's parents:
 *
 * <pre>
 * {
 *   "$resolver": "marker",
 *   "field": "parents"
 * }
 * </pre>
 */
public final class MarkerResolver implements EventResolver {

    private static final TemplateResolver<LogEvent> NAME_RESOLVER =
            (final LogEvent logEvent, final JsonWriter jsonWriter) -> {
                final Marker marker = logEvent.getMarker();
                if (marker == null) {
                    jsonWriter.writeNull();
                } else {
                    jsonWriter.writeString(marker.getName());
                }
            };

    private static final TemplateResolver<LogEvent> PARENTS_RESOLVER =
            (final LogEvent logEvent, final JsonWriter jsonWriter) -> {

                // Short-circuit if there are no parents
                final Marker marker = logEvent.getMarker();
                if (marker == null || !marker.hasParents()) {
                    jsonWriter.writeNull();
                    return;
                }

                // Write parents
                final Marker[] parents = marker.getParents();
                jsonWriter.writeArrayStart();
                for (int parentIndex = 0; parentIndex < parents.length; parentIndex++) {
                    if (parentIndex > 0) {
                        jsonWriter.writeSeparator();
                    }
                    final Marker parentMarker = parents[parentIndex];
                    jsonWriter.writeString(parentMarker.getName());
                }
                jsonWriter.writeArrayEnd();
            };

    private final TemplateResolver<LogEvent> internalResolver;

    MarkerResolver(final TemplateResolverConfig config) {
        this.internalResolver = createInternalResolver(config);
    }

    private TemplateResolver<LogEvent> createInternalResolver(final TemplateResolverConfig config) {
        final String fieldName = config.getString("field");

        if ("name".equals(fieldName)) {
            return NAME_RESOLVER;
        }

        if ("parents".equals(fieldName)) {
            return PARENTS_RESOLVER;
        }

        throw new IllegalArgumentException("unknown field: " + config);
    }

    static String getName() {
        return "marker";
    }

    @Override
    public boolean isResolvable(final LogEvent logEvent) {
        return logEvent.getMarker() != null;
    }

    @Override
    public void resolve(final LogEvent logEvent, final JsonWriter jsonWriter) {
        internalResolver.resolve(logEvent, jsonWriter);
    }
}
