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
import org.apache.logging.log4j.util.Constants;
import org.apache.logging.log4j.util.Lazy;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.PropertiesUtil;

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

    /**
     * Constant used to disable the {@link ThreadContextMap}.
     * <p>
     *     <strong>Warning:</strong> the value of this constant does not point to a concrete class name.
     * </p>
     * @see #getThreadContextMap
     */
    protected static final String NO_OP_CONTEXT_MAP = "NoOp";

    /**
     * Constant used to select a web application-safe implementation of {@link ThreadContextMap}.
     * <p>
     *     This implementation only binds JRE classes to {@link ThreadLocal} variables.
     * </p>
     * <p>
     *     <strong>Warning:</strong> the value of this constant does not point to a concrete class name.
     * </p>
     * @see #getThreadContextMap
     */
    protected static final String WEB_APP_CONTEXT_MAP = "WebApp";

    /**
     * Constant used to select a copy-on-write implementation of {@link ThreadContextMap}.
     * <p>
     *     <strong>Warning:</strong> the value of this constant does not point to a concrete class name.
     * </p>
     * @see #getThreadContextMap
     */
    protected static final String COPY_ON_WRITE_CONTEXT_MAP = "CopyOnWrite";

    /**
     * Constant used to select a garbage-free implementation of {@link ThreadContextMap}.
     * <p>
     *     This implementation must ensure that common operations don't create new object instances. The drawback is
     *     the necessity to bind custom classes to {@link ThreadLocal} variables.
     * </p>
     * <p>
     *     <strong>Warning:</strong> the value of this constant does not point to a concrete class name.
     * </p>
     * @see #getThreadContextMap
     */
    protected static final String GARBAGE_FREE_CONTEXT_MAP = "GarbageFree";

    // Property keys relevant for context map selection
    private static final String DISABLE_CONTEXT_MAP = "log4j2.disableThreadContextMap";
    private static final String DISABLE_THREAD_CONTEXT = "log4j2.disableThreadContext";
    private static final String THREAD_CONTEXT_MAP_PROPERTY = "log4j2.threadContextMap";
    private static final String GC_FREE_THREAD_CONTEXT_PROPERTY = "log4j2.garbagefree.threadContextMap";

    private static final Integer DEFAULT_PRIORITY = -1;
    private static final Logger LOGGER = StatusLogger.getLogger();

    private final Integer priority;
    // LoggerContextFactory
    @Deprecated
    private final String className;

    private final Class<? extends LoggerContextFactory> loggerContextFactoryClass;
    private final Lazy<LoggerContextFactory> loggerContextFactoryLazy = Lazy.lazy(this::createLoggerContextFactory);
    // ThreadContextMap
    @Deprecated
    private final String threadContextMap;

    private final Class<? extends ThreadContextMap> threadContextMapClass;
    private final Lazy<ThreadContextMap> threadContextMapLazy = Lazy.lazy(this::createThreadContextMap);
    private final String versions;

    @Deprecated
    private final URL url;

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
        priority = weight == null ? DEFAULT_PRIORITY : Integer.valueOf(weight);
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
    public Provider(final Integer priority, final String versions) {
        this(priority, versions, null, null);
    }

    /**
     * @param priority A positive number specifying the provider's priority or {@code null} if default,
     * @param versions Minimal API version required, should be set to {@link #CURRENT_VERSION},
     * @param loggerContextFactoryClass A public exported implementation of {@link LoggerContextFactory} or {@code
     * null} if {@link #createLoggerContextFactory()} is also implemented.
     */
    public Provider(
            final Integer priority,
            final String versions,
            final Class<? extends LoggerContextFactory> loggerContextFactoryClass) {
        this(priority, versions, loggerContextFactoryClass, null);
    }

    /**
     * @param priority A positive number specifying the provider's priority or {@code null} if default,
     * @param versions Minimal API version required, should be set to {@link #CURRENT_VERSION},
     * @param loggerContextFactoryClass A public exported implementation of {@link LoggerContextFactory} or {@code
     * null} if {@link #createLoggerContextFactory()} is also implemented,
     * @param threadContextMapClass A public exported implementation of {@link ThreadContextMap} or {@code null} if
     * {@link #createThreadContextMap()} is implemented.
     */
    public Provider(
            final Integer priority,
            final String versions,
            final Class<? extends LoggerContextFactory> loggerContextFactoryClass,
            final Class<? extends ThreadContextMap> threadContextMapClass) {
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
        return versions;
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
    public String getClassName() {
        return loggerContextFactoryClass != null ? loggerContextFactoryClass.getName() : className;
    }

    /**
     * Loads the {@link LoggerContextFactory} class specified by this Provider.
     *
     * @return the LoggerContextFactory implementation class or {@code null} if unspecified or a loader error occurred.
     * @see #createLoggerContextFactory()
     */
    public Class<? extends LoggerContextFactory> loadLoggerContextFactory() {
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

    private LoggerContextFactory createLoggerContextFactory() {
        final Class<? extends LoggerContextFactory> factoryClass = loadLoggerContextFactory();
        if (factoryClass != null) {
            try {
                return LoaderUtil.newInstanceOf(factoryClass);
            } catch (final Exception e) {
                LOGGER.error(
                        "Unable to create instance of class {} specified in {}", factoryClass.getName(), getUrl(), e);
            }
        }
        LOGGER.warn("Falling back to {}", SimpleLoggerContextFactory.INSTANCE);
        return SimpleLoggerContextFactory.INSTANCE;
    }

    /**
     * @return The logger context factory to be used by {@link org.apache.logging.log4j.LogManager}.
     * @since 2.24.0
     */
    public LoggerContextFactory getLoggerContextFactory() {
        return loggerContextFactoryLazy.get();
    }

    /**
     * Gets the class name of the {@link ThreadContextMap} implementation of this Provider.
     * <p>
     *     This method should return one of the internal implementations:
     *     <ol>
     *         <li>{@code null} if {@link #loadThreadContextMap} is implemented,</li>
     *         <li>{@link #NO_OP_CONTEXT_MAP},</li>
     *         <li>{@link #WEB_APP_CONTEXT_MAP},</li>
     *         <li>{@link #COPY_ON_WRITE_CONTEXT_MAP},</li>
     *         <li>{@link #GARBAGE_FREE_CONTEXT_MAP}.</li>
     *     </ol>
     * </p>
     * @return the class name of a ThreadContextMap implementation
     * @see #loadThreadContextMap()
     */
    public String getThreadContextMap() {
        if (threadContextMapClass != null) {
            return threadContextMapClass.getName();
        }
        // Field value
        if (threadContextMap != null) {
            return threadContextMap;
        }
        // Properties
        final PropertiesUtil props = PropertiesUtil.getProperties();
        if (props.getBooleanProperty(DISABLE_CONTEXT_MAP) || props.getBooleanProperty(DISABLE_THREAD_CONTEXT)) {
            return NO_OP_CONTEXT_MAP;
        }
        final String threadContextMapClass = props.getStringProperty(THREAD_CONTEXT_MAP_PROPERTY);
        if (threadContextMapClass != null) {
            return threadContextMapClass;
        }
        // Default based on properties
        if (props.getBooleanProperty(GC_FREE_THREAD_CONTEXT_PROPERTY)) {
            return GARBAGE_FREE_CONTEXT_MAP;
        }
        return Constants.ENABLE_THREADLOCALS ? COPY_ON_WRITE_CONTEXT_MAP : WEB_APP_CONTEXT_MAP;
    }

    /**
     * Loads the {@link ThreadContextMap} class specified by this Provider.
     *
     * @return the {@code ThreadContextMap} implementation class or {@code null} if unspecified or a loading error
     * occurred.
     * @see #createThreadContextMap()
     */
    public Class<? extends ThreadContextMap> loadThreadContextMap() {
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
            LOGGER.error("Unable to load class {} specified in {}", threadContextMap, url.toString(), e);
        }
        return null;
    }

    /**
     * Creates a {@link ThreadContextMap} using the legacy {@link #loadThreadContextMap()} and
     * {@link #getThreadContextMap()} methods:
     * <ol>
     *     <li>calls {@link #loadThreadContextMap},</li>
     *     <li>if the previous call returns {@code null}, it calls {@link #getThreadContextMap} to instantiate one of
     *     the internal implementations,</li>
     *     <li>it returns a no-op map otherwise.</li>
     * </ol>
     */
    @SuppressWarnings("deprecation")
    ThreadContextMap createThreadContextMap() {
        final Class<? extends ThreadContextMap> threadContextMapClass = loadThreadContextMap();
        if (threadContextMapClass != null) {
            try {
                return LoaderUtil.newInstanceOf(threadContextMapClass);
            } catch (final Exception e) {
                LOGGER.error(
                        "Unable to create instance of class {} specified in {}",
                        threadContextMapClass.getName(),
                        getUrl(),
                        e);
            }
        }
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
                case NO_OP_CONTEXT_MAP:
                case "org.apache.logging.log4j.spi.NoOpThreadContextMap":
                    return new NoOpThreadContextMap();
                case WEB_APP_CONTEXT_MAP:
                case "org.apache.logging.log4j.spi.DefaultThreadContextMap":
                    return new DefaultThreadContextMap();
                case GARBAGE_FREE_CONTEXT_MAP:
                case "org.apache.logging.log4j.spi.GarbageFreeSortedArrayThreadContextMap":
                    return new GarbageFreeSortedArrayThreadContextMap();
                case COPY_ON_WRITE_CONTEXT_MAP:
                case "org.apache.logging.log4j.spi.CopyOnWriteSortedArrayThreadContextMap":
                    return new CopyOnWriteSortedArrayThreadContextMap();
            }
        }
        LOGGER.warn("Falling back to {}", NoOpThreadContextMap.class.getName());
        return new NoOpThreadContextMap();
    }

    // Used for testing
    void resetThreadContextMap() {
        threadContextMapLazy.set(null);
    }

    /**
     * @return The thread context map to be used by {@link org.apache.logging.log4j.ThreadContext}.
     * @since 2.24.0
     */
    public ThreadContextMap getThreadContextMapInstance() {
        return threadContextMapLazy.get();
    }

    /**
     * @return An implementation of the {@link ScopedContextProvider} service to use.
     */
    public ScopedContextProvider getScopedContextProvider() {
        return ScopedContextProvider.simple();
    }

    /**
     * Gets the URL containing this Provider's Log4j details.
     *
     * @return the URL corresponding to the Provider {@code META-INF/log4j-provider.properties} file or {@code null}
     * for a provider class.
     * @deprecated since 2.24.0, without replacement.
     */
    @Deprecated
    public URL getUrl() {
        return url;
    }

    @Override
    public String toString() {
        final StringBuilder result =
                new StringBuilder("Provider '").append(getClass().getName()).append("'");
        if (!DEFAULT_PRIORITY.equals(priority)) {
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
        int result = priority != null ? priority.hashCode() : 0;
        result = 31 * result + (className != null ? className.hashCode() : 0);
        result = 31 * result + (loggerContextFactoryClass != null ? loggerContextFactoryClass.hashCode() : 0);
        result = 31 * result + (versions != null ? versions.hashCode() : 0);
        return result;
    }
}
