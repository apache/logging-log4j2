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

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.layout.json.template.util.JsonWriter;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MultiformatMessage;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.StringBuilderFormattable;

final class MessageResolver implements EventResolver {

    private static final String[] FORMATS = { "JSON" };

    private final String key;

    MessageResolver(final String key) {
        this.key = key;
    }

    static String getName() {
        return "message";
    }

    @Override
    public void resolve(
            final LogEvent logEvent,
            final JsonWriter jsonWriter) {
        final Message message = logEvent.getMessage();
        if (FORMATS[0].equalsIgnoreCase(key)) {
            resolveJson(message, jsonWriter);
        } else {
            resolveText(message, jsonWriter);
        }
    }

    private void resolveText(
            final Message message,
            final JsonWriter jsonWriter) {
        if (message instanceof StringBuilderFormattable) {
            final StringBuilderFormattable formattable =
                    (StringBuilderFormattable) message;
            jsonWriter.writeString(formattable);
        } else {
            final String formattedMessage = message.getFormattedMessage();
            jsonWriter.writeString(formattedMessage);
        }
    }

    private void resolveJson(
            final Message message,
            final JsonWriter jsonWriter) {

        // Try SimpleMessage serializer.
        if (writeSimpleMessage(jsonWriter, message)) {
            return;
        }

        // Try MultiformatMessage serializer.
        if (writeMultiformatMessage(jsonWriter, message)) {
            return;
        }

        // Try ObjectMessage serializer.
        if (writeObjectMessage(jsonWriter, message)) {
            return;
        }

        // Fallback to plain Object write.
        resolveText(message, jsonWriter);

    }

    private boolean writeSimpleMessage(
            final JsonWriter jsonWriter,
            final Message message) {

        // Check type.
        if (!(message instanceof SimpleMessage)) {
            return false;
        }
        final SimpleMessage simpleMessage = (SimpleMessage) message;

        // Write message.
        final String formattedMessage = simpleMessage.getFormattedMessage();
        jsonWriter.writeString(formattedMessage);
        return true;

    }

    private boolean writeMultiformatMessage(
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

        // Write the formatted JSON, if supported.
        if (jsonSupported) {
            final String messageJson = multiformatMessage.getFormattedMessage(FORMATS);
            jsonWriter.writeRawString(messageJson);
            return true;
        }

        // Fallback to the default message formatter.
        resolveText(message, jsonWriter);
        return true;

    }

    private boolean writeObjectMessage(
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
