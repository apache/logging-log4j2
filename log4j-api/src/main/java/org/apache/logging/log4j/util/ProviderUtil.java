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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.Provider;
import org.apache.logging.log4j.status.StatusLogger;

/**
 *
 */
public final class ProviderUtil {

    private static final String PROVIDER_RESOURCE = "META-INF/log4j-provider.properties";
    private static final String API_VERSION = "Log4jAPIVersion";

    private static final String[] COMPATIBLE_API_VERSIONS = {
        "2.0.0"
    };

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static final List<Provider> PROVIDERS = new ArrayList<Provider>();

    private ProviderUtil() {
    }

    static {
        final ClassLoader cl = findClassLoader();
        Enumeration<URL> enumResources = null;
        try {
            enumResources = cl.getResources(PROVIDER_RESOURCE);
        } catch (final IOException e) {
            LOGGER.fatal("Unable to locate " + PROVIDER_RESOURCE, e);
        }

        if (enumResources != null) {
            while (enumResources.hasMoreElements()) {
                final URL url = enumResources.nextElement();
                Properties props;
                try {
                    props = PropertiesUtil.loadClose(url.openStream(), url);
                    if (!validVersion(props.getProperty(API_VERSION))) {
                        continue;
                    }
                    PROVIDERS.add(new Provider(props, url));
                } catch (final IOException ioe) {
                    LOGGER.error("Unable to open " + url.toString(), ioe);
                }
            }
        }
    }

    public static Iterator<Provider> getProviders() {
        return PROVIDERS.iterator();
    }

    public static boolean hasProviders() {
        return PROVIDERS.size() > 0;
    }

    public static ClassLoader findClassLoader() {
        ClassLoader cl;
        if (System.getSecurityManager() == null) {
            cl = Thread.currentThread().getContextClassLoader();
        } else {
            cl = java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction<ClassLoader>() {
                    @Override
                    public ClassLoader run() {
                        return Thread.currentThread().getContextClassLoader();
                    }
                }
            );
        }
        if (cl == null) {
            cl = ProviderUtil.class.getClassLoader();
        }

        return cl;
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
