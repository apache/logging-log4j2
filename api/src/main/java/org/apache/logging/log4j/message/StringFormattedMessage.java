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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.IllegalFormatException;

/**
 * Handles messages that consist of a format string conforming to java.util.Formatter.
 */
public class StringFormattedMessage implements Message, Serializable {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static final long serialVersionUID = -665975803997290697L;

    private static final int HASHVAL = 31;

    private String messagePattern;
    private transient Object[] argArray;
    private String[] stringArgs;
    private transient String formattedMessage;

    /**
     * Create the StringFormattedMessage.
     */
    public StringFormattedMessage() {
        this(null, null, null);
    }


    public StringFormattedMessage(String messagePattern, Object... arguments) {
        this.messagePattern = messagePattern;
        this.argArray = arguments;
    }

    /**
     * Return the formatted message.
     * @return the formatted message.
     */
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
    public String getMessageFormat() {
        return messagePattern;
    }

    /**
     * Set the message pattern.
     * @param messagePattern The message pattern.
     */
    public void setMessageFormat(String messagePattern) {
        this.messagePattern = messagePattern;
        this.formattedMessage = null;
    }

    /**
     * Returns the message parameters.
     * @return the message parameters.
     */
    public Object[] getParameters() {
        if (argArray != null) {
            return argArray;
        }
        return stringArgs;
    }

    /**
     * Sets the parameters for the message.
     * @param parameters The parameters.
     */
    public void setParameters(Object[] parameters) {
        this.argArray = parameters;
        this.formattedMessage = null;
    }

    protected String formatMessage(String msgPattern, Object... args) {
        try {
            return String.format(msgPattern, args);
        } catch (IllegalFormatException ife) {
            LOGGER.error("Unable to format msg: " + msgPattern, ife);
            return msgPattern;
        }
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        StringFormattedMessage that = (StringFormattedMessage) o;

        if (messagePattern != null ? !messagePattern.equals(that.messagePattern) : that.messagePattern != null) {
            return false;
        }
        if (!Arrays.equals(stringArgs, that.stringArgs)) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result = messagePattern != null ? messagePattern.hashCode() : 0;
        result = HASHVAL * result + (stringArgs != null ? Arrays.hashCode(stringArgs) : 0);
        return result;
    }


    public String toString() {
        return "StringFormatMessage[messagePattern=" + messagePattern + ", args=" +
            Arrays.toString(argArray) +  "]";
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        getFormattedMessage();
        out.writeUTF(formattedMessage);
        out.writeUTF(messagePattern);
        out.writeInt(argArray.length);
        stringArgs = new String[argArray.length];
        int i = 0;
        for (Object obj : argArray) {
            stringArgs[i] = obj.toString();
            ++i;
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        formattedMessage = in.readUTF();
        messagePattern = in.readUTF();
        int length = in.readInt();
        stringArgs = new String[length];
        for (int i = 0; i < length; ++i) {
            stringArgs[i] = in.readUTF();
        }
    }
}
