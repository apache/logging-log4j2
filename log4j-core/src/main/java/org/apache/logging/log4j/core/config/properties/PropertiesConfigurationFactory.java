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
import org.apache.logging.log4j.core.config.Order;
import org.apache.logging.log4j.core.config.assembler.api.AppenderAssembler;
import org.apache.logging.log4j.core.config.assembler.api.AppenderRefAssembler;
import org.apache.logging.log4j.core.config.assembler.api.ComponentAssembler;
import org.apache.logging.log4j.core.config.assembler.api.ConfigurationAssembler;
import org.apache.logging.log4j.core.config.assembler.api.FilterAssembler;
import org.apache.logging.log4j.core.config.assembler.api.LayoutAssembler;
import org.apache.logging.log4j.core.config.assembler.api.LoggerAssembler;
import org.apache.logging.log4j.core.config.assembler.api.RootLoggerAssembler;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.util.PropertiesUtil;
import org.apache.logging.log4j.util.Strings;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Creates a PropertiesConfiguration from a properties file.
 */
@Plugin(name = "PropertiesConfigurationFactory", category = ConfigurationFactory.CATEGORY)
@Order(8)
public class PropertiesConfigurationFactory extends ConfigurationFactory {
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
        ConfigurationAssembler<PropertiesConfiguration> assembler = newConfiguration(PropertiesConfiguration.class);
        String value = properties.getProperty(STATUS_KEY);
        if (value != null) {
            assembler.setStatusLevel(Level.toLevel(value, Level.ERROR));
        } else {
            assembler.setStatusLevel(Level.ERROR);
        }
        value = properties.getProperty(SHUTDOWN_HOOK);
        if (value != null) {
            assembler.setShutdownHook(value);
        }
        value = properties.getProperty(VERBOSE);
        if (value != null) {
            assembler.setVerbosity(value);
        }
        value = properties.getProperty(PACKAGES);
        if (value != null) {
            assembler.setPackages(value);
        }
        value = properties.getProperty(CONFIG_NAME);
        if (value != null) {
            assembler.setConfigurationName(value);
        }
        value = properties.getProperty(MONITOR_INTERVAL);
        if (value != null) {
            assembler.setMonitorInterval(value);
        }
        Properties props = PropertiesUtil.extractSubset(properties, "property");
        for (String key : props.stringPropertyNames()) {
            assembler.addProperty(key, props.getProperty(key));
        }

        Properties levelProps = PropertiesUtil.extractSubset(properties, "customLevel");
        if (levelProps.size() > 0) {
            for (String key : levelProps.stringPropertyNames()) {
                assembler.add(assembler.newCustomLevel(key, Integer.parseInt(props.getProperty(key))));
            }
        }

        String filterProp = properties.getProperty("filters");
        if (filterProp != null) {
            String[] filterNames = filterProp.split(",");
            for (String filterName : filterNames) {
                String name = filterName.trim();
                assembler.add(createFilter(assembler, name, PropertiesUtil.extractSubset(properties, "filter." + name)));
            }
        }
        String appenderProp = properties.getProperty("appenders");
        if (appenderProp != null) {
            String[] appenderNames = appenderProp.split(",");
            for (String appenderName : appenderNames) {
                String name = appenderName.trim();
                assembler.add(
                        createAppender(assembler, name, PropertiesUtil.extractSubset(properties, "appender." + name)));
            }
        }
        String loggerProp = properties.getProperty("loggers");
        if (appenderProp != null) {
            String[] loggerNames = loggerProp.split(",");
            for (String loggerName : loggerNames) {
                String name = loggerName.trim();
                if (!name.equals("root")) {
                    assembler.add(
                            createLogger(assembler, name, PropertiesUtil.extractSubset(properties, "logger." + name)));
                }
            }
        }

        props = PropertiesUtil.extractSubset(properties, "rootLogger");
        if (props.size() > 0) {
            assembler.add(createRootLogger(assembler, props));
        }

        return assembler.assemble();
    }

    private AppenderAssembler createAppender(ConfigurationAssembler<PropertiesConfiguration> assembler, String key, Properties properties) {
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
        AppenderAssembler appenderAssembler = assembler.newAppender(name, type);
        String filters = properties.getProperty("filters");
        if (filters != null) {
            properties.remove("filters");
            String[] filterNames = filters.split(",");
            for (String filterName : filterNames) {
                filterName = filterName.trim();
                Properties filterProps = PropertiesUtil.extractSubset(properties, "filter." + filterName);
                appenderAssembler.add(createFilter(assembler, filterName, filterProps));
            }
        }
        Properties layoutProps = PropertiesUtil.extractSubset(properties, "layout");
        if (layoutProps.size() > 0) {
            appenderAssembler.add(createLayout(assembler, name, layoutProps));
        }

        processRemainingProperties(appenderAssembler, name, properties);
        return appenderAssembler;
    }

    private FilterAssembler createFilter(ConfigurationAssembler<PropertiesConfiguration> assembler, String key, Properties properties) {
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
        FilterAssembler filterAssembler = assembler.newFilter(type, onMatch, onMisMatch);
        processRemainingProperties(filterAssembler, key, properties);
        return filterAssembler;
    }

    private AppenderRefAssembler createAppenderRef(ConfigurationAssembler<PropertiesConfiguration> assembler, String key, Properties properties) {
        String ref = properties.getProperty("ref");
        if (Strings.isEmpty(ref)) {
            throw new ConfigurationException("No ref attribute provided for AppenderRef " + key);
        }
        properties.remove("ref");
        AppenderRefAssembler appenderRefAssembler = assembler.newAppenderRef(ref);
        String level = properties.getProperty("level");
        if (!Strings.isEmpty(level)) {
            appenderRefAssembler.addAttribute("level", level);
        }
        String filters = properties.getProperty("filters");
        if (filters != null) {
            properties.remove("filters");
            String[] filterNames = filters.split(",");
            for (String filterName : filterNames) {
                filterName = filterName.trim();
                Properties filterProps = PropertiesUtil.extractSubset(properties, "filter." + filterName);
                appenderRefAssembler.add(createFilter(assembler, filterName, filterProps));
            }
        }
        return appenderRefAssembler;
    }

    private LoggerAssembler createLogger(ConfigurationAssembler<PropertiesConfiguration> assembler, String key, Properties properties) {
        String name = properties.getProperty(CONFIG_NAME);
        if (Strings.isEmpty(name)) {
            throw new ConfigurationException("No name attribute provided for Logger " + key);
        }
        properties.remove(CONFIG_NAME);
        String level = properties.getProperty("level");
        if (level != null) {
            properties.remove("level");
        }
        LoggerAssembler loggerAssembler;
        String type = properties.getProperty(CONFIG_TYPE);
        if (type != null) {
            if (type.equalsIgnoreCase("asyncLogger")) {
                loggerAssembler = assembler.newAsyncLogger(name, level);
            } else {
                throw new ConfigurationException("Unknown Logger type " + type + " for Logger " + name);
            }
        } else {
            loggerAssembler = assembler.newLogger(name, level);
        }
        String appenderRefs = properties.getProperty("appenderRefs");
        if (appenderRefs != null) {
            properties.remove("appenderRefs");
            String[] refNames = appenderRefs.split(",");
            for (String appenderRef : refNames) {
                appenderRef = appenderRef.trim();
                Properties refProps = PropertiesUtil.extractSubset(properties, "appenderRef." + appenderRef);
                loggerAssembler.add(createAppenderRef(assembler, appenderRef, refProps));
            }
        }
        String filters = properties.getProperty("filters");
        if (filters != null) {
            properties.remove("filters");
            String[] filterNames = filters.split(",");
            for (String filterName : filterNames) {
                filterName = filterName.trim();
                Properties filterProps = PropertiesUtil.extractSubset(properties, "filter." + filterName);
                loggerAssembler.add(createFilter(assembler, filterName, filterProps));
            }
        }
        String additivity = properties.getProperty("additivity");
        if (!Strings.isEmpty(additivity)) {
            loggerAssembler.addAttribute("additivity", additivity);
        }
        return loggerAssembler;
    }

    private RootLoggerAssembler createRootLogger(ConfigurationAssembler<PropertiesConfiguration> assembler, Properties properties) {
        String level = properties.getProperty("level");
        if (level != null) {
            properties.remove("level");
        }
        RootLoggerAssembler loggerAssembler;
        String type = properties.getProperty(CONFIG_TYPE);
        if (type != null) {
            if (type.equalsIgnoreCase("asyncRoot")) {
                loggerAssembler = assembler.newAsyncRootLogger(level);
            } else {
                throw new ConfigurationException("Unknown Logger type for root logger" + type);
            }
        } else {
            loggerAssembler = assembler.newRootLogger(level);
        }
        String appenderRefs = properties.getProperty("appenderRefs");
        if (appenderRefs != null) {
            properties.remove("appenderRefs");
            String[] refNames = appenderRefs.split(",");
            for (String appenderRef : refNames) {
                appenderRef = appenderRef.trim();
                Properties refProps = PropertiesUtil.extractSubset(properties, "appenderRef." + appenderRef);
                loggerAssembler.add(createAppenderRef(assembler, appenderRef, refProps));
            }
        }
        String filters = properties.getProperty("filters");
        if (filters != null) {
            properties.remove("filters");
            String[] filterNames = filters.split(",");
            for (String filterName : filterNames) {
                filterName = filterName.trim();
                Properties filterProps = PropertiesUtil.extractSubset(properties, "filter." + filterName);
                loggerAssembler.add(createFilter(assembler, filterName, filterProps));
            }
        }
        return loggerAssembler;
    }

    private LayoutAssembler createLayout(ConfigurationAssembler<PropertiesConfiguration> assembler, String appenderName, Properties properties) {
        String type = properties.getProperty(CONFIG_TYPE);
        if (Strings.isEmpty(type)) {
            throw new ConfigurationException("No type attribute provided for Layout on Appender " + appenderName);
        }
        properties.remove(CONFIG_TYPE);
        LayoutAssembler layoutAssembler = assembler.newLayout(type);
        processRemainingProperties(layoutAssembler, appenderName, properties);
        return layoutAssembler;
    }

    @SuppressWarnings("rawtypes")
    private ComponentAssembler<?> createComponent(ComponentAssembler parent, String key, Properties properties) {
        String name = properties.getProperty(CONFIG_NAME);
        if (name != null) {
            properties.remove(CONFIG_NAME);
        }
        String type = properties.getProperty(CONFIG_TYPE);
        if (Strings.isEmpty(type)) {
            throw new ConfigurationException("No type attribute provided for component " + key);
        }
        properties.remove(CONFIG_TYPE);
        @SuppressWarnings("unchecked")
        ComponentAssembler<?> componentAssembler = parent.getAssembler().newComponent(name, type);
        processRemainingProperties(componentAssembler, name, properties);
        return componentAssembler;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void processRemainingProperties(ComponentAssembler assembler, String name, Properties properties) {
        while (properties.size() > 0) {
            String propertyName = properties.stringPropertyNames().iterator().next();

            int index = propertyName.indexOf('.');
            if (index > 0) {
                String prefix = propertyName.substring(0, index);
                Properties componentProperties = PropertiesUtil.extractSubset(properties, prefix);
                assembler.addComponent(createComponent(assembler, prefix, componentProperties));
            } else  {
                assembler.addAttribute(propertyName, properties.getProperty(propertyName));
                properties.remove(propertyName);
            }
        }
    }
}
