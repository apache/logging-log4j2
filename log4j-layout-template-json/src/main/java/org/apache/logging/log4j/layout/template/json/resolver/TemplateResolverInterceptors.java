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
import org.apache.logging.log4j.core.config.plugins.util.PluginType;
import org.apache.logging.log4j.core.config.plugins.util.PluginUtil;
import org.apache.logging.log4j.status.StatusLogger;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for {@link TemplateResolverInterceptor}.
 */
public class TemplateResolverInterceptors {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private TemplateResolverInterceptors() {}

    /**
     * Populates plugins implementing
     * {@link TemplateResolverInterceptor TemplateResolverInterceptor&lt;V, C&gt;},
     * where {@code V} and {@code C} denote the value and context class types,
     * respectively.
     */
    public static <V, C extends TemplateResolverContext<V, C>, I extends TemplateResolverInterceptor<V, C>> List<I> populateInterceptors(
            final List<String> pluginPackages,
            final Class<V> valueClass,
            final Class<C> contextClass) {

        // Populate interceptors.
        final Map<String, PluginType<?>> pluginTypeByName =
                PluginUtil.collectPluginsByCategoryAndPackage(
                        TemplateResolverInterceptor.CATEGORY,
                        pluginPackages);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                    "found {} plugins of category \"{}\": {}",
                    pluginTypeByName.size(),
                    TemplateResolverFactory.CATEGORY,
                    pluginTypeByName.keySet());
        }

        // Filter matching interceptors.
        final List<I> interceptors =
                populateInterceptors(pluginTypeByName, valueClass, contextClass);
        LOGGER.debug(
                "{} interceptors matched out of {} for value class {} and context class {}",
                interceptors.size(),
                pluginTypeByName.size(),
                valueClass,
                contextClass);
        return interceptors;

    }

    private static <V, C extends TemplateResolverContext<V, C>, I extends TemplateResolverInterceptor<V, C>> List<I> populateInterceptors(
            final Map<String, PluginType<?>> pluginTypeByName,
            final Class<V> valueClass,
            final Class<C> contextClass) {
        final List<I> interceptors = new LinkedList<>();
        final Set<String> pluginNames = pluginTypeByName.keySet();
        for (final String pluginName : pluginNames) {
            final PluginType<?> pluginType = pluginTypeByName.get(pluginName);
            final Class<?> pluginClass = pluginType.getPluginClass();
            final boolean pluginClassMatched =
                    TemplateResolverInterceptor.class.isAssignableFrom(pluginClass);
            if (pluginClassMatched) {
                final TemplateResolverInterceptor<?, ?> rawInterceptor =
                        instantiateInterceptor(pluginName, pluginClass);
                final I interceptor =
                        castInterceptor(valueClass, contextClass, rawInterceptor);
                if (interceptor != null) {
                    interceptors.add(interceptor);
                }
            }
        }
        return interceptors;
    }

    private static TemplateResolverInterceptor<?, ?> instantiateInterceptor(
            final String pluginName,
            final Class<?> pluginClass) {
        try {
            return (TemplateResolverInterceptor<?, ?>)
                    PluginUtil.instantiatePlugin(pluginClass);
        } catch (final Exception error) {
            final String message = String.format(
                    "failed instantiating resolver interceptor plugin %s of name %s",
                    pluginClass, pluginName);
            throw new RuntimeException(message, error);
        }
    }

    private static <V, C extends TemplateResolverContext<V, C>, I extends TemplateResolverInterceptor<V, C>> I castInterceptor(
            final Class<V> valueClass,
            final Class<C> contextClass,
            final TemplateResolverInterceptor<?, ?> interceptor) {
        final Class<?> interceptorValueClass = interceptor.getValueClass();
        final Class<?> interceptorContextClass = interceptor.getContextClass();
        final boolean interceptorValueClassMatched =
                valueClass.isAssignableFrom(interceptorValueClass);
        final boolean interceptorContextClassMatched =
                contextClass.isAssignableFrom(interceptorContextClass);
        if (interceptorValueClassMatched && interceptorContextClassMatched) {
            @SuppressWarnings({"unchecked"})
            final I typedInterceptor = (I) interceptor;
            return typedInterceptor;
        }
        return null;
    }

}
