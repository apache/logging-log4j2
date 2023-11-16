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

import java.util.Locale;
import java.util.function.Function;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayoutDefaults;
import org.apache.logging.log4j.layout.template.json.util.JsonReader;
import org.apache.logging.log4j.layout.template.json.util.JsonWriter;

/**
 * Converts the case of string values.
 *
 * <h3>Configuration</h3>
 *
 * <pre>
 * config                = case , input , [ locale ] , [ errorHandlingStrategy ]
 * input                 = JSON
 * case                  = "case" -> ( "upper" | "lower" )
 * locale                = "locale" -> (
 *                             language                                   |
 *                           ( language , "_" , country )                 |
 *                           ( language , "_" , country , "_" , variant )
 *                         )
 * errorHandlingStrategy = "errorHandlingStrategy" -> (
 *                           "fail"    |
 *                           "pass"    |
 *                           "replace"
 *                         )
 * replacement           = "replacement" -> JSON
 * </pre>
 *
 * {@code input} can be any available template value; e.g., a JSON literal,
 * a lookup string, an object pointing to another resolver.
 * <p>
 * Unless provided, {@code locale} points to the one returned by
 * {@link JsonTemplateLayoutDefaults#getLocale()}, which is configured by
 * {@code log4j.layout.jsonTemplate.locale} system property and by default set
 * to the default system locale.
 * <p>
 * {@code errorHandlingStrategy} determines the behavior when either the
 * {@code input} doesn't resolve to a string value or case conversion throws an
 * exception:
 * <ul>
 * <li>{@code fail} propagates the failure
 * <li>{@code pass} causes the resolved value to be passed as is
 * <li>{@code replace} suppresses the failure and replaces it with the
 * {@code replacement}, which is set to {@code null} by default
 * </ul>
 * {@code errorHandlingStrategy} is set to {@code replace} by default.
 * <p>
 * Most of the time JSON logs are persisted to a storage solution
 * (e.g., Elasticsearch) that keeps a statically-typed index on fields.
 * Hence, if a field is always expected to be of type string, using non-string
 * {@code replacement}s or {@code pass} in {@code errorHandlingStrategy} might
 * result in type incompatibility issues at the storage level.
 * <p>
 * Unless the {@code input} value is {@code pass}ed intact or {@code replace}d,
 * case conversion is not garbage-free.
 *
 * <h3>Examples</h3>
 *
 * Convert the resolved log level strings to upper-case:
 *
 * <pre>
 * {
 *   "$resolver": "caseConverter",
 *   "case": "upper",
 *   "input": {
 *     "$resolver": "level",
 *     "field": "name"
 *   }
 * }
 * </pre>
 *
 * Convert the resolved {@code USER} environment variable to lower-case using
 * {@code nl_NL} locale:
 *
 * <pre>
 * {
 *   "$resolver": "caseConverter",
 *   "case": "lower",
 *   "locale": "nl_NL",
 *   "input": "${env:USER}"
 * }
 * </pre>
 *
 * Convert the resolved {@code sessionId} thread context data (MDC) to
 * lower-case:
 *
 * <pre>
 * {
 *   "$resolver": "caseConverter",
 *   "case": "lower",
 *   "input": {
 *     "$resolver": "mdc",
 *     "key": "sessionId"
 *   }
 * }
 * </pre>
 *
 * Above, if {@code sessionId} MDC resolves to a, say, number, case conversion
 * will fail. Since {@code errorHandlingStrategy} is set to {@code replace} and
 * {@code replacement} is set to {@code null} by default, the resolved value
 * will be {@code null}. One can suppress this behavior and let the resolved
 * {@code sessionId} number be left as is:
 *
 * <pre>
 * {
 *   "$resolver": "caseConverter",
 *   "case": "lower",
 *   "input": {
 *     "$resolver": "mdc",
 *     "key": "sessionId"
 *   },
 *   "errorHandlingStrategy": "pass"
 * }
 * </pre>
 *
 * or replace it with a custom string:
 *
 * <pre>
 * {
 *   "$resolver": "caseConverter",
 *   "case": "lower",
 *   "input": {
 *     "$resolver": "mdc",
 *     "key": "sessionId"
 *   },
 *   "errorHandlingStrategy": "replace"
 *   "replacement": "unknown"
 * }
 * </pre>
 */
public final class CaseConverterResolver implements EventResolver {

    private final TemplateResolver<LogEvent> inputResolver;

    private final Function<String, String> converter;

    private final ErrorHandlingStrategy errorHandlingStrategy;

    private final TemplateResolver<LogEvent> replacementResolver;

    private enum ErrorHandlingStrategy {
        FAIL("fail"),

        PASS("pass"),

        REPLACE("replace");

        private final String name;

        ErrorHandlingStrategy(final String name) {
            this.name = name;
        }
    }

    CaseConverterResolver(final EventResolverContext context, final TemplateResolverConfig config) {
        this.inputResolver = createDelegate(context, config);
        this.converter = createConverter(config);
        this.errorHandlingStrategy = readErrorHandlingStrategy(config);
        this.replacementResolver = createReplacement(context, config);
    }

    private static TemplateResolver<LogEvent> createDelegate(
            final EventResolverContext context, final TemplateResolverConfig config) {
        final Object delegateObject = config.getObject("input");
        return TemplateResolvers.ofObject(context, delegateObject);
    }

    private static Function<String, String> createConverter(final TemplateResolverConfig config) {
        final Locale locale = config.getLocale("locale");
        final String _case = config.getString("case");
        if ("upper".equals(_case)) {
            return input -> input.toUpperCase(locale);
        } else if ("lower".equals(_case)) {
            return input -> input.toLowerCase(locale);
        } else {
            throw new IllegalArgumentException("invalid case: " + config);
        }
    }

    private static ErrorHandlingStrategy readErrorHandlingStrategy(final TemplateResolverConfig config) {
        final String strategyName = config.getString("errorHandlingStrategy");
        if (strategyName == null) {
            return ErrorHandlingStrategy.REPLACE;
        }
        for (ErrorHandlingStrategy strategy : ErrorHandlingStrategy.values()) {
            if (strategy.name.equals(strategyName)) {
                return strategy;
            }
        }
        throw new IllegalArgumentException("illegal error handling strategy: " + config);
    }

    private static TemplateResolver<LogEvent> createReplacement(
            final EventResolverContext context, final TemplateResolverConfig config) {
        final Object replacementObject = config.getObject("replacement");
        return TemplateResolvers.ofObject(context, replacementObject);
    }

    static String getName() {
        return "caseConverter";
    }

    @Override
    public boolean isFlattening() {
        return inputResolver.isFlattening();
    }

    @Override
    public boolean isResolvable() {
        return inputResolver.isResolvable();
    }

    @Override
    public boolean isResolvable(final LogEvent logEvent) {
        return inputResolver.isResolvable(logEvent);
    }

    @Override
    public void resolve(final LogEvent logEvent, final JsonWriter jsonWriter) {
        final int startIndex = jsonWriter.getStringBuilder().length();
        inputResolver.resolve(logEvent, jsonWriter);
        convertCase(logEvent, jsonWriter, startIndex);
    }

    @Override
    public void resolve(final LogEvent logEvent, final JsonWriter jsonWriter, final boolean succeedingEntry) {
        final int startIndex = jsonWriter.getStringBuilder().length();
        inputResolver.resolve(logEvent, jsonWriter, succeedingEntry);
        convertCase(logEvent, jsonWriter, startIndex);
    }

    private void convertCase(final LogEvent logEvent, final JsonWriter jsonWriter, final int startIndex) {

        // If the last emitted JSON token was a string, convert it.
        final StringBuilder jsonWriterStringBuilder = jsonWriter.getStringBuilder();
        final int endIndex = jsonWriterStringBuilder.length();
        final boolean stringTyped = (startIndex + 1) < endIndex
                && jsonWriterStringBuilder.charAt(startIndex) == '"'
                && jsonWriterStringBuilder.charAt(endIndex - 1) == '"';
        if (stringTyped) {
            final String json = jsonWriterStringBuilder.substring(startIndex, endIndex);
            convertCase(logEvent, jsonWriter, startIndex, json);
        }

        // Otherwise, see what we can do.
        else if (ErrorHandlingStrategy.FAIL.equals(errorHandlingStrategy)) {
            final String json = jsonWriterStringBuilder.substring(startIndex, endIndex);
            throw new RuntimeException("was expecting a string value, found: " + json);
        } else if (ErrorHandlingStrategy.PASS.equals(errorHandlingStrategy)) {
            // Do nothing.
        } else if (ErrorHandlingStrategy.REPLACE.equals(errorHandlingStrategy)) {
            jsonWriterStringBuilder.setLength(startIndex);
            replacementResolver.resolve(logEvent, jsonWriter);
        } else {
            throw new AssertionError("should not have reached here");
        }
    }

    private void convertCase(
            final LogEvent logEvent, final JsonWriter jsonWriter, final int startIndex, final String json) {
        final StringBuilder jsonWriterStringBuilder = jsonWriter.getStringBuilder();
        final String string = (String) JsonReader.read(json);
        final String convertedString;
        try {
            convertedString = converter.apply(string);
        } catch (final Exception error) {
            if (ErrorHandlingStrategy.FAIL.equals(errorHandlingStrategy)) {
                throw new RuntimeException("case conversion failure for string: " + string, error);
            } else if (ErrorHandlingStrategy.PASS.equals(errorHandlingStrategy)) {
                return;
            } else if (ErrorHandlingStrategy.REPLACE.equals(errorHandlingStrategy)) {
                jsonWriterStringBuilder.setLength(startIndex);
                replacementResolver.resolve(logEvent, jsonWriter);
                return;
            }
            throw new AssertionError("should not have reached here");
        }
        jsonWriterStringBuilder.setLength(startIndex);
        jsonWriter.writeString(convertedString);
    }
}
