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
import org.apache.logging.log4j.core.config.ConfigurationExtension;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAliases;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Lazy;
import org.apache.logging.log4j.util.LoaderUtil;

@Configurable(printObject = true)
@Plugin("Disruptor")
@PluginAliases("AsyncWaitStrategyFactory")
public final class DisruptorConfiguration extends AbstractLifeCycle implements ConfigurationExtension {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private final AsyncWaitStrategyFactory waitStrategyFactory;
    private final Lazy<AsyncLoggerConfigDisruptor> loggerConfigDisruptor =
            Lazy.lazy(() -> new AsyncLoggerConfigDisruptor(getWaitStrategyFactory()));

    private DisruptorConfiguration(final AsyncWaitStrategyFactory waitStrategyFactory) {
        this.waitStrategyFactory = waitStrategyFactory;
    }

    public AsyncWaitStrategyFactory getWaitStrategyFactory() {
        return waitStrategyFactory;
    }

    public AsyncLoggerConfigDelegate getAsyncLoggerConfigDelegate() {
        return loggerConfigDisruptor.get();
    }

    @Override
    public void start() {
        if (loggerConfigDisruptor.isInitialized()) {
            LOGGER.info("Starting AsyncLoggerConfigDisruptor.");
            loggerConfigDisruptor.get().start();
        }
        super.start();
    }

    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        if (loggerConfigDisruptor.isInitialized()) {
            LOGGER.info("Stopping AsyncLoggerConfigDisruptor.");
            loggerConfigDisruptor.get().stop(timeout, timeUnit);
        }
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

        public Builder setFactoryClassName(final String factoryClassName) {
            this.factoryClassName = factoryClassName;
            return this;
        }

        public Builder setWaitFactory(final String waitFactory) {
            this.waitFactory = waitFactory;
            return this;
        }

        @Override
        public DisruptorConfiguration build() {
            return new DisruptorConfiguration(
                    createWaitStrategyFactory(Objects.toString(waitFactory, factoryClassName)));
        }

        private static AsyncWaitStrategyFactory createWaitStrategyFactory(final String factoryClassName) {
            if (factoryClassName != null) {
                try {
                    final AsyncWaitStrategyFactory asyncWaitStrategyFactory =
                            LoaderUtil.newCheckedInstanceOf(factoryClassName, AsyncWaitStrategyFactory.class);
                    LOGGER.info("Using configured AsyncWaitStrategy factory {}.", factoryClassName);
                    return asyncWaitStrategyFactory;
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
            }
            LOGGER.info("Using default AsyncWaitStrategy factory.");
            return null;
        }
    }
}
