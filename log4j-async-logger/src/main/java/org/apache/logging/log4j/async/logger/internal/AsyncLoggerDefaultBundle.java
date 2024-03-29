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
package org.apache.logging.log4j.async.logger.internal;

import com.lmax.disruptor.WaitStrategy;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.async.logger.AsyncLoggerConfigDisruptor;
import org.apache.logging.log4j.async.logger.AsyncLoggerDisruptor;
import org.apache.logging.log4j.async.logger.AsyncLoggerProperties;
import org.apache.logging.log4j.async.logger.AsyncWaitStrategyFactory;
import org.apache.logging.log4j.async.logger.DisruptorConfiguration;
import org.apache.logging.log4j.core.async.AsyncQueueFullPolicy;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.impl.LogEventFactory;
import org.apache.logging.log4j.kit.env.PropertyEnvironment;
import org.apache.logging.log4j.plugins.Factory;
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.plugins.SingletonFactory;
import org.apache.logging.log4j.plugins.condition.ConditionalOnMissingBinding;
import org.apache.logging.log4j.plugins.condition.ConditionalOnPresentBindings;

/**
 * Provides default services for the per-context instance factory.
 */
public final class AsyncLoggerDefaultBundle {

    @Factory
    @Named("AsyncLogger")
    @ConditionalOnMissingBinding
    public WaitStrategy asyncLoggerWaitStrategy(
            final PropertyEnvironment environment, final @Named("StatusLogger") Logger statusLogger) {
        statusLogger.debug("No AsyncWaitStrategyFactory was configured in the configuration, using default factory...");
        return new DefaultAsyncWaitStrategyFactory(
                        environment.getProperty(AsyncLoggerProperties.class).waitStrategy())
                .createWaitStrategy();
    }

    @SingletonFactory
    @ConditionalOnMissingBinding
    public AsyncLoggerDisruptor.Factory asyncLoggerDisruptorFactory(
            final AsyncQueueFullPolicy asyncQueueFullPolicy,
            final @Named("AsyncLogger") WaitStrategy waitStrategy,
            final PropertyEnvironment environment) {
        return contextName -> new AsyncLoggerDisruptor(
                contextName, asyncQueueFullPolicy, waitStrategy, environment.getProperty(AsyncLoggerProperties.class));
    }

    /**
     * The {@link WaitStrategy} for an {@link org.apache.logging.log4j.async.logger.AsyncLoggerConfig} can be also
     * configured in a configuration file, whereas the strategy for an
     * {@link org.apache.logging.log4j.async.logger.AsyncLogger} must be configured through properties only.
     * @see #asyncLoggerWaitStrategy
     */
    @Factory
    @Named("AsyncLoggerConfig")
    @ConditionalOnPresentBindings(bindings = Configuration.class)
    public WaitStrategy defaultAsyncLoggerWaitStrategy(
            final Configuration configuration,
            final PropertyEnvironment environment,
            final @Named("StatusLogger") Logger statusLogger) {
        final DisruptorConfiguration disruptorConfiguration = configuration.getExtension(DisruptorConfiguration.class);
        if (disruptorConfiguration != null) {
            final AsyncWaitStrategyFactory factory = disruptorConfiguration.getWaitStrategyFactory();
            if (factory != null) {
                statusLogger.debug(
                        "Using configured AsyncWaitStrategyFactory {}",
                        factory.getClass().getName());
                return factory.createWaitStrategy();
            }
        }
        return asyncLoggerWaitStrategy(environment, statusLogger);
    }

    @Factory
    @ConditionalOnPresentBindings(bindings = Configuration.class)
    public AsyncLoggerConfigDisruptor asyncLoggerConfigDisruptor(
            final AsyncQueueFullPolicy asyncQueueFullPolicy,
            final @Named("AsyncLoggerConfig") WaitStrategy waitStrategy,
            final LogEventFactory logEventFactory,
            final PropertyEnvironment environment) {
        return new AsyncLoggerConfigDisruptor(
                asyncQueueFullPolicy,
                waitStrategy,
                logEventFactory,
                environment.getProperty(AsyncLoggerProperties.class));
    }
}
