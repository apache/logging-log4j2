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
package org.apache.logging.log4j.message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.IllegalFormatException;
import java.util.Locale;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Handles messages that consist of a format string conforming to java.text.MessageFormat.
 */
public class MessageFormatMessage implements Message {

    private static final Logger LOGGER = StatusLogger.getLogger();

    /**
     * @serial In version 2.1, due to a bug in the serialization format, the serialization format was changed along with
     * its {@code serialVersionUID} value.
     */
    private static final long serialVersionUID = 1L;

    private static final int HASHVAL = 31;

    private String messagePattern;
    private transient Object[] parameters;
    private String[] serializedParameters;
    private transient String formattedMessage;
    private transient Throwable throwable;
    private final Locale locale;

    /**
     * Constructs a message.
     *
     * @param locale the locale for this message format
     * @param messagePattern the pattern for this message format
     * @param parameters The objects to format
     * @since 2.6
     */
    public MessageFormatMessage(final Locale locale, final String messagePattern, final Object... parameters) {
        this.locale = locale;
        this.messagePattern = messagePattern;
        this.parameters = parameters;
        final int length = parameters == null ? 0 : parameters.length;
        if (length > 0 && parameters[length - 1] instanceof Throwable) {
            this.throwable = (Throwable) parameters[length - 1];
        }
    }

    /**
     * Constructs a message.
     *
     * @param messagePattern the pattern for this message format
     * @param parameters The objects to format
     */
    public MessageFormatMessage(final String messagePattern, final Object... parameters) {
        this(Locale.getDefault(Locale.Category.FORMAT), messagePattern, parameters);
    }

    /**
     * Returns the formatted message.
     * @return the formatted message.
     */
    @Override
    public String getFormattedMessage() {
        if (formattedMessage == null) {
            formattedMessage = formatMessage(messagePattern, parameters);
        }
        return formattedMessage;
    }

    /**
     * Returns the message pattern.
     * @return the message pattern.
     */
    @Override
    public String getFormat() {
        return messagePattern;
    }

    /**
     * Returns the message parameters.
     * @return the message parameters.
     */
    @Override
    public Object[] getParameters() {
        if (parameters != null) {
            return parameters;
        }
        return serializedParameters;
    }

    protected String formatMessage(final String msgPattern, final Object... args) {
        try {
            final MessageFormat temp = new MessageFormat(msgPattern, locale);
            return temp.format(args);
        } catch (final IllegalFormatException ife) {
            LOGGER.error("Unable to format msg: {}", msgPattern, ife);
            return msgPattern;
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MessageFormatMessage)) {
            return false;
        }

        final MessageFormatMessage that = (MessageFormatMessage) o;

        if (messagePattern != null ? !messagePattern.equals(that.messagePattern) : that.messagePattern != null) {
            return false;
        }
        return Arrays.equals(serializedParameters, that.serializedParameters);
    }

    @Override
    public int hashCode() {
        int result = messagePattern != null ? messagePattern.hashCode() : 0;
        result = HASHVAL * result + (serializedParameters != null ? Arrays.hashCode(serializedParameters) : 0);
        return result;
    }

    @Override
    public String toString() {
        return getFormattedMessage();
    }

    private void writeObject(final ObjectOutputStream out) throws IOException {
        getFormattedMessage();
        out.writeUTF(formattedMessage);
        out.writeUTF(messagePattern);
        final int length = parameters == null ? 0 : parameters.length;
        out.writeInt(length);
        serializedParameters = new String[length];
        if (length > 0) {
            for (int i = 0; i < length; i++) {
                serializedParameters[i] = String.valueOf(parameters[i]);
                out.writeUTF(serializedParameters[i]);
            }
        }
    }

    private void readObject(final ObjectInputStream in) throws IOException {
        parameters = null;
        throwable = null;
        formattedMessage = in.readUTF();
        messagePattern = in.readUTF();
        final int length = in.readInt();
        serializedParameters = new String[length];
        for (int i = 0; i < length; ++i) {
            serializedParameters[i] = in.readUTF();
        }
    }

    /**
     * Return the throwable passed to the Message.
     *
     * @return the Throwable.
     */
    @Override
    public Throwable getThrowable() {
        return throwable;
    }
}
