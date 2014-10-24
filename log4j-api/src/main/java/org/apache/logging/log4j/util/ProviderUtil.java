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
package org.apache.logging.log4j.util;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.Provider;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * <em>Consider this class private.</em>
 * Utility class for Log4j {@link Provider}s. When integrating with an application container framework, any Log4j
 * Providers not accessible through standard classpath scanning should {@link #loadProvider(java.net.URL, ClassLoader)}
 * a classpath accordingly.
 */
public final class ProviderUtil {

    /**
     * Resource name for a Log4j 2 provider properties file.
     */
    protected static final String PROVIDER_RESOURCE = "META-INF/log4j-provider.properties";
    private static final String API_VERSION = "Log4jAPIVersion";

    private static final String[] COMPATIBLE_API_VERSIONS = {
        "2.0.0", "2.1.0"
    };

    private static final Logger LOGGER = StatusLogger.getLogger();

    protected static final Collection<Provider> PROVIDERS = new HashSet<Provider>();

    /**
     * Guards the ProviderUtil singleton instance from lazy initialization. This is primarily used for OSGi support.
     *
     * @since 2.1
     */
    protected static final Lock STARTUP_LOCK = new ReentrantLock();
    // STARTUP_LOCK guards INSTANCE for lazy initialization; this allows the OSGi Activator to pause the startup and
    // wait for a Provider to be installed. See LOG4J2-373
    private static volatile ProviderUtil INSTANCE;

    private ProviderUtil() {
        for (final LoaderUtil.UrlResource resource : LoaderUtil.findUrlResources(PROVIDER_RESOURCE)) {
            loadProvider(resource.getUrl(), resource.getClassLoader());
        }
    }

    /**
     * Loads an individual Provider implementation. This method is really only useful for the OSGi bundle activator
     * and this class itself.
     *
     * @param url the URL to the provider properties file
     * @param cl the ClassLoader to load the provider classes with
     */
    protected static void loadProvider(final URL url, final ClassLoader cl) {
        try {
            final Properties props = PropertiesUtil.loadClose(url.openStream(), url);
            if (validVersion(props.getProperty(API_VERSION))) {
                PROVIDERS.add(new Provider(props, url, cl));
            }
        } catch (final IOException e) {
            LOGGER.error("Unable to open {}", url, e);
        }
    }

    /**
     * @deprecated Use {@link #loadProvider(java.net.URL, ClassLoader)} instead. Will be removed in 3.0.
     */
    @Deprecated
    protected static void loadProviders(final Enumeration<URL> urls, final ClassLoader cl) {
        if (urls != null) {
            while (urls.hasMoreElements()) {
                loadProvider(urls.nextElement(), cl);
            }
        }
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
     *
     * @since 2.1
     */
    protected static void lazyInit() {
        //noinspection DoubleCheckedLocking
        if (INSTANCE == null) {
            try {
                STARTUP_LOCK.lockInterruptibly();
                if (INSTANCE == null) {
                    INSTANCE = new ProviderUtil();
                }
            } catch (final InterruptedException e) {
                LOGGER.fatal("Interrupted before Log4j Providers could be loaded.", e);
                Thread.currentThread().interrupt();
            } finally {
                STARTUP_LOCK.unlock();
            }
        }
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
