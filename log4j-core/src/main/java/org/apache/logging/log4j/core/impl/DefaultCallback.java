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
import org.apache.logging.log4j.core.config.ConfigurationScheduler;
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
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.core.util.ShutdownCallbackRegistry;
import org.apache.logging.log4j.plugins.PluginException;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.plugins.di.InjectorCallback;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.plugins.di.ReflectionCallerContext;
import org.apache.logging.log4j.spi.CopyOnWrite;
import org.apache.logging.log4j.spi.DefaultThreadContextMap;
import org.apache.logging.log4j.spi.ReadOnlyThreadContextMap;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;

import java.util.Map;
import java.util.function.Supplier;

public class DefaultCallback implements InjectorCallback {
    private static final ReflectionCallerContext CORE_CALLER_CONTEXT = object -> object.setAccessible(true);
    private static final Logger LOGGER = StatusLogger.getLogger();

    private static class PropertyLoader {
        private final Injector injector;
        private final PropertiesUtil properties;
        private final ClassLoader classLoader;

        private PropertyLoader(
                final Injector injector, final PropertiesUtil properties, final ClassLoader classLoader) {
            this.injector = injector;
            this.classLoader = classLoader;
            this.properties = properties;
        }

        <T> T getInstance(final String propertyName, final Class<T> type, final Supplier<Class<? extends T>> fallback) {
            final String className = properties.getStringProperty(propertyName);
            if (className != null) {
                try {
                    return injector.getInstance(classLoader.loadClass(className).asSubclass(type));
                } catch (ClassNotFoundException | ClassCastException | PluginException e) {
                    LOGGER.error("Unable to create {} {}: {}", type.getSimpleName(), className, e.getMessage(), e);
                }
            }
            return injector.getInstance(fallback.get());
        }
    }

    @Override
    public void configure(final Injector injector) {
        final PropertiesUtil properties = PropertiesUtil.getProperties();
        final var loader = new PropertyLoader(injector, properties, Loader.getClassLoader());
        injector.setCallerContext(CORE_CALLER_CONTEXT);
        injector.bindIfAbsent(ContextSelector.KEY,
                        () -> loader.getInstance(Constants.LOG4J_CONTEXT_SELECTOR, ContextSelector.class,
                                () -> ClassLoaderContextSelector.class))
                .bindIfAbsent(ShutdownCallbackRegistry.KEY,
                        () -> loader.getInstance(ShutdownCallbackRegistry.SHUTDOWN_CALLBACK_REGISTRY,
                                ShutdownCallbackRegistry.class,
                                () -> DefaultShutdownCallbackRegistry.class))
                .bindIfAbsent(Key.forClass(ConfigurationScheduler.class), ConfigurationScheduler::new)
                .bindIfAbsent(Clock.KEY,
                        () -> {
                            final var property = properties.getStringProperty(ClockFactory.PROPERTY_NAME);
                            if (property == null) {
                                return logSupportedPrecision(new SystemClock());
                            }
                            final Supplier<Clock> fallback =
                                    () -> loader.getInstance(ClockFactory.PROPERTY_NAME, Clock.class, () -> SystemClock.class);
                            return logSupportedPrecision(CLOCK_ALIASES.getOrDefault(property, fallback).get());
                        })
                .bindIfAbsent(NanoClock.KEY, DummyNanoClock::new)
                .bindIfAbsent(ContextDataInjector.KEY,
                        () -> loader.getInstance("log4j2.ContextDataInjector", ContextDataInjector.class, () -> {
                            final ReadOnlyThreadContextMap threadContextMap = ThreadContext.getThreadContextMap();

                            // note: map may be null (if legacy custom ThreadContextMap was installed by user)
                            if (threadContextMap instanceof DefaultThreadContextMap || threadContextMap == null) {
                                // for non StringMap-based context maps
                                return ThreadContextDataInjector.ForDefaultThreadContextMap.class;
                            }
                            if (threadContextMap instanceof CopyOnWrite) {
                                return ThreadContextDataInjector.ForCopyOnWriteThreadContextMap.class;
                            }
                            return ThreadContextDataInjector.ForGarbageFreeThreadContextMap.class;
                        }))
                .bindIfAbsent(LogEventFactory.KEY,
                        () -> loader.getInstance(Constants.LOG4J_LOG_EVENT_FACTORY, LogEventFactory.class,
                                () -> Constants.ENABLE_THREADLOCALS ? ReusableLogEventFactory.class :
                                        DefaultLogEventFactory.class))
                .bindIfAbsent(Constants.DEFAULT_STATUS_LEVEL_KEY, () -> {
                    final String statusLevel =
                            properties.getStringProperty(Constants.LOG4J_DEFAULT_STATUS_LEVEL, Level.ERROR.name());
                    try {
                        return Level.toLevel(statusLevel);
                    } catch (final Exception ex) {
                        return Level.ERROR;
                    }
                });
    }

    private static final Map<String, Supplier<Clock>> CLOCK_ALIASES = Map.of(
            "SystemClock", SystemClock::new,
            "SystemMillisClock", SystemMillisClock::new,
            "CachedClock", CachedClock::instance,
            "CoarseCachedClock", CoarseCachedClock::instance,
            "org.apache.logging.log4j.core.time.internal.CachedClock", CachedClock::instance,
            "org.apache.logging.log4j.core.time.internal.CoarseCachedClock", CoarseCachedClock::instance
    );

    private static Clock logSupportedPrecision(final Clock clock) {
        final String support = clock instanceof PreciseClock ? "supports" : "does not support";
        LOGGER.debug("{} {} precise timestamps.", clock.getClass().getName(), support);
        return clock;
    }

    @Override
    public String toString() {
        return getClass().getName();
    }
}
