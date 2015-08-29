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
package org.apache.logging.log4j.core.config.assembler.api;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;

/**
 * Interface for assembling logging configurations.
 */
public interface ConfigurationAssembler<T extends Configuration> extends Assembler<T> {

    /**
     * Set the name of the configuration.
     *
     * @param name the name of the {@link Configuration}. By default is {@code "Constructed"}.
     * @return this Assembler instance.
     */
    ConfigurationAssembler<T> setConfigurationName(String name);

    /**
     * Set the configuration source, if one exists.
     * @param configurationSource the ConfigurationSource.
     * @return this Assembler instance.
     */
    ConfigurationAssembler<T> setConfigurationSource(ConfigurationSource configurationSource);

    /**
     * Set the level of the StatusLogger.
     * @param level The logging level.
     * @return this Assembler instance.
     */
    ConfigurationAssembler<T> setStatusLevel(Level level);

    /**
     * Set whether the logging should include constructing Plugins.
     * @param verbosity "disable" will hide messages from plugin construction.
     * @return this Assembler instance.
     */
    ConfigurationAssembler<T> setVerbosity(String verbosity);

    /**
     * Set the list of packages to search for plugins.
     * @param packages The comma separated list of packages.
     * @return this Assembler instance.
     */
    ConfigurationAssembler<T> setPackages(String packages);

    /**
     * Set whether the shutdown hook should be disabled.
     * @param flag "disable" will prevent the shutdown hook from being set.
     * @return this Assembler instance.
     */
    ConfigurationAssembler<T> setShutdownHook(String flag);

    /**
     * Sets the interval at which the configuration file should be checked for changes.
     * @param intervalSeconds The number of seconds that should pass between checks of the configuration file.
     * @return this Assembler instance.
     */
    ConfigurationAssembler<T> setMonitorInterval(String intervalSeconds);

    /**
     * Adds an AppenderComponent.
     * @param assembler The AppenderAssembler with all of its attributes and sub components set.
     * @return this Assembler instance.
     */
    ConfigurationAssembler<T> add(AppenderAssembler assembler);

    /**
     * Adds a CustomLevel component.
     * @param assembler The CustomLevelAssembler with all of its attributes set.
     * @return this Assembler instance.
     */
    ConfigurationAssembler<T> add(CustomLevelAssembler assembler);

    /**
     * Add a Logger component.
     * @param assembler The LoggerAssembler with all of its attributes and sub components set.
     * @return this Assembler instance.
     */
    ConfigurationAssembler<T> add(LoggerAssembler assembler);

    /**
     * Add the root Logger component.
     * @param assembler The RootLoggerAssembler with all of its attributes and sub components set.
     * @return this Assembler instance.
     */
    ConfigurationAssembler<T> add(RootLoggerAssembler assembler);

    /**
     * Add a Filter component.
     * @param assembler the FilterAssembler with all of its attributes and sub components set.
     * @return this Assembler instance.
     */
    ConfigurationAssembler<T> add(FilterAssembler assembler);

    /**
     * Add a Property key and value.
     * @param key The property key.
     * @param value The property value.
     * @return this Assembler instance.
     */
    ConfigurationAssembler<T> addProperty(String key, String value);

    /**
     * Returns an Assembler for creating Appenders.
     * @param name The name of the Appender.
     * @param pluginName The Plugin type of the Appender.
     * @return the AppenderAssembler.
     */
    AppenderAssembler newAppender(String name, String pluginName);


    /**
     * Returns an Assembler for creating AppenderRefs.
     * @param ref The name of the Appender being referenced.
     * @return the AppenderRefAssembler.
     */
    AppenderRefAssembler newAppenderRef(String ref);

    /**
     * Returns an Assembler for creating generic components.
     * @param name The name of the component (may be null).
     * @param pluginName The Plugin type of the component.
     * @return The ComponentAssembler.
     */
    @SuppressWarnings("rawtypes")
    ComponentAssembler newComponent(String name, String pluginName);

    /**
     * Returns an Assembler for creating generic components.
     * @param name The name of the component (may be null).
     * @param pluginName The Plugin type of the component.
     * @param value The value of the component.
     * @return The ComponentAssembler.
     */
    @SuppressWarnings("rawtypes")
    ComponentAssembler<ComponentAssembler> newComponent(String name, String pluginName, String value);

    /**
     * Returns an Asssembler for creating CustomLevels
     * @param name The name of the custom level.
     * @param level The integer value to be assigned to the level.
     * @return The CustomLevelAssembler.
     */
    CustomLevelAssembler newCustomLevel(String name, int level);

    /**
     * Returns an Asssembler for creating Filters.
     * @param pluginName The Plugin type of the Filter.
     * @param onMatch "ACCEPT", "DENY", or "NEUTRAL"
     * @param onMisMatch "ACCEPT", "DENY", or "NEUTRAL"
     * @return The FilterAssembler.
     */
    FilterAssembler newFilter(String pluginName, Filter.Result onMatch, Filter.Result onMisMatch);

    /**
     * Returns an Asssembler for creating Filters.
     * @param pluginName The Plugin type of the Filter.
     * @param onMatch "ACCEPT", "DENY", or "NEUTRAL"
     * @param onMisMatch "ACCEPT", "DENY", or "NEUTRAL"
     * @return The FilterAssembler.
     */
    FilterAssembler newFilter(String pluginName, String onMatch, String onMisMatch);

    /**
     * Returns an Assembler for creating Layouts.
     * @param type The Plugin type of the Layout.
     * @return The LayoutAssembler.
     */
    LayoutAssembler newLayout(String pluginName);

    /**
     * Returns an Assembler for creating Loggers.
     * @param name The name of the Logger.
     * @param level The logging Level to be assigned to the Logger.
     * @return The LoggerAssembler.
     */
    LoggerAssembler newLogger(String name, Level level);


    /**
     * Returns an Assembler for creating Loggers.
     * @param name The name of the Logger.
     * @param level The logging Level to be assigned to the Logger.
     * @return The LoggerAssembler.
     */
    LoggerAssembler newLogger(String name, String level);

    /**
     * Returns an Assembler for creating Async Loggers.
     * @param name The name of the Logger.
     * @param level The logging Level to be assigned to the Logger.
     * @return The LoggerAssembler.
     */
    LoggerAssembler newAsyncLogger(String name, Level level);

    /**
     * Returns an Assembler for creating Async Loggers.
     * @param name The name of the Logger.
     * @param level The logging Level to be assigned to the Logger.
     * @return The LoggerAssembler.
     */
    LoggerAssembler newAsyncLogger(String name, String level);

    /**
     * Returns an Assembler for creating the root Logger.
     * @param level The logging Level to be assigned to the root Logger.
     * @return The RootLoggerAssembler.
     */
    RootLoggerAssembler newRootLogger(Level level);

    /**
     * Returns an Assembler for creating the root Logger.
     * @param level The logging Level to be assigned to the root Logger.
     * @return The RootLoggerAssembler.
     */
    RootLoggerAssembler newRootLogger(String level);


    /**
     * Returns an Assembler for creating the async root Logger.
     * @param level The logging Level to be assigned to the root Logger.
     * @return The RootLoggerAssembler.
     */
    RootLoggerAssembler newAsyncRootLogger(Level level);


    /**
     * Returns an Assembler for creating the async root Logger.
     * @param level The logging Level to be assigned to the root Logger.
     * @return The RootLoggerAssembler.
     */
    RootLoggerAssembler newAsyncRootLogger(String level);
}
