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

/**
 * Creates messages. Implementations can provide different message format syntaxes.
 *
 * @see ParameterizedMessageFactory
 * @since 2.6
 */
public interface MessageFactory2 extends MessageFactory {

    /**
     * Creates a new message for the specified CharSequence.
     * @param charSequence the (potentially mutable) CharSequence
     * @return a new message for the specified CharSequence
     */
    Message newMessage(CharSequence charSequence);

    /**
     * Creates a new parameterized message.
     *
     * @param message a message template, the kind of message template depends on the implementation.
     * @param p0 a message parameter
     * @return a new message
     * @see ParameterizedMessageFactory
     */
    Message newMessage(String message, Object p0);

    /**
     * Creates a new parameterized message.
     *
     * @param message a message template, the kind of message template depends on the implementation.
     * @param p0 a message parameter
     * @param p1 a message parameter
     * @return a new message
     * @see ParameterizedMessageFactory
     */
    Message newMessage(String message, Object p0, Object p1);

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
    Message newMessage(String message, Object p0, Object p1, Object p2);

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
    Message newMessage(String message, Object p0, Object p1, Object p2, Object p3);

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
    Message newMessage(String message, Object p0, Object p1, Object p2, Object p3, Object p4);

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
    Message newMessage(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5);

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
    Message newMessage(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6);

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
    Message newMessage(
            String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7);

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
    Message newMessage(
            String message,
            Object p0,
            Object p1,
            Object p2,
            Object p3,
            Object p4,
            Object p5,
            Object p6,
            Object p7,
            Object p8);

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
    Message newMessage(
            String message,
            Object p0,
            Object p1,
            Object p2,
            Object p3,
            Object p4,
            Object p5,
            Object p6,
            Object p7,
            Object p8,
            Object p9);
}
