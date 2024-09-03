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

import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.Objects;
import java.util.Properties;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.simple.SimpleLoggerContextFactory;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Service class used to bind the Log4j API with an implementation.
 * <p>
 *     Implementors should register an implementation of this class with {@link java.util.ServiceLoader}.
 * </p>
 * <p>
 *     <strong>Deprecated:</strong> the automatic registration of providers from
 *     {@code META-INF/log4j-provider.properties} is supported for compatibility reasons. Support for this file will
 *     be dropped in a future version.
 * </p>
 */
@NullMarked
public class Provider {
    /**
     * Constant inlined by the compiler
     */
    protected static final String CURRENT_VERSION = "2.6.0";

    /**
     * Property name to set for a Log4j 2 provider to specify the priority of this implementation.
     * @deprecated since 2.24.0
     */
    @Deprecated
    public static final String FACTORY_PRIORITY = "FactoryPriority";

    /**
     * Property name to set to the implementation of {@link ThreadContextMap}.
     * @deprecated since 2.24.0
     */
    @Deprecated
    public static final String THREAD_CONTEXT_MAP = "ThreadContextMap";

    /**
     * Property name to set to the implementation of {@link LoggerContextFactory}.
     * @deprecated since 2.24.0
     */
    @Deprecated
    public static final String LOGGER_CONTEXT_FACTORY = "LoggerContextFactory";

    /**
     * System property used to specify the class name of the provider to use.
     * @since 2.24.0
     */
    public static final String PROVIDER_PROPERTY_NAME = "log4j.provider";

    private static final String DISABLE_CONTEXT_MAP = "log4j2.disableThreadContextMap";
    private static final String DISABLE_THREAD_CONTEXT = "log4j2.disableThreadContext";

    private static final int DEFAULT_PRIORITY = -1;
    private static final Logger LOGGER = StatusLogger.getLogger();

    private final int priority;
    // LoggerContextFactory
    @Deprecated
    private final @Nullable String className;

    private final @Nullable Class<? extends LoggerContextFactory> loggerContextFactoryClass;
    // ThreadContextMap
    @Deprecated
    private final @Nullable String threadContextMap;

    private final @Nullable Class<? extends ThreadContextMap> threadContextMapClass;
    private final @Nullable String versions;

    @Deprecated
    private final @Nullable URL url;

    @Deprecated
    private final WeakReference<ClassLoader> classLoader;

    /**
     * Constructor used by the deprecated {@code META-INF/log4j-provider.properties} format.
     * @deprecated since 2.24.0
     */
    @Deprecated
    public Provider(final Properties props, final URL url, final ClassLoader classLoader) {
        this.url = url;
        this.classLoader = new WeakReference<>(classLoader);
        final String weight = props.getProperty(FACTORY_PRIORITY);
        priority = weight == null ? DEFAULT_PRIORITY : Integer.parseInt(weight);
        className = props.getProperty(LOGGER_CONTEXT_FACTORY);
        threadContextMap = props.getProperty(THREAD_CONTEXT_MAP);
        loggerContextFactoryClass = null;
        threadContextMapClass = null;
        versions = null;
    }

    /**
     * @param priority A positive number specifying the provider's priority or {@code null} if default,
     * @param versions Minimal API version required, should be set to {@link #CURRENT_VERSION}.
     * @since 2.24.0
     */
    public Provider(final @Nullable Integer priority, final String versions) {
        this(priority, versions, null, null);
    }

    /**
     * @param priority A positive number specifying the provider's priority or {@code null} if default,
     * @param versions Minimal API version required, should be set to {@link #CURRENT_VERSION},
     * @param loggerContextFactoryClass A public exported implementation of {@link LoggerContextFactory} or {@code
     * null} if {@link #getLoggerContextFactory()} is also implemented.
     */
    public Provider(
            final @Nullable Integer priority,
            final String versions,
            final @Nullable Class<? extends LoggerContextFactory> loggerContextFactoryClass) {
        this(priority, versions, loggerContextFactoryClass, null);
    }

    /**
     * @param priority A positive number specifying the provider's priority or {@code null} if default,
     * @param versions Minimal API version required, should be set to {@link #CURRENT_VERSION},
     * @param loggerContextFactoryClass A public exported implementation of {@link LoggerContextFactory} or {@code
     * null} if {@link #getLoggerContextFactory()} is also implemented,
     * @param threadContextMapClass A public exported implementation of {@link ThreadContextMap} or {@code null} if
     * {@link #getThreadContextMapInstance()} is implemented.
     */
    public Provider(
            final @Nullable Integer priority,
            final String versions,
            final @Nullable Class<? extends LoggerContextFactory> loggerContextFactoryClass,
            final @Nullable Class<? extends ThreadContextMap> threadContextMapClass) {
        this.priority = priority != null ? priority : DEFAULT_PRIORITY;
        this.versions = versions;
        this.loggerContextFactoryClass = loggerContextFactoryClass;
        this.threadContextMapClass = threadContextMapClass;
        // Deprecated
        className = null;
        threadContextMap = null;
        url = null;
        classLoader = new WeakReference<>(null);
    }

    /**
     * Returns the Log4j API versions supported by the implementation.
     * @return A String containing the Log4j versions supported.
     */
    public String getVersions() {
        return versions != null ? versions : "";
    }

    /**
     * Gets the priority (natural ordering) of this Provider.
     * <p>
     *     Log4j selects the highest priority provider.
     * </p>
     * @return the priority of this Provider
     */
    public Integer getPriority() {
        return priority;
    }

    /**
     * Gets the class name of the {@link LoggerContextFactory} implementation of this Provider.
     *
     * @return the class name of a LoggerContextFactory implementation or {@code null} if unspecified.
     * @see #loadLoggerContextFactory()
     */
    public @Nullable String getClassName() {
        return loggerContextFactoryClass != null ? loggerContextFactoryClass.getName() : className;
    }

    /**
     * Loads the {@link LoggerContextFactory} class specified by this Provider.
     *
     * @return the LoggerContextFactory implementation class or {@code null} if unspecified or a loader error occurred.
     */
    public @Nullable Class<? extends LoggerContextFactory> loadLoggerContextFactory() {
        if (loggerContextFactoryClass != null) {
            return loggerContextFactoryClass;
        }
        final String className = getClassName();
        final ClassLoader loader = classLoader.get();
        // Support for deprecated {@code META-INF/log4j-provider.properties} format.
        // In the remaining cases {@code loader == null}.
        if (loader == null || className == null) {
            return null;
        }
        try {
            final Class<?> clazz = loader.loadClass(className);
            if (LoggerContextFactory.class.isAssignableFrom(clazz)) {
                return clazz.asSubclass(LoggerContextFactory.class);
            } else {
                LOGGER.error(
                        "Class {} specified in {} does not extend {}",
                        className,
                        getUrl(),
                        LoggerContextFactory.class.getName());
            }
        } catch (final Exception e) {
            LOGGER.error("Unable to create class {} specified in {}", className, getUrl(), e);
        }
        return null;
    }

    /**
     * @return The logger context factory to be used by {@link org.apache.logging.log4j.LogManager}.
     * @since 2.24.0
     */
    public LoggerContextFactory getLoggerContextFactory() {
        final Class<? extends LoggerContextFactory> implementation = loadLoggerContextFactory();
        if (implementation != null) {
            try {
                return LoaderUtil.newInstanceOf(implementation);
            } catch (final ReflectiveOperationException e) {
                LOGGER.error("Failed to instantiate logger context factory {}.", implementation.getName(), e);
            }
        }
        LOGGER.error("Falling back to simple logger context factory: {}", SimpleLoggerContextFactory.class.getName());
        return SimpleLoggerContextFactory.INSTANCE;
    }

    /**
     * Gets the class name of the {@link org.apache.logging.log4j.spi.ThreadContextMap} implementation of this Provider.
     *
     * @return the class name of a ThreadContextMap implementation
     */
    public @Nullable String getThreadContextMap() {
        return threadContextMapClass != null ? threadContextMapClass.getName() : threadContextMap;
    }

    /**
     * Loads the {@link ThreadContextMap} class specified by this Provider.
     *
     * @return the {@code ThreadContextMap} implementation class or {@code null} if unspecified or a loading error
     * occurred.
     */
    public @Nullable Class<? extends ThreadContextMap> loadThreadContextMap() {
        if (threadContextMapClass != null) {
            return threadContextMapClass;
        }
        final String threadContextMap = getThreadContextMap();
        final ClassLoader loader = classLoader.get();
        // Support for deprecated {@code META-INF/log4j-provider.properties} format.
        // In the remaining cases {@code loader == null}.
        if (loader == null || threadContextMap == null) {
            return null;
        }
        try {
            final Class<?> clazz = loader.loadClass(threadContextMap);
            if (ThreadContextMap.class.isAssignableFrom(clazz)) {
                return clazz.asSubclass(ThreadContextMap.class);
            } else {
                LOGGER.error(
                        "Class {} specified in {} does not extend {}",
                        threadContextMap,
                        getUrl(),
                        ThreadContextMap.class.getName());
            }
        } catch (final Exception e) {
            LOGGER.error("Unable to load class {} specified in {}", threadContextMap, url, e);
        }
        return null;
    }

    /**
     * @return The thread context map to be used by {@link org.apache.logging.log4j.ThreadContext}.
     * @since 2.24.0
     */
    public ThreadContextMap getThreadContextMapInstance() {
        final Class<? extends ThreadContextMap> implementation = loadThreadContextMap();
        if (implementation != null) {
            try {
                return LoaderUtil.newInstanceOf(implementation);
            } catch (final ReflectiveOperationException e) {
                LOGGER.error("Failed to instantiate logger context factory {}.", implementation.getName(), e);
            }
        }
        final PropertiesUtil props = PropertiesUtil.getProperties();
        return props.getBooleanProperty(DISABLE_CONTEXT_MAP) || props.getBooleanProperty(DISABLE_THREAD_CONTEXT)
                ? NoOpThreadContextMap.INSTANCE
                : new DefaultThreadContextMap();
    }

    /**
     * Gets the URL containing this Provider's Log4j details.
     *
     * @return the URL corresponding to the Provider {@code META-INF/log4j-provider.properties} file or {@code null}
     * for a provider class.
     * @deprecated since 2.24.0, without replacement.
     */
    @Deprecated
    public @Nullable URL getUrl() {
        return url;
    }

    @Override
    public String toString() {
        final StringBuilder result =
                new StringBuilder("Provider '").append(getClass().getName()).append("'");
        if (priority != DEFAULT_PRIORITY) {
            result.append("\n\tpriority = ").append(priority);
        }
        final String threadContextMap = getThreadContextMap();
        if (threadContextMap != null) {
            result.append("\n\tthreadContextMap = ").append(threadContextMap);
        }
        final String loggerContextFactory = getClassName();
        if (loggerContextFactory != null) {
            result.append("\n\tloggerContextFactory = ").append(loggerContextFactory);
        }
        if (url != null) {
            result.append("\n\turl = ").append(url);
        }
        if (Provider.class.equals(getClass())) {
            final ClassLoader loader = classLoader.get();
            if (loader == null) {
                result.append("\n\tclassLoader = null or not reachable");
            } else {
                result.append("\n\tclassLoader = ").append(loader);
            }
        }
        return result.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof Provider) {
            final Provider provider = (Provider) o;
            return Objects.equals(priority, provider.priority)
                    && Objects.equals(className, provider.className)
                    && Objects.equals(loggerContextFactoryClass, provider.loggerContextFactoryClass)
                    && Objects.equals(versions, provider.versions);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(priority, className, loggerContextFactoryClass, versions);
    }
}
