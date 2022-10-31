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

package org.apache.logging.log4j.util3;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.simple.SimpleLoggingSystemProvider;
import org.apache.logging.log4j.spi.LegacyLoggingSystemProvider;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.apache.logging.log4j.spi.LoggingSystemProvider;
import org.apache.logging.log4j.spi.ThreadContextMap;
import org.apache.logging.log4j.spi.ThreadContextStack;
import org.apache.logging.log4j.util.Lazy;
import org.apache.logging.log4j.util.PropertyEnvironment;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Central tracking for how to obtain the system's {@link LoggerContextFactory} along with which
 * {@link LoggingSystemProvider} is installed in general.
 */
public class LoggingSystem {
    /**
     * Resource name for a Log4j 2 provider properties file.
     */
    private static final String PROVIDER_RESOURCE = "META-INF/log4j-provider.properties";
    // these API versions correspond to the general LoggerContextFactory/LoggerContext/Logger/Message/Marker API
    // this does not correspond to the version of the provider
    // (whether it be a v2-style properties file or a v3-style service provider)
    private static final String[] COMPATIBLE_API_VERSIONS = {"2.6.0"};
    private static final Lazy<LoggingSystem> INSTANCE = Lazy.relaxed(LoggingSystem::new);

    private final Lock initializationLock = new ReentrantLock();
    private volatile LoggingSystemProvider provider;
    private final Lazy<LoggerContextFactory> loggerContextFactory =
            Lazy.relaxed(() -> {
                final PropertyEnvironment environment = PropertiesUtil.getProperties();
                final String factoryClassName = environment.getStringProperty(LogManager.FACTORY_PROPERTY_NAME);
                if (factoryClassName != null) {
                    try {
                        return LoaderUtil.newCheckedInstanceOf(factoryClassName, LoggerContextFactory.class);
                    } catch (final ClassNotFoundException e) {
                        LowLevelLogUtil.logException(String.format("Unable to locate configured LoggerContextFactory '%s'", factoryClassName), e);
                    } catch (final InvocationTargetException | InstantiationException | IllegalAccessException | LinkageError e) {
                        LowLevelLogUtil.logException(String.format("Unable to create configured LoggerContextFactory '%s'", factoryClassName), e);
                    }
                }
                return getProvider().getLoggerContextFactory();
            });
    private final Lazy<ThreadContextMap.Factory> contextMapFactory =
            Lazy.relaxed(() -> getProvider().getContextMapFactory());
    private final Lazy<ThreadContextStack.Factory> contextStackFactory =
            Lazy.relaxed(() -> getProvider().getContextStackFactory());

    /**
     * Indicates if a LoggingSystemProvider has been selected.
     */
    public boolean hasProvider() {
        return provider != null;
    }

    /**
     * Acquires a lock on the initialization of locating a logging system provider. This lock should be
     * {@linkplain #releaseInitializationLock() released} once the logging system provider is loaded. This lock is
     * provided to allow for lazy initialization via frameworks like OSGi to wait for a provider to be installed
     * before allowing initialization to continue.
     *
     * @see <a href="https://issues.apache.org/jira/browse/LOG4J2-373">LOG4J2-373</a>
     */
    public void acquireInitializationLock() {
        initializationLock.lock();
    }

    /**
     * Releases a lock on the initialization phase of this logging system.
     */
    public void releaseInitializationLock() {
        initializationLock.unlock();
    }

    /**
     * Gets or initializes the LoggingSystemProvider.
     */
    public LoggingSystemProvider getProvider() {
        var provider = this.provider;
        if (provider == null) {
            try {
                initializationLock.lockInterruptibly();
                provider = this.provider;
                if (provider == null) {
                    this.provider = provider = findProvider();
                }
            } catch (final InterruptedException e) {
                LowLevelLogUtil.logException("ERROR: Interrupted before Log4j Providers could be loaded.", e);
                provider = createFallbackProvider();
                Thread.currentThread().interrupt();
            } finally {
                releaseInitializationLock();
            }
        }
        return provider;
    }

    private LoggingSystemProvider findProvider() {
        final SortedMap<Integer, LoggingSystemProvider> providers = new TreeMap<>();
        loadDefaultProviders().forEach(provider -> providers.put(provider.getPriority(), provider));
        loadLegacyProviders().forEach(provider -> providers.put(provider.getPriority(), provider));
        if (providers.size() == 1) {
            return providers.get(providers.lastKey());
        } else if (providers.size() > 1) {
            final StringBuilder sb = new StringBuilder("WARN: Multiple logging implementations found: \n");
            for (final Map.Entry<Integer, LoggingSystemProvider> entry : providers.entrySet()) {
                sb.append("Provider: ").append(entry.getValue().getClass().getName());
                sb.append(", Weighting: ").append(entry.getKey()).append('\n');
            }
            final LoggingSystemProvider provider = providers.get(providers.lastKey());
            sb.append("Using provider: ").append(provider.getClass().getName());
            LowLevelLogUtil.log(sb.toString());
            return provider;
        }
        return createFallbackProvider();
    }

    public LoggerContextFactory getLoggerContextFactory() {
        return loggerContextFactory.value();
    }

    public void setLoggerContextFactory(final LoggerContextFactory factory) {
        loggerContextFactory.set(factory);
    }

    public ThreadContextMap createContextMap() {
        return contextMapFactory.value().createThreadContextMap();
    }

    public void setContextMapFactory(final ThreadContextMap.Factory factory) {
        contextMapFactory.set(factory);
    }

    public ThreadContextStack createContextStack() {
        return contextStackFactory.value().createThreadContextStack();
    }

    public boolean isContextStackEnabled() {
        return contextStackFactory.value().isEnabled();
    }

    public void setContextStackFactory(final ThreadContextStack.Factory factory) {
        contextStackFactory.set(factory);
    }

    private static LoggingSystemProvider createFallbackProvider() {
        LowLevelLogUtil.log("ERROR: Log4j could not find a logging implementation. "
                + "Please add log4j-core to the classpath. Using SimpleLogger to log to the console...");
        return new SimpleLoggingSystemProvider();
    }

    /**
     * Returns the LoggingSystem instance.
     */
    public static LoggingSystem getInstance() {
        return INSTANCE.value();
    }

    private static List<LoggingSystemProvider> loadDefaultProviders() {
        return ServiceRegistry.getInstance()
                .getServices(LoggingSystemProvider.class, MethodHandles.lookup(), LoggingSystem::validProvider);
    }

    private static List<LoggingSystemProvider> loadLegacyProviders() {
        return LoaderUtil.findUrlResources(PROVIDER_RESOURCE, false)
                .stream()
                .map(resource -> loadProvider(resource.getUrl(), resource.getClassLoader()))
                .filter(LoggingSystem::validProvider)
                .collect(Collectors.toList());
    }

    private static LoggingSystemProvider loadProvider(final URL url, final ClassLoader cl) {
        try {
            return new LegacyLoggingSystemProvider<>(url, cl);
        } catch (final IOException e) {
            LowLevelLogUtil.logException("Unable to load " + url, e);
            return null;
        }
    }

    private static boolean validProvider(final LoggingSystemProvider provider) {
        return provider != null && validVersion(provider.getVersion());
    }

    private static boolean validVersion(final String version) {
        for (final String v : COMPATIBLE_API_VERSIONS) {
            if (version.startsWith(v)) {
                return true;
            }
        }
        return false;
    }
}
