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
package org.apache.logging.log4j.core.impl;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.core.annotation.ConditionalOnPropertyKey;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.composite.MergeStrategy;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.core.util.ShutdownCallbackRegistry;
import org.apache.logging.log4j.plugins.Factory;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.plugins.Ordered;
import org.apache.logging.log4j.plugins.SingletonFactory;
import org.apache.logging.log4j.plugins.condition.ConditionalOnMissingBinding;
import org.apache.logging.log4j.plugins.di.InjectException;
import org.apache.logging.log4j.plugins.di.InstanceFactory;
import org.apache.logging.log4j.util.PropertyEnvironment;
import org.apache.logging.log4j.util.PropertyKey;

/**
 * Provides instance bindings for property-based overrides.
 *
 * @see Log4jPropertyKey
 * @see ContextSelector
 * @see ShutdownCallbackRegistry
 * @see ConfigurationFactory
 * @see MergeStrategy
 * @see ContextDataInjector
 * @see LogEventFactory
 */
public class SystemPropertyBundle {

    private final InstanceFactory instanceFactory;
    private final PropertyEnvironment properties;
    private final ClassLoader classLoader;

    @Inject
    public SystemPropertyBundle(
            final InstanceFactory instanceFactory,
            final PropertyEnvironment properties,
            final ClassLoader classLoader) {
        this.instanceFactory = instanceFactory;
        this.properties = properties;
        this.classLoader = classLoader;
    }

    @ConditionalOnPropertyKey(key = Log4jPropertyKey.CONTEXT_SELECTOR_CLASS_NAME)
    @ConditionalOnMissingBinding
    @SingletonFactory
    public ContextSelector systemPropertyContextSelector() throws ClassNotFoundException {
        return newInstanceOfProperty(Log4jPropertyKey.CONTEXT_SELECTOR_CLASS_NAME, ContextSelector.class);
    }

    @ConditionalOnPropertyKey(key = Log4jPropertyKey.SHUTDOWN_CALLBACK_REGISTRY)
    @ConditionalOnMissingBinding
    @SingletonFactory
    @Ordered(100)
    public ShutdownCallbackRegistry systemPropertyShutdownCallbackRegistry() throws ClassNotFoundException {
        return newInstanceOfProperty(Log4jPropertyKey.SHUTDOWN_CALLBACK_REGISTRY, ShutdownCallbackRegistry.class);
    }

    @ConditionalOnPropertyKey(key = Log4jPropertyKey.THREAD_CONTEXT_DATA_INJECTOR_CLASS_NAME)
    @ConditionalOnMissingBinding
    @Factory
    public ContextDataInjector systemPropertyContextDataInjector() throws ClassNotFoundException {
        return newInstanceOfProperty(
                Log4jPropertyKey.THREAD_CONTEXT_DATA_INJECTOR_CLASS_NAME, ContextDataInjector.class);
    }

    @ConditionalOnPropertyKey(key = Log4jPropertyKey.LOG_EVENT_FACTORY_CLASS_NAME)
    @ConditionalOnMissingBinding
    @SingletonFactory
    public LogEventFactory systemPropertyLogEventFactory() throws ClassNotFoundException {
        return newInstanceOfProperty(Log4jPropertyKey.LOG_EVENT_FACTORY_CLASS_NAME, LogEventFactory.class);
    }

    @ConditionalOnPropertyKey(key = Log4jPropertyKey.CONFIG_MERGE_STRATEGY)
    @ConditionalOnMissingBinding
    @SingletonFactory
    public MergeStrategy systemPropertyMergeStrategy() throws ClassNotFoundException {
        return newInstanceOfProperty(Log4jPropertyKey.CONFIG_MERGE_STRATEGY, MergeStrategy.class);
    }

    @ConditionalOnPropertyKey(key = Log4jPropertyKey.STATUS_DEFAULT_LEVEL)
    @ConditionalOnMissingBinding
    @SingletonFactory
    @Named("StatusLogger")
    public Level systemPropertyDefaultStatusLevel() {
        return Level.getLevel(properties.getStringProperty(Log4jPropertyKey.STATUS_DEFAULT_LEVEL));
    }

    private <T> T newInstanceOfProperty(final PropertyKey propertyKey, final Class<T> supertype)
            throws ClassNotFoundException {
        final String property = properties.getStringProperty(propertyKey);
        if (property == null) {
            throw new InjectException("No property defined for name " + propertyKey.toString());
        }
        return instanceFactory.getInstance(classLoader.loadClass(property).asSubclass(supertype));
    }
}
