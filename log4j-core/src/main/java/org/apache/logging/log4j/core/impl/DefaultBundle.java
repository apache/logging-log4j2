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

import java.util.Map;
import java.util.function.Supplier;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.DefaultConfigurationFactory;
import org.apache.logging.log4j.core.config.URIConfigurationFactory;
import org.apache.logging.log4j.core.config.composite.DefaultMergeStrategy;
import org.apache.logging.log4j.core.config.composite.MergeStrategy;
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
import org.apache.logging.log4j.message.FlowMessageFactory;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.SingletonFactory;
import org.apache.logging.log4j.plugins.condition.ConditionalOnMissingBinding;
import org.apache.logging.log4j.plugins.di.ConfigurableInstanceFactory;
import org.apache.logging.log4j.spi.CopyOnWrite;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.apache.logging.log4j.spi.LoggingSystem;
import org.apache.logging.log4j.spi.Provider;
import org.apache.logging.log4j.spi.ReadOnlyThreadContextMap;
import org.apache.logging.log4j.spi.recycler.RecyclerFactory;
import org.apache.logging.log4j.status.StatusLogger;

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
public class DefaultBundle {

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
    public MessageFactory defaultMessageFactory() {
        return LoggingSystem.getMessageFactory();
    }

    @SingletonFactory
    @ConditionalOnMissingBinding
    public FlowMessageFactory defaultFlowMessageFactory() {
        return LoggingSystem.getFlowMessageFactory();
    }

    @SingletonFactory
    @ConditionalOnMissingBinding
    public RecyclerFactory defaultRecyclerFactory() {
        return LoggingSystem.getRecyclerFactory();
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

    @SingletonFactory
    @ConditionalOnMissingBinding
    public URIConfigurationFactory configurationFactory() {
        return new DefaultConfigurationFactory();
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
}
