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

import java.security.Permission;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.plugins.model.PluginRegistry;
import org.apache.logging.log4j.plugins.model.PluginService;
import org.apache.logging.log4j.status.StatusLogger;
import org.osgi.framework.AdaptPermission;
import org.osgi.framework.AdminPermission;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.wiring.BundleWiring;

/**
 * OSGi BundleActivator.
 */
public final class Activator implements BundleActivator, SynchronousBundleListener {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static final SecurityManager SECURITY_MANAGER = System.getSecurityManager();
    public static final String CORE_MODULE_NAME = "org.apache.logging.log4j.core";

    private final AtomicReference<BundleContext> contextRef = new AtomicReference<>();

    private int state = Bundle.UNINSTALLED;

    private ServiceReference<PluginRegistry> pluginRegistryServiceReference;
    private PluginRegistry pluginRegistry;

    @Override
    public void start(final BundleContext bundleContext) throws Exception {
        state = Bundle.STARTING;
        bundleContext.addBundleListener(this);
        this.contextRef.compareAndSet(null, bundleContext);
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
                final Collection<ServiceReference<PluginService>> serviceReferences =
                        bundleContext.getServiceReferences(PluginService.class, null);
                for (final ServiceReference<PluginService> serviceReference : serviceReferences) {
                    final PluginService pluginService = bundleContext.getService(serviceReference);
                    pluginRegistry.loadFromBundle(bundleContext.getBundle().getBundleId(), pluginService.getNamespaces());
                }
            }
        } catch (final SecurityException e) {
            LOGGER.debug("Cannot access bundle [{}] contents. Ignoring.", bundle.getSymbolicName(), e);
        } catch (final InvalidSyntaxException ex) {
            LOGGER.error("Error accessing Plugins", ex);
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

    private void scanInstalledBundlesForPlugins(final BundleContext context) {
        final Bundle[] bundles = context.getBundles();
        for (final Bundle bundle : bundles) {
            scanBundleForPlugins(bundle);
        }
    }

    private void scanBundleForPlugins(final Bundle bundle) {
        final long bundleId = bundle.getBundleId();
        // LOG4J2-920: don't scan system bundle for plugins
        if (bundle.getState() == Bundle.ACTIVE && bundleId != 0) {
            LOGGER.trace("Scanning bundle [{}, id={}] for plugins.", bundle.getSymbolicName(), bundleId);
            final ClassLoader classLoader = bundle.adapt(BundleWiring.class).getClassLoader();
            pluginRegistry.loadFromBundle(bundleId, classLoader);
        }
    }

    private void stopBundlePlugins(final Bundle bundle) {
        LOGGER.trace("Stopping bundle [{}] plugins.", bundle.getSymbolicName());
        if (pluginRegistry != null) {
            pluginRegistry.clearBundlePlugins(bundle.getBundleId());
        }
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
        final Bundle bundle = event.getBundle();
        switch (event.getType()) {
            case BundleEvent.STARTING:
                if (CORE_MODULE_NAME.equals(bundle.getSymbolicName()) && state != Bundle.ACTIVE) {
                    break;
                }
                break;

            case BundleEvent.STARTED:
                if (CORE_MODULE_NAME.equals(bundle.getSymbolicName()) && state != Bundle.ACTIVE) {
                    final BundleContext bundleContext = contextRef.get();
                    pluginRegistryServiceReference =
                            bundleContext.getServiceReference(PluginRegistry.class);
                    pluginRegistry = bundleContext.getService(pluginRegistryServiceReference);
                    scanInstalledBundlesForPlugins(bundleContext);
                    state = Bundle.ACTIVE;
                } else if (state == Bundle.ACTIVE) {
                    loadPlugins(bundle);
                    scanBundleForPlugins(bundle);
                }

            case BundleEvent.STOPPING:
                if (CORE_MODULE_NAME.equals(bundle.getSymbolicName()) && pluginRegistry != null) {
                    pluginRegistry = null;
                    contextRef.get().ungetService(pluginRegistryServiceReference);
                }
                stopBundlePlugins(bundle);
                break;

            default:
                break;
        }
    }
}
