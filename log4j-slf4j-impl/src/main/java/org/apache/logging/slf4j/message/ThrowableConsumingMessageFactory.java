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
package org.apache.logging.slf4j.message;

import java.util.Arrays;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory2;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.message.SimpleMessage;

/**
 * A message factory that eagerly removes a trailing throwable argument.
 * <p>
 *     This factory implements the algorithm used by Logback 1.1.x or later to determine
 *     {@link Message#getThrowable()}: if the last argument is a {@link Throwable} and there are less arguments than
 *     the number of placeholders incremented by one, the last argument will be used as
 *     {@link Message#getThrowable()} and will <strong>not</strong> appear in the parameterized message.
 * </p>
 * <p>
 *     The usual Log4j semantic only looks for throwables once <strong>all</strong> the placeholders have been filled.
 * </p>
 * @since 2.24.0
 */
public final class ThrowableConsumingMessageFactory implements MessageFactory2 {

    private Message newParameterizedMessage(final Object throwable, final String pattern, final Object... args) {
        return new ParameterizedMessage(pattern, args, (Throwable) throwable);
    }

    @Override
    public Message newMessage(final Object message) {
        return new ObjectMessage(message);
    }

    @Override
    public Message newMessage(final String message) {
        return new SimpleMessage(message);
    }

    @Override
    public Message newMessage(final String message, final Object... params) {
        if (params != null && params.length > 0) {
            final Object lastArg = params[params.length - 1];
            return lastArg instanceof Throwable
                    ? newParameterizedMessage(lastArg, message, Arrays.copyOf(params, params.length - 1))
                    : newParameterizedMessage(null, message, params);
        }
        return new SimpleMessage(message);
    }

    @Override
    public Message newMessage(final CharSequence charSequence) {
        return new SimpleMessage(charSequence);
    }

    @Override
    public Message newMessage(final String message, final Object p0) {
        return p0 instanceof Throwable
                ? newParameterizedMessage(p0, message)
                : newParameterizedMessage(null, message, p0);
    }

    @Override
    public Message newMessage(final String message, final Object p0, final Object p1) {
        return p1 instanceof Throwable
                ? newParameterizedMessage(p1, message, p0)
                : newParameterizedMessage(null, message, p0, p1);
    }

    @Override
    public Message newMessage(final String message, final Object p0, final Object p1, final Object p2) {
        return p2 instanceof Throwable
                ? newParameterizedMessage(p2, message, p0, p1)
                : newParameterizedMessage(null, message, p0, p1, p2);
    }

    @Override
    public Message newMessage(
            final String message, final Object p0, final Object p1, final Object p2, final Object p3) {
        return p3 instanceof Throwable
                ? newParameterizedMessage(p3, message, p0, p1, p2)
                : newParameterizedMessage(null, message, p0, p1, p2, p3);
    }

    @Override
    public Message newMessage(
            final String message, final Object p0, final Object p1, final Object p2, final Object p3, final Object p4) {
        return p4 instanceof Throwable
                ? newParameterizedMessage(p4, message, p0, p1, p2, p3)
                : newParameterizedMessage(null, message, p0, p1, p2, p3, p4);
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
        return p5 instanceof Throwable
                ? newParameterizedMessage(p5, message, p0, p1, p2, p3, p4)
                : newParameterizedMessage(null, message, p0, p1, p2, p3, p4, p5);
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
        return p6 instanceof Throwable
                ? newParameterizedMessage(p6, message, p0, p1, p2, p3, p4, p5)
                : newParameterizedMessage(null, message, p0, p1, p2, p3, p4, p5, p6);
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
        return p7 instanceof Throwable
                ? newParameterizedMessage(p7, message, p0, p1, p2, p3, p4, p5, p6)
                : newParameterizedMessage(null, message, p0, p1, p2, p3, p4, p5, p6, p7);
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
        return p8 instanceof Throwable
                ? newParameterizedMessage(p8, message, p0, p1, p2, p3, p4, p5, p6, p7)
                : newParameterizedMessage(null, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
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
        return p9 instanceof Throwable
                ? newParameterizedMessage(p9, message, p0, p1, p2, p3, p4, p5, p6, p7, p8)
                : newParameterizedMessage(null, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }
}
