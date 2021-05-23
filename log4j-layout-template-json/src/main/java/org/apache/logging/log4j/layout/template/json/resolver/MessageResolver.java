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
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MultiformatMessage;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.StringBuilderFormattable;

/**
 * {@link Message} resolver.
 *
 * <h3>Configuration</h3>
 *
 * <pre>
 * config      = [ stringified ] , [ fallbackKey ]
 * stringified = "stringified" -> boolean
 * fallbackKey = "fallbackKey" -> string
 * </pre>
 *
 * <h3>Examples</h3>
 *
 * Resolve the message into a string:
 *
 * <pre>
 * {
 *   "$resolver": "message",
 *   "stringified": true
 * }
 * </pre>
 *
 * Resolve the message such that if it is a {@link ObjectMessage} or {@link
 * MultiformatMessage} with JSON support, its emitted JSON type (string, list,
 * object, etc.) will be retained:
 *
 * <pre>
 * {
 *   "$resolver": "message"
 * }
 * </pre>
 *
 * Given the above configuration, a {@link SimpleMessage} will generate a
 * <tt>"sample log message"</tt>, whereas a {@link MapMessage} will generate a
 * <tt>{"action": "login", "sessionId": "87asd97a"}</tt>. Certain indexed log
 * storage systems (e.g., <a
 * href="https://www.elastic.co/elasticsearch/">Elasticsearch</a>) will not
 * allow both values to coexist due to type mismatch: one is a <tt>string</tt>
 * while the other is an <tt>object</tt>. Here one can use a
 * <tt>fallbackKey</tt> to work around the problem:
 *
 * <pre>
 * {
 *   "$resolver": "message",
 *   "fallbackKey": "formattedMessage"
 * }
 * </pre>
 *
 * Using this configuration, a {@link SimpleMessage} will generate a
 * <tt>{"formattedMessage": "sample log message"}</tt> and a {@link MapMessage}
 * will generate a <tt>{"action": "login", "sessionId": "87asd97a"}</tt>. Note
 * that both emitted JSONs are of type <tt>object</tt> and have no
 * type-conflicting fields.
 */
public final class MessageResolver implements EventResolver {

    private static final String[] FORMATS = { "JSON" };

    private final EventResolver internalResolver;

    MessageResolver(final TemplateResolverConfig config) {
        this.internalResolver = createInternalResolver(config);
    }

    static String getName() {
        return "message";
    }

    private static EventResolver createInternalResolver(
            final TemplateResolverConfig config) {
        final boolean stringified = config.getBoolean("stringified", false);
        final String fallbackKey = config.getString("fallbackKey");
        if (stringified && fallbackKey != null) {
            throw new IllegalArgumentException(
                    "fallbackKey is not allowed when stringified is enable: " + config);
        }
        return stringified
                ? createStringResolver(fallbackKey)
                : createObjectResolver(fallbackKey);
    }

    @Override
    public void resolve(
            final LogEvent logEvent,
            final JsonWriter jsonWriter) {
        internalResolver.resolve(logEvent, jsonWriter);
    }

    private static EventResolver createStringResolver(final String fallbackKey) {
        return (final LogEvent logEvent, final JsonWriter jsonWriter) ->
                resolveString(fallbackKey, logEvent, jsonWriter);
    }

    private static void resolveString(
            final String fallbackKey,
            final LogEvent logEvent,
            final JsonWriter jsonWriter) {
        final Message message = logEvent.getMessage();
        resolveString(fallbackKey, message, jsonWriter);
    }

    private static void resolveString(
            final String fallbackKey,
            final Message message,
            final JsonWriter jsonWriter) {
        if (fallbackKey != null) {
            jsonWriter.writeObjectStart();
            jsonWriter.writeObjectKey(fallbackKey);
        }
        if (message instanceof StringBuilderFormattable) {
            final StringBuilderFormattable formattable =
                    (StringBuilderFormattable) message;
            jsonWriter.writeString(formattable);
        } else {
            final String formattedMessage = message.getFormattedMessage();
            jsonWriter.writeString(formattedMessage);
        }
        if (fallbackKey != null) {
            jsonWriter.writeObjectEnd();
        }
    }

    private static EventResolver createObjectResolver(final String fallbackKey) {
        return (final LogEvent logEvent, final JsonWriter jsonWriter) -> {

            // Skip custom serializers for SimpleMessage.
            final Message message = logEvent.getMessage();
            final boolean simple = message instanceof SimpleMessage;
            if (!simple) {

                // Try MultiformatMessage serializer.
                if (writeMultiformatMessage(jsonWriter, message)) {
                    return;
                }

                // Try ObjectMessage serializer.
                if (writeObjectMessage(jsonWriter, message)) {
                    return;
                }

            }

            // Fallback to plain String serializer.
            resolveString(fallbackKey, logEvent, jsonWriter);

        };
    }

    private static boolean writeMultiformatMessage(
            final JsonWriter jsonWriter,
            final Message message) {

        // Check type.
        if (!(message instanceof MultiformatMessage)) {
            return false;
        }
        final MultiformatMessage multiformatMessage = (MultiformatMessage) message;

        // Check formatter's JSON support.
        boolean jsonSupported = false;
        final String[] formats = multiformatMessage.getFormats();
        for (final String format : formats) {
            if (FORMATS[0].equalsIgnoreCase(format)) {
                jsonSupported = true;
                break;
            }
        }
        if (!jsonSupported) {
            return false;
        }

        // Write the formatted JSON.
        final String messageJson = multiformatMessage.getFormattedMessage(FORMATS);
        jsonWriter.writeRawString(messageJson);
        return true;

    }

    private static boolean writeObjectMessage(
            final JsonWriter jsonWriter,
            final Message message) {

        // Check type.
        if (!(message instanceof ObjectMessage)) {
            return false;
        }

        // Serialize object.
        final ObjectMessage objectMessage = (ObjectMessage) message;
        final Object object = objectMessage.getParameter();
        jsonWriter.writeValue(object);
        return true;

    }

}
