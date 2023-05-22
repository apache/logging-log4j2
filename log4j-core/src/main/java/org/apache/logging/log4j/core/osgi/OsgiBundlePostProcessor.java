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

import java.util.Hashtable;
import java.util.List;

import org.apache.logging.log4j.core.impl.Log4jPropertyKey;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.core.util.ContextDataProvider;
import org.apache.logging.log4j.plugins.di.Binding;
import org.apache.logging.log4j.plugins.di.ConfigurableInstanceFactory;
import org.apache.logging.log4j.plugins.di.spi.ConfigurableInstanceFactoryPostProcessor;
import org.apache.logging.log4j.plugins.model.PluginRegistry;
import org.apache.logging.log4j.plugins.model.PluginService;
import org.apache.logging.log4j.util.ServiceRegistry;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;

/**
 * Post-processor for OSGi environments for scanning for plugins from other bundles.
 */
public class OsgiBundlePostProcessor implements ConfigurableInstanceFactoryPostProcessor, AutoCloseable {
    private final BundleContext context;
    private final ServiceRegistration<PluginRegistry> pluginRegistryServiceRegistration;

    public OsgiBundlePostProcessor(final BundleContext context) {
        this.context = context;
        pluginRegistryServiceRegistration = context.registerService(
                PluginRegistry.class, new PluginRegistry(), new Hashtable<>());
        final Bundle bundle = context.getBundle();
        final ClassLoader classLoader = bundle.adapt(BundleWiring.class).getClassLoader();
        final long bundleId = bundle.getBundleId();
        final ServiceRegistry registry = ServiceRegistry.getInstance();
        registry.registerBundleServices(ConfigurableInstanceFactoryPostProcessor.class, bundleId, List.of(this));
        registry.loadServicesFromBundle(PluginService.class, bundleId, classLoader);
        registry.loadServicesFromBundle(ContextDataProvider.class, bundleId, classLoader);
        registry.loadServicesFromBundle(ConfigurableInstanceFactoryPostProcessor.class, bundleId, classLoader);
    }


    @Override
    public void postProcessFactory(final ConfigurableInstanceFactory factory) {
        factory.registerBinding(Binding.from(PluginRegistry.class)
                .to(() -> context.getService(pluginRegistryServiceRegistration.getReference())));
        final String customContextSelector = context.getProperty(Log4jPropertyKey.CONTEXT_SELECTOR_CLASS_NAME.getKey());
        if (customContextSelector == null) {
            factory.registerBinding(Binding.from(ContextSelector.KEY).toInstance(new BundleContextSelector(factory)));
        }
    }

    @Override
    public void close() throws Exception {
        pluginRegistryServiceRegistration.unregister();
    }
}
