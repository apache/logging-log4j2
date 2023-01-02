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

import java.util.function.Supplier;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.core.async.AsyncQueueFullPolicy;
import org.apache.logging.log4j.core.async.AsyncQueueFullPolicyFactory;
import org.apache.logging.log4j.core.config.DefaultLoggerContextNamingStrategy;
import org.apache.logging.log4j.core.config.LoggerContextNamingStrategy;
import org.apache.logging.log4j.core.config.composite.DefaultMergeStrategy;
import org.apache.logging.log4j.core.config.composite.MergeStrategy;
import org.apache.logging.log4j.core.lookup.InterpolatorFactory;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.selector.ClassLoaderContextSelector;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.core.time.Clock;
import org.apache.logging.log4j.core.time.NanoClock;
import org.apache.logging.log4j.core.time.PreciseClock;
import org.apache.logging.log4j.core.time.internal.CachedClock;
import org.apache.logging.log4j.core.time.internal.CoarseCachedClock;
import org.apache.logging.log4j.core.time.internal.DummyNanoClock;
import org.apache.logging.log4j.core.time.internal.SystemClock;
import org.apache.logging.log4j.core.time.internal.SystemMillisClock;
import org.apache.logging.log4j.core.util.AuthorizationProvider;
import org.apache.logging.log4j.core.util.BasicAuthorizationProvider;
import org.apache.logging.log4j.core.util.DefaultShutdownCallbackRegistry;
import org.apache.logging.log4j.core.util.ShutdownCallbackRegistry;
import org.apache.logging.log4j.plugins.Factory;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.plugins.Ordered;
import org.apache.logging.log4j.plugins.SingletonFactory;
import org.apache.logging.log4j.plugins.condition.ConditionalOnProperty;
import org.apache.logging.log4j.plugins.di.InjectException;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.spi.ClassFactory;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertyResolver;
import org.apache.logging.log4j.util.RecyclerFactories;
import org.apache.logging.log4j.util.RecyclerFactory;

import static org.apache.logging.log4j.util.Constants.isThreadLocalsEnabled;

/**
 * Contains default bindings for Log4j.
 *
 * @see Log4jProperties
 * @see ContextSelector
 * @see ShutdownCallbackRegistry
 * @see Clock
 * @see NanoClock
 * @see MergeStrategy
 * @see ContextDataInjector
 * @see ContextDataFactory
 * @see LogEventFactory
 * @see StrSubstitutor
 * @see AsyncQueueFullPolicy
 * @see AuthorizationProvider
 */
public class DefaultBundle {
    private static final Logger LOGGER = StatusLogger.getLogger();

    private final Injector injector;
    private final PropertyResolver propertyResolver;
    private final ClassFactory classFactory;

    @Inject
    public DefaultBundle(final Injector injector, final PropertyResolver propertyResolver, final ClassFactory classFactory) {
        this.injector = injector;
        this.propertyResolver = propertyResolver;
        this.classFactory = classFactory;
    }

    @SingletonFactory
    @Ordered(Integer.MIN_VALUE)
    public LoggerContextNamingStrategy defaultLoggerContextNamingStrategy() {
        return new DefaultLoggerContextNamingStrategy();
    }

    @ConditionalOnProperty(name = Log4jProperties.CONTEXT_SELECTOR_CLASS_NAME)
    @SingletonFactory
    @Ordered(100)
    public ContextSelector systemPropertyContextSelector() throws ClassNotFoundException {
        return newInstanceOfProperty(Log4jProperties.CONTEXT_SELECTOR_CLASS_NAME, ContextSelector.class);
    }

    @Factory
    @Ordered(Integer.MIN_VALUE)
    public ContextSelector defaultContextSelector(final ClassLoaderContextSelector contextSelector) {
        return contextSelector;
    }

    @ConditionalOnProperty(name = Log4jProperties.SHUTDOWN_CALLBACK_REGISTRY_CLASS_NAME)
    @SingletonFactory
    @Ordered(100)
    public ShutdownCallbackRegistry systemPropertyShutdownCallbackRegistry() throws ClassNotFoundException {
        return newInstanceOfProperty(Log4jProperties.SHUTDOWN_CALLBACK_REGISTRY_CLASS_NAME, ShutdownCallbackRegistry.class);
    }

    @Factory
    @Ordered(Integer.MIN_VALUE)
    public ShutdownCallbackRegistry defaultShutdownCallbackRegistry(final DefaultShutdownCallbackRegistry registry) {
        return registry;
    }

    @ConditionalOnProperty(name = Log4jProperties.CONFIG_CLOCK, value = "SystemClock")
    @Factory
    @Ordered(200)
    public Clock systemClock() {
        return logSupportedPrecision(new SystemClock());
    }

    @ConditionalOnProperty(name = Log4jProperties.CONFIG_CLOCK, value = "SystemMillisClock")
    @Factory
    @Ordered(200)
    public Clock systemMillisClock() {
        return logSupportedPrecision(new SystemMillisClock());
    }

    @ConditionalOnProperty(name = Log4jProperties.CONFIG_CLOCK, value = "CachedClock")
    @Factory
    @Ordered(200)
    public Clock cachedClock() {
        return logSupportedPrecision(CachedClock.instance());
    }

    @ConditionalOnProperty(name = Log4jProperties.CONFIG_CLOCK, value = "org.apache.logging.log4j.core.time.internal.CachedClock")
    @Factory
    @Ordered(200)
    public Clock cachedClockFqcn() {
        return logSupportedPrecision(CachedClock.instance());
    }

    @ConditionalOnProperty(name = Log4jProperties.CONFIG_CLOCK, value = "CoarseCachedClock")
    @Factory
    @Ordered(200)
    public Clock coarseCachedClock() {
        return logSupportedPrecision(CoarseCachedClock.instance());
    }

    @ConditionalOnProperty(name = Log4jProperties.CONFIG_CLOCK, value = "org.apache.logging.log4j.core.time.internal.CoarseCachedClock")
    @Factory
    @Ordered(200)
    public Clock coarseCachedClockFqcn() {
        return logSupportedPrecision(CoarseCachedClock.instance());
    }

    @ConditionalOnProperty(name = Log4jProperties.CONFIG_CLOCK)
    @Factory
    @Ordered(100)
    public Clock systemPropertyClock() throws ClassNotFoundException {
        return logSupportedPrecision(newInstanceOfProperty(Log4jProperties.CONFIG_CLOCK, Clock.class));
    }

    @Factory
    @Ordered(Integer.MIN_VALUE)
    public Clock defaultClock() {
        return new SystemClock();
    }

    @Factory
    @Ordered(Integer.MIN_VALUE)
    public NanoClock defaultNanoClock() {
        return new DummyNanoClock();
    }

    @ConditionalOnProperty(name = Log4jProperties.STATUS_DEFAULT_LEVEL)
    @SingletonFactory
    @Named("StatusLogger")
    @Ordered(100)
    public Level systemPropertyDefaultStatusLevel() {
        return propertyResolver.getString(Log4jProperties.STATUS_DEFAULT_LEVEL).map(Level::getLevel).orElse(Level.ERROR);
    }

    @SingletonFactory
    @Named("StatusLogger")
    @Ordered(Integer.MIN_VALUE)
    public Level defaultStatusLevel() {
        return Level.ERROR;
    }

    @Factory
    @Ordered(Integer.MIN_VALUE)
    public MergeStrategy defaultMergeStrategy(final DefaultMergeStrategy strategy) {
        return strategy;
    }

    @Factory
    @Ordered(Integer.MIN_VALUE)
    public ContextDataFactory defaultContextDataFactory() {
        return new DefaultContextDataFactory();
    }

    @Factory
    @Ordered(Integer.MIN_VALUE)
    public ContextDataInjector defaultContextDataInjector(final ContextDataFactory factory) {
        return ThreadContextDataInjector.create(factory);
    }

    @Factory
    @Ordered(Integer.MIN_VALUE)
    public LogEventFactory defaultLogEventFactory(final Injector injector, final PropertyResolver resolver) {
        final Class<? extends LogEventFactory> factoryClass = isThreadLocalsEnabled(resolver)
                ? ReusableLogEventFactory.class
                : DefaultLogEventFactory.class;
        return injector.getInstance(factoryClass);
    }

    @Factory
    @Ordered(Integer.MIN_VALUE)
    public Supplier<AsyncQueueFullPolicy> defaultAsyncQueueFullPolicyFactory(final AsyncQueueFullPolicyFactory factory) {
        return factory;
    }

    @Factory
    @Ordered(Integer.MIN_VALUE)
    public AuthorizationProvider defaultAuthorizationProvider(final BasicAuthorizationProvider provider) {
        return provider;
    }

    @Factory
    @Ordered(Integer.MIN_VALUE)
    public StrSubstitutor defaultStrSubstitutor(final InterpolatorFactory factory) {
        return new StrSubstitutor(factory.newInterpolator(null));
    }

    @Factory
    @Ordered(Integer.MIN_VALUE)
    public RecyclerFactory defaultRecyclerFactory() {
        return RecyclerFactories.ofSpec(null);
    }

    private <T> T newInstanceOfProperty(final String propertyName, final Class<T> supertype) throws ClassNotFoundException {
        final String property = propertyResolver.getString(propertyName)
                .orElseThrow(() -> new InjectException("No property defined for name " + propertyName));
        final Class<? extends T> clazz = classFactory.getClass(property, supertype);
        return injector.getInstance(clazz);
    }

    private static Clock logSupportedPrecision(final Clock clock) {
        final String support = clock instanceof PreciseClock ? "supports" : "does not support";
        LOGGER.debug("{} {} precise timestamps.", clock.getClass().getName(), support);
        return clock;
    }
}
