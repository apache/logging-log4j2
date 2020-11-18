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
package org.apache.logging.log4j.layout.template.json.resolver;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.util.Throwables;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayout;
import org.apache.logging.log4j.layout.template.json.util.JsonWriter;

/**
 * Exception root cause resolver.
 *
 * Note that this resolver is toggled by {@link
 * JsonTemplateLayout.Builder#setStackTraceEnabled(boolean)}.
 *
 * @see ExceptionInternalResolverFactory
 */
final class ExceptionRootCauseResolver implements EventResolver {

    private static final ExceptionInternalResolverFactory INTERNAL_RESOLVER_FACTORY =
            new ExceptionInternalResolverFactory() {

                @Override
                EventResolver createClassNameResolver() {
                    return (final LogEvent logEvent, final JsonWriter jsonWriter) -> {
                        final Throwable exception = logEvent.getThrown();
                        if (exception == null) {
                            jsonWriter.writeNull();
                        } else {
                            final Throwable rootCause = Throwables.getRootCause(exception);
                            final String rootCauseClassName = rootCause.getClass().getCanonicalName();
                            jsonWriter.writeString(rootCauseClassName);
                        }
                    };
                }

                @Override
                EventResolver createMessageResolver(final EventResolverContext context) {
                    return (final LogEvent logEvent, final JsonWriter jsonWriter) -> {
                        final Throwable exception = logEvent.getThrown();
                        if (exception == null) {
                            jsonWriter.writeNull();
                        } else {
                            final Throwable rootCause = Throwables.getRootCause(exception);
                            final String rootCauseMessage = rootCause.getMessage();
                            jsonWriter.writeString(rootCauseMessage);
                        }
                    };
                }

                @Override
                EventResolver createStackTraceStringResolver(final EventResolverContext context) {
                    final StackTraceStringResolver stackTraceStringResolver =
                            new StackTraceStringResolver(context);
                    return (final LogEvent logEvent, final JsonWriter jsonWriter) -> {
                        final Throwable exception = logEvent.getThrown();
                        if (exception == null) {
                            jsonWriter.writeNull();
                        } else {
                            final Throwable rootCause = Throwables.getRootCause(exception);
                            stackTraceStringResolver.resolve(rootCause, jsonWriter);
                        }
                    };
                }

                @Override
                EventResolver createStackTraceObjectResolver(EventResolverContext context) {
                    return (final LogEvent logEvent, final JsonWriter jsonWriter) -> {
                        final Throwable exception = logEvent.getThrown();
                        if (exception == null) {
                            jsonWriter.writeNull();
                        } else {
                            final Throwable rootCause = Throwables.getRootCause(exception);
                            context.getStackTraceObjectResolver().resolve(rootCause, jsonWriter);
                        }
                    };
                }

            };

    private final boolean stackTraceEnabled;

    private final EventResolver internalResolver;

    ExceptionRootCauseResolver(
            final EventResolverContext context,
            final TemplateResolverConfig config) {
        this.stackTraceEnabled = context.isStackTraceEnabled();
        this.internalResolver = INTERNAL_RESOLVER_FACTORY
                .createInternalResolver(context, config);
    }

    static String getName() {
        return "exceptionRootCause";
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
    public void resolve(
            final LogEvent logEvent,
            final JsonWriter jsonWriter) {
        internalResolver.resolve(logEvent, jsonWriter);
    }

}
