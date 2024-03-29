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
package org.apache.logging.log4j.core.async;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.impl.CoreProperties.QueueFullPolicyProperties;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;

/**
 * Creates {@link AsyncQueueFullPolicy} instances based on user-specified system properties. The {@code AsyncQueueFullPolicy}
 * created by this factory is used in AsyncLogger, AsyncLoggerConfig and AsyncAppender
 * to control if events are logged in the current thread, the background thread, or discarded.
 * <p>
 * Property {@link QueueFullPolicyProperties#type()} controls the routing behaviour. If this property is
 * not specified or has
 * value {@code "Default"}, this factory creates {@link DefaultAsyncQueueFullPolicy} objects.
 * </p> <p>
 * If this property has value {@code "Discard"}, this factory creates {@link DiscardingAsyncQueueFullPolicy} objects.
 * By default, this router discards events of level {@code INFO}, {@code DEBUG} and {@code TRACE} if the queue is full.
 * This can be adjusted with property {@link QueueFullPolicyProperties#discardThreshold()} (name of the level at
 * which to start
 * discarding).
 * </p> <p>
 * For any other value, this
 * factory interprets the value as the fully qualified name of a class implementing the {@link AsyncQueueFullPolicy}
 * interface. The class must have a default constructor.
 * </p>
 *
 * @since 2.6
 */
public final class AsyncQueueFullPolicyFactory {
    static final String DEFAULT_POLICY = "Default";
    static final String DISCARDING_POLICY = "Discard";

    private static final Logger LOGGER = StatusLogger.getLogger();

    private AsyncQueueFullPolicyFactory() {}

    /**
     * Creates and returns {@link AsyncQueueFullPolicy} instances based on user-specified system properties.
     * <p>
     * Property {@code "log4j2.AsyncQueueFullPolicy"} controls the routing behaviour. If this property is not specified or
     * has value {@code "Default"}, this method returns {@link DefaultAsyncQueueFullPolicy} objects.
     * </p> <p>
     * If this property has value {@code "Discard"}, this method returns {@link DiscardingAsyncQueueFullPolicy} objects.
     * </p> <p>
     * For any other value, this method interprets the value as the fully qualified name of a class implementing the
     * {@link AsyncQueueFullPolicy} interface. The class must have a default constructor.
     * </p>
     *
     * @return a new AsyncQueueFullPolicy
     */
    public static AsyncQueueFullPolicy create(final QueueFullPolicyProperties policy, final Logger statusLogger) {
        if (policy.type() != null) {
            if (isPolicySelected(policy.type(), DefaultAsyncQueueFullPolicy.class, DEFAULT_POLICY)) {
                return new DefaultAsyncQueueFullPolicy();
            }
            if (isPolicySelected(policy.type(), DiscardingAsyncQueueFullPolicy.class, DISCARDING_POLICY)) {
                return createDiscardingAsyncQueueFullPolicy(policy, statusLogger);
            }
            return createCustomPolicy(policy.type(), statusLogger);
        }
        return new DefaultAsyncQueueFullPolicy();
    }

    private static boolean isPolicySelected(
            final String propertyValue,
            final Class<? extends AsyncQueueFullPolicy> policy,
            final String shortPropertyValue) {
        return shortPropertyValue.equalsIgnoreCase(propertyValue)
                || policy.getName().equals(propertyValue)
                || policy.getSimpleName().equals(propertyValue);
    }

    private static AsyncQueueFullPolicy createCustomPolicy(final String policyType, final Logger statusLogger) {
        try {
            statusLogger.debug("Creating custom AsyncQueueFullPolicy '{}'", policyType);
            return LoaderUtil.newCheckedInstanceOf(policyType, AsyncQueueFullPolicy.class);
        } catch (final Exception ex) {
            statusLogger.debug(
                    "Using DefaultAsyncQueueFullPolicy. Could not create custom AsyncQueueFullPolicy '{}': {}",
                    policyType,
                    ex.getMessage(),
                    ex);
            return new DefaultAsyncQueueFullPolicy();
        }
    }

    private static AsyncQueueFullPolicy createDiscardingAsyncQueueFullPolicy(
            final QueueFullPolicyProperties policy, final Logger statusLogger) {
        statusLogger.debug(
                "Creating custom DiscardingAsyncQueueFullPolicy(discardThreshold:{})", policy.discardThreshold());
        return new DiscardingAsyncQueueFullPolicy(policy.discardThreshold());
    }
}
