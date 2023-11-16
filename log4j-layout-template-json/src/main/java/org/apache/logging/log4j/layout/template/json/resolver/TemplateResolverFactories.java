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
package org.apache.logging.log4j.layout.template.json.resolver;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.util.PluginType;
import org.apache.logging.log4j.core.config.plugins.util.PluginUtil;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Utility class for {@link TemplateResolverFactory}.
 */
public final class TemplateResolverFactories {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private TemplateResolverFactories() {}

    /**
     * Populates plugins implementing
     * {@link TemplateResolverFactory TemplateResolverFactory&lt;V, C&gt;},
     * where {@code V} and {@code C} denote the value and context class types,
     * respectively.
     */
    public static <V, C extends TemplateResolverContext<V, C>, F extends TemplateResolverFactory<V, C>>
            Map<String, F> populateFactoryByName(
                    final List<String> pluginPackages, final Class<V> valueClass, final Class<C> contextClass) {

        // Populate template resolver factories.
        final Map<String, PluginType<?>> pluginTypeByName =
                PluginUtil.collectPluginsByCategoryAndPackage(TemplateResolverFactory.CATEGORY, pluginPackages);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                    "found {} plugins of category \"{}\": {}",
                    pluginTypeByName.size(),
                    TemplateResolverFactory.CATEGORY,
                    pluginTypeByName.keySet());
        }

        // Filter matching resolver factories.
        final Map<String, F> factoryByName = populateFactoryByName(pluginTypeByName, valueClass, contextClass);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                    "matched {} resolver factories out of {} for value class {} and context class {}: {}",
                    factoryByName.size(),
                    pluginTypeByName.size(),
                    valueClass,
                    contextClass,
                    factoryByName.keySet());
        }
        return factoryByName;
    }

    private static <V, C extends TemplateResolverContext<V, C>, F extends TemplateResolverFactory<V, C>>
            Map<String, F> populateFactoryByName(
                    final Map<String, PluginType<?>> pluginTypeByName,
                    final Class<V> valueClass,
                    final Class<C> contextClass) {
        final Map<String, F> factoryByName = new LinkedHashMap<>();
        final Set<String> pluginNames = pluginTypeByName.keySet();
        for (final String pluginName : pluginNames) {
            final PluginType<?> pluginType = pluginTypeByName.get(pluginName);
            final Class<?> pluginClass = pluginType.getPluginClass();
            final boolean pluginClassMatched = TemplateResolverFactory.class.isAssignableFrom(pluginClass);
            if (pluginClassMatched) {
                final TemplateResolverFactory<?, ?> rawFactory = instantiateFactory(pluginName, pluginClass);
                final F factory = castFactory(valueClass, contextClass, rawFactory);
                if (factory != null) {
                    addFactory(factoryByName, factory);
                }
            }
        }
        return factoryByName;
    }

    private static TemplateResolverFactory<?, ?> instantiateFactory(
            final String pluginName, final Class<?> pluginClass) {
        try {
            return (TemplateResolverFactory<?, ?>) PluginUtil.instantiatePlugin(pluginClass);
        } catch (final Exception error) {
            final String message = String.format(
                    "failed instantiating resolver factory plugin %s of name %s", pluginClass, pluginName);
            throw new RuntimeException(message, error);
        }
    }

    private static <V, C extends TemplateResolverContext<V, C>, F extends TemplateResolverFactory<V, C>> F castFactory(
            final Class<V> valueClass, final Class<C> contextClass, final TemplateResolverFactory<?, ?> factory) {
        final Class<?> factoryValueClass = factory.getValueClass();
        final Class<?> factoryContextClass = factory.getContextClass();
        final boolean factoryValueClassMatched = valueClass.isAssignableFrom(factoryValueClass);
        final boolean factoryContextClassMatched = contextClass.isAssignableFrom(factoryContextClass);
        if (factoryValueClassMatched && factoryContextClassMatched) {
            @SuppressWarnings("unchecked")
            final F typedFactory = (F) factory;
            return typedFactory;
        }
        return null;
    }

    private static <V, C extends TemplateResolverContext<V, C>, F extends TemplateResolverFactory<V, C>>
            void addFactory(final Map<String, F> factoryByName, final F factory) {
        final String factoryName = factory.getName();
        final F conflictingFactory = factoryByName.putIfAbsent(factoryName, factory);
        if (conflictingFactory != null) {
            final String message = String.format(
                    "found resolver factories with overlapping names: %s (%s and %s)",
                    factoryName, conflictingFactory, factory);
            throw new IllegalArgumentException(message);
        }
    }
}
