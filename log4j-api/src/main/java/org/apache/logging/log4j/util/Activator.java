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

import java.net.URL;
import java.security.Permission;
import java.util.Collection;
import java.util.List;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.apache.logging.log4j.spi.Provider;
import org.apache.logging.log4j.status.StatusLogger;
import org.osgi.annotation.bundle.Header;
import org.osgi.framework.AdaptPermission;
import org.osgi.framework.AdminPermission;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

/**
 * <em>Consider this class private.</em>
 * OSGi bundle activator. Used for locating an implementation of
 * {@link org.apache.logging.log4j.spi.LoggerContextFactory} et al. that have corresponding
 * {@code META-INF/log4j-provider.properties} files. As with all OSGi BundleActivator classes, this class is not for
 * public use and is only useful in an OSGi framework environment.
 */
@Header(name = Constants.BUNDLE_ACTIVATOR, value = "${@class}")
@Header(name = Constants.BUNDLE_ACTIVATIONPOLICY, value = Constants.ACTIVATION_LAZY)
@InternalApi
public class Activator implements BundleActivator, SynchronousBundleListener {

    private static final SecurityManager SECURITY_MANAGER = System.getSecurityManager();

    private static final Logger LOGGER = StatusLogger.getLogger();

    // until we have at least one Provider, we'll lock ProviderUtil which locks LogManager.<clinit> by extension.
    // this variable needs to be reset once the lock has been released
    private boolean lockingProviderUtil;

    private static void checkPermission(final Permission permission) {
        if (SECURITY_MANAGER != null) {
            SECURITY_MANAGER.checkPermission(permission);
        }
    }

    private void loadProvider(final Bundle bundle) {
        if (bundle.getState() == Bundle.UNINSTALLED) {
            return;
        }
        try {
            checkPermission(new AdminPermission(bundle, AdminPermission.RESOURCE));
            checkPermission(new AdaptPermission(BundleWiring.class.getName(), bundle, AdaptPermission.ADAPT));
            final BundleContext bundleContext = bundle.getBundleContext();
            if (bundleContext == null) {
                LOGGER.debug(
                        "Bundle {} has no context (state={}), skipping loading provider",
                        bundle.getSymbolicName(),
                        toStateString(bundle.getState()));
            } else {
                loadProvider(bundleContext, bundle.adapt(BundleWiring.class));
            }
        } catch (final SecurityException e) {
            LOGGER.debug("Cannot access bundle [{}] contents. Ignoring.", bundle.getSymbolicName(), e);
        } catch (final Exception e) {
            LOGGER.warn("Problem checking bundle {} for Log4j 2 provider.", bundle.getSymbolicName(), e);
        }
    }

    private String toStateString(final int state) {
        switch (state) {
            case Bundle.UNINSTALLED:
                return "UNINSTALLED";
            case Bundle.INSTALLED:
                return "INSTALLED";
            case Bundle.RESOLVED:
                return "RESOLVED";
            case Bundle.STARTING:
                return "STARTING";
            case Bundle.STOPPING:
                return "STOPPING";
            case Bundle.ACTIVE:
                return "ACTIVE";
            default:
                return Integer.toString(state);
        }
    }

    private void loadProvider(final BundleContext bundleContext, final BundleWiring bundleWiring) {
        final String filter = "(APIVersion>=2.6.0)";
        try {
            final Collection<ServiceReference<Provider>> serviceReferences =
                    bundleContext.getServiceReferences(Provider.class, filter);
            Provider maxProvider = null;
            for (final ServiceReference<Provider> serviceReference : serviceReferences) {
                final Provider provider = bundleContext.getService(serviceReference);
                if (maxProvider == null || provider.getPriority() > maxProvider.getPriority()) {
                    maxProvider = provider;
                }
            }
            if (maxProvider != null) {
                ProviderUtil.addProvider(maxProvider);
            }
        } catch (final InvalidSyntaxException ex) {
            LOGGER.error("Invalid service filter: " + filter, ex);
        }
        final List<URL> urls = bundleWiring.findEntries("META-INF", "log4j-provider.properties", 0);
        for (final URL url : urls) {
            ProviderUtil.loadProvider(url, bundleWiring.getClassLoader());
        }
    }

    @Override
    public void start(final BundleContext bundleContext) throws Exception {
        ProviderUtil.STARTUP_LOCK.lock();
        lockingProviderUtil = true;
        final BundleWiring self = bundleContext.getBundle().adapt(BundleWiring.class);
        final List<BundleWire> required = self.getRequiredWires(LoggerContextFactory.class.getName());
        for (final BundleWire wire : required) {
            loadProvider(bundleContext, wire.getProviderWiring());
        }
        bundleContext.addBundleListener(this);
        final Bundle[] bundles = bundleContext.getBundles();
        for (final Bundle bundle : bundles) {
            loadProvider(bundle);
        }
        unlockIfReady();
    }

    private void unlockIfReady() {
        if (lockingProviderUtil && !ProviderUtil.PROVIDERS.isEmpty()) {
            ProviderUtil.STARTUP_LOCK.unlock();
            lockingProviderUtil = false;
        }
    }

    @Override
    public void stop(final BundleContext bundleContext) throws Exception {
        bundleContext.removeBundleListener(this);
        unlockIfReady();
    }

    @Override
    public void bundleChanged(final BundleEvent event) {
        switch (event.getType()) {
            case BundleEvent.STARTED:
                loadProvider(event.getBundle());
                unlockIfReady();
                break;

            default:
                break;
        }
    }
}
