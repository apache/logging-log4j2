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
package org.apache.logging.log4j.core.config.builder.api;

import aQute.bnd.annotation.baseline.BaselineIgnore;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Filter.Result;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.async.AsyncLogger;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.CustomLevels;
import org.apache.logging.log4j.core.config.LoggerConfig.RootLogger;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.script.Script;
import org.apache.logging.log4j.core.script.ScriptFile;
import org.apache.logging.log4j.core.util.Builder;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.jspecify.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Interface for building logging configurations.
 * @param <T> The Configuration type created by this builder.
 * @since 2.4
 */
@ProviderType
public interface ConfigurationBuilder<T extends Configuration> extends Builder<T> {

    /**
     * Adds the {@link Script} component built by the given {@code ScriptComponentBuilder} to this builder.
     * <p>
     *   Note: the provided {@code builder} will be built by this method; therefore, it must be fully configured
     *   <i>before</i> calling this method.  Changes to the builder after calling this method will not have
     *   any effect.
     * </p>
     *
     * @param builder The {@code ScriptComponentBuilder} to add with all of its attributes and subcomponents set.
     * @return this builder (for chaining)
     * @throws NullPointerException if the given {@code builder} is {@code null}
     */
    ConfigurationBuilder<T> add(ScriptComponentBuilder builder);

    /**
     * Adds the {@link ScriptFile} component built by the given {@code ScriptFileComponentBuilder} to this builder.
     * <p>
     *   Note: the provided {@code builder} will be built by this method; therefore, it must be fully configured
     *   <i>before</i> calling this method.  Changes to the builder after calling this method will not have
     *   any effect.
     * </p>
     *
     * @param builder The {@code ScriptFileComponentBuilder} to add with all of its attributes and subcomponents set.
     * @return this builder (for chaining)
     * @throws NullPointerException if the given {@code builder} is {@code null}
     */
    ConfigurationBuilder<T> add(ScriptFileComponentBuilder builder);

    /**
     * Adds the {@link Appender} component built by the given {@code AppenderComponentBuilder} to this builder.
     * <p>
     *   Note: the provided {@code builder} will be built by this method; therefore, it must be fully configured
     *   <i>before</i> calling this method.  Changes to the builder after calling this method will not have
     *   any effect.
     * </p>
     *
     * @param builder The {@code AppenderComponentBuilder} to add with all of its attributes and subcomponents set.
     * @return this builder (for chaining)
     * @throws NullPointerException if the given {@code builder} is {@code null}
     */
    ConfigurationBuilder<T> add(AppenderComponentBuilder builder);

    /**
     * Adds the {@link CustomLevels} component built by the given {@code CustomLevelComponentBuilder} to this builder.
     * <p>
     *   Note: the provided {@code builder} will be built by this method; therefore, it must be fully configured
     *   <i>before</i> calling this method.  Changes to the builder after calling this method will not have
     *   any effect.
     * </p>
     *
     * @param builder The {@code CustomLevelComponentBuilder} with all of its attributes and subcomponents set.
     * @return this builder (for chaining)
     * @throws NullPointerException if the given {@code builder} is {@code null}
     */
    ConfigurationBuilder<T> add(CustomLevelComponentBuilder builder);

    /**
     * Adds the {@link Filter} component built by the given {@code FilterComponentBuilder} to this builder.
     * <p>
     *   Note: the provided {@code builder} will be built by this method; therefore, it must be fully configured
     *   <i>before</i> calling this method.  Changes to the builder after calling this method will not have
     *   any effect.
     * </p>
     *
     * @param builder The {@code FilterComponentBuilder} with all of its attributes and subcomponents set.
     * @return this builder (for chaining)
     * @throws NullPointerException if the given {@code builder} is {@code null}
     */
    ConfigurationBuilder<T> add(FilterComponentBuilder builder);

    /**
     * Adds the {@link Logger} component built by the given {@code LoggerComponentBuilder} to this builder.
     * <p>
     *   Note: the provided {@code builder} will be built by this method; therefore, it must be fully configured
     *   <i>before</i> calling this method.  Changes to the builder after calling this method will not have
     *   any effect.
     * </p>
     *
     * @param builder The {@code LoggerComponentBuilder} with all of its attributes and subcomponents set.
     * @return this builder (for chaining)
     * @throws NullPointerException if the given {@code builder} is {@code null}
     */
    ConfigurationBuilder<T> add(LoggerComponentBuilder builder);

    /**
     * Adds the {@link RootLogger} component built by the given {@code RootLoggerComponentBuilder}
     * to this builder.
     * <p>
     *   Note: the provided {@code builder} will be built by this method; therefore, it must be fully configured
     *   <i>before</i> calling this method.  Changes to the builder after calling this method will not have
     *   any effect.
     * </p>
     *
     * @param builder The {@code RootLoggerComponentBuilder} with all of its attributes and subcomponents set.
     * @return this builder (for chaining)
     * @throws NullPointerException if the given {@code builder} is {@code null}
     */
    ConfigurationBuilder<T> add(RootLoggerComponentBuilder builder);

    /**
     * Adds a the {@link Property} component built by the given {@link PropertyComponentBuilder}.
     * @param builder the {@code PropertyComponentBuilder} to add
     * @return this builder (for chaining)
     */
    ConfigurationBuilder<T> add(PropertyComponentBuilder builder);

    /**
     * Adds a {@link Property} component with the given {@code key} and {@code value}.
     *
     * @param name the property name
     * @param value the property value
     * @return this builder (for chaining)
     * @throws NullPointerException if the given {@code builder} is {@code null}
     * @deprecated use {@link #add(PropertyComponentBuilder)}
     */
    @Deprecated
    ConfigurationBuilder<T> addProperty(@Nullable String name, @Nullable String value);

    /**
     * Returns a {@link ScriptComponentBuilder} for creating a {@link Script} component.
     * @param name the script name
     * @param language the script language
     * @param text the script to execute
     * @return the new component builder instance
     */
    ScriptComponentBuilder newScript(@Nullable String name, @Nullable String language, @Nullable String text);

    /**
     * Returns a {@link ScriptFileComponentBuilder} for creating a {@link ScriptFile} component.
     * @param path the script file path
     * @return the new component builder instance
     */
    ScriptFileComponentBuilder newScriptFile(@Nullable String path);

    /**
     * Returns a {@link ScriptFileComponentBuilder} for creating a {@link ScriptFile} component.
     * @param name the script file name
     * @param path the script file path
     * @return the new component builder instance
     */
    ScriptFileComponentBuilder newScriptFile(@Nullable String name, @Nullable String path);

    /**
     * Returns a {@link AppenderComponentBuilder} for creating an {@link Appender} component.
     * @param name the appender name
     * @param pluginName the appender plugin-type
     * @return the new component builder instance
     */
    AppenderComponentBuilder newAppender(@Nullable String name, String pluginName);

    /**
     * Returns a {@link AppenderRefComponentBuilder} for creating an {@link AppenderRef} component.
     * @param ref the name of the {@code Appender} being referenced
     * @return the new component builder instance
     */
    AppenderRefComponentBuilder newAppenderRef(@Nullable String ref);

    /**
     * Returns a {@link LoggerComponentBuilder} builder for creating an {@link AsyncLogger} component.
     * @param name the logger name
     * @return the new component builder instance
     */
    LoggerComponentBuilder newAsyncLogger(@Nullable String name);

    /**
     * Returns a {@link LoggerComponentBuilder} for creating an {@link AsyncLogger} component.
     * @param name the logger name
     * @param includeLocation {@code true} to include location information; otherwise, {@code false}
     * @return the new component builder instance
     */
    LoggerComponentBuilder newAsyncLogger(@Nullable String name, boolean includeLocation);

    /**
     * Returns a {@link LoggerComponentBuilder} for creating an {@link AsyncLogger} component.
     * @param name The name of the Logger.
     * @param level The logging Level to be assigned to the Logger.
     * @return A new LoggerComponentBuilder.
     */
    LoggerComponentBuilder newAsyncLogger(@Nullable String name, @Nullable Level level);

    /**
     * Returns a {@link LoggerComponentBuilder} for creating an {@link AsyncLogger} component.
     * @param name the logger name
     * @param level the logger level
     * @param includeLocation {@code true} to include location information; otherwise, {@code false}
     * @return the new component builder instance
     */
    LoggerComponentBuilder newAsyncLogger(@Nullable String name, @Nullable Level level, boolean includeLocation);

    /**
     * Returns a {@link LoggerComponentBuilder} for creating an {@link AsyncLogger} component
     * @param name the logger name
     * @param level the logger level
     * @return the new component builder instance
     */
    LoggerComponentBuilder newAsyncLogger(@Nullable String name, @Nullable String level);

    /**
     * Returns a {@link LoggerComponentBuilder} for creating an {@link AsyncLogger} component.
     * @param name the logger name
     * @param level the logger level
     * @param includeLocation {@code true} to include location inforrmation; otherwise, {@code false}
     * @return the new component builder instance
     */
    LoggerComponentBuilder newAsyncLogger(@Nullable String name, @Nullable String level, boolean includeLocation);

    /**
     * Returns a {@link RootLoggerComponentBuilder} for creating a root {@link AsyncLogger} component.
     * @return the new component builder instance
     */
    RootLoggerComponentBuilder newAsyncRootLogger();

    /**
     * Returns a {@link RootLoggerComponentBuilder} for creating a root {@link AsyncLogger} component.
     * @param includeLocation {@code true} to include location information; otherwise, {@code false}
     * @return the new component builder instance
     */
    RootLoggerComponentBuilder newAsyncRootLogger(boolean includeLocation);

    /**
     * Returns a {@link RootLoggerComponentBuilder} for creating a root {@link AsyncLogger} component.
     * @param level the logger level
     * @return the new component builder instance
     */
    RootLoggerComponentBuilder newAsyncRootLogger(@Nullable Level level);

    /**
     * Returns a {@link RootLoggerComponentBuilder} for creating a root {@link AsyncLogger} component.
     * @param level the logger level
     * @param includeLocation {@code true} to include location information; otherwise, {@code false}
     * @return the new component builder instance
     */
    RootLoggerComponentBuilder newAsyncRootLogger(@Nullable Level level, boolean includeLocation);

    /**
     * Returns a {@link RootLoggerComponentBuilder} for creating a root {@link AsyncLogger} component.
     * @param level the logger level
     * @return the new component builder instance
     */
    RootLoggerComponentBuilder newAsyncRootLogger(@Nullable String level);

    /**
     * Returns a {@link RootLoggerComponentBuilder} for creating a root {@link AsyncLogger} component.
     * @param level the logger level
     * @param includeLocation {@code true} to include location information; otherwise, {@code false}
     * @return the new component builder instance
     */
    RootLoggerComponentBuilder newAsyncRootLogger(@Nullable String level, boolean includeLocation);

    /**
     * Returns a new {@link ComponentBuilder} for creating a generic {@link Component}.
     * @param <B> the {@code ComponentBuilder} target type
     * @param pluginType the component plugin type
     * @return the new component builder instance
     */
    <B extends ComponentBuilder<B>> ComponentBuilder<B> newComponent(String pluginType);

    /**
     * Returns a new {@link ComponentBuilder} for creating a generic {@link Component}.
     * @param <B> the {@code ComponentBuilder} target type
     * @param name the component name
     * @param pluginType the component plugin type
     * @return the new component builder instance
     */
    <B extends ComponentBuilder<B>> ComponentBuilder<B> newComponent(@Nullable String name, String pluginType);

    /**
     * Returns a new {@link ComponentBuilder} for creating a generic {@link Component}.
     * @param <B> the {@code ComponentBuilder} target type
     * @param name the component name
     * @param pluginType the component plugin type
     * @param value the component value
     * @return the new component builder instance
     */
    <B extends ComponentBuilder<B>> ComponentBuilder<B> newComponent(
            @Nullable String name, String pluginType, @Nullable String value);

    /**
     * Returns a new {@link PropertyComponentBuilder} for creating a {@link Property} component.
     * @param name the property name
     * @param value the property value
     * @return the new component builder instance
     */
    PropertyComponentBuilder newProperty(@Nullable String name, @Nullable String value);

    /**
     * Returns a new {@link PropertyComponentBuilder} for creating a {@link KeyValuePair} component.
     * @param key the key
     * @param value the value
     * @return the new component builder instance
     */
    KeyValuePairComponentBuilder newKeyValuePair(@Nullable String key, @Nullable String value);

    /**
     * Returns a new {@link CustomLevelComponentBuilder} for creating a {@link CustomLevels} component.
     * @param name the custom level name
     * @param intLevel the integer value to be assigned to the level
     * @return the new component builder instance
     */
    CustomLevelComponentBuilder newCustomLevel(@Nullable String name, int intLevel);

    /**
     * Returns a new {@link FilterComponentBuilder} for creating a {@link Filter} component.
     * @param pluginType the plugin type of the filter
     * @return the new component builder instance
     */
    FilterComponentBuilder newFilter(String pluginType);

    /**
     * Returns a new {@link FilterComponentBuilder} for creating a {@code Filter} component.
     * @param pluginType The Plugin type of the Filter.
     * @param onMatch "ACCEPT", "DENY", or "NEUTRAL"
     * @param onMismatch "ACCEPT", "DENY", or "NEUTRAL"
     * @return the new component builder instance
     */
    FilterComponentBuilder newFilter(String pluginType, @Nullable Result onMatch, @Nullable Result onMismatch);

    /**
     * Returns a new {@link FilterComponentBuilder} for creating a {@link Filter} component.
     * @param pluginType the plugin type of the filter
     * @param onMatch "ACCEPT", "DENY", or "NEUTRAL"
     * @param onMismatch "ACCEPT", "DENY", or "NEUTRAL"
     * @return the new component builder instance
     */
    FilterComponentBuilder newFilter(String pluginType, @Nullable String onMatch, @Nullable String onMismatch);

    /**
     * Returns a new {@link LayoutComponentBuilder} for creating a {@link Layout} component.
     * @param pluginType the plugin type of the layout
     * @return the new component builder instance
     */
    LayoutComponentBuilder newLayout(String pluginType);

    /**
     * Returns a new {@link LayoutComponentBuilder} for creating a {@link Layout} component.
     * @param name the logger name
     * @return the new component builder instance
     */
    LoggerComponentBuilder newLogger(@Nullable String name);

    /**
     * Returns a new {@link LoggerComponentBuilder} for creating a {@link Logger} component.
     * @param name the logger name
     * @param includeLocation {@code true} to include location information; otherwise, {@code false}
     * @return the new component builder instance
     */
    LoggerComponentBuilder newLogger(@Nullable String name, boolean includeLocation);

    /**
     * Returns a new {@link LoggerComponentBuilder} for creating a {@link Logger} component.
     * @param name the logger name
     * @param level the logger level
     * @return the new component builder instance
     */
    LoggerComponentBuilder newLogger(@Nullable String name, @Nullable Level level);

    /**
     * Returns a new {@link LoggerComponentBuilder} for creating a {@link Logger} component.
     * @param name the logger name
     * @param level the logger level
     * @param includeLocation {@code true} to include location information; otherwise, {@code false}
     * @return the new component builder instance
     */
    LoggerComponentBuilder newLogger(@Nullable String name, @Nullable Level level, boolean includeLocation);

    /**
     * Returns a new {@link LoggerComponentBuilder} for creating a {@link Logger} component.
     * @param name the logger name
     * @param level the logger level
     * @return the new component builder instance
     */
    LoggerComponentBuilder newLogger(@Nullable String name, @Nullable String level);

    /**
     * Returns a new {@link LoggerComponentBuilder} for creating a {@link Logger} component.
     * @param name the logger name
     * @param level the logger level
     * @param includeLocation {@code true} to include location information; otherwise, {@code false}
     * @return the new component builder instance
     */
    LoggerComponentBuilder newLogger(@Nullable String name, @Nullable String level, boolean includeLocation);

    /**
     * Returns a new {@link RootLoggerComponentBuilder} for creating a {@link RootLogger} component.
     * @return the new component builder instance
     */
    RootLoggerComponentBuilder newRootLogger();

    /**
     * Returns a new {@link RootLoggerComponentBuilder} for creating a {@link RootLogger} component.
     * @param includeLocation {@code true} to include location information; otherwise, {@code false}
     * @return the new component builder instancec
     */
    RootLoggerComponentBuilder newRootLogger(boolean includeLocation);

    /**
     * Returns a new {@link RootLoggerComponentBuilder} for creating a {@link RootLogger} component.
     * @param level the logger level
     * @return the new component builder instance
     */
    RootLoggerComponentBuilder newRootLogger(@Nullable Level level);

    /**
     * Returns a new {@link RootLoggerComponentBuilder} for creating a {@link RootLogger} component.
     * @param level the logger level
     * @param includeLocation {@code true} to include location information; otherwise, {@code false}
     * @return the new component builder instance
     */
    RootLoggerComponentBuilder newRootLogger(@Nullable Level level, boolean includeLocation);

    /**
     * Returns a new {@link RootLoggerComponentBuilder} for creating a {@link RootLogger} component.
     * @param level the logger level
     * @return the new component builder instance
     */
    RootLoggerComponentBuilder newRootLogger(@Nullable String level);

    /**
     * Returns a new {@link RootLoggerComponentBuilder} for creating a {@link RootLogger} component.
     * @param level the logger level
     * @param includeLocation {@code true} to include location information; otherwise, {@code false}
     * @return the new component builder instance
     */
    RootLoggerComponentBuilder newRootLogger(@Nullable String level, boolean includeLocation);

    /**
     * Set the Advertiser Plugin name.
     * @param advertiser The Advertiser Plugin name.
     * @return this builder instance.
     */
    ConfigurationBuilder<T> setAdvertiser(@Nullable String advertiser);

    /**
     * Sets the name of the configuration.
     * @param name the name of the {@link Configuration}. By default is {@code "Constructed"}.
     * @return this builder instance.
     */
    ConfigurationBuilder<T> setConfigurationName(@Nullable String name);

    /**
     * Sets the configuration source.
     * @param configurationSource the configuration source
     * @return this builder instance (for chaining)
     */
    ConfigurationBuilder<T> setConfigurationSource(@Nullable ConfigurationSource configurationSource);

    /**
     * Specifies the destination for StatusLogger events. This can be {@code out} (default) for using
     * {@link System#out standard out}, {@code err} for using {@link System#err standard error}, or a file URI to
     * which log events will be written. If the provided URI is invalid, then the default destination of standard
     * out will be used.
     *
     * @param destination where status log messages should be output.
     * @return this builder instance.
     */
    ConfigurationBuilder<T> setDestination(@Nullable String destination);

    /**
     * Sets the logger context.
     * @param loggerContext the logger context.
     */
    @BaselineIgnore("2.25.0")
    ConfigurationBuilder<T> setLoggerContext(@Nullable LoggerContext loggerContext);

    /**
     * Sets the configuration's "monitorInterval" attribute.
     * <p>
     *   The monitor interval specifies the number of seconds between checks for changes to the configuration source.
     * </p>
     * @param intervalSeconds the number of seconds that should pass between checks of the configuration source
     * @return this builder instance
     */
    ConfigurationBuilder<T> setMonitorInterval(int intervalSeconds);

    /**
     * Sets the configuration's "monitorInterval" attribute.
     * <p>
     *   The monitor interval specifies the number of seconds between checks for changes to the configuration source.
     * </p>
     * @param intervalSeconds the number of seconds that should pass between checks of the configuration source
     * @return this builder instance
     * @throws NumberFormatException if the {@code intervalSeconds} argument is not a valid integer representation
     */
    ConfigurationBuilder<T> setMonitorInterval(String intervalSeconds);

    /**
     * Sets the configuration's list of packages to search for Log4j plugins.
     * @param packages a comma separated list of packages
     * @return this builder (for chaining)
     */
    ConfigurationBuilder<T> setPackages(@Nullable String packages);

    /**
     * Sets the configuration's "shutdownHook" attribute.
     * @param flag "disable" will prevent the shutdown hook from being set.
     * @return this builder (for chaining)
     */
    ConfigurationBuilder<T> setShutdownHook(@Nullable String flag);

    /**
     * Sets the configuration's "shutdownTimeout" attribute.
     * <p>
     *   The shutdown-timeout specifies how long appenders and background tasks will get to shut down when the
     *   JVM is shutting down.
     * </p>
     * <p>
     *   The default is zero which means that each appender uses its default timeout, and doesn't wait for background
     *   tasks. Not all appenders will honor this, it is a hint and not an absolute guarantee that the shutdown
     *   procedure will not take longer.
     * </p>
     * <p>
     *   Setting the shutdown-timeout too low increase the risk of losing outstanding log events that have not yet
     *   been written to the final destination.
     * </p>
     * <p>
     *   This setting is ignored if {@link #setShutdownHook(String)} has been set to "disable".
     * </p>
     *
     * @param shutdownTimeoutMillis the shutdown timeout in milliseconds
     * @return this builder (for chaining)
     * @throws IllegalArgumentException if the {@code shutdownTimeoutMillis} argument is less than 0
     * @see LoggerContext#stop(long, TimeUnit)
     */
    ConfigurationBuilder<T> setShutdownTimeout(long shutdownTimeoutMillis);

    /**
     * Sets the configuration's "shutdownTimeout" attribute.
     * <p>
     *   The shutdown-timeout specifies how long appenders and background tasks will get to shut down when the
     *   JVM is shutting down.
     * </p>
     * <p>
     *   The default is zero which means that each appender uses its default timeout, and doesn't wait for background
     *   tasks. Not all appenders will honor this, it is a hint and not an absolute guarantee that the shutdown
     *   procedure will not take longer.
     * </p>
     * <p>
     *   Setting the shutdown-timeout too low increase the risk of losing outstanding log events that have not yet
     *   been written to the final destination.
     * </p>
     * <p>
     *   This setting is ignored if {@link #setShutdownHook(String)} has been set to "disable".
     * </p>
     *
     * @param timeout the timeout (in the given {@code TimeUnit}
     * @param timeUnit the time-unit used to convert the timeout to milliseconds
     * @return this builder (for chaining)
     * @throws NullPointerException if the given {@code timeUnit} argument is {@code null}
     * @see LoggerContext#stop(long, TimeUnit)
     */
    ConfigurationBuilder<T> setShutdownTimeout(long timeout, TimeUnit timeUnit);

    /**
     * Sets the level of the StatusLogger.
     * @param level The logging level.
     * @return this builder instance.
     */
    ConfigurationBuilder<T> setStatusLevel(Level level);

    /**
     * Set a property with the given {@code key} and {@code value} on the root node.
     * @param key the property key
     * @param value the property value
     * @return this builder (for chaining)
     * @throws NullPointerException if the {@code key} argument is {@code null}
     */
    ConfigurationBuilder<T> setRootProperty(String key, @Nullable String value);

    /**
     * Sets whether the logging should include constructing Plugins.
     * @param verbosity "disable" will hide messages from plugin construction.
     * @return this builder instance.
     */
    ConfigurationBuilder<T> setVerbosity(@Nullable String verbosity);

    /**
     * Add a property with the given key and value to the configuration's root node.
     * @param key the property key
     * @param value the property value
     * @return this builder (for chaining)
     * @deprecated use {@link #setRootProperty(String, String)}
     */
    @Deprecated
    ConfigurationBuilder<T> addRootProperty(String key, String value);

    /**
     * Build the configuration and optionally initialize it.
     * @param initialize true if the configuration should be initialized, false otherwise. Generally, Configurations
     *                   should not be initialized when they are constructed.
     * @return The constructed Configuration.
     */
    T build(boolean initialize);

    /**
     * Constructs an XML configuration from this builder.
     *
     * @param output  OutputStream to write to, will not be closed
     *
     * @since 2.7
     */
    void writeXmlConfiguration(OutputStream output) throws IOException;

    /**
     * Constructs an XML configuration from this builder.
     *
     * @return  XML configuration
     *
     * @since 2.7
     */
    String toXmlConfiguration();
}
