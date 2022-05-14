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
package org.apache.logging.log4j.layout.template.json.resolver;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.plugins.util.PluginCategory;
import org.apache.logging.log4j.plugins.util.PluginType;
import org.apache.logging.log4j.plugins.util.TypeUtil;
import org.apache.logging.log4j.status.StatusLogger;

import java.util.LinkedHashMap;
import java.util.Map;

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
    public static <V, C extends TemplateResolverContext<V, C>, F extends TemplateResolverFactory<V, C>> Map<String, F> populateFactoryByName(
            final Configuration configuration,
            final Class<V> valueClass,
            final Class<C> contextClass) {

        // Populate template resolver factories.
        final PluginCategory factoryPlugins =
                configuration.getComponent(TemplateResolverFactory.PLUGIN_CATEGORY_KEY);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                    "found {} plugins of category \"{}\": {}",
                    factoryPlugins.size(),
                    TemplateResolverFactory.CATEGORY,
                    factoryPlugins.getPluginKeys());
        }

        // Filter matching resolver factories.
        final Map<String, F> factoryByName =
                populateFactoryByName(factoryPlugins, configuration, valueClass, contextClass);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                    "matched {} resolver factories out of {} for value class {} and context class {}: {}",
                    factoryByName.size(),
                    factoryPlugins.size(),
                    valueClass,
                    contextClass,
                    factoryByName.keySet());
        }
        return factoryByName;

    }

    private static <V, C extends TemplateResolverContext<V, C>, F extends TemplateResolverFactory<V, C>> Map<String, F> populateFactoryByName(
            final PluginCategory factoryPlugins,
            final Configuration configuration,
            final Class<V> valueClass,
            final Class<C> contextClass) {
        final Map<String, F> factoryByName = new LinkedHashMap<>();
        for (final PluginType<?> pluginType : factoryPlugins) {
            final Class<?> pluginClass = pluginType.getPluginClass();
            final boolean pluginClassMatched =
                    TemplateResolverFactory.class.isAssignableFrom(pluginClass);
            if (pluginClassMatched) {
                @SuppressWarnings("rawtypes")
                final Class<? extends TemplateResolverFactory> factoryClass =
                        pluginClass.asSubclass(TemplateResolverFactory.class);
                final TemplateResolverFactory<?, ?> rawFactory =
                        configuration.getComponent(Key.forClass(factoryClass));
                final F factory = castFactory(valueClass, contextClass, rawFactory);
                if (factory != null) {
                    addFactory(factoryByName, factory);
                }
            }
        }
        return factoryByName;
    }

    private static <V, C extends TemplateResolverContext<V, C>, F extends TemplateResolverFactory<V, C>> F castFactory(
            final Class<V> valueClass,
            final Class<C> contextClass,
            final TemplateResolverFactory<?, ?> factory) {
        final Class<?> factoryValueClass = factory.getValueClass();
        final Class<?> factoryContextClass = factory.getContextClass();
        final boolean factoryValueClassMatched =
                valueClass.isAssignableFrom(factoryValueClass);
        final boolean factoryContextClassMatched =
                contextClass.isAssignableFrom(factoryContextClass);
        if (factoryValueClassMatched && factoryContextClassMatched) {
            return TypeUtil.cast(factory);
        }
        return null;
    }

    private static <V, C extends TemplateResolverContext<V, C>, F extends TemplateResolverFactory<V, C>> void addFactory(
            final Map<String, F> factoryByName,
            final F factory) {
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
