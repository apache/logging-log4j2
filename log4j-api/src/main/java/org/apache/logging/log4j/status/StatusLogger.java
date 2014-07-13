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
package org.apache.logging.log4j.status;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.simple.SimpleLogger;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.Strings;

/**
 * Mechanism to record events that occur in the logging system.
 */
public final class StatusLogger extends AbstractLogger {

    private static final long serialVersionUID = 2L;

    /**
     * System property that can be configured with the number of entries in the queue. Once the limit
     * is reached older entries will be removed as new entries are added.
     */
    public static final String MAX_STATUS_ENTRIES = "log4j2.status.entries";

    private static final String NOT_AVAIL = "?";

    private static final PropertiesUtil PROPS = new PropertiesUtil("log4j2.StatusLogger.properties");

    private static final int MAX_ENTRIES = PROPS.getIntegerProperty(MAX_STATUS_ENTRIES, 200);

    private static final String DEFAULT_STATUS_LEVEL = PROPS.getStringProperty("log4j2.StatusLogger.level");

    private static final StatusLogger STATUS_LOGGER = new StatusLogger();

    private final Lock lock = new ReentrantLock();
    private final LoggerFilters sysOut;
    private final LoggerFilters sysErr;
    private final Map<File, LoggerFilters> files = new HashMap<File, LoggerFilters>();
    private final Collection<ListenerLevel> listeners = new CopyOnWriteArrayList<ListenerLevel>();
    private final Queue<StatusData> messages = new BoundedQueue<StatusData>(MAX_ENTRIES);

    private Level defaultLogLevel = Level.toLevel(DEFAULT_STATUS_LEVEL, Level.WARN);
    private Level registeredLogLevel = null;

    private StatusLogger() {
        sysErr = new LoggerFilters("System.err", defaultLogLevel, System.err);
        sysOut = new LoggerFilters("System.out", Level.OFF, System.out);
    }

    /**
     * Retrieve the StatusLogger.
     * @return The StatusLogger.
     */
    public static StatusLogger getLogger() {
        return STATUS_LOGGER;
    }

    public void setLevel(final Level level) {
        lock.lock();
        try {
            defaultLogLevel = level;
            if (!hasRegistrations()) {
                sysErr.logger.setLevel(level);
            }
        } finally {
            lock.unlock();
        }
    }
    public void registerSystemOutFilter(final StatusFilter filter, final Level level) {
        registerFilter(sysOut, filter, level);
    }

    public boolean removeSystemOutFilter(final StatusFilter filter) {
        return removeFilter(sysOut, filter);
    }

    public void registerSystemErrFilter(final StatusFilter filter, final Level level) {
        registerFilter(sysErr, filter, level);
    }

    public boolean removeSystemErrFilter(final StatusFilter filter) {
        return removeFilter(sysErr, filter);
    }

    public void registerFileFilter(final File file, final StatusFilter filter, final Level level) throws FileNotFoundException {
        lock.lock();
        try {
            LoggerFilters filtered = files.get(file);
            if (filtered == null) {
                filtered = new LoggerFilters(file.toString(), level, new PrintStream(file));
                files.put(file, filtered);
            }
            registerFilter(filtered, filter, level);
        } finally {
            lock.unlock();
        }
    }

    public boolean removeFileFilter(final File file, final StatusFilter filter) {
        lock.lock();
        try {
            LoggerFilters filtered = files.get(file);
            if (filtered != null && filtered.remove(filter)) {
                if (filtered.isEmpty()) {
                    files.remove(file);
                    closeSilently(filtered);
                }
                updateRegisteredLevel();
                return true;
            }
        } finally {
            lock.unlock();
        }
        return false;
    }

    private void registerFilter(final LoggerFilters filtered, final StatusFilter filter, final Level level) {
        lock.lock();
        try {
            filtered.add(filter, level);
            updateRegisteredLevel(level);
        } finally {
            lock.unlock();
        }
    }

    private boolean removeFilter(final LoggerFilters filtered, final StatusFilter filter) {
        lock.lock();
        try {
            if (filtered.remove(filter)) {
                updateRegisteredLevel();
                return true;
            }
        } finally {
            lock.unlock();
        }
        return false;
    }

    /**
     * Register a new listener.
     * @param listener The StatusListener to register.
     */
    public void registerListener(final StatusListener listener, final Level level) {
        lock.lock();
        try {
            listeners.add(new ListenerLevel(listener, level));
            updateRegisteredLevel(level);
        } finally {
            lock.unlock();
        }
    }
    /**
     * Remove a StatusListener.
     * 
     * @param listener The StatusListener to remove.
     */
    public boolean removeListener(final StatusListener listener) {
        lock.lock();
        try {
            for (final Iterator<ListenerLevel> i = listeners.iterator(); i.hasNext(); ) {
                final ListenerLevel listenerLevel = i.next();
                if (listenerLevel.listener == listener) {
                    listeners.remove(listenerLevel);
                    updateRegisteredLevel();
                    return true;
                }
            }
        } finally {
            lock.unlock();
        }
        return false;
    }

    private boolean hasRegistrations() {
        return registeredLogLevel != null;
    }

    private void updateRegisteredLevel(Level level) {
        registeredLogLevel = leastSpecificOf(level, registeredLogLevel);
    }

    private void updateRegisteredLevel() {
        Level level = null;
        for (ListenerLevel listener : listeners) {
            level = leastSpecificOf(level, listener.level);
        }
        level = leastSpecificOf(level, sysOut.leastSpecificLevel());
        level = leastSpecificOf(level, sysErr.leastSpecificLevel());
        for (LoggerFilters file : files.values()) {
            level = leastSpecificOf(level, file.leastSpecificLevel());
        }
        registeredLogLevel = level;
    }

    private static Level leastSpecificOf(Level first, Level second) {
        if (first == null) {
            return second;
        } else if (second == null || first.isLessSpecificThan(second)) {
            return first;
        }
        return second;
    }

    /**
     * Clears the list of status events and listeners.
     */
    public void reset() {
        lock.lock();
        try {
            listeners.clear();
            sysOut.filters.clear();
            sysErr.filters.clear();
            for (LoggerFilters file : files.values()) {
                closeSilently(file);
            }
            files.clear();
        } finally {
            lock.unlock();
            // note this should certainly come after the unlock to avoid unnecessary nested locking
            clear();
        }
    }

    private static void closeSilently(final Closeable resource) {
        try {
            resource.close();
        } catch (final IOException ignored) {
        }
    }

    /**
     * Returns a List of all events as StatusData objects.
     * @return The list of StatusData objects.
     */
    public List<StatusData> getStatusData() {
        lock.lock();
        try {
            return new ArrayList<StatusData>(messages);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Clears the list of status events.
     */
    public void clear() {
        lock.lock();
        try {
            messages.clear();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Level getLevel() {
        return registeredLogLevel;
    }

    /**
     * Add an event.
     * @param marker The Marker
     * @param fqcn   The fully qualified class name of the <b>caller</b>
     * @param level  The logging level
     * @param msg    The message associated with the event.
     * @param t      A Throwable or null.
     */
    @Override
    public void logMessage(final String fqcn, final Level level, final Marker marker, final Message msg, final Throwable t) {
        StackTraceElement element = null;
        if (fqcn != null) {
            element = getStackTraceElement(fqcn, Thread.currentThread().getStackTrace());
        }
        final StatusData data = new StatusData(element, level, marker, msg, t);
        lock.lock();
        try {
            messages.add(data);
        } finally {
            lock.unlock();
        }
        if (hasRegistrations()) {
            logMessage(sysOut, fqcn, data);
            logMessage(sysErr, fqcn, data);
            for (LoggerFilters file : files.values()) {
                logMessage(file, fqcn, data);
            }
            for (final ListenerLevel listener : listeners) {
                if (listener.level.isLessSpecificThan(level)) {
                    listener.listener.log(data);
                }
            }
        } else {
            sysErr.logger.logMessage(fqcn, level, marker, msg, t);
        }
    }

    private void logMessage(final LoggerFilters filtered, final String fqcn, final StatusData data) {
        if (filtered.logger.isEnabled(data.getLevel())) {
            for (FilterLevel filter : filtered.filters) {
                if (filter.filter.isEnabled(data)) {
                    filtered.logger.logMessage(fqcn, data.getLevel(), data.getMarker(), data.getMessage(), data.getThrowable());
                    break;
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
    public boolean isEnabled(final Level level, final Marker marker, final Object message, final Throwable t) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final Message message, final Throwable t) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker) {
        if (hasRegistrations()) {
            return level.isMoreSpecificThan(registeredLogLevel);
        }
        return sysErr.logger.isEnabled(level, marker);
    }

    /**
     * Queue for status events.
     * @param <E> Object type to be stored in the queue.
     */
    private class BoundedQueue<E> extends ConcurrentLinkedQueue<E> {

        private static final long serialVersionUID = -3945953719763255337L;

        private final int size;

        public BoundedQueue(final int size) {
            this.size = size;
        }

        @Override
        public boolean add(final E object) {
            while (size() > size) {
                poll();
            }
            return super.add(object);
        }
    }

    private static class LoggerFilters implements Closeable {
        final SimpleLogger logger;
        final Collection<FilterLevel> filters = new CopyOnWriteArrayList<FilterLevel>();

        LoggerFilters(final String name, final Level level, final PrintStream out) {
            logger = new SimpleLogger(name, level, false, true, false, false, Strings.EMPTY, null, PROPS, out);
        }

        boolean isEmpty() {
            return filters.isEmpty();
        }

        public void close() throws IOException {
            filters.clear();
            logger.close();
        }

        void add(final StatusFilter filter, final Level level) {
            filters.add(new FilterLevel(filter, level));
            if (level.isLessSpecificThan(logger.getLevel())) {
                logger.setLevel(level);
            }
        }

        boolean remove(final StatusFilter filter) {
            for (final Iterator<FilterLevel> i = filters.iterator(); i.hasNext();) {
                final FilterLevel existing = i.next();
                if (existing.filter == filter) {
                    filters.remove(existing);
                    logger.setLevel(leastSpecificLevel());
                    return true;
                }
            }
            return false;
        }

        Level leastSpecificLevel() {
            Level level = Level.OFF;
            for (FilterLevel existing : filters) {
                if (existing.level.isLessSpecificThan(level)) {
                    level = existing.level;
                }
            }
            return level;
        }
    }

    private static class FilterLevel {
        final StatusFilter filter;
        final Level level;

        FilterLevel(final StatusFilter filter, final Level level) {
            this.filter = filter;
            this.level = level;
        }
    }

    private static class ListenerLevel {
        final StatusListener listener;
        final Level level;

        ListenerLevel(final StatusListener listener, final Level level) {
            this.listener = listener;
            this.level = level;
        }
    }
}
