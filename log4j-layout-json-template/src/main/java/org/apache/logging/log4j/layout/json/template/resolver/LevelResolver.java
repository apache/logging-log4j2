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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.net.Severity;
import org.apache.logging.log4j.layout.json.template.util.JsonWriter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

final class LevelResolver implements EventResolver {

    private static Map<Level, String> SEVERITY_CODE_RESOLUTION_BY_LEVEL = Arrays
            .stream(Level.values())
            .collect(Collectors.toMap(
                    Function.identity(),
                    level -> "" + Severity.getSeverity(level).getCode()));

    private static final EventResolver SEVERITY_CODE_RESOLVER =
            (final LogEvent logEvent, final JsonWriter jsonWriter) -> {
                final String severityCodeResolution =
                        SEVERITY_CODE_RESOLUTION_BY_LEVEL.get(logEvent.getLevel());
                jsonWriter.writeRawString(severityCodeResolution);
            };

    private final EventResolver internalResolver;

    LevelResolver(final EventResolverContext context, final String key) {
        final JsonWriter jsonWriter = context.getJsonWriter();
        if (key == null) {
            internalResolver = createNameResolver(jsonWriter);
        } else if ("severity".equals(key)) {
            internalResolver = createSeverityNameResolver(jsonWriter);
        } else if ("severity:code".equals(key)) {
            internalResolver = SEVERITY_CODE_RESOLVER;
        } else {
            throw new IllegalArgumentException("unknown key: " + key);
        }
    }

    private static EventResolver createNameResolver(
            final JsonWriter contextJsonWriter) {
        final Map<Level, String> resolutionByLevel = Arrays
                .stream(Level.values())
                .collect(Collectors.toMap(
                        Function.identity(),
                        (final Level level) -> contextJsonWriter.use(() -> {
                            final String name = level.name();
                            contextJsonWriter.writeString(name);
                        })));
        return (final LogEvent logEvent, final JsonWriter jsonWriter) -> {
            final String resolution = resolutionByLevel.get(logEvent.getLevel());
            jsonWriter.writeRawString(resolution);
        };
    }

    private static EventResolver createSeverityNameResolver(
            final JsonWriter contextJsonWriter) {
        final Map<Level, String> resolutionByLevel = Arrays
                .stream(Level.values())
                .collect(Collectors.toMap(
                        Function.identity(),
                        (final Level level) -> contextJsonWriter.use(() -> {
                            final String severityName = Severity.getSeverity(level).name();
                            contextJsonWriter.writeString(severityName);
                        })));
        return (final LogEvent logEvent, final JsonWriter jsonWriter) -> {
            final String resolution = resolutionByLevel.get(logEvent.getLevel());
            jsonWriter.writeRawString(resolution);
        };
    }

    static String getName() {
        return "level";
    }

    @Override
    public void resolve(
            final LogEvent logEvent,
            final JsonWriter jsonWriter) {
        internalResolver.resolve(logEvent, jsonWriter);
    }

}
