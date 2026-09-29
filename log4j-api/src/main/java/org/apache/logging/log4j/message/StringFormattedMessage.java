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
import java.util.Arrays;
import java.util.IllegalFormatException;
import java.util.Locale;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Handles messages that consist of a format string conforming to {@link java.util.Formatter}.
 *
 * <p>
 * <strong>Note to implementors:</strong>
 * </p>
 * <p>
 * This class implements the unrolled args API even though StringFormattedMessage does not. This leaves the room for
 * StringFormattedMessage to unroll itself later.
 * </p>
 */
public class StringFormattedMessage implements Message {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static final long serialVersionUID = -665975803997290697L;

    private static final int HASHVAL = 31;

    private String messagePattern;
    private transient Object[] argArray;
    private String[] stringArgs;
    private transient String formattedMessage;
    private transient Throwable throwable;
    private final Locale locale;

    /**
     * Constructs a message.
     *
     * @param locale the locale for this message format
     * @param messagePattern the pattern for this message format
     * @param arguments The objects to format
     * @since 2.6
     */
    public StringFormattedMessage(final Locale locale, final String messagePattern, final Object... arguments) {
        this.locale = locale;
        this.messagePattern = messagePattern;
        this.argArray = arguments;
        if (arguments != null && arguments.length > 0 && arguments[arguments.length - 1] instanceof Throwable) {
            this.throwable = (Throwable) arguments[arguments.length - 1];
        }
    }

    /**
     * Constructs a message.
     *
     * @param messagePattern the pattern for this message format
     * @param arguments The objects to format
     * @since 2.6
     */
    public StringFormattedMessage(final String messagePattern, final Object... arguments) {
        this(Locale.getDefault(Locale.Category.FORMAT), messagePattern, arguments);
    }

    /**
     * Returns the formatted message.
     * @return the formatted message.
     */
    @Override
    public String getFormattedMessage() {
        if (formattedMessage == null) {
            formattedMessage = formatMessage(messagePattern, argArray);
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
        if (argArray != null) {
            return argArray;
        }
        return stringArgs;
    }

    protected String formatMessage(final String msgPattern, final Object... args) {
        if (args != null && args.length == 0) {
            // Avoids some exceptions for LOG4J2-3458
            return msgPattern;
        }
        try {
            return String.format(locale, msgPattern, args);
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
        if (!(o instanceof StringFormattedMessage)) {
            return false;
        }

        final StringFormattedMessage that = (StringFormattedMessage) o;

        if (messagePattern != null ? !messagePattern.equals(that.messagePattern) : that.messagePattern != null) {
            return false;
        }

        return Arrays.equals(stringArgs, that.stringArgs);
    }

    @Override
    public int hashCode() {
        int result = messagePattern != null ? messagePattern.hashCode() : 0;
        result = HASHVAL * result + (stringArgs != null ? Arrays.hashCode(stringArgs) : 0);
        return result;
    }

    @Override
    public String toString() {
        return getFormattedMessage();
    }

    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        getFormattedMessage();
        out.writeUTF(formattedMessage);
        out.writeUTF(messagePattern);
        out.writeInt(argArray.length);
        stringArgs = new String[argArray.length];
        int i = 0;
        for (final Object obj : argArray) {
            final String string = String.valueOf(obj);
            stringArgs[i] = string;
            out.writeUTF(string);
            ++i;
        }
    }

    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        formattedMessage = in.readUTF();
        messagePattern = in.readUTF();
        final int length = in.readInt();
        stringArgs = new String[length];
        for (int i = 0; i < length; ++i) {
            stringArgs[i] = in.readUTF();
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
