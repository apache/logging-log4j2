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

import static org.apache.logging.log4j.spi.LoggingSystemProperty.THREAD_CONTEXT_ENABLE;
import static org.apache.logging.log4j.spi.LoggingSystemProperty.THREAD_CONTEXT_GARBAGE_FREE_ENABLED;
import static org.apache.logging.log4j.spi.LoggingSystemProperty.THREAD_CONTEXT_INITIAL_CAPACITY;
import static org.apache.logging.log4j.spi.LoggingSystemProperty.THREAD_CONTEXT_MAP_CLASS;
import static org.apache.logging.log4j.spi.LoggingSystemProperty.THREAD_CONTEXT_MAP_INHERITABLE;
import static org.apache.logging.log4j.spi.LoggingSystemProperty.THREAD_CONTEXT_STACK_ENABLED;

import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.simple.SimpleLoggerContextFactory;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Lazy;
import org.apache.logging.log4j.util.LowLevelLogUtil;
import org.apache.logging.log4j.util.PropertiesUtil;

/**
 * Model class for a Log4j 2 provider. The properties in this class correspond to the properties used in a
 * {@code META-INF/log4j-provider.properties} file. Note that this class is automatically created by Log4j and should
 * not be used by providers.
 */
public class Provider {
    /**
     * Property name to set for a Log4j 2 provider to specify the priority of this implementation.
     */
    public static final String FACTORY_PRIORITY = "FactoryPriority";
    /**
     * Property name to set to the implementation of {@link org.apache.logging.log4j.spi.ThreadContextMap}.
     */
    public static final String THREAD_CONTEXT_MAP = "ThreadContextMap";
    /**
     * Property name to set to the implementation of {@link org.apache.logging.log4j.spi.LoggerContextFactory}.
     */
    public static final String LOGGER_CONTEXT_FACTORY = "LoggerContextFactory";

    public static final int THREAD_CONTEXT_DEFAULT_INITIAL_CAPACITY = 16;

    private static final Integer DEFAULT_PRIORITY = -1;
    private static final Logger LOGGER = StatusLogger.getLogger();

    private final Integer priority;
    private final String className;
    private final Class<? extends LoggerContextFactory> loggerContextFactoryClass;
    private final String threadContextMap;
    private final Class<? extends ThreadContextMap> threadContextMapClass;
    private final String versions;
    private final URL url;
    private final WeakReference<ClassLoader> classLoader;

    // Temporary fields until we revert to Log4j API 2.x
    private final Lazy<LoggerContextFactory> loggerContextFactory = Lazy.lazy(this::createLoggerContextFactory);
    private final Lazy<ThreadContextMap> threadContextMapFactory = Lazy.lazy(this::createThreadContextMap);
    private final Lazy<ThreadContextStack> threadContextStack = Lazy.lazy(this::createThreadContextStack);
    private final Lock lock = new ReentrantLock();

    public Provider(final Properties props, final URL url, final ClassLoader classLoader) {
        this.url = url;
        this.classLoader = new WeakReference<>(classLoader);
        final String weight = props.getProperty(FACTORY_PRIORITY);
        priority = weight == null ? DEFAULT_PRIORITY : Integer.valueOf(weight);
        className = props.getProperty(LOGGER_CONTEXT_FACTORY);
        threadContextMap = props.getProperty(THREAD_CONTEXT_MAP);
        loggerContextFactoryClass = null;
        threadContextMapClass = null;
        versions = null;
    }

    public Provider(
            final Integer priority,
            final String versions,
            final Class<? extends LoggerContextFactory> loggerContextFactoryClass) {
        this(priority, versions, loggerContextFactoryClass, null);
    }

    public Provider(
            final Integer priority,
            final String versions,
            final Class<? extends LoggerContextFactory> loggerContextFactoryClass,
            final Class<? extends ThreadContextMap> threadContextMapClass) {
        this.url = null;
        this.classLoader = null;
        this.priority = priority;
        this.loggerContextFactoryClass = loggerContextFactoryClass;
        this.threadContextMapClass = threadContextMapClass;
        this.className = null;
        this.threadContextMap = null;
        this.versions = versions;
    }

    /**
     * Returns the Log4j API versions supported by the implementation.
     * @return A String containing the Log4j versions supported.
     */
    public String getVersions() {
        return versions;
    }

    /**
     * Gets the priority (natural ordering) of this Provider.
     *
     * @return the priority of this Provider
     */
    public Integer getPriority() {
        return priority;
    }

    /**
     * Gets the class name of the {@link org.apache.logging.log4j.spi.LoggerContextFactory} implementation of this
     * Provider.
     *
     * @return the class name of a LoggerContextFactory implementation
     */
    public String getClassName() {
        if (loggerContextFactoryClass != null) {
            return loggerContextFactoryClass.getName();
        }
        return className;
    }

    /**
     * Loads the {@link org.apache.logging.log4j.spi.LoggerContextFactory} class specified by this Provider.
     *
     * @return the LoggerContextFactory implementation class or {@code null} if there was an error loading it
     */
    public Class<? extends LoggerContextFactory> loadLoggerContextFactory() {
        if (loggerContextFactoryClass != null) {
            return loggerContextFactoryClass;
        }
        if (className == null) {
            return null;
        }
        final ClassLoader loader = classLoader.get();
        if (loader == null) {
            return null;
        }
        try {
            final Class<?> clazz = loader.loadClass(className);
            if (LoggerContextFactory.class.isAssignableFrom(clazz)) {
                return clazz.asSubclass(LoggerContextFactory.class);
            }
        } catch (final Exception e) {
            LOGGER.error("Unable to create class {} specified in {}", className, url.toString(), e);
        }
        return null;
    }

    /**
     * Gets the class name of the {@link org.apache.logging.log4j.spi.ThreadContextMap} implementation of this Provider.
     *
     * @return the class name of a ThreadContextMap implementation
     */
    public String getThreadContextMap() {
        if (threadContextMapClass != null) {
            return threadContextMapClass.getName();
        }
        return threadContextMap;
    }

    /**
     * Loads the {@link org.apache.logging.log4j.spi.ThreadContextMap} class specified by this Provider.
     *
     * @return the ThreadContextMap implementation class or {@code null} if there was an error loading it
     */
    public Class<? extends ThreadContextMap> loadThreadContextMap() {
        if (threadContextMapClass != null) {
            return threadContextMapClass;
        }
        if (threadContextMap == null) {
            return null;
        }
        final ClassLoader loader = classLoader.get();
        if (loader == null) {
            return null;
        }
        try {
            final Class<?> clazz = loader.loadClass(threadContextMap);
            if (ThreadContextMap.class.isAssignableFrom(clazz)) {
                return clazz.asSubclass(ThreadContextMap.class);
            }
        } catch (final Exception e) {
            LOGGER.error("Unable to create class {} specified in {}", threadContextMap, url.toString(), e);
        }
        return null;
    }

    /**
     * Gets the URL containing this Provider's Log4j details.
     *
     * @return the URL corresponding to the Provider {@code META-INF/log4j-provider.properties} file
     */
    public URL getUrl() {
        return url;
    }

    public LoggerContextFactory getLoggerContextFactory() {
        return loggerContextFactory.get();
    }

    protected LoggerContextFactory createLoggerContextFactory() {
        final String customFactoryClass =
                PropertiesUtil.getProperties().getStringProperty(LoggingSystemProperty.LOGGER_CONTEXT_FACTORY_CLASS);
        if (customFactoryClass != null) {
            final LoggerContextFactory customFactory =
                    LoggingSystem.createInstance(customFactoryClass, LoggerContextFactory.class);
            if (customFactory != null) {
                return customFactory;
            }
        }
        final Class<? extends LoggerContextFactory> factoryClass = loadLoggerContextFactory();
        if (factoryClass != null) {
            final LoggerContextFactory factory = LoggingSystem.tryInstantiate(factoryClass);
            if (factory != null) {
                return factory;
            }
        }
        LowLevelLogUtil.log("Log4j could not find a logging implementation. "
                + "Please add log4j-core dependencies to classpath or module path. "
                + "Using SimpleLogger to log to the console.");
        return SimpleLoggerContextFactory.INSTANCE;
    }

    public ThreadContextMap getThreadContextMapFactory() {
        return threadContextMapFactory.get();
    }

    protected ThreadContextMap createThreadContextMap() {
        // Temporarily copied from Log4j 2.x API until Log4j 3.x API is removed

        // Standard Log4j API implementations are internal and can be only specified by name:
        final String threadContextMap = getThreadContextMap();
        if (threadContextMap != null) {
            /*
             * The constructors are called explicitly to improve GraalVM support.
             *
             * The class names of the package-private implementations from version 2.23.1 must be recognized even
             * if the class is moved.
             */
            switch (threadContextMap) {
                case "org.apache.logging.log4j.spi.NoOpThreadContextMap":
                    return new NoOpThreadContextMap();
                case "org.apache.logging.log4j.spi.GarbageFreeSortedArrayThreadContextMap":
                    return new GarbageFreeSortedArrayThreadContextMap();
                case "org.apache.logging.log4j.spi.CopyOnWriteSortedArrayThreadContextMap":
                    return new CopyOnWriteSortedArrayThreadContextMap();
            }
        }
        //
        final PropertiesUtil environment = PropertiesUtil.getProperties();
        final String customThreadContextMap = environment.getStringProperty(THREAD_CONTEXT_MAP_CLASS);
        if (customThreadContextMap != null) {
            final ThreadContextMap customContextMap =
                    LoggingSystem.createInstance(customThreadContextMap, ThreadContextMap.class);
            if (customContextMap != null) {
                return customContextMap;
            }
        }
        final boolean enableMap = environment.getBooleanProperty(
                LoggingSystemProperty.THREAD_CONTEXT_MAP_ENABLED,
                environment.getBooleanProperty(LoggingSystemProperty.THREAD_CONTEXT_ENABLE, true));
        if (!enableMap) {
            return new NoOpThreadContextMap();
        }
        final Class<? extends ThreadContextMap> mapClass = loadThreadContextMap();
        if (mapClass != null) {
            final ThreadContextMap map = LoggingSystem.tryInstantiate(mapClass);
            if (map != null) {
                return map;
            }
        }
        final boolean garbageFreeEnabled = environment.getBooleanProperty(THREAD_CONTEXT_GARBAGE_FREE_ENABLED);
        final boolean inheritableMap = environment.getBooleanProperty(THREAD_CONTEXT_MAP_INHERITABLE);
        final int initialCapacity = environment.getIntegerProperty(
                THREAD_CONTEXT_INITIAL_CAPACITY, THREAD_CONTEXT_DEFAULT_INITIAL_CAPACITY);
        if (garbageFreeEnabled) {
            return new GarbageFreeSortedArrayThreadContextMap(inheritableMap, initialCapacity);
        }
        return new CopyOnWriteSortedArrayThreadContextMap(inheritableMap, initialCapacity);
    }

    public ThreadContextStack getThreadContextStack() {
        return threadContextStack.get();
    }

    protected ThreadContextStack createThreadContextStack() {
        final PropertiesUtil environment = PropertiesUtil.getProperties();
        final boolean enableStack = environment.getBooleanProperty(
                THREAD_CONTEXT_STACK_ENABLED, environment.getBooleanProperty(THREAD_CONTEXT_ENABLE, true));
        return new DefaultThreadContextStack(enableStack);
    }

    public synchronized void setThreadContextMapFactory(final ThreadContextMap threadContextMapFactory) {
        this.threadContextMapFactory.set(threadContextMapFactory);
    }

    void reset() {
        loggerContextFactory.set(null);
        threadContextMapFactory.set(null);
        threadContextStack.set(null);
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder("Provider[");
        if (!DEFAULT_PRIORITY.equals(priority)) {
            result.append("priority=").append(priority).append(", ");
        }
        if (threadContextMap != null) {
            result.append("threadContextMap=").append(threadContextMap).append(", ");
        } else if (threadContextMapClass != null) {
            result.append("threadContextMapClass=").append(threadContextMapClass.getName());
        }
        if (className != null) {
            result.append("className=").append(className).append(", ");
        } else if (loggerContextFactoryClass != null) {
            result.append("class=").append(loggerContextFactoryClass.getName());
        }
        if (url != null) {
            result.append("url=").append(url);
        }
        final ClassLoader loader;
        if (classLoader == null || (loader = classLoader.get()) == null) {
            result.append(", classLoader=null(not reachable)");
        } else {
            result.append(", classLoader=").append(loader);
        }
        result.append("]");
        return result.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Provider)) {
            return false;
        }

        final Provider provider = (Provider) o;
        return Objects.equals(priority, provider.priority)
                && Objects.equals(className, provider.className)
                && Objects.equals(loggerContextFactoryClass, provider.loggerContextFactoryClass)
                && Objects.equals(versions, provider.versions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(priority, className, loggerContextFactoryClass, versions);
    }
}
