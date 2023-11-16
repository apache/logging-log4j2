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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.ParameterizedNoReferenceMessageFactory;
import org.apache.logging.log4j.simple.SimpleLogger;
import org.apache.logging.log4j.simple.SimpleLoggerContext;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.util.Constants;
import org.apache.logging.log4j.util.PropertiesUtil;

/**
 * Records events that occur in the logging system. By default, only error messages are logged to {@link System#err}.
 * Normally, the Log4j StatusLogger is configured via the root {@code <Configuration status="LEVEL"/>} node in a Log4j
 * configuration file. However, this can be overridden via a system property named
 * {@value #DEFAULT_STATUS_LISTENER_LEVEL} and will work with any Log4j provider.
 *
 * @see SimpleLogger
 * @see SimpleLoggerContext
 */
public final class StatusLogger extends AbstractLogger {

    /**
     * System property that can be configured with the number of entries in the queue. Once the limit is reached older
     * entries will be removed as new entries are added.
     */
    public static final String MAX_STATUS_ENTRIES = "log4j2.status.entries";

    /**
     * System property that can be configured with the {@link Level} name to use as the default level for
     * {@link StatusListener}s.
     */
    public static final String DEFAULT_STATUS_LISTENER_LEVEL = "log4j2.StatusLogger.level";

    /**
     * System property that can be configured with a date-time format string to use as the format for timestamps
     * in the status logger output. See {@link java.text.SimpleDateFormat} for supported formats.
     * @since 2.11.0
     */
    public static final String STATUS_DATE_FORMAT = "log4j2.StatusLogger.DateFormat";

    private static final long serialVersionUID = 2L;

    private static final String NOT_AVAIL = "?";

    static final PropertiesUtil PROPS = new PropertiesUtil("log4j2.StatusLogger.properties");

    private static final int MAX_ENTRIES = PROPS.getIntegerProperty(MAX_STATUS_ENTRIES, 200);

    private static final String DEFAULT_STATUS_LEVEL = PROPS.getStringProperty(DEFAULT_STATUS_LISTENER_LEVEL);

    static final boolean DEBUG_ENABLED =
            PropertiesUtil.getProperties().getBooleanProperty(Constants.LOG4J2_DEBUG, false, true);

    // LOG4J2-1176: normal parameterized message remembers param object, causing memory leaks.
    private static final StatusLogger STATUS_LOGGER = new StatusLogger(
            StatusLogger.class.getName(),
            ParameterizedNoReferenceMessageFactory.INSTANCE,
            SimpleLoggerFactory.getInstance());

    private final SimpleLogger logger;

    private final Collection<StatusListener> listeners = new CopyOnWriteArrayList<>();

    @SuppressWarnings("NonSerializableFieldInSerializableClass")
    // ReentrantReadWriteLock is Serializable
    private final ReadWriteLock listenersLock = new ReentrantReadWriteLock();

    private final Queue<StatusData> messages = new BoundedQueue<>(MAX_ENTRIES);

    @SuppressWarnings("NonSerializableFieldInSerializableClass")
    // ReentrantLock is Serializable
    private final Lock msgLock = new ReentrantLock();

    private int listenersLevel;

    /**
     * Constructs the singleton instance for the STATUS_LOGGER constant.
     * <p>
     * This is now the logger level is set:
     * </p>
     * <ol>
     * <li>If the property {@value Constants#LOG4J2_DEBUG} is {@code "true"}, then use {@link Level#TRACE}, otherwise,</li>
     * <li>Use {@link Level#ERROR}</li>
     * </ol>
     * <p>
     * This is now the listener level is set:
     * </p>
     * <ol>
     * <li>If the property {@value #DEFAULT_STATUS_LISTENER_LEVEL} is set, then use <em>it</em>, otherwise,</li>
     * <li>Use {@link Level#WARN}</li>
     * </ol>
     * <p>
     * See:
     * <ol>
     * <li>LOG4J2-1813 Provide shorter and more intuitive way to switch on Log4j internal debug logging. If system property
     * "log4j2.debug" is defined, print all status logging.</li>
     * <li>LOG4J2-3340 StatusLogger's log Level cannot be changed as advertised.</li>
     * </ol>
     * </p>
     *
     * @param name The logger name.
     * @param messageFactory The message factory.
     */
    private StatusLogger(
            final String name, final MessageFactory messageFactory, final SimpleLoggerFactory loggerFactory) {
        super(name, messageFactory);
        final Level loggerLevel = DEBUG_ENABLED ? Level.TRACE : Level.ERROR;
        this.logger = loggerFactory.createSimpleLogger("StatusLogger", loggerLevel, messageFactory, System.err);
        this.listenersLevel = Level.toLevel(DEFAULT_STATUS_LEVEL, Level.WARN).intLevel();
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
            int lowest = Level.toLevel(DEFAULT_STATUS_LEVEL, Level.WARN).intLevel();
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
        listenersLock.writeLock().lock();
        try {
            for (final StatusListener listener : listeners) {
                closeSilently(listener);
            }
        } finally {
            listeners.clear();
            listenersLock.writeLock().unlock();
            // note this should certainly come after the unlock to avoid unnecessary nested locking
            clear();
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
        msgLock.lock();
        try {
            return new ArrayList<>(messages);
        } finally {
            msgLock.unlock();
        }
    }

    /**
     * Clears the list of status events.
     */
    public void clear() {
        msgLock.lock();
        try {
            messages.clear();
        } finally {
            msgLock.unlock();
        }
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
        msgLock.lock();
        try {
            messages.add(data);
        } finally {
            msgLock.unlock();
        }
        // LOG4J2-1813 if system property "log4j2.debug" is defined, all status logging is enabled
        if (DEBUG_ENABLED || (listeners.size() <= 0)) {
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
        if (DEBUG_ENABLED) {
            return true;
        }
        if (listeners.size() > 0) {
            return listenersLevel >= level.intLevel();
        }
        return logger.isEnabled(level, marker);
    }

    /**
     * Queues for status events.
     *
     * @param <E> Object type to be stored in the queue.
     */
    private class BoundedQueue<E> extends ConcurrentLinkedQueue<E> {

        private static final long serialVersionUID = -3945953719763255337L;

        private final int size;

        BoundedQueue(final int size) {
            this.size = size;
        }

        @Override
        public boolean add(final E object) {
            super.add(object);
            while (messages.size() > size) {
                messages.poll();
            }
            return size > 0;
        }
    }
}
