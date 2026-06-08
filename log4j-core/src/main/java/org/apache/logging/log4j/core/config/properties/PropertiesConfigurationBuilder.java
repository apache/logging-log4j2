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
package org.apache.logging.log4j.core.config.properties;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.AppenderRefComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.FilterComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.FilterableComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.LoggableComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.LoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ScriptComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ScriptFileComponentBuilder;
import org.apache.logging.log4j.core.filter.AbstractFilter.AbstractFilterBuilder;
import org.apache.logging.log4j.core.util.Builder;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.Strings;

/**
 * Helper builder for parsing properties files into a PropertiesConfiguration.
 *
 * @since 2.6
 */
public class PropertiesConfigurationBuilder extends ConfigurationBuilderFactory
        implements Builder<PropertiesConfiguration> {

    private static final String ADVERTISER_KEY = "advertiser";
    private static final String STATUS_KEY = "status";
    private static final String SHUTDOWN_HOOK = "shutdownHook";
    private static final String SHUTDOWN_TIMEOUT = "shutdownTimeout";
    private static final String DEST = "dest";
    private static final String PACKAGES = "packages";
    private static final String CONFIG_NAME = "name";
    private static final String MONITOR_INTERVAL = "monitorInterval";
    private static final String CONFIG_TYPE = "type";

    private final ConfigurationBuilder<PropertiesConfiguration> builder;
    private LoggerContext loggerContext;
    private Properties rootProperties;

    public PropertiesConfigurationBuilder() {
        this.builder = newConfigurationBuilder(PropertiesConfiguration.class);
    }

    public PropertiesConfigurationBuilder setRootProperties(final Properties rootProperties) {
        this.rootProperties = rootProperties;
        return this;
    }

    public PropertiesConfigurationBuilder setConfigurationSource(final ConfigurationSource source) {
        builder.setConfigurationSource(source);
        return this;
    }

    @Override
    public PropertiesConfiguration build() {
        for (final String key : rootProperties.stringPropertyNames()) {
            if (!key.contains(".")) {
                builder.addRootProperty(key, rootProperties.getProperty(key));
            }
        }
        final Properties rootLoggerSubset = PropertiesUtil.extractSubset(rootProperties, "rootLogger");
        if (rootLoggerSubset.size() > 0) {
            builder.add(createRootLogger(rootLoggerSubset));
        }
        final Map<String, Properties> loggers =
                PropertiesUtil.partitionOnCommonPrefixes(PropertiesUtil.extractSubset(rootProperties, "logger"));
        for (final Map.Entry<String, Properties> entry : loggers.entrySet()) {
            final LoggerComponentBuilder loggerBuilder = createLogger(entry.getKey(), entry.getValue());
            if (loggerBuilder != null) {
                builder.add(loggerBuilder);
            }
        }
        final Map<String, Properties> appenders =
                PropertiesUtil.partitionOnCommonPrefixes(PropertiesUtil.extractSubset(rootProperties, "appender"));
        for (final Map.Entry<String, Properties> entry : appenders.entrySet()) {
            final AppenderComponentBuilder appenderBuilder = createAppender(entry.getKey(), entry.getValue());
            if (appenderBuilder != null) {
                builder.add(appenderBuilder);
            }
        }
        final Map<String, Properties> filters =
                PropertiesUtil.partitionOnCommonPrefixes(PropertiesUtil.extractSubset(rootProperties, "filter"));
        for (final Map.Entry<String, Properties> entry : filters.entrySet()) {
            builder.add(createFilter(entry.getKey(), entry.getValue()));
        }
        final String loggerProp = rootProperties.getProperty("loggers");
        if (loggerProp != null) {
            final String[] parts = loggerProp.split(",");
            for (final String logger : parts) {
                final String name = logger.trim();
                final Properties sub = PropertiesUtil.extractSubset(rootProperties, "logger." + name);
                final String levelAndRefs = sub.getProperty("");
                if (levelAndRefs != null) {
                    sub.remove("");
                    final String[] refParts = levelAndRefs.split(",");
                    final String level = refParts[0].trim();
                    final LoggerComponentBuilder loggerBuilder = builder.newLogger(name, level);
                    for (int i = 1; i < refParts.length; i++) {
                        loggerBuilder.add(builder.newAppenderRef(refParts[i].trim()));
                    }
                    addLoggersToComponent(loggerBuilder, sub);
                    addFiltersToComponent(loggerBuilder, sub);
                    builder.add(loggerBuilder);
                } else {
                    final LoggerComponentBuilder loggerBuilder = createLogger(name, sub);
                    if (loggerBuilder != null) {
                        builder.add(loggerBuilder);
                    }
                }
            }
        }
        final String appenderProp = rootProperties.getProperty("appenders");
        if (appenderProp != null) {
            final String[] parts = appenderProp.split(",");
            for (final String appender : parts) {
                final String name = appender.trim();
                final Properties sub = PropertiesUtil.extractSubset(rootProperties, "appender." + name);
                final AppenderComponentBuilder appenderBuilder = createAppender(name, sub);
                if (appenderBuilder != null) {
                    builder.add(appenderBuilder);
                }
            }
        }
        final String filterProp = rootProperties.getProperty("filters");
        if (filterProp != null) {
            final String[] parts = filterProp.split(",");
            for (final String filter : parts) {
                final String name = filter.trim();
                final Properties sub = PropertiesUtil.extractSubset(rootProperties, "filter." + name);
                final FilterComponentBuilder filterBuilder = createFilter(name, sub);
                if (filterBuilder != null) {
                    builder.add(filterBuilder);
                }
            }
        }
        return builder.build();
    }

    private void removeDefinedButUnusedProperties(final String prefix) {
        PropertiesUtil.extractSubset(rootProperties, prefix);
    }

    private void processRemainingProperties(
            final ConfigurationBuilder<PropertiesConfiguration> builder, final Properties properties) {
        while (properties.size() > 0) {
            final String propertyName =
                    properties.stringPropertyNames().iterator().next();
            final int index = propertyName.indexOf('.');
            if (index > 0) {
                final String prefix = propertyName.substring(0, index);
                final Properties componentProperties = PropertiesUtil.extractSubset(properties, prefix);
                ComponentBuilder<?> componentBuilder = createComponent(builder, prefix, componentProperties);
                builder.addComponent(componentBuilder);
            } else {
                properties.remove(propertyName);
            }
        }
    }

    private ScriptComponentBuilder createScript(final Properties properties) {
        final String name = (String) properties.remove("name");
        final String language = (String) properties.remove("language");
        final String text = (String) properties.remove("text");
        final ScriptComponentBuilder scriptBuilder = builder.newScript(name, language, text);
        return processRemainingProperties(scriptBuilder, properties);
    }

    private ScriptFileComponentBuilder createScriptFile(final Properties properties) {
        final String name = (String) properties.remove("name");
        final String path = (String) properties.remove("path");
        final ScriptFileComponentBuilder scriptFileBuilder = builder.newScriptFile(name, path);
        return processRemainingProperties(scriptFileBuilder, properties);
    }

    private AppenderComponentBuilder createAppender(final String key, final Properties properties) {
        String name = (String) properties.remove(CONFIG_NAME);
        if (name == null) {
            String prefix = null;
            for (final String propName : properties.stringPropertyNames()) {
                if (propName.endsWith("." + CONFIG_NAME)) {
                    final int dotCount =
                            propName.length() - propName.replace(".", "").length();
                    if (dotCount == 1) {
                        final String tempPrefix = propName.substring(0, propName.length() - CONFIG_NAME.length() - 1);
                        if (!tempPrefix.equals("property")
                                && !tempPrefix.equals("appenderRef")
                                && !tempPrefix.equals("filter")) {
                            prefix = tempPrefix;
                            name = properties.getProperty(propName);
                            break;
                        }
                    }
                }
            }
            if (prefix != null) {
                final Properties flatProperties = new Properties();
                final String prefixWithDot = prefix + ".";
                for (final String propName : properties.stringPropertyNames()) {
                    if (propName.startsWith(prefixWithDot)) {
                        flatProperties.setProperty(
                                propName.substring(prefixWithDot.length()), properties.getProperty(propName));
                    } else {
                        flatProperties.setProperty(propName, properties.getProperty(propName));
                    }
                }
                properties.clear();
                properties.putAll(flatProperties);
                properties.remove(CONFIG_NAME);
            }
        }

        if (Strings.isEmpty(name)) {
            return null;
        }
        final String type = (String) properties.remove("type");
        if (Strings.isEmpty(type)) {
            return null;
        }
        final AppenderComponentBuilder appenderBuilder = builder.newAppender(name, type);

        final java.util.List<String> keysToRemove = new java.util.ArrayList<>();
        for (final String propName : properties.stringPropertyNames()) {
            if (propName.startsWith("appenderRef") || propName.startsWith("filter")) {
                keysToRemove.add(propName);
            }
        }
        for (final String propName : keysToRemove) {
            properties.remove(propName);
        }

        return processRemainingProperties(appenderBuilder, properties);
    }

    private FilterComponentBuilder createFilter(final String key, final Properties properties) {
        final String type = (String) properties.remove(CONFIG_TYPE);
        if (Strings.isEmpty(type)) {
            return null;
        }
        final String onMatch = (String) properties.remove(AbstractFilterBuilder.ATTR_ON_MATCH);
        final String onMismatch = (String) properties.remove(AbstractFilterBuilder.ATTR_ON_MISMATCH);
        final FilterComponentBuilder filterBuilder = builder.newFilter(type, onMatch, onMismatch);
        return processRemainingProperties(filterBuilder, properties);
    }

    private AppenderRefComponentBuilder createAppenderRef(final String key, final Properties properties) {
        final String ref = (String) properties.remove("ref");
        if (Strings.isEmpty(ref)) {
            throw new ConfigurationException("No ref attribute provided for AppenderRef " + key);
        }
        final AppenderRefComponentBuilder appenderRefBuilder = builder.newAppenderRef(ref);
        final String level = Strings.trimToNull((String) properties.remove("level"));
        if (!Strings.isEmpty(level)) {
            appenderRefBuilder.addAttribute("level", level);
        }
        return addFiltersToComponent(appenderRefBuilder, properties);
    }

    private LoggerComponentBuilder createLogger(final String key, final Properties properties) {
        String name = (String) properties.remove(CONFIG_NAME);
        if (name == null) {
            String prefix = null;
            for (final String propName : properties.stringPropertyNames()) {
                if (propName.endsWith("." + CONFIG_NAME)) {
                    final int dotCount =
                            propName.length() - propName.replace(".", "").length();
                    if (dotCount == 1) {
                        final String tempPrefix = propName.substring(0, propName.length() - CONFIG_NAME.length() - 1);
                        if (!tempPrefix.equals("property")
                                && !tempPrefix.equals("appenderRef")
                                && !tempPrefix.equals("filter")) {
                            prefix = tempPrefix;
                            name = properties.getProperty(propName);
                            break;
                        }
                    }
                }
            }
            if (prefix != null) {
                final Properties flatProperties = new Properties();
                final String prefixWithDot = prefix + ".";
                for (final String propName : properties.stringPropertyNames()) {
                    if (propName.startsWith(prefixWithDot)) {
                        flatProperties.setProperty(
                                propName.substring(prefixWithDot.length()), properties.getProperty(propName));
                    } else {
                        flatProperties.setProperty(propName, properties.getProperty(propName));
                    }
                }
                properties.clear();
                properties.putAll(flatProperties);
                properties.remove(CONFIG_NAME);
            }
        }
        if (Strings.isEmpty(name)) {
            return null;
        }
        final String location = (String) properties.remove("includeLocation");
        final String level = (String) properties.remove("level");
        final String type = (String) properties.remove("type");
        final LoggerComponentBuilder loggerBuilder =
                type != null ? builder.newLogger(name, type) : builder.newLogger(name);
        if (location != null) {
            loggerBuilder.addAttribute("includeLocation", location);
        }
        if (level != null) {
            loggerBuilder.addAttribute("level", level);
        }
        addLoggersToComponent(loggerBuilder, properties);
        addFiltersToComponent(loggerBuilder, properties);
        final String additivity = (String) properties.remove("additivity");
        if (!Strings.isEmpty(additivity)) {
            loggerBuilder.addAttribute("additivity", additivity);
        }

        // Clean up keys processed by addLoggersToComponent and addFiltersToComponent
        final java.util.List<String> keysToRemove = new java.util.ArrayList<>();
        for (final String propName : properties.stringPropertyNames()) {
            if (propName.startsWith("appenderRef") || propName.startsWith("filter")) {
                keysToRemove.add(propName);
            }
        }
        for (final String propName : keysToRemove) {
            properties.remove(propName);
        }

        return processRemainingProperties(loggerBuilder, properties);
    }

    private RootLoggerComponentBuilder createRootLogger(final Properties properties) {
        final String levelAndRefs = properties.getProperty("");
        final RootLoggerComponentBuilder loggerBuilder;
        if (levelAndRefs != null) {
            properties.remove("");
            final String[] parts = levelAndRefs.split(",");
            final String level = parts[0].trim();
            loggerBuilder = builder.newRootLogger(level);
            for (int i = 1; i < parts.length; i++) {
                loggerBuilder.add(builder.newAppenderRef(parts[i].trim()));
            }
        } else {
            loggerBuilder = builder.newRootLogger();
            final String level = (String) properties.remove("level");
            if (level != null) {
                loggerBuilder.addAttribute("level", level);
            }
            final String includeLocation = (String) properties.remove("includeLocation");
            if (includeLocation != null) {
                loggerBuilder.addAttribute("includeLocation", includeLocation);
            }
        }
        addLoggersToComponent(loggerBuilder, properties);
        addFiltersToComponent(loggerBuilder, properties);

        final List<String> keysToRemove = new java.util.ArrayList<>();
        for (final String propName : properties.stringPropertyNames()) {
            if (propName.startsWith("appenderRef") || propName.startsWith("filter")) {
                keysToRemove.add(propName);
            }
        }
        for (final String propName : keysToRemove) {
            properties.remove(propName);
        }

        return processRemainingProperties(loggerBuilder, properties);
    }

    private LayoutComponentBuilder createLayout(final String appenderName, final Properties properties) {
        final String type = (String) properties.remove(CONFIG_TYPE);
        if (Strings.isEmpty(type)) {
            throw new ConfigurationException("No type attribute provided for Layout on Appender " + appenderName);
        }
        final LayoutComponentBuilder layoutBuilder = builder.newLayout(type);
        return processRemainingProperties(layoutBuilder, properties);
    }

    private static <B extends ComponentBuilder<B>> ComponentBuilder<B> createComponent(
            final ComponentBuilder<?> parent, final String key, final Properties properties) {
        return createComponent(parent.getBuilder(), key, properties);
    }

    private static <B extends ComponentBuilder<B>> ComponentBuilder<B> createComponent(
            final ConfigurationBuilder<?> parentBuilder, final String key, final Properties properties) {
        final String name = (String) properties.remove(CONFIG_NAME);
        final String type = (String) properties.remove(CONFIG_TYPE);
        if (Strings.isEmpty(type)) {
            throw new ConfigurationException("No type attribute provided for component " + key);
        }
        final ComponentBuilder<B> componentBuilder = parentBuilder.newComponent(name, type);
        return processRemainingProperties(componentBuilder, properties);
    }

    private static <B extends ComponentBuilder<?>> B processRemainingProperties(
            final B builder, final Properties properties) {
        while (properties.size() > 0) {
            final String propertyName =
                    properties.stringPropertyNames().iterator().next();
            final int index = propertyName.indexOf('.');
            if (index > 0) {
                final String prefix = propertyName.substring(0, index);
                final Properties componentProperties = PropertiesUtil.extractSubset(properties, prefix);
                if (componentProperties.containsKey("type")) {
                    builder.addComponent(createComponent(builder, prefix, componentProperties));
                }
            } else if (!propertyName.isEmpty()) {
                builder.addAttribute(propertyName, properties.getProperty(propertyName));
                properties.remove(propertyName);
            } else {
                properties.remove(propertyName);
            }
        }
        return builder;
    }

    private <B extends FilterableComponentBuilder<? extends ComponentBuilder<?>>> B addFiltersToComponent(
            final B componentBuilder, final Properties properties) {
        final Map<String, Properties> filters =
                PropertiesUtil.partitionOnCommonPrefixes(PropertiesUtil.extractSubset(properties, "filter"));
        for (final Map.Entry<String, Properties> entry : filters.entrySet()) {
            componentBuilder.add(createFilter(entry.getKey().trim(), entry.getValue()));
        }
        return componentBuilder;
    }

    private <B extends LoggableComponentBuilder<? extends ComponentBuilder<?>>> B addLoggersToComponent(
            final B loggerBuilder, final Properties properties) {
        final Map<String, Properties> appenderRefs =
                PropertiesUtil.partitionOnCommonPrefixes(PropertiesUtil.extractSubset(properties, "appenderRef"));
        for (final Map.Entry<String, Properties> entry : appenderRefs.entrySet()) {
            loggerBuilder.add(createAppenderRef(entry.getKey().trim(), entry.getValue()));
        }
        return loggerBuilder;
    }

    public PropertiesConfigurationBuilder setLoggerContext(
            final org.apache.logging.log4j.core.LoggerContext loggerContext) {
        this.loggerContext = loggerContext;
        return this;
    }

    public LoggerContext getLoggerContext() {
        return loggerContext;
    }
}
