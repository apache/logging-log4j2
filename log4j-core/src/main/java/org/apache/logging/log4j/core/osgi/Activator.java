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
package org.apache.logging.log4j.core.osgi;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.util.PluginRegistry;
import org.apache.logging.log4j.core.impl.Log4jProvider;
import org.apache.logging.log4j.core.impl.ThreadContextDataInjector;
import org.apache.logging.log4j.core.impl.ThreadContextDataProvider;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.ContextDataProvider;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.ProviderActivator;
import org.osgi.annotation.bundle.Header;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.wiring.BundleWiring;

/**
 * OSGi BundleActivator.
 */
@Header(name = org.osgi.framework.Constants.BUNDLE_ACTIVATOR, value = "${@class}")
@Header(
        name = org.osgi.framework.Constants.BUNDLE_ACTIVATIONPOLICY,
        value = org.osgi.framework.Constants.ACTIVATION_LAZY)
public final class Activator extends ProviderActivator implements SynchronousBundleListener {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private final AtomicReference<BundleContext> contextRef = new AtomicReference<>();

    private ServiceRegistration<ContextDataProvider> contextDataRegistration = null;

    public Activator() {
        super(new Log4jProvider());
    }

    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);
        final ContextDataProvider threadContextProvider = new ThreadContextDataProvider();
        contextDataRegistration = context.registerService(ContextDataProvider.class, threadContextProvider, null);
        loadContextProviders(context);
        // allow the user to override the default ContextSelector (e.g., by using BasicContextSelector for a global cfg)
        if (PropertiesUtil.getProperties().getStringProperty(Constants.LOG4J_CONTEXT_SELECTOR) == null) {
            System.setProperty(Constants.LOG4J_CONTEXT_SELECTOR, BundleContextSelector.class.getName());
        }
        if (this.contextRef.compareAndSet(null, context)) {
            context.addBundleListener(this);
            // done after the BundleListener as to not miss any new bundle installs in the interim
            scanInstalledBundlesForPlugins(context);
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
            PluginRegistry.getInstance()
                    .loadFromBundle(bundleId, bundle.adapt(BundleWiring.class).getClassLoader());
        }
    }

    private static void loadContextProviders(final BundleContext bundleContext) {
        try {
            final Collection<ServiceReference<ContextDataProvider>> serviceReferences =
                    bundleContext.getServiceReferences(ContextDataProvider.class, null);
            for (final ServiceReference<ContextDataProvider> serviceReference : serviceReferences) {
                final ContextDataProvider provider = bundleContext.getService(serviceReference);
                ThreadContextDataInjector.contextDataProviders.add(provider);
            }
        } catch (final InvalidSyntaxException ex) {
            LOGGER.error("Error accessing context data provider", ex);
        }
    }

    private static void stopBundlePlugins(final Bundle bundle) {
        LOGGER.trace("Stopping bundle [{}] plugins.", bundle.getSymbolicName());
        // TODO: plugin lifecycle code
        PluginRegistry.getInstance().clearBundlePlugins(bundle.getBundleId());
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        contextDataRegistration.unregister();
        this.contextRef.compareAndSet(context, null);
        LogManager.shutdown();
        super.stop(context);
    }

    @Override
    public void bundleChanged(final BundleEvent event) {
        switch (event.getType()) {
                // FIXME: STARTING instead of STARTED?
            case BundleEvent.STARTED:
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
