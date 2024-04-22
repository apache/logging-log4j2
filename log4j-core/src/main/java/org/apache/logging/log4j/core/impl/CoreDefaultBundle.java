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

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.async.AsyncQueueFullPolicy;
import org.apache.logging.log4j.core.async.AsyncQueueFullPolicyFactory;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.DefaultConfigurationFactory;
import org.apache.logging.log4j.core.config.URIConfigurationFactory;
import org.apache.logging.log4j.core.config.composite.DefaultMergeStrategy;
import org.apache.logging.log4j.core.config.composite.MergeStrategy;
import org.apache.logging.log4j.core.impl.internal.ReusableMessageFactory;
import org.apache.logging.log4j.core.lookup.ConfigurationStrSubstitutor;
import org.apache.logging.log4j.core.lookup.Interpolator;
import org.apache.logging.log4j.core.lookup.InterpolatorFactory;
import org.apache.logging.log4j.core.lookup.RuntimeStrSubstitutor;
import org.apache.logging.log4j.core.lookup.StrLookup;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.selector.ClassLoaderContextSelector;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.core.time.Clock;
import org.apache.logging.log4j.core.time.NanoClock;
import org.apache.logging.log4j.core.time.internal.DummyNanoClock;
import org.apache.logging.log4j.core.util.DefaultShutdownCallbackRegistry;
import org.apache.logging.log4j.core.util.ShutdownCallbackRegistry;
import org.apache.logging.log4j.kit.env.PropertyEnvironment;
import org.apache.logging.log4j.kit.recycler.RecyclerFactory;
import org.apache.logging.log4j.kit.recycler.RecyclerFactoryProvider;
import org.apache.logging.log4j.kit.recycler.RecyclerProperties;
import org.apache.logging.log4j.message.DefaultFlowMessageFactory;
import org.apache.logging.log4j.message.FlowMessageFactory;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.plugins.Factory;
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.SingletonFactory;
import org.apache.logging.log4j.plugins.condition.ConditionalOnMissingBinding;
import org.apache.logging.log4j.plugins.di.ConfigurableInstanceFactory;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.apache.logging.log4j.spi.Provider;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.ServiceLoaderUtil;

/**
 * Provides instance binding defaults.
 *
 * @see ContextSelector
 * @see ShutdownCallbackRegistry
 * @see NanoClock
 * @see ConfigurationFactory
 * @see MergeStrategy
 * @see InterpolatorFactory
 * @see LogEventFactory
 * @see StrSubstitutor
 */
public final class CoreDefaultBundle {

    @SingletonFactory
    @ConditionalOnMissingBinding
    public Provider provider(final ConfigurableInstanceFactory instanceFactory) {
        return new Log4jProvider(instanceFactory);
    }

    @SingletonFactory
    @ConditionalOnMissingBinding
    public LoggerContextFactory loggerContextFactory(final ConfigurableInstanceFactory instanceFactory) {
        return new Log4jContextFactory(instanceFactory);
    }

    @SingletonFactory
    @ConditionalOnMissingBinding
    public MessageFactory defaultMessageFactory(final RecyclerFactory recyclerFactory) {
        return new ReusableMessageFactory(recyclerFactory);
    }

    @SingletonFactory
    @ConditionalOnMissingBinding
    public FlowMessageFactory defaultFlowMessageFactory() {
        return new DefaultFlowMessageFactory();
    }

    @SingletonFactory
    @ConditionalOnMissingBinding
    public RecyclerFactoryProvider defaultRecyclerFactoryProvider(
            final PropertyEnvironment environment,
            final ClassLoader loader,
            final @Named("StatusLogger") org.apache.logging.log4j.Logger statusLogger) {
        final String factory = environment.getProperty(RecyclerProperties.class).factory();
        final Stream<RecyclerFactoryProvider> providerStream = ServiceLoaderUtil.safeStream(
                RecyclerFactoryProvider.class, ServiceLoader.load(RecyclerFactoryProvider.class, loader), statusLogger);
        final Optional<RecyclerFactoryProvider> provider = factory != null
                ? providerStream.filter(p -> factory.equals(p.getName())).findAny()
                : providerStream.min(Comparator.comparing(RecyclerFactoryProvider::getOrder));
        return provider.orElseGet(RecyclerFactoryProvider::getInstance);
    }

    @SingletonFactory
    @ConditionalOnMissingBinding
    public RecyclerFactory defaultRecyclerFactory(
            final PropertyEnvironment environment, final RecyclerFactoryProvider provider) {
        return provider.createForEnvironment(environment);
    }

    @SingletonFactory
    @ConditionalOnMissingBinding
    public ContextSelector defaultContextSelector(final ConfigurableInstanceFactory instanceFactory) {
        return new ClassLoaderContextSelector(instanceFactory);
    }

    @SingletonFactory
    @ConditionalOnMissingBinding
    public ShutdownCallbackRegistry defaultShutdownCallbackRegistry() {
        return new DefaultShutdownCallbackRegistry();
    }

    @SingletonFactory
    @ConditionalOnMissingBinding
    public NanoClock defaultNanoClock() {
        return new DummyNanoClock();
    }

    @SingletonFactory
    @ConditionalOnMissingBinding
    public LogEventFactory reusableLogEventFactory(
            final Clock clock, final NanoClock nanoClock, final RecyclerFactory recyclerFactory) {
        return new ReusableLogEventFactory(clock, nanoClock, recyclerFactory);
    }

    @SingletonFactory
    @ConditionalOnMissingBinding
    public InterpolatorFactory interpolatorFactory(
            @Namespace(StrLookup.CATEGORY) final Map<String, Supplier<StrLookup>> strLookupPlugins) {
        return defaultLookup -> new Interpolator(defaultLookup, strLookupPlugins);
    }

    @SingletonFactory
    @ConditionalOnMissingBinding
    public ConfigurationStrSubstitutor configurationStrSubstitutor(final InterpolatorFactory factory) {
        return new ConfigurationStrSubstitutor(factory.newInterpolator(null));
    }

    @SingletonFactory
    @ConditionalOnMissingBinding
    public RuntimeStrSubstitutor runtimeStrSubstitutor(final InterpolatorFactory factory) {
        return new RuntimeStrSubstitutor(factory.newInterpolator(null));
    }

    /**
     * Spring Boot needs a ConfigurationFactory for compatibility.
     */
    @SingletonFactory
    @ConditionalOnMissingBinding
    public ConfigurationFactory configurationFactory() {
        return new DefaultConfigurationFactory();
    }

    @SingletonFactory
    @ConditionalOnMissingBinding
    public URIConfigurationFactory configurationFactory(final ConfigurationFactory factory) {
        return factory;
    }

    @SingletonFactory
    @ConditionalOnMissingBinding
    public MergeStrategy defaultMergeStrategy() {
        return new DefaultMergeStrategy();
    }

    @SingletonFactory
    @Named("StatusLogger")
    @ConditionalOnMissingBinding
    public Level defaultStatusLevel() {
        return Level.ERROR;
    }

    @SingletonFactory
    @Named("StatusLogger")
    @ConditionalOnMissingBinding
    public Logger defaultStatusLogger() {
        return StatusLogger.getLogger();
    }

    @Factory
    @ConditionalOnMissingBinding
    public AsyncQueueFullPolicy asyncQueueFullPolicy(
            final PropertyEnvironment environment, final @Named("StatusLogger") Logger statusLogger) {
        return AsyncQueueFullPolicyFactory.create(
                environment.getProperty(CoreProperties.QueueFullPolicyProperties.class), statusLogger);
    }
}
