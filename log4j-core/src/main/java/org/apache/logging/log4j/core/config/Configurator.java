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
package org.apache.logging.log4j.core.config;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.util.FileUtils;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Initializes and configure the Logging system. This class provides several ways to construct a LoggerContext using
 * the location of a configuration file, a context name, and various optional parameters.
 */
public final class Configurator {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static final String FQCN = Configurator.class.getName();

    private Configurator() {
    }

    /**
     * Initializes the Logging Context.
     * @param name The Context name.
     * @param loader The ClassLoader for the Context (or null).
     * @param configLocation The configuration for the logging context.
     * @return The LoggerContext.
     */
    public static LoggerContext initialize(final String name, final ClassLoader loader, final String configLocation) {
        return initialize(name, loader, configLocation, null);

    }

    /**
     * Initializes the Logging Context.
     * @param name The Context name.
     * @param loader The ClassLoader for the Context (or null).
     * @param configLocation The configuration for the logging context.
     * @param externalContext The external context to be attached to the LoggerContext
     * @return The LoggerContext.
     */
    public static LoggerContext initialize(final String name, final ClassLoader loader, final String configLocation,
                                           final Object externalContext) {

        try {
            final URI uri = configLocation == null ? null : FileUtils.getCorrectedFilePathUri(configLocation);
            return initialize(name, loader, uri, externalContext);
        } catch (final URISyntaxException ex) {
            LOGGER.error("There was a problem parsing the configuration location [{}].", configLocation, ex);
        }
        return null;
    }

    /**
     * Initializes the Logging Context.
     * @param name The Context name.
     * @param configLocation The configuration for the logging context.
     * @return The LoggerContext.
     */
    public static LoggerContext initialize(final String name, final String configLocation) {
        return initialize(name, null, configLocation);
    }

    /**
     * Initializes the Logging Context.
     * @param name The Context name.
     * @param loader The ClassLoader for the Context (or null).
     * @param configLocation The configuration for the logging context.
     * @return The LoggerContext.
     */
    public static LoggerContext initialize(final String name, final ClassLoader loader, final URI configLocation) {
        return initialize(name, loader, configLocation, null);
    }

    /**
     * Initializes the Logging Context.
     * @param name The Context name.
     * @param loader The ClassLoader for the Context (or null).
     * @param configLocation The configuration for the logging context.
     * @param externalContext The external context to be attached to the LoggerContext
     * @return The LoggerContext.
     */
    public static LoggerContext initialize(final String name, final ClassLoader loader, final URI configLocation,
                                           final Object externalContext) {

        try {
            final Log4jContextFactory factory = getFactory();
            return factory == null ? null :
                    factory.getContext(FQCN, loader, externalContext, false, configLocation, name);
        } catch (final Exception ex) {
            LOGGER.error("There was a problem initializing the LoggerContext [{}] using configuration at [{}].",
                    name, configLocation, ex);
        }
        return null;
    }

    /**
     * Initializes the Logging Context.
     * @param loader The ClassLoader for the Context (or null).
     * @param source The InputSource for the configuration.
     * @return The LoggerContext.
     */
    public static LoggerContext initialize(final ClassLoader loader,
                                           final ConfigurationSource source) {
        return initialize(loader, source, null);
    }

    /**
     * Initializes the Logging Context.
     * @param loader The ClassLoader for the Context (or null).
     * @param source The InputSource for the configuration.
     * @param externalContext The external context to be attached to the LoggerContext.
     * @return The LoggerContext.
     */

    public static LoggerContext initialize(final ClassLoader loader,
                                           final ConfigurationSource source,
                                           final Object externalContext)
    {

        try {
            final Log4jContextFactory factory = getFactory();
            return factory == null ? null :
                    factory.getContext(FQCN, loader, externalContext, false, source);
        } catch (final Exception ex) {
            LOGGER.error("There was a problem obtaining a LoggerContext using the configuration source [{}]", source, ex);
        }
        return null;
    }

    private static Log4jContextFactory getFactory() {
        final LoggerContextFactory factory = LogManager.getFactory();
        if (factory instanceof Log4jContextFactory) {
            return (Log4jContextFactory) factory;
        } else if (factory != null) {
            LOGGER.error("LogManager returned an instance of {} which does not implement {}. Unable to initialize Log4j.",
                    factory.getClass().getName(), Log4jContextFactory.class.getName());
            return null;
        } else {
            LOGGER.fatal("LogManager did not return a LoggerContextFactory. This indicates something has gone terribly wrong!");
            return null;
        }
    }

    /**
     * Shuts down the given logging context.
     * @param ctx the logging context to shut down, may be null.
     */
    public static void shutdown(final LoggerContext ctx) {
        if (ctx != null) {
            ctx.stop();
        }
    }
}
