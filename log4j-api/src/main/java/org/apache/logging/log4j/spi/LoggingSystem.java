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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
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
import org.apache.logging.log4j.util.InternalApi;
import org.apache.logging.log4j.util.Lazy;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.LowLevelLogUtil;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.PropertyEnvironment;
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

    public static final int THREAD_CONTEXT_DEFAULT_INITIAL_CAPACITY = 16;

    private static final Lazy<LoggingSystem> SYSTEM = Lazy.relaxed(LoggingSystem::new);

    private final Lock initializationLock = new ReentrantLock();
    private volatile SystemProvider provider;
    private final Lazy<PropertyEnvironment> environmentLazy = Lazy.relaxed(PropertiesUtil::getProperties);
    private final Lazy<LoggerContextFactory> loggerContextFactoryLazy = environmentLazy.map(environment ->
            getProvider().createLoggerContextFactory(environment));
    private final Lazy<MessageFactory> messageFactoryLazy = environmentLazy.map(environment -> {
        final String className = environment.getStringProperty(LOGGER_MESSAGE_FACTORY_CLASS);
        if (className != null) {
            final MessageFactory factory = createInstance(className, MessageFactory.class);
            if (factory != null) {
                return factory;
            }
        }
        return Constants.isThreadLocalsEnabled() ? new ReusableMessageFactory() : new ParameterizedMessageFactory();
    });
    private final Lazy<FlowMessageFactory> flowMessageFactoryLazy = environmentLazy.map(environment -> {
        final String className = environment.getStringProperty(LOGGER_FLOW_MESSAGE_FACTORY_CLASS);
        if (className != null) {
            final FlowMessageFactory factory = createInstance(className, FlowMessageFactory.class);
            if (factory != null) {
                return factory;
            }
        }
        return new DefaultFlowMessageFactory();
    });
    private final Lazy<Supplier<ThreadContextMap>> threadContextMapFactoryLazy = environmentLazy.map(environment ->
            () -> getProvider().createContextMap(environment));
    private final Lazy<Supplier<ThreadContextStack>> threadContextStackFactoryLazy = environmentLazy.map(environment ->
            () -> getProvider().createContextStack(environment));

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

    public void setLoggerContextFactory(final LoggerContextFactory loggerContextFactory) {
        loggerContextFactoryLazy.set(loggerContextFactory);
    }

    public void setMessageFactory(final MessageFactory messageFactory) {
        messageFactoryLazy.set(messageFactory);
    }

    public void setFlowMessageFactory(final FlowMessageFactory flowMessageFactory) {
        flowMessageFactoryLazy.set(flowMessageFactory);
    }

    public void setThreadContextMapFactory(final Supplier<ThreadContextMap> threadContextMapFactory) {
        threadContextMapFactoryLazy.set(threadContextMapFactory);
    }

    public void setThreadContextStackFactory(final Supplier<ThreadContextStack> threadContextStackFactory) {
        threadContextStackFactoryLazy.set(threadContextStackFactory);
    }

    /**
     * Gets the LoggingSystem instance.
     */
    public static LoggingSystem getInstance() {
        return SYSTEM.value();
    }

    /**
     * Gets the current LoggerContextFactory. This may initialize the instance if this is the first time it was
     * requested.
     */
    public static LoggerContextFactory getLoggerContextFactory() {
        return getInstance().loggerContextFactoryLazy.value();
    }

    public static MessageFactory getMessageFactory() {
        return getInstance().messageFactoryLazy.value();
    }

    public static FlowMessageFactory getFlowMessageFactory() {
        return getInstance().flowMessageFactoryLazy.value();
    }

    /**
     * Creates a new ThreadContextMap.
     */
    public static ThreadContextMap createContextMap() {
        return getInstance().threadContextMapFactoryLazy.value().get();
    }

    /**
     * Creates a new ThreadContextStack.
     */
    public static ThreadContextStack createContextStack() {
        return getInstance().threadContextStackFactoryLazy.value().get();
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

    private static <T> T tryInstantiate(final Class<T> clazz) {
        Constructor<T> constructor;
        try {
            constructor = clazz.getConstructor();
        } catch (final NoSuchMethodException ignored) {
            try {
                constructor = clazz.getDeclaredConstructor();
                if (!(constructor.canAccess(null) || constructor.trySetAccessible())) {
                    LowLevelLogUtil.log("Unable to access constructor for " + clazz);
                    return null;
                }
            } catch (final NoSuchMethodException e) {
                LowLevelLogUtil.logException("Unable to find a default constructor for " + clazz, e);
                return null;
            }
        }
        try {
            return constructor.newInstance();
        } catch (final InvocationTargetException e) {
            LowLevelLogUtil.logException("Exception thrown by constructor for " + clazz, e.getCause());
        } catch (final InstantiationException | LinkageError e) {
            LowLevelLogUtil.logException("Unable to create instance of " + clazz, e);
        } catch (final IllegalAccessException e) {
            LowLevelLogUtil.logException("Unable to access constructor for " + clazz, e);
        }
        return null;
    }

    private static <T> T createInstance(final String className, final Class<T> type) {
        try {
            final Class<?> loadedClass = LoaderUtil.loadClass(className);
            final Class<? extends T> typedClass = loadedClass.asSubclass(type);
            return tryInstantiate(typedClass);
        } catch (final ClassNotFoundException | ClassCastException e) {
            LowLevelLogUtil.logException(String.format("Unable to load %s class '%s'", type.getSimpleName(), className), e);
            return null;
        }
    }

    private static class SystemProvider {
        private final Provider provider;

        private SystemProvider() {
            this(null);
        }

        private SystemProvider(final Provider provider) {
            this.provider = provider;
        }

        public LoggerContextFactory createLoggerContextFactory(final PropertyEnvironment environment) {
            final String customFactoryClass = environment.getStringProperty(LogManager.FACTORY_PROPERTY_NAME);
            if (customFactoryClass != null) {
                final LoggerContextFactory customFactory = createInstance(customFactoryClass, LoggerContextFactory.class);
                if (customFactory != null) {
                    return customFactory;
                }
            }
            if (provider != null) {
                final Class<? extends LoggerContextFactory> factoryClass = provider.loadLoggerContextFactory();
                if (factoryClass != null) {
                    final LoggerContextFactory factory = tryInstantiate(factoryClass);
                    if (factory != null) {
                        return factory;
                    }
                }
            }
            LowLevelLogUtil.log("Log4j could not find a logging implementation. " +
                    "Please add log4j-core dependencies to classpath or module path. " +
                    "Using SimpleLogger to log to the console.");
            return SimpleLoggerContextFactory.INSTANCE;
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
        public ThreadContextMap createContextMap(final PropertyEnvironment environment) {
            final String customThreadContextMap = environment.getStringProperty(THREAD_CONTEXT_MAP_CLASS);
            if (customThreadContextMap != null) {
                final ThreadContextMap customContextMap = createInstance(customThreadContextMap, ThreadContextMap.class);
                if (customContextMap != null) {
                    return customContextMap;
                }
            }
            final boolean disableMap = environment.getBooleanProperty(THREAD_CONTEXT_MAP_DISABLED,
                    environment.getBooleanProperty(THREAD_CONTEXT_DISABLED));
            if (disableMap) {
                return new NoOpThreadContextMap();
            }
            final Class<? extends ThreadContextMap> mapClass = provider.loadThreadContextMap();
            if (mapClass != null) {
                final ThreadContextMap map = tryInstantiate(mapClass);
                if (map != null) {
                    return map;
                }
            }
            final boolean threadLocalsEnabled = Constants.isThreadLocalsEnabled();
            final boolean garbageFreeEnabled = environment.getBooleanProperty(THREAD_CONTEXT_GARBAGE_FREE_ENABLED);
            final boolean inheritableMap = environment.getBooleanProperty(THREAD_CONTEXT_MAP_INHERITABLE);
            final int initialCapacity = environment.getIntegerProperty(THREAD_CONTEXT_INITIAL_CAPACITY,
                    THREAD_CONTEXT_DEFAULT_INITIAL_CAPACITY);
            if (threadLocalsEnabled) {
                if (garbageFreeEnabled) {
                    return new GarbageFreeSortedArrayThreadContextMap(inheritableMap, initialCapacity);
                }
                return new CopyOnWriteSortedArrayThreadContextMap(inheritableMap, initialCapacity);
            }
            return new DefaultThreadContextMap(true, inheritableMap);
        }

        public ThreadContextStack createContextStack(final PropertyEnvironment environment) {
            final boolean disableStack = environment.getBooleanProperty(THREAD_CONTEXT_STACK_DISABLED,
                    environment.getBooleanProperty(THREAD_CONTEXT_DISABLED));
            return new DefaultThreadContextStack(!disableStack);
        }
    }
}
