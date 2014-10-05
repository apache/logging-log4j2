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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationListener;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.core.config.Reconfigurable;
import org.apache.logging.log4j.core.jmx.Server;
import org.apache.logging.log4j.core.util.Assert;
import org.apache.logging.log4j.core.util.Cancellable;
import org.apache.logging.log4j.core.util.NetUtils;
import org.apache.logging.log4j.core.util.ShutdownCallbackRegistry;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.spi.LoggerContextFactory;

import static org.apache.logging.log4j.core.util.ShutdownCallbackRegistry.SHUTDOWN_HOOK_MARKER;

/**
 * The LoggerContext is the anchor for the logging system. It maintains a list
 * of all the loggers requested by applications and a reference to the
 * Configuration. The Configuration will contain the configured loggers,
 * appenders, filters, etc and will be atomically updated whenever a reconfigure
 * occurs.
 */
public class LoggerContext extends AbstractLifeCycle implements org.apache.logging.log4j.spi.LoggerContext, ConfigurationListener {

    private static final long serialVersionUID = 1L;

    public static final String PROPERTY_CONFIG = "config";
    private static final Configuration NULL_CONFIGURATION = new NullConfiguration();

    private final ConcurrentMap<String, Logger> loggers = new ConcurrentHashMap<String, Logger>();
    private final CopyOnWriteArrayList<PropertyChangeListener> propertyChangeListeners = new CopyOnWriteArrayList<PropertyChangeListener>();

    /**
     * The Configuration is volatile to guarantee that initialization of the
     * Configuration has completed before the reference is updated.
     */
    private volatile Configuration config = new DefaultConfiguration();
    private Object externalContext;
    private final String name;
    private URI configLocation;
    private Cancellable shutdownCallback;

    private final Lock configLock = new ReentrantLock();

    /**
     * Constructor taking only a name.
     * @param name The context name.
     */
    public LoggerContext(final String name) {
        this(name, null, (URI) null);
    }

    /**
     * Constructor taking a name and a reference to an external context.
     * @param name The context name.
     * @param externalContext The external context.
     */
    public LoggerContext(final String name, final Object externalContext) {
        this(name, externalContext, (URI) null);
    }

    /**
     * Constructor taking a name, external context and a configuration URI.
     * @param name The context name.
     * @param externalContext The external context.
     * @param configLocn The location of the configuration as a URI.
     */
    public LoggerContext(final String name, final Object externalContext, final URI configLocn) {
        this.name = name;
        this.externalContext = externalContext;
        this.configLocation = configLocn;
    }

    /**
     * Constructor taking a name external context and a configuration location
     * String. The location must be resolvable to a File.
     *
     * @param name The configuration location.
     * @param externalContext The external context.
     * @param configLocn The configuration location.
     */
    public LoggerContext(final String name, final Object externalContext, final String configLocn) {
        this.name = name;
        this.externalContext = externalContext;
        if (configLocn != null) {
            URI uri;
            try {
                uri = new File(configLocn).toURI();
            } catch (final Exception ex) {
                uri = null;
            }
            configLocation = uri;
        } else {
            configLocation = null;
        }
    }

    @Override
    public void start() {
        LOGGER.debug("Starting LoggerContext[name={}, {}]...", getName(), this);
        if (configLock.tryLock()) {
            try {
                if (this.isInitialized() || this.isStopped()) {
                    this.setStarting();
                    reconfigure();
                    if (this.config.isShutdownHookEnabled()) {
                        setUpShutdownHook();
                    }
                    this.setStarted();
                }
            } finally {
                configLock.unlock();
            }
        }
        LOGGER.debug("LoggerContext[name={}, {}] started OK.", getName(), this);
    }

    /**
     * Start with a specific configuration.
     * @param config The new Configuration.
     */
    public void start(final Configuration config) {
        LOGGER.debug("Starting LoggerContext[name={}, {}] with configuration {}...", getName(), this, config);
        if (configLock.tryLock()) {
            try {
                if (this.isInitialized() || this.isStopped()) {
                    if (this.config.isShutdownHookEnabled()) {
                        setUpShutdownHook();
                    }
                    this.setStarted();
                }
            } finally {
                configLock.unlock();
            }
        }
        setConfiguration(config);
        LOGGER.debug("LoggerContext[name={}, {}] started OK with configuration {}.", getName(), this, config);
    }

    private void setUpShutdownHook() {
        if (shutdownCallback == null) {
            final LoggerContextFactory factory = LogManager.getFactory();
            if (factory instanceof ShutdownCallbackRegistry) {
                LOGGER.debug(SHUTDOWN_HOOK_MARKER, "Shutdown hook enabled. Registering a new one.");
                try {
                    this.shutdownCallback = ((ShutdownCallbackRegistry) factory).addShutdownCallback(new Runnable() {
                        @Override
                        public void run() {
                            final LoggerContext context = LoggerContext.this;
                            LOGGER.debug(SHUTDOWN_HOOK_MARKER, "Stopping LoggerContext[name={}, {}]", context.getName(),
                                context);
                            context.stop();
                        }

                        @Override
                        public String toString() {
                            return "Shutdown callback for LoggerContext[name=" + LoggerContext.this.getName() + ']';
                        }
                    });
                } catch (final IllegalStateException ise) {
                    LOGGER.fatal(SHUTDOWN_HOOK_MARKER, "Unable to register shutdown hook because JVM is shutting down.");
                } catch (final SecurityException se) {
                    LOGGER.error(SHUTDOWN_HOOK_MARKER, "Unable to register shutdown hook due to security restrictions");
                }
            }
        }
    }

    @Override
    public void stop() {
        LOGGER.debug("Stopping LoggerContext[name={}, {}]...", getName(), this);
        configLock.lock();
        try {
            if (this.isStopped()) {
                return;
            }
            this.setStopping();
            if (shutdownCallback != null) {
                shutdownCallback.cancel();
                shutdownCallback = null;
            }
            final Configuration prev = config;
            config = NULL_CONFIGURATION;
            updateLoggers();
            prev.stop();
            externalContext = null;
            LogManager.getFactory().removeContext(this);
            this.setStopped();
        } finally {
            configLock.unlock();

            // in finally: unregister MBeans even if an exception occurred while stopping
            Server.unregisterLoggerContext(getName()); // LOG4J2-406, LOG4J2-500
        }
        LOGGER.debug("Stopped LoggerContext[name={}, {}]...", getName(), this);
    }

    /**
     * Gets the name.
     *
     * @return the name.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the external context.
     * @param context The external context.
     */
    public void setExternalContext(final Object context) {
        this.externalContext = context;
    }

    /**
     * Returns the external context.
     * @return The external context.
     */
    @Override
    public Object getExternalContext() {
        return this.externalContext;
    }

    /**
     * Obtain a Logger from the Context.
     * @param name The name of the Logger to return.
     * @return The Logger.
     */
    @Override
    public Logger getLogger(final String name) {
        return getLogger(name, null);
    }

    /**
     * Gets a collection of the current loggers.
     * <p>
     * Whether this collection is a copy of the underlying collection or not is undefined. Therefore, modify this collection at your own
     * risk.
     * </p>
     *
     * @return a collection of the current loggers.
     */
    public Collection<Logger> getLoggers() {
        return loggers.values();
    }

    /**
     * Obtain a Logger from the Context.
     * @param name The name of the Logger to return.
     * @param messageFactory The message factory is used only when creating a
     *            logger, subsequent use does not change the logger but will log
     *            a warning if mismatched.
     * @return The Logger.
     */
    @Override
    public Logger getLogger(final String name, final MessageFactory messageFactory) {
        Logger logger = loggers.get(name);
        if (logger != null) {
            AbstractLogger.checkMessageFactory(logger, messageFactory);
            return logger;
        }

        logger = newInstance(this, name, messageFactory);
        final Logger prev = loggers.putIfAbsent(name, logger);
        return prev == null ? logger : prev;
    }

    /**
     * Determine if the specified Logger exists.
     * @param name The Logger name to search for.
     * @return True if the Logger exists, false otherwise.
     */
    @Override
    public boolean hasLogger(final String name) {
        return loggers.containsKey(name);
    }

    /**
     * Returns the current Configuration. The Configuration will be replaced
     * when a reconfigure occurs.
     *
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
    public void addFilter(final Filter filter) {
        config.addFilter(filter);
    }

    /**
     * Removes a Filter from the current Configuration.
     * @param filter The Filter to remove.
     */
    public void removeFilter(final Filter filter) {
        config.removeFilter(filter);
    }

    /**
     * Set the Configuration to be used.
     * @param config The new Configuration.
     * @return The previous Configuration.
     */
    private synchronized Configuration setConfiguration(final Configuration config) {
        Assert.requireNonNull(config, "No Configuration was provided");
        final Configuration prev = this.config;
        config.addListener(this);
        final ConcurrentMap<String, String> map = config.getComponent(Configuration.CONTEXT_PROPERTIES);

        try { // LOG4J2-719 network access may throw android.os.NetworkOnMainThreadException
            map.putIfAbsent("hostName", NetUtils.getLocalHostname());
        } catch (final Exception ex) {
            LOGGER.debug("Ignoring {}, setting hostName to 'unknown'", ex.toString());
            map.putIfAbsent("hostName", "unknown");
        }
        map.putIfAbsent("contextName", name);
        config.start();
        this.config = config;
        updateLoggers();
        if (prev != null) {
            prev.removeListener(this);
            prev.stop();
        }

        firePropertyChangeEvent(new PropertyChangeEvent(this, PROPERTY_CONFIG, prev, config));

        try {
            Server.reregisterMBeansAfterReconfigure();
        } catch (final Throwable t) {
            // LOG4J2-716: Android has no java.lang.management
            LOGGER.error("Could not reconfigure JMX", t);
        }
        return prev;
    }

    private void firePropertyChangeEvent(final PropertyChangeEvent event) {
        for (final PropertyChangeListener listener : propertyChangeListeners) {
            listener.propertyChange(event);
        }
    }

    public void addPropertyChangeListener(final PropertyChangeListener listener) {
        propertyChangeListeners.add(Assert.requireNonNull(listener, "listener"));
    }

    public void removePropertyChangeListener(final PropertyChangeListener listener) {
        propertyChangeListeners.remove(listener);
    }

    /**
     * Returns the initial configuration location or {@code null}. The returned value may not be the location of the
     * current configuration. Use
     * {@link #getConfiguration()}.{@link Configuration#getConfigurationSource() getConfigurationSource()}.{@link
     * ConfigurationSource#getLocation() getLocation()} to get the actual source of the current configuration.
     * @return the initial configuration location or {@code null}
     */
    public synchronized URI getConfigLocation() {
        return configLocation;
    }

    /**
     * Sets the configLocation to the specified value and reconfigures this context.
     * @param configLocation the location of the new configuration
     */
    public synchronized void setConfigLocation(final URI configLocation) {
        this.configLocation = configLocation;
        reconfigure();
    }

    /**
     * Reconfigure the context.
     */
    public synchronized void reconfigure() {
        final ClassLoader cl = ClassLoader.class.isInstance(externalContext) ? (ClassLoader) externalContext : null;
        LOGGER.debug("Reconfiguration started for context[name={}] at {} ({}) with optional ClassLoader: {}", name,
            configLocation, this, cl);
        final Configuration instance = ConfigurationFactory.getInstance().getConfiguration(name, configLocation, cl);
        setConfiguration(instance);
        /*
         * instance.start(); Configuration old = setConfiguration(instance);
         * updateLoggers(); if (old != null) { old.stop(); }
         */

        LOGGER.debug("Reconfiguration complete for context[name={}] at {} ({}) with optional ClassLoader: {}", name,
            configLocation, this, cl);
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
    public void updateLoggers(final Configuration config) {
        for (final Logger logger : loggers.values()) {
            logger.updateConfiguration(config);
        }
    }

    /**
     * Cause a reconfiguration to take place when the underlying configuration
     * file changes.
     *
     * @param reconfigurable The Configuration that can be reconfigured.
     */
    @Override
    public synchronized void onChange(final Reconfigurable reconfigurable) {
        LOGGER.debug("Reconfiguration started for context {} ({})", name, this);
        final Configuration config = reconfigurable.reconfigure();
        if (config != null) {
            setConfiguration(config);
            LOGGER.debug("Reconfiguration completed for {} ({})", name, this);
        } else {
            LOGGER.debug("Reconfiguration failed for {} ({})", name, this);
        }
    }

    // LOG4J2-151: changed visibility from private to protected
    protected Logger newInstance(final LoggerContext ctx, final String name, final MessageFactory messageFactory) {
        return new Logger(ctx, name, messageFactory);
    }

}
