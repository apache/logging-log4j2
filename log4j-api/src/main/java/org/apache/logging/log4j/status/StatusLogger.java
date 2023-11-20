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
package org.apache.logging.log4j.status;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ParameterizedNoReferenceMessageFactory;
import org.apache.logging.log4j.simple.SimpleLogger;
import org.apache.logging.log4j.simple.SimpleLoggerContext;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.spi.LoggingSystemProperty;
import org.apache.logging.log4j.util.LowLevelLogUtil;
import org.apache.logging.log4j.util.QueueFactories;

/**
 * Records events that occur in the logging system. By default, only error messages are logged to {@link System#err}.
 * Normally, the Log4j StatusLogger is configured via the root {@code <Configuration status="LEVEL"/>} node in a Log4j
 * configuration file. However, this can be overridden via a system property named
 * {@link LoggingSystemProperty#STATUS_DEFAULT_LISTENER_LEVEL} and will work with any Log4j provider.
 *
 * @see SimpleLogger
 * @see SimpleLoggerContext
 */
public final class StatusLogger extends AbstractLogger {

    private static final String NOT_AVAIL = "?";

    private static final StatusLogger STATUS_LOGGER =
            StatusLoggerFactory.getInstance().createStatusLogger();

    static {
        // now safe to use StatusLogger in LowLevelLogUtil
        LowLevelLogUtil.setLogger(STATUS_LOGGER);
    }

    private final SimpleLogger logger;
    private final StatusLoggerConfiguration configuration;

    private final Collection<StatusListener> listeners = new CopyOnWriteArrayList<>();

    private final ReadWriteLock listenersLock = new ReentrantReadWriteLock();

    private final Queue<StatusData> messages;

    private int listenersLevel;

    /**
     * Constructs the singleton instance for the STATUS_LOGGER constant.
     * <p>
     * This is now the logger level is set:
     * </p>
     * <ol>
     * <li>If the property {@link LoggingSystemProperty#STATUS_LOGGER_DEBUG} is {@code "true"}, then use {@link Level#TRACE}, otherwise,</li>
     * <li>Use {@link Level#ERROR}</li>
     * </ol>
     * <p>
     * This is now the listener level is set:
     * </p>
     * <ol>
     * <li>If the property {@link LoggingSystemProperty#STATUS_DEFAULT_LISTENER_LEVEL} is set, then use <em>it</em>, otherwise,</li>
     * <li>Use {@link Level#WARN}</li>
     * </ol>
     * <p>
     * See:
     * <ol>
     * <li>LOG4J2-1813 Provide shorter and more intuitive way to switch on Log4j internal debug logging. If system property
     * {@link LoggingSystemProperty#STATUS_LOGGER_DEBUG} is defined, print all status logging.</li>
     * <li>LOG4J2-3340 StatusLogger's log Level cannot be changed as advertised.</li>
     * </ol>
     * </p>
     */
    StatusLogger(final SimpleLogger logger, final StatusLoggerConfiguration configuration) {
        super(StatusLogger.class.getName(), ParameterizedNoReferenceMessageFactory.INSTANCE);
        this.logger = logger;
        this.configuration = configuration;
        this.listenersLevel = configuration.getDefaultLevel().intLevel();
        messages = QueueFactories.MPMC.create(configuration.getMaxEntries());
    }

    /**
     * Retrieve the StatusLogger.
     *
     * @return The StatusLogger.
     */
    public static StatusLogger getLogger() {
        return STATUS_LOGGER;
    }

    public void setLevel(final Level level) {
        logger.setLevel(level);
    }

    /**
     * Registers a new listener.
     *
     * @param listener The StatusListener to register.
     */
    public void registerListener(final StatusListener listener) {
        listenersLock.writeLock().lock();
        try {
            listeners.add(listener);
            final Level lvl = listener.getStatusLevel();
            if (listenersLevel < lvl.intLevel()) {
                listenersLevel = lvl.intLevel();
            }
        } finally {
            listenersLock.writeLock().unlock();
        }
    }

    /**
     * Removes a StatusListener.
     *
     * @param listener The StatusListener to remove.
     */
    public void removeListener(final StatusListener listener) {
        closeSilently(listener);
        listenersLock.writeLock().lock();
        try {
            listeners.remove(listener);
            int lowest = configuration.getDefaultLevel().intLevel();
            for (final StatusListener statusListener : listeners) {
                final int level = statusListener.getStatusLevel().intLevel();
                if (lowest < level) {
                    lowest = level;
                }
            }
            listenersLevel = lowest;
        } finally {
            listenersLock.writeLock().unlock();
        }
    }

    public void updateListenerLevel(final Level status) {
        if (status.intLevel() > listenersLevel) {
            listenersLevel = status.intLevel();
        }
    }

    /**
     * Returns a thread safe Iterable for the StatusListener.
     *
     * @return An Iterable for the list of StatusListeners.
     */
    public Iterable<StatusListener> getListeners() {
        return listeners;
    }

    /**
     * Clears the list of status events and listeners.
     */
    public void reset() {
        messages.clear();
        listenersLock.writeLock().lock();
        try {
            for (final StatusListener listener : listeners) {
                closeSilently(listener);
            }
        } finally {
            listeners.clear();
            listenersLock.writeLock().unlock();
        }
    }

    private static void closeSilently(final Closeable resource) {
        try {
            resource.close();
        } catch (final IOException ignored) {
            // ignored
        }
    }

    /**
     * Returns a List of all events as StatusData objects.
     *
     * @return The list of StatusData objects.
     */
    public List<StatusData> getStatusData() {
        return List.copyOf(messages);
    }

    /**
     * Clears the list of status events.
     */
    public void clear() {
        messages.clear();
    }

    @Override
    public Level getLevel() {
        return logger.getLevel();
    }

    /**
     * Adds an event.
     *
     * @param marker The Marker
     * @param fqcn The fully qualified class name of the <b>caller</b>
     * @param level The logging level
     * @param msg The message associated with the event.
     * @param t A Throwable or null.
     */
    @Override
    public void logMessage(
            final String fqcn, final Level level, final Marker marker, final Message msg, final Throwable t) {
        StackTraceElement element = null;
        if (fqcn != null) {
            element = getStackTraceElement(fqcn, Thread.currentThread().getStackTrace());
        }
        final StatusData data = new StatusData(element, level, msg, t, null);
        while (!messages.offer(data)) {
            // discard an old message and retry
            messages.poll();
        }
        // LOG4J2-1813 if system property "log4j2.debug" is defined, all status logging is enabled
        if (configuration.isDebugEnabled() || listeners.isEmpty()) {
            logger.logMessage(fqcn, level, marker, msg, t);
        } else {
            for (final StatusListener listener : listeners) {
                if (data.getLevel().isMoreSpecificThan(listener.getStatusLevel())) {
                    listener.log(data);
                }
            }
        }
    }

    private StackTraceElement getStackTraceElement(final String fqcn, final StackTraceElement[] stackTrace) {
        if (fqcn == null) {
            return null;
        }
        boolean next = false;
        for (final StackTraceElement element : stackTrace) {
            final String className = element.getClassName();
            if (next && !fqcn.equals(className)) {
                return element;
            }
            if (fqcn.equals(className)) {
                next = true;
            } else if (NOT_AVAIL.equals(className)) {
                break;
            }
        }
        return null;
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String message, final Throwable t) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String message) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String message, final Object... params) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String message, final Object p0) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(
            final Level level, final Marker marker, final String message, final Object p0, final Object p1) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8,
            final Object p9) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final CharSequence message, final Throwable t) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final Object message, final Throwable t) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final Message message, final Throwable t) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker) {
        if (configuration.isDebugEnabled()) {
            return true;
        }
        if (!listeners.isEmpty()) {
            return listenersLevel >= level.intLevel();
        }
        return logger.isEnabled(level, marker);
    }
}
