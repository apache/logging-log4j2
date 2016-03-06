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

package org.apache.logging.log4j.core.config.properties;

import java.util.Properties;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.ConfigurationException;
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
    private static final String VERBOSE = "verbose";
    private static final String PACKAGES = "packages";
    private static final String CONFIG_NAME = "name";
    private static final String MONITOR_INTERVAL = "monitorInterval";
    private static final String CONFIG_TYPE = "type";

    private final ConfigurationBuilder<PropertiesConfiguration> builder;
    private Properties rootProperties;

    public PropertiesConfigurationBuilder() {
        this.builder = newConfigurationBuilder(PropertiesConfiguration.class);
    }

    public PropertiesConfigurationBuilder setRootProperties(final Properties rootProperties) {
        this.rootProperties = rootProperties;
        return this;
    }

    @Override
    public PropertiesConfiguration build() {
        String value = rootProperties.getProperty(STATUS_KEY);
        if (value != null) {
            builder.setStatusLevel(Level.toLevel(value, Level.ERROR));
        } else {
            builder.setStatusLevel(Level.ERROR);
        }
        value = rootProperties.getProperty(SHUTDOWN_HOOK);
        if (value != null) {
            builder.setShutdownHook(value);
        }
        value = rootProperties.getProperty(VERBOSE);
        if (value != null) {
            builder.setVerbosity(value);
        }
        value = rootProperties.getProperty(PACKAGES);
        if (value != null) {
            builder.setPackages(value);
        }
        value = rootProperties.getProperty(CONFIG_NAME);
        if (value != null) {
            builder.setConfigurationName(value);
        }
        value = rootProperties.getProperty(MONITOR_INTERVAL);
        if (value != null) {
            builder.setMonitorInterval(value);
        }
        value = rootProperties.getProperty(ADVERTISER_KEY);
        if (value != null) {
            builder.setAdvertiser(value);
        }
        Properties props = PropertiesUtil.extractSubset(rootProperties, "property");
        for (String key : props.stringPropertyNames()) {
            builder.addProperty(key, props.getProperty(key));
        }

        String scriptProp = rootProperties.getProperty("scripts");
        if (scriptProp != null) {
            String[] scriptNames = scriptProp.split(",");
            for (String scriptName : scriptNames) {
                String name = scriptName.trim();
                Properties scriptProps = PropertiesUtil.extractSubset(rootProperties, "script." + name);
                String type = scriptProps.getProperty("type");
                if (type == null) {
                    throw new ConfigurationException("No type provided for script - must be Script or ScriptFile");
                }
                scriptProps.remove("type");
                if (type.equalsIgnoreCase("script")) {
                    builder.add(createScript(scriptProps));
                } else {
                    builder.add(createScriptFile(scriptProps));
                }
            }
        }

        Properties levelProps = PropertiesUtil.extractSubset(rootProperties, "customLevel");
        if (levelProps.size() > 0) {
            for (String key : levelProps.stringPropertyNames()) {
                builder.add(builder.newCustomLevel(key, Integer.parseInt(props.getProperty(key))));
            }
        }

        String filterProp = rootProperties.getProperty("filters");
        if (filterProp != null) {
            String[] filterNames = filterProp.split(",");
            for (String filterName : filterNames) {
                String name = filterName.trim();
                builder.add(createFilter(name, PropertiesUtil.extractSubset(rootProperties, "filter." + name)));
            }
        }
        String appenderProp = rootProperties.getProperty("appenders");
        if (appenderProp != null) {
            String[] appenderNames = appenderProp.split(",");
            for (String appenderName : appenderNames) {
                String name = appenderName.trim();
                builder.add(createAppender(name, PropertiesUtil.extractSubset(rootProperties, "appender." + name)));
            }
        }
        String loggerProp = rootProperties.getProperty("loggers");
        if (loggerProp != null) {
            String[] loggerNames = loggerProp.split(",");
            for (String loggerName : loggerNames) {
                String name = loggerName.trim();
                if (!name.equals(LoggerConfig.ROOT)) {
                    builder.add(createLogger(name, PropertiesUtil.extractSubset(rootProperties, "logger." + name)));
                }
            }
        }

        props = PropertiesUtil.extractSubset(rootProperties, "rootLogger");
        if (props.size() > 0) {
            builder.add(createRootLogger(props));
        }


        return builder.build();
    }

    private ScriptComponentBuilder createScript(final Properties properties) {
        String name = properties.getProperty("name");
        if (name != null) {
            properties.remove("name");
        }
        String language = properties.getProperty("language");
        if (language != null) {
            properties.remove("language");
        }
        String text = properties.getProperty("text");
        if (text != null) {
            properties.remove("text");
        }
        ScriptComponentBuilder scriptBuilder = builder.newScript(name, language, text);
        return processRemainingProperties(scriptBuilder, properties);
    }


    private ScriptFileComponentBuilder createScriptFile(final Properties properties) {
        String name = properties.getProperty("name");
        if (name != null) {
            properties.remove("name");
        }
        String path = properties.getProperty("path");
        if (path != null) {
            properties.remove("path");
        }
        ScriptFileComponentBuilder scriptFileBuilder = builder.newScriptFile(name, path);
        return processRemainingProperties(scriptFileBuilder, properties);
    }

    private AppenderComponentBuilder createAppender(final String key, final Properties properties) {
        String name = properties.getProperty(CONFIG_NAME);
        if (Strings.isEmpty(name)) {
            throw new ConfigurationException("No name attribute provided for Appender " + key);
        }
        properties.remove(CONFIG_NAME);
        String type = properties.getProperty(CONFIG_TYPE);
        if (Strings.isEmpty(type)) {
            throw new ConfigurationException("No type attribute provided for Appender " + key);
        }
        properties.remove(CONFIG_TYPE);
        AppenderComponentBuilder appenderBuilder = builder.newAppender(name, type);
        addFiltersToComponent(appenderBuilder, properties);
        Properties layoutProps = PropertiesUtil.extractSubset(properties, "layout");
        if (layoutProps.size() > 0) {
            appenderBuilder.add(createLayout(name, layoutProps));
        }

        return processRemainingProperties(appenderBuilder, properties);
    }

    private FilterComponentBuilder createFilter(final String key, final Properties properties) {
        String type = properties.getProperty(CONFIG_TYPE);
        if (Strings.isEmpty(type)) {
            throw new ConfigurationException("No type attribute provided for Appender " + key);
        }
        properties.remove(CONFIG_TYPE);
        String onMatch = properties.getProperty("onMatch");
        if (onMatch != null) {
            properties.remove("onMatch");
        }
        String onMisMatch = properties.getProperty("onMisMatch");
        if (onMisMatch != null) {
            properties.remove("onMisMatch");
        }
        FilterComponentBuilder filterBuilder = builder.newFilter(type, onMatch, onMisMatch);
        return processRemainingProperties(filterBuilder, properties);
    }

    private AppenderRefComponentBuilder createAppenderRef(final String key, final Properties properties) {
        String ref = properties.getProperty("ref");
        if (Strings.isEmpty(ref)) {
            throw new ConfigurationException("No ref attribute provided for AppenderRef " + key);
        }
        properties.remove("ref");
        AppenderRefComponentBuilder appenderRefBuilder = builder.newAppenderRef(ref);
        String level = properties.getProperty("level");
        if (!Strings.isEmpty(level)) {
            appenderRefBuilder.addAttribute("level", level);
        }
        return addFiltersToComponent(appenderRefBuilder, properties);
    }

    private LoggerComponentBuilder createLogger(final String key, final Properties properties) {
        String name = properties.getProperty(CONFIG_NAME);
        if (Strings.isEmpty(name)) {
            throw new ConfigurationException("No name attribute provided for Logger " + key);
        }
        properties.remove(CONFIG_NAME);
        String level = properties.getProperty("level");
        if (level != null) {
            properties.remove("level");
        }
        LoggerComponentBuilder loggerBuilder;
        String type = properties.getProperty(CONFIG_TYPE);
        if (type != null) {
            if (type.equalsIgnoreCase("asyncLogger")) {
                loggerBuilder = builder.newAsyncLogger(name, level);
            } else {
                throw new ConfigurationException("Unknown Logger type " + type + " for Logger " + name);
            }
        } else {
            loggerBuilder = builder.newLogger(name, level);
        }
        addLoggersToComponent(loggerBuilder, properties);
        addFiltersToComponent(loggerBuilder, properties);
        String additivity = properties.getProperty("additivity");
        if (!Strings.isEmpty(additivity)) {
            loggerBuilder.addAttribute("additivity", additivity);
        }
        return loggerBuilder;
    }

    private RootLoggerComponentBuilder createRootLogger(final Properties properties) {
        String level = properties.getProperty("level");
        if (level != null) {
            properties.remove("level");
        }
        RootLoggerComponentBuilder loggerBuilder;
        String type = properties.getProperty(CONFIG_TYPE);
        if (type != null) {
            if (type.equalsIgnoreCase("asyncRoot")) {
                loggerBuilder = builder.newAsyncRootLogger(level);
            } else {
                throw new ConfigurationException("Unknown Logger type for root logger" + type);
            }
        } else {
            loggerBuilder = builder.newRootLogger(level);
        }
        addLoggersToComponent(loggerBuilder, properties);
        return addFiltersToComponent(loggerBuilder, properties);
    }

    private LayoutComponentBuilder createLayout(final String appenderName, final Properties properties) {
        String type = properties.getProperty(CONFIG_TYPE);
        if (Strings.isEmpty(type)) {
            throw new ConfigurationException("No type attribute provided for Layout on Appender " + appenderName);
        }
        properties.remove(CONFIG_TYPE);
        LayoutComponentBuilder layoutBuilder = builder.newLayout(type);
        return processRemainingProperties(layoutBuilder, properties);
    }

    private static <B extends ComponentBuilder<B>> ComponentBuilder<B> createComponent(final ComponentBuilder<?> parent,
                                                                                       final String key,
                                                                                       final Properties properties) {
        String name = properties.getProperty(CONFIG_NAME);
        if (name != null) {
            properties.remove(CONFIG_NAME);
        }
        String type = properties.getProperty(CONFIG_TYPE);
        if (Strings.isEmpty(type)) {
            throw new ConfigurationException("No type attribute provided for component " + key);
        }
        properties.remove(CONFIG_TYPE);
        ComponentBuilder<B> componentBuilder = parent.getBuilder().newComponent(name, type);
        return processRemainingProperties(componentBuilder, properties);
    }

    private static <B extends ComponentBuilder<?>> B processRemainingProperties(final B builder,
                                                                                final Properties properties) {
        while (properties.size() > 0) {
            String propertyName = properties.stringPropertyNames().iterator().next();
            int index = propertyName.indexOf('.');
            if (index > 0) {
                String prefix = propertyName.substring(0, index);
                Properties componentProperties = PropertiesUtil.extractSubset(properties, prefix);
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
        final String filters = properties.getProperty("filters");
        if (filters != null) {
            properties.remove("filters");
            final String[] filterNames = filters.split(",");
            for (final String name : filterNames) {
                final String filterName = name.trim();
                final Properties filterProps = PropertiesUtil.extractSubset(properties, "filter." + filterName);
                componentBuilder.add(createFilter(filterName, filterProps));
            }
        }
        return componentBuilder;
    }

    private <B extends LoggableComponentBuilder<? extends ComponentBuilder<?>>> B addLoggersToComponent(
        final B loggerBuilder, final Properties properties) {
        final String appenderRefs = properties.getProperty("appenderRefs");
        if (appenderRefs != null) {
            properties.remove("appenderRefs");
            final String[] refNames = appenderRefs.split(",");
            for (final String refName : refNames) {
                final String appenderRef = refName.trim();
                final Properties refProps = PropertiesUtil.extractSubset(properties, "appenderRef." + appenderRef);
                loggerBuilder.add(createAppenderRef(appenderRef, refProps));
            }
        }
        return loggerBuilder;
    }
}
