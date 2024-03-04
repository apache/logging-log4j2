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
package org.apache.logging.log4j.async.logger;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.AbstractLifeCycle;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationExtension;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAliases;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Lazy;
import org.apache.logging.log4j.util.LoaderUtil;
import org.jspecify.annotations.Nullable;

/**
 * A container for:
 * <ol>
 *     <li>A user provided wait strategy factory.</li>
 *     <li>The common {@link AsyncLoggerConfigDisruptor} instance shared by all logger configs.</li>
 * </ol>
 * TODO: the only reason the disruptor needs a holder is that
 * {@link org.apache.logging.log4j.plugins.di.InstanceFactory} is currently unable to stop the services it creates.
 * In the future the disruptor will be in the instance factory.
 */
@Configurable(printObject = true)
@Plugin("Disruptor")
@PluginAliases("AsyncWaitStrategyFactory")
public final class DisruptorConfiguration extends AbstractLifeCycle implements ConfigurationExtension {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private final @Nullable AsyncWaitStrategyFactory waitStrategyFactory;
    private final Lazy<AsyncLoggerConfigDisruptor> loggerConfigDisruptor;

    private DisruptorConfiguration(
            final @Nullable AsyncWaitStrategyFactory waitStrategyFactory, final Configuration configuration) {
        this.waitStrategyFactory = waitStrategyFactory;
        this.loggerConfigDisruptor =
                Lazy.lazy(() -> configuration.getComponent(Key.forClass(AsyncLoggerConfigDisruptor.class)));
    }

    public @Nullable AsyncWaitStrategyFactory getWaitStrategyFactory() {
        return waitStrategyFactory;
    }

    AsyncLoggerConfigDisruptor getLoggerConfigDisruptor() {
        return loggerConfigDisruptor.get();
    }

    @Override
    public void start() {
        LOGGER.info("Starting AsyncLoggerConfigDisruptor.");
        loggerConfigDisruptor.get().start();
        super.start();
    }

    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        LOGGER.info("Stopping AsyncLoggerConfigDisruptor.");
        loggerConfigDisruptor.get().stop(timeout, timeUnit);
        return super.stop(timeout, timeUnit);
    }

    @PluginFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder implements org.apache.logging.log4j.plugins.util.Builder<DisruptorConfiguration> {

        @PluginBuilderAttribute("class")
        private String factoryClassName;

        @PluginBuilderAttribute
        private String waitFactory;

        private Configuration configuration;

        public Builder setFactoryClassName(final String factoryClassName) {
            this.factoryClassName = factoryClassName;
            return this;
        }

        public Builder setWaitFactory(final String waitFactory) {
            this.waitFactory = waitFactory;
            return this;
        }

        @Inject
        public Builder setConfiguration(final Configuration configuration) {
            this.configuration = configuration;
            return this;
        }

        @Override
        public DisruptorConfiguration build() {
            final String factoryClassName = Objects.toString(waitFactory, this.factoryClassName);
            if (factoryClassName != null) {
                try {
                    final AsyncWaitStrategyFactory asyncWaitStrategyFactory =
                            LoaderUtil.newCheckedInstanceOf(factoryClassName, AsyncWaitStrategyFactory.class);
                    LOGGER.info("Using configured AsyncWaitStrategy factory {}.", factoryClassName);
                    return new DisruptorConfiguration(asyncWaitStrategyFactory, configuration);
                } catch (final ClassCastException e) {
                    LOGGER.error(
                            "Ignoring factory '{}': it is not assignable to AsyncWaitStrategyFactory",
                            factoryClassName);
                } catch (final ReflectiveOperationException | LinkageError e) {
                    LOGGER.warn(
                            "Invalid implementation class name value: error creating AsyncWaitStrategyFactory {}: {}",
                            factoryClassName,
                            e.getMessage(),
                            e);
                }
            } else {
                LOGGER.info("Using default AsyncWaitStrategy factory.");
            }
            return new DisruptorConfiguration(null, configuration);
        }
    }
}
