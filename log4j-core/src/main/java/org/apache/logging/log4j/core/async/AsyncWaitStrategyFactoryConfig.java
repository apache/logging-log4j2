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

import java.util.Objects;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.plugins.validation.constraints.Required;
import org.apache.logging.log4j.spi.ClassFactory;
import org.apache.logging.log4j.spi.InstanceFactory;
import org.apache.logging.log4j.spi.LoggingSystem;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Cast;

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
    private final ClassFactory classFactory;
    private final InstanceFactory instanceFactory;

    public AsyncWaitStrategyFactoryConfig(final String factoryClassName, final ClassFactory classFactory,
                                          final InstanceFactory instanceFactory) {
        this.factoryClassName = Objects.requireNonNull(factoryClassName, "factoryClassName");
        this.classFactory = classFactory;
        this.instanceFactory = instanceFactory;
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
            implements org.apache.logging.log4j.plugins.util.Builder<AsyncWaitStrategyFactoryConfig> {


        private String factoryClassName;
        private ClassFactory classFactory;
        private InstanceFactory instanceFactory;

        public String getFactoryClassName() {
            return factoryClassName;
        }

        @Inject
        public B setFactoryClassName(
                @PluginBuilderAttribute("class")
                @Required(message = "AsyncWaitStrategyFactory cannot be configured without a factory class name")
                String className) {
            this.factoryClassName = className;
            return asBuilder();
        }

        public ClassFactory getClassFactory() {
            if (classFactory == null) {
                classFactory = LoggingSystem.getInstance().getClassFactory();
            }
            return classFactory;
        }

        @Inject
        public B setClassFactory(final ClassFactory classFactory) {
            this.classFactory = classFactory;
            return asBuilder();
        }

        public InstanceFactory getInstanceFactory() {
            if (instanceFactory == null) {
                return LoggingSystem.getInstance().getInstanceFactory();
            }
            return instanceFactory;
        }

        @Inject
        public B setInstanceFactory(final InstanceFactory instanceFactory) {
            this.instanceFactory = instanceFactory;
            return asBuilder();
        }

        @Override
        public AsyncWaitStrategyFactoryConfig build() {
            return new AsyncWaitStrategyFactoryConfig(getFactoryClassName(), getClassFactory(), getInstanceFactory());
        }

        public B asBuilder() {
            return Cast.cast(this);
        }
    }

    public AsyncWaitStrategyFactory createWaitStrategyFactory() {
        return classFactory.tryGetClass(factoryClassName, AsyncWaitStrategyFactory.class)
                .flatMap(instanceFactory::tryGetInstance)
                .orElse(null);
    }
}
