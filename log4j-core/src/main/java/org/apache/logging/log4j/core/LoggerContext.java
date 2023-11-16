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
package org.apache.logging.log4j.core;

import static org.apache.logging.log4j.core.util.ShutdownCallbackRegistry.SHUTDOWN_HOOK_MARKER;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
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
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.jmx.Server;
import org.apache.logging.log4j.core.util.Cancellable;
import org.apache.logging.log4j.core.util.ExecutorServices;
import org.apache.logging.log4j.core.util.NetUtils;
import org.apache.logging.log4j.core.util.ShutdownCallbackRegistry;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.apache.logging.log4j.spi.LoggerContextShutdownAware;
import org.apache.logging.log4j.spi.LoggerContextShutdownEnabled;
import org.apache.logging.log4j.spi.LoggerRegistry;
import org.apache.logging.log4j.spi.Terminable;
import org.apache.logging.log4j.spi.ThreadContextMapFactory;
import org.apache.logging.log4j.util.PropertiesUtil;

/**
 * The LoggerContext is the anchor for the logging system. It maintains a list of all the loggers requested by
 * applications and a reference to the Configuration. The Configuration will contain the configured loggers, appenders,
 * filters, etc and will be atomically updated whenever a reconfigure occurs.
 */
public class LoggerContext extends AbstractLifeCycle
        implements org.apache.logging.log4j.spi.LoggerContext,
                AutoCloseable,
                Terminable,
                ConfigurationListener,
                LoggerContextShutdownEnabled {

    /**
     * Property name of the property change event fired if the configuration is changed.
     */
    public static final String PROPERTY_CONFIG = "config";

    private static final Configuration NULL_CONFIGURATION = new NullConfiguration();

    private final LoggerRegistry<Logger> loggerRegistry = new LoggerRegistry<>();
    private final CopyOnWriteArrayList<PropertyChangeListener> propertyChangeListeners = new CopyOnWriteArrayList<>();
    private volatile List<LoggerContextShutdownAware> listeners;

    /**
     * The Configuration is volatile to guarantee that initialization of the Configuration has completed before the
     * reference is updated.
     */
    private volatile Configuration configuration = new DefaultConfiguration();

    private static final String EXTERNAL_CONTEXT_KEY = "__EXTERNAL_CONTEXT_KEY__";
    private ConcurrentMap<String, Object> externalMap = new ConcurrentHashMap<>();
    private String contextName;
    private volatile URI configLocation;
    private Cancellable shutdownCallback;

    private final Lock configLock = new ReentrantLock();

    /**
     * Constructor taking only a name.
     *
     * @param name The context name.
     */
    public LoggerContext(final String name) {
        this(name, null, (URI) null);
    }

    /**
     * Constructor taking a name and a reference to an external context.
     *
     * @param name The context name.
     * @param externalContext The external context.
     */
    public LoggerContext(final String name, final Object externalContext) {
        this(name, externalContext, (URI) null);
    }

    /**
     * Constructor taking a name, external context and a configuration URI.
     *
     * @param name The context name.
     * @param externalContext The external context.
     * @param configLocn The location of the configuration as a URI.
     */
    public LoggerContext(final String name, final Object externalContext, final URI configLocn) {
        this.contextName = name;
        if (externalContext == null) {
            externalMap.remove(EXTERNAL_CONTEXT_KEY);
        } else {
            externalMap.put(EXTERNAL_CONTEXT_KEY, externalContext);
        }
        this.configLocation = configLocn;
    }

    /**
     * Constructor taking a name external context and a configuration location String. The location must be resolvable
     * to a File.
     *
     * @param name The configuration location.
     * @param externalContext The external context.
     * @param configLocn The configuration location.
     */
    @SuppressFBWarnings(
            value = "PATH_TRAVERSAL_IN",
            justification = "The configLocn comes from a secure source (Log4j properties)")
    public LoggerContext(final String name, final Object externalContext, final String configLocn) {
        this.contextName = name;
        if (externalContext == null) {
            externalMap.remove(EXTERNAL_CONTEXT_KEY);
        } else {
            externalMap.put(EXTERNAL_CONTEXT_KEY, externalContext);
        }
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
    public void addShutdownListener(final LoggerContextShutdownAware listener) {
        if (listeners == null) {
            synchronized (this) {
                if (listeners == null) {
                    listeners = new CopyOnWriteArrayList<LoggerContextShutdownAware>();
                }
            }
        }
        listeners.add(listener);
    }

    @Override
    public List<LoggerContextShutdownAware> getListeners() {
        return listeners;
    }

    /**
     * Returns the current LoggerContext.
     * <p>
     * Avoids the type cast for:
     * </p>
     *
     * <pre>
     * (LoggerContext) LogManager.getContext();
     * </pre>
     *
     * <p>
     * WARNING - The LoggerContext returned by this method may not be the LoggerContext used to create a Logger for the
     * calling class.
     * </p>
     *
     * @return The current LoggerContext.
     * @see LogManager#getContext()
     */
    public static LoggerContext getContext() {
        return (LoggerContext) LogManager.getContext();
    }

    /**
     * Returns a LoggerContext.
     * <p>
     * Avoids the type cast for:
     * </p>
     *
     * <pre>
     * (LoggerContext) LogManager.getContext(currentContext);
     * </pre>
     *
     * @param currentContext if false the LoggerContext appropriate for the caller of this method is returned. For
     *            example, in a web application if the caller is a class in WEB-INF/lib then one LoggerContext may be
     *            returned and if the caller is a class in the container's classpath then a different LoggerContext may
     *            be returned. If true then only a single LoggerContext will be returned.
     * @return a LoggerContext.
     * @see LogManager#getContext(boolean)
     */
    public static LoggerContext getContext(final boolean currentContext) {
        return (LoggerContext) LogManager.getContext(currentContext);
    }

    /**
     * Returns a LoggerContext.
     * <p>
     * Avoids the type cast for:
     * </p>
     *
     * <pre>
     * (LoggerContext) LogManager.getContext(loader, currentContext, configLocation);
     * </pre>
     *
     * @param loader The ClassLoader for the context. If null the context will attempt to determine the appropriate
     *            ClassLoader.
     * @param currentContext if false the LoggerContext appropriate for the caller of this method is returned. For
     *            example, in a web application if the caller is a class in WEB-INF/lib then one LoggerContext may be
     *            returned and if the caller is a class in the container's classpath then a different LoggerContext may
     *            be returned. If true then only a single LoggerContext will be returned.
     * @param configLocation The URI for the configuration to use.
     * @return a LoggerContext.
     * @see LogManager#getContext(ClassLoader, boolean, URI)
     */
    public static LoggerContext getContext(
            final ClassLoader loader, final boolean currentContext, final URI configLocation) {
        return (LoggerContext) LogManager.getContext(loader, currentContext, configLocation);
    }

    @Override
    public void start() {
        LOGGER.debug("Starting LoggerContext[name={}, {}]...", getName(), this);
        if (PropertiesUtil.getProperties().getBooleanProperty("log4j.LoggerContext.stacktrace.on.start", false)) {
            LOGGER.debug(
                    "Stack trace to locate invoker",
                    new Exception("Not a real error, showing stack trace to locate invoker"));
        }
        if (configLock.tryLock()) {
            try {
                if (this.isInitialized() || this.isStopped()) {
                    this.setStarting();
                    reconfigure();
                    if (this.configuration.isShutdownHookEnabled()) {
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
     * Starts with a specific configuration.
     *
     * @param config The new Configuration.
     */
    public void start(final Configuration config) {
        LOGGER.debug("Starting LoggerContext[name={}, {}] with configuration {}...", getName(), this, config);
        if (configLock.tryLock()) {
            try {
                if (this.isInitialized() || this.isStopped()) {
                    if (this.configuration.isShutdownHookEnabled()) {
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
                // LOG4J2-1642 preload ExecutorServices as it is used in shutdown hook
                ExecutorServices.ensureInitialized();
                try {
                    final long shutdownTimeoutMillis = this.configuration.getShutdownTimeoutMillis();
                    this.shutdownCallback = ((ShutdownCallbackRegistry) factory).addShutdownCallback(new Runnable() {
                        @Override
                        public void run() {
                            @SuppressWarnings("resource")
                            final LoggerContext context = LoggerContext.this;
                            LOGGER.debug(
                                    SHUTDOWN_HOOK_MARKER,
                                    "Stopping LoggerContext[name={}, {}]",
                                    context.getName(),
                                    context);
                            context.stop(shutdownTimeoutMillis, TimeUnit.MILLISECONDS);
                        }

                        @Override
                        public String toString() {
                            return "Shutdown callback for LoggerContext[name=" + LoggerContext.this.getName() + ']';
                        }
                    });
                } catch (final IllegalStateException e) {
                    throw new IllegalStateException(
                            "Unable to register Log4j shutdown hook because JVM is shutting down.", e);
                } catch (final SecurityException e) {
                    LOGGER.error(
                            SHUTDOWN_HOOK_MARKER, "Unable to register shutdown hook due to security restrictions", e);
                }
            }
        }
    }

    @Override
    public void close() {
        stop();
    }

    @Override
    public void terminate() {
        stop();
    }

    /**
     * Blocks until all Log4j tasks have completed execution after a shutdown request and all appenders have shut down,
     * or the timeout occurs, or the current thread is interrupted, whichever happens first.
     * <p>
     * Not all appenders will honor this, it is a hint and not an absolute guarantee that the this method not block longer.
     * Setting timeout too low increase the risk of losing outstanding log events not yet written to the final
     * destination.
     * <p>
     * Log4j can start threads to perform certain actions like file rollovers, calling this method with a positive timeout will
     * block until the rollover thread is done.
     *
     * @param timeout the maximum time to wait, or 0 which mean that each apppender uses its default timeout, and don't wait for background
     * tasks
     * @param timeUnit
     *            the time unit of the timeout argument
     * @return {@code true} if the logger context terminated and {@code false} if the timeout elapsed before
     *         termination.
     * @since 2.7
     */
    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        LOGGER.debug("Stopping LoggerContext[name={}, {}]...", getName(), this);
        configLock.lock();
        try {
            if (this.isStopped()) {
                return true;
            }

            this.setStopping();
            try {
                Server.unregisterLoggerContext(getName()); // LOG4J2-406, LOG4J2-500
            } catch (final LinkageError | Exception e) {
                // LOG4J2-1506 Hello Android, GAE
                LOGGER.error("Unable to unregister MBeans", e);
            }
            if (shutdownCallback != null) {
                shutdownCallback.cancel();
                shutdownCallback = null;
            }
            final Configuration prev = configuration;
            configuration = NULL_CONFIGURATION;
            updateLoggers();
            if (prev instanceof LifeCycle2) {
                ((LifeCycle2) prev).stop(timeout, timeUnit);
            } else {
                prev.stop();
            }
            externalMap.clear();
            LogManager.getFactory().removeContext(this);
        } finally {
            configLock.unlock();
            this.setStopped();
        }
        if (listeners != null) {
            for (LoggerContextShutdownAware listener : listeners) {
                try {
                    listener.contextShutdown(this);
                } catch (Exception ex) {
                    // Ignore the exception.
                }
            }
        }
        LOGGER.debug("Stopped LoggerContext[name={}, {}] with status {}", getName(), this, true);
        return true;
    }

    /**
     * Gets the name.
     *
     * @return the name.
     */
    public String getName() {
        return contextName;
    }

    /**
     * Gets the root logger.
     *
     * @return the root logger.
     */
    public Logger getRootLogger() {
        return getLogger(LogManager.ROOT_LOGGER_NAME);
    }

    /**
     * Sets the name.
     *
     * @param name the new LoggerContext name
     * @throws NullPointerException if the specified name is {@code null}
     */
    public void setName(final String name) {
        contextName = Objects.requireNonNull(name);
    }

    @Override
    public Object getObject(final String key) {
        return externalMap.get(key);
    }

    @Override
    public Object putObject(final String key, final Object value) {
        return externalMap.put(key, value);
    }

    @Override
    public Object putObjectIfAbsent(final String key, final Object value) {
        return externalMap.putIfAbsent(key, value);
    }

    @Override
    public Object removeObject(final String key) {
        return externalMap.remove(key);
    }

    @Override
    public boolean removeObject(final String key, final Object value) {
        return externalMap.remove(key, value);
    }

    /**
     * Sets the external context.
     *
     * @param context The external context.
     */
    public void setExternalContext(final Object context) {
        if (context != null) {
            this.externalMap.put(EXTERNAL_CONTEXT_KEY, context);
        } else {
            this.externalMap.remove(EXTERNAL_CONTEXT_KEY);
        }
    }

    /**
     * Returns the external context.
     *
     * @return The external context.
     */
    @Override
    public Object getExternalContext() {
        return this.externalMap.get(EXTERNAL_CONTEXT_KEY);
    }

    /**
     * Gets a Logger from the Context.
     *
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
     * Whether this collection is a copy of the underlying collection or not is undefined. Therefore, modify this
     * collection at your own risk.
     * </p>
     *
     * @return a collection of the current loggers.
     */
    public Collection<Logger> getLoggers() {
        return loggerRegistry.getLoggers();
    }

    /**
     * Obtains a Logger from the Context.
     *
     * @param name The name of the Logger to return.
     * @param messageFactory The message factory is used only when creating a logger, subsequent use does not change the
     *            logger but will log a warning if mismatched.
     * @return The Logger.
     */
    @Override
    public Logger getLogger(final String name, final MessageFactory messageFactory) {
        // Note: This is the only method where we add entries to the 'loggerRegistry' ivar.
        Logger logger = loggerRegistry.getLogger(name, messageFactory);
        if (logger != null) {
            AbstractLogger.checkMessageFactory(logger, messageFactory);
            return logger;
        }

        logger = newInstance(this, name, messageFactory);
        loggerRegistry.putIfAbsent(name, messageFactory, logger);
        return loggerRegistry.getLogger(name, messageFactory);
    }

    /**
     * Gets the LoggerRegistry.
     *
     * @return the LoggerRegistry.
     * @since 2.17.2
     */
    public LoggerRegistry<Logger> getLoggerRegistry() {
        return loggerRegistry;
    }

    /**
     * Determines if the specified Logger exists.
     *
     * @param name The Logger name to search for.
     * @return True if the Logger exists, false otherwise.
     */
    @Override
    public boolean hasLogger(final String name) {
        return loggerRegistry.hasLogger(name);
    }

    /**
     * Determines if the specified Logger exists.
     *
     * @param name The Logger name to search for.
     * @return True if the Logger exists, false otherwise.
     */
    @Override
    public boolean hasLogger(final String name, final MessageFactory messageFactory) {
        return loggerRegistry.hasLogger(name, messageFactory);
    }

    /**
     * Determines if the specified Logger exists.
     *
     * @param name The Logger name to search for.
     * @return True if the Logger exists, false otherwise.
     */
    @Override
    public boolean hasLogger(final String name, final Class<? extends MessageFactory> messageFactoryClass) {
        return loggerRegistry.hasLogger(name, messageFactoryClass);
    }

    /**
     * Returns the current Configuration. The Configuration will be replaced when a reconfigure occurs.
     *
     * @return The current Configuration, never {@code null}, but may be
     * {@link org.apache.logging.log4j.core.config.NullConfiguration}.
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Adds a Filter to the Configuration. Filters that are added through the API will be lost when a reconfigure
     * occurs.
     *
     * @param filter The Filter to add.
     */
    public void addFilter(final Filter filter) {
        configuration.addFilter(filter);
    }

    /**
     * Removes a Filter from the current Configuration.
     *
     * @param filter The Filter to remove.
     */
    public void removeFilter(final Filter filter) {
        configuration.removeFilter(filter);
    }

    /**
     * Sets the Configuration to be used.
     *
     * @param config The new Configuration.
     * @return The previous Configuration.
     */
    public Configuration setConfiguration(final Configuration config) {
        if (config == null) {
            LOGGER.error("No configuration found for context '{}'.", contextName);
            // No change, return the current configuration.
            return this.configuration;
        }
        configLock.lock();
        try {
            final Configuration prev = this.configuration;
            config.addListener(this);

            final ConcurrentMap<String, String> map = config.getComponent(Configuration.CONTEXT_PROPERTIES);

            try { // LOG4J2-719 network access may throw android.os.NetworkOnMainThreadException
                // LOG4J2-2808 don't block unless necessary
                map.computeIfAbsent("hostName", s -> NetUtils.getLocalHostname());
            } catch (final Exception ex) {
                LOGGER.debug("Ignoring {}, setting hostName to 'unknown'", ex.toString());
                map.putIfAbsent("hostName", "unknown");
            }
            map.putIfAbsent("contextName", contextName);
            config.start();
            this.configuration = config;
            updateLoggers();
            if (prev != null) {
                prev.removeListener(this);
                prev.stop();
            }

            firePropertyChangeEvent(new PropertyChangeEvent(this, PROPERTY_CONFIG, prev, config));

            try {
                Server.reregisterMBeansAfterReconfigure();
            } catch (final LinkageError | Exception e) {
                // LOG4J2-716: Android has no java.lang.management
                LOGGER.error("Could not reconfigure JMX", e);
            }
            // AsyncLoggers update their nanoClock when the configuration changes
            Log4jLogEvent.setNanoClock(configuration.getNanoClock());

            return prev;
        } finally {
            configLock.unlock();
        }
    }

    private void firePropertyChangeEvent(final PropertyChangeEvent event) {
        for (final PropertyChangeListener listener : propertyChangeListeners) {
            listener.propertyChange(event);
        }
    }

    public void addPropertyChangeListener(final PropertyChangeListener listener) {
        propertyChangeListeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public void removePropertyChangeListener(final PropertyChangeListener listener) {
        propertyChangeListeners.remove(listener);
    }

    /**
     * Returns the initial configuration location or {@code null}. The returned value may not be the location of the
     * current configuration. Use {@link #getConfiguration()}.{@link Configuration#getConfigurationSource()
     * getConfigurationSource()}.{@link ConfigurationSource#getLocation() getLocation()} to get the actual source of the
     * current configuration.
     *
     * @return the initial configuration location or {@code null}
     */
    public URI getConfigLocation() {
        return configLocation;
    }

    /**
     * Sets the configLocation to the specified value and reconfigures this context.
     *
     * @param configLocation the location of the new configuration
     */
    public void setConfigLocation(final URI configLocation) {
        this.configLocation = configLocation;
        reconfigure(configLocation);
    }

    /**
     * Reconfigures the context.
     */
    private void reconfigure(final URI configURI) {
        final Object externalContext = externalMap.get(EXTERNAL_CONTEXT_KEY);
        final ClassLoader cl = ClassLoader.class.isInstance(externalContext) ? (ClassLoader) externalContext : null;
        LOGGER.debug(
                "Reconfiguration started for context[name={}] at URI {} ({}) with optional ClassLoader: {}",
                contextName,
                configURI,
                this,
                cl);
        final Configuration instance =
                ConfigurationFactory.getInstance().getConfiguration(this, contextName, configURI, cl);
        if (instance == null) {
            LOGGER.error(
                    "Reconfiguration failed: No configuration found for '{}' at '{}' in '{}'",
                    contextName,
                    configURI,
                    cl);
        } else {
            setConfiguration(instance);
            /*
             * instance.start(); Configuration old = setConfiguration(instance); updateLoggers(); if (old != null) {
             * old.stop(); }
             */
            final String location =
                    configuration == null ? "?" : String.valueOf(configuration.getConfigurationSource());
            LOGGER.debug(
                    "Reconfiguration complete for context[name={}] at URI {} ({}) with optional ClassLoader: {}",
                    contextName,
                    location,
                    this,
                    cl);
        }
    }

    /**
     * Reconfigures the context. Log4j does not remove Loggers during a reconfiguration. Log4j will create new
     * LoggerConfig objects and Log4j will point the Loggers at the new LoggerConfigs. Log4j will free the old
     * LoggerConfig, along with old Appenders and Filters.
     */
    public void reconfigure() {
        reconfigure(configLocation);
    }

    public void reconfigure(Configuration configuration) {
        setConfiguration(configuration);
        final ConfigurationSource source = configuration.getConfigurationSource();
        if (source != null) {
            final URI uri = source.getURI();
            if (uri != null) {
                configLocation = uri;
            }
        }
    }

    /**
     * Causes all Loggers to be updated against the current Configuration.
     */
    public void updateLoggers() {
        updateLoggers(this.configuration);
    }

    /**
     * Causes all Logger to be updated against the specified Configuration.
     *
     * @param config The Configuration.
     */
    public void updateLoggers(final Configuration config) {
        final Configuration old = this.configuration;
        for (final Logger logger : loggerRegistry.getLoggers()) {
            logger.updateConfiguration(config);
        }
        firePropertyChangeEvent(new PropertyChangeEvent(this, PROPERTY_CONFIG, old, config));
    }

    /**
     * Causes a reconfiguration to take place when the underlying configuration file changes.
     *
     * @param reconfigurable The Configuration that can be reconfigured.
     */
    @Override
    public synchronized void onChange(final Reconfigurable reconfigurable) {
        final long startMillis = System.currentTimeMillis();
        LOGGER.debug("Reconfiguration started for context {} ({})", contextName, this);
        initApiModule();
        final Configuration newConfig = reconfigurable.reconfigure();
        if (newConfig != null) {
            setConfiguration(newConfig);
            LOGGER.debug(
                    "Reconfiguration completed for {} ({}) in {} milliseconds.",
                    contextName,
                    this,
                    System.currentTimeMillis() - startMillis);
        } else {
            LOGGER.debug(
                    "Reconfiguration failed for {} ({}) in {} milliseconds.",
                    contextName,
                    this,
                    System.currentTimeMillis() - startMillis);
        }
    }

    private void initApiModule() {
        ThreadContextMapFactory
                .init(); // Or make public and call ThreadContext.init() which calls ThreadContextMapFactory.init().
    }

    // LOG4J2-151: changed visibility from private to protected
    protected Logger newInstance(final LoggerContext ctx, final String name, final MessageFactory messageFactory) {
        return new Logger(ctx, name, messageFactory);
    }
}
