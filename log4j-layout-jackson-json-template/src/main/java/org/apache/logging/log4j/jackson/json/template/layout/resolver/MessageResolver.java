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
package org.apache.logging.log4j.jackson.json.template.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.jackson.json.template.layout.util.JsonGenerators;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MultiformatMessage;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.Strings;
import org.apache.logging.log4j.util.TriConsumer;

import java.io.IOException;

final class MessageResolver implements EventResolver {

    private static final String NAME = "message";

    private static final String[] FORMATS = { "JSON" };

    private final EventResolverContext context;

    private final String key;

    MessageResolver(final EventResolverContext context, final String key) {
        this.context = context;
        this.key = key;
    }

    static String getName() {
        return NAME;
    }

    @Override
    public void resolve(
            final LogEvent logEvent,
            final JsonGenerator jsonGenerator)
            throws IOException {
        final Message message = logEvent.getMessage();
        if (FORMATS[0].equalsIgnoreCase(key)) {
            resolveJson(message, jsonGenerator);
        } else {
            resolveText(message, jsonGenerator);
        }
    }

    private void resolveText(
            final Message message,
            final JsonGenerator jsonGenerator)
            throws IOException {
        final String formattedMessage = resolveText(message);
        if (formattedMessage == null) {
            jsonGenerator.writeNull();
        } else {
            jsonGenerator.writeString(formattedMessage);
        }
    }

    private String resolveText(final Message message) {
        final String formattedMessage = message.getFormattedMessage();
        final boolean messageExcluded =
                context.isBlankPropertyExclusionEnabled() &&
                        Strings.isEmpty(formattedMessage);
        return messageExcluded ? null : formattedMessage;
    }

    private void resolveJson(
            final Message message,
            final JsonGenerator jsonGenerator)
            throws IOException {

        // Try SimpleMessage serializer.
        if (writeSimpleMessage(jsonGenerator, message)) {
            return;
        }

        // Try MultiformatMessage serializer.
        if (writeMultiformatMessage(jsonGenerator, message)) {
            return;
        }

        // Try ObjectMessage serializer.
        if (writeObjectMessage(jsonGenerator, message)) {
            return;
        }

        // Fallback to plain Object write.
        writeObject(message, jsonGenerator);

    }

    private boolean writeSimpleMessage(
            final JsonGenerator jsonGenerator,
            final Message message)
            throws IOException {

        // Check type.
        if (!(message instanceof SimpleMessage)) {
            return false;
        }
        final SimpleMessage simpleMessage = (SimpleMessage) message;

        // Write message.
        final String formattedMessage = simpleMessage.getFormattedMessage();
        final boolean messageExcluded =
                context.isBlankPropertyExclusionEnabled() &&
                        Strings.isEmpty(formattedMessage);
        if (messageExcluded) {
            jsonGenerator.writeNull();
        } else {
            jsonGenerator.writeString(formattedMessage);
        }
        return true;

    }

    private boolean writeMultiformatMessage(
            final JsonGenerator jsonGenerator,
            final Message message)
            throws IOException {

        // Check type.
        if (!(message instanceof MultiformatMessage)) {
            return false;
        }
        final MultiformatMessage multiformatMessage = (MultiformatMessage) message;

        // As described in LOG4J2-2703, MapMessage#getFormattedMessage() is
        // incorrectly formatting Object's. Hence, we will temporarily work
        // around the problem by serializing it ourselves rather than using the
        // default provided formatter.

        // Override the provided MapMessage formatter.
        if (context.isMapMessageFormatterIgnored() && message instanceof MapMessage) {
            @SuppressWarnings("unchecked")
            final MapMessage<?, Object> mapMessage = (MapMessage) message;
            writeMapMessage(jsonGenerator, mapMessage);
            return true;
        }

        // Check formatter's JSON support.
        boolean jsonSupported = false;
        final String[] formats = multiformatMessage.getFormats();
        for (final String format : formats) {
            if (FORMATS[0].equalsIgnoreCase(format)) {
                jsonSupported = true;
                break;
            }
        }

        // Get the formatted message, if there is any.
        if (!jsonSupported) {
            writeObject(message, jsonGenerator);
            return true;
        }

        // Write the formatted JSON.
        final String messageJson = multiformatMessage.getFormattedMessage(FORMATS);
        final JsonNode jsonNode = readMessageJson(context, messageJson);
        final boolean nodeExcluded = isNodeExcluded(jsonNode);
        if (nodeExcluded) {
            jsonGenerator.writeNull();
        } else {
            jsonGenerator.writeTree(jsonNode);
        }
        return true;

    }

    private static void writeMapMessage(
            final JsonGenerator jsonGenerator,
            final MapMessage<?, Object> mapMessage)
            throws IOException {
        jsonGenerator.writeStartObject();
        mapMessage.forEach(MAP_MESSAGE_ENTRY_WRITER, jsonGenerator);
        jsonGenerator.writeEndObject();
    }

    private static TriConsumer<String, Object, JsonGenerator> MAP_MESSAGE_ENTRY_WRITER =
            (final String key, final Object value, final JsonGenerator jsonGenerator) -> {
                try {
                    jsonGenerator.writeFieldName(key);
                    JsonGenerators.writeObject(jsonGenerator, value);
                } catch (final IOException error) {
                    throw new RuntimeException("MapMessage entry serialization failure", error);
                }
            };

    private static JsonNode readMessageJson(
            final EventResolverContext context,
            final String messageJson) {
        try {
            return context.getObjectMapper().readTree(messageJson);
        } catch (final IOException error) {
            throw new RuntimeException("JSON message read failure", error);
        }
    }

    private void writeObject(
            final Message message,
            final JsonGenerator jsonGenerator)
            throws IOException {

        // Resolve text node.
        final String formattedMessage = resolveText(message);
        if (formattedMessage == null) {
            jsonGenerator.writeNull();
            return;
        }

        // Put textual representation of the message in an object.
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectField(NAME, formattedMessage);
        jsonGenerator.writeEndObject();

    }

    private boolean isNodeExcluded(final JsonNode jsonNode) {

        if (!context.isBlankPropertyExclusionEnabled()) {
            return false;
        }

        if (jsonNode.isNull()) {
            return true;
        }

        if (jsonNode.isTextual() && Strings.isEmpty(jsonNode.asText())) {
            return true;
        }

        // noinspection RedundantIfStatement
        if (jsonNode.isContainerNode() && jsonNode.size() == 0) {
            return true;
        }

        return false;

    }

    private boolean writeObjectMessage(
            final JsonGenerator jsonGenerator,
            final Message message)
            throws IOException {

        // Check type.
        if (!(message instanceof ObjectMessage)) {
            return false;
        }

        // Serialize object.
        final ObjectMessage objectMessage = (ObjectMessage) message;
        final Object object = objectMessage.getParameter();
        JsonGenerators.writeObject(jsonGenerator, object);
        return true;

    }

}
