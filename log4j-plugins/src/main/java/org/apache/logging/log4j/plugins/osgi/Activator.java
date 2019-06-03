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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.plugins.processor.PluginService;
import org.apache.logging.log4j.plugins.util.PluginRegistry;
import org.apache.logging.log4j.spi.Provider;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.osgi.framework.*;
import org.osgi.framework.wiring.BundleWiring;

import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * OSGi BundleActivator.
 */
public final class Activator implements BundleActivator, SynchronousBundleListener {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private final AtomicReference<BundleContext> contextRef = new AtomicReference<>();

    ServiceRegistration provideRegistration = null;

    @Override
    public void start(final BundleContext context) throws Exception { /*
        final PluginService pluginService = new Log4jProvider();
        final Hashtable<String, String> props = new Hashtable<>();
        props.put("APIVersion", "3.0");
        provideRegistration = context.registerService(pluginService.class.getName(), provider, props);
        if (this.contextRef.compareAndSet(null, context)) {
            context.addBundleListener(this);
            // done after the BundleListener as to not miss any new bundle installs in the interim
            scanInstalledBundlesForPlugins(context);
        } */
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
        provideRegistration.unregister();
        this.contextRef.compareAndSet(context, null);
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
