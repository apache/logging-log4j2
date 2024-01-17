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

import org.apache.logging.log4j.spi.LoggingSystem;
import org.apache.logging.log4j.spi.recycler.Recycler;
import org.apache.logging.log4j.spi.recycler.RecyclerFactory;
import org.apache.logging.log4j.util.PerformanceSensitive;

/**
 * Implementation of the {@link MessageFactory} interface that avoids allocating temporary objects where possible.
 * Message instances are cached in a {@link Recycler} and reused when a new message is requested.
 * Messages returned from this factory must be {@linkplain #recycle(Message) recycled} when done using.
 * @see ReusableSimpleMessage
 * @see ReusableObjectMessage
 * @see ReusableParameterizedMessage
 * @see Recycler
 * @since 2.6
 */
@PerformanceSensitive("allocation")
public final class ReusableMessageFactory implements MessageFactory {

    /**
     * Instance of {@link ReusableMessageFactory}.
     */
    public static final ReusableMessageFactory INSTANCE = new ReusableMessageFactory();

    private final Recycler<ReusableParameterizedMessage> parameterizedMessageRecycler;
    private final Recycler<ReusableSimpleMessage> simpleMessageRecycler;
    private final Recycler<ReusableObjectMessage> objectMessageRecycler;

    /**
     * Constructs a message factory using the default {@link RecyclerFactory}.
     */
    public ReusableMessageFactory() {
        this(LoggingSystem.getRecyclerFactory());
    }

    public ReusableMessageFactory(final RecyclerFactory recyclerFactory) {
        super();
        parameterizedMessageRecycler =
                recyclerFactory.create(ReusableParameterizedMessage::new, ReusableParameterizedMessage::clear);
        simpleMessageRecycler = recyclerFactory.create(ReusableSimpleMessage::new, ReusableSimpleMessage::clear);
        objectMessageRecycler = recyclerFactory.create(ReusableObjectMessage::new, ReusableObjectMessage::clear);
    }

    /**
     * Invokes {@link ReusableMessage#clear()} when possible.
     * This flag is used internally to verify that a reusable message is no longer in use and
     * can be reused.
     * @param message the message to make available again
     * @since 2.7
     */
    @SuppressWarnings("removal")
    public static void release(final Message message) { // LOG4J2-1583
        if (message instanceof ReusableMessage) {
            ((ReusableMessage) message).clear();
        } else if (message instanceof Clearable) {
            ((Clearable) message).clear();
        }
    }

    @Override
    public void recycle(final Message message) {
        if (message instanceof ReusableMessage) {
            ((ReusableMessage) message).clear();
        }
        // related to LOG4J2-1583 and nested log messages clobbering each other. recycle messages today!
        if (message instanceof ReusableParameterizedMessage) {
            parameterizedMessageRecycler.release((ReusableParameterizedMessage) message);
        } else if (message instanceof ReusableObjectMessage) {
            objectMessageRecycler.release((ReusableObjectMessage) message);
        } else if (message instanceof ReusableSimpleMessage) {
            simpleMessageRecycler.release((ReusableSimpleMessage) message);
        }
    }

    @Override
    public Message newMessage(final CharSequence charSequence) {
        final ReusableSimpleMessage result = simpleMessageRecycler.acquire();
        result.set(charSequence);
        return result;
    }

    /**
     * Creates {@link ReusableParameterizedMessage} instances.
     *
     * @param message The message pattern.
     * @param params The message parameters.
     * @return The Message.
     *
     * @see MessageFactory#newMessage(String, Object...)
     */
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

    /**
     * Creates {@link ReusableSimpleMessage} instances.
     *
     * @param message The message String.
     * @return The Message.
     *
     * @see MessageFactory#newMessage(String)
     */
    @Override
    public Message newMessage(final String message) {
        final ReusableSimpleMessage result = simpleMessageRecycler.acquire();
        result.set(message);
        return result;
    }

    /**
     * Creates {@link ReusableObjectMessage} instances.
     *
     * @param message The message Object.
     * @return The Message.
     *
     * @see MessageFactory#newMessage(Object)
     */
    @Override
    public Message newMessage(final Object message) {
        final ReusableObjectMessage result = objectMessageRecycler.acquire();
        result.set(message);
        return result;
    }
}
