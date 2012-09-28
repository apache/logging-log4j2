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

import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Mechanism to record events that occur in the logging system.
 */
public final class StatusLogger extends AbstractLogger {

    /**
     * System property that can be configured with the number of entries in the queue. Once the limit
     * is reached older entries will be removed as new entries are added.
     */
    public static final String MAX_STATUS_ENTRIES = "log4j2.status.entries";

    private static final String NOT_AVAIL = "?";

    private static final int MAX_ENTRIES = Integer.getInteger(MAX_STATUS_ENTRIES, 200);

    // private static final String FQCN = AbstractLogger.class.getName();

    private static StatusLogger statusLogger = new StatusLogger();

    private Logger logger = null;

    private CopyOnWriteArrayList<StatusListener> listeners = new CopyOnWriteArrayList<StatusListener>();
    private ReentrantReadWriteLock listenersLock = new ReentrantReadWriteLock();

    private Queue<StatusData> messages = new BoundedQueue<StatusData>(MAX_ENTRIES);
    private ReentrantLock msgLock = new ReentrantLock();

    private StatusLogger() {
    }

    /**
     * Retrieve the StatusLogger.
     * @return The StatusLogger.
     */
    public static StatusLogger getLogger() {
        return statusLogger;
    }

    /**
     * Register a new listener.
     * @param listener The StatusListener to register.
     */
    public void registerListener(StatusListener listener) {
        listenersLock.writeLock().lock();
        try {
            listeners.add(listener);
        } finally {
            listenersLock.writeLock().unlock();
        }
    }

    /**
     * Remove a StatusListener.
     * @param listener The StatusListener to remove.
     */
    public void removeListener(StatusListener listener) {
        listenersLock.writeLock().lock();
        try {
            listeners.remove(listener);
        } finally {
            listenersLock.writeLock().unlock();
        }
    }

    /**
     * Returns a thread safe Iterator for the StatusListener.
     * @return An Iterator for the list of StatusListeners.
     */
    public Iterator<StatusListener> getListeners() {
        return listeners.iterator();
    }

    /**
     * Clears the list of status events and listeners.
     */
    public void reset() {
        listeners.clear();
        clear();
    }

    /**
     * Returns a List of all events as StatusData objects.
     * @return The list of StatusData objects.
     */
    public List<StatusData> getStatusData() {
        msgLock.lock();
        try {
            return new ArrayList<StatusData>(messages);
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


    /**
     * Add an event.
     * @param marker The Marker
     * @param fqcn   The fully qualified class name of the <b>caller</b>
     * @param level  The logging level
     * @param msg    The message associated with the event.
     * @param t      A Throwable or null.
     */
    @Override
    public void log(Marker marker, String fqcn, Level level, Message msg, Throwable t) {
        StackTraceElement element = null;
        if (fqcn != null) {
            element = getStackTraceElement(fqcn, Thread.currentThread().getStackTrace());
        }
        StatusData data = new StatusData(element, level, msg, t);
        msgLock.lock();
        try {
            messages.add(data);
        } finally {
            msgLock.unlock();
        }
        for (StatusListener listener : listeners) {
            listener.log(data);
        }
    }

    private StackTraceElement getStackTraceElement(String fqcn, StackTraceElement[] stackTrace) {
        if (fqcn == null) {
            return null;
        }
        boolean next = false;
        for (StackTraceElement element : stackTrace) {
            if (next) {
                return element;
            }
            String className = element.getClassName();
            if (fqcn.equals(className)) {
                next = true;
            } else if (NOT_AVAIL.equals(className)) {
                break;
            }
        }
        return null;
    }

    @Override
    protected boolean isEnabled(Level level, Marker marker, String data) {
        return isEnabled(level, marker);
    }

    @Override
    protected boolean isEnabled(Level level, Marker marker, String data, Throwable t) {
        return isEnabled(level, marker);
    }

    @Override
    protected boolean isEnabled(Level level, Marker marker, String data, Object... p1) {
        return isEnabled(level, marker);
    }

    @Override
    protected boolean isEnabled(Level level, Marker marker, Object data, Throwable t) {
        return isEnabled(level, marker);
    }

    @Override
    protected boolean isEnabled(Level level, Marker marker, Message data, Throwable t) {
        return isEnabled(level, marker);
    }

    protected boolean isEnabled(Level level, Marker marker) {
        if (logger == null) {
            return true;
        }
        switch (level) {
            case FATAL:
                return logger.isFatalEnabled(marker);
            case TRACE:
                return logger.isTraceEnabled(marker);
            case DEBUG:
                return logger.isDebugEnabled(marker);
            case INFO:
                return logger.isInfoEnabled(marker);
            case WARN:
                return logger.isWarnEnabled(marker);
            case ERROR:
                return logger.isErrorEnabled(marker);
        }
        return false;
    }

    /**
     * Queue for status events.
     * @param <E> Object type to be stored in the queue.
     */
    private class BoundedQueue<E> extends ConcurrentLinkedQueue<E> {

        private final int size;

        public BoundedQueue(int size) {
            this.size = size;
        }

        @Override
        public boolean add(E object) {
            while (messages.size() > size) {
                messages.poll();
            }
            return super.add(object);
        }
    }
}
