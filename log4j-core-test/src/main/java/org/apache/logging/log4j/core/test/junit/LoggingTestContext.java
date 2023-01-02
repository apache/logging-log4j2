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
import org.apache.logging.log4j.plugins.util.Builder;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.opentest4j.AssertionFailedError;

public class LoggingTestContext implements ExtensionContext.Store.CloseableResource, LoggerContextAccessor {
    public static Configurer configurer() {
        return new Configurer();
    }

    private static final String FQCN = LoggingTestContext.class.getName();
    private final long timeout;
    private final TimeUnit unit;
    private final String configurationLocation;
    private final URI configUri;
    private final String contextName;
    private final ReconfigurationPolicy reconfigurationPolicy;
    private final boolean v1Config;
    private final boolean bootstrap;
    private final ClassLoader classLoader;
    private LoggerContext loggerContext;

    LoggingTestContext(final Configurer configuration) {
        timeout = configuration.getTimeout();
        unit = configuration.getUnit();
        configurationLocation = configuration.getConfigurationLocation();
        configUri = configuration.getConfigUri();
        contextName = configuration.getContextName();
        reconfigurationPolicy = configuration.getReconfigurationPolicy();
        v1Config = configuration.isV1Config();
        bootstrap = configuration.isBootstrap();
        classLoader = configuration.getClassLoader();
    }

    public void init(final Consumer<Injector> configurer) {
        final Log4jContextFactory factory;
        if (bootstrap) {
            final Injector injector = DI.createInjector();
            injector.init();
            if (configurer != null) {
                configurer.accept(injector);
            }
            factory = injector.getInstance(Log4jContextFactory.class);
            LogManager.setFactory(factory);
        } else {
            factory = (Log4jContextFactory) LogManager.getFactory();
        }
        if (v1Config) {
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

    public void beforeEachTest() {
        if (reconfigurationPolicy == ReconfigurationPolicy.BEFORE_EACH) {
            loggerContext.reconfigure();
        }
    }

    public void afterEachTest() {
        if (reconfigurationPolicy == ReconfigurationPolicy.AFTER_EACH) {
            loggerContext.reconfigure();
        }
    }

    @Override
    public LoggerContext getLoggerContext() {
        return loggerContext;
    }

    @Override
    public void close() {
        if (loggerContext != null) {
            if (!loggerContext.stop(timeout, unit)) {
                StatusLogger.getLogger().error("Logger context {} did not shutdown completely after {} {}.",
                        contextName, timeout, unit);
            }
        }
    }

    public long getTimeout() {
        return timeout;
    }

    public TimeUnit getUnit() {
        return unit;
    }

    public String getConfigurationLocation() {
        return configurationLocation;
    }

    public URI getConfigUri() {
        return configUri;
    }

    public String getContextName() {
        return contextName;
    }

    public ReconfigurationPolicy getReconfigurationPolicy() {
        return reconfigurationPolicy;
    }

    public boolean isV1Config() {
        return v1Config;
    }

    public boolean isBootstrap() {
        return bootstrap;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public static class Configurer implements Builder<LoggingTestContext> {
        private long timeout;
        private TimeUnit unit = TimeUnit.SECONDS;
        private String configurationLocation;
        private URI configUri;
        private String contextName;
        private ReconfigurationPolicy reconfigurationPolicy = ReconfigurationPolicy.NEVER;
        private boolean v1Config;
        private boolean bootstrap;
        private ClassLoader classLoader;

        public long getTimeout() {
            return timeout;
        }

        public Configurer setTimeout(final long timeout, final TimeUnit unit) {
            this.timeout = timeout;
            this.unit = unit;
            return this;
        }

        public TimeUnit getUnit() {
            return unit;
        }

        public String getConfigurationLocation() {
            return configurationLocation;
        }

        public Configurer setConfigurationLocation(final String configurationLocation) {
            this.configurationLocation = configurationLocation;
            return this;
        }

        public URI getConfigUri() {
            return configUri;
        }

        public Configurer setConfigUri(final URI configUri) {
            this.configUri = configUri;
            return this;
        }

        public String getContextName() {
            return contextName;
        }

        public Configurer setContextName(final String contextName) {
            this.contextName = contextName;
            return this;
        }

        public ReconfigurationPolicy getReconfigurationPolicy() {
            return reconfigurationPolicy;
        }

        public Configurer setReconfigurationPolicy(final ReconfigurationPolicy reconfigurationPolicy) {
            this.reconfigurationPolicy = reconfigurationPolicy;
            return this;
        }

        public boolean isV1Config() {
            return v1Config;
        }

        public Configurer setV1Config(final boolean v1Config) {
            this.v1Config = v1Config;
            return this;
        }

        public boolean isBootstrap() {
            return bootstrap;
        }

        public Configurer setBootstrap(final boolean bootstrap) {
            this.bootstrap = bootstrap;
            return this;
        }

        public ClassLoader getClassLoader() {
            return classLoader;
        }

        public Configurer setClassLoader(final ClassLoader classLoader) {
            this.classLoader = classLoader;
            return this;
        }

        @Override
        public LoggingTestContext build() {
            if (timeout != 0 && unit == null) {
                throw new IllegalStateException("No unit specified for timeout value: " + timeout);
            }
            if (reconfigurationPolicy == null) {
                reconfigurationPolicy = ReconfigurationPolicy.NEVER;
            }
            return new LoggingTestContext(this);
        }
    }
}
