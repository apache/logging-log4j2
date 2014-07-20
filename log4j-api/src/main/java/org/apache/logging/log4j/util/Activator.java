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
import java.security.Permission;
import java.util.Enumeration;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
import org.osgi.framework.AdminPermission;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;

/**
 * OSGi bundle activator. Used for locating an implementation of
 * {@link org.apache.logging.log4j.spi.LoggerContextFactory} et al. that have corresponding
 * {@code META-INF/log4j-provider.properties} files. As with all OSGi BundleActivator classes, this class is not for
 * public use and is only useful in an OSGi framework environment.
 */
public class Activator implements BundleActivator, SynchronousBundleListener {

    private static final SecurityManager SECURITY_MANAGER = System.getSecurityManager();

    private static final Logger LOGGER = StatusLogger.getLogger();

    static {
        checkPermission(new RuntimePermission("createClassLoader"));
    }

    private static void checkPermission(final Permission permission) {
        if (SECURITY_MANAGER != null) {
            SECURITY_MANAGER.checkPermission(permission);
        }
    }

    private void loadProvider(final Bundle bundle) {
        try {
            checkPermission(new AdminPermission(bundle, AdminPermission.RESOURCE));
            final URL url = bundle.getEntry(ProviderUtil.PROVIDER_RESOURCE);
            if (url != null) {
                checkPermission(new AdminPermission(bundle, AdminPermission.CLASS));
                ProviderUtil.loadProvider(url, new BundleDelegatingClassLoader(bundle));
            }
        } catch (final Exception e) {
            LOGGER.warn("Problem checking bundle {} for Log4j 2 provider.", bundle.getSymbolicName(), e);
        }
    }

    @Override
    public void start(final BundleContext context) throws Exception {
        context.addBundleListener(this);
        final Bundle[] bundles = context.getBundles();
        for (final Bundle bundle : bundles) {
            loadProvider(bundle);
        }
    }

    @Override
    public void stop(final BundleContext context) throws Exception {

    }

    @Override
    public void bundleChanged(final BundleEvent event) {
        switch (event.getType()) {
            case BundleEvent.STARTED:
                // FIXME: LogManager won't see this update if it happens after LogManager is loaded
                loadProvider(event.getBundle());
                break;

            default:
                break;
        }
    }

    /**
     * ClassLoader that delegates to an OSGi Bundle.
     */
    public static class BundleDelegatingClassLoader extends ClassLoader {

        private final Bundle bundle;

        public BundleDelegatingClassLoader(final Bundle bundle) {
            this.bundle = bundle;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            return bundle.loadClass(name);
        }

        @Override
        protected URL findResource(String name) {
            return bundle.getResource(name);
        }

        @Override
        protected Enumeration<URL> findResources(String name) throws IOException {
            return bundle.getResources(name);
        }
    }
}
