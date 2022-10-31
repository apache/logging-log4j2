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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.LegacyLoggingSystemProvider;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.apache.logging.log4j.spi.LoggingSystemProvider;
import org.apache.logging.log4j.status.StatusLogger;
import org.osgi.framework.AdaptPermission;
import org.osgi.framework.AdminPermission;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

import java.io.IOException;
import java.net.URL;
import java.security.Permission;
import java.util.List;

/**
 * <em>Consider this class private.</em>
 * OSGi bundle activator. Used for locating an implementation of
 * {@link org.apache.logging.log4j.spi.LoggerContextFactory} et al. that have corresponding
 * {@code META-INF/log4j-provider.properties} files. As with all OSGi BundleActivator classes, this class is not for
 * public use and is only useful in an OSGi framework environment.
 */
public class Activator implements BundleActivator, SynchronousBundleListener {

    private static final SecurityManager SECURITY_MANAGER = System.getSecurityManager();

    private static final Logger LOGGER = StatusLogger.getLogger();

    // until we have at least one LoggingSystemProvider, we'll acquire an initialization lock on LoggingSystem
    // which in turn locks LogManager and ThreadContext internally
    // this variable needs to be reset once the lock has been released
    private boolean acquiredLoggingSystemInitializationLock;
    private LoggingSystem loggingSystem;
    private ServiceRegistry serviceRegistry;

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
                LOGGER.debug("Bundle {} has no context (state={}), skipping loading provider", bundle.getSymbolicName(), toStateString(bundle.getState()));
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
        final ClassLoader classLoader = bundleWiring.getClassLoader();
        final long bundleId = bundleContext.getBundle().getBundleId();
        serviceRegistry.loadServicesFromBundle(LoggingSystemProvider.class, bundleId, classLoader);
        for (final URL url : bundleWiring.findEntries("META-INF", "log4j-provider.properties", 0)) {
            try {
                serviceRegistry.registerBundleServices(LoggingSystemProvider.class, bundleId,
                        List.of(new LegacyLoggingSystemProvider<>(url, classLoader)));
            } catch (final IOException e) {
                LOGGER.error("Unable to load {}", url, e);
            }
        }
    }

    @Override
    public void start(final BundleContext bundleContext) throws Exception {
        serviceRegistry = ServiceRegistry.getInstance();
        loggingSystem = LoggingSystem.getInstance();
        loggingSystem.acquireInitializationLock();
        acquiredLoggingSystemInitializationLock = true;
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
        if (acquiredLoggingSystemInitializationLock && loggingSystem.hasProvider()) {
            loggingSystem.releaseInitializationLock();
            acquiredLoggingSystemInitializationLock = false;
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

            case BundleEvent.STOPPED:
                serviceRegistry.unregisterBundleServices(event.getBundle().getBundleId());
                break;

            default:
                break;
        }
    }

}
