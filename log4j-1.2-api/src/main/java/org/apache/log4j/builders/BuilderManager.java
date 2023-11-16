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
package org.apache.log4j.builders;

import static org.apache.logging.log4j.util.Strings.toRootLowerCase;

import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.bridge.AppenderWrapper;
import org.apache.log4j.bridge.FilterWrapper;
import org.apache.log4j.bridge.LayoutWrapper;
import org.apache.log4j.bridge.RewritePolicyWrapper;
import org.apache.log4j.builders.appender.AppenderBuilder;
import org.apache.log4j.builders.filter.FilterBuilder;
import org.apache.log4j.builders.layout.LayoutBuilder;
import org.apache.log4j.builders.rewrite.RewritePolicyBuilder;
import org.apache.log4j.builders.rolling.TriggeringPolicyBuilder;
import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.rewrite.RewritePolicy;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.xml.XmlConfiguration;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;
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

    public static final Appender INVALID_APPENDER = new AppenderWrapper(null);
    public static final Filter INVALID_FILTER = new FilterWrapper(null);
    public static final Layout INVALID_LAYOUT = new LayoutWrapper(null);
    public static final RewritePolicy INVALID_REWRITE_POLICY = new RewritePolicyWrapper(null);
    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final Class<?>[] CONSTRUCTOR_PARAMS = new Class[] {String.class, Properties.class};
    private final Map<String, PluginType<?>> plugins;

    /**
     * Constructs a new instance.
     */
    public BuilderManager() {
        final PluginManager manager = new PluginManager(CATEGORY);
        manager.collectPlugins();
        plugins = manager.getPlugins();
    }

    private <T extends Builder<U>, U> T createBuilder(
            final PluginType<T> plugin, final String prefix, final Properties props) {
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
        final String key = toRootLowerCase(className).trim();
        final PluginType<?> pluginType = plugins.get(key);
        if (pluginType == null) {
            LOGGER.warn("Unable to load plugin class name {} with key {}", className, key);
        }
        return (PluginType<T>) pluginType;
    }

    private <T extends Builder<U>, U> U newInstance(
            final PluginType<T> plugin, final Function<T, U> consumer, final U invalidValue) {
        if (plugin != null) {
            try {
                final T builder = LoaderUtil.newInstanceOf(plugin.getPluginClass());
                if (builder != null) {
                    final U result = consumer.apply(builder);
                    // returning an empty wrapper is short for "we support this legacy class, but it has validation
                    // errors"
                    return result != null ? result : invalidValue;
                }
            } catch (final ReflectiveOperationException ex) {
                LOGGER.warn("Unable to load plugin: {} due to: {}", plugin.getKey(), ex.getMessage());
            }
        }
        return null;
    }

    public <P extends Parser<T>, T> T parse(
            final String className,
            final String prefix,
            final Properties props,
            final PropertiesConfiguration config,
            final T invalidValue) {
        final P parser = createBuilder(getPlugin(className), prefix, props);
        if (parser != null) {
            final T value = parser.parse(config);
            return value != null ? value : invalidValue;
        }
        return null;
    }

    public Appender parseAppender(
            final String className, final Element appenderElement, final XmlConfiguration config) {
        return newInstance(
                this.<AppenderBuilder<Appender>>getPlugin(className),
                b -> b.parseAppender(appenderElement, config),
                INVALID_APPENDER);
    }

    public Appender parseAppender(
            final String name,
            final String className,
            final String prefix,
            final String layoutPrefix,
            final String filterPrefix,
            final Properties props,
            final PropertiesConfiguration config) {
        final AppenderBuilder<Appender> builder = createBuilder(getPlugin(className), prefix, props);
        if (builder != null) {
            final Appender appender = builder.parseAppender(name, prefix, layoutPrefix, filterPrefix, props, config);
            return appender != null ? appender : INVALID_APPENDER;
        }
        return null;
    }

    public Filter parseFilter(final String className, final Element filterElement, final XmlConfiguration config) {
        return newInstance(
                this.<FilterBuilder>getPlugin(className), b -> b.parse(filterElement, config), INVALID_FILTER);
    }

    public Layout parseLayout(final String className, final Element layoutElement, final XmlConfiguration config) {
        return newInstance(
                this.<LayoutBuilder>getPlugin(className), b -> b.parse(layoutElement, config), INVALID_LAYOUT);
    }

    public RewritePolicy parseRewritePolicy(
            final String className, final Element rewriteElement, final XmlConfiguration config) {
        return newInstance(
                this.<RewritePolicyBuilder>getPlugin(className),
                b -> b.parse(rewriteElement, config),
                INVALID_REWRITE_POLICY);
    }

    public TriggeringPolicy parseTriggeringPolicy(
            final String className, final Element policyElement, final XmlConfiguration config) {
        return newInstance(
                this.<TriggeringPolicyBuilder>getPlugin(className),
                b -> b.parse(policyElement, config),
                (TriggeringPolicy) null);
    }
}
