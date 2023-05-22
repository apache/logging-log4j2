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
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.DefaultConfigurationFactory;
import org.apache.logging.log4j.core.config.composite.DefaultMergeStrategy;
import org.apache.logging.log4j.core.config.composite.MergeStrategy;
import org.apache.logging.log4j.core.lookup.Interpolator;
import org.apache.logging.log4j.core.lookup.InterpolatorFactory;
import org.apache.logging.log4j.core.lookup.StrLookup;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.selector.ClassLoaderContextSelector;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.core.time.Clock;
import org.apache.logging.log4j.core.time.NanoClock;
import org.apache.logging.log4j.core.time.internal.DummyNanoClock;
import org.apache.logging.log4j.core.util.DefaultShutdownCallbackRegistry;
import org.apache.logging.log4j.core.util.ShutdownCallbackRegistry;
import org.apache.logging.log4j.plugins.Factory;
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.SingletonFactory;
import org.apache.logging.log4j.plugins.condition.ConditionalOnMissingBinding;
import org.apache.logging.log4j.plugins.di.ConfigurableInstanceFactory;
import org.apache.logging.log4j.spi.CopyOnWrite;
import org.apache.logging.log4j.spi.DefaultThreadContextMap;
import org.apache.logging.log4j.spi.ReadOnlyThreadContextMap;

import static org.apache.logging.log4j.util.Constants.isThreadLocalsEnabled;

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

    @Factory
    @ConditionalOnMissingBinding
    public ContextDataInjector defaultContextDataInjector() {
        final ReadOnlyThreadContextMap threadContextMap = ThreadContext.getThreadContextMap();

        // note: map may be null (if legacy custom ThreadContextMap was installed by user)
        if (threadContextMap instanceof DefaultThreadContextMap || threadContextMap == null) {
            // for non StringMap-based context maps
            return new ThreadContextDataInjector.ForDefaultThreadContextMap();
        }
        if (threadContextMap instanceof CopyOnWrite) {
            return new ThreadContextDataInjector.ForCopyOnWriteThreadContextMap();
        }
        return new ThreadContextDataInjector.ForGarbageFreeThreadContextMap();
    }

    @SingletonFactory
    @ConditionalOnMissingBinding
    public LogEventFactory defaultLogEventFactory(
            final ContextDataInjector injector, final Clock clock, final NanoClock nanoClock) {
        return isThreadLocalsEnabled() ? new ReusableLogEventFactory(injector, clock, nanoClock) :
                new DefaultLogEventFactory(injector, clock, nanoClock);
    }

    @SingletonFactory
    @ConditionalOnMissingBinding
    public InterpolatorFactory interpolatorFactory(
            @Namespace(StrLookup.CATEGORY) final Map<String, Supplier<StrLookup>> strLookupPlugins) {
        return defaultLookup -> new Interpolator(defaultLookup, strLookupPlugins);
    }

    @SingletonFactory
    @ConditionalOnMissingBinding
    public StrSubstitutor strSubstitutor(final InterpolatorFactory factory) {
        return new StrSubstitutor(factory.newInterpolator(null));
    }

    @SingletonFactory
    @ConditionalOnMissingBinding
    public ConfigurationFactory configurationFactory(final ConfigurableInstanceFactory instanceFactory) {
        return new DefaultConfigurationFactory(instanceFactory);
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

}
