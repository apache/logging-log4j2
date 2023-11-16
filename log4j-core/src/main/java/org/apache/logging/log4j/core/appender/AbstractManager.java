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
package org.apache.logging.log4j.core.appender;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.AbstractLifeCycle;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Abstract base class used to register managers.
 * <p>
 * This class implements {@link AutoCloseable} mostly to allow unit tests to be written safely and succinctly. While
 * managers do need to allocate resources (usually on construction) and then free these resources, a manager is longer
 * lived than other auto-closeable objects like streams. None the less, making a manager AutoCloseable forces readers to
 * be aware of the pattern: allocate resources on construction and call {@link #close()} at some point.
 * </p>
 */
public abstract class AbstractManager implements AutoCloseable {

    /**
     * Implementations should extend this class for passing data between the getManager method and the manager factory
     * class.
     */
    protected abstract static class AbstractFactoryData {

        private final Configuration configuration;

        /**
         * Constructs the base factory data.
         *
         * @param configuration Configuration creating this instance.
         */
        protected AbstractFactoryData(final Configuration configuration) {
            this.configuration = configuration;
        }

        /**
         * Gets my configuration.
         *
         * @return my configuration.
         */
        public Configuration getConfiguration() {
            return configuration;
        }
    }

    /**
     * Allow subclasses access to the status logger without creating another instance.
     */
    protected static final Logger LOGGER = StatusLogger.getLogger();

    // Need to lock that map instead of using a ConcurrentMap due to stop removing the
    // manager from the map and closing the stream, requiring the whole stop method to be locked.
    private static final Map<String, AbstractManager> MAP = new HashMap<>();

    private static final Lock LOCK = new ReentrantLock();

    /**
     * Number of Appenders using this manager.
     */
    protected int count;

    private final String name;

    private final LoggerContext loggerContext;

    protected AbstractManager(final LoggerContext loggerContext, final String name) {
        this.loggerContext = loggerContext;
        this.name = name;
        LOGGER.debug("Starting {} {}", this.getClass().getSimpleName(), name);
    }

    /**
     * Called to signify that this Manager is no longer required by an Appender.
     */
    @Override
    public void close() {
        stop(AbstractLifeCycle.DEFAULT_STOP_TIMEOUT, AbstractLifeCycle.DEFAULT_STOP_TIMEUNIT);
    }

    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        boolean stopped = true;
        LOCK.lock();
        try {
            --count;
            if (count <= 0) {
                MAP.remove(name);
                LOGGER.debug("Shutting down {} {}", this.getClass().getSimpleName(), getName());
                stopped = releaseSub(timeout, timeUnit);
                LOGGER.debug(
                        "Shut down {} {}, all resources released: {}",
                        this.getClass().getSimpleName(),
                        getName(),
                        stopped);
            }
        } finally {
            LOCK.unlock();
        }
        return stopped;
    }

    /**
     * Retrieves a Manager if it has been previously created or creates a new Manager.
     * @param name The name of the Manager to retrieve.
     * @param factory The Factory to use to create the Manager.
     * @param data An Object that should be passed to the factory when creating the Manager.
     * @param <M> The Type of the Manager to be created.
     * @param <T> The type of the Factory data.
     * @return A Manager with the specified name and type.
     */
    // @SuppressWarnings("resource"): this is a factory method, the resource is allocated and released elsewhere.
    @SuppressWarnings("resource")
    public static <M extends AbstractManager, T> M getManager(
            final String name, final ManagerFactory<M, T> factory, final T data) {
        LOCK.lock();
        try {
            @SuppressWarnings("unchecked")
            M manager = (M) MAP.get(name);
            if (manager == null) {
                manager = Objects.requireNonNull(factory, "factory").createManager(name, data);
                if (manager == null) {
                    throw new IllegalStateException("ManagerFactory [" + factory + "] unable to create manager for ["
                            + name + "] with data [" + data + "]");
                }
                MAP.put(name, manager);
            } else {
                manager.updateData(data);
            }
            manager.count++;
            return manager;
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Used by Log4j to update the Manager during reconfiguration. This method should be considered private.
     * Implementations may not be thread safe. This method may be made protected in a future release.
     * @param data The data to update.
     */
    public void updateData(final Object data) {
        // This default implementation does nothing.
    }

    /**
     * Determines if a Manager with the specified name exists.
     * @param name The name of the Manager.
     * @return True if the Manager exists, false otherwise.
     */
    public static boolean hasManager(final String name) {
        LOCK.lock();
        try {
            return MAP.containsKey(name);
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Returns the specified manager, cast to the specified narrow type.
     * @param narrowClass the type to cast to
     * @param manager the manager object to return
     * @param <M> the narrow type
     * @return the specified manager, cast to the specified narrow type
     * @throws ConfigurationException if the manager cannot be cast to the specified type, which only happens when
     *          the configuration has multiple incompatible appenders pointing to the same resource
     * @since 2.9
     * @see <a href="https://issues.apache.org/jira/browse/LOG4J2-1908">LOG4J2-1908</a>
     */
    protected static <M extends AbstractManager> M narrow(final Class<M> narrowClass, final AbstractManager manager) {
        if (narrowClass.isAssignableFrom(manager.getClass())) {
            return (M) manager;
        }
        throw new ConfigurationException(
                "Configuration has multiple incompatible Appenders pointing to the same resource '" + manager.getName()
                        + "'");
    }

    protected static StatusLogger logger() {
        return StatusLogger.getLogger();
    }

    /**
     * For testing purposes.
     */
    static int getManagerCount() {
        return MAP.size();
    }

    /**
     * May be overridden by managers to perform processing while the manager is being released and the
     * lock is held. A timeout is passed for implementors to use as they see fit.
     * @param timeout timeout
     * @param timeUnit timeout time unit
     * @return true if all resources were closed normally, false otherwise.
     */
    protected boolean releaseSub(final long timeout, final TimeUnit timeUnit) {
        // This default implementation does nothing.
        return true;
    }

    protected int getCount() {
        return count;
    }

    /**
     * Gets the logger context used to create this instance or null. The logger context is usually set when an appender
     * creates a manager and that appender is given a Configuration. Not all appenders are given a Configuration by
     * their factory method or builder.
     *
     * @return the logger context used to create this instance or null.
     */
    public LoggerContext getLoggerContext() {
        return loggerContext;
    }

    /**
     * Called to signify that this Manager is no longer required by an Appender.
     * @deprecated In 2.7, use {@link #close()}.
     */
    @Deprecated
    public void release() {
        close();
    }

    /**
     * Returns the name of the Manager.
     * @return The name of the Manager.
     */
    public String getName() {
        return name;
    }

    /**
     * Provide a description of the content format supported by this Manager.  Default implementation returns an empty
     * (unspecified) Map.
     *
     * @return a Map of key/value pairs describing the Manager-specific content format, or an empty Map if no content
     * format descriptors are specified.
     */
    public Map<String, String> getContentFormat() {
        return new HashMap<>();
    }

    /**
     * Gets my configuration's StrSubstitutor or null.
     *
     * @return my configuration's StrSubstitutor or null.
     */
    protected StrSubstitutor getStrSubstitutor() {
        if (loggerContext == null) {
            return null;
        }
        final Configuration configuration = loggerContext.getConfiguration();
        if (configuration == null) {
            return null;
        }
        return configuration.getStrSubstitutor();
    }

    protected void log(final Level level, final String message, final Throwable throwable) {
        final Message m = LOGGER.getMessageFactory()
                .newMessage("{} {} {}: {}", getClass().getSimpleName(), getName(), message, throwable);
        LOGGER.log(level, m, throwable);
    }

    protected void logDebug(final String message, final Throwable throwable) {
        log(Level.DEBUG, message, throwable);
    }

    protected void logError(final String message, final Throwable throwable) {
        log(Level.ERROR, message, throwable);
    }

    protected void logWarn(final String message, final Throwable throwable) {
        log(Level.WARN, message, throwable);
    }
}
