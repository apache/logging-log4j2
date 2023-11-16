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
 * Creates {@link FormattedMessage} instances for {@link MessageFactory2} methods (and {@link MessageFactory} by
 * extension.)
 *
 * <p>
 * <strong>Note to implementors:</strong>
 * </p>
 * <p>
 * This class implements all {@link MessageFactory2} methods.
 * </p>
 */
public class FormattedMessageFactory extends AbstractMessageFactory {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a message factory with default flow strings.
     */
    public FormattedMessageFactory() {}

    /**
     * Creates {@link StringFormattedMessage} instances.
     *
     * @param message The message format.
     * @param params Message parameters.
     * @return The Message object.
     *
     * @see MessageFactory#newMessage(String, Object...)
     */
    @Override
    public Message newMessage(final String message, final Object... params) {
        return new FormattedMessage(message, params);
    }

    /**
     * @since 2.6.1
     */
    @Override
    public Message newMessage(final String message, final Object p0) {
        return new FormattedMessage(message, p0);
    }

    /**
     * @since 2.6.1
     */
    @Override
    public Message newMessage(final String message, final Object p0, final Object p1) {
        return new FormattedMessage(message, p0, p1);
    }

    /**
     * @since 2.6.1
     */
    @Override
    public Message newMessage(final String message, final Object p0, final Object p1, final Object p2) {
        return new FormattedMessage(message, p0, p1, p2);
    }

    /**
     * @since 2.6.1
     */
    @Override
    public Message newMessage(
            final String message, final Object p0, final Object p1, final Object p2, final Object p3) {
        return new FormattedMessage(message, p0, p1, p2, p3);
    }

    /**
     * @since 2.6.1
     */
    @Override
    public Message newMessage(
            final String message, final Object p0, final Object p1, final Object p2, final Object p3, final Object p4) {
        return new FormattedMessage(message, p0, p1, p2, p3, p4);
    }

    /**
     * @since 2.6.1
     */
    @Override
    public Message newMessage(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5) {
        return new FormattedMessage(message, p0, p1, p2, p3, p4, p5);
    }

    /**
     * @since 2.6.1
     */
    @Override
    public Message newMessage(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6) {
        return new FormattedMessage(message, p0, p1, p2, p3, p4, p5, p6);
    }

    /**
     * @since 2.6.1
     */
    @Override
    public Message newMessage(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7) {
        return new FormattedMessage(message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    /**
     * @since 2.6.1
     */
    @Override
    public Message newMessage(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8) {
        return new FormattedMessage(message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    /**
     * @since 2.6.1
     */
    @Override
    public Message newMessage(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8,
            final Object p9) {
        return new FormattedMessage(message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }
}
