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
import org.apache.logging.log4j.layout.template.json.util.JsonWriter;
import org.apache.logging.log4j.layout.template.json.util.Recycler;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ParameterConsumer;
import org.apache.logging.log4j.message.ParameterVisitable;

/**
 * {@link Message} parameter (i.e., {@link Message#getParameters()}) resolver.
 *
 * <h3>Configuration</h3>
 *
 * <pre>
 * config      = [ stringified ] , [ index ]
 * stringified = "stringified" -> boolean
 * index       = "index" -> number
 * </pre>
 *
 * <h3>Examples</h3>
 *
 * Resolve the message parameters into an array:
 *
 * <pre>
 * {
 *   "$resolver": "messageParameter"
 * }
 * </pre>
 *
 * Resolve the string representation of all message parameters into an array:
 *
 * <pre>
 * {
 *   "$resolver": "messageParameter",
 *   "stringified": true
 * }
 * </pre>
 *
 * Resolve the first message parameter:
 *
 * <pre>
 * {
 *   "$resolver": "messageParameter",
 *   "index": 0
 * }
 *
 * Resolve the string representation of the first message parameter:
 *
 * <pre>
 * {
 *   "$resolver": "messageParameter",
 *   "index": 0,
 *   "stringified": true
 * }
 * </pre>
 */
public final class MessageParameterResolver implements EventResolver {

    private final Recycler<ParameterConsumerState> parameterConsumerStateRecycler;

    private final boolean stringified;

    private final int index;

    MessageParameterResolver(
            final EventResolverContext context,
            final TemplateResolverConfig config) {
        this.parameterConsumerStateRecycler = context
                .getRecyclerFactory()
                .create(ParameterConsumerState::new);
        this.stringified = config.getBoolean("stringified", false);
        final Integer index = config.getInteger("index");
        if (index != null && index < 0) {
            throw new IllegalArgumentException("was expecting a positive index: " + config);
        }
        this.index = index == null ? -1 : index;
    }

    static String getName() {
        return "messageParameter";
    }

    @Override
    public void resolve(final LogEvent logEvent, final JsonWriter jsonWriter) {

        // If possible, perform a garbage-free resolution.
        final Message message = logEvent.getMessage();
        if (message instanceof ParameterVisitable) {
            final ParameterVisitable parameterVisitable = (ParameterVisitable) message;
            resolve(parameterVisitable, jsonWriter);
            return;
        }

        // Short-circuit if there are no parameters.
        final Object[] parameters = message.getParameters();
        if (parameters == null || parameters.length == 0 || index >= parameters.length) {
            if (index < 0) {
                jsonWriter.writeArrayStart();
                jsonWriter.writeArrayEnd();
            } else {
                jsonWriter.writeNull();
            }
            return;
        }

        // Resolve all parameters.
        if (index < 0) {
            jsonWriter.writeArrayStart();
            for (int i = 0; i < parameters.length; i++) {
                if (i > 0) {
                    jsonWriter.writeSeparator();
                }
                final Object parameter = parameters[i];
                if (stringified) {
                    final String stringifiedParameter = String.valueOf(parameter);
                    jsonWriter.writeString(stringifiedParameter);
                } else {
                    jsonWriter.writeValue(parameter);
                }
            }
            jsonWriter.writeArrayEnd();
        }

        // Resolve a single parameter.
        else {
            final Object parameter = parameters[index];
            if (stringified) {
                final String stringifiedParameter = String.valueOf(parameter);
                jsonWriter.writeString(stringifiedParameter);
            } else {
                jsonWriter.writeValue(parameter);
            }
        }

    }

    /**
     * Perform a garbage-free resolution via {@link ParameterVisitable} interface.
     */
    private void resolve(
            final ParameterVisitable parameterVisitable,
            final JsonWriter jsonWriter) {
        final ParameterConsumerState parameterConsumerState =
                parameterConsumerStateRecycler.acquire();
        try {
            final boolean arrayNeeded = index < 0;
            if (arrayNeeded) {
                jsonWriter.writeArrayStart();
            }
            final StringBuilder buf = jsonWriter.getStringBuilder();
            final int startIndex = buf.length();
            parameterConsumerState.resolver = this;
            parameterConsumerState.jsonWriter = jsonWriter;
            parameterVisitable.forEachParameter(
                    PARAMETER_CONSUMER, parameterConsumerState);
            if (arrayNeeded) {
                jsonWriter.writeArrayEnd();
            } else if (startIndex == buf.length()) {
                // Handle the case in which index was not present in the event.
                jsonWriter.writeNull();
            }
        } finally {
            parameterConsumerStateRecycler.release(parameterConsumerState);
        }
    }

    private static final class ParameterConsumerState {

        private MessageParameterResolver resolver;

        private JsonWriter jsonWriter;

        private ParameterConsumerState() {}

    }

    private static final ParameterConsumer<ParameterConsumerState> PARAMETER_CONSUMER =
            (final Object parameter, final int index, final ParameterConsumerState state) -> {

                // Write the separator, if needed.
                final boolean arrayNeeded = state.resolver.index < 0;
                if (arrayNeeded && index > 0) {
                    state.jsonWriter.writeSeparator();
                }

                // Write the value.
                if (arrayNeeded || state.resolver.index == index) {
                    if (state.resolver.stringified) {
                        final String stringifiedParameter = String.valueOf(parameter);
                        state.jsonWriter.writeString(stringifiedParameter);
                    } else {
                        state.jsonWriter.writeValue(parameter);
                    }
                }

            };

}
