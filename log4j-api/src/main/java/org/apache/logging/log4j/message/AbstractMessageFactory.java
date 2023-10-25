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

import java.io.Serializable;

/**
 * Provides an abstract superclass for {@link MessageFactory2} implementations with default implementations (and for
 * {@link MessageFactory} by extension).
 * <p>
 * This class is immutable.
 * </p>
 * <p>
 * <strong>Note to implementors:</strong>
 * </p>
 * <p>
 * Subclasses can implement the {@link MessageFactory2} methods when they can most effectively build {@link Message}
 * instances. If a subclass does not implement {@link MessageFactory2} methods, these calls are routed through
 * {@link #newMessage(String, Object...)} in this class.
 * </p>
 */
public abstract class AbstractMessageFactory implements MessageFactory2, Serializable {
    private static final long serialVersionUID = -1307891137684031187L;

    @Override
    public Message newMessage(final CharSequence message) {
        return new SimpleMessage(message);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.logging.log4j.message.MessageFactory#newMessage(java.lang.Object)
     */
    @Override
    public Message newMessage(final Object message) {
        return new ObjectMessage(message);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.logging.log4j.message.MessageFactory#newMessage(java.lang.String)
     */
    @Override
    public Message newMessage(final String message) {
        return new SimpleMessage(message);
    }

    /**
     * @since 2.6.1
     */
    @Override
    public Message newMessage(final String message, final Object p0) {
        return newMessage(message, new Object[] {p0});
    }

    /**
     * @since 2.6.1
     */
    @Override
    public Message newMessage(final String message, final Object p0, final Object p1) {
        return newMessage(message, new Object[] {p0, p1});
    }

    /**
     * @since 2.6.1
     */
    @Override
    public Message newMessage(final String message, final Object p0, final Object p1, final Object p2) {
        return newMessage(message, new Object[] {p0, p1, p2});
    }

    /**
     * @since 2.6.1
     */
    @Override
    public Message newMessage(
            final String message, final Object p0, final Object p1, final Object p2, final Object p3) {
        return newMessage(message, new Object[] {p0, p1, p2, p3});
    }

    /**
     * @since 2.6.1
     */
    @Override
    public Message newMessage(
            final String message, final Object p0, final Object p1, final Object p2, final Object p3, final Object p4) {
        return newMessage(message, new Object[] {p0, p1, p2, p3, p4});
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
        return newMessage(message, new Object[] {p0, p1, p2, p3, p4, p5});
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
        return newMessage(message, new Object[] {p0, p1, p2, p3, p4, p5, p6});
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
        return newMessage(message, new Object[] {p0, p1, p2, p3, p4, p5, p6, p7});
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
        return newMessage(message, new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8});
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
        return newMessage(message, new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9});
    }
}
