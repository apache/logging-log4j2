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
import org.apache.logging.log4j.message.Message;

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
final class MessageParameterResolver implements EventResolver {

    private final boolean stringified;

    private final int index;

    MessageParameterResolver(final TemplateResolverConfig config) {
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

        // Short-circuit if there are no parameters.
        final Object[] parameters = logEvent.getMessage().getParameters();
        if (parameters.length == 0) {
            jsonWriter.writeNull();
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

}
