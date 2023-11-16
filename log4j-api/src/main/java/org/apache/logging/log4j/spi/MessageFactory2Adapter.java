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
package org.apache.logging.log4j.spi;

import java.util.Objects;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.MessageFactory2;
import org.apache.logging.log4j.message.SimpleMessage;

/**
 * Adapts a legacy MessageFactory to the new MessageFactory2 interface.
 *
 * @since 2.6
 */
public class MessageFactory2Adapter implements MessageFactory2 {
    private final MessageFactory wrapped;

    public MessageFactory2Adapter(final MessageFactory wrapped) {
        this.wrapped = Objects.requireNonNull(wrapped);
    }

    public MessageFactory getOriginal() {
        return wrapped;
    }

    @Override
    public Message newMessage(final CharSequence charSequence) {
        return new SimpleMessage(charSequence);
    }

    @Override
    public Message newMessage(final String message, final Object p0) {
        return wrapped.newMessage(message, p0);
    }

    @Override
    public Message newMessage(final String message, final Object p0, final Object p1) {
        return wrapped.newMessage(message, p0, p1);
    }

    @Override
    public Message newMessage(final String message, final Object p0, final Object p1, final Object p2) {
        return wrapped.newMessage(message, p0, p1, p2);
    }

    @Override
    public Message newMessage(
            final String message, final Object p0, final Object p1, final Object p2, final Object p3) {
        return wrapped.newMessage(message, p0, p1, p2, p3);
    }

    @Override
    public Message newMessage(
            final String message, final Object p0, final Object p1, final Object p2, final Object p3, final Object p4) {
        return wrapped.newMessage(message, p0, p1, p2, p3, p4);
    }

    @Override
    public Message newMessage(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5) {
        return wrapped.newMessage(message, p0, p1, p2, p3, p4, p5);
    }

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
        return wrapped.newMessage(message, p0, p1, p2, p3, p4, p5, p6);
    }

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
        return wrapped.newMessage(message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

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
        return wrapped.newMessage(message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

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
        return wrapped.newMessage(message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    @Override
    public Message newMessage(final Object message) {
        return wrapped.newMessage(message);
    }

    @Override
    public Message newMessage(final String message) {
        return wrapped.newMessage(message);
    }

    @Override
    public Message newMessage(final String message, final Object... params) {
        return wrapped.newMessage(message, params);
    }
}
