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
import java.util.Objects;
import org.apache.logging.log4j.util.StringBuilderFormattable;

/**
 * The simplest possible implementation of Message. It just returns the String given as the constructor argument.
 */
public class SimpleMessage implements Message, StringBuilderFormattable, CharSequence {
    private static final long serialVersionUID = -8398002534962715992L;

    private String message;
    private transient CharSequence charSequence;

    /**
     * Basic constructor.
     */
    public SimpleMessage() {
        this(null);
    }

    /**
     * Constructor that includes the message.
     * @param message The String message.
     */
    public SimpleMessage(final String message) {
        this.message = message;
        this.charSequence = message;
    }

    /**
     * Constructor that includes the message.
     * @param charSequence The CharSequence message.
     */
    public SimpleMessage(final CharSequence charSequence) {
        // this.message = String.valueOf(charSequence); // postponed until getFormattedMessage
        this.charSequence = charSequence;
    }

    /**
     * Returns the message.
     * @return the message.
     */
    @Override
    public String getFormattedMessage() {
        return message = message == null ? String.valueOf(charSequence) : message;
    }

    @Override
    public void formatTo(final StringBuilder buffer) {
        buffer.append(message != null ? message : charSequence);
    }

    /**
     * Returns the message.
     * @return the message.
     */
    @Override
    public String getFormat() {
        return message;
    }

    /**
     * Returns null since there are no parameters.
     * @return null.
     */
    @Override
    public Object[] getParameters() {
        return null;
    }

    @Override
    @SuppressWarnings("UndefinedEquals")
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SimpleMessage)) {
            return false;
        }

        final SimpleMessage that = (SimpleMessage) o;

        /*
         * https://errorprone.info/bugpattern/UndefinedEquals
         *
         * If the char sequences are different, we fall back on string comparison.
         */
        return Objects.equals(this.charSequence, that.charSequence)
                || Objects.equals(this.getFormattedMessage(), that.getFormattedMessage());
    }

    @Override
    public int hashCode() {
        return charSequence != null ? charSequence.hashCode() : 0;
    }

    @Override
    public String toString() {
        return getFormattedMessage();
    }

    /**
     * Always returns null.
     *
     * @return null
     */
    @Override
    public Throwable getThrowable() {
        return null;
    }

    // CharSequence impl

    @Override
    public int length() {
        return charSequence == null ? 0 : charSequence.length();
    }

    @Override
    public char charAt(final int index) {
        return charSequence.charAt(index);
    }

    @Override
    public CharSequence subSequence(final int start, final int end) {
        return charSequence.subSequence(start, end);
    }

    private void writeObject(final ObjectOutputStream out) throws IOException {
        getFormattedMessage(); // initialize the message:String field
        out.defaultWriteObject();
    }

    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        charSequence = message;
    }
}
