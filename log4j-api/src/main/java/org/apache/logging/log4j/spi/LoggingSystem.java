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

import static org.apache.logging.log4j.spi.LoggingSystemProperty.LOGGER_FLOW_MESSAGE_FACTORY_CLASS;
import static org.apache.logging.log4j.spi.LoggingSystemProperty.LOGGER_MESSAGE_FACTORY_CLASS;

import aQute.bnd.annotation.spi.ServiceConsumer;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.message.DefaultFlowMessageFactory;
import org.apache.logging.log4j.message.FlowMessageFactory;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.ReusableMessageFactory;
import org.apache.logging.log4j.simple.SimpleLoggerContextFactory;
import org.apache.logging.log4j.spi.recycler.RecyclerFactory;
import org.apache.logging.log4j.spi.recycler.RecyclerFactoryRegistry;
import org.apache.logging.log4j.util.Lazy;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.LowLevelLogUtil;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.PropertyEnvironment;
import org.apache.logging.log4j.util.ServiceLoaderUtil;

/**
 * Handles initializing the Log4j API through {@link Provider} discovery. This keeps track of which
 * {@link LoggerContextFactory} to use in {@link LogManager} along with factories for {@link ThreadContextMap}
 * and {@link ThreadContextStack} to use in {@link ThreadContext}.
 *
 * @since 3.0.0
 */
@ServiceConsumer(Provider.class)
public class LoggingSystem {
    /**
     * Resource name for a Log4j 2 provider properties file.
     */
    private static final String PROVIDER_RESOURCE = "META-INF/log4j-provider.properties";

    private static final String API_VERSION = "Log4jAPIVersion";
    private static final String[] COMPATIBLE_API_VERSIONS = {"3.0.0"};

    public static final int THREAD_CONTEXT_DEFAULT_INITIAL_CAPACITY = 16;

    private static final Lazy<LoggingSystem> SYSTEM = Lazy.relaxed(LoggingSystem::new);

    private final Lazy<Provider> providerLazy = Lazy.relaxed(this::findProvider);
    private final Lazy<PropertyEnvironment> environmentLazy = Lazy.relaxed(PropertiesUtil::getProperties);
    private final Lazy<MessageFactory> messageFactoryLazy = environmentLazy.map(environment -> {
        final String className = environment.getStringProperty(LOGGER_MESSAGE_FACTORY_CLASS);
        if (className != null) {
            final MessageFactory factory = createInstance(className, MessageFactory.class);
            if (factory != null) {
                return factory;
            }
        }
        return new ReusableMessageFactory();
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
    private final Lazy<Supplier<ThreadContextMap>> threadContextMapFactoryLazy =
            environmentLazy.map(environment -> () -> getProvider().getThreadContextMapFactory());
    private final Lazy<RecyclerFactory> recyclerFactoryLazy =
            environmentLazy.map(RecyclerFactoryRegistry::findRecyclerFactory);

    public LoggingSystem() {}

    public static Provider getProvider() {
        return getInstance().providerLazy.get();
    }

    private Provider findProvider() {
        final SortedMap<Integer, Provider> providers = new TreeMap<>();
        loadDefaultProviders().forEach(p -> providers.put(p.getPriority(), p));
        loadLegacyProviders().forEach(p -> providers.put(p.getPriority(), p));
        if (providers.isEmpty()) {
            return new SimpleProvider();
        }
        final Provider provider = providers.get(providers.lastKey());
        if (providers.size() > 1) {
            final StringBuilder sb = new StringBuilder("Multiple logging implementations found: \n");
            providers.forEach((i, p) -> sb.append(p).append('\n'));
            sb.append("Using ").append(provider);
            LowLevelLogUtil.log(sb.toString());
        }
        return provider;
    }

    public void setMessageFactory(final MessageFactory messageFactory) {
        messageFactoryLazy.set(messageFactory);
    }

    public void setThreadContextMapFactory(final Supplier<ThreadContextMap> threadContextMapFactory) {
        threadContextMapFactoryLazy.set(threadContextMapFactory);
    }

    public void setRecyclerFactory(final RecyclerFactory factory) {
        recyclerFactoryLazy.set(factory);
    }

    /**
     * Gets the LoggingSystem instance.
     */
    public static LoggingSystem getInstance() {
        return SYSTEM.get();
    }

    public static MessageFactory getMessageFactory() {
        return getInstance().messageFactoryLazy.get();
    }

    public static FlowMessageFactory getFlowMessageFactory() {
        return getInstance().flowMessageFactoryLazy.get();
    }

    public static RecyclerFactory getRecyclerFactory() {
        return getInstance().recyclerFactoryLazy.get();
    }

    /**
     * Temporarily for tests
     */
    public static void reset() {
        getProvider().reset();
    }

    private static List<Provider> loadDefaultProviders() {
        return ServiceLoaderUtil.safeStream(ServiceLoader.load(Provider.class, Provider.class.getClassLoader()))
                .filter(provider -> validVersion(provider.getVersions()))
                .collect(Collectors.toList());
    }

    private static List<Provider> loadLegacyProviders() {
        return LoaderUtil.findUrlResources(PROVIDER_RESOURCE).stream()
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

    static <T> T tryInstantiate(final Class<T> clazz) {
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

    static <T> T createInstance(final String className, final Class<T> type) {
        try {
            final Class<?> loadedClass = LoaderUtil.loadClass(className);
            final Class<? extends T> typedClass = loadedClass.asSubclass(type);
            return tryInstantiate(typedClass);
        } catch (final ClassNotFoundException | ClassCastException e) {
            LowLevelLogUtil.logException(
                    String.format("Unable to load %s class '%s'", type.getSimpleName(), className), e);
            return null;
        }
    }

    private static class SimpleProvider extends Provider {
        public SimpleProvider() {
            super(0, "3.0.0", SimpleLoggerContextFactory.class, NoOpThreadContextMap.class);
        }
    }
}
