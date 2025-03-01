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
package org.apache.logging.log4j.core.config.builder.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter.Result;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.AppenderRefComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.Component;
import org.apache.logging.log4j.core.config.builder.api.ComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.CustomLevelComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.FilterComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.KeyValuePairComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.LoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.PropertyComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ScriptComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ScriptFileComponentBuilder;
import org.apache.logging.log4j.core.util.Throwables;
import org.jspecify.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;

/**
 * A {@link ConfigurationBuilder} implementation for building a {@link BuiltConfiguration} or a custom
 * implementation thereof.
 *
 * @param <T> The BuiltConfiguration type.
 * @since 2.4
 */
@ProviderType
public class DefaultConfigurationBuilder<T extends BuiltConfiguration> implements ConfigurationBuilder<T> {

    private static final String INDENT = "  ";

    private final Component root = new Component("root");
    private final Component loggers;
    private final Component appenders;
    private final Component filters;
    private final Component properties;
    private final Component customLevels;
    private final Component scripts;
    private final Class<T> clazz;
    private @Nullable ConfigurationSource configurationSource = null;
    private int monitorInterval = 0;
    private @Nullable Level statusLevel;
    private @Nullable String destination;
    private @Nullable String packages;
    private @Nullable String shutdownFlag;
    private long shutdownTimeoutMillis = 0L;
    private @Nullable String advertiser;
    private @Nullable LoggerContext loggerContext;
    private @Nullable String configurationName;

    /* static initialization */
    @SuppressFBWarnings(
            value = {"XXE_DTD_TRANSFORM_FACTORY", "XXE_XSLT_TRANSFORM_FACTORY"},
            justification = "This method only uses internally generated data.")
    public static void formatXml(final Source source, final javax.xml.transform.Result result)
            throws TransformerFactoryConfigurationError, TransformerException {
        final Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", Integer.toString(INDENT.length()));
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(source, result);
    }

    /**
     * Constructs a new configuration builder using the default {@link BuiltConfiguration} as
     * a target configuration type.
     */
    @SuppressWarnings("unchecked")
    public DefaultConfigurationBuilder() {
        this((Class<T>) BuiltConfiguration.class);
        root.addAttribute("name", "Built");
    }

    /**
     * Constructs a new configuration builder with the given target {@link BuiltConfiguration} implementation class.
     * <p>
     *   Note the implementation will be instantiated per reflection and must have a constructor with the following
     *   parameters:
     * </p>
     * <pre>{@code
     *   public BuiltConfigurationClazz(LogggerContext, ConfigurationSource, Component) {
     *     ...
     *   }
     * }</pre>
     *
     * @param clazz the {@code BuiltConfiguration} implementation class
     */
    public DefaultConfigurationBuilder(final Class<T> clazz) {

        Objects.requireNonNull(clazz, "The 'clazz' argument must not be null.");

        this.clazz = clazz;
        final List<Component> components = root.getComponents();
        properties = new Component("Properties");
        components.add(properties);
        scripts = new Component("Scripts");
        components.add(scripts);
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
     * {@inheritDoc}
     *
     * @throws NullPointerException if the {@code builder} argument is {@code null}
     */
    @Override
    public ConfigurationBuilder<T> add(final AppenderComponentBuilder builder) {
        return add(appenders, builder);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if the {@code builder} argument is {@code null}
     */
    @Override
    public ConfigurationBuilder<T> add(final CustomLevelComponentBuilder builder) {
        return add(customLevels, builder);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if the {@code builder} argument is {@code null}
     */
    @Override
    public ConfigurationBuilder<T> add(final FilterComponentBuilder builder) {
        return add(filters, builder);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if the {@code builder} argument is {@code null}
     */
    @Override
    public ConfigurationBuilder<T> add(final LoggerComponentBuilder builder) {
        return add(loggers, builder);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if the {@code builder} argument is {@code null}
     */
    @Override
    public ConfigurationBuilder<T> add(final RootLoggerComponentBuilder builder) {
        for (final Component c : loggers.getComponents()) {
            if (c.getPluginType().equals(LoggerConfig.ROOT)) {
                throw new ConfigurationException("Root Logger was previously defined");
            }
        }
        return add(loggers, builder);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if the {@code builder} argument is {@code null}
     */
    @Override
    public ConfigurationBuilder<T> add(final PropertyComponentBuilder builder) {
        return add(properties, builder);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if the {@code builder} argument is {@code null}
     */
    @Override
    public ConfigurationBuilder<T> add(final ScriptComponentBuilder builder) {
        return add(scripts, builder);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if the {@code builder} argument is {@code null}
     */
    @Override
    public ConfigurationBuilder<T> add(final ScriptFileComponentBuilder builder) {
        return add(scripts, builder);
    }

    /**
     * Adds the {@code Component} built by the given {@code builder} as a child of the given {@code parent} component.
     * @param parent the parent component
     * @param builder the builder used to build the child componentt
     * @return this builder (for chaining)
     */
    protected ConfigurationBuilder<T> add(final Component parent, final ComponentBuilder<?> builder) {
        Objects.requireNonNull(parent, "The 'parent' argument must not be null.");
        Objects.requireNonNull(builder, "The 'builder' argument must not be null.");
        parent.getComponents().add(builder.build());
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated use {@link #add(PropertyComponentBuilder)}
     */
    @Override
    @Deprecated
    public ConfigurationBuilder<T> addProperty(@Nullable String name, @Nullable String value) {
        return add(newProperty(name, value));
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated use {@link #setRootProperty(String, String)}
     */
    @Override
    @Deprecated
    public ConfigurationBuilder<T> addRootProperty(String key, String value) {
        return setRootProperty(key, value);
    }

    /** {@inheritDoc} */
    @Override
    public T build() {
        return build(true);
    }

    /** {@inheritDoc} */
    @Override
    public T build(final boolean initialize) {
        T configuration;
        try {
            if (configurationSource == null) {
                configurationSource = ConfigurationSource.NULL_SOURCE;
            }
            final Constructor<T> constructor =
                    clazz.getConstructor(LoggerContext.class, ConfigurationSource.class, Component.class);
            configuration = constructor.newInstance(loggerContext, configurationSource, root);
            configuration.getRootNode().getAttributes().putAll(root.getAttributes());
            if (configurationName != null) {
                configuration.setName(configurationName);
            }
            if (statusLevel != null) {
                configuration.getStatusConfiguration().withStatus(statusLevel);
            }
            if (destination != null) {
                configuration.getStatusConfiguration().withDestination(destination);
            }
            if (packages != null) {
                configuration.setPluginPackages(packages);
            }
            if (shutdownFlag != null) {
                configuration.setShutdownHook(shutdownFlag);
            }
            if (shutdownTimeoutMillis > 0) {
                configuration.setShutdownTimeoutMillis(shutdownTimeoutMillis);
            }
            if (advertiser != null) {
                configuration.createAdvertiser(advertiser, configurationSource);
            }
            configuration.setMonitorInterval(monitorInterval);
        } catch (final Exception ex) {
            throw new IllegalArgumentException("Invalid Configuration class specified", ex);
        }
        configuration.getStatusConfiguration().initialize();
        if (initialize) {
            configuration.initialize();
        }
        return configuration;
    }

    /** {@inheritDoc} */
    @Override
    public ScriptComponentBuilder newScript(
            final @Nullable String name, final @Nullable String language, final @Nullable String text) {
        return new DefaultScriptComponentBuilder(this, name)
                .setLanguageAttribute(language)
                .setTextAttribute(text);
    }

    /** {@inheritDoc} */
    @Override
    public ScriptFileComponentBuilder newScriptFile(final @Nullable String path) {
        return new DefaultScriptFileComponentBuilder(this, path).setPathAttribute(path);
    }

    /** {@inheritDoc} */
    @Override
    public ScriptFileComponentBuilder newScriptFile(final @Nullable String name, final @Nullable String path) {
        return new DefaultScriptFileComponentBuilder(this, (name != null) ? name : path).setPathAttribute(path);
    }

    /** {@inheritDoc} */
    @Override
    public AppenderComponentBuilder newAppender(final @Nullable String name, final String type) {
        return new DefaultAppenderComponentBuilder(this, type, name);
    }

    /** {@inheritDoc} */
    @Override
    public AppenderRefComponentBuilder newAppenderRef(final @Nullable String ref) {
        return new DefaultAppenderRefComponentBuilder(this).setRefAttribute(ref);
    }

    /** {@inheritDoc} */
    @Override
    public LoggerComponentBuilder newAsyncLogger(final @Nullable String name) {
        return new DefaultLoggerComponentBuilder(this, "AsyncLogger", name);
    }

    /** {@inheritDoc} */
    @Override
    public LoggerComponentBuilder newAsyncLogger(final @Nullable String name, final boolean includeLocation) {
        return this.newAsyncLogger(name).setIncludeLocationAttribute(includeLocation);
    }

    /** {@inheritDoc} */
    @Override
    public LoggerComponentBuilder newAsyncLogger(final @Nullable String name, final @Nullable Level level) {
        return this.newAsyncLogger(name).setLevelAttribute(level);
    }

    /** {@inheritDoc} */
    @Override
    public LoggerComponentBuilder newAsyncLogger(
            final @Nullable String name, final @Nullable Level level, final boolean includeLocation) {
        return this.newAsyncLogger(name)
                .setIncludeLocationAttribute(includeLocation)
                .setLevelAttribute(level);
    }

    /** {@inheritDoc} */
    @Override
    public LoggerComponentBuilder newAsyncLogger(final @Nullable String name, final @Nullable String level) {
        return this.newAsyncLogger(name).setLevelAttribute(level);
    }

    /** {@inheritDoc} */
    @Override
    public LoggerComponentBuilder newAsyncLogger(
            final @Nullable String name, final @Nullable String level, final boolean includeLocation) {
        return this.newAsyncLogger(name).setLevelAttribute(level).setIncludeLocationAttribute(includeLocation);
    }

    /** {@inheritDoc} */
    @Override
    public RootLoggerComponentBuilder newAsyncRootLogger() {
        return new DefaultRootLoggerComponentBuilder(this, "AsyncRoot");
    }

    /** {@inheritDoc} */
    @Override
    public RootLoggerComponentBuilder newAsyncRootLogger(final boolean includeLocation) {
        return newAsyncRootLogger().setIncludeLocationAttribute(includeLocation);
    }

    /** {@inheritDoc} */
    @Override
    public RootLoggerComponentBuilder newAsyncRootLogger(final @Nullable Level level) {
        return newAsyncRootLogger().setLevelAttribute(level);
    }

    /** {@inheritDoc} */
    @Override
    public RootLoggerComponentBuilder newAsyncRootLogger(final @Nullable Level level, final boolean includeLocation) {
        return newAsyncRootLogger().setLevelAttribute(level).setIncludeLocationAttribute(includeLocation);
    }

    /** {@inheritDoc} */
    @Override
    public RootLoggerComponentBuilder newAsyncRootLogger(final @Nullable String level) {
        return newAsyncRootLogger().setLevelAttribute(level);
    }

    /** {@inheritDoc} */
    @Override
    public RootLoggerComponentBuilder newAsyncRootLogger(final @Nullable String level, final boolean includeLocation) {
        return newAsyncRootLogger().setLevelAttribute(level).setIncludeLocationAttribute(includeLocation);
    }

    /** {@inheritDoc} */
    @Override
    public <B extends ComponentBuilder<B>> ComponentBuilder<B> newComponent(final String pluginType) {
        return newComponent(null, pluginType, null);
    }

    /** {@inheritDoc} */
    @Override
    public <B extends ComponentBuilder<B>> ComponentBuilder<B> newComponent(
            final @Nullable String name, final String pluginType) {
        return this.newComponent(name, pluginType, null);
    }

    /** {@inheritDoc} */
    @Override
    public <B extends ComponentBuilder<B>> ComponentBuilder<B> newComponent(
            final @Nullable String name, final String pluginType, final @Nullable String value) {
        return new DefaultComponentBuilder<>(this, pluginType, name, value);
    }

    /** {@inheritDoc} */
    @Override
    public PropertyComponentBuilder newProperty(final @Nullable String name, final @Nullable String value) {
        return new DefaultPropertyComponentBuilder(this, name, value);
    }

    /** {@inheritDoc} */
    @Override
    public KeyValuePairComponentBuilder newKeyValuePair(final @Nullable String key, final @Nullable String value) {
        return new DefaultKeyValuePairComponentBuilder(this)
                .setKeyAttribute(key)
                .setValueAttribute(value);
    }

    /** {@inheritDoc} */
    @Override
    public CustomLevelComponentBuilder newCustomLevel(final @Nullable String name, final int intLevel) {
        return new DefaultCustomLevelComponentBuilder(this, name).setIntLevelAttribute(intLevel);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if the {@code pluginType} argument is {@code null}
     */
    @Override
    public FilterComponentBuilder newFilter(final String pluginType) {
        return new DefaultFilterComponentBuilder(this, pluginType);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if the {@code pluginType} argument is {@code null}
     */
    @Override
    public FilterComponentBuilder newFilter(
            final String pluginType, final @Nullable Result onMatch, final @Nullable Result onMismatch) {
        return newFilter(pluginType).setOnMatchAttribute(onMatch).setOnMismatchAttribute(onMismatch);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if the {@code pluginType} argument is {@code null}
     */
    @Override
    public FilterComponentBuilder newFilter(
            final String pluginType, final @Nullable String onMatch, final @Nullable String onMismatch) {
        return newFilter(pluginType).setOnMatchAttribute(onMatch).setOnMismatchAttribute(onMismatch);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if the {@code pluginType} argument is {@code null}
     */
    @Override
    public LayoutComponentBuilder newLayout(final String pluginType) {
        return new DefaultLayoutComponentBuilder(this, pluginType);
    }

    /** {@inheritDoc} */
    @Override
    public LoggerComponentBuilder newLogger(final @Nullable String name) {
        return new DefaultLoggerComponentBuilder(this, name);
    }

    /** {@inheritDoc} */
    @Override
    public LoggerComponentBuilder newLogger(final @Nullable String name, final boolean includeLocation) {
        return newLogger(name).setIncludeLocationAttribute(includeLocation);
    }

    /** {@inheritDoc} */
    @Override
    public LoggerComponentBuilder newLogger(final @Nullable String name, final @Nullable Level level) {
        return newLogger(name).setLevelAttribute(level);
    }

    /** {@inheritDoc} */
    @Override
    public LoggerComponentBuilder newLogger(
            final @Nullable String name, final @Nullable Level level, final boolean includeLocation) {
        return newLogger(name).setLevelAttribute(level).setIncludeLocationAttribute(includeLocation);
    }

    /** {@inheritDoc} */
    @Override
    public LoggerComponentBuilder newLogger(final @Nullable String name, final @Nullable String level) {
        return newLogger(name).setLevelAttribute(level);
    }

    /** {@inheritDoc} */
    @Override
    public LoggerComponentBuilder newLogger(
            final @Nullable String name, final @Nullable String level, final boolean includeLocation) {
        return newLogger(name).setIncludeLocationAttribute(includeLocation).setLevelAttribute(level);
    }

    @Override
    public RootLoggerComponentBuilder newRootLogger() {
        return new DefaultRootLoggerComponentBuilder(this);
    }

    /** {@inheritDoc} */
    @Override
    public RootLoggerComponentBuilder newRootLogger(final boolean includeLocation) {
        return newRootLogger().setIncludeLocationAttribute(includeLocation);
    }

    /** {@inheritDoc} */
    @Override
    public RootLoggerComponentBuilder newRootLogger(final @Nullable Level level) {
        return newRootLogger().setLevelAttribute(level);
    }

    /** {@inheritDoc} */
    @Override
    public RootLoggerComponentBuilder newRootLogger(final @Nullable Level level, final boolean includeLocation) {
        return newRootLogger().setLevelAttribute(level).setIncludeLocationAttribute(includeLocation);
    }

    /** {@inheritDoc} */
    @Override
    public RootLoggerComponentBuilder newRootLogger(final @Nullable String level) {
        return newRootLogger().setLevelAttribute(level);
    }

    /** {@inheritDoc} */
    @Override
    public RootLoggerComponentBuilder newRootLogger(final @Nullable String level, final boolean includeLocation) {
        return newRootLogger().setLevelAttribute(level).setIncludeLocationAttribute(includeLocation);
    }

    /**
     * Returns the advertiser.
     * @return the advertiser or {@code null} if undefined
     */
    protected @Nullable String getAdvertiser() {
        return this.advertiser;
    }

    /**
     * Returns the configuration name.
     * @return the configuration name or {@code null} if undefined
     */
    protected @Nullable String getConfigurationName() {
        return this.configurationName;
    }

    /**
     * Returns the configuration source.
     *
     * @return the configuration source or {@code null} if undefined
     */
    protected @Nullable ConfigurationSource getConfigurationSource() {
        return this.configurationSource;
    }

    /**
     * Returns the configuration's destination.
     * @return the destination or {@code null} if undefined
     */
    protected @Nullable String getDestination() {
        return this.destination;
    }

    /**
     * Returns the configuration's logger-context.
     * @return the logger-context or {@code null} if undefined
     */
    protected @Nullable LoggerContext getLoggerContext() {
        return this.loggerContext;
    }

    /**
     * Returns the configuration's monitor interval in seconds.
     * @return the monitor interval (in seconds)
     */
    protected int getMonitorInterval() {
        return this.monitorInterval;
    }

    /**
     * Returns the comma-separated of package-names to search for plugins.
     * @return the packages or {@code null} if undefined
     */
    protected @Nullable String getPackages() {
        return this.packages;
    }

    /**
     * Returns the root property with the given key.
     * @param key the key
     * @return the property value or {@code null} if undefined
     * @throws NullPointerException if the given {@code key} argument is {@code null}
     */
    protected @Nullable String getRootProperty(String key) {

        Objects.requireNonNull(key, "The 'key' argument must not be null.");

        return this.root.getAttributes().get(key);
    }

    /**
     * Returns the configuration's shutdown timeout in milliseconds.
     * @return the shutdown timeout (in millis)
     */
    protected long getShutdownTimeout() {
        return this.shutdownTimeoutMillis;
    }

    /**
     * Returns the configuration's status logger level.
     * @return the status logger level or {@code null} if undefined
     */
    protected @Nullable Level getStatusLevel() {
        return this.statusLevel;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigurationBuilder<T> setAdvertiser(final @Nullable String advertiser) {
        this.advertiser = advertiser;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigurationBuilder<T> setConfigurationName(final @Nullable String name) {
        this.configurationName = name;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigurationBuilder<T> setConfigurationSource(final @Nullable ConfigurationSource configurationSource) {
        this.configurationSource = configurationSource;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigurationBuilder<T> setDestination(final @Nullable String destination) {
        this.destination = destination;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public void setLoggerContext(final @Nullable LoggerContext loggerContext) {
        this.loggerContext = loggerContext;
    }

    /**
     * {@inheritDoc}
     *
     * @throws NumberFormatException if the {@code intervalSeconds} argument is not a valid integer representation
     */
    @Override
    public ConfigurationBuilder<T> setMonitorInterval(final int intervalSeconds) {
        if (intervalSeconds >= 0) {
            monitorInterval = intervalSeconds;
        } else {
            throw new IllegalArgumentException("The monitor interval must be greater than or equal to 0.");
        }
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @throws NumberFormatException if the {@code intervalSeconds} argument is not a valid integer representation
     */
    @Override
    public ConfigurationBuilder<T> setMonitorInterval(final @Nullable String intervalSeconds) {
        return setMonitorInterval(intervalSeconds != null ? Integer.parseInt(intervalSeconds) : 0);
    }

    /** {@inheritDoc} */
    @Override
    public ConfigurationBuilder<T> setPackages(final @Nullable String packages) {
        this.packages = packages;
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if the {@code key} argument is {@code null}
     */
    @Override
    public ConfigurationBuilder<T> setRootProperty(final String key, final @Nullable String value) {

        Objects.requireNonNull(key, "The 'key' argument must not be null.");

        if (value != null) {
            root.getAttributes().put(key, value);
        } else {
            root.getAttributes().remove(key);
        }

        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigurationBuilder<T> setShutdownHook(final @Nullable String flag) {
        this.shutdownFlag = flag;
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if the {@code shutdownTimeoutMillis} argument is invalid
     */
    @Override
    public ConfigurationBuilder<T> setShutdownTimeout(final long shutdownTimeoutMillis) {
        if (shutdownTimeoutMillis >= 0) {
            this.shutdownTimeoutMillis = shutdownTimeoutMillis;
        } else {
            throw new IllegalArgumentException(
                    "The 'shutdownTimeoutMillis' argument must be greater than " + "or equal to 0.");
        }
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if the given {@code timeUnit} argument is {@code null}
     */
    @Override
    public ConfigurationBuilder<T> setShutdownTimeout(final long shutdownTimeout, final TimeUnit timeUnit) {
        Objects.requireNonNull(timeUnit, "The 'timeUnit' argument must not be null.");
        this.shutdownTimeoutMillis = timeUnit.toMillis(shutdownTimeout);
        return this;
    }

    /**
     * Sets the status level.
     * @param level the configuration status logging level
     * @return this builder (for chaining)
     */
    @Override
    public ConfigurationBuilder<T> setStatusLevel(final @Nullable Level level) {
        this.statusLevel = level;
        return this;
    }

    /**
     * Sets the verbosity.
     * @param verbosity the configuration verbosity
     * @return this component builder (for chaining)
     * @deprecated This method is ineffective and only kept for binary backward compatibility.
     */
    @Override
    @Deprecated
    public ConfigurationBuilder<T> setVerbosity(final @Nullable String verbosity) {
        return this;
    }

    // XML generation

    private String formatXml(final String xml) throws TransformerException, TransformerFactoryConfigurationError {
        Objects.requireNonNull(xml, "The 'xml' argument must not be null.");
        final StringWriter writer = new StringWriter();
        formatXml(new StreamSource(new StringReader(xml)), new StreamResult(writer));
        return writer.toString();
    }

    @Override
    public void writeXmlConfiguration(final OutputStream output) throws IOException {
        Objects.requireNonNull(output, "The 'output' argument must not be null.");
        try {
            final XMLStreamWriter xmlWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(output);
            writeXmlConfiguration(xmlWriter);
            xmlWriter.close();
        } catch (final XMLStreamException e) {
            if (e.getNestedException() instanceof IOException) {
                throw (IOException) e.getNestedException();
            }
            Throwables.rethrow(e);
        }
    }

    @Override
    public String toXmlConfiguration() {
        final StringWriter writer = new StringWriter();
        try {
            final XMLStreamWriter xmlWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(writer);
            writeXmlConfiguration(xmlWriter);
            xmlWriter.close();
            return formatXml(writer.toString());
        } catch (final XMLStreamException | TransformerException e) {
            Throwables.rethrow(e);
        }
        return writer.toString();
    }

    private void writeXmlConfiguration(final XMLStreamWriter xmlWriter) throws XMLStreamException {
        Objects.requireNonNull(xmlWriter, "The 'xmlWriter' argument must not be null.");
        xmlWriter.writeStartDocument();
        xmlWriter.writeStartElement("Configuration");
        if (configurationName != null) {
            xmlWriter.writeAttribute("name", configurationName);
        }
        if (statusLevel != null) {
            xmlWriter.writeAttribute("status", statusLevel.name());
        }
        if (destination != null) {
            xmlWriter.writeAttribute("dest", destination);
        }
        if (packages != null) {
            xmlWriter.writeAttribute("packages", packages);
        }
        if (shutdownFlag != null) {
            xmlWriter.writeAttribute("shutdownHook", shutdownFlag);
        }
        if (shutdownTimeoutMillis > 0) {
            xmlWriter.writeAttribute("shutdownTimeout", String.valueOf(shutdownTimeoutMillis));
        }
        if (advertiser != null) {
            xmlWriter.writeAttribute("advertiser", advertiser);
        }
        if (monitorInterval > 0) {
            xmlWriter.writeAttribute("monitorInterval", String.valueOf(monitorInterval));
        }

        writeXmlSection(xmlWriter, properties);
        writeXmlSection(xmlWriter, scripts);
        writeXmlSection(xmlWriter, customLevels);
        if (filters.getComponents().size() == 1) {
            writeXmlComponent(xmlWriter, filters.getComponents().get(0));
        } else if (filters.getComponents().size() > 1) {
            writeXmlSection(xmlWriter, filters);
        }
        writeXmlSection(xmlWriter, appenders);
        writeXmlSection(xmlWriter, loggers);

        xmlWriter.writeEndElement(); // "Configuration"
        xmlWriter.writeEndDocument();
    }

    private void writeXmlSection(final XMLStreamWriter xmlWriter, final Component component) throws XMLStreamException {
        Objects.requireNonNull(xmlWriter, "The 'xmlWriter' argument must not be null.");
        Objects.requireNonNull(component, "The 'component' argument must not be null.");
        if (!component.getAttributes().isEmpty()
                || !component.getComponents().isEmpty()
                || component.getValue() != null) {
            writeXmlComponent(xmlWriter, component);
        }
    }

    private void writeXmlComponent(final XMLStreamWriter xmlWriter, final Component component)
            throws XMLStreamException {
        Objects.requireNonNull(xmlWriter, "The 'xmlWriter' argument must not be null.");
        Objects.requireNonNull(component, "The 'component' argument must not be null.");
        if (!component.getComponents().isEmpty() || component.getValue() != null) {
            xmlWriter.writeStartElement(component.getPluginType());
            writeXmlAttributes(xmlWriter, component);
            for (final Component subComponent : component.getComponents()) {
                writeXmlComponent(xmlWriter, subComponent);
            }
            if (component.getValue() != null) {
                xmlWriter.writeCharacters(component.getValue());
            }
            xmlWriter.writeEndElement();
        } else {
            xmlWriter.writeEmptyElement(component.getPluginType());
            writeXmlAttributes(xmlWriter, component);
        }
    }

    private void writeXmlAttributes(final XMLStreamWriter xmlWriter, final Component component)
            throws XMLStreamException {
        Objects.requireNonNull(xmlWriter, "The 'xmlWriter' argument must not be null.");
        Objects.requireNonNull(component, "The 'component' argument must not be null.");
        for (final Map.Entry<String, String> attribute :
                component.getAttributes().entrySet()) {
            xmlWriter.writeAttribute(attribute.getKey(), attribute.getValue());
        }
    }
}
