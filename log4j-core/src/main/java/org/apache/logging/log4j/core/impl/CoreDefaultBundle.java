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

import aQute.bnd.annotation.Cardinality;
import aQute.bnd.annotation.spi.ServiceConsumer;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.ContextDataInjector;
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
import org.apache.logging.log4j.core.selector.BasicContextSelector;
import org.apache.logging.log4j.core.selector.ClassLoaderContextSelector;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.core.time.Clock;
import org.apache.logging.log4j.core.time.NanoClock;
import org.apache.logging.log4j.core.time.internal.DummyNanoClock;
import org.apache.logging.log4j.core.util.DefaultShutdownCallbackRegistry;
import org.apache.logging.log4j.core.util.ShutdownCallbackRegistry;
import org.apache.logging.log4j.core.util.internal.SystemUtils;
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
import org.apache.logging.log4j.spi.CopyOnWrite;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.apache.logging.log4j.spi.Provider;
import org.apache.logging.log4j.spi.ReadOnlyThreadContextMap;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.ServiceLoaderUtil;
import org.jspecify.annotations.Nullable;

/**
 * Provides instance binding defaults.
 *
 * @see ContextSelector
 * @see ShutdownCallbackRegistry
 * @see NanoClock
 * @see ConfigurationFactory
 * @see MergeStrategy
 * @see InterpolatorFactory
 * @see ContextDataInjector
 * @see LogEventFactory
 * @see StrSubstitutor
 */
@ServiceConsumer(value = RecyclerFactoryProvider.class, cardinality = Cardinality.MULTIPLE)
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
    public RecyclerFactory defaultRecyclerFactory(
            final PropertyEnvironment environment,
            final ClassLoader loader,
            final @Named("StatusLogger") org.apache.logging.log4j.Logger statusLogger) {

        // Collect providers
        @Nullable
        final String providerName =
                environment.getProperty(RecyclerProperties.class).factory();
        final List<RecyclerFactoryProvider> providers = ServiceLoaderUtil.safeStream(
                        RecyclerFactoryProvider.class,
                        ServiceLoader.load(RecyclerFactoryProvider.class, loader),
                        statusLogger)
                .sorted(Comparator.comparing(RecyclerFactoryProvider::getOrder))
                .toList();
        final String providerNames = providers.stream()
                .map(provider -> "`" + provider.getName() + "`")
                .collect(Collectors.joining(", "));

        // Try to create the configured provider
        if (providerName != null) {
            @Nullable
            final RecyclerFactoryProvider matchingProvider = providers.stream()
                    .filter(provider -> provider.getName().equals(providerName))
                    .findFirst()
                    .orElse(null);
            if (matchingProvider != null) {
                return matchingProvider.createForEnvironment(environment);
            } else {
                statusLogger.error(
                        "Configured recycler factory provider `{}` is not found! Available recycler factory providers: {}. Will choose the first one available for the current environment.",
                        providerName,
                        providerNames);
            }
        }

        // Fallback to the first available provider
        return providers.stream()
                .map(provider -> provider.createForEnvironment(environment))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "None of the available recycler factory providers are found to be available for the current environment: "
                                + providerNames));
    }

    @SingletonFactory
    @ConditionalOnMissingBinding
    public ContextSelector defaultContextSelector(final ConfigurableInstanceFactory instanceFactory) {
        return SystemUtils.isOsAndroid()
                ? new BasicContextSelector(instanceFactory)
                : new ClassLoaderContextSelector(instanceFactory);
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
    public ContextDataInjector defaultContextDataInjector() {
        final ReadOnlyThreadContextMap threadContextMap = ThreadContext.getThreadContextMap();
        if (threadContextMap != null) {
            return threadContextMap instanceof CopyOnWrite
                    ? new ThreadContextDataInjector.ForCopyOnWriteThreadContextMap()
                    : new ThreadContextDataInjector.ForGarbageFreeThreadContextMap();
        }
        // for non StringMap-based context maps
        return new ThreadContextDataInjector.ForDefaultThreadContextMap();
    }

    @SingletonFactory
    @ConditionalOnMissingBinding
    public LogEventFactory reusableLogEventFactory(
            final ContextDataInjector injector,
            final Clock clock,
            final NanoClock nanoClock,
            final RecyclerFactory recyclerFactory) {
        return new ReusableLogEventFactory(injector, clock, nanoClock, recyclerFactory);
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
    public Level defaultStatusLevel(PropertyEnvironment environment) {
        return environment
                .getProperty(CoreProperties.StatusLoggerProperties.class)
                .level();
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
