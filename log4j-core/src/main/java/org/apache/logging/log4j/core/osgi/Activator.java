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

package org.apache.logging.log4j.core.osgi;

import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.util.PluginRegistry;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.wiring.BundleWiring;

/**
 * OSGi BundleActivator.
 */
public final class Activator implements BundleActivator, SynchronousBundleListener {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private final AtomicReference<BundleContext> contextRef = new AtomicReference<>();

    @Override
    public void start(final BundleContext context) throws Exception {
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
            // LOG4J2-920: don't scan system bundle for plugins
            if (bundle.getState() == Bundle.ACTIVE && bundle.getBundleId() != 0) {
                // TODO: bundle state can change during this
                scanBundleForPlugins(bundle);
            }
        }
    }

    private static void scanBundleForPlugins(final Bundle bundle) {
        LOGGER.trace("Scanning bundle [{}] for plugins.", bundle.getSymbolicName());
        PluginRegistry.getInstance().loadFromBundle(bundle.getBundleId(),
            bundle.adapt(BundleWiring.class).getClassLoader());
    }

    private static void stopBundlePlugins(final Bundle bundle) {
        LOGGER.trace("Stopping bundle [{}] plugins.", bundle.getSymbolicName());
        // TODO: plugin lifecycle code
        PluginRegistry.getInstance().clearBundlePlugins(bundle.getBundleId());
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        this.contextRef.compareAndSet(context, null);
        LogManager.shutdown();
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
