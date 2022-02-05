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
package org.apache.log4j.builders;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.builders.appender.AppenderBuilder;
import org.apache.log4j.builders.filter.FilterBuilder;
import org.apache.log4j.builders.layout.LayoutBuilder;
import org.apache.log4j.builders.rewrite.RewritePolicyBuilder;
import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.rewrite.RewritePolicy;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.xml.XmlConfiguration;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.util.PluginManager;
import org.apache.logging.log4j.core.config.plugins.util.PluginType;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;
import org.w3c.dom.Element;

/**
 *
 */
public class BuilderManager {

    public static final String CATEGORY = "Log4j Builder";
    private static final Logger LOGGER = StatusLogger.getLogger();
    private static Class<?>[] CONSTRUCTOR_PARAMS = new Class[] { String.class, Properties.class };
    private final Map<String, PluginType<?>> plugins;

    public BuilderManager() {
        final PluginManager manager = new PluginManager(CATEGORY);
        manager.collectPlugins();
        plugins = manager.getPlugins();
    }

    public Appender parseAppender(String className, Element appenderElement, XmlConfiguration config) {
        PluginType<?> plugin = plugins.get(className.toLowerCase());
        if (plugin != null) {
            try {
                AppenderBuilder builder = (AppenderBuilder) LoaderUtil.newInstanceOf(plugin.getPluginClass());
                return builder.parseAppender(appenderElement, config);
            } catch (ReflectiveOperationException ex) {
                LOGGER.warn("Unable to load plugin: {} due to: {}", plugin.getKey(), ex.getMessage());
            }
        }
        return null;
    }

    public Appender parseAppender(String name, String className, String prefix, String layoutPrefix,
            String filterPrefix, Properties props, PropertiesConfiguration config) {
        Objects.requireNonNull(plugins, "plugins");
        Objects.requireNonNull(className, "className");
        PluginType<?> plugin = plugins.get(className.toLowerCase());
        if (plugin != null) {
            AppenderBuilder builder = createBuilder(plugin, prefix, props);
            if (builder != null) {
                return builder.parseAppender(name, prefix, layoutPrefix, filterPrefix, props, config);
            }
        }
        return null;
    }

    public Filter parseFilter(String className, Element filterElement, XmlConfiguration config) {
        PluginType<?> plugin = plugins.get(className.toLowerCase());
        if (plugin != null) {
            try {
                FilterBuilder builder = (FilterBuilder) LoaderUtil.newInstanceOf(plugin.getPluginClass());
                return builder.parseFilter(filterElement, config);
            } catch (ReflectiveOperationException ex) {
                LOGGER.warn("Unable to load plugin: {} due to: {}", plugin.getKey(), ex.getMessage());
            }
        }
        return null;
    }

    public Filter parseFilter(String className, String filterPrefix, Properties props, PropertiesConfiguration config) {
        PluginType<?> plugin = plugins.get(className.toLowerCase());
        if (plugin != null) {
            FilterBuilder builder = createBuilder(plugin, filterPrefix, props);
            if (builder != null) {
                return builder.parseFilter(config);
            }
        }
        return null;
    }

    public Layout parseLayout(String className, Element layoutElement, XmlConfiguration config) {
        PluginType<?> plugin = plugins.get(className.toLowerCase());
        if (plugin != null) {
            try {
                LayoutBuilder builder = (LayoutBuilder) LoaderUtil.newInstanceOf(plugin.getPluginClass());
                return builder.parseLayout(layoutElement, config);
            } catch (ReflectiveOperationException ex) {
                LOGGER.warn("Unable to load plugin: {} due to: {}", plugin.getKey(), ex.getMessage());
            }
        }
        return null;
    }
    public Layout parseLayout(String className, String layoutPrefix, Properties props, PropertiesConfiguration config) {
        PluginType<?> plugin = plugins.get(className.toLowerCase());
        if (plugin != null) {
            LayoutBuilder builder = createBuilder(plugin, layoutPrefix, props);
            if (builder != null) {
                return builder.parseLayout(config);
            }
        }
        return null;
    }

    public RewritePolicy parseRewritePolicy(String className, Element rewriteElement, XmlConfiguration config) {
        PluginType<?> plugin = plugins.get(className.toLowerCase());
        if (plugin != null) {
            try {
                RewritePolicyBuilder builder = (RewritePolicyBuilder) LoaderUtil.newInstanceOf(plugin.getPluginClass());
                return builder.parseRewritePolicy(rewriteElement, config);
            } catch (ReflectiveOperationException ex) {
                LOGGER.warn("Unable to load plugin: {} due to: {}", plugin.getKey(), ex.getMessage());
            }
        }
        return null;
    }
    public RewritePolicy parseRewritePolicy(String className, String policyPrefix, Properties props, PropertiesConfiguration config) {
        PluginType<?> plugin = plugins.get(className.toLowerCase());
        if (plugin != null) {
            RewritePolicyBuilder builder = createBuilder(plugin, policyPrefix, props);
            if (builder != null) {
                return builder.parseRewritePolicy(config);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T extends Builder> T createBuilder(PluginType<?> plugin, String prefix, Properties props) {
        try {
            Class<?> clazz = plugin.getPluginClass();
            if (AbstractBuilder.class.isAssignableFrom(clazz)) {
                Constructor<T> constructor =
                        (Constructor<T>) clazz.getConstructor(CONSTRUCTOR_PARAMS);
                return constructor.newInstance(prefix, props);
            }
            Object builder = LoaderUtil.newInstanceOf(clazz);
            // Reasonable message instead of `ClassCastException`
            if (!Builder.class.isAssignableFrom(clazz)) {
                LOGGER.warn("Unable to load plugin: builder {} does not implement {}", clazz, Builder.class);
                return null;
            }
            return (T) builder;
        } catch (ReflectiveOperationException ex) {
            LOGGER.warn("Unable to load plugin: {} due to: {}", plugin.getKey(), ex.getMessage());
            return null;
        }
    }

}
