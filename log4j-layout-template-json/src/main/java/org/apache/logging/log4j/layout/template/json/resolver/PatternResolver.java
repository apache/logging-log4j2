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

import java.util.Optional;
import java.util.function.BiConsumer;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayout;
import org.apache.logging.log4j.layout.template.json.util.JsonWriter;
import org.apache.logging.log4j.util.Strings;

/**
 * Resolver delegating to {@link PatternLayout}.
 *
 * <h3>Configuration</h3>
 *
 * <pre>
 * config            = pattern , [ stackTraceEnabled ]
 * pattern           = "pattern" -> string
 * stackTraceEnabled = "stackTraceEnabled" -> boolean
 * </pre>
 *
 * Lookups (e.g., <tt>${java:version}</tt>, <tt>${env:USER}</tt>, <tt>${date:MM-dd-yyyy}</tt>) found in the <tt>pattern</tt> will be resolved.
 *
 * The default value of <tt>stackTraceEnabled</tt> is inherited from the parent
 * {@link JsonTemplateLayout}.
 *
 * <h3>Examples</h3>
 *
 * Resolve the string produced by <tt>%p %c{1.} [%t] %X{userId} %X %m%ex</tt>
 * pattern:
 *
 * <pre>
 * {
 *   "$resolver": "pattern",
 *   "pattern": "%p %c{1.} [%t] %X{userId} %X %m%ex"
 * }
 * </pre>
 */
public final class PatternResolver implements EventResolver {

    private final BiConsumer<StringBuilder, LogEvent> emitter;

    PatternResolver(final EventResolverContext context, final TemplateResolverConfig config) {
        final String pattern = readPattern(context.getSubstitutor(), config);
        final boolean stackTraceEnabled =
                Optional.ofNullable(config.getBoolean("stackTraceEnabled")).orElse(context.isStackTraceEnabled());
        final PatternLayout patternLayout = PatternLayout.newBuilder()
                .withConfiguration(context.getConfiguration())
                .withCharset(context.getCharset())
                .withPattern(pattern)
                .withAlwaysWriteExceptions(stackTraceEnabled)
                .build();
        this.emitter = (final StringBuilder stringBuilder, final LogEvent logEvent) ->
                patternLayout.serialize(logEvent, stringBuilder);
    }

    private static String readPattern(
            final EventResolverStringSubstitutor substitutor, final TemplateResolverConfig config) {
        final String pattern = config.getString("pattern");
        if (Strings.isBlank(pattern)) {
            throw new IllegalArgumentException("blank pattern: " + config);
        }
        final boolean substitutionNeeded = pattern.contains("${");
        return substitutionNeeded ? substitutor.replace(null, pattern) : pattern;
    }

    static String getName() {
        return "pattern";
    }

    @Override
    public void resolve(final LogEvent logEvent, final JsonWriter jsonWriter) {
        jsonWriter.writeString(emitter, logEvent);
    }
}
