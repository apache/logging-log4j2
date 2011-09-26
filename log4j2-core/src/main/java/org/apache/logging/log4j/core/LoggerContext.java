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
package org.apache.logging.log4j.core;

import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationListener;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.spi.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The LoggerContext is the anchor for the logging system. It maintains a list of all the loggers requested by
 * applications and a reference to the Configuration. The Configuration will contain the configured loggers, appenders,
 * filters, etc and will be atomically updated whenever a reconfigure occurs.
 */
public class LoggerContext implements org.apache.logging.log4j.spi.LoggerContext, ConfigurationListener {

    private static StatusLogger logger = StatusLogger.getLogger();

    private static final LoggerFactory<LoggerContext> FACTORY = new Factory();

    private static final long JVM_START_TIME = System.currentTimeMillis();

    private final ConcurrentMap<String, Logger> loggers = new ConcurrentHashMap<String, Logger>();

    /**
     * The Configuration is volatile to guarantee that initialization of the Configuration has completed before
     * the reference is updated.
     */
    private volatile Configuration config;

    private Object externalContext = null;

    private final String contextName;

    private final URI configLocation;

    /**
     * Constructor taking only a name.
     * @param name The context name.
     */
    public LoggerContext(String name) {
        this(name, null, (URI) null);
    }

    /**
     * Constructor taking a name and a reference to an external context.
     * @param name The context name.
     * @param externalContext The external context.
     */
    public LoggerContext(String name, Object externalContext) {
        this(name, externalContext, (URI) null);
    }

    /**
     * Constructor taking a name, external context and a configuration URI.
     * @param name The context name.
     * @param externalContext The external context.
     * @param configLocn The location of the configuration as a URI.
     */
    public LoggerContext(String name, Object externalContext, URI configLocn) {
        contextName = name;
        this.externalContext = externalContext;
        this.configLocation = configLocn;
        reconfigure();
    }

    /**
     * Constructor taking a name external context and a configuration location String. The location
     * must be resolvable to a File.
     * @param name The configuration location.
     * @param externalContext The external context.
     * @param configLocn The configuration location.
     */
    public LoggerContext(String name, Object externalContext, String configLocn) {
        contextName = name;
        this.externalContext = externalContext;
        if (configLocn != null) {
            URI uri;
            try {
                uri = new File(configLocn).toURI();
            } catch (Exception ex) {
                uri = null;
            }
            configLocation = uri;
        } else {
            configLocation = null;
        }
        reconfigure();
    }

    /**
     * The time the LoggerContext class was loaded as a long.
     * @return The time the LoggerContext was loaded.
     */
    public static long getStartTime() {
        return JVM_START_TIME;
    }

    /**
     * Set the external context.
     * @param context The external context.
     */
    public void setExternalContext(Object context) {
        this.externalContext = context;
    }

    /**
     * Return the external context.
     * @return The external context.
     */
    public Object getExternalContext() {
        return this.externalContext;
    }

    /**
     * Obtain a Logger from the Context.
     * @param name The name of the Logger to return.
     * @return The Logger.
     */
    public Logger getLogger(String name) {
        return getLogger(FACTORY, name);
    }

    /**
     * Obtain a Logger from the Context using the specified LoggerFactory.
     * @param factory The LoggerFactory.
     * @param name The name of the Logger.
     * @return The Logger.
     */
    public Logger getLogger(LoggerFactory factory, String name) {

        Logger logger = loggers.get(name);
        if (logger != null) {
            return logger;
        }

        logger = (Logger) factory.newInstance(this, name);
        Logger prev = loggers.putIfAbsent(name, logger);
        return prev == null ? logger : prev;
    }

    /**
     * Determine if the specified Logger exists.
     * @param name The Logger name to search for.
     * @return True if the Logger exists, false otherwise.
     */
    public boolean hasLogger(String name) {
        return loggers.containsKey(name);
    }

    /**
     * Return the current Configuration. The Configuration will be replaced when a reconfigure occurs.
     * @return The Configuration.
     */
    public Configuration getConfiguration() {
        return config;
    }

    /**
     * Add a Filter to the Configuration. Filters that are added through the API will be lost
     * when a reconfigure occurs.
     * @param filter The Filter to add.
     */
    public void addFilter(Filter filter) {
        config.addFilter(filter);
    }

    /**
     * Removes a Filter from the current Configuration.
     * @param filter The Filter to remove.
     */
    public void removeFiler(Filter filter) {
        config.removeFilter(filter);
    }

    /**
     * Shutdown the logging system.
     */
    public synchronized void shutdown() {
        updateLoggers(new NullConfiguration());
        config.stop();
        externalContext = null;
    }

    /**
     * Set the Configuration to be used.
     * @param config The new Configuration.
     * @return The previous Configuration.
     */
    public synchronized Configuration setConfiguration(Configuration config) {
        if (config == null) {
            throw new NullPointerException("No Configuration was provided");
        }
        Configuration prev = this.config;
        config.addListener(this);
        config.start();
        this.config = config;
        updateLoggers();
        if (prev != null) {
            prev.removeListener(this);
            prev.stop();
        }
        return prev;
    }

    /**
     *  Reconfigure the context.
     */
    public synchronized void reconfigure() {
        logger.debug("Reconfiguration started for context " + contextName);
        Configuration instance = ConfigurationFactory.getInstance().getConfiguration(contextName, configLocation);
        setConfiguration(instance);
        /*instance.start();
        Configuration old = setConfiguration(instance);
        updateLoggers();
        if (old != null) {
            old.stop();
        } */
        logger.debug("Reconfiguration completed");
    }

    /**
     * Cause all Loggers to be updated against the current Configuration.
     */
    public void updateLoggers() {
        updateLoggers(this.config);
    }

    /**
     * Cause all Logger to be updated against the specified Configuration.
     * @param config The Configuration.
     */
    public void updateLoggers(Configuration config) {
        for (Logger logger : loggers.values()) {
            logger.updateConfiguration(config);
        }
    }

    /**
     * Cause a reconfiguration to take place when the underlying configuration file changes.
     */
    public void onChange() {
        reconfigure();
    }

    /**
     * The default LoggerFactory.
     */
    private static class Factory implements LoggerFactory<LoggerContext> {

        public Logger newInstance(LoggerContext ctx, String name) {
            return new Logger(ctx, name);
        }
    }
}
