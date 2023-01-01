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
package org.apache.logging.log4j.core.test.junit;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.LoggerContextAccessor;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.util.NetUtils;
import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.opentest4j.AssertionFailedError;

public class LoggingTestContext implements ExtensionContext.Store.CloseableResource, LoggerContextAccessor {
    private static final String FQCN = LoggingTestContext.class.getName();
    private final LoggingTestConfiguration configuration;
    private LoggerContext loggerContext;

    LoggingTestContext(final LoggingTestConfiguration configuration) {
        this.configuration = configuration;
    }

    public void init(final Consumer<Injector> configurer) {
        final Log4jContextFactory factory;
        if (configuration.isBootstrap()) {
            final Injector injector = DI.createInjector();
            if (configurer != null) {
                configurer.accept(injector);
            }
            injector.init();
            factory = injector.getInstance(Log4jContextFactory.class);
            LogManager.setFactory(factory);
        } else {
            factory = (Log4jContextFactory) LogManager.getFactory();
        }
        final String configurationLocation = configuration.getConfigurationLocation();
        final URI configUri = configuration.getConfigUri();
        final ClassLoader classLoader = configuration.getClassLoader();
        final String contextName = configuration.getContextName();
        if (configuration.isV1Config()) {
            System.setProperty(ConfigurationFactory.LOG4J1_CONFIGURATION_FILE_PROPERTY, configurationLocation);
            loggerContext = factory.getContext(FQCN, classLoader, null, false);
        } else if (configUri != null) {
            loggerContext = factory.getContext(FQCN, classLoader, false, configUri, contextName, configurer);
        } else if (Strings.isBlank(configurationLocation)) {
            loggerContext = factory.getContext(FQCN, classLoader, false, null, contextName, configurer);
        } else if (configurationLocation.contains(",")) {
            loggerContext = factory.getContext(FQCN, classLoader, null, false, NetUtils.toURIs(configurationLocation), contextName);
        } else {
            loggerContext = factory.getContext(FQCN, classLoader, false, NetUtils.toURI(configurationLocation), contextName, configurer);
        }
        if (loggerContext == null) {
            throw new AssertionFailedError("Error creating LoggerContext");
        }
    }

    @Override
    public LoggerContext getLoggerContext() {
        return loggerContext;
    }

    @Override
    public void close() {
        if (loggerContext != null) {
            final long timeout = configuration.getTimeout();
            final TimeUnit unit = configuration.getUnit();
            if (!loggerContext.stop(timeout, unit)) {
                StatusLogger.getLogger().error("Logger context {} did not shutdown completely after {} {}.",
                        loggerContext.getName(), timeout, unit);
            }
        }
    }
}
