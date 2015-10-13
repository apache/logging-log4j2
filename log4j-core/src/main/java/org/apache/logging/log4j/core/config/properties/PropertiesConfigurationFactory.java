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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

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
    public PropertiesConfiguration getConfiguration(ConfigurationSource source) {
        final InputStream configStream = source.getInputStream();
        Properties properties = new Properties();
        try {
            properties.load(configStream);
        } catch (IOException ioe) {
            throw new ConfigurationException("Unable to load " + source.toString(), ioe);
        }
        ConfigurationBuilder<PropertiesConfiguration> builder = newConfigurationBuilder(PropertiesConfiguration.class);
        String value = properties.getProperty(STATUS_KEY);
        if (value != null) {
            builder.setStatusLevel(Level.toLevel(value, Level.ERROR));
        } else {
            builder.setStatusLevel(Level.ERROR);
        }
        value = properties.getProperty(SHUTDOWN_HOOK);
        if (value != null) {
            builder.setShutdownHook(value);
        }
        value = properties.getProperty(VERBOSE);
        if (value != null) {
            builder.setVerbosity(value);
        }
        value = properties.getProperty(PACKAGES);
        if (value != null) {
            builder.setPackages(value);
        }
        value = properties.getProperty(CONFIG_NAME);
        if (value != null) {
            builder.setConfigurationName(value);
        }
        value = properties.getProperty(MONITOR_INTERVAL);
        if (value != null) {
            builder.setMonitorInterval(value);
        }
        value = properties.getProperty(ADVERTISER_KEY);
        if (value != null) {
            builder.setAdvertiser(value);
        }
        Properties props = PropertiesUtil.extractSubset(properties, "property");
        for (String key : props.stringPropertyNames()) {
            builder.addProperty(key, props.getProperty(key));
        }

        String scriptProp = properties.getProperty("scripts");
        if (scriptProp != null) {
            String[] scriptNames = scriptProp.split(",");
            for (String scriptName : scriptNames) {
                String name = scriptName.trim();
                Properties scriptProps = PropertiesUtil.extractSubset(properties, "script." + name);
                String type = scriptProps.getProperty("type");
                if (type == null) {
                    throw new ConfigurationException("No type provided for script - must be Script or ScriptFile");
                }
                scriptProps.remove("type");
                if (type.equalsIgnoreCase("script")) {
                    builder.add(createScript(builder, name, scriptProps));
                } else {
                    builder.add(createScriptFile(builder, name, scriptProps));
                }
            }
        }

        Properties levelProps = PropertiesUtil.extractSubset(properties, "customLevel");
        if (levelProps.size() > 0) {
            for (String key : levelProps.stringPropertyNames()) {
                builder.add(builder.newCustomLevel(key, Integer.parseInt(props.getProperty(key))));
            }
        }

        String filterProp = properties.getProperty("filters");
        if (filterProp != null) {
            String[] filterNames = filterProp.split(",");
            for (String filterName : filterNames) {
                String name = filterName.trim();
                builder.add(createFilter(builder, name, PropertiesUtil.extractSubset(properties, "filter." + name)));
            }
        }
        String appenderProp = properties.getProperty("appenders");
        if (appenderProp != null) {
            String[] appenderNames = appenderProp.split(",");
            for (String appenderName : appenderNames) {
                String name = appenderName.trim();
                builder.add(createAppender(builder, name, PropertiesUtil.extractSubset(properties, "appender." +
                        name)));
            }
        }
        String loggerProp = properties.getProperty("loggers");
        if (loggerProp != null) {
            String[] loggerNames = loggerProp.split(",");
            for (String loggerName : loggerNames) {
                String name = loggerName.trim();
                if (!name.equals(LoggerConfig.ROOT)) {
                    builder.add(createLogger(builder, name, PropertiesUtil.extractSubset(properties, "logger." +
                            name)));
                }
            }
        }

        props = PropertiesUtil.extractSubset(properties, "rootLogger");
        if (props.size() > 0) {
            builder.add(createRootLogger(builder, props));
        }

        return builder.build();
    }


    private ScriptComponentBuilder createScript(ConfigurationBuilder<PropertiesConfiguration> builder, String key,
                                                Properties properties) {
        String name = properties.getProperty("name");
        if (name != null) {
            properties.remove("name");
        }
        String language = properties.getProperty("language");
        if (language!= null) {
            properties.remove("language");
        }
        String text = properties.getProperty("text");
        if (text != null) {
            properties.remove("text");
        }
        ScriptComponentBuilder scriptBuilder = builder.newScript(name, language, text);
        processRemainingProperties(scriptBuilder, key, properties);
        return scriptBuilder;
    }

    private ScriptFileComponentBuilder createScriptFile(ConfigurationBuilder<PropertiesConfiguration> builder, String key,
                                                Properties properties) {
        String name = properties.getProperty("name");
        if (name != null) {
            properties.remove("name");
        }
        String path = properties.getProperty("path");
        if (path != null) {
            properties.remove("path");
        }
        ScriptFileComponentBuilder scriptFileBuilder = builder.newScriptFile(name, path);
        processRemainingProperties(scriptFileBuilder, key, properties);
        return scriptFileBuilder;
    }


    private AppenderComponentBuilder createAppender(ConfigurationBuilder<PropertiesConfiguration> builder, String key,
            Properties properties) {
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
        String filters = properties.getProperty("filters");
        if (filters != null) {
            properties.remove("filters");
            String[] filterNames = filters.split(",");
            for (String filterName : filterNames) {
                filterName = filterName.trim();
                Properties filterProps = PropertiesUtil.extractSubset(properties, "filter." + filterName);
                appenderBuilder.add(createFilter(builder, filterName, filterProps));
            }
        }
        Properties layoutProps = PropertiesUtil.extractSubset(properties, "layout");
        if (layoutProps.size() > 0) {
            appenderBuilder.add(createLayout(builder, name, layoutProps));
        }

        processRemainingProperties(appenderBuilder, name, properties);
        return appenderBuilder;
    }

    private FilterComponentBuilder createFilter(ConfigurationBuilder<PropertiesConfiguration> builder, String key,
            Properties properties) {
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
        processRemainingProperties(filterBuilder, key, properties);
        return filterBuilder;
    }

    private AppenderRefComponentBuilder createAppenderRef(ConfigurationBuilder<PropertiesConfiguration> builder,
            String key, Properties properties) {
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
        String filters = properties.getProperty("filters");
        if (filters != null) {
            properties.remove("filters");
            String[] filterNames = filters.split(",");
            for (String filterName : filterNames) {
                filterName = filterName.trim();
                Properties filterProps = PropertiesUtil.extractSubset(properties, "filter." + filterName);
                appenderRefBuilder.add(createFilter(builder, filterName, filterProps));
            }
        }
        return appenderRefBuilder;
    }

    private LoggerComponentBuilder createLogger(ConfigurationBuilder<PropertiesConfiguration> builder, String key,
            Properties properties) {
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
        String appenderRefs = properties.getProperty("appenderRefs");
        if (appenderRefs != null) {
            properties.remove("appenderRefs");
            String[] refNames = appenderRefs.split(",");
            for (String appenderRef : refNames) {
                appenderRef = appenderRef.trim();
                Properties refProps = PropertiesUtil.extractSubset(properties, "appenderRef." + appenderRef);
                loggerBuilder.add(createAppenderRef(builder, appenderRef, refProps));
            }
        }
        String filters = properties.getProperty("filters");
        if (filters != null) {
            properties.remove("filters");
            String[] filterNames = filters.split(",");
            for (String filterName : filterNames) {
                filterName = filterName.trim();
                Properties filterProps = PropertiesUtil.extractSubset(properties, "filter." + filterName);
                loggerBuilder.add(createFilter(builder, filterName, filterProps));
            }
        }
        String additivity = properties.getProperty("additivity");
        if (!Strings.isEmpty(additivity)) {
            loggerBuilder.addAttribute("additivity", additivity);
        }
        return loggerBuilder;
    }

    private RootLoggerComponentBuilder createRootLogger(ConfigurationBuilder<PropertiesConfiguration> builder,
            Properties properties) {
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
        String appenderRefs = properties.getProperty("appenderRefs");
        if (appenderRefs != null) {
            properties.remove("appenderRefs");
            String[] refNames = appenderRefs.split(",");
            for (String appenderRef : refNames) {
                appenderRef = appenderRef.trim();
                Properties refProps = PropertiesUtil.extractSubset(properties, "appenderRef." + appenderRef);
                loggerBuilder.add(createAppenderRef(builder, appenderRef, refProps));
            }
        }
        String filters = properties.getProperty("filters");
        if (filters != null) {
            properties.remove("filters");
            String[] filterNames = filters.split(",");
            for (String filterName : filterNames) {
                filterName = filterName.trim();
                Properties filterProps = PropertiesUtil.extractSubset(properties, "filter." + filterName);
                loggerBuilder.add(createFilter(builder, filterName, filterProps));
            }
        }
        return loggerBuilder;
    }

    private LayoutComponentBuilder createLayout(ConfigurationBuilder<PropertiesConfiguration> builder,
            String appenderName, Properties properties) {
        String type = properties.getProperty(CONFIG_TYPE);
        if (Strings.isEmpty(type)) {
            throw new ConfigurationException("No type attribute provided for Layout on Appender " + appenderName);
        }
        properties.remove(CONFIG_TYPE);
        LayoutComponentBuilder layoutBuilder = builder.newLayout(type);
        processRemainingProperties(layoutBuilder, appenderName, properties);
        return layoutBuilder;
    }

    private <B extends ComponentBuilder<B>> ComponentBuilder<B> createComponent(ComponentBuilder<?> parent, String key,
            Properties properties) {
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
        processRemainingProperties(componentBuilder, name, properties);
        return componentBuilder;
    }

    private void processRemainingProperties(ComponentBuilder<?> builder, String name, Properties properties) {
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
    }
}
