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
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.filter.Filterable;
import org.apache.logging.log4j.core.impl.LogEventFactory;
import org.apache.logging.log4j.core.lookup.ConfigurationStrSubstitutor;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.net.Advertiser;
import org.apache.logging.log4j.core.script.ScriptManager;
import org.apache.logging.log4j.core.time.NanoClock;
import org.apache.logging.log4j.core.util.NetUtils;
import org.apache.logging.log4j.core.util.WatchManager;
import org.apache.logging.log4j.kit.env.PropertyEnvironment;
import org.apache.logging.log4j.kit.recycler.RecyclerFactory;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.di.Key;

/**
 * Interface that must be implemented to create a configuration.
 * <p>
 * Custom implementations are recommended to extend {@link AbstractConfiguration}.
 * </p>
 *
 * @see AbstractConfiguration
 */
public interface Configuration extends Filterable {

    /** Injection key for the current Configuration. */
    Key<Configuration> KEY = Key.forClass(Configuration.class);

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
     * Returns the configuration properties. These will initially include entries for {@code contextName}
     * with the {@linkplain LoggerContext#getName() context name} and
     * {@code hostName} with the {@linkplain NetUtils#getLocalHostname() local host name}. Additional
     * properties may be defined by plugins.
     */
    Map<String, String> getProperties();

    /**
     * Returns the {@linkplain LoggerContext#getEnvironment() context properties}
     * associated with the logger context for this configuration.
     *
     * @return the context properties
     */
    PropertyEnvironment getEnvironment();

    /**
     * Returns the root Logger.
     *
     * @return the root Logger.
     */
    LoggerConfig getRootLogger();

    void addListener(Consumer<Reconfigurable> listener);

    void removeListener(Consumer<Reconfigurable> listener);

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

    <T> Supplier<T> getFactory(Key<T> key);

    default <T> T getComponent(Key<T> key) {
        return getFactory(key).get();
    }

    <T> void setComponent(Key<T> key, Supplier<? extends T> supplier);

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
     * Gets the logger context. This may be {@code null} if the context has already been stopped and garbage collected.
     *
     * @return the logger context.
     */
    LoggerContext getLoggerContext();

    default LogEventFactory getLogEventFactory() {
        return getComponent(LogEventFactory.KEY);
    }

    default RecyclerFactory getRecyclerFactory() {
        return getComponent(Key.forClass(RecyclerFactory.class));
    }

    /**
     * Registers a new configuration extension, if it doesn't exist.
     * <p>
     *     To preventing polluting the main configuration element,
     *     each JAR that wishes to extend the {@link Configuration} should use a single child element.
     * </p>
     * @param extensionType the concrete type of the extension,
     * @param supplier a factory to create a new extension element,
     * @return the current extension if present or a newly generated one.
     * @since 3.0
     * @see #getExtension(Class)
     */
    <T extends ConfigurationExtension> T addExtensionIfAbsent(Class<T> extensionType, Supplier<? extends T> supplier);

    /**
     * Returns an extension of the given type.
     * <p>
     *     Only the first extension of the given type is returned.
     * </p>
     * @param extensionType a concrete type of the extension,
     * @return an extension the matches the given type.
     * @since 3.0
     */
    <T extends ConfigurationExtension> T getExtension(Class<T> extensionType);
}
