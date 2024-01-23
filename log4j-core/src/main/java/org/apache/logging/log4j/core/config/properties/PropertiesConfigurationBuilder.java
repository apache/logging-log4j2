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

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.LoggerConfig;
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
import org.apache.logging.log4j.core.util.Integers;
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
        builder.setStatusLevel(Level.toLevel(rootProperties.getProperty(STATUS_KEY), Level.ERROR))
                .setShutdownHook(rootProperties.getProperty(SHUTDOWN_HOOK))
                .setShutdownTimeout(
                        Long.parseLong(rootProperties.getProperty(SHUTDOWN_TIMEOUT, "0")), TimeUnit.MILLISECONDS)
                .setDestination(rootProperties.getProperty(DEST))
                .setPackages(rootProperties.getProperty(PACKAGES))
                .setConfigurationName(rootProperties.getProperty(CONFIG_NAME))
                .setMonitorInterval(rootProperties.getProperty(MONITOR_INTERVAL, "0"))
                .setAdvertiser(rootProperties.getProperty(ADVERTISER_KEY));

        final Properties propertyPlaceholders = PropertiesUtil.extractSubset(rootProperties, "property");
        for (final String key : propertyPlaceholders.stringPropertyNames()) {
            builder.addProperty(key, propertyPlaceholders.getProperty(key));
        }

        final Map<String, Properties> scripts =
                PropertiesUtil.partitionOnCommonPrefixes(PropertiesUtil.extractSubset(rootProperties, "script"));
        for (final Map.Entry<String, Properties> entry : scripts.entrySet()) {
            final Properties scriptProps = entry.getValue();
            final String type = (String) scriptProps.remove("type");
            if (type == null) {
                throw new ConfigurationException("No type provided for script - must be Script or ScriptFile");
            }
            if (type.equalsIgnoreCase("script")) {
                builder.add(createScript(scriptProps));
            } else {
                builder.add(createScriptFile(scriptProps));
            }
        }

        final Properties levelProps = PropertiesUtil.extractSubset(rootProperties, "customLevel");
        if (levelProps.size() > 0) {
            for (final String key : levelProps.stringPropertyNames()) {
                builder.add(builder.newCustomLevel(key, Integers.parseInt(levelProps.getProperty(key))));
            }
        }

        final String filterProp = rootProperties.getProperty("filters");
        if (filterProp != null) {
            final String[] filterNames = filterProp.split(",");
            for (final String filterName : filterNames) {
                final String name = filterName.trim();
                builder.add(createFilter(name, PropertiesUtil.extractSubset(rootProperties, "filter." + name)));
            }
        } else {

            final Map<String, Properties> filters =
                    PropertiesUtil.partitionOnCommonPrefixes(PropertiesUtil.extractSubset(rootProperties, "filter"));
            for (final Map.Entry<String, Properties> entry : filters.entrySet()) {
                builder.add(createFilter(entry.getKey().trim(), entry.getValue()));
            }
        }

        final String appenderProp = rootProperties.getProperty("appenders");
        if (appenderProp != null) {
            final String[] appenderNames = appenderProp.split(",");
            for (final String appenderName : appenderNames) {
                final String name = appenderName.trim();
                builder.add(createAppender(
                        appenderName.trim(), PropertiesUtil.extractSubset(rootProperties, "appender." + name)));
            }
        } else {
            final Map<String, Properties> appenders = PropertiesUtil.partitionOnCommonPrefixes(
                    PropertiesUtil.extractSubset(rootProperties, Appender.ELEMENT_TYPE));
            for (final Map.Entry<String, Properties> entry : appenders.entrySet()) {
                builder.add(createAppender(entry.getKey().trim(), entry.getValue()));
            }
        }

        final String loggerProp = rootProperties.getProperty("loggers");
        if (loggerProp != null) {
            final String[] loggerNames = loggerProp.split(",");
            for (final String loggerName : loggerNames) {
                final String name = loggerName.trim();
                if (!name.equals(LoggerConfig.ROOT)) {
                    builder.add(createLogger(name, PropertiesUtil.extractSubset(rootProperties, "logger." + name)));
                }
            }
        } else {

            final Map<String, Properties> loggers = PropertiesUtil.partitionOnCommonPrefixes(
                    PropertiesUtil.extractSubset(rootProperties, "logger"), true);
            for (final Map.Entry<String, Properties> entry : loggers.entrySet()) {
                final String name = entry.getKey().trim();
                if (!name.equals(LoggerConfig.ROOT)) {
                    builder.add(createLogger(name, entry.getValue()));
                }
            }
        }

        final String rootProp = rootProperties.getProperty("rootLogger");
        final Properties props = PropertiesUtil.extractSubset(rootProperties, "rootLogger");
        if (rootProp != null) {
            props.setProperty("", rootProp);
            rootProperties.remove("rootLogger");
        }
        if (props.size() > 0) {
            builder.add(createRootLogger(props));
        }

        builder.setLoggerContext(loggerContext);

        return builder.build(false);
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
        final String name = (String) properties.remove(CONFIG_NAME);
        if (Strings.isEmpty(name)) {
            throw new ConfigurationException("No name attribute provided for Appender " + key);
        }
        final String type = (String) properties.remove(CONFIG_TYPE);
        if (Strings.isEmpty(type)) {
            throw new ConfigurationException("No type attribute provided for Appender " + key);
        }
        final AppenderComponentBuilder appenderBuilder = builder.newAppender(name, type);
        addFiltersToComponent(appenderBuilder, properties);
        final Properties layoutProps = PropertiesUtil.extractSubset(properties, "layout");
        if (layoutProps.size() > 0) {
            appenderBuilder.add(createLayout(name, layoutProps));
        }

        return processRemainingProperties(appenderBuilder, properties);
    }

    private FilterComponentBuilder createFilter(final String key, final Properties properties) {
        final String type = (String) properties.remove(CONFIG_TYPE);
        if (Strings.isEmpty(type)) {
            throw new ConfigurationException("No type attribute provided for Filter " + key);
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
        final String levelAndRefs = properties.getProperty("");
        final String name = (String) properties.remove(CONFIG_NAME);
        final String location = (String) properties.remove("includeLocation");
        if (Strings.isEmpty(name)) {
            throw new ConfigurationException("No name attribute provided for Logger " + key);
        }
        final String level = Strings.trimToNull((String) properties.remove("level"));
        final String type = (String) properties.remove(CONFIG_TYPE);
        final LoggerComponentBuilder loggerBuilder;
        boolean includeLocation;
        if (type != null) {
            if (type.equalsIgnoreCase("asyncLogger")) {
                if (location != null) {
                    includeLocation = Boolean.parseBoolean(location);
                    loggerBuilder = builder.newAsyncLogger(name, level, includeLocation);
                } else {
                    loggerBuilder = builder.newAsyncLogger(name, level);
                }
            } else {
                throw new ConfigurationException("Unknown Logger type " + type + " for Logger " + name);
            }
        } else if (location != null) {
            includeLocation = Boolean.parseBoolean(location);
            loggerBuilder = builder.newLogger(name, level, includeLocation);
        } else {
            loggerBuilder = builder.newLogger(name, level);
        }
        addLoggersToComponent(loggerBuilder, properties);
        addFiltersToComponent(loggerBuilder, properties);
        final String additivity = (String) properties.remove("additivity");
        if (!Strings.isEmpty(additivity)) {
            loggerBuilder.addAttribute("additivity", additivity);
        }
        if (levelAndRefs != null) {
            loggerBuilder.addAttribute("levelAndRefs", levelAndRefs);
        }
        return loggerBuilder;
    }

    private RootLoggerComponentBuilder createRootLogger(final Properties properties) {
        final String levelAndRefs = properties.getProperty("");
        final String level = Strings.trimToNull((String) properties.remove("level"));
        final String type = (String) properties.remove(CONFIG_TYPE);
        final String location = (String) properties.remove("includeLocation");
        final boolean includeLocation;
        final RootLoggerComponentBuilder loggerBuilder;
        if (type != null) {
            if (type.equalsIgnoreCase("asyncRoot")) {
                if (location != null) {
                    includeLocation = Boolean.parseBoolean(location);
                    loggerBuilder = builder.newAsyncRootLogger(level, includeLocation);
                } else {
                    loggerBuilder = builder.newAsyncRootLogger(level);
                }
            } else {
                throw new ConfigurationException("Unknown Logger type for root logger" + type);
            }
        } else if (location != null) {
            includeLocation = Boolean.parseBoolean(location);
            loggerBuilder = builder.newRootLogger(level, includeLocation);
        } else {
            loggerBuilder = builder.newRootLogger(level);
        }
        addLoggersToComponent(loggerBuilder, properties);
        if (levelAndRefs != null) {
            loggerBuilder.addAttribute("levelAndRefs", levelAndRefs);
        }
        return addFiltersToComponent(loggerBuilder, properties);
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
        final String name = (String) properties.remove(CONFIG_NAME);
        final String type = (String) properties.remove(CONFIG_TYPE);
        if (Strings.isEmpty(type)) {
            throw new ConfigurationException("No type attribute provided for component " + key);
        }
        final ComponentBuilder<B> componentBuilder = parent.getBuilder().newComponent(name, type);
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
                builder.addComponent(createComponent(builder, prefix, componentProperties));
            } else {
                builder.addAttribute(propertyName, properties.getProperty(propertyName));
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

    public PropertiesConfigurationBuilder setLoggerContext(final LoggerContext loggerContext) {
        this.loggerContext = loggerContext;
        return this;
    }

    public LoggerContext getLoggerContext() {
        return loggerContext;
    }
}
