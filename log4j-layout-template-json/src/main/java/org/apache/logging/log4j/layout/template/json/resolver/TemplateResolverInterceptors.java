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
import org.apache.logging.log4j.plugins.util.TypeUtil;
import org.apache.logging.log4j.status.StatusLogger;

import java.util.LinkedList;
import java.util.List;

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
            final Configuration configuration,
            final Class<V> valueClass,
            final Class<C> contextClass) {

        // Populate interceptors.
        final PluginCategory interceptorPlugins =
                configuration.getComponent(TemplateResolverInterceptor.PLUGIN_CATEGORY_KEY);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                    "found {} plugins of category \"{}\": {}",
                    interceptorPlugins.size(),
                    TemplateResolverInterceptor.CATEGORY,
                    interceptorPlugins.getPluginKeys());
        }

        // Filter matching interceptors.
        final List<I> interceptors =
                populateInterceptors(interceptorPlugins, configuration, valueClass, contextClass);
        LOGGER.debug(
                "{} interceptors matched out of {} for value class {} and context class {}",
                interceptors.size(),
                interceptorPlugins.size(),
                valueClass,
                contextClass);
        return interceptors;

    }

    private static <V, C extends TemplateResolverContext<V, C>, I extends TemplateResolverInterceptor<V, C>> List<I> populateInterceptors(
            final PluginCategory interceptorPlugins,
            final Configuration configuration,
            final Class<V> valueClass,
            final Class<C> contextClass) {
        final List<I> interceptors = new LinkedList<>();
        interceptorPlugins.forEachMatching(
                pluginType -> pluginType.getImplementedInterfaces().contains(TemplateResolverInterceptor.class),
                pluginType -> {
                    @SuppressWarnings("rawtypes")
                    final Class<? extends TemplateResolverInterceptor> interceptorClass =
                            pluginType.getPluginClass().asSubclass(TemplateResolverInterceptor.class);
                    final TemplateResolverInterceptor<?, ?> rawInterceptor =
                            configuration.getComponent(Key.forClass(interceptorClass));
                    final I interceptor =
                            castInterceptor(valueClass, contextClass, rawInterceptor);
                    if (interceptor != null) {
                        interceptors.add(interceptor);
                    }
                }
        );
        return interceptors;
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
            return TypeUtil.cast(interceptor);
        }
        return null;
    }

}
