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

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;

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

    /** Plugin category. */
    public static final String CATEGORY = "Log4j Builder";

    private static final Logger LOGGER = StatusLogger.getLogger();
    private static Class<?>[] CONSTRUCTOR_PARAMS = new Class[] {String.class, Properties.class};
    private final Map<String, PluginType<?>> plugins;

    /**
     * Constructs a new instance.
     */
    public BuilderManager() {
        final PluginManager manager = new PluginManager(CATEGORY);
        manager.collectPlugins();
        plugins = manager.getPlugins();
    }

    private <T extends Builder<U>, U> T createBuilder(final PluginType<T> plugin, final String prefix, final Properties props) {
        if (plugin == null) {
            return null;
        }
        try {
            final Class<T> clazz = plugin.getPluginClass();
            if (AbstractBuilder.class.isAssignableFrom(clazz)) {
                return clazz.getConstructor(CONSTRUCTOR_PARAMS).newInstance(prefix, props);
            }
            final T builder = LoaderUtil.newInstanceOf(clazz);
            // Reasonable message instead of `ClassCastException`
            if (!Builder.class.isAssignableFrom(clazz)) {
                LOGGER.warn("Unable to load plugin: builder {} does not implement {}", clazz, Builder.class);
                return null;
            }
            return builder;
        } catch (final ReflectiveOperationException ex) {
            LOGGER.warn("Unable to load plugin: {} due to: {}", plugin.getKey(), ex.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> PluginType<T> getPlugin(final String className) {
        Objects.requireNonNull(plugins, "plugins");
        Objects.requireNonNull(className, "className");
        final String key = className.toLowerCase(Locale.ROOT).trim();
        final PluginType<?> pluginType = plugins.get(key);
        if (pluginType == null) {
            LOGGER.warn("Unable to load plugin class name {} with key {}", className, key);
        }
        return (PluginType<T>) pluginType;
    }

    private <T extends Builder<U>, U> U newInstance(final PluginType<T> plugin, final Function<T, U> consumer) {
        if (plugin != null) {
            try {
                final T builder = LoaderUtil.newInstanceOf(plugin.getPluginClass());
                if (builder != null) {
                    return consumer.apply(builder);
                }
            } catch (final ReflectiveOperationException ex) {
                LOGGER.warn("Unable to load plugin: {} due to: {}", plugin.getKey(), ex.getMessage());
            }
        }
        return null;
    }

    public <P extends Parser<T>, T> T parse(final String className, final String prefix, final Properties props, final PropertiesConfiguration config) {
        final P parser = createBuilder(getPlugin(className), prefix, props);
        return parser != null ? parser.parse(config) : null;
    }

    public Appender parseAppender(final String className, final Element appenderElement, final XmlConfiguration config) {
        return newInstance(this.<AppenderBuilder<Appender>>getPlugin(className), b -> b.parseAppender(appenderElement, config));
    }

    public Appender parseAppender(final String name, final String className, final String prefix, final String layoutPrefix, final String filterPrefix,
        final Properties props, final PropertiesConfiguration config) {
        final AppenderBuilder<Appender> builder = createBuilder(getPlugin(className), prefix, props);
        return builder != null ? builder.parseAppender(name, prefix, layoutPrefix, filterPrefix, props, config) : null;
    }

    public Filter parseFilter(final String className, final Element filterElement, final XmlConfiguration config) {
        return newInstance(this.<FilterBuilder>getPlugin(className), b -> b.parse(filterElement, config));
    }

    public Layout parseLayout(final String className, final Element layoutElement, final XmlConfiguration config) {
        return newInstance(this.<LayoutBuilder>getPlugin(className), b -> b.parse(layoutElement, config));
    }

    public RewritePolicy parseRewritePolicy(final String className, final Element rewriteElement, final XmlConfiguration config) {
        return newInstance(this.<RewritePolicyBuilder>getPlugin(className), b -> b.parse(rewriteElement, config));
    }

}
