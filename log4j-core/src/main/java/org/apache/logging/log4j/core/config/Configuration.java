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
package org.apache.logging.log4j.core.config;

import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.async.AsyncLoggerConfigDelegate;
import org.apache.logging.log4j.core.async.AsyncWaitStrategyFactory;
import org.apache.logging.log4j.core.filter.Filterable;
import org.apache.logging.log4j.core.lookup.ConfigurationStrSubstitutor;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.net.Advertiser;
import org.apache.logging.log4j.core.script.ScriptManager;
import org.apache.logging.log4j.core.util.NanoClock;
import org.apache.logging.log4j.core.util.WatchManager;

/**
 * Interface that must be implemented to create a configuration.
 * <p>
 * Custom implementations are recommended to extend {@link AbstractConfiguration}.
 * </p>
 *
 * @see AbstractConfiguration
 * @see org.apache.logging.log4j.core.LifeCycle2
 */
public interface Configuration extends Filterable {

    /** Key for storing the Context properties. */
    String CONTEXT_PROPERTIES = "ContextProperties";

    /**
     * Returns the configuration name.
     *
     * @return the name of the configuration.
     */
    String getName();

    /**
     * Locates the appropriate LoggerConfig for a Logger name. This will remove tokens from the package name as
     * necessary or return the root LoggerConfig if no other matches were found.
     *
     * @param name The Logger name.
     * @return The located LoggerConfig.
     */
    LoggerConfig getLoggerConfig(String name);

    /**
     * Returns the Appender with the specified name.
     *
     * @param <T>  The expected Appender type.
     * @param name The name of the Appender.
     * @return the Appender with the specified name or null if the Appender cannot be located.
     */
    <T extends Appender> T getAppender(String name);

    /**
     * Returns a Map containing all the Appenders and their name.
     *
     * @return A Map containing each Appender's name and the Appender object.
     */
    Map<String, Appender> getAppenders();

    void addAppender(final Appender appender);

    Map<String, LoggerConfig> getLoggers();

    void addLoggerAppender(Logger logger, Appender appender);

    void addLoggerFilter(Logger logger, Filter filter);

    void setLoggerAdditive(Logger logger, boolean additive);

    void addLogger(final String name, final LoggerConfig loggerConfig);

    void removeLogger(final String name);

    /**
     * Returns the list of packages to scan for plugins for this Configuration.
     *
     * @return the list of plugin packages.
     * @since 2.1
     */
    List<String> getPluginPackages();

    Map<String, String> getProperties();

    /**
     * Returns the root Logger.
     *
     * @return the root Logger.
     */
    LoggerConfig getRootLogger();

    void addListener(ConfigurationListener listener);

    void removeListener(ConfigurationListener listener);

    StrSubstitutor getStrSubstitutor();

    default StrSubstitutor getConfigurationStrSubstitutor() {
        final StrSubstitutor defaultSubstitutor = getStrSubstitutor();
        if (defaultSubstitutor == null) {
            return new ConfigurationStrSubstitutor();
        }
        return new ConfigurationStrSubstitutor(defaultSubstitutor);
    }

    void createConfiguration(Node node, LogEvent event);

    <T> T getComponent(String name);

    void addComponent(String name, Object object);

    void setAdvertiser(Advertiser advertiser);

    Advertiser getAdvertiser();

    boolean isShutdownHookEnabled();

    long getShutdownTimeoutMillis();

    ConfigurationScheduler getScheduler();

    /**
     * Returns the source of this configuration.
     *
     * @return the source of this configuration, never {@code null}, but may be
     * {@link org.apache.logging.log4j.core.config.ConfigurationSource#NULL_SOURCE}
     * or
     * {@link org.apache.logging.log4j.core.config.ConfigurationSource#COMPOSITE_SOURCE}
     */
    ConfigurationSource getConfigurationSource();

    /**
     * <p>
     * Returns a list of descriptors of the custom levels defined in the current configuration. The returned list does
     * <em>not</em> include custom levels that are defined in code with direct calls to {@link Level#forName(String, int)}.
     * </p>
     * <p>
     * Note that the list does not include levels of previous configurations. For example, suppose a configuration
     * contains custom levels A, B and C. The configuration is then modified to contain custom levels B, C and D. For
     * the new configuration, this method will return only {B, C, D}, that is, only the custom levels defined in
     * <em>this</em> configuration. The previously defined level A still exists (and can be obtained with
     * {@link Level#getLevel(String)}), it is just not in the current configuration. {@link Level#values()} will return
     * {A, B, C, D and the built-in levels}.
     * </p>
     *
     * @return the custom levels defined in the current configuration
     */
    List<CustomLevelConfig> getCustomLevels();

    ScriptManager getScriptManager();

    /**
     * Returns the {@code AsyncLoggerConfigDelegate} shared by all
     * {@code AsyncLoggerConfig} instances defined in this Configuration.
     *
     * @return the {@code AsyncLoggerConfigDelegate}
     */
    AsyncLoggerConfigDelegate getAsyncLoggerConfigDelegate();

    /**
     * Returns the {@code AsyncWaitStrategyFactory} defined in this Configuration;
     * this factory is used to create the LMAX disruptor {@code WaitStrategy} used
     * by the disruptor ringbuffer for Async Loggers.
     *
     * @return the {@code AsyncWaitStrategyFactory}
     * @since 2.17.3
     */
    AsyncWaitStrategyFactory getAsyncWaitStrategyFactory();

    /**
     * Return the WatchManager.
     *
     * @return the WatchManager.
     */
    WatchManager getWatchManager();

    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.logging.log4j.core.config.ReliabilityStrategyFactory#getReliabilityStrategy(org.apache.logging.log4j
     * .core.config.LoggerConfig)
     */

    ReliabilityStrategy getReliabilityStrategy(LoggerConfig loggerConfig);

    /**
     * Returns the {@link NanoClock} instance for this configuration.
     *
     * @return the nano clock
     */
    NanoClock getNanoClock();

    /**
     * Sets the {@link NanoClock} instance for this configuration.
     *
     * @param nanoClock the new nano clock for this configuration. Must be non-null.
     */
    void setNanoClock(NanoClock nanoClock);

    /**
     * Gets the logger context.
     *
     * @return the logger context.
     */
    LoggerContext getLoggerContext();
}
