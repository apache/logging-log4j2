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
package org.apache.logging.log4j.core.impl.internal;

import org.apache.logging.log4j.kit.message.RecyclingMessageFactory;
import org.apache.logging.log4j.kit.recycler.Recycler;
import org.apache.logging.log4j.kit.recycler.RecyclerFactory;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ReusableObjectMessage;
import org.apache.logging.log4j.message.ReusableParameterizedMessage;
import org.apache.logging.log4j.message.ReusableSimpleMessage;
import org.apache.logging.log4j.util.PerformanceSensitive;

/**
 * Message factory that avoids allocating temporary objects where possible.
 * <p>
 *     Message instances are cached in a {@link Recycler} and reused when a new message is requested.
 * </p>
 * Message instances are cached in a {@link Recycler} and reused when a new message is requested.
 * @see Recycler
 * @since 3.0.0
 */
@PerformanceSensitive("allocation")
public final class ReusableMessageFactory implements RecyclingMessageFactory {

    private final Recycler<ReusableParameterizedMessage> parameterizedMessageRecycler;
    private final Recycler<ReusableSimpleMessage> simpleMessageRecycler;
    private final Recycler<ReusableObjectMessage> objectMessageRecycler;

    public ReusableMessageFactory(final RecyclerFactory recyclerFactory) {
        parameterizedMessageRecycler =
                recyclerFactory.create(ReusableParameterizedMessage::new, ReusableParameterizedMessage::clear);
        simpleMessageRecycler = recyclerFactory.create(ReusableSimpleMessage::new, ReusableSimpleMessage::clear);
        objectMessageRecycler = recyclerFactory.create(ReusableObjectMessage::new, ReusableObjectMessage::clear);
    }

    @Override
    public void recycle(final Message message) {
        // related to LOG4J2-1583 and nested log messages clobbering each other. recycle messages today!
        if (message instanceof final ReusableParameterizedMessage reusable) {
            reusable.clear();
            parameterizedMessageRecycler.release(reusable);
        } else if (message instanceof final ReusableObjectMessage reusable) {
            reusable.clear();
            objectMessageRecycler.release(reusable);
        } else if (message instanceof final ReusableSimpleMessage reusable) {
            reusable.clear();
            simpleMessageRecycler.release(reusable);
        }
    }

    @Override
    public Message newMessage(final CharSequence charSequence) {
        final ReusableSimpleMessage result = simpleMessageRecycler.acquire();
        result.set(charSequence);
        return result;
    }

    @Override
    public Message newMessage(final String message, final Object... params) {
        return parameterizedMessageRecycler.acquire().set(message, params);
    }

    @Override
    public Message newMessage(final String message, final Object p0) {
        return parameterizedMessageRecycler.acquire().set(message, p0);
    }

    @Override
    public Message newMessage(final String message, final Object p0, final Object p1) {
        return parameterizedMessageRecycler.acquire().set(message, p0, p1);
    }

    @Override
    public Message newMessage(final String message, final Object p0, final Object p1, final Object p2) {
        return parameterizedMessageRecycler.acquire().set(message, p0, p1, p2);
    }

    @Override
    public Message newMessage(
            final String message, final Object p0, final Object p1, final Object p2, final Object p3) {
        return parameterizedMessageRecycler.acquire().set(message, p0, p1, p2, p3);
    }

    @Override
    public Message newMessage(
            final String message, final Object p0, final Object p1, final Object p2, final Object p3, final Object p4) {
        return parameterizedMessageRecycler.acquire().set(message, p0, p1, p2, p3, p4);
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
        return parameterizedMessageRecycler.acquire().set(message, p0, p1, p2, p3, p4, p5);
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
        return parameterizedMessageRecycler.acquire().set(message, p0, p1, p2, p3, p4, p5, p6);
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
        return parameterizedMessageRecycler.acquire().set(message, p0, p1, p2, p3, p4, p5, p6, p7);
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
        return parameterizedMessageRecycler.acquire().set(message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
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
        return parameterizedMessageRecycler.acquire().set(message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    @Override
    public Message newMessage(final String message) {
        final ReusableSimpleMessage result = simpleMessageRecycler.acquire();
        result.set(message);
        return result;
    }

    @Override
    public Message newMessage(final Object message) {
        final ReusableObjectMessage result = objectMessageRecycler.acquire();
        result.set(message);
        return result;
    }
}
