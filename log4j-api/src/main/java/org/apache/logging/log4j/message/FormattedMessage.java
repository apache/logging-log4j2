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
package org.apache.logging.log4j.message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.Format;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Handles messages that contain a format String. Dynamically determines if the format conforms to
 * MessageFormat or String.format and if not then uses ParameterizedMessage to format.
 */
public class FormattedMessage implements Message {

    private static final long serialVersionUID = -665975803997290697L;
    private static final int HASHVAL = 31;
    private static final String FORMAT_SPECIFIER = "%(\\d+\\$)?([-#+ 0,(\\<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])";
    private static final Pattern MSG_PATTERN = Pattern.compile(FORMAT_SPECIFIER);

    private String messagePattern;
    private transient Object[] argArray;
    private String[] stringArgs;
    private transient String formattedMessage;
    private final Throwable throwable;
    private Message message;

    public FormattedMessage(final String messagePattern, final Object[] arguments, final Throwable throwable) {
        this.messagePattern = messagePattern;
        this.argArray = arguments;
        this.throwable = throwable;
    }

    public FormattedMessage(final String messagePattern, final Object[] arguments) {
        this(messagePattern, arguments, null);
    }

    /**
     * Constructor with a pattern and a single parameter.
     * @param messagePattern The message pattern.
     * @param arg The parameter.
     */
    public FormattedMessage(final String messagePattern, final Object arg) {
        this(messagePattern, new Object[] {arg}, null);
    }

    /**
     * Constructor with a pattern and two parameters.
     * @param messagePattern The message pattern.
     * @param arg1 The first parameter.
     * @param arg2 The second parameter.
     */
    public FormattedMessage(final String messagePattern, final Object arg1, final Object arg2) {
        this(messagePattern, new Object[]{arg1, arg2});
    }


    /**
     * Returns the formatted message.
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

    protected Message getMessage(final String msgPattern, final Object[] args, final Throwable throwable) {
        try {
            final MessageFormat format = new MessageFormat(msgPattern);
            final Format[] formats = format.getFormats();
            if (formats != null && formats.length > 0) {
                return new MessageFormatMessage(msgPattern, args);
            }
        } catch (final Exception ignored) {
            // Obviously, the message is not a proper pattern for MessageFormat.
        }
        try {
            if (MSG_PATTERN.matcher(msgPattern).find()) {
                return new StringFormattedMessage(msgPattern, args);
            }
        } catch (final Exception ignored) {
            // Also not properly formatted.
        }
        return new ParameterizedMessage(msgPattern, args, throwable);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final FormattedMessage that = (FormattedMessage) o;

        if (messagePattern != null ? !messagePattern.equals(that.messagePattern) : that.messagePattern != null) {
            return false;
        }
        if (!Arrays.equals(stringArgs, that.stringArgs)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = messagePattern != null ? messagePattern.hashCode() : 0;
        result = HASHVAL * result + (stringArgs != null ? Arrays.hashCode(stringArgs) : 0);
        return result;
    }


    @Override
    public String toString() {
        return "FormattedMessage[messagePattern=" + messagePattern + ", args=" +
            Arrays.toString(argArray) + ']';
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
            stringArgs[i] = obj.toString();
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
     * Always returns null.
     *
     * @return null
     */
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
}
