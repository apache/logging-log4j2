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
package org.apache.logging.log4j.core.config.assembler.impl;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.assembler.api.AppenderAssembler;
import org.apache.logging.log4j.core.config.assembler.api.AppenderRefAssembler;
import org.apache.logging.log4j.core.config.assembler.api.Component;
import org.apache.logging.log4j.core.config.assembler.api.ComponentAssembler;
import org.apache.logging.log4j.core.config.assembler.api.ConfigurationAssembler;
import org.apache.logging.log4j.core.config.assembler.api.CustomLevelAssembler;
import org.apache.logging.log4j.core.config.assembler.api.FilterAssembler;
import org.apache.logging.log4j.core.config.assembler.api.LayoutAssembler;
import org.apache.logging.log4j.core.config.assembler.api.LoggerAssembler;
import org.apache.logging.log4j.core.config.assembler.api.RootLoggerAssembler;

import java.lang.reflect.Constructor;
import java.util.List;

/**
 *
 */
public class DefaultConfigurationAssembler<T extends AssembledConfiguration> implements ConfigurationAssembler<T> {

    private AssembledConfiguration configuration;

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

    public DefaultConfigurationAssembler() {
        this(AssembledConfiguration.class);
        root.addAttribute("name", "Assembled");
    }

    public <T extends AssembledConfiguration> DefaultConfigurationAssembler(Class<T> clazz) {
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
    public ConfigurationAssembler<T> setConfigurationName(String name) {
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
    public ConfigurationAssembler<T> setConfigurationSource(ConfigurationSource configurationSource) {
        source = configurationSource;
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ConfigurationAssembler<T> setMonitorInterval(String intervalSeconds) {
        monitorInterval = Integer.parseInt(intervalSeconds);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ConfigurationAssembler<T> setStatusLevel(Level level) {
        this.level = level;
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ConfigurationAssembler<T> setVerbosity(String verbosity) {
        this.verbosity = verbosity;
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ConfigurationAssembler<T> setPackages(String packages) {
        this.packages = packages;
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ConfigurationAssembler<T> setShutdownHook(String flag) {
        this.shutdownFlag = flag;
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ConfigurationAssembler<T> add(AppenderAssembler assembler) {
        appenders.getComponents().add(assembler.assemble());
        return this;
    }

    @Override
    public ConfigurationAssembler<T> add(CustomLevelAssembler assembler) {
        customLevels.getComponents().add(assembler.assemble());
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ConfigurationAssembler<T> add(LoggerAssembler assembler) {
        loggers.getComponents().add(assembler.assemble());
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ConfigurationAssembler<T> add(RootLoggerAssembler assembler) {
        for (Component c : loggers.getComponents()) {
            if (c.getPluginType().equals("root")) {
                throw new ConfigurationException("root Logger was previously defined");
            }
        }
        loggers.getComponents().add(assembler.assemble());
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ConfigurationAssembler<T> add(FilterAssembler assembler) {
        filters.getComponents().add(assembler.assemble());
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ConfigurationAssembler<T> addProperty(String key, String value) {
        properties.addComponent(newComponent(key, "Property", value).assemble());
        return this;
    }

    @Override
    public AppenderAssembler newAppender(String name, String type) {
        return new DefaultAppenderAssembler(this, name, type);
    }


    @Override
    public AppenderRefAssembler newAppenderRef(String ref) {
        return new DefaultAppenderRefAssembler(this, ref);
    }


    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ComponentAssembler<ComponentAssembler> newComponent(String name, String type) {
        return new DefaultComponentAssembler(this, name, type);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ComponentAssembler<ComponentAssembler> newComponent(String name, String type, String value) {
        return new DefaultComponentAssembler(this, name, type, value);
    }

    @Override
    public CustomLevelAssembler newCustomLevel(String name, int level) {
        return new DefaultCustomLevelAssembler(this, name, level);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public FilterAssembler newFilter(String type, String onMatch, String onMisMatch) {
        return new DefaultFilterAssembler(this, type, onMatch, onMisMatch);
    }


    @Override
    @SuppressWarnings("rawtypes")
    public FilterAssembler newFilter(String type, Filter.Result onMatch, Filter.Result onMisMatch) {
        return new DefaultFilterAssembler(this, type, onMatch.name(), onMisMatch.name());
    }

    @Override
    public LayoutAssembler newLayout(String type) {
        return new DefaultLayoutAssembler(this, type);
    }

    @Override
    public LoggerAssembler newLogger(String name, String level) {
        return new DefaultLoggerAssembler(this, name, level);
    }

    @Override
    public LoggerAssembler newLogger(String name, Level level) {
        return new DefaultLoggerAssembler(this, name, level.toString());
    }

    @Override
    public LoggerAssembler newAsyncLogger(String name, String level) {
        return new DefaultLoggerAssembler(this, name, level, "AsyncLogger");
    }

    @Override
    public LoggerAssembler newAsyncLogger(String name, Level level) {
        return new DefaultLoggerAssembler(this, name, level.toString(), "AsyncLogger");
    }

    @Override
    public RootLoggerAssembler newRootLogger(String level) {
        return new DefaultRootLoggerAssembler(this, level);
    }

    @Override
    public RootLoggerAssembler newRootLogger(Level level) {
        return new DefaultRootLoggerAssembler(this, level.toString());
    }

    @Override
    public RootLoggerAssembler newAsyncRootLogger(String level) {
        return new DefaultRootLoggerAssembler(this, level, "AsyncRoot");
    }

    @Override
    public RootLoggerAssembler newAsyncRootLogger(Level level) {
        return new DefaultRootLoggerAssembler(this, level.toString(), "AsyncRoot");
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public T assemble() {
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
        return (T)configuration;
    }
}
