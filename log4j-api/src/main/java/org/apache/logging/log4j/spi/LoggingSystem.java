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

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.message.DefaultFlowMessageFactory;
import org.apache.logging.log4j.message.FlowMessageFactory;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.ParameterizedMessageFactory;
import org.apache.logging.log4j.message.ReusableMessageFactory;
import org.apache.logging.log4j.simple.SimpleLoggerContextFactory;
import org.apache.logging.log4j.util.Constants;
import org.apache.logging.log4j.util.DefaultPropertyResolver;
import org.apache.logging.log4j.util.InternalApi;
import org.apache.logging.log4j.util.JsonResourcePropertySource;
import org.apache.logging.log4j.util.Lazy;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.LowLevelLogUtil;
import org.apache.logging.log4j.util.PropertiesPropertySource;
import org.apache.logging.log4j.util.PropertyResolver;
import org.apache.logging.log4j.util.PropertySource;
import org.apache.logging.log4j.util.ServiceRegistry;

import static org.apache.logging.log4j.spi.LoggingSystemProperties.*;

/**
 * Handles initializing the Log4j API through {@link Provider} discovery. This keeps track of which
 * {@link LoggerContextFactory} to use in {@link LogManager} along with factories for {@link ThreadContextMap}
 * and {@link ThreadContextStack} to use in {@link ThreadContext}.
 *
 * @since 3.0.0
 */
public class LoggingSystem {
    /**
     * Resource name for a Log4j 2 provider properties file.
     */
    private static final String PROVIDER_RESOURCE = "META-INF/log4j-provider.properties";
    private static final String API_VERSION = "Log4jAPIVersion";
    private static final String[] COMPATIBLE_API_VERSIONS = {"3.0.0"};
    private static final String JSON_FILE_NAME = "log4j2.component.json";
    private static final String PROPERTIES_FILE_NAME = "log4j2.component.properties";
    private static final String DEFAULT_JSON_FILE_NAME = "META-INF/log4j2.default.component.json";
    private static final String DEFAULT_PROPERTIES_FILE_NAME = "META-INF/log4j2.default.component.properties";

    public static final int THREAD_CONTEXT_DEFAULT_INITIAL_CAPACITY = 16;

    private static final Lazy<LoggingSystem> SYSTEM = Lazy.relaxed(LoggingSystem::new);

    private final Lock initializationLock = new ReentrantLock();
    private volatile SystemProvider provider;
    private final PropertyResolver propertyResolver = new DefaultPropertyResolver();
    private final Lazy<ClassFactory> classFactoryLazy = Lazy.relaxed(DefaultClassFactory::new);
    private final Lazy<InstanceFactory> instanceFactoryLazy = Lazy.relaxed(DefaultInstanceFactory::new);
    private final Lazy<LoggerContextFactory> loggerContextFactoryLazy = Lazy.lazy(() ->
            getProvider().createLoggerContextFactory());

    private LoggingSystem() {
        loadPropertySources(propertyResolver);
    }

    /**
     * Acquires a lock on the initialization of locating a logging system provider. This lock should be
     * {@linkplain #releaseInitializationLock() released} once the logging system provider is loaded. This lock is
     * provided to allow for lazy initialization via frameworks like OSGi to wait for a provider to be installed
     * before allowing initialization to continue.
     *
     * @see <a href="https://issues.apache.org/jira/browse/LOG4J2-373">LOG4J2-373</a>
     */
    @InternalApi
    public void acquireInitializationLock() {
        initializationLock.lock();
    }

    /**
     * Releases a lock on the initialization phase of this logging system.
     */
    @InternalApi
    public void releaseInitializationLock() {
        initializationLock.unlock();
    }

    public PropertyResolver propertyResolver() {
        return propertyResolver;
    }

    public void setLoggerContextFactory(final LoggerContextFactory loggerContextFactory) {
        loggerContextFactoryLazy.set(loggerContextFactory);
    }

    public LoggerContextFactory getLoggerContextFactory() {
        return loggerContextFactoryLazy.value();
    }

    public void setClassFactory(final ClassFactory classFactory) {
        classFactoryLazy.set(classFactory);
    }

    public ClassFactory getClassFactory() {
        return classFactoryLazy.value();
    }

    public void setInstanceFactory(final InstanceFactory instanceFactory) {
        instanceFactoryLazy.set(instanceFactory);
    }

    public InstanceFactory getInstanceFactory() {
        return instanceFactoryLazy.value();
    }

    private SystemProvider getProvider() {
        var provider = this.provider;
        if (provider == null) {
            try {
                initializationLock.lockInterruptibly();
                provider = this.provider;
                if (provider == null) {
                    this.provider = provider = findProvider();
                }
            } catch (InterruptedException e) {
                LowLevelLogUtil.logException("Interrupted before Log4j Providers could be loaded", e);
                provider = new SystemProvider();
                Thread.currentThread().interrupt();
            } finally {
                releaseInitializationLock();
            }
        }
        return provider;
    }

    private SystemProvider findProvider() {
        final SortedMap<Integer, Provider> providers = new TreeMap<>();
        loadDefaultProviders().forEach(p -> providers.put(p.getPriority(), p));
        loadLegacyProviders().forEach(p -> providers.put(p.getPriority(), p));
        if (providers.isEmpty()) {
            return new SystemProvider();
        }
        final Provider provider = providers.get(providers.lastKey());
        if (providers.size() > 1) {
            final StringBuilder sb = new StringBuilder("Multiple logging implementations found: \n");
            providers.forEach((i, p) -> sb.append(p).append('\n'));
            sb.append("Using ").append(provider);
            LowLevelLogUtil.log(sb.toString());
        }
        return new SystemProvider(provider);
    }

    private <T> Optional<T> tryGetInstance(final String className, final Class<T> type) {
        return getClassFactory()
                .tryGetClass(className, type)
                .flatMap(getInstanceFactory()::tryGetInstance);
    }

    private boolean isThreadLocalsEnabled() {
        final String value = propertyResolver.getString(SYSTEM_THREAD_LOCALS_ENABLED).orElse("true");
        return "calculate".equalsIgnoreCase(value) ? !isWebAppEnabled() : "true".equalsIgnoreCase(value);
    }

    private boolean isWebAppEnabled() {
        return propertyResolver.getString(SYSTEM_ENABLE_WEBAPP)
                .filter(value -> "calculate".equalsIgnoreCase(value) ? isServletClassAvailable() : "true".equalsIgnoreCase(value))
                .isPresent();
    }

    private boolean isServletClassAvailable() {
        return getClassFactory().isClassAvailable("javax.servlet.Servlet") ||
                getClassFactory().isClassAvailable("jakarta.servlet.Servlet");
    }

    private MessageFactory newMessageFactory(final String context) {
        return propertyResolver.getString(context, LOGGER_MESSAGE_FACTORY_CLASS)
                .flatMap(className -> tryGetInstance(className, MessageFactory.class))
                .orElseGet(() -> isThreadLocalsEnabled() ? ReusableMessageFactory.INSTANCE : ParameterizedMessageFactory.INSTANCE);
    }

    private FlowMessageFactory newFlowMessageFactory(final String context) {
        return propertyResolver.getString(context, LOGGER_FLOW_MESSAGE_FACTORY_CLASS)
                .flatMap(className -> tryGetInstance(className, FlowMessageFactory.class))
                .orElseGet(DefaultFlowMessageFactory::new);
    }

    /**
     * Gets the LoggingSystem instance.
     */
    public static LoggingSystem getInstance() {
        return SYSTEM.value();
    }

    public static PropertyResolver getPropertyResolver() {
        return getInstance().propertyResolver;
    }

    /**
     * Gets the current LoggerContextFactory. This may initialize the instance if this is the first time it was
     * requested.
     */
    public static LoggerContextFactory loggerContextFactory() {
        return getInstance().getLoggerContextFactory();
    }

    public static MessageFactory createMessageFactory(final String context) {
        return getInstance().newMessageFactory(context);
    }

    public static FlowMessageFactory createFlowMessageFactory(final String context) {
        return getInstance().newFlowMessageFactory(context);
    }

    public static MessageFactory getMessageFactory() {
        return createMessageFactory(PropertyResolver.DEFAULT_CONTEXT);
    }

    public static FlowMessageFactory getFlowMessageFactory() {
        return createFlowMessageFactory(PropertyResolver.DEFAULT_CONTEXT);
    }

    /**
     * Creates a new ThreadContextMap.
     */
    public static ThreadContextMap createContextMap() {
        return getInstance().getProvider().createContextMap();
    }

    /**
     * Creates a new ThreadContextStack.
     */
    public static ThreadContextStack createContextStack() {
        return getInstance().getProvider().createContextStack();
    }

    private static void loadPropertySources(final PropertyResolver resolver) {
        loadPropertySourceServices(resolver);
        loadJsonSources(resolver, DEFAULT_JSON_FILE_NAME, 1000);
        loadPropertiesSources(resolver, DEFAULT_PROPERTIES_FILE_NAME, 1000);
        loadJsonSources(resolver, JSON_FILE_NAME, 50);
        loadPropertiesSources(resolver, PROPERTIES_FILE_NAME, 50);
    }

    private static void loadPropertySourceServices(final PropertyResolver resolver) {
        ServiceRegistry.getInstance()
                .getServices(PropertySource.class, MethodHandles.lookup(), null)
                .forEach(resolver::addSource);
    }

    private static void loadJsonSources(final PropertyResolver resolver, final String name, final int priority) {
        LoaderUtil.findResources(name, false)
                .stream()
                .map(url -> loadJsonSource(url, priority))
                .filter(Objects::nonNull)
                .forEach(resolver::addSource);
    }

    private static PropertySource loadJsonSource(final URL url, final int priority) {
        try {
            return JsonResourcePropertySource.fromUrl(url, priority);
        } catch (final RuntimeException e) {
            LowLevelLogUtil.logException("Unable to read " + url, e);
            return null;
        }
    }

    private static void loadPropertiesSources(final PropertyResolver resolver, final String name, final int priority) {
        LoaderUtil.findResources(name, false)
                .stream()
                .map(url -> loadPropertiesSource(url, priority))
                .filter(Objects::nonNull)
                .forEach(resolver::addSource);
    }

    private static PropertySource loadPropertiesSource(final URL url, final int priority) {
        final Properties properties = new Properties();
        try (final InputStream in = url.openStream()) {
            properties.load(in);
            return new PropertiesPropertySource(properties, priority);
        } catch (final IOException e) {
            LowLevelLogUtil.logException("Unable to read " + url, e);
            return null;
        }
    }

    private static List<Provider> loadDefaultProviders() {
        return ServiceRegistry.getInstance()
                .getServices(Provider.class, MethodHandles.lookup(), provider -> validVersion(provider.getVersions()));
    }

    private static List<Provider> loadLegacyProviders() {
        return LoaderUtil.findUrlResources(PROVIDER_RESOURCE, false)
                .stream()
                .map(urlResource -> loadLegacyProvider(urlResource.getUrl(), urlResource.getClassLoader()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static Provider loadLegacyProvider(final URL url, final ClassLoader classLoader) {
        final Properties properties = new Properties();
        try (final InputStream in = url.openStream()) {
            properties.load(in);
        } catch (IOException e) {
            LowLevelLogUtil.logException("Unable to load file " + url, e);
            return null;
        }
        if (validVersion(properties.getProperty(API_VERSION))) {
            return new Provider(properties, url, classLoader);
        }
        return null;
    }

    private static boolean validVersion(final String version) {
        for (final String v : COMPATIBLE_API_VERSIONS) {
            if (version.startsWith(v)) {
                return true;
            }
        }
        return false;
    }

    private class SystemProvider {
        private final Provider provider;

        private SystemProvider() {
            this(null);
        }

        private SystemProvider(final Provider provider) {
            this.provider = provider;
        }

        public LoggerContextFactory createLoggerContextFactory() {
            return propertyResolver.getString(LogManager.FACTORY_PROPERTY_NAME)
                    .flatMap(className -> tryGetInstance(className, LoggerContextFactory.class))
                    .or(() -> Optional.ofNullable(provider)
                            .map(Provider::loadLoggerContextFactory)
                            .flatMap(getInstanceFactory()::tryGetInstance))
                    .orElseGet(() -> {
                        LowLevelLogUtil.log("Log4j could not find a logging implementation. " +
                                "Please add log4j-core dependencies to classpath or module path. " +
                                "Using SimpleLogger to log to the console.");
                        return SimpleLoggerContextFactory.INSTANCE;
                    });
        }

        /**
         * Creates the ThreadContextMap instance used by the ThreadContext.
         * <p>
         * If {@linkplain Constants#isThreadLocalsEnabled() Log4j can use ThreadLocals}, a garbage-free StringMap-based context map can
         * be installed by setting system property {@value LoggingSystemProperties#THREAD_CONTEXT_GARBAGE_FREE_ENABLED} to {@code true}.
         * </p><p>
         * Furthermore, any custom {@code ThreadContextMap} can be installed by setting system property
         * {@value LoggingSystemProperties#THREAD_CONTEXT_MAP_CLASS} to the fully qualified class name of the class implementing the
         * {@code ThreadContextMap} interface. (Also implement the {@code ReadOnlyThreadContextMap} interface if your custom
         * {@code ThreadContextMap} implementation should be accessible to applications via the
         * {@link ThreadContext#getThreadContextMap()} method.)
         * </p><p>
         * Instead of system properties, the above can also be specified in a properties file named
         * {@code log4j2.component.properties} in the classpath.
         * </p>
         *
         * @see ThreadContextMap
         * @see ReadOnlyThreadContextMap
         * @see org.apache.logging.log4j.ThreadContext
         */
        public ThreadContextMap createContextMap() {
            return propertyResolver.getString(THREAD_CONTEXT_MAP_CLASS)
                    .flatMap(className -> tryGetInstance(className, ThreadContextMap.class))
                    .orElseGet(() -> {
                        final boolean enabled = propertyResolver.getBoolean(THREAD_CONTEXT_MAP_ENABLED, true) &&
                                propertyResolver.getBoolean(THREAD_CONTEXT_ENABLED, true);
                        if (!enabled) {
                            return new NoOpThreadContextMap();
                        }
                        return Optional.ofNullable(provider.loadThreadContextMap())
                                .<ThreadContextMap>flatMap(getInstanceFactory()::tryGetInstance)
                                .orElseGet(() -> {
                                    final boolean garbageFreeEnabled = propertyResolver.getBoolean(THREAD_CONTEXT_GARBAGE_FREE_ENABLED);
                                    final boolean inheritableMap = propertyResolver.getBoolean(THREAD_CONTEXT_MAP_INHERITABLE);
                                    final int initialCapacity = propertyResolver.getInt(THREAD_CONTEXT_INITIAL_CAPACITY)
                                            .orElse(THREAD_CONTEXT_DEFAULT_INITIAL_CAPACITY);
                                    if (isThreadLocalsEnabled()) {
                                        if (garbageFreeEnabled) {
                                            return new GarbageFreeSortedArrayThreadContextMap(inheritableMap, initialCapacity);
                                        }
                                        return new CopyOnWriteSortedArrayThreadContextMap(inheritableMap, initialCapacity);
                                    }
                                    return new DefaultThreadContextMap(true, inheritableMap);
                                });
                    });
        }

        public ThreadContextStack createContextStack() {
            final boolean enabled = propertyResolver.getBoolean(THREAD_CONTEXT_STACK_ENABLED, true) &&
                    propertyResolver.getBoolean(THREAD_CONTEXT_ENABLED, true);
            return new DefaultThreadContextStack(enabled);
        }
    }
}
