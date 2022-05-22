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

package org.apache.logging.log4j.core.impl;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.DefaultConfigurationFactory;
import org.apache.logging.log4j.core.config.composite.CompositeConfiguration;
import org.apache.logging.log4j.core.config.composite.DefaultMergeStrategy;
import org.apache.logging.log4j.core.config.composite.MergeStrategy;
import org.apache.logging.log4j.core.lookup.Interpolator;
import org.apache.logging.log4j.core.lookup.InterpolatorFactory;
import org.apache.logging.log4j.core.lookup.StrLookup;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.selector.ClassLoaderContextSelector;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.core.time.Clock;
import org.apache.logging.log4j.core.time.ClockFactory;
import org.apache.logging.log4j.core.time.NanoClock;
import org.apache.logging.log4j.core.time.PreciseClock;
import org.apache.logging.log4j.core.time.internal.CachedClock;
import org.apache.logging.log4j.core.time.internal.CoarseCachedClock;
import org.apache.logging.log4j.core.time.internal.DummyNanoClock;
import org.apache.logging.log4j.core.time.internal.SystemClock;
import org.apache.logging.log4j.core.time.internal.SystemMillisClock;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.DefaultShutdownCallbackRegistry;
import org.apache.logging.log4j.core.util.ShutdownCallbackRegistry;
import org.apache.logging.log4j.plugins.Factory;
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Ordered;
import org.apache.logging.log4j.plugins.SingletonFactory;
import org.apache.logging.log4j.plugins.condition.ConditionalOnMissingBinding;
import org.apache.logging.log4j.plugins.condition.ConditionalOnProperty;
import org.apache.logging.log4j.plugins.di.InjectException;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.plugins.util.PluginNamespace;
import org.apache.logging.log4j.spi.CopyOnWrite;
import org.apache.logging.log4j.spi.DefaultThreadContextMap;
import org.apache.logging.log4j.spi.ReadOnlyThreadContextMap;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;

import static org.apache.logging.log4j.util.Constants.isThreadLocalsEnabled;

/**
 * Contains default bindings for Log4j including support for {@link PropertiesUtil}-based configuration.
 *
 * @see Constants
 * @see ContextSelector
 * @see ShutdownCallbackRegistry
 * @see Clock
 * @see NanoClock
 * @see ConfigurationFactory
 * @see MergeStrategy
 * @see InterpolatorFactory
 * @see ContextDataInjector
 * @see LogEventFactory
 * @see StrSubstitutor
 */
public class DefaultBundle {
    private static final Logger LOGGER = StatusLogger.getLogger();

    private final Injector injector;
    private final PropertiesUtil properties;
    private final ClassLoader classLoader;

    public DefaultBundle(final Injector injector, final PropertiesUtil properties, final ClassLoader classLoader) {
        this.injector = injector;
        this.properties = properties;
        this.classLoader = classLoader;
    }

    @ConditionalOnProperty(name = Constants.LOG4J_CONTEXT_SELECTOR)
    @SingletonFactory
    @Ordered(-100)
    public ContextSelector systemPropertyContextSelector() throws ClassNotFoundException {
        return newInstanceOfProperty(Constants.LOG4J_CONTEXT_SELECTOR, ContextSelector.class);
    }

    @ConditionalOnMissingBinding
    @SingletonFactory
    public ContextSelector defaultContextSelector() {
        return new ClassLoaderContextSelector(injector);
    }

    @ConditionalOnProperty(name = ShutdownCallbackRegistry.SHUTDOWN_CALLBACK_REGISTRY)
    @SingletonFactory
    @Ordered(-100)
    public ShutdownCallbackRegistry systemPropertyShutdownCallbackRegistry() throws ClassNotFoundException {
        return newInstanceOfProperty(ShutdownCallbackRegistry.SHUTDOWN_CALLBACK_REGISTRY, ShutdownCallbackRegistry.class);
    }

    @ConditionalOnMissingBinding
    @SingletonFactory
    public ShutdownCallbackRegistry defaultShutdownCallbackRegistry() {
        return new DefaultShutdownCallbackRegistry();
    }

    @ConditionalOnProperty(name = ClockFactory.PROPERTY_NAME, value = "SystemClock")
    @SingletonFactory
    @Ordered(-200)
    public Clock systemClock() {
        return logSupportedPrecision(new SystemClock());
    }

    @ConditionalOnProperty(name = ClockFactory.PROPERTY_NAME, value = "SystemMillisClock")
    @SingletonFactory
    @Ordered(-200)
    public Clock systemMillisClock() {
        return logSupportedPrecision(new SystemMillisClock());
    }

    @ConditionalOnProperty(name = ClockFactory.PROPERTY_NAME, value = "CachedClock")
    @SingletonFactory
    @Ordered(-200)
    public Clock cachedClock() {
        return logSupportedPrecision(CachedClock.instance());
    }

    @ConditionalOnProperty(name = ClockFactory.PROPERTY_NAME, value = "org.apache.logging.log4j.core.time.internal.CachedClock")
    @SingletonFactory
    @Ordered(-200)
    public Clock cachedClockFqcn() {
        return logSupportedPrecision(CachedClock.instance());
    }

    @ConditionalOnProperty(name = ClockFactory.PROPERTY_NAME, value = "CoarseCachedClock")
    @SingletonFactory
    @Ordered(-200)
    public Clock coarseCachedClock() {
        return logSupportedPrecision(CoarseCachedClock.instance());
    }

    @ConditionalOnProperty(name = ClockFactory.PROPERTY_NAME, value = "org.apache.logging.log4j.core.time.internal.CoarseCachedClock")
    @SingletonFactory
    @Ordered(-200)
    public Clock coarseCachedClockFqcn() {
        return logSupportedPrecision(CoarseCachedClock.instance());
    }

    @ConditionalOnProperty(name = ClockFactory.PROPERTY_NAME)
    @SingletonFactory
    @Ordered(-100)
    public Clock systemPropertyClock() throws ClassNotFoundException {
        return logSupportedPrecision(newInstanceOfProperty(ClockFactory.PROPERTY_NAME, Clock.class));
    }

    @ConditionalOnMissingBinding
    @SingletonFactory
    public Clock defaultClock() {
        return new SystemClock();
    }

    @ConditionalOnMissingBinding
    @SingletonFactory
    public NanoClock defaultNanoClock() {
        return new DummyNanoClock();
    }

    @ConditionalOnProperty(name = "log4j2.ContextDataInjector")
    @Factory
    @Ordered(-100)
    public ContextDataInjector systemPropertyContextDataInjector() throws ClassNotFoundException {
        return newInstanceOfProperty("log4j2.ContextDataInjector", ContextDataInjector.class);
    }

    @ConditionalOnMissingBinding
    @Factory
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

    @ConditionalOnProperty(name = Constants.LOG4J_LOG_EVENT_FACTORY)
    @SingletonFactory
    @Ordered(-100)
    public LogEventFactory systemPropertyLogEventFactory() throws ClassNotFoundException {
        return newInstanceOfProperty(Constants.LOG4J_LOG_EVENT_FACTORY, LogEventFactory.class);
    }

    @ConditionalOnMissingBinding
    @SingletonFactory
    public LogEventFactory defaultLogEventFactory(
            final ContextDataInjector injector, final Clock clock, final NanoClock nanoClock) {
        // TODO(ms): can we combine conditional annotations for @ConditionalOnMissingBinding and @ConditionalOnProperty
        return isThreadLocalsEnabled() ? new ReusableLogEventFactory(injector, clock, nanoClock) :
                new DefaultLogEventFactory(injector, clock, nanoClock);
    }

    @ConditionalOnMissingBinding
    @SingletonFactory
    public InterpolatorFactory interpolatorFactory(@Namespace(StrLookup.CATEGORY) final PluginNamespace namespace) {
        // TODO(ms): inject Map<String, Supplier<StrLookup>> instead of PluginNamespace
        return defaultLookup -> new Interpolator(defaultLookup, namespace, injector::getInstance);
    }

    @ConditionalOnMissingBinding
    @SingletonFactory
    public StrSubstitutor strSubstitutor(final InterpolatorFactory factory) {
        return new StrSubstitutor(factory.newInterpolator(null));
    }

    @ConditionalOnMissingBinding
    @SingletonFactory
    public ConfigurationFactory configurationFactory(final StrSubstitutor substitutor) {
        // TODO(ms): should be able to @Import classes to get @ConditionalOnWhatever on the classes to treat as bundles-ish?
        final DefaultConfigurationFactory factory = new DefaultConfigurationFactory(injector);
        factory.setSubstitutor(substitutor);
        return factory;
    }

    @ConditionalOnProperty(name = CompositeConfiguration.MERGE_STRATEGY_PROPERTY)
    @SingletonFactory
    @Ordered(-100)
    public MergeStrategy systemPropertyMergeStrategy() throws ClassNotFoundException {
        return newInstanceOfProperty(CompositeConfiguration.MERGE_STRATEGY_PROPERTY, MergeStrategy.class);
    }

    @ConditionalOnMissingBinding
    @SingletonFactory
    public MergeStrategy defaultMergeStrategy() {
        return new DefaultMergeStrategy();
    }

    @ConditionalOnProperty(name = Constants.LOG4J_DEFAULT_STATUS_LEVEL)
    @SingletonFactory
    @Named("StatusLogger")
    @Ordered(-100)
    public Level systemPropertyDefaultStatusLevel() {
        return Level.getLevel(properties.getStringProperty(Constants.LOG4J_DEFAULT_STATUS_LEVEL));
    }

    @ConditionalOnMissingBinding
    @SingletonFactory
    @Named("StatusLogger")
    public Level defaultStatusLevel() {
        return Level.ERROR;
    }

    private <T> T newInstanceOfProperty(final String propertyName, final Class<T> supertype) throws ClassNotFoundException {
        final String property = properties.getStringProperty(propertyName);
        if (property == null) {
            throw new InjectException("No property defined for name " + propertyName);
        }
        return injector.getInstance(classLoader.loadClass(property).asSubclass(supertype));
    }

    private static Clock logSupportedPrecision(final Clock clock) {
        final String support = clock instanceof PreciseClock ? "supports" : "does not support";
        LOGGER.debug("{} {} precise timestamps.", clock.getClass().getName(), support);
        return clock;
    }
}
