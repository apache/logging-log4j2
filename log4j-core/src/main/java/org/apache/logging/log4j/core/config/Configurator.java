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
package org.apache.logging.log4j.core.config;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.util.NetUtils;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.StackLocatorUtil;
import org.apache.logging.log4j.util.Strings;

/**
 * Initializes and configure the Logging system. This class provides several ways to construct a LoggerContext using
 * the location of a configuration file, a context name, and various optional parameters.
 */
public final class Configurator {

    private static final String FQCN = Configurator.class.getName();

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static Log4jContextFactory getFactory() {
        final LoggerContextFactory factory = LogManager.getFactory();
        if (factory instanceof Log4jContextFactory) {
            return (Log4jContextFactory) factory;
        }
        if (factory != null) {
            LOGGER.error(
                    "LogManager returned an instance of {} which does not implement {}. Unable to initialize Log4j.",
                    factory.getClass().getName(),
                    Log4jContextFactory.class.getName());
        } else {
            LOGGER.fatal(
                    "LogManager did not return a LoggerContextFactory. This indicates something has gone terribly wrong!");
        }
        return null;
    }

    /**
     * Initializes the Logging Context.
     * @param loader The ClassLoader for the Context (or null).
     * @param source The InputSource for the configuration.
     * @return The LoggerContext.
     */
    public static LoggerContext initialize(final ClassLoader loader, final ConfigurationSource source) {
        return initialize(loader, source, null);
    }

    /**
     * Initializes the Logging Context.
     * @param loader The ClassLoader for the Context (or null).
     * @param source The InputSource for the configuration.
     * @param externalContext The external context to be attached to the LoggerContext.
     * @return The LoggerContext.
     */
    public static LoggerContext initialize(
            final ClassLoader loader, final ConfigurationSource source, final Object externalContext) {

        try {
            final Log4jContextFactory factory = getFactory();
            return factory == null ? null : factory.getContext(FQCN, loader, externalContext, false, source);
        } catch (final Exception ex) {
            LOGGER.error(
                    "There was a problem obtaining a LoggerContext using the configuration source [{}]", source, ex);
        }
        return null;
    }

    /**
     * Initializes the Logging Context.
     * @param name The Context name.
     * @param loader The ClassLoader for the Context (or null).
     * @param configLocation The configuration for the logging context.
     * @return The LoggerContext or null if an error occurred (check the status logger).
     */
    public static LoggerContext initialize(final String name, final ClassLoader loader, final String configLocation) {
        return initialize(name, loader, configLocation, null);
    }

    /**
     * Initializes the Logging Context.
     * @param name The Context name.
     * @param loader The ClassLoader for the Context (or null).
     * @param configLocation The configuration for the logging context (or null, or blank).
     * @param externalContext The external context to be attached to the LoggerContext
     * @return The LoggerContext or null if an error occurred (check the status logger).
     */
    public static LoggerContext initialize(
            final String name, final ClassLoader loader, final String configLocation, final Object externalContext) {
        if (Strings.isBlank(configLocation)) {
            return initialize(name, loader, (URI) null, externalContext);
        }
        return configLocation.contains(",")
                ? initialize(name, loader, NetUtils.toURIs(configLocation), externalContext)
                : initialize(name, loader, NetUtils.toURI(configLocation), externalContext);
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
     * @param configLocation The configuration for the logging context (or null).
     * @param externalContext The external context to be attached to the LoggerContext
     * @return The LoggerContext.
     */
    public static LoggerContext initialize(
            final String name, final ClassLoader loader, final URI configLocation, final Object externalContext) {

        try {
            final Log4jContextFactory factory = getFactory();
            return factory == null
                    ? null
                    : factory.getContext(FQCN, loader, externalContext, false, configLocation, name);
        } catch (final Exception ex) {
            LOGGER.error(
                    "There was a problem initializing the LoggerContext [{}] using configuration at [{}].",
                    name,
                    configLocation,
                    ex);
        }
        return null;
    }

    /**
     * Initializes the Logging Context.
     * @param name The Context name.
     * @param loader The ClassLoader for the Context (or null).
     * @param configLocation The configuration for the logging context (or null).
     * @param entry The external context entry to be attached to the LoggerContext
     * @return The LoggerContext.
     */
    public static LoggerContext initialize(
            final String name,
            final ClassLoader loader,
            final URI configLocation,
            final Map.Entry<String, Object> entry) {

        try {
            final Log4jContextFactory factory = getFactory();
            return factory == null ? null : factory.getContext(FQCN, loader, entry, false, configLocation, name);
        } catch (final Exception ex) {
            LOGGER.error(
                    "There was a problem initializing the LoggerContext [{}] using configuration at [{}].",
                    name,
                    configLocation,
                    ex);
        }
        return null;
    }

    public static LoggerContext initialize(
            final String name,
            final ClassLoader loader,
            final List<URI> configLocations,
            final Object externalContext) {
        try {
            final Log4jContextFactory factory = getFactory();
            return factory == null
                    ? null
                    : factory.getContext(FQCN, loader, externalContext, false, configLocations, name);
        } catch (final Exception ex) {
            LOGGER.error(
                    "There was a problem initializing the LoggerContext [{}] using configurations at [{}].",
                    name,
                    configLocations,
                    ex);
        }
        return null;
    }

    /**
     * Initializes the Logging Context.
     * @param name The Context name.
     * @param configLocation The configuration for the logging context.
     * @return The LoggerContext or null if an error occurred (check the status logger).
     */
    public static LoggerContext initialize(final String name, final String configLocation) {
        return initialize(name, null, configLocation);
    }

    /**
     * Initializes the Logging Context.
     * @param configuration The Configuration.
     * @return The LoggerContext.
     */
    public static LoggerContext initialize(final Configuration configuration) {
        return initialize(null, configuration, null);
    }

    /**
     * Initializes the Logging Context.
     * @param loader The ClassLoader.
     * @param configuration The Configuration.
     * @return The LoggerContext.
     */
    public static LoggerContext initialize(final ClassLoader loader, final Configuration configuration) {
        return initialize(loader, configuration, null);
    }

    /**
     * Initializes the Logging Context.
     * @param loader The ClassLoader.
     * @param configuration The Configuration.
     * @param externalContext - The external context to be attached to the LoggerContext.
     * @return The LoggerContext.
     */
    public static LoggerContext initialize(
            final ClassLoader loader, final Configuration configuration, final Object externalContext) {
        try {
            final Log4jContextFactory factory = getFactory();
            return factory == null ? null : factory.getContext(FQCN, loader, externalContext, false, configuration);
        } catch (final Exception ex) {
            LOGGER.error(
                    "There was a problem initializing the LoggerContext using configuration {}",
                    configuration.getName(),
                    ex);
        }
        return null;
    }

    /**
     * Reconfigure using an already constructed Configuration.
     * @param configuration The configuration.
     * @since 2.13.0
     */
    public static void reconfigure(final Configuration configuration) {
        try {
            final Log4jContextFactory factory = getFactory();
            if (factory != null) {
                factory.getContext(FQCN, null, null, false).reconfigure(configuration);
            }
        } catch (final Exception ex) {
            LOGGER.error(
                    "There was a problem initializing the LoggerContext using configuration {}",
                    configuration.getName(),
                    ex);
        }
    }

    /**
     * Reload the existing reconfiguration.
     * @since 2.12.0
     */
    public static void reconfigure() {
        try {
            final Log4jContextFactory factory = getFactory();
            if (factory != null) {
                factory.getSelector().getContext(FQCN, null, false).reconfigure();
            } else {
                LOGGER.warn("Unable to reconfigure - Log4j has not been initialized.");
            }
        } catch (final Exception ex) {
            LOGGER.error("Error encountered trying to reconfigure logging", ex);
        }
    }

    /**
     * Reconfigure with a potentially new configuration.
     * @param uri The location of the configuration.
     * @since 2.12.0
     */
    public static void reconfigure(final URI uri) {
        try {
            final Log4jContextFactory factory = getFactory();
            if (factory != null) {
                factory.getSelector().getContext(FQCN, null, false).setConfigLocation(uri);
            } else {
                LOGGER.warn("Unable to reconfigure - Log4j has not been initialized.");
            }
        } catch (final Exception ex) {
            LOGGER.error("Error encountered trying to reconfigure logging", ex);
        }
    }

    /**
     * Sets the levels of <code>parentLogger</code> and all 'child' loggers to the given <code>level</code>.
     *
     * @param parentLogger the parent logger
     * @param level the new level
     */
    public static void setAllLevels(final String parentLogger, final Level level) {
        // 1) get logger config
        // 2) if exact match, use it, if not, create it.
        // 3) set level on logger config
        // 4) update child logger configs with level
        // 5) update loggers
        final LoggerContext loggerContext =
                LoggerContext.getContext(StackLocatorUtil.getCallerClassLoader(2), false, null);
        final Configuration config = loggerContext.getConfiguration();
        boolean set = setLevel(parentLogger, level, config);
        for (final Map.Entry<String, LoggerConfig> entry : config.getLoggers().entrySet()) {
            if (entry.getKey().startsWith(parentLogger)) {
                set |= setLevel(entry.getValue(), level);
            }
        }
        if (set) {
            loggerContext.updateLoggers();
        }
    }

    /**
     * Sets a logger's level.
     *
     * @param logger
     *            the logger
     * @param level
     *            the new level
     * @return the given logger
     */
    public static Logger setLevel(final Logger logger, final Level level) {
        setLevel(
                LoggerContext.getContext(StackLocatorUtil.getCallerClassLoader(2), false, null),
                logger.getName(),
                level);
        return logger;
    }

    /**
     * Sets a logger's level.
     *
     * @param clazz
     *            the logger
     * @param level
     *            the new level
     */
    public static void setLevel(final Class<?> clazz, final Level level) {
        final String canonicalName = clazz.getCanonicalName();
        setLevel(
                LoggerContext.getContext(StackLocatorUtil.getCallerClassLoader(2), false, null),
                canonicalName != null ? canonicalName : clazz.getName(),
                level);
    }

    private static boolean setLevel(final LoggerConfig loggerConfig, final Level level) {
        final boolean set = !loggerConfig.getLevel().equals(level);
        if (set) {
            loggerConfig.setLevel(level);
        }
        return set;
    }

    private static void setLevel(final LoggerContext loggerContext, final String loggerName, final Level level) {
        if (Strings.isEmpty(loggerName)) {
            setRootLevel(level, loggerContext);
        } else if (setLevel(loggerName, level, loggerContext.getConfiguration())) {
            loggerContext.updateLoggers();
        }
    }

    /**
     * Sets logger levels.
     *
     * @param levelMap
     *            a levelMap where keys are level names and values are new
     *            Levels.
     */
    public static void setLevel(final Map<String, Level> levelMap) {
        final LoggerContext loggerContext =
                LoggerContext.getContext(StackLocatorUtil.getCallerClassLoader(2), false, null);
        final Configuration config = loggerContext.getConfiguration();
        boolean set = false;
        for (final Map.Entry<String, Level> entry : levelMap.entrySet()) {
            final String loggerName = entry.getKey();
            final Level level = entry.getValue();
            set |= setLevel(loggerName, level, config);
        }
        if (set) {
            loggerContext.updateLoggers();
        }
    }

    /**
     * Sets a logger's level.
     *
     * @param loggerName
     *            the logger name
     * @param level
     *            the new level
     */
    public static void setLevel(final String loggerName, final Level level) {
        setLevel(LoggerContext.getContext(StackLocatorUtil.getCallerClassLoader(2), false, null), loggerName, level);
    }

    /**
     * Sets a logger's level.
     *
     * @param loggerName
     *            the logger name
     * @param level
     *            the new level
     */
    public static void setLevel(final String loggerName, final String level) {
        setLevel(
                LoggerContext.getContext(StackLocatorUtil.getCallerClassLoader(2), false, null),
                loggerName,
                Level.toLevel(level));
    }

    private static boolean setLevel(final String loggerName, final Level level, final Configuration config) {
        boolean set;
        LoggerConfig loggerConfig = config.getLoggerConfig(loggerName);
        if (!loggerName.equals(loggerConfig.getName())) {
            // TODO Should additivity be inherited?
            loggerConfig = new LoggerConfig(loggerName, level, true);
            config.addLogger(loggerName, loggerConfig);
            loggerConfig.setLevel(level);
            set = true;
        } else {
            set = setLevel(loggerConfig, level);
        }
        return set;
    }

    /**
     * Sets the root logger's level.
     *
     * @param level
     *            the new level
     */
    public static void setRootLevel(final Level level) {
        setRootLevel(level, LoggerContext.getContext(StackLocatorUtil.getCallerClassLoader(2), false, null));
    }

    private static void setRootLevel(final Level level, final LoggerContext loggerContext) {
        final LoggerConfig loggerConfig = loggerContext.getConfiguration().getRootLogger();
        if (!loggerConfig.getLevel().equals(level)) {
            loggerConfig.setLevel(level);
            loggerContext.updateLoggers();
        }
    }

    /**
     * Shuts down the given logger context. This request does not wait for Log4j tasks to complete.
     * <p>
     * Log4j starts threads to perform certain actions like file rollovers; calling this method will not wait until the
     * rollover thread is done. When this method returns, these tasks' status are undefined, the tasks may be done or
     * not.
     * </p>
     *
     * @param ctx
     *            the logger context to shut down, may be null.
     */
    public static void shutdown(final LoggerContext ctx) {
        if (ctx != null) {
            ctx.stop();
        }
    }

    /**
     * Shuts down the given logger context.
     * <p>
     * Log4j can start threads to perform certain actions like file rollovers; calling this method with a positive
     * timeout will block until the rollover thread is done.
     * </p>
     *
     * @param ctx
     *            the logger context to shut down, may be null.
     * @param timeout
     *            the maximum time to wait
     * @param timeUnit
     *            the time unit of the timeout argument
     * @return {@code true} if the logger context terminated and {@code false} if the timeout elapsed before
     *         termination.
     *
     * @see LoggerContext#stop(long, TimeUnit)
     *
     * @since 2.7
     */
    public static boolean shutdown(final LoggerContext ctx, final long timeout, final TimeUnit timeUnit) {
        if (ctx != null) {
            return ctx.stop(timeout, timeUnit);
        }
        return true;
    }

    private Configurator() {
        // empty
    }
}
