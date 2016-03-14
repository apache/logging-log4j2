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

import java.io.Serializable;

import org.apache.logging.log4j.util.PerformanceSensitive;
import org.apache.logging.log4j.util.ThreadLocalRegistry;
import org.apache.logging.log4j.util.ThreadLocalRegistryAware;

/**
 * Implementation of the {@link MessageFactory} interface that avoids allocating temporary objects where possible.
 * Message instances are cached in a ThreadLocal and reused when a new message is requested within the same thread.
 * @see ParameterizedMessageFactory
 * @see ReusableSimpleMessage
 * @see ReusableObjectMessage
 * @see ReusableParameterizedMessage
 * @since 2.6
 */
@PerformanceSensitive("allocation")
public final class ReusableMessageFactory implements MessageFactory, Serializable, ThreadLocalRegistryAware {

    private static final long serialVersionUID = -8970940216592525651L;
    // @TODO - The ThreadLocals need to be restored on deserialization.
    private transient ThreadLocal<ReusableParameterizedMessage> threadLocalParameterized = null;
    private transient ThreadLocal<ReusableSimpleMessage> threadLocalSimpleMessage = null;
    private transient ThreadLocal<ReusableObjectMessage> threadLocalObjectMessage = null;

    /**
     * Constructs a message factory.
     * @param registry The ThreadLocalRegistry
     */
    @SuppressWarnings("unchecked")
    public ReusableMessageFactory(ThreadLocalRegistry registry) {
        super();
        if (registry != null) {
            threadLocalParameterized = (ThreadLocal<ReusableParameterizedMessage>) registry.get("ParameterizedMessage");
            threadLocalSimpleMessage = (ThreadLocal<ReusableSimpleMessage>) registry.get("SimpleMessage");
            threadLocalObjectMessage = (ThreadLocal<ReusableObjectMessage>) registry.get("ObjectMessage");
        }
    }

    public ReusableMessageFactory() {
    }

    private ReusableParameterizedMessage getParameterized() {
        ReusableParameterizedMessage result = threadLocalParameterized.get();
        if (result == null) {
            result = new ReusableParameterizedMessage();
            threadLocalParameterized.set(result);
        }
        return result;
    }

    private ReusableSimpleMessage getSimple() {
        ReusableSimpleMessage result = threadLocalSimpleMessage.get();
        if (result == null) {
            result = new ReusableSimpleMessage();
            threadLocalSimpleMessage.set(result);
        }
        return result;
    }

    private ReusableObjectMessage getObject() {
        ReusableObjectMessage result = threadLocalObjectMessage.get();
        if (result == null) {
            result = new ReusableObjectMessage();
            threadLocalObjectMessage.set(result);
        }
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
        return threadLocalParameterized != null ? getParameterized().set(message, params) :
                new ParameterizedMessage(message, params);
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
        if (threadLocalSimpleMessage != null) {
            ReusableSimpleMessage result = getSimple();
            result.set(message);
            return result;
        }
        return new SimpleMessage(message);
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
        if (threadLocalObjectMessage != null) {
            ReusableObjectMessage result = getObject();
            result.set(message);
            return result;
        }
        return new ObjectMessage(message);
    }
}
