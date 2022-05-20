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
package org.apache.logging.log4j.core.async;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.plugins.validation.constraints.Required;
import org.apache.logging.log4j.status.StatusLogger;

import java.util.Objects;

/**
 * This class allows users to configure the factory used to create
 * an instance of the LMAX disruptor WaitStrategy
 * used by Async Loggers in the log4j configuration.
 */
@Configurable(printObject = true)
@Plugin("AsyncWaitStrategyFactory")
public class AsyncWaitStrategyFactoryConfig {

    /**
     * Status logger for internal logging.
     */
    protected static final Logger LOGGER = StatusLogger.getLogger();

    private final String factoryClassName;

    public AsyncWaitStrategyFactoryConfig(final String factoryClassName) {
        this.factoryClassName = Objects.requireNonNull(factoryClassName, "factoryClassName");
    }

    @PluginFactory
    public static <B extends AsyncWaitStrategyFactoryConfig.Builder<B>> B newBuilder() {
        return new AsyncWaitStrategyFactoryConfig.Builder<B>().asBuilder();
    }

    /**
     * Builds AsyncWaitStrategyFactoryConfig instances.
     *
     * @param <B>
     *            The type to build
     */
    public static class Builder<B extends AsyncWaitStrategyFactoryConfig.Builder<B>>
            implements org.apache.logging.log4j.core.util.Builder<AsyncWaitStrategyFactoryConfig> {

        @PluginBuilderAttribute("class")
        @Required(message = "AsyncWaitStrategyFactory cannot be configured without a factory class name")
        private String factoryClassName;

        public String getFactoryClassName() {
            return factoryClassName;
        }

        public B withFactoryClassName(String className) {
            this.factoryClassName = className;
            return asBuilder();
        }

        @Override
        public AsyncWaitStrategyFactoryConfig build() {
            return new AsyncWaitStrategyFactoryConfig(factoryClassName);
        }

        @SuppressWarnings("unchecked")
        public B asBuilder() {
            return (B) this;
        }
    }

    public AsyncWaitStrategyFactory createWaitStrategyFactory() {
        try {
            @SuppressWarnings("unchecked")
            final Class<? extends AsyncWaitStrategyFactory> klass = (Class<? extends AsyncWaitStrategyFactory>) Loader.loadClass(factoryClassName);
            if (AsyncWaitStrategyFactory.class.isAssignableFrom(klass)) {
                return klass.newInstance();
            }
            LOGGER.error("Ignoring factory '{}': it is not assignable to AsyncWaitStrategyFactory", factoryClassName);
            return null;
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            LOGGER.info("Invalid implementation class name value: error creating AsyncWaitStrategyFactory {}: {}", factoryClassName, e);
            return null;
        }

    }
}
