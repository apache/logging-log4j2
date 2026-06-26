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

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.ParameterizedMessageFactory;
import org.apache.logging.log4j.spi.LoggerRegistry;
import org.apache.logging.log4j.status.StatusLogger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A registry of {@link Logger}s namespaced by name and message factory using {@link WeakReference}s.
 * <p>
 * This class extends {@link LoggerRegistry} to provide garbage-free logger tracking
 * with minimal lock contention. Loggers are created outside the write lock to prevent
 * deadlocks and improve concurrency.
 * </p>
 * @since 3.0.0
 */
@NullMarked
public class InternalLoggerRegistry extends LoggerRegistry<Logger> {

    private final Map<MessageFactory, Map<String, WeakReference<Logger>>> loggerRefByNameByMessageFactory =
            new WeakHashMap<>();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    private final ReferenceQueue<Logger> staleLoggerRefs = new ReferenceQueue<>();

    public InternalLoggerRegistry() {}

    private void expungeStaleEntries() {
        final Reference<? extends Logger> loggerRef = staleLoggerRefs.poll();
        if (loggerRef != null) {
            writeLock.lock();
            try {
                while (staleLoggerRefs.poll() != null) {
                    // Clear ref queue
                }
                final Iterator<Map.Entry<MessageFactory, Map<String, WeakReference<Logger>>>> it =
                        loggerRefByNameByMessageFactory.entrySet().iterator();
                while (it.hasNext()) {
                    final Map.Entry<MessageFactory, Map<String, WeakReference<Logger>>> entry = it.next();
                    final Map<String, WeakReference<Logger>> loggerRefByName = entry.getValue();
                    loggerRefByName.values().removeIf(weakRef -> weakRef.get() == null);
                    if (loggerRefByName.isEmpty()) {
                        it.remove();
                    }
                }
            } finally {
                writeLock.unlock();
            }
        }
    }

    @Override
    public @Nullable Logger getLogger(final String name, @Nullable final MessageFactory messageFactory) {
        requireNonNull(name, "name");
        final MessageFactory mf = messageFactory != null ? messageFactory : ParameterizedMessageFactory.INSTANCE;
        expungeStaleEntries();
        readLock.lock();
        try {
            final Map<String, WeakReference<Logger>> loggerRefByName = loggerRefByNameByMessageFactory.get(mf);
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

    @Override
    public Collection<Logger> getLoggers() {
        expungeStaleEntries();
        readLock.lock();
        try {
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

    @Override
    public Collection<Logger> getLoggers(final Collection<Logger> destination) {
        requireNonNull(destination, "destination");
        expungeStaleEntries();
        readLock.lock();
        try {
            for (final Map<String, WeakReference<Logger>> loggerRefByName : loggerRefByNameByMessageFactory.values()) {
                for (final WeakReference<Logger> loggerRef : loggerRefByName.values()) {
                    @Nullable Logger logger = loggerRef.get();
                    if (logger != null) {
                        destination.add(logger);
                    }
                }
            }
        } finally {
            readLock.unlock();
            return destination;
        }
    }

    @Override
    public boolean hasLogger(final String name, @Nullable final MessageFactory messageFactory) {
        requireNonNull(name, "name");
        return getLogger(name, messageFactory) != null;
    }

    @Override
    public boolean hasLogger(final String name, final Class<? extends MessageFactory> messageFactoryClass) {
        requireNonNull(name, "name");
        requireNonNull(messageFactoryClass, "messageFactoryClass");
        expungeStaleEntries();
        readLock.lock();
        try {
            return loggerRefByNameByMessageFactory.entrySet().stream()
                    .filter(entry -> messageFactoryClass.equals(entry.getKey().getClass()))
                    .anyMatch(entry -> entry.getValue().containsKey(name));
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void putIfAbsent(final String name, final MessageFactory messageFactory, final Logger logger) {
        requireNonNull(name, "name");
        requireNonNull(messageFactory, "messageFactory");
        requireNonNull(logger, "logger");
        writeLock.lock();
        try {
            Map<String, WeakReference<Logger>> loggerRefByName = loggerRefByNameByMessageFactory.get(messageFactory);
            if (loggerRefByName == null) {
                loggerRefByNameByMessageFactory.put(messageFactory, loggerRefByName = new HashMap<>());
            }
            final WeakReference<Logger> loggerRef = loggerRefByName.get(name);
            if (loggerRef == null || loggerRef.get() == null) {
                loggerRefByName.put(name, new WeakReference<>(logger, staleLoggerRefs));
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Returns the logger associated with the given name and message factory, creating it if necessary
     * using the provided supplier. The logger is created outside the write lock to avoid deadlocks
     * and reduce contention.
     *
     * @param name a logger name
     * @param messageFactory a message factory
     * @param loggerSupplier a function to create the logger
     * @return the existing or newly created logger
     */
    public Logger computeIfAbsent(
            final String name,
            final MessageFactory messageFactory,
            final BiFunction<String, MessageFactory, Logger> loggerSupplier) {
        requireNonNull(name, "name");
        requireNonNull(messageFactory, "messageFactory");
        requireNonNull(loggerSupplier, "loggerSupplier");

        @Nullable Logger logger = getLogger(name, messageFactory);
        if (logger != null) {
            return logger;
        }

        Logger newLogger = loggerSupplier.apply(name, messageFactory);

        final String loggerName = newLogger.getName();
        final MessageFactory loggerMessageFactory = newLogger.getMessageFactory();
        if (!loggerName.equals(name) || !loggerMessageFactory.equals(messageFactory)) {
            StatusLogger.getLogger()
                    .error(
                            "Newly registered logger with name `{}` and message factory `{}`, "
                                    + "is requested to be associated with a different name `{}` or message factory `{}`.\n"
                                    + "Effectively the message factory of the logger will be used and the other one will be ignored.\n"
                                    + "This generally hints a problem at the logger context implementation.\n"
                                    + "Please report this using the Log4j project issue tracker.",
                            loggerName,
                            loggerMessageFactory,
                            name,
                            messageFactory);
        }

        writeLock.lock();
        try {
            Map<String, WeakReference<Logger>> loggerRefByName = loggerRefByNameByMessageFactory.get(messageFactory);
            if (loggerRefByName == null) {
                loggerRefByNameByMessageFactory.put(messageFactory, loggerRefByName = new HashMap<>());
            }
            final WeakReference<Logger> loggerRef = loggerRefByName.get(name);
            if (loggerRef == null || (logger = loggerRef.get()) == null) {
                loggerRefByName.put(name, new WeakReference<>(logger = newLogger, staleLoggerRefs));
            }
            return logger;
        } finally {
            writeLock.unlock();
        }
    }
}
