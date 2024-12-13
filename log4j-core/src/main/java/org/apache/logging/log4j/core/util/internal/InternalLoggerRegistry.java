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
package org.apache.logging.log4j.core.util.internal;

import static java.util.Objects.requireNonNull;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.status.StatusLogger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Convenience class used by {@link org.apache.logging.log4j.core.LoggerContext}
 * <p>
 *   We don't use {@link org.apache.logging.log4j.spi.LoggerRegistry} from the Log4j API to keep Log4j Core independent
 *   from the version of the Log4j API at runtime.
 * </p>
 * @since 2.25.0
 */
@NullMarked
public final class InternalLoggerRegistry {

    private final Map<MessageFactory, Map<String, WeakReference<Logger>>> loggerRefByNameByMessageFactory =
            new WeakHashMap<>();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final Lock readLock = lock.readLock();

    private final Lock writeLock = lock.writeLock();

    public InternalLoggerRegistry() {}

    /**
     * Returns the logger associated with the given name and message factory.
     *
     * @param name a logger name
     * @param messageFactory a message factory
     * @return the logger associated with the given name and message factory
     */
    public @Nullable Logger getLogger(final String name, final MessageFactory messageFactory) {
        requireNonNull(name, "name");
        requireNonNull(messageFactory, "messageFactory");
        readLock.lock();
        try {
            return Optional.of(loggerRefByNameByMessageFactory)
                    .map(loggerRefByNameByMessageFactory -> loggerRefByNameByMessageFactory.get(messageFactory))
                    .map(loggerRefByName -> loggerRefByName.get(name))
                    .map(WeakReference::get)
                    .orElse(null);
        } finally {
            readLock.unlock();
        }
    }

    public Collection<Logger> getLoggers() {
        readLock.lock();
        try {
            // Return a new collection to allow concurrent iteration over the loggers
            //
            // https://github.com/apache/logging-log4j2/issues/3234
            return loggerRefByNameByMessageFactory.values().stream()
                    .flatMap(loggerRefByName -> loggerRefByName.values().stream())
                    .flatMap(loggerRef -> {
                        @Nullable Logger logger = loggerRef.get();
                        return logger != null ? Stream.of(logger) : Stream.empty();
                    })
                    .collect(Collectors.toList());
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Checks if a logger associated with the given name and message factory exists.
     *
     * @param name a logger name
     * @param messageFactory a message factory
     * @return {@code true}, if the logger exists; {@code false} otherwise.
     */
    public boolean hasLogger(final String name, final MessageFactory messageFactory) {
        requireNonNull(name, "name");
        requireNonNull(messageFactory, "messageFactory");
        return getLogger(name, messageFactory) != null;
    }

    /**
     * Checks if a logger associated with the given name and message factory type exists.
     *
     * @param name a logger name
     * @param messageFactoryClass a message factory class
     * @return {@code true}, if the logger exists; {@code false} otherwise.
     */
    public boolean hasLogger(final String name, final Class<? extends MessageFactory> messageFactoryClass) {
        requireNonNull(name, "name");
        requireNonNull(messageFactoryClass, "messageFactoryClass");
        readLock.lock();
        try {
            return loggerRefByNameByMessageFactory.entrySet().stream()
                    .filter(entry -> messageFactoryClass.equals(entry.getKey().getClass()))
                    .anyMatch(entry -> entry.getValue().containsKey(name));
        } finally {
            readLock.unlock();
        }
    }

    public Logger computeIfAbsent(
            final String name,
            final MessageFactory messageFactory,
            final BiFunction<String, MessageFactory, Logger> loggerSupplier) {

        // Check arguments
        requireNonNull(name, "name");
        requireNonNull(messageFactory, "messageFactory");
        requireNonNull(loggerSupplier, "loggerSupplier");

        // Read lock fast path: See if logger already exists
        @Nullable Logger logger = getLogger(name, messageFactory);
        if (logger != null) {
            return logger;
        }

        // Write lock slow path: Insert the logger
        writeLock.lock();
        try {

            // See if the logger is created by another thread in the meantime
            final Map<String, WeakReference<Logger>> loggerRefByName =
                    loggerRefByNameByMessageFactory.computeIfAbsent(messageFactory, ignored -> new HashMap<>());
            WeakReference<Logger> loggerRef = loggerRefByName.get(name);
            if (loggerRef != null && (logger = loggerRef.get()) != null) {
                return logger;
            }

            // Create the logger
            logger = loggerSupplier.apply(name, messageFactory);

            // Report name and message factory mismatch if there are any
            final String loggerName = logger.getName();
            final MessageFactory loggerMessageFactory = logger.getMessageFactory();
            if (!loggerMessageFactory.equals(messageFactory)) {
                StatusLogger.getLogger()
                        .error(
                                "Newly registered logger with name `{}` and message factory `{}`, is requested to be associated with a different name `{}` or message factory `{}`.\n"
                                        + "Effectively the message factory of the logger will be used and the other one will be ignored.\n"
                                        + "This generally hints a problem at the logger context implementation.\n"
                                        + "Please report this using the Log4j project issue tracker.",
                                loggerName,
                                loggerMessageFactory,
                                name,
                                messageFactory);
                // Register logger under alternative keys
                loggerRefByNameByMessageFactory
                        .computeIfAbsent(loggerMessageFactory, ignored -> new HashMap<>())
                        .putIfAbsent(loggerName, new WeakReference<>(logger));
            }

            // Insert the logger
            loggerRefByName.put(name, new WeakReference<>(logger));
            return logger;
        } finally {
            writeLock.unlock();
        }
    }
}
