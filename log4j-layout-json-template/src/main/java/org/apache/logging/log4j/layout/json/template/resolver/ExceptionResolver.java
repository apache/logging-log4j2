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
import org.apache.logging.log4j.util.Strings;

import java.io.IOException;

class ExceptionResolver implements EventResolver {

    private static final ExceptionInternalResolverFactory INTERNAL_RESOLVER_FACTORY =
            new ExceptionInternalResolverFactory() {

                @Override
                EventResolver createClassNameResolver() {
                    return (final LogEvent logEvent, final JsonGenerator jsonGenerator) -> {
                        final Throwable exception = logEvent.getThrown();
                        if (exception == null) {
                            jsonGenerator.writeNull();
                        } else {
                            String exceptionClassName = exception.getClass().getCanonicalName();
                            jsonGenerator.writeString(exceptionClassName);
                        }
                    };
                }

                @Override
                EventResolver createMessageResolver(final EventResolverContext context) {
                    return (final LogEvent logEvent, final JsonGenerator jsonGenerator) -> {
                        final Throwable exception = logEvent.getThrown();
                        if (exception != null) {
                            String exceptionMessage = exception.getMessage();
                            boolean exceptionMessageExcluded =
                                    context.isBlankFieldExclusionEnabled() &&
                                            Strings.isBlank(exceptionMessage);
                            if (!exceptionMessageExcluded) {
                                jsonGenerator.writeString(exceptionMessage);
                                return;
                            }
                        }
                        jsonGenerator.writeNull();
                    };
                }

                @Override
                EventResolver createStackTraceTextResolver(final EventResolverContext context) {
                    StackTraceTextResolver stackTraceTextResolver =
                            new StackTraceTextResolver(context);
                    return (final LogEvent logEvent, final JsonGenerator jsonGenerator) -> {
                        final Throwable exception = logEvent.getThrown();
                        if (exception == null) {
                            jsonGenerator.writeNull();
                        } else {
                            stackTraceTextResolver.resolve(exception, jsonGenerator);
                        }
                    };
                }

                @Override
                EventResolver createStackTraceObjectResolver(final EventResolverContext context) {
                    return (final LogEvent logEvent, final JsonGenerator jsonGenerator) -> {
                        final Throwable exception = logEvent.getThrown();
                        if (exception == null) {
                            jsonGenerator.writeNull();
                        } else {
                            context.getStackTraceObjectResolver().resolve(exception, jsonGenerator);
                        }
                    };
                }

            };

    private final EventResolver internalResolver;

    ExceptionResolver(final EventResolverContext context, final String key) {
        this.internalResolver = INTERNAL_RESOLVER_FACTORY.createInternalResolver(context, key);
    }

    static String getName() {
        return "exception";
    }

    @Override
    public void resolve(
            final LogEvent logEvent,
            final JsonGenerator jsonGenerator)
            throws IOException {
        internalResolver.resolve(logEvent, jsonGenerator);
    }

}
