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
package org.apache.logging.log4j.spi;

import static java.util.Objects.requireNonNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.ParameterizedMessageFactory;
import org.apache.logging.log4j.status.StatusLogger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Convenience class to be used as an {@link ExtendedLogger} registry by {@code LoggerContext} implementations.
 */
@NullMarked
public class LoggerRegistry<T extends ExtendedLogger> {

    private final Map<String, Map<MessageFactory, WeakReference<T>>> loggerRefByMessageFactoryByName = new HashMap<>();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final Lock readLock = lock.readLock();

    private final Lock writeLock = lock.writeLock();

    /**
     * Data structure contract for the internal storage of admitted loggers.
     *
     * @param <T> subtype of {@code ExtendedLogger}
     * @deprecated As of version {@code 2.25.0}, planned to be removed!
     */
    @Deprecated
    public interface MapFactory<T extends ExtendedLogger> {

        Map<String, T> createInnerMap();

        Map<String, Map<String, T>> createOuterMap();

        void putIfAbsent(Map<String, T> innerMap, String name, T logger);
    }

    /**
     * {@link MapFactory} implementation using {@link ConcurrentHashMap}.
     *
     * @param <T> subtype of {@code ExtendedLogger}
     * @deprecated As of version {@code 2.25.0}, planned to be removed!
     */
    @Deprecated
    public static class ConcurrentMapFactory<T extends ExtendedLogger> implements MapFactory<T> {

        @Override
        public Map<String, T> createInnerMap() {
            return new ConcurrentHashMap<>();
        }

        @Override
        public Map<String, Map<String, T>> createOuterMap() {
            return new ConcurrentHashMap<>();
        }

        @Override
        public void putIfAbsent(final Map<String, T> innerMap, final String name, final T logger) {
            innerMap.putIfAbsent(name, logger);
        }
    }

    /**
     * {@link MapFactory} implementation using {@link WeakHashMap}.
     *
     * @param <T> subtype of {@code ExtendedLogger}
     * @deprecated As of version {@code 2.25.0}, planned to be removed!
     */
    @Deprecated
    public static class WeakMapFactory<T extends ExtendedLogger> implements MapFactory<T> {

        @Override
        public Map<String, T> createInnerMap() {
            return new WeakHashMap<>();
        }

        @Override
        public Map<String, Map<String, T>> createOuterMap() {
            return new WeakHashMap<>();
        }

        @Override
        public void putIfAbsent(final Map<String, T> innerMap, final String name, final T logger) {
            innerMap.put(name, logger);
        }
    }

    public LoggerRegistry() {}

    /**
     * Constructs an instance <b>ignoring</b> the given the map factory.
     *
     * @param mapFactory a map factory
     * @deprecated As of version {@code 2.25.0}, planned to be removed!
     */
    @Deprecated
    public LoggerRegistry(@Nullable final MapFactory<T> mapFactory) {
        this();
    }

    /**
     * Returns the logger associated with the given name.
     * <p>
     * There can be made no assumptions on the message factory of the returned logger.
     * Callers are strongly advised to switch to {@link #getLogger(String, MessageFactory)} and <b>provide a message factory parameter!</b>
     * </p>
     *
     * @param name a logger name
     * @return the logger associated with the name
     * @deprecated As of version {@code 2.25.0}, planned to be removed!
     * Use {@link #getLogger(String, MessageFactory)} instead.
     */
    @Deprecated
    public T getLogger(final String name) {
        requireNonNull(name, "name");
        return getLogger(name, null);
    }

    /**
     * Returns the logger associated with the given name and message factory.
     * <p>
     * In the absence of a message factory, there can be made no assumptions on the message factory of the returned logger.
     * This lenient behaviour is only kept for backward compatibility.
     * Callers are strongly advised to <b>provide a message factory parameter to the method!</b>
     * </p>
     *
     * @param name a logger name
     * @param messageFactory a message factory
     * @return the logger associated with the given name and message factory
     */
    public T getLogger(final String name, @Nullable final MessageFactory messageFactory) {
        requireNonNull(name, "name");
        readLock.lock();
        try {
            final Map<MessageFactory, WeakReference<T>> loggerRefByMessageFactory =
                    loggerRefByMessageFactoryByName.get(name);
            if (loggerRefByMessageFactory == null) {
                return null;
            }
            final MessageFactory effectiveMessageFactory =
                    messageFactory != null ? messageFactory : ParameterizedMessageFactory.INSTANCE;
            final WeakReference<T> loggerRef = loggerRefByMessageFactory.get(effectiveMessageFactory);
            if (loggerRef == null) {
                return null;
            }
            return loggerRef.get();
        } finally {
            readLock.unlock();
        }
    }

    public Collection<T> getLoggers() {
        return getLoggers(new ArrayList<T>());
    }

    public Collection<T> getLoggers(final Collection<T> destination) {
        requireNonNull(destination, "destination");
        readLock.lock();
        try {
            loggerRefByMessageFactoryByName.values().stream()
                    .flatMap(loggerRefByMessageFactory ->
                            loggerRefByMessageFactory.values().stream().map(WeakReference::get))
                    .filter(Objects::nonNull)
                    .forEach(destination::add);
        } finally {
            readLock.unlock();
        }
        return destination;
    }

    /**
     * Checks if a logger associated with the given name exists.
     * <p>
     * There can be made no assumptions on the message factory of the found logger.
     * Callers are strongly advised to switch to {@link #hasLogger(String, MessageFactory)} and <b>provide a message factory parameter!</b>
     * </p>
     *
     * @param name a logger name
     * @return {@code true}, if the logger exists; {@code false} otherwise.
     * @deprecated As of version {@code 2.25.0}, planned to be removed!
     * Use {@link #hasLogger(String, MessageFactory)} instead.
     */
    @Deprecated
    public boolean hasLogger(final String name) {
        requireNonNull(name, "name");
        final T logger = getLogger(name);
        return logger != null;
    }

    /**
     * Checks if a logger associated with the given name and message factory exists.
     * <p>
     * In the absence of a message factory, there can be made no assumptions on the message factory of the found logger.
     * This lenient behaviour is only kept for backward compatibility.
     * Callers are strongly advised to <b>provide a message factory parameter to the method!</b>
     * </p>
     *
     * @param name a logger name
     * @param messageFactory a message factory
     * @return {@code true}, if the logger exists; {@code false} otherwise.
     * @since 2.5
     */
    public boolean hasLogger(final String name, @Nullable final MessageFactory messageFactory) {
        requireNonNull(name, "name");
        final T logger = getLogger(name, messageFactory);
        return logger != null;
    }

    /**
     * Checks if a logger associated with the given name and message factory type exists.
     *
     * @param name a logger name
     * @param messageFactoryClass a message factory class
     * @return {@code true}, if the logger exists; {@code false} otherwise.
     * @since 2.5
     */
    public boolean hasLogger(final String name, final Class<? extends MessageFactory> messageFactoryClass) {
        requireNonNull(name, "name");
        requireNonNull(messageFactoryClass, "messageFactoryClass");
        readLock.lock();
        try {
            return loggerRefByMessageFactoryByName.getOrDefault(name, Collections.emptyMap()).keySet().stream()
                    .anyMatch(messageFactory -> messageFactoryClass.equals(messageFactory.getClass()));
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Registers the provided logger.
     * <b>Logger name and message factory parameters are ignored</b>, those will be obtained from the logger instead.
     *
     * @param name ignored – kept for backward compatibility
     * @param messageFactory ignored – kept for backward compatibility
     * @param logger a logger instance
     * @deprecated As of version {@code 2.25.0}, planned to be removed!
     * Use {@link #computeIfAbsent(String, MessageFactory, BiFunction)} instead.
     */
    @Deprecated
    public void putIfAbsent(
            @Nullable final String name, @Nullable final MessageFactory messageFactory, final T logger) {

        // Check arguments
        requireNonNull(logger, "logger");

        // Insert the logger
        writeLock.lock();
        try {
            final String loggerName = logger.getName();
            final Map<MessageFactory, WeakReference<T>> loggerRefByMessageFactory =
                    loggerRefByMessageFactoryByName.computeIfAbsent(
                            loggerName, this::createLoggerRefByMessageFactoryMap);
            final MessageFactory loggerMessageFactory = logger.getMessageFactory();
            final WeakReference<T> loggerRef = loggerRefByMessageFactory.get(loggerMessageFactory);
            if (loggerRef == null || loggerRef.get() == null) {
                loggerRefByMessageFactory.put(loggerMessageFactory, new WeakReference<>(logger));
            }
        } finally {
            writeLock.unlock();
        }
    }

    public T computeIfAbsent(
            final String name,
            final MessageFactory messageFactory,
            final BiFunction<String, MessageFactory, T> loggerSupplier) {

        // Check arguments
        requireNonNull(name, "name");
        requireNonNull(messageFactory, "messageFactory");
        requireNonNull(loggerSupplier, "loggerSupplier");

        // Read lock fast path: See if logger already exists
        T logger = getLogger(name, messageFactory);
        if (logger != null) {
            return logger;
        }

        // Write lock slow path: Insert the logger
        writeLock.lock();
        try {

            // See if the logger is created by another thread in the meantime
            final Map<MessageFactory, WeakReference<T>> loggerRefByMessageFactory =
                    loggerRefByMessageFactoryByName.computeIfAbsent(name, this::createLoggerRefByMessageFactoryMap);
            final WeakReference<T> loggerRef;
            if ((loggerRef = loggerRefByMessageFactory.get(messageFactory)) != null
                    && (logger = loggerRef.get()) != null) {
                return logger;
            }

            // Create the logger
            logger = loggerSupplier.apply(name, messageFactory);

            // Report message factory mismatches, if there is any
            final MessageFactory loggerMessageFactory = logger.getMessageFactory();
            if (!loggerMessageFactory.equals(messageFactory)) {
                StatusLogger.getLogger()
                        .error(
                                "Newly registered logger with name `{}` and message factory `{}`, is requested to be associated with a different message factory: `{}`.\n"
                                        + "Effectively the message factory of the logger will be used and the other one will be ignored.\n"
                                        + "This generally hints a problem at the logger context implementation.\n"
                                        + "Please report this using the Log4j project issue tracker.",
                                name,
                                loggerMessageFactory,
                                messageFactory);
            }

            // Insert the logger
            loggerRefByMessageFactory.put(loggerMessageFactory, new WeakReference<>(logger));
            return logger;
        } finally {
            writeLock.unlock();
        }
    }

    private Map<MessageFactory, WeakReference<T>> createLoggerRefByMessageFactoryMap(final String ignored) {
        return new WeakHashMap<>();
    }
}
