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
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.composite.MergeStrategy;
import org.apache.logging.log4j.core.impl.CoreProperties.AuthenticationProperties;
import org.apache.logging.log4j.core.impl.CoreProperties.ConfigurationProperties;
import org.apache.logging.log4j.core.impl.CoreProperties.LogEventProperties;
import org.apache.logging.log4j.core.impl.CoreProperties.LoggerContextProperties;
import org.apache.logging.log4j.core.impl.CoreProperties.MessageProperties;
import org.apache.logging.log4j.core.impl.CoreProperties.StatusLoggerProperties;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.core.time.ClockFactory;
import org.apache.logging.log4j.core.util.AuthorizationProvider;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.ShutdownCallbackRegistry;
import org.apache.logging.log4j.kit.env.PropertyEnvironment;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.plugins.Ordered;
import org.apache.logging.log4j.plugins.di.ConfigurableInstanceFactory;
import org.apache.logging.log4j.plugins.di.spi.ConfigurableInstanceFactoryPostProcessor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Post-processor that registers {@link CoreDefaultBundle} for default bindings
 * used in Log4j and the services specified by properties.
 */
@Ordered(Ordered.LAST - 1000)
@ServiceProvider(value = ConfigurableInstanceFactoryPostProcessor.class, resolution = Resolution.OPTIONAL)
@NullMarked
public class CoreInstanceFactoryPostProcessor implements ConfigurableInstanceFactoryPostProcessor {
    @Override
    public void postProcessFactory(final ConfigurableInstanceFactory factory) {
        final PropertyEnvironment env = factory.getInstance(PropertyEnvironment.class);
        // TODO: replace with an appropriate scoping of the definitions
        if (factory.hasBinding(Configuration.KEY)) {
            registerConfigurationServices(factory, env);
        } else if (factory.hasBinding(LoggerContext.KEY)) {
            registerLoggerContextSevices(factory, env);
        } else {
            registerGlobalServices(factory, env);
        }
        factory.registerBundles(ClockFactory.class, CoreDefaultBundle.class);
    }

    private void registerConfigurationServices(
            final ConfigurableInstanceFactory factory, final PropertyEnvironment env) {
        final LogEventProperties logEvent = env.getProperty(LogEventProperties.class);
        registerIfPresent(factory, LogEventFactory.class, logEvent.factory());

        final StatusLoggerProperties statusLogger = env.getProperty(StatusLoggerProperties.class);
        factory.registerBinding(Constants.STATUS_LOGGER_LEVEL_KEY, statusLogger::level);
    }

    private void registerGlobalServices(final ConfigurableInstanceFactory factory, final PropertyEnvironment env) {
        final LoggerContextProperties loggerContext = env.getProperty(LoggerContextProperties.class);
        registerIfPresent(factory, ContextSelector.class, loggerContext.selector());
        registerIfPresent(factory, ShutdownCallbackRegistry.class, loggerContext.shutdownCallbackRegistry());
    }

    private void registerLoggerContextSevices(
            final ConfigurableInstanceFactory factory, final PropertyEnvironment env) {
        final AuthenticationProperties auth = env.getProperty(AuthenticationProperties.class);
        registerIfPresent(factory, AuthorizationProvider.class, auth.provider());

        final ConfigurationProperties configuration = env.getProperty(ConfigurationProperties.class);
        registerIfPresent(factory, ConfigurationFactory.class, configuration.factory());
        registerIfPresent(factory, MergeStrategy.class, configuration.mergeStrategy());

        final MessageProperties message = env.getProperty(MessageProperties.class);
        registerIfPresent(factory, MessageFactory.class, message.factory());
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
