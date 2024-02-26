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

import static org.apache.logging.log4j.plugins.di.Key.forClass;

import aQute.bnd.annotation.Resolution;
import aQute.bnd.annotation.spi.ServiceProvider;
import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.composite.MergeStrategy;
import org.apache.logging.log4j.core.impl.CoreKeys.Configuration;
import org.apache.logging.log4j.core.impl.CoreKeys.Logger;
import org.apache.logging.log4j.core.impl.CoreKeys.LoggerContext;
import org.apache.logging.log4j.core.impl.CoreKeys.StatusLogger;
import org.apache.logging.log4j.core.impl.CoreKeys.ThreadContext;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.core.time.ClockFactory;
import org.apache.logging.log4j.core.util.AuthorizationProvider;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.ShutdownCallbackRegistry;
import org.apache.logging.log4j.kit.env.PropertyEnvironment;
import org.apache.logging.log4j.plugins.Ordered;
import org.apache.logging.log4j.plugins.di.ConfigurableInstanceFactory;
import org.apache.logging.log4j.plugins.di.spi.ConfigurableInstanceFactoryPostProcessor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Post-processor that registers {@link DefaultBundle} for default bindings
 * used in Log4j and the services specified by properties.
 */
@Ordered(Ordered.LAST - 1000)
@ServiceProvider(value = ConfigurableInstanceFactoryPostProcessor.class, resolution = Resolution.OPTIONAL)
@NullMarked
public class Log4jInstanceFactoryPostProcessor implements ConfigurableInstanceFactoryPostProcessor {
    @Override
    public void postProcessFactory(final ConfigurableInstanceFactory factory) {
        final PropertyEnvironment env = factory.getInstance(PropertyEnvironment.class);
        registerFromProperties(factory, env);
        factory.registerBundles(ClockFactory.class, DefaultBundle.class);
    }

    private void registerFromProperties(final ConfigurableInstanceFactory factory, final PropertyEnvironment env) {
        final Configuration configuration = env.getProperty(Configuration.class);
        registerIfPresent(factory, AuthorizationProvider.class, configuration.authorizationProvider());
        registerIfPresent(factory, ConfigurationFactory.class, configuration.configurationFactory());
        registerIfPresent(factory, MergeStrategy.class, configuration.mergeStrategy());

        final Logger logger = env.getProperty(Logger.class);
        registerIfPresent(factory, LogEventFactory.class, logger.logEventFactory());

        final LoggerContext loggerContext = env.getProperty(LoggerContext.class);
        registerIfPresent(factory, ContextSelector.class, loggerContext.selector());
        registerIfPresent(factory, ShutdownCallbackRegistry.class, loggerContext.shutdownCallbackRegistry());

        final StatusLogger statusLogger = env.getProperty(StatusLogger.class);
        if (statusLogger.defaultStatusLevel() != null) {
            factory.registerBinding(Constants.DEFAULT_STATUS_LEVEL_KEY, statusLogger::defaultStatusLevel);
        }

        final ThreadContext threadContext = env.getProperty(ThreadContext.class);
        registerIfPresent(factory, ContextDataInjector.class, threadContext.contextDataInjector());
    }

    private <T> void registerIfPresent(
            final ConfigurableInstanceFactory instanceFactory,
            final Class<T> serviceType,
            final @Nullable Class<? extends T> serviceImplementation) {
        if (serviceImplementation != null) {
            instanceFactory.registerBinding(
                    forClass(serviceType), () -> instanceFactory.getInstance(serviceImplementation));
        }
    }
}
