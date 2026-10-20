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
import java.text.Format;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Locale;

/**
 * Handles messages that contain a format String. Dynamically determines if the format conforms to
 * MessageFormat or String.format and if not then uses ParameterizedMessage to format.
 */
public class FormattedMessage implements Message {

    private static final long serialVersionUID = -665975803997290697L;
    private static final int HASHVAL = 31;

    private String messagePattern;
    private transient Object[] argArray;
    private String[] stringArgs;
    private transient String formattedMessage;
    private final Throwable throwable;
    private Message message;
    private final Locale locale;

    /**
     * Constructs with a locale, a pattern and a single parameter.
     * @param locale The locale
     * @param messagePattern The message pattern.
     * @param arg The parameter.
     * @since 2.6
     */
    public FormattedMessage(final Locale locale, final String messagePattern, final Object arg) {
        this(locale, messagePattern, new Object[] {arg}, null);
    }

    /**
     * Constructs with a locale, a pattern and two parameters.
     * @param locale The locale
     * @param messagePattern The message pattern.
     * @param arg1 The first parameter.
     * @param arg2 The second parameter.
     * @since 2.6
     */
    public FormattedMessage(final Locale locale, final String messagePattern, final Object arg1, final Object arg2) {
        this(locale, messagePattern, new Object[] {arg1, arg2});
    }

    /**
     * Constructs with a locale, a pattern and a parameter array.
     * @param locale The locale
     * @param messagePattern The message pattern.
     * @param arguments The parameter.
     * @since 2.6
     */
    public FormattedMessage(final Locale locale, final String messagePattern, final Object... arguments) {
        this(locale, messagePattern, arguments, null);
    }

    /**
     * Constructs with a locale, a pattern, a parameter array, and a throwable.
     * @param locale The Locale
     * @param messagePattern The message pattern.
     * @param arguments The parameter.
     * @param throwable The throwable
     * @since 2.6
     */
    public FormattedMessage(
            final Locale locale, final String messagePattern, final Object[] arguments, final Throwable throwable) {
        this.locale = locale;
        this.messagePattern = messagePattern;
        this.argArray = arguments;
        this.throwable = throwable;
    }

    /**
     * Constructs with a pattern and a single parameter.
     * @param messagePattern The message pattern.
     * @param arg The parameter.
     */
    public FormattedMessage(final String messagePattern, final Object arg) {
        this(messagePattern, new Object[] {arg}, null);
    }

    /**
     * Constructs with a pattern and two parameters.
     * @param messagePattern The message pattern.
     * @param arg1 The first parameter.
     * @param arg2 The second parameter.
     */
    public FormattedMessage(final String messagePattern, final Object arg1, final Object arg2) {
        this(messagePattern, new Object[] {arg1, arg2});
    }

    /**
     * Constructs with a pattern and a parameter array.
     * @param messagePattern The message pattern.
     * @param arguments The parameter.
     */
    public FormattedMessage(final String messagePattern, final Object... arguments) {
        this(messagePattern, arguments, null);
    }

    /**
     * Constructs with a pattern, a parameter array, and a throwable.
     * @param messagePattern The message pattern.
     * @param arguments The parameter.
     * @param throwable The throwable
     */
    public FormattedMessage(final String messagePattern, final Object[] arguments, final Throwable throwable) {
        this.locale = Locale.getDefault(Locale.Category.FORMAT);
        this.messagePattern = messagePattern;
        this.argArray = arguments;
        this.throwable = throwable;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FormattedMessage)) {
            return false;
        }

        final FormattedMessage that = (FormattedMessage) o;

        if (messagePattern != null ? !messagePattern.equals(that.messagePattern) : that.messagePattern != null) {
            return false;
        }
        return Arrays.equals(stringArgs, that.stringArgs);
    }

    /**
     * Gets the message pattern.
     * @return the message pattern.
     */
    @Override
    public String getFormat() {
        return messagePattern;
    }

    /**
     * Gets the formatted message.
     * @return the formatted message.
     */
    @Override
    public String getFormattedMessage() {
        if (formattedMessage == null) {
            if (message == null) {
                message = getMessage(messagePattern, argArray, throwable);
            }
            formattedMessage = message.getFormattedMessage();
        }
        return formattedMessage;
    }

    /**
     * Gets the message implementation to which formatting is delegated.
     *
     * <ul>
     *     <li>if {@code msgPattern} contains {@link MessageFormat} format specifiers a {@link MessageFormatMessage}
     * is returned,</li>
     *     <li>if {@code msgPattern} contains {@code {}} placeholders a {@link ParameterizedMessage} is returned,</li>
     *     <li>if {@code msgPattern} contains {@link Format} specifiers a {@link StringFormattedMessage} is returned
     *    .</li>
     * </ul>
     * <p>
     *     Mixing specifiers from multiple types is not supported.
     * </p>
     *
     * @param msgPattern The message pattern.
     * @param args       The parameters.
     * @param aThrowable The throwable
     * @return The message that performs formatting.
     */
    protected Message getMessage(final String msgPattern, final Object[] args, final Throwable aThrowable) {
        // Check for valid `{ ArgumentIndex [, FormatType [, FormatStyle]] }` format specifiers
        try {
            final MessageFormat format = new MessageFormat(msgPattern);
            final Format[] formats = format.getFormats();
            if (formats.length > 0) {
                return new MessageFormatMessage(locale, msgPattern, args);
            }
        } catch (final Exception ignored) {
            // Obviously, the message is not a proper pattern for MessageFormat.
        }
        // Check for non-escaped `{}` format specifiers
        // This case also includes patterns without any `java.util.Formatter` specifiers
        if (ParameterFormatter.analyzePattern(msgPattern, 1).placeholderCount > 0 || msgPattern.indexOf('%') == -1) {
            return new ParameterizedMessage(msgPattern, args, aThrowable);
        }
        // Interpret as `java.util.Formatter` format
        return new StringFormattedMessage(locale, msgPattern, args);
    }

    /**
     * Gets the message parameters.
     * @return the message parameters.
     */
    @Override
    public Object[] getParameters() {
        if (argArray != null) {
            return argArray;
        }
        return stringArgs;
    }

    @Override
    public Throwable getThrowable() {
        if (throwable != null) {
            return throwable;
        }
        if (message == null) {
            message = getMessage(messagePattern, argArray, null);
        }
        return message.getThrowable();
    }

    @Override
    public int hashCode() {
        int result = messagePattern != null ? messagePattern.hashCode() : 0;
        result = HASHVAL * result + (stringArgs != null ? Arrays.hashCode(stringArgs) : 0);
        return result;
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
}
