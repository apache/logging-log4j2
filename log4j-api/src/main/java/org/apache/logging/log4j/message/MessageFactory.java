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

/**
 * Creates messages. Implementations can provide different message format syntaxes.
 *
 * @see ParameterizedMessageFactory
 * @see StringFormatterMessageFactory
 */
public interface MessageFactory {

    /**
     * Creates a new message based on an Object.
     *
     * @param message
     *            a message object
     * @return a new message
     */
    default Message newMessage(final Object message) {
        return new ObjectMessage(message);
    }

    /**
     * Creates a new message based on a String.
     *
     * @param message
     *            a message String
     * @return a new message
     */
    default Message newMessage(final String message) {
        return new SimpleMessage(message);
    }

    /**
     * Creates a new parameterized message.
     *
     * @param message
     *            a message template, the kind of message template depends on the implementation.
     * @param params
     *            the message parameters
     * @return a new message
     * @see ParameterizedMessageFactory
     * @see StringFormatterMessageFactory
     */
    Message newMessage(String message, Object... params);

    /**
     * Creates a new message for the specified CharSequence.
     * @param charSequence the (potentially mutable) CharSequence
     * @return a new message for the specified CharSequence
     */
    default Message newMessage(final CharSequence charSequence) {
        return new SimpleMessage(charSequence);
    }

    /**
     * Creates a new parameterized message.
     *
     * @param message a message template, the kind of message template depends on the implementation.
     * @param p0 a message parameter
     * @return a new message
     * @see ParameterizedMessageFactory
     */
    default Message newMessage(final String message, final Object p0) {
        return newMessage(message, new Object[] { p0 });
    }

    /**
     * Creates a new parameterized message.
     *
     * @param message a message template, the kind of message template depends on the implementation.
     * @param p0 a message parameter
     * @param p1 a message parameter
     * @return a new message
     * @see ParameterizedMessageFactory
     */
    default Message newMessage(final String message, final Object p0, final Object p1) {
        return newMessage(message, new Object[] { p0, p1 });
    }

    /**
     * Creates a new parameterized message.
     *
     * @param message a message template, the kind of message template depends on the implementation.
     * @param p0 a message parameter
     * @param p1 a message parameter
     * @param p2 a message parameter
     * @return a new message
     * @see ParameterizedMessageFactory
     */
    default Message newMessage(final String message, final Object p0, final Object p1, final Object p2) {
        return newMessage(message, new Object[] { p0, p1, p2 });
    }

    /**
     * Creates a new parameterized message.
     *
     * @param message a message template, the kind of message template depends on the implementation.
     * @param p0 a message parameter
     * @param p1 a message parameter
     * @param p2 a message parameter
     * @param p3 a message parameter
     * @return a new message
     * @see ParameterizedMessageFactory
     */
    default Message newMessage(final String message, final Object p0, final Object p1, final Object p2, final Object p3) {
        return newMessage(message, new Object[] { p0, p1, p2, p3 });
    }

    /**
     * Creates a new parameterized message.
     *
     * @param message a message template, the kind of message template depends on the implementation.
     * @param p0 a message parameter
     * @param p1 a message parameter
     * @param p2 a message parameter
     * @param p3 a message parameter
     * @param p4 a message parameter
     * @return a new message
     * @see ParameterizedMessageFactory
     */
    default Message newMessage(final String message, final Object p0, final Object p1, final Object p2, final Object p3, final Object p4) {
        return newMessage(message, new Object[] { p0, p1, p2, p3, p4 });
    }

    /**
     * Creates a new parameterized message.
     *
     * @param message a message template, the kind of message template depends on the implementation.
     * @param p0 a message parameter
     * @param p1 a message parameter
     * @param p2 a message parameter
     * @param p3 a message parameter
     * @param p4 a message parameter
     * @param p5 a message parameter
     * @return a new message
     * @see ParameterizedMessageFactory
     */
    default Message newMessage(final String message, final Object p0, final Object p1, final Object p2, final Object p3, final Object p4, final Object p5) {
        return newMessage(message, new Object[] { p0, p1, p2, p3, p4, p5 });
    }

    /**
     * Creates a new parameterized message.
     *
     * @param message a message template, the kind of message template depends on the implementation.
     * @param p0 a message parameter
     * @param p1 a message parameter
     * @param p2 a message parameter
     * @param p3 a message parameter
     * @param p4 a message parameter
     * @param p5 a message parameter
     * @param p6 a message parameter
     * @return a new message
     * @see ParameterizedMessageFactory
     */
    default Message newMessage(final String message, final Object p0, final Object p1, final Object p2, final Object p3, final Object p4, final Object p5,
                               final Object p6) {
        return newMessage(message, new Object[] { p0, p1, p2, p3, p4, p5, p6 });
    }

    /**
     * Creates a new parameterized message.
     *
     * @param message a message template, the kind of message template depends on the implementation.
     * @param p0 a message parameter
     * @param p1 a message parameter
     * @param p2 a message parameter
     * @param p3 a message parameter
     * @param p4 a message parameter
     * @param p5 a message parameter
     * @param p6 a message parameter
     * @param p7 a message parameter
     * @return a new message
     * @see ParameterizedMessageFactory
     */
    default Message newMessage(final String message, final Object p0, final Object p1, final Object p2, final Object p3, final Object p4, final Object p5,
                               final Object p6, final Object p7) {
        return newMessage(message, new Object[] { p0, p1, p2, p3, p4, p5, p6, p7 });
    }

    /**
     * Creates a new parameterized message.
     *
     * @param message a message template, the kind of message template depends on the implementation.
     * @param p0 a message parameter
     * @param p1 a message parameter
     * @param p2 a message parameter
     * @param p3 a message parameter
     * @param p4 a message parameter
     * @param p5 a message parameter
     * @param p6 a message parameter
     * @param p7 a message parameter
     * @param p8 a message parameter
     * @return a new message
     * @see ParameterizedMessageFactory
     */
    default Message newMessage(final String message, final Object p0, final Object p1, final Object p2, final Object p3, final Object p4, final Object p5,
                               final Object p6, final Object p7, final Object p8) {
        return newMessage(message, new Object[] { p0, p1, p2, p3, p4, p5, p6, p7, p8 });
    }

    /**
     * Creates a new parameterized message.
     *
     * @param message a message template, the kind of message template depends on the implementation.
     * @param p0 a message parameter
     * @param p1 a message parameter
     * @param p2 a message parameter
     * @param p3 a message parameter
     * @param p4 a message parameter
     * @param p5 a message parameter
     * @param p6 a message parameter
     * @param p7 a message parameter
     * @param p8 a message parameter
     * @param p9 a message parameter
     * @return a new message
     * @see ParameterizedMessageFactory
     */
    default Message newMessage(final String message, final Object p0, final Object p1, final Object p2, final Object p3, final Object p4, final Object p5,
                               final Object p6, final Object p7, final Object p8, final Object p9) {
        return newMessage(message, new Object[] { p0, p1, p2, p3, p4, p5, p6, p7, p8, p9 });
    }
}
