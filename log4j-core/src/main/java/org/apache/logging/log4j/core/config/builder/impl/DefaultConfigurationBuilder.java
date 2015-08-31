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
package org.apache.logging.log4j.core.config.builder.impl;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.AppenderRefComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.Component;
import org.apache.logging.log4j.core.config.builder.api.ComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.CustomLevelComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.FilterComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.LoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;

import java.lang.reflect.Constructor;
import java.util.List;

/**
 *
 */
public class DefaultConfigurationBuilder<T extends AssembledConfiguration> implements ConfigurationBuilder<T> {

    private final Component root = new Component();
    private Component loggers;
    private Component appenders;
    private Component filters;
    private Component properties;
    private Component customLevels;
    private final Class<?> clazz;
    private ConfigurationSource source;
    private int monitorInterval = 0;
    private Level level = null;
    private String verbosity = null;
    private String packages = null;
    private String shutdownFlag = null;
    private String name = null;

    /**
     * The key with which Apache Log4j loads the selector class.
     *
     * @see <a href=
     *      "http://logging.apache.org/log4j/2.0/manual/async.html">
     *      Async Loggers</a>
     */
    private static final String LOG4J_ASYNC_LOGGERS = "Log4jContextSelector";

    public DefaultConfigurationBuilder() {
        this(AssembledConfiguration.class);
        root.addAttribute("name", "Assembled");
    }

    public <T extends AssembledConfiguration> DefaultConfigurationBuilder(Class<T> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("A Configuration class must be provided");
        }
        this.clazz = clazz;
        List<Component> components = root.getComponents();
        properties = new Component("Properties");
        components.add(properties);
        customLevels = new Component("CustomLevels");
        components.add(customLevels);
        filters = new Component("Filters");
        components.add(filters);
        appenders = new Component("Appenders");
        components.add(appenders);
        loggers = new Component("Loggers");
        components.add(loggers);
    }

    /**
     * Set the name of the configuration.
     *
     * @param name the name of the {@link Configuration}. By default is {@code "Assembled"}.
     * @return this builder instance
     */
    @Override
    @SuppressWarnings("unchecked")
    public ConfigurationBuilder<T> setConfigurationName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Set the ConfigurationSource.
     *
     * @param configurationSource the {@link ConfigurationSource).}
     * @return this builder instance
     */
    @Override
    @SuppressWarnings("unchecked")
    public ConfigurationBuilder<T> setConfigurationSource(ConfigurationSource configurationSource) {
        source = configurationSource;
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ConfigurationBuilder<T> setMonitorInterval(String intervalSeconds) {
        monitorInterval = Integer.parseInt(intervalSeconds);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ConfigurationBuilder<T> setStatusLevel(Level level) {
        this.level = level;
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ConfigurationBuilder<T> setVerbosity(String verbosity) {
        this.verbosity = verbosity;
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ConfigurationBuilder<T> setPackages(String packages) {
        this.packages = packages;
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ConfigurationBuilder<T> setShutdownHook(String flag) {
        this.shutdownFlag = flag;
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ConfigurationBuilder<T> add(AppenderComponentBuilder assembler) {
        appenders.getComponents().add(assembler.build());
        return this;
    }

    @Override
    public ConfigurationBuilder<T> add(CustomLevelComponentBuilder assembler) {
        customLevels.getComponents().add(assembler.build());
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ConfigurationBuilder<T> add(LoggerComponentBuilder assembler) {
        loggers.getComponents().add(assembler.build());
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ConfigurationBuilder<T> add(RootLoggerComponentBuilder assembler) {
        for (Component c : loggers.getComponents()) {
            if (c.getPluginType().equals("root")) {
                throw new ConfigurationException("root Logger was previously defined");
            }
        }
        loggers.getComponents().add(assembler.build());
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ConfigurationBuilder<T> add(FilterComponentBuilder assembler) {
        filters.getComponents().add(assembler.build());
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ConfigurationBuilder<T> addProperty(String key, String value) {
        properties.addComponent(newComponent(key, "Property", value).build());
        return this;
    }

    @Override
    public AppenderComponentBuilder newAppender(String name, String type) {
        return new DefaultAppenderComponentBuilder(this, name, type);
    }


    @Override
    public AppenderRefComponentBuilder newAppenderRef(String ref) {
        return new DefaultAppenderRefComponentBuilder(this, ref);
    }


    @Override
    public ComponentBuilder<ComponentBuilder<?>> newComponent(String name, String type) {
        return new DefaultComponentBuilder<>(this, name, type);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ComponentBuilder<ComponentBuilder> newComponent(String name, String type, String value) {
        return new DefaultComponentBuilder<>(this, name, type, value);
    }

    @Override
    public CustomLevelComponentBuilder newCustomLevel(String name, int level) {
        return new DefaultCustomLevelComponentBuilder(this, name, level);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public FilterComponentBuilder newFilter(String type, String onMatch, String onMisMatch) {
        return new DefaultFilterComponentBuilder(this, type, onMatch, onMisMatch);
    }


    @Override
    @SuppressWarnings("rawtypes")
    public FilterComponentBuilder newFilter(String type, Filter.Result onMatch, Filter.Result onMisMatch) {
        return new DefaultFilterComponentBuilder(this, type, onMatch.name(), onMisMatch.name());
    }

    @Override
    public LayoutComponentBuilder newLayout(String type) {
        return new DefaultLayoutComponentBuilder(this, type);
    }

    @Override
    public LoggerComponentBuilder newLogger(String name, String level) {
        return new DefaultLoggerComponentBuilder(this, name, level);
    }

    @Override
    public LoggerComponentBuilder newLogger(String name, Level level) {
        return new DefaultLoggerComponentBuilder(this, name, level.toString());
    }

    @Override
    public LoggerComponentBuilder newAsyncLogger(String name, String level) {
        return new DefaultLoggerComponentBuilder(this, name, level, "AsyncLogger");
    }

    @Override
    public LoggerComponentBuilder newAsyncLogger(String name, Level level) {
        return new DefaultLoggerComponentBuilder(this, name, level.toString(), "AsyncLogger");
    }

    @Override
    public RootLoggerComponentBuilder newRootLogger(String level) {
        return new DefaultRootLoggerComponentBuilder(this, level);
    }

    @Override
    public RootLoggerComponentBuilder newRootLogger(Level level) {
        return new DefaultRootLoggerComponentBuilder(this, level.toString());
    }

    @Override
    public RootLoggerComponentBuilder newAsyncRootLogger(String level) {
        return new DefaultRootLoggerComponentBuilder(this, level, "AsyncRoot");
    }

    @Override
    public RootLoggerComponentBuilder newAsyncRootLogger(Level level) {
        return new DefaultRootLoggerComponentBuilder(this, level.toString(), "AsyncRoot");
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public T build() {
        AssembledConfiguration configuration;
        try {
            if (source == null) {
                source = ConfigurationSource.NULL_SOURCE;
            }
            Constructor constructor = clazz.getConstructor(ConfigurationSource.class, Component.class);
            configuration = (AssembledConfiguration) constructor.newInstance(source, root);
            configuration.setMonitorInterval(monitorInterval);
            if (name != null) {
                configuration.setName(name);
            }
            if (level != null) {
                configuration.getStatusConfiguration().withStatus(level);
            }
            if (verbosity != null) {
                configuration.getStatusConfiguration().withVerbosity(verbosity);
            }
            if (packages != null) {
                configuration.setPluginPackages(packages);
            }
            if (shutdownFlag != null) {
                configuration.setShutdownHook(shutdownFlag);
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid Configuration class specified", ex);
        }
        configuration.getStatusConfiguration().initialize();
        configuration.initialize();
        return (T)configuration;
    }
}
