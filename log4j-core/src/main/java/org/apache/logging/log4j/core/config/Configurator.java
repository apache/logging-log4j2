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
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.impl.ContextAnchor;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Initializes and configure the Logging system.
 */
public final class Configurator {

    protected static final StatusLogger LOGGER = StatusLogger.getLogger();

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
            final URI uri = configLocation == null ? null : new URI(configLocation);
            return initialize(name, loader, uri, externalContext);
        } catch (final URISyntaxException ex) {
            ex.printStackTrace();
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
            final org.apache.logging.log4j.spi.LoggerContext context = LogManager.getContext(loader, false, configLocation);
            if (context instanceof LoggerContext) {
                final LoggerContext ctx = (LoggerContext) context;
                ContextAnchor.THREAD_CONTEXT.set(ctx);
                if (externalContext != null) {
                    ctx.setExternalContext(externalContext);
                }
                final Configuration config = ConfigurationFactory.getInstance().getConfiguration(name, configLocation);
                ctx.start(config);
                ContextAnchor.THREAD_CONTEXT.remove();
                return ctx;
            } else {
                LOGGER.error("LogManager returned an instance of {} which does not implement {}. Unable to initialize Log4j",
                    context.getClass().getName(), LoggerContext.class.getName());
            }
        } catch (final Exception ex) {
            ex.printStackTrace();
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
                                           final ConfigurationFactory.ConfigurationSource source) {

        try {
            URI configLocation = null;
            try {
                configLocation = source.getLocation() == null ? null : new URI(source.getLocation());
            } catch (final Exception ex) {
                // Invalid source location.
            }
            final org.apache.logging.log4j.spi.LoggerContext context = LogManager.getContext(loader, false, configLocation);
            if (context instanceof LoggerContext) {
                final LoggerContext ctx = (LoggerContext) context;
                ContextAnchor.THREAD_CONTEXT.set(ctx);
                final Configuration config = ConfigurationFactory.getInstance().getConfiguration(source);
                ctx.start(config);
                ContextAnchor.THREAD_CONTEXT.remove();
                return ctx;
            } else {
                LOGGER.error("LogManager returned an instance of {} which does not implement {}. Unable to initialize Log4j",
                    context.getClass().getName(), LoggerContext.class.getName());
            }
        } catch (final Exception ex) {
            ex.printStackTrace();
        }
        return null;
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
