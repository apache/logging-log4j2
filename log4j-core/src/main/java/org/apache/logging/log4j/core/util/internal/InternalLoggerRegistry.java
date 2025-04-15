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
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.status.StatusLogger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A registry of {@link Logger}s namespaced by name and message factory.
 * This class is internally used by {@link LoggerContext}.
 * <p>
 * We don't use {@linkplain org.apache.logging.log4j.spi.LoggerRegistry the registry from Log4j API} to keep Log4j Core independent from the version of Log4j API at runtime.
 * This also allows Log4j Core to evolve independently from Log4j API.
 * </p>
 *
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
            final Map<String, WeakReference<Logger>> loggerRefByName =
                    loggerRefByNameByMessageFactory.get(messageFactory);
            if (loggerRefByName != null) {
                final WeakReference<Logger> loggerRef = loggerRefByName.get(name);
                if (loggerRef != null) {
                    return loggerRef.get();
                }
            }
            return null;
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

        // Intentionally moving the logger creation outside the write lock, because:
        //
        // - Logger instantiation is expensive (causes contention on the write-lock)
        //
        // - User code might have circular code paths, though through different threads.
        //   Consider `T1[ILR:computeIfAbsent] -> ... -> T1[Logger::new] -> ... -> T2[ILR::computeIfAbsent]`.
        //   Hence, having logger instantiation while holding a write lock might cause deadlocks:
        //   https://github.com/apache/logging-log4j2/issues/3252
        //   https://github.com/apache/logging-log4j2/issues/3399
        //
        // - Creating loggers without a lock, allows multiple threads to create loggers in parallel, which also improves
        // performance.
        //
        // Since all loggers with the same parameters are equivalent, we can safely return the logger from the
        // thread that finishes first.
        Logger newLogger = loggerSupplier.apply(name, messageFactory);

        // Report name and message factory mismatch if there are any
        final String loggerName = newLogger.getName();
        final MessageFactory loggerMessageFactory = newLogger.getMessageFactory();
        if (!loggerName.equals(name) || !loggerMessageFactory.equals(messageFactory)) {
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
        }

        // Write lock slow path: Insert the logger
        writeLock.lock();
        try {
            Map<String, WeakReference<Logger>> loggerRefByName = loggerRefByNameByMessageFactory.get(messageFactory);
            // noinspection Java8MapApi (avoid the allocation of lambda passed to `Map::computeIfAbsent`)
            if (loggerRefByName == null) {
                loggerRefByNameByMessageFactory.put(messageFactory, loggerRefByName = new HashMap<>());
            }
            final WeakReference<Logger> loggerRef = loggerRefByName.get(name);
            if (loggerRef == null || (logger = loggerRef.get()) == null) {
                loggerRefByName.put(name, new WeakReference<>(logger = newLogger));
            }
            return logger;
        } finally {
            writeLock.unlock();
        }
    }
}
