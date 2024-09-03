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
package org.apache.logging.log4j.util;

import static org.apache.logging.log4j.spi.Provider.PROVIDER_PROPERTY_NAME;

import aQute.bnd.annotation.Cardinality;
import aQute.bnd.annotation.Resolution;
import aQute.bnd.annotation.spi.ServiceConsumer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.simple.internal.SimpleProvider;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.apache.logging.log4j.spi.Provider;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * <em>Consider this class private.</em>
 * <p>
 *     Utility class for Log4j {@link Provider}s. When integrating with an application container framework, any Log4j
 *     Providers not accessible through standard classpath scanning should
 *     {@link #loadProvider(java.net.URL, ClassLoader)} a classpath accordingly.
 * </p>
 */
@InternalApi
@ServiceConsumer(value = Provider.class, resolution = Resolution.OPTIONAL, cardinality = Cardinality.MULTIPLE)
public final class ProviderUtil {

    /**
     * Resource name for a Log4j 2 provider properties file.
     */
    static final String PROVIDER_RESOURCE = "META-INF/log4j-provider.properties";

    /**
     * Loaded providers.
     */
    static final Collection<Provider> PROVIDERS = new HashSet<>();

    /**
     * Guards the ProviderUtil singleton instance from lazy initialization.
     * <p>
     *     This is primarily used for OSGi support. It allows the OSGi Activator to pause the startup and wait for a
     *     Provider to be installed. See <a href="https://issues.apache.org/jira/browse/LOG4J2-373">LOG4J2-373</a>.
     * </p>
     */
    static final Lock STARTUP_LOCK = new ReentrantLock();

    private static final String[] COMPATIBLE_API_VERSIONS = {"2.6.0"};
    private static final Logger LOGGER = StatusLogger.getLogger();

    private static volatile Provider PROVIDER;

    private ProviderUtil() {}

    static void addProvider(final Provider provider) {
        if (validVersion(provider.getVersions())) {
            PROVIDERS.add(provider);
            LOGGER.debug("Loaded provider:\n{}", provider);
        } else {
            LOGGER.warn("Ignoring provider for incompatible version {}:\n{}", provider.getVersions(), provider);
        }
    }

    /**
     * Loads an individual Provider implementation. This method is really only useful for the OSGi bundle activator and
     * this class itself.
     *
     * @param url the URL to the provider properties file
     * @param cl the ClassLoader to load the provider classes with
     */
    @SuppressFBWarnings(
            value = "URLCONNECTION_SSRF_FD",
            justification = "Uses a fixed URL that ends in 'META-INF/log4j-provider.properties'.")
    @SuppressWarnings("deprecation")
    static void loadProvider(final URL url, final ClassLoader cl) {
        try {
            final Properties props = PropertiesUtil.loadClose(url.openStream(), url);
            addProvider(new Provider(props, url, cl));
        } catch (final IOException e) {
            LOGGER.error("Unable to open {}", url, e);
        }
    }

    /**
     * @deprecated Use {@link #loadProvider(java.net.URL, ClassLoader)} instead. Will be removed in 3.0.
     */
    @Deprecated
    static void loadProviders(final Enumeration<URL> urls, final ClassLoader cl) {
        if (urls != null) {
            while (urls.hasMoreElements()) {
                loadProvider(urls.nextElement(), cl);
            }
        }
    }

    /**
     * @since 2.24.0
     */
    public static Provider getProvider() {
        lazyInit();
        return PROVIDER;
    }

    public static Iterable<Provider> getProviders() {
        lazyInit();
        return PROVIDERS;
    }

    public static boolean hasProviders() {
        lazyInit();
        return !PROVIDERS.isEmpty();
    }

    /**
     * Lazily initializes the ProviderUtil singleton.
     * <p>
     *     Note that the following initial call to ProviderUtil may block until a Provider has been installed when
     *     running in an OSGi environment.
     * </p>
     */
    static void lazyInit() {
        if (PROVIDER == null) {
            try {
                STARTUP_LOCK.lockInterruptibly();
                try {
                    if (PROVIDER == null) {
                        ServiceLoaderUtil.safeStream(
                                        Provider.class,
                                        ServiceLoader.load(Provider.class, ProviderUtil.class.getClassLoader()),
                                        LOGGER)
                                .filter(provider -> validVersion(provider.getVersions()))
                                .forEach(ProviderUtil::addProvider);

                        for (final LoaderUtil.UrlResource resource :
                                LoaderUtil.findUrlResources(PROVIDER_RESOURCE, false)) {
                            loadProvider(resource.getUrl(), resource.getClassLoader());
                        }
                        PROVIDER = selectProvider(PropertiesUtil.getProperties(), PROVIDERS, LOGGER);
                    }
                } finally {
                    STARTUP_LOCK.unlock();
                }
            } catch (final InterruptedException e) {
                LOGGER.fatal("Interrupted before Log4j Providers could be loaded.", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Used to test the public {@link #getProvider()} method.
     */
    @SuppressWarnings("deprecation")
    static Provider selectProvider(
            final PropertiesUtil properties, final Collection<Provider> providers, final Logger statusLogger) {
        Provider selected = null;
        // 1. Select provider using "log4j.provider" property
        final String providerClass = properties.getStringProperty(PROVIDER_PROPERTY_NAME);
        if (providerClass != null) {
            if (SimpleProvider.class.getName().equals(providerClass)) {
                selected = new SimpleProvider();
            } else {
                try {
                    selected = LoaderUtil.newInstanceOf(providerClass);
                } catch (final Exception e) {
                    statusLogger.error(
                            "Unable to create provider {}.\nFalling back to default selection process.", PROVIDER, e);
                }
            }
        }
        // 2. Use deprecated "log4j2.loggerContextFactory" property to choose the provider
        final String factoryClassName = properties.getStringProperty(LogManager.FACTORY_PROPERTY_NAME);
        if (factoryClassName != null) {
            if (selected != null) {
                statusLogger.warn(
                        "Ignoring {} system property, since {} was set.",
                        LogManager.FACTORY_PROPERTY_NAME,
                        PROVIDER_PROPERTY_NAME);
                // 2a. Scan the known providers for one matching the logger context factory class name.
            } else {
                statusLogger.warn(
                        "Usage of the {} property is deprecated. Use the {} property instead.",
                        LogManager.FACTORY_PROPERTY_NAME,
                        PROVIDER_PROPERTY_NAME);
                for (final Provider provider : providers) {
                    if (factoryClassName.equals(provider.getClassName())) {
                        selected = provider;
                        break;
                    }
                }
            }
            // 2b. Instantiate
            if (selected == null) {
                statusLogger.warn(
                        "No provider found using {} as logger context factory. The factory will be instantiated directly.",
                        factoryClassName);
                try {
                    final Class<?> clazz = LoaderUtil.loadClass(factoryClassName);
                    if (LoggerContextFactory.class.isAssignableFrom(clazz)) {
                        selected = new Provider(null, Strings.EMPTY, clazz.asSubclass(LoggerContextFactory.class));
                    } else {
                        statusLogger.error(
                                "Class {} specified in the {} system property does not extend {}",
                                factoryClassName,
                                LogManager.FACTORY_PROPERTY_NAME,
                                LoggerContextFactory.class.getName());
                    }
                } catch (final Exception e) {
                    statusLogger.error(
                            "Unable to create class {} specified in the {} system property",
                            factoryClassName,
                            LogManager.FACTORY_PROPERTY_NAME,
                            e);
                }
            }
        }
        // 3. Select a provider automatically.
        if (selected == null) {
            final Comparator<Provider> comparator = Comparator.comparing(Provider::getPriority);
            switch (providers.size()) {
                case 0:
                    statusLogger.error("Log4j API could not find a logging provider.");
                    break;
                case 1:
                    break;
                default:
                    statusLogger.warn(providers.stream()
                            .sorted(comparator)
                            .map(Provider::toString)
                            .collect(Collectors.joining("\n", "Log4j API found multiple logging providers:\n", "")));
                    break;
            }
            selected = providers.stream().max(comparator).orElseGet(SimpleProvider::new);
        }
        statusLogger.info("Using provider:\n{}", selected);
        return selected;
    }

    public static ClassLoader findClassLoader() {
        return LoaderUtil.getThreadContextClassLoader();
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
