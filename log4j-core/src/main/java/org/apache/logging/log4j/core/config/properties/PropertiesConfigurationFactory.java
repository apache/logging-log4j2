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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Order;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.AppenderRefComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.FilterComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.LoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ScriptComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ScriptFileComponentBuilder;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.Strings;

/**
 * Creates a PropertiesConfiguration from a properties file.
 * 
 * @since 2.4
 */
@Plugin(name = "PropertiesConfigurationFactory", category = ConfigurationFactory.CATEGORY)
@Order(8)
public class PropertiesConfigurationFactory extends ConfigurationFactory {
    private static final String ADVERTISER_KEY = "advertiser";
    private static final String STATUS_KEY = "status";
    private static final String SHUTDOWN_HOOK = "shutdownHook";
    private static final String VERBOSE = "verbose";
    private static final String PACKAGES = "packages";
    private static final String CONFIG_NAME = "name";
    private static final String MONITOR_INTERVAL = "monitorInterval";
    private static final String CONFIG_TYPE = "type";

    @Override
    protected String[] getSupportedTypes() {
        return new String[] {".properties"};
    }

    @Override
    public PropertiesConfiguration getConfiguration(final ConfigurationSource source) {
        final InputStream configStream = source.getInputStream();
        final Properties properties = new Properties();
        try {
            properties.load(configStream);
        } catch (IOException ioe) {
            throw new ConfigurationException("Unable to load " + source.toString(), ioe);
        }
        final ConfigurationBuilder<PropertiesConfiguration> builder =
            newConfigurationBuilder(PropertiesConfiguration.class)
            .setStatusLevel(Level.toLevel(properties.getProperty(STATUS_KEY), Level.ERROR))
            .setShutdownHook(properties.getProperty(SHUTDOWN_HOOK))
            .setVerbosity(properties.getProperty(VERBOSE))
            .setPackages(properties.getProperty(PACKAGES))
            .setConfigurationName(properties.getProperty(CONFIG_NAME))
            .setMonitorInterval(properties.getProperty(MONITOR_INTERVAL))
            .setAdvertiser(properties.getProperty(ADVERTISER_KEY));
        final Properties props = PropertiesUtil.extractSubset(properties, "property");
        for (final String key : props.stringPropertyNames()) {
            builder.addProperty(key, props.getProperty(key));
        }

        final String scriptProp = properties.getProperty("scripts");
        if (scriptProp != null) {
            final String[] scriptNames = scriptProp.split(",");
            for (String scriptName : scriptNames) {
                final String name = scriptName.trim();
                final Properties scriptProps = PropertiesUtil.extractSubset(properties, "script." + name);
                final String type = scriptProps.getProperty("type");
                if (type == null) {
                    throw new ConfigurationException("No type provided for script - must be Script or ScriptFile");
                }
                scriptProps.remove("type");
                if (type.equalsIgnoreCase("script")) {
                    builder.add(createScript(builder, scriptProps));
                } else {
                    builder.add(createScriptFile(builder, scriptProps));
                }
            }
        }

        final Properties levelProps = PropertiesUtil.extractSubset(properties, "customLevel");
        if (levelProps.size() > 0) {
            for (final String key : levelProps.stringPropertyNames()) {
                builder.add(builder.newCustomLevel(key, Integer.parseInt(props.getProperty(key))));
            }
        }

        final String filterProp = properties.getProperty("filters");
        if (filterProp != null) {
            final String[] filterNames = filterProp.split(",");
            for (final String filterName : filterNames) {
                final String name = filterName.trim();
                builder.add(createFilter(builder, name, PropertiesUtil.extractSubset(properties, "filter." + name)));
            }
        }
        final String appenderProp = properties.getProperty("appenders");
        if (appenderProp != null) {
            final String[] appenderNames = appenderProp.split(",");
            for (final String appenderName : appenderNames) {
                final String name = appenderName.trim();
                builder.add(createAppender(builder, name, PropertiesUtil.extractSubset(properties, "appender." +
                        name)));
            }
        }
        final String loggerProp = properties.getProperty("loggers");
        if (loggerProp != null) {
            final String[] loggerNames = loggerProp.split(",");
            for (final String loggerName : loggerNames) {
                final String name = loggerName.trim();
                if (!name.equals(LoggerConfig.ROOT)) {
                    builder.add(createLogger(builder, name, PropertiesUtil.extractSubset(properties, "logger." +
                            name)));
                }
            }
        }

        final Properties rootLogger = PropertiesUtil.extractSubset(properties, "rootLogger");
        if (rootLogger.size() > 0) {
            builder.add(createRootLogger(builder, rootLogger));
        }

        return builder.build();
    }


    private ScriptComponentBuilder createScript(final ConfigurationBuilder<PropertiesConfiguration> builder,
                                                final Properties properties) {
        final String name = properties.getProperty("name");
        if (name != null) {
            properties.remove("name");
        }
        final String language = properties.getProperty("language");
        if (language!= null) {
            properties.remove("language");
        }
        final String text = properties.getProperty("text");
        if (text != null) {
            properties.remove("text");
        }
        final ScriptComponentBuilder scriptBuilder = builder.newScript(name, language, text);
        processRemainingProperties(scriptBuilder, properties);
        return scriptBuilder;
    }

    private ScriptFileComponentBuilder createScriptFile(final ConfigurationBuilder<PropertiesConfiguration> builder,
                                                        final Properties properties) {
        final String name = properties.getProperty("name");
        if (name != null) {
            properties.remove("name");
        }
        final String path = properties.getProperty("path");
        if (path != null) {
            properties.remove("path");
        }
        final ScriptFileComponentBuilder scriptFileBuilder = builder.newScriptFile(name, path);
        processRemainingProperties(scriptFileBuilder, properties);
        return scriptFileBuilder;
    }


    private AppenderComponentBuilder createAppender(final ConfigurationBuilder<PropertiesConfiguration> builder,
                                                    final String key, final Properties properties) {
        final String name = properties.getProperty(CONFIG_NAME);
        if (Strings.isEmpty(name)) {
            throw new ConfigurationException("No name attribute provided for Appender " + key);
        }
        properties.remove(CONFIG_NAME);
        final String type = properties.getProperty(CONFIG_TYPE);
        if (Strings.isEmpty(type)) {
            throw new ConfigurationException("No type attribute provided for Appender " + key);
        }
        properties.remove(CONFIG_TYPE);
        final AppenderComponentBuilder appenderBuilder = builder.newAppender(name, type);
        final String filters = properties.getProperty("filters");
        if (filters != null) {
            properties.remove("filters");
            final String[] filterNames = filters.split(",");
            for (final String filterName : filterNames) {
                final String filter = filterName.trim();
                final Properties filterProps = PropertiesUtil.extractSubset(properties, "filter." + filter);
                appenderBuilder.add(createFilter(builder, filter, filterProps));
            }
        }
        final Properties layoutProps = PropertiesUtil.extractSubset(properties, "layout");
        if (layoutProps.size() > 0) {
            appenderBuilder.add(createLayout(builder, name, layoutProps));
        }

        processRemainingProperties(appenderBuilder, properties);
        return appenderBuilder;
    }

    private FilterComponentBuilder createFilter(final ConfigurationBuilder<PropertiesConfiguration> builder,
                                                final String key, final Properties properties) {
        final String type = properties.getProperty(CONFIG_TYPE);
        if (Strings.isEmpty(type)) {
            throw new ConfigurationException("No type attribute provided for Appender " + key);
        }
        properties.remove(CONFIG_TYPE);
        final String onMatch = properties.getProperty("onMatch");
        if (onMatch != null) {
            properties.remove("onMatch");
        }
        final String onMisMatch = properties.getProperty("onMisMatch");
        if (onMisMatch != null) {
            properties.remove("onMisMatch");
        }
        final FilterComponentBuilder filterBuilder = builder.newFilter(type, onMatch, onMisMatch);
        processRemainingProperties(filterBuilder, properties);
        return filterBuilder;
    }

    private AppenderRefComponentBuilder createAppenderRef(final ConfigurationBuilder<PropertiesConfiguration> builder,
                                                          final String key, final Properties properties) {
        final String ref = properties.getProperty("ref");
        if (Strings.isEmpty(ref)) {
            throw new ConfigurationException("No ref attribute provided for AppenderRef " + key);
        }
        properties.remove("ref");
        final AppenderRefComponentBuilder appenderRefBuilder = builder.newAppenderRef(ref);
        final String level = properties.getProperty("level");
        if (!Strings.isEmpty(level)) {
            appenderRefBuilder.addAttribute("level", level);
        }
        final String filters = properties.getProperty("filters");
        if (filters != null) {
            properties.remove("filters");
            final String[] filterNames = filters.split(",");
            for (final String filterName : filterNames) {
                final String filter = filterName.trim();
                final Properties filterProps = PropertiesUtil.extractSubset(properties, "filter." + filter);
                appenderRefBuilder.add(createFilter(builder, filter, filterProps));
            }
        }
        return appenderRefBuilder;
    }

    private LoggerComponentBuilder createLogger(final ConfigurationBuilder<PropertiesConfiguration> builder,
                                                final String key, final Properties properties) {
        final String name = properties.getProperty(CONFIG_NAME);
        if (Strings.isEmpty(name)) {
            throw new ConfigurationException("No name attribute provided for Logger " + key);
        }
        properties.remove(CONFIG_NAME);
        final String level = properties.getProperty("level");
        if (level != null) {
            properties.remove("level");
        }
        final LoggerComponentBuilder loggerBuilder;
        final String type = properties.getProperty(CONFIG_TYPE);
        if (type != null) {
            if (type.equalsIgnoreCase("asyncLogger")) {
                loggerBuilder = builder.newAsyncLogger(name, level);
            } else {
                throw new ConfigurationException("Unknown Logger type " + type + " for Logger " + name);
            }
        } else {
            loggerBuilder = builder.newLogger(name, level);
        }
        createAppenderRefs(builder, properties, loggerBuilder);
        final String filters = properties.getProperty("filters");
        if (filters != null) {
            properties.remove("filters");
            final String[] filterNames = filters.split(",");
            for (final String filterName : filterNames) {
                final String filter = filterName.trim();
                final Properties filterProps = PropertiesUtil.extractSubset(properties, "filter." + filter);
                loggerBuilder.add(createFilter(builder, filter, filterProps));
            }
        }
        final String additivity = properties.getProperty("additivity");
        if (!Strings.isEmpty(additivity)) {
            loggerBuilder.addAttribute("additivity", additivity);
        }
        return loggerBuilder;
    }

    private void createAppenderRefs(ConfigurationBuilder<PropertiesConfiguration> builder, Properties properties,
                                    LoggerComponentBuilder loggerBuilder) {
        final String appenderRefs = properties.getProperty("appenderRefs");
        if (appenderRefs != null) {
            properties.remove("appenderRefs");
            final String[] refNames = appenderRefs.split(",");
            for (final String appenderRef : refNames) {
                final String appender = appenderRef.trim();
                final Properties refProps = PropertiesUtil.extractSubset(properties, "appenderRef." + appender);
                loggerBuilder.add(createAppenderRef(builder, appender, refProps));
            }
        }
    }

    private RootLoggerComponentBuilder createRootLogger(final ConfigurationBuilder<PropertiesConfiguration> builder,
                                                        final Properties properties) {
        final String level = properties.getProperty("level");
        if (level != null) {
            properties.remove("level");
        }
        final RootLoggerComponentBuilder loggerBuilder;
        final String type = properties.getProperty(CONFIG_TYPE);
        if (type != null) {
            if (type.equalsIgnoreCase("asyncRoot")) {
                loggerBuilder = builder.newAsyncRootLogger(level);
            } else {
                throw new ConfigurationException("Unknown Logger type for root logger" + type);
            }
        } else {
            loggerBuilder = builder.newRootLogger(level);
        }
        final String appenderRefs = properties.getProperty("appenderRefs");
        if (appenderRefs != null) {
            properties.remove("appenderRefs");
            final String[] refNames = appenderRefs.split(",");
            for (final String appenderRef : refNames) {
                final String appender = appenderRef.trim();
                final Properties refProps = PropertiesUtil.extractSubset(properties, "appenderRef." + appender);
                loggerBuilder.add(createAppenderRef(builder, appender, refProps));
            }
        }
        final String filters = properties.getProperty("filters");
        if (filters != null) {
            properties.remove("filters");
            final String[] filterNames = filters.split(",");
            for (final String filterName : filterNames) {
                final String filter = filterName.trim();
                final Properties filterProps = PropertiesUtil.extractSubset(properties, "filter." + filter);
                loggerBuilder.add(createFilter(builder, filter, filterProps));
            }
        }
        return loggerBuilder;
    }

    private LayoutComponentBuilder createLayout(final ConfigurationBuilder<PropertiesConfiguration> builder,
                                                final String appenderName, final Properties properties) {
        final String type = properties.getProperty(CONFIG_TYPE);
        if (Strings.isEmpty(type)) {
            throw new ConfigurationException("No type attribute provided for Layout on Appender " + appenderName);
        }
        properties.remove(CONFIG_TYPE);
        final LayoutComponentBuilder layoutBuilder = builder.newLayout(type);
        processRemainingProperties(layoutBuilder, properties);
        return layoutBuilder;
    }

    private <B extends ComponentBuilder<B>> ComponentBuilder<B> createComponent(final ComponentBuilder<?> parent,
                                                                                final String key, final Properties properties) {
        final String name = properties.getProperty(CONFIG_NAME);
        if (name != null) {
            properties.remove(CONFIG_NAME);
        }
        final String type = properties.getProperty(CONFIG_TYPE);
        if (Strings.isEmpty(type)) {
            throw new ConfigurationException("No type attribute provided for component " + key);
        }
        properties.remove(CONFIG_TYPE);
        final ComponentBuilder<B> componentBuilder = parent.getBuilder().newComponent(name, type);
        processRemainingProperties(componentBuilder, properties);
        return componentBuilder;
    }

    private void processRemainingProperties(final ComponentBuilder<?> builder, final Properties properties) {
        while (properties.size() > 0) {
            final String propertyName = properties.stringPropertyNames().iterator().next();

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
    }
}
