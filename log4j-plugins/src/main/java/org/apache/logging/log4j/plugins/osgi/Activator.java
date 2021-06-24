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

package org.apache.logging.log4j.plugins.osgi;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.plugins.processor.PluginService;
import org.apache.logging.log4j.plugins.util.PluginRegistry;
import org.apache.logging.log4j.status.StatusLogger;
import org.osgi.framework.*;
import org.osgi.framework.wiring.BundleWiring;

import java.security.Permission;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

/**
 * OSGi BundleActivator.
 */
public final class Activator implements BundleActivator, SynchronousBundleListener {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static final SecurityManager SECURITY_MANAGER = System.getSecurityManager();

    private final AtomicReference<BundleContext> contextRef = new AtomicReference<>();

    @Override
    public void start(final BundleContext bundleContext) throws Exception {
        loadPlugins(bundleContext);
        bundleContext.addBundleListener(this);
        final Bundle[] bundles = bundleContext.getBundles();
        for (final Bundle bundle : bundles) {
            loadPlugins(bundle);
        }
        scanInstalledBundlesForPlugins(bundleContext);
        this.contextRef.compareAndSet(null, bundleContext);
    }

    private void loadPlugins(final BundleContext bundleContext) {
        try {
            final Collection<ServiceReference<PluginService>> serviceReferences = bundleContext.getServiceReferences(PluginService.class, null);
            for (final ServiceReference<PluginService> serviceReference : serviceReferences) {
                final PluginService pluginService = bundleContext.getService(serviceReference);
                PluginRegistry.getInstance().loadFromBundle(pluginService.getCategories(), bundleContext.getBundle().getBundleId());
            }
        } catch (final InvalidSyntaxException ex) {
            LOGGER.error("Error accessing Plugins", ex);
        }
    }

    private void loadPlugins(final Bundle bundle) {
        if (bundle.getState() == Bundle.UNINSTALLED) {
            return;
        }
        try {
            checkPermission(new AdminPermission(bundle, AdminPermission.RESOURCE));
            checkPermission(new AdaptPermission(BundleWiring.class.getName(), bundle, AdaptPermission.ADAPT));
            final BundleContext bundleContext = bundle.getBundleContext();
            if (bundleContext == null) {
                LOGGER.debug("Bundle {} has no context (state={}), skipping loading plugins", bundle.getSymbolicName(), toStateString(bundle.getState()));
            } else {
                loadPlugins(bundleContext);
            }
        } catch (final SecurityException e) {
            LOGGER.debug("Cannot access bundle [{}] contents. Ignoring.", bundle.getSymbolicName(), e);
        } catch (final Exception e) {
            LOGGER.warn("Problem checking bundle {} for Log4j 2 provider.", bundle.getSymbolicName(), e);
        }
    }

    private static void checkPermission(final Permission permission) {
        if (SECURITY_MANAGER != null) {
            SECURITY_MANAGER.checkPermission(permission);
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

    private static void scanInstalledBundlesForPlugins(final BundleContext context) {
        final Bundle[] bundles = context.getBundles();
        for (final Bundle bundle : bundles) {
            // TODO: bundle state can change during this
            scanBundleForPlugins(bundle);
        }
    }

    private static void scanBundleForPlugins(final Bundle bundle) {
        final long bundleId = bundle.getBundleId();
        // LOG4J2-920: don't scan system bundle for plugins
        if (bundle.getState() == Bundle.ACTIVE && bundleId != 0) {
            LOGGER.trace("Scanning bundle [{}, id=%d] for plugins.", bundle.getSymbolicName(), bundleId);
            PluginRegistry.getInstance().loadFromBundle(bundleId,
                    bundle.adapt(BundleWiring.class).getClassLoader());
        }
    }

    private static void stopBundlePlugins(final Bundle bundle) {
        LOGGER.trace("Stopping bundle [{}] plugins.", bundle.getSymbolicName());
        // TODO: plugin lifecycle code
        PluginRegistry.getInstance().clearBundlePlugins(bundle.getBundleId());
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        final Bundle[] bundles = context.getBundles();
        for (final Bundle bundle : bundles) {
            stopBundlePlugins(bundle);
        }
        stopBundlePlugins(context.getBundle());
        this.contextRef.compareAndSet(context, null);
    }

    @Override
    public void bundleChanged(final BundleEvent event) {
        switch (event.getType()) {
            // FIXME: STARTING instead of STARTED?
            case BundleEvent.STARTED:
                loadPlugins(event.getBundle());
                scanBundleForPlugins(event.getBundle());
                break;

            case BundleEvent.STOPPING:
                stopBundlePlugins(event.getBundle());
                break;

            default:
                break;
        }
    }
}
