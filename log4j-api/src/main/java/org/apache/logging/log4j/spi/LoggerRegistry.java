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
package org.apache.logging.log4j.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.message.MessageFactory;

/**
 * Convenience class to be used by {@code LoggerContext} implementations.
 */
public class LoggerRegistry<T extends ExtendedLogger> {
    private static final String DEFAULT_FACTORY_KEY = AbstractLogger.DEFAULT_MESSAGE_FACTORY_CLASS.getName();
    private final MapFactory<T> factory;
    private final Map<String, Map<String, T>> map;

    /**
     * Interface to control the data structure used by the registry to store the Loggers.
     * @param <T> subtype of {@code ExtendedLogger}
     */
    public interface MapFactory<T extends ExtendedLogger> {
        Map<String, T> createInnerMap();

        Map<String, Map<String, T>> createOuterMap();

        void putIfAbsent(Map<String, T> innerMap, String name, T logger);
    }

    /**
     * Generates ConcurrentHashMaps for use by the registry to store the Loggers.
     * @param <T> subtype of {@code ExtendedLogger}
     */
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
            ((ConcurrentMap<String, T>) innerMap).putIfAbsent(name, logger);
        }
    }

    /**
     * Generates WeakHashMaps for use by the registry to store the Loggers.
     * @param <T> subtype of {@code ExtendedLogger}
     */
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

    public LoggerRegistry() {
        this(new ConcurrentMapFactory<T>());
    }

    public LoggerRegistry(final MapFactory<T> factory) {
        this.factory = Objects.requireNonNull(factory, "factory");
        this.map = factory.createOuterMap();
    }

    private static String factoryClassKey(final Class<? extends MessageFactory> messageFactoryClass) {
        return messageFactoryClass == null ? DEFAULT_FACTORY_KEY : messageFactoryClass.getName();
    }

    private static String factoryKey(final MessageFactory messageFactory) {
        return messageFactory == null ? DEFAULT_FACTORY_KEY : messageFactory.getClass().getName();
    }

    /**
     * Returns an ExtendedLogger.
     * @param name The name of the Logger to return.
     * @return The logger with the specified name.
     */
    public T getLogger(final String name) {
        return getOrCreateInnerMap(DEFAULT_FACTORY_KEY).get(name);
    }

    /**
     * Returns an ExtendedLogger.
     * @param name The name of the Logger to return.
     * @param messageFactory The message factory is used only when creating a logger, subsequent use does not change
     *                       the logger but will log a warning if mismatched.
     * @return The logger with the specified name.
     */
    public T getLogger(final String name, final MessageFactory messageFactory) {
        return getOrCreateInnerMap(factoryKey(messageFactory)).get(name);
    }

    public Collection<T> getLoggers() {
        return getLoggers(new ArrayList<T>());
    }

    public Collection<T> getLoggers(final Collection<T> destination) {
        for (final Map<String, T> inner : map.values()) {
            destination.addAll(inner.values());
        }
        return destination;
    }

    private Map<String, T> getOrCreateInnerMap(final String factoryName) {
        Map<String, T> inner = map.get(factoryName);
        if (inner == null) {
            inner = factory.createInnerMap();
            map.put(factoryName, inner);
        }
        return inner;
    }

    /**
     * Detects if a Logger with the specified name exists.
     * @param name The Logger name to search for.
     * @return true if the Logger exists, false otherwise.
     */
    public boolean hasLogger(final String name) {
        return getOrCreateInnerMap(DEFAULT_FACTORY_KEY).containsKey(name);
    }

    /**
     * Detects if a Logger with the specified name and MessageFactory exists.
     * @param name The Logger name to search for.
     * @param messageFactory The message factory to search for.
     * @return true if the Logger exists, false otherwise.
     * @since 2.5
     */
    public boolean hasLogger(final String name, final MessageFactory messageFactory) {
        return getOrCreateInnerMap(factoryKey(messageFactory)).containsKey(name);
    }

    /**
     * Detects if a Logger with the specified name and MessageFactory type exists.
     * @param name The Logger name to search for.
     * @param messageFactoryClass The message factory class to search for.
     * @return true if the Logger exists, false otherwise.
     * @since 2.5
     */
    public boolean hasLogger(final String name, final Class<? extends MessageFactory> messageFactoryClass) {
        return getOrCreateInnerMap(factoryClassKey(messageFactoryClass)).containsKey(name);
    }

    public void putIfAbsent(final String name, final MessageFactory messageFactory, final T logger) {
        factory.putIfAbsent(getOrCreateInnerMap(factoryKey(messageFactory)), name, logger);
    }
}
