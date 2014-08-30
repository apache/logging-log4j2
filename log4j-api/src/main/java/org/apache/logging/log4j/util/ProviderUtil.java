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
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.Provider;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * <em>Consider this class private.</em>
 */
public final class ProviderUtil {

    /**
     * Resource name for a Log4j 2 provider properties file.
     */
    protected static final String PROVIDER_RESOURCE = "META-INF/log4j-provider.properties";
    private static final String API_VERSION = "Log4jAPIVersion";

    private static final String[] COMPATIBLE_API_VERSIONS = {
        "2.0.0"
    };

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static final Collection<Provider> PROVIDERS = new CopyOnWriteArraySet<Provider>();

    private ProviderUtil() {
    }

    static {
        final ClassLoader cl = findClassLoader();
        Enumeration<URL> enumResources = null;
        try {
            enumResources = cl.getResources(PROVIDER_RESOURCE);
        } catch (final IOException e) {
            LOGGER.fatal("Unable to locate {}", PROVIDER_RESOURCE, e);
        }
        loadProviders(enumResources, cl);
    }

    protected static void loadProviders(final Enumeration<URL> enumResources, ClassLoader cl) {
        if (enumResources != null) {
            while (enumResources.hasMoreElements()) {
                final URL url = enumResources.nextElement();
                loadProvider(url, cl);
            }
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

    public static Iterable<Provider> getProviders() {
        return PROVIDERS;
    }

    public static boolean hasProviders() {
        return !PROVIDERS.isEmpty();
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
