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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayout;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayoutDefaults;
import org.apache.logging.log4j.layout.template.json.util.JsonWriter;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Exception resolver.
 * <p>
 * Note that this resolver is toggled by {@link
 * JsonTemplateLayout.Builder#setStackTraceEnabled(boolean) stackTraceEnabled}
 * layout configuration, which is by default populated from <tt>log4j.layout.jsonTemplate.stackTraceEnabled</tt>
 * system property.
 *
 * <h3>Configuration</h3>
 *
 * <pre>
 * config              = field , [ stringified ] , [ stackTrace ]
 * field               = "field" -> ( "className" | "message" | "stackTrace" )
 *
 * stackTrace          = "stackTrace" -> (
 *                         [ stringified ]
 *                       , [ elementTemplate ]
 *                       )
 *
 * stringified         = "stringified" -> ( boolean | truncation )
 * truncation          = "truncation" -> (
 *                         [ suffix ]
 *                       , [ pointMatcherStrings ]
 *                       , [ pointMatcherRegexes ]
 *                       )
 * suffix              = "suffix" -> string
 * pointMatcherStrings = "pointMatcherStrings" -> string[]
 * pointMatcherRegexes = "pointMatcherRegexes" -> string[]
 *
 * elementTemplate     = "elementTemplate" -> object
 * </pre>
 *
 * <tt>stringified</tt> is set to <tt>false</tt> by default.
 * <tt>stringified</tt> at the root level is <b>deprecated</b> in favor of
 * <tt>stackTrace.stringified</tt>, which has precedence if both are provided.
 * <p>
 * <tt>pointMatcherStrings</tt> and <tt>pointMatcherRegexes</tt> enable the
 * truncation of stringified stack traces after the given matching point. If
 * both parameters are provided, <tt>pointMatcherStrings</tt> will be checked
 * first.
 * <p>
 * If a stringified stack trace truncation takes place, it will be indicated
 * with a <tt>suffix</tt>, which by default is set to the configured
 * <tt>truncatedStringSuffix</tt> in the layout, unless explicitly provided.
 * Every truncation suffix is prefixed with a newline.
 * <p>
 * Stringified stack trace truncation operates in <tt>Caused by:</tt> and
 * <tt>Suppressed:</tt> label blocks. That is, matchers are executed against
 * each label in isolation.
 * <p>
 * <tt>elementTemplate</tt> is an object describing the template to be used
 * while resolving the {@link StackTraceElement} array. If <tt>stringified</tt>
 * is set to <tt>true</tt>, <tt>elementTemplate</tt> will be discarded. By
 * default, <tt>elementTemplate</tt> is set to <tt>null</tt> and rather
 * populated from the layout configuration. That is, the stack trace element
 * template can also be provided using {@link JsonTemplateLayout.Builder#setStackTraceElementTemplate(String) stackTraceElementTemplate}
 * and {@link JsonTemplateLayout.Builder#setStackTraceElementTemplateUri(String) setStackTraceElementTemplateUri}
 * layout configuration parameters. The template to be employed is determined
 * in the following order:
 * <ol>
 * <li><tt>elementTemplate</tt> provided in the resolver configuration
 * <li><tt>stackTraceElementTemplate</tt> parameter from layout configuration
 * (the default is populated from <tt>log4j.layout.jsonTemplate.stackTraceElementTemplate</tt>
 * system property)
 * <li><tt>stackTraceElementTemplateUri</tt> parameter from layout configuration
 * (the default is populated from <tt>log4j.layout.jsonTemplate.stackTraceElementTemplateUri</tt>
 * system property)
 * </ol>
 * <p>
 * See {@link StackTraceElementResolver}
 * for the list of available resolvers in a stack trace element template.
 *
 * <h3>Examples</h3>
 *
 * Resolve <tt>logEvent.getThrown().getClass().getCanonicalName()</tt>:
 *
 * <pre>
 *  {
 *   "$resolver": "exception",
 *   "field": "className"
 * }
 * </pre>
 *
 * Resolve the stack trace into a list of <tt>StackTraceElement</tt> objects:
 *
 * <pre>
 *  {
 *   "$resolver": "exception",
 *   "field": "stackTrace"
 * }
 * </pre>
 *
 * Resolve the stack trace into a string field:
 *
 * <pre>
 *  {
 *   "$resolver": "exception",
 *   "field": "stackTrace",
 *   "stackTrace": {
 *     "stringified": true
 *   }
 * }
 * </pre>
 *
 * Resolve the stack trace into a string field
 * such that the content will be truncated after the given point matcher:
 *
 * <pre>
 *  {
 *   "$resolver": "exception",
 *   "field": "stackTrace",
 *   "stackTrace": {
 *     "stringified": {
 *       "truncation": {
 *         "suffix": "... [truncated]",
 *         "pointMatcherStrings": ["at javax.servlet.http.HttpServlet.service"]
 *       }
 *     }
 *   }
 * }
 * </pre>
 *
 * Resolve the stack trace into an object described by the provided stack trace
 * element template:
 *
 * <pre>
 *  {
 *   "$resolver": "exception",
 *   "field": "stackTrace",
 *   "stackTrace": {
 *     "elementTemplate": {
 *       "class": {
 *         "$resolver": "stackTraceElement",
 *         "field": "className"
 *       },
 *       "method": {
 *         "$resolver": "stackTraceElement",
 *         "field": "methodName"
 *       },
 *       "file": {
 *         "$resolver": "stackTraceElement",
 *         "field": "fileName"
 *       },
 *       "line": {
 *         "$resolver": "stackTraceElement",
 *         "field": "lineNumber"
 *       }
 *     }
 *   }
 * }
 * </pre>
 *
 * @see JsonTemplateLayout.Builder#getTruncatedStringSuffix()
 * @see JsonTemplateLayoutDefaults#getTruncatedStringSuffix()
 * @see JsonTemplateLayout.Builder#getStackTraceElementTemplate()
 * @see JsonTemplateLayoutDefaults#getStackTraceElementTemplate()
 * @see JsonTemplateLayout.Builder#getStackTraceElementTemplateUri()
 * @see JsonTemplateLayoutDefaults#getStackTraceElementTemplateUri()
 * @see ExceptionRootCauseResolver
 * @see StackTraceElementResolver
 */
public class ExceptionResolver implements EventResolver {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static final EventResolver NULL_RESOLVER = (ignored, jsonGenerator) -> jsonGenerator.writeNull();

    private final boolean stackTraceEnabled;

    private final EventResolver internalResolver;

    ExceptionResolver(final EventResolverContext context, final TemplateResolverConfig config) {
        this.stackTraceEnabled = context.isStackTraceEnabled();
        this.internalResolver = createInternalResolver(context, config);
    }

    EventResolver createInternalResolver(final EventResolverContext context, final TemplateResolverConfig config) {
        final String fieldName = config.getString("field");
        if ("className".equals(fieldName)) {
            return createClassNameResolver();
        } else if ("message".equals(fieldName)) {
            return createMessageResolver();
        } else if ("stackTrace".equals(fieldName)) {
            return createStackTraceResolver(context, config);
        }
        throw new IllegalArgumentException("unknown field: " + config);
    }

    private EventResolver createClassNameResolver() {
        return (final LogEvent logEvent, final JsonWriter jsonWriter) -> {
            final Throwable exception = extractThrowable(logEvent);
            if (exception == null) {
                jsonWriter.writeNull();
            } else {
                final String exceptionClassName = exception.getClass().getCanonicalName();
                jsonWriter.writeString(exceptionClassName);
            }
        };
    }

    private EventResolver createMessageResolver() {
        return (final LogEvent logEvent, final JsonWriter jsonWriter) -> {
            final Throwable exception = extractThrowable(logEvent);
            if (exception == null) {
                jsonWriter.writeNull();
            } else {
                final String exceptionMessage = exception.getMessage();
                jsonWriter.writeString(exceptionMessage);
            }
        };
    }

    private EventResolver createStackTraceResolver(
            final EventResolverContext context, final TemplateResolverConfig config) {
        if (!context.isStackTraceEnabled()) {
            return NULL_RESOLVER;
        }
        final boolean stringified = isStackTraceStringified(config);
        return stringified
                ? createStackTraceStringResolver(context, config)
                : createStackTraceObjectResolver(context, config);
    }

    private static boolean isStackTraceStringified(final TemplateResolverConfig config) {
        final Boolean stringifiedOld = config.getBoolean("stringified");
        if (stringifiedOld != null) {
            LOGGER.warn("\"stringified\" flag at the root level of an exception "
                    + "[root cause] resolver is deprecated in favor of "
                    + "\"stackTrace.stringified\"");
        }
        final Object stringifiedNew = config.getObject(new String[] {"stackTrace", "stringified"});
        if (stringifiedOld == null && stringifiedNew == null) {
            return false;
        } else if (stringifiedNew == null) {
            return stringifiedOld;
        } else {
            return !(stringifiedNew instanceof Boolean) || (boolean) stringifiedNew;
        }
    }

    private EventResolver createStackTraceStringResolver(
            final EventResolverContext context, final TemplateResolverConfig config) {

        // Read the configuration.
        final String truncationSuffix = readTruncationSuffix(context, config);
        final List<String> truncationPointMatcherStrings = readTruncationPointMatcherStrings(config);
        final List<String> truncationPointMatcherRegexes = readTruncationPointMatcherRegexes(config);

        // Create the resolver.
        final StackTraceStringResolver resolver = new StackTraceStringResolver(
                context, truncationSuffix, truncationPointMatcherStrings, truncationPointMatcherRegexes);

        // Create the null-protected resolver.
        return (final LogEvent logEvent, final JsonWriter jsonWriter) -> {
            final Throwable exception = extractThrowable(logEvent);
            if (exception == null) {
                jsonWriter.writeNull();
            } else {
                resolver.resolve(exception, jsonWriter);
            }
        };
    }

    private static String readTruncationSuffix(
            final EventResolverContext context, final TemplateResolverConfig config) {
        final String suffix = config.getString(new String[] {"stackTrace", "stringified", "truncation", "suffix"});
        return suffix != null ? suffix : context.getTruncatedStringSuffix();
    }

    private static List<String> readTruncationPointMatcherStrings(final TemplateResolverConfig config) {
        List<String> strings = config.getList(
                new String[] {"stackTrace", "stringified", "truncation", "pointMatcherStrings"}, String.class);
        if (strings == null) {
            strings = Collections.emptyList();
        }
        return strings;
    }

    private static List<String> readTruncationPointMatcherRegexes(final TemplateResolverConfig config) {

        // Extract the regexes.
        List<String> regexes = config.getList(
                new String[] {"stackTrace", "stringified", "truncation", "pointMatcherRegexes"}, String.class);
        if (regexes == null) {
            regexes = Collections.emptyList();
        }

        // Check the regex syntax.
        for (int i = 0; i < regexes.size(); i++) {
            final String regex = regexes.get(i);
            try {
                Pattern.compile(regex);
            } catch (final PatternSyntaxException error) {
                final String message =
                        String.format("invalid truncation point matcher regex at index %d: %s", i, regex);
                throw new IllegalArgumentException(message, error);
            }
        }

        // Return the extracted regexes.
        return regexes;
    }

    private static final Map<String, StackTraceElementResolverFactory> STACK_TRACE_ELEMENT_RESOLVER_FACTORY_BY_NAME;

    static {
        final StackTraceElementResolverFactory stackTraceElementResolverFactory =
                StackTraceElementResolverFactory.getInstance();
        STACK_TRACE_ELEMENT_RESOLVER_FACTORY_BY_NAME =
                Collections.singletonMap(stackTraceElementResolverFactory.getName(), stackTraceElementResolverFactory);
    }

    private EventResolver createStackTraceObjectResolver(
            final EventResolverContext context, final TemplateResolverConfig config) {
        final TemplateResolver<StackTraceElement> stackTraceElementResolver =
                createStackTraceElementResolver(context, config);
        final StackTraceObjectResolver stackTraceResolver = new StackTraceObjectResolver(stackTraceElementResolver);
        return (final LogEvent logEvent, final JsonWriter jsonWriter) -> {
            final Throwable throwable = extractThrowable(logEvent);
            if (throwable == null) {
                jsonWriter.writeNull();
            } else {
                stackTraceResolver.resolve(throwable, jsonWriter);
            }
        };
    }

    private static TemplateResolver<StackTraceElement> createStackTraceElementResolver(
            final EventResolverContext context, final TemplateResolverConfig config) {
        final StackTraceElementResolverStringSubstitutor substitutor = new StackTraceElementResolverStringSubstitutor(
                context.getSubstitutor().getInternalSubstitutor());
        final StackTraceElementResolverContext stackTraceElementResolverContext =
                StackTraceElementResolverContext.newBuilder()
                        .setResolverFactoryByName(STACK_TRACE_ELEMENT_RESOLVER_FACTORY_BY_NAME)
                        .setSubstitutor(substitutor)
                        .setJsonWriter(context.getJsonWriter())
                        .build();
        final String stackTraceElementTemplate = findEffectiveStackTraceElementTemplate(context, config);
        return TemplateResolvers.ofTemplate(stackTraceElementResolverContext, stackTraceElementTemplate);
    }

    private static String findEffectiveStackTraceElementTemplate(
            final EventResolverContext context, final TemplateResolverConfig config) {

        // First, check the template configured in the resolver configuration.
        final Object stackTraceElementTemplateObject = config.getObject(new String[] {"stackTrace", "elementTemplate"});
        if (stackTraceElementTemplateObject != null) {
            final JsonWriter jsonWriter = context.getJsonWriter();
            return jsonWriter.use(() -> jsonWriter.writeValue(stackTraceElementTemplateObject));
        }

        // Otherwise, use the template provided in the context.
        return context.getStackTraceElementTemplate();
    }

    Throwable extractThrowable(final LogEvent logEvent) {
        return logEvent.getThrown();
    }

    static String getName() {
        return "exception";
    }

    @Override
    public boolean isResolvable() {
        return stackTraceEnabled;
    }

    @Override
    public boolean isResolvable(final LogEvent logEvent) {
        return stackTraceEnabled && logEvent.getThrown() != null;
    }

    @Override
    public void resolve(final LogEvent logEvent, final JsonWriter jsonWriter) {
        internalResolver.resolve(logEvent, jsonWriter);
    }
}
