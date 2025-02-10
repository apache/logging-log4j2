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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
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
import org.apache.logging.log4j.core.util.Integers;
import org.apache.logging.log4j.core.util.Throwables;

/**
 * A default ConfigurationBuilder that is used to build a BuiltConfiguration instance.
 *
 * @param <T> The BuiltConfiguration type.
 * @since 2.4
 */
public class DefaultConfigurationBuilder<T extends BuiltConfiguration> implements ConfigurationBuilder<T> {

    /** Indentation prefix used when generating an XML representation of this builder's configuration. */
    private static final String XML_INDENT = "  ";

    /** The class of the configuration instance being built. */
    private final Class<T> configurationClass;

    /** The root component. */
    private final Component root = new Component();

    /** Standard component: "Appenders". */
    private final Component appenders;

    /** Standard component: "CustomLevels". */
    private final Component customLevels;

    /** Standard component: "Filters". */
    private final Component filters;

    /** Standard component: "Loggers". */
    private final Component loggers;

    /** Standard component: "Properties". */
    private final Component properties;

    /** Standard component: "Scripts". */
    private final Component scripts;

    /** The configuration source passed to the constructor of the built configuration instance. */
    private ConfigurationSource source;

    /** The logger context passed to the constructor of the built configuration instance. */
    private LoggerContext loggerContext;

    /**
     * Constructs a new instance with a standard {@link BuiltConfiguration} target type.
     */
    @SuppressWarnings("unchecked")
    public DefaultConfigurationBuilder() {
        this((Class<T>) BuiltConfiguration.class);
        root.addAttribute("name", "Built");
    }

    /**
     * Constructs a new instance with the given {@link BuiltConfiguration} implementation class.
     * @param clazz the {@code Class} of the {@code BuiltConfiguration} implementation to build
     * @throws NullPointerException if the argument is null
     */
    public DefaultConfigurationBuilder(final Class<T> clazz) {

        super();

        this.configurationClass = Objects.requireNonNull(clazz, "The 'configurationClass' argument must not be null.");
        this.root.addAttribute("name", "Built");

        properties = getOrCreateComponent("Properties");
        scripts = getOrCreateComponent("Scripts");
        customLevels = getOrCreateComponent("CustomLevels");
        filters = getOrCreateComponent("Filters");
        appenders = getOrCreateComponent("Appenders");
        loggers = getOrCreateComponent("Loggers");
    }

    //
    // ACCESSORS / MUTATORS
    //

    /**
     * Adds the component built by the provided builder as a child component of the given parent component.
     *
     * @param parent the parent component to add to
     * @param builder the builder to generate the child component from
     * @throws ConfigurationException if an error occurs while building the given builder
     * @throws NullPointerException if either argument is null
     */
    protected ConfigurationBuilder<T> add(final Component parent, final ComponentBuilder<?> builder) {
        Objects.requireNonNull(parent, "The 'parent' component must not be null.");
        Objects.requireNonNull(builder, "The 'builder' component must not be null.");
        parent.getComponents().add(builder.build());
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigurationBuilder<T> add(final AppenderComponentBuilder builder) {
        add(this.appenders, builder);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigurationBuilder<T> add(final CustomLevelComponentBuilder builder) {
        add(this.customLevels, builder);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigurationBuilder<T> add(final FilterComponentBuilder builder) {
        add(this.filters, builder);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigurationBuilder<T> add(final ScriptComponentBuilder builder) {
        add(this.scripts, builder);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigurationBuilder<T> add(final ScriptFileComponentBuilder builder) {
        add(this.scripts, builder);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigurationBuilder<T> add(final LoggerComponentBuilder builder) {
        add(this.loggers, builder);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigurationBuilder<T> add(RootLoggerComponentBuilder builder) {
        for (final Component c : loggers.getComponents()) {
            if (c.getPluginType().equals(LoggerConfig.ROOT)) {
                throw new ConfigurationException("Root Logger was previously defined");
            }
        }
        add(this.loggers, builder);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigurationBuilder<T> addProperty(final String key, final String value) {
        add(this.properties, newProperty(key, value));
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigurationBuilder<T> addProperty(final PropertyComponentBuilder builder) {
        add(this.properties, builder);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigurationBuilder<T> addRootProperty(String key, String value) {
        Objects.requireNonNull(key, "The 'key' argument must not be null.");
        if (key.isEmpty()) {
            throw new IllegalArgumentException("The 'key' argument must not be empty.");
        }
        if (value != null) {
            this.getRootComponent().getAttributes().put(key, value);
        } else {
            this.getRootComponent().getAttributes().remove(key);
        }
        return this;
    }

    /**
     * Gets the value of a property (attribute) on the root component.
     * @param key the property key
     * @return an optional containing the property value or that is <i>empty</i> if not set
     */
    protected Optional<String> getRootProperty(String key) {
        Objects.requireNonNull(key, "The 'key' argument must not be null.");
        if (key.isEmpty()) {
            throw new IllegalArgumentException("The 'key' argument must not be empty.");
        }
        return Optional.ofNullable(this.getRootComponent().getAttributes().get(key));
    }

    /**
     * Gets the child component with the given plugin-type.
     * @param pluginType the plugin-type to lookup
     * @return an optional containing the resolved component or that is <i>empty</i> if not found
     */
    protected Optional<Component> getComponent(final String pluginType) {
        Objects.requireNonNull(pluginType, "The 'pluginType' argument must not be null.");
        return this.root.getComponents().stream()
                .filter(c -> c.getPluginType().equals(pluginType))
                .findFirst();
    }

    /**
     * Gets the root component.
     *
     * @return the root component (will never be null)
     */
    protected Component getRootComponent() {
        return this.root;
    }

    /**
     * Gets or creates the child component with the given plugin-type.
     * <p>
     *     The lookup is case-sensitive.
     * </p>
     *
     * @param pluginType the plugin-type to lookup
     * @return either the existing component or a new component with the given plugin-type if not found (never null)
     */
    protected Component getOrCreateComponent(String pluginType) {
        Objects.requireNonNull(pluginType, "The 'pluginType' must not be null.");
        return getComponent(pluginType).orElseGet(() -> {
            Component c = new Component(pluginType);
            this.root.getComponents().add(c);
            return c;
        });
    }

    /**
     * Gets the value of the "advertiser" property.
     * @return an optional containing the property value or that is <i>empty</i> if undefined
     */
    protected Optional<String> getAdvertiser() {
        return getRootProperty("advertiser");
    }

    /**
     * Gets the value of the "name" property.
     * @return an optional containing the property value or that is <i>empty</i> if undefined
     */
    protected Optional<String> getConfigurationName() {
        return getRootProperty("name");
    }

    /**
     * Gets the configuration-source.
     * @return an optional containing the configuration-source or that is <i>empy</i> if undefined
     */
    protected Optional<ConfigurationSource> getConfigurationSource() {
        return Optional.ofNullable(this.source);
    }

    /**
     * Gets the value of the "dest" property.
     * @return an optional containing the property value or that is <i>empty</i> if undefined
     */
    protected Optional<String> getDestination() {
        return getRootProperty("dest");
    }

    /**
     * Gets the logger-context.
     *
     * @return an optional containing the logger-context or that is <i>empty</i> if undefined
     */
    protected Optional<LoggerContext> getLoggerContext() {
        return Optional.ofNullable(this.loggerContext);
    }

    /**
     * Returns the configured monitor interval (in seconds) as an {@code Integer}.
     * @return an optional containing the configured monitor interval or that is <i>empty</i> if undefined (or invalid)
     */
    protected Optional<Integer> getMonitorInterval() {
        try {
            return getRootProperty("monitorInterval").map(Integer::parseInt);
        } catch (final Exception ex) {
            return Optional.empty();
        }
    }

    /**
     * Gets the value of the "packages" property.
     * @return an optional containing the property value or that is <i>empty</i> if undefined
     */
    protected Optional<String> getPackages() {
        return getRootProperty("packages");
    }

    /**
     * Gets the value of the "shutdownHook" property.
     * @return an optional containing the property value or that is <i>empty</i> if undefined
     */
    protected Optional<String> getShutdownHook() {
        return getRootProperty("shutdownHook");
    }

    /**
     * Gets the value of the "shutdownTimeout" property as a {@code Long}.
     * @return an optional containing the property value or that is <i>empty</i> if undefined
     */
    protected Optional<Long> getShutdownTimeout() {
        try {
            return getRootProperty("shutdownTimeout").map(Long::parseLong);
        } catch (final Exception ex) {
            return Optional.empty();
        }
    }

    /**
     * Gets the value of the "status" property as a {@code Level}.
     * @return an optional containing the property value or that is <i>empty</i> if undefined
     */
    protected Optional<Level> getStatusLevel() {
        return getRootProperty("status").map(Level::getLevel);
    }

    /**
     * Gets the "Appenders" component.
     *
     * @return the appenders component
     */
    protected final Component getAppenders() {
        return this.appenders;
    }

    /**
     * Gets the "CustomLevels" component.
     *
     * @return the custom-levels component
     */
    protected final Component getCustomLevels() {
        return this.customLevels;
    }

    /**
     * Gets the "Filters" component.
     *
     * @return the filters component
     */
    protected final Component getFilters() {
        return this.filters;
    }

    /**
     * Gets the "Loggers" component.
     *
     * @return the loggers component
     */
    protected final Component getLoggers() {
        return this.loggers;
    }

    /**
     * Gets the "Properties" component.
     *
     * @return the properties component
     */
    protected final Component getProperties() {
        return this.properties;
    }

    /**
     * Gets the "Scripts" component.
     *
     * @return the scripts component
     */
    protected final Component getScripts() {
        return this.scripts;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigurationBuilder<T> setAdvertiser(final String advertiser) {
        this.addRootProperty("advertiser", advertiser);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigurationBuilder<T> setConfigurationName(final String name) {
        this.addRootProperty("name", name);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigurationBuilder<T> setConfigurationSource(final ConfigurationSource configurationSource) {
        source = configurationSource;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigurationBuilder<T> setDestination(final String destination) {
        this.addRootProperty("dest", (destination == null) ? null : destination.trim());
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigurationBuilder<T> setMonitorInterval(final String intervalSeconds) {
        int iMonitorInterval = 0;
        if (intervalSeconds != null && !intervalSeconds.isEmpty() && !"0".equals(intervalSeconds.trim())) {
            try {
                iMonitorInterval = Integers.parseInt(intervalSeconds.trim());
            } catch (final Exception ex) {
                throw new IllegalArgumentException("Invalid monitor interval: " + intervalSeconds, ex);
            }
        }
        return this.setMonitorInterval(iMonitorInterval);
    }

    /** {@inheritDoc} */
    @Override
    public ConfigurationBuilder<T> setMonitorInterval(final int intervalSeconds) {
        addRootProperty("monitorInterval", intervalSeconds <= 0 ? null : String.valueOf(intervalSeconds));
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigurationBuilder<T> setPackages(final String packages) {
        addRootProperty("packages", packages);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigurationBuilder<T> setShutdownHook(final String flag) {
        addRootProperty("shutdownHook", flag);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigurationBuilder<T> setShutdownTimeout(final String timeout, final TimeUnit timeUnit) {
        Objects.requireNonNull(timeUnit, "The 'timeUnit' argument must not be null.");
        long shutdownTimeoutMillis = 0L;
        if (timeout != null && !timeout.isEmpty()) {
            try {
                shutdownTimeoutMillis = timeUnit.toMillis(Long.parseLong(timeout));
            } catch (final Exception ex) {
                throw new IllegalArgumentException("Invalid shutdown timeout: " + timeout, ex);
            }
        }
        return this.setShutdownTimeout(shutdownTimeoutMillis, timeUnit);
    }

    /** {@inheritDoc} */
    @Override
    public ConfigurationBuilder<T> setShutdownTimeout(final long timeout, final TimeUnit timeUnit) {
        Objects.requireNonNull(timeUnit, "The 'timeUnit' argument must not be null.");
        return this.setShutdownTimeout(timeUnit.toMillis(timeout));
    }

    /** {@inheritDoc} */
    @Override
    public ConfigurationBuilder<T> setShutdownTimeout(final long timeoutMillis) {
        this.addRootProperty("shutdownTimeout", timeoutMillis <= 0L ? null : String.valueOf(timeoutMillis));
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigurationBuilder<T> setStatusLevel(final Level level) {
        this.addRootProperty("status", (level != null) ? level.toString() : null);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigurationBuilder<T> setStatusLevel(final String level) {
        return this.setStatusLevel((level != null) ? Level.getLevel(level.trim()) : null);
    }

    /**
     * {@inheritDoc}
     * @deprecated This method is ineffective and only kept for binary backward compatibility.
     */
    @Override
    @Deprecated
    public ConfigurationBuilder<T> setVerbosity(final String verbosity) {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public void setLoggerContext(final LoggerContext loggerContext) {
        this.loggerContext = loggerContext;
    }

    //
    // BUILD
    //

    /** {@inheritDoc} */
    @Override
    public T build() {
        return build(true);
    }

    /** {@inheritDoc} */
    @Override
    public T build(final boolean initialize) {
        if (source == null) {
            source = ConfigurationSource.NULL_SOURCE;
        }
        T configuration = createNewConfigurationInstance(this.configurationClass);
        if (initialize) {
            configuration.initialize();
        }
        return configuration;
    }

    /**
     * Instantiate a new instance of the {@code Configuration} implementation.
     * <p>
     *     Subclasses may override this if they need to provide some non-standard behaviour.
     * </p>
     *
     * @return the new configuration instance
     * @throws IllegalStateException if the configuration cannot be instantiated
     */
    protected T createNewConfigurationInstance(Class<T> configurationClass) {
        Objects.requireNonNull(configurationClass, "The 'configurationClass' argument must not be null.");
        try {
            final Constructor<T> constructor =
                    configurationClass.getConstructor(LoggerContext.class, ConfigurationSource.class, Component.class);
            return constructor.newInstance(loggerContext, source, root);
        } catch (final Exception ex) {
            throw new IllegalStateException(
                    "Configuration class '" + configurationClass.getName() + "' cannot be instantiated.", ex);
        }
    }

    //
    // BUILDER FACTORY METHODS
    //

    /** {@inheritDoc} */
    @Override
    public ScriptComponentBuilder newScript(final String name, final String language, final String text) {
        return new DefaultScriptComponentBuilder(this, name, language, text);
    }

    /** {@inheritDoc} */
    @Override
    public ScriptFileComponentBuilder newScriptFile(final String path) {
        return new DefaultScriptFileComponentBuilder(this, path, path);
    }

    /** {@inheritDoc} */
    @Override
    public ScriptFileComponentBuilder newScriptFile(final String name, final String path) {
        return new DefaultScriptFileComponentBuilder(this, name, path);
    }

    /** {@inheritDoc} */
    @Override
    public AppenderComponentBuilder newAppender(final String name, final String type) {
        return new DefaultAppenderComponentBuilder(this, name, type);
    }

    /** {@inheritDoc} */
    @Override
    public AppenderRefComponentBuilder newAppenderRef(final String ref) {
        return new DefaultAppenderRefComponentBuilder(this, ref);
    }

    /** {@inheritDoc} */
    @Override
    public LoggerComponentBuilder newAsyncLogger(final String name) {
        return new DefaultLoggerComponentBuilder(this, name, null, "AsyncLogger");
    }

    /** {@inheritDoc} */
    @Override
    public LoggerComponentBuilder newAsyncLogger(final String name, final boolean includeLocation) {
        return new DefaultLoggerComponentBuilder(this, name, null, "AsyncLogger", includeLocation);
    }

    /** {@inheritDoc} */
    @Override
    public LoggerComponentBuilder newAsyncLogger(final String name, final Level level) {
        return new DefaultLoggerComponentBuilder(this, name, level.toString(), "AsyncLogger");
    }

    /** {@inheritDoc} */
    @Override
    public LoggerComponentBuilder newAsyncLogger(final String name, final Level level, final boolean includeLocation) {
        return new DefaultLoggerComponentBuilder(this, name, level.toString(), "AsyncLogger", includeLocation);
    }

    /** {@inheritDoc} */
    @Override
    public LoggerComponentBuilder newAsyncLogger(final String name, final String level) {
        return new DefaultLoggerComponentBuilder(this, name, level, "AsyncLogger");
    }

    /** {@inheritDoc} */
    @Override
    public LoggerComponentBuilder newAsyncLogger(final String name, final String level, final boolean includeLocation) {
        return new DefaultLoggerComponentBuilder(this, name, level, "AsyncLogger", includeLocation);
    }

    /** {@inheritDoc} */
    @Override
    public RootLoggerComponentBuilder newAsyncRootLogger() {
        return new DefaultRootLoggerComponentBuilder(this, "AsyncRoot");
    }

    /** {@inheritDoc} */
    @Override
    public RootLoggerComponentBuilder newAsyncRootLogger(final boolean includeLocation) {
        return new DefaultRootLoggerComponentBuilder(this, null, "AsyncRoot", includeLocation);
    }

    /** {@inheritDoc} */
    @Override
    public RootLoggerComponentBuilder newAsyncRootLogger(final Level level) {
        return new DefaultRootLoggerComponentBuilder(this, level.toString(), "AsyncRoot");
    }

    /** {@inheritDoc} */
    @Override
    public RootLoggerComponentBuilder newAsyncRootLogger(final Level level, final boolean includeLocation) {
        return new DefaultRootLoggerComponentBuilder(this, level.toString(), "AsyncRoot", includeLocation);
    }

    /** {@inheritDoc} */
    @Override
    public RootLoggerComponentBuilder newAsyncRootLogger(final String level) {
        return new DefaultRootLoggerComponentBuilder(this, level, "AsyncRoot");
    }

    /** {@inheritDoc} */
    @Override
    public RootLoggerComponentBuilder newAsyncRootLogger(final String level, final boolean includeLocation) {
        return new DefaultRootLoggerComponentBuilder(this, level, "AsyncRoot", includeLocation);
    }

    /** {@inheritDoc} */
    @Override
    public <B extends ComponentBuilder<B>> ComponentBuilder<B> newComponent(final String type) {
        return new DefaultComponentBuilder<>(this, type);
    }

    /** {@inheritDoc} */
    @Override
    public <B extends ComponentBuilder<B>> ComponentBuilder<B> newComponent(final String name, final String type) {
        return new DefaultComponentBuilder<>(this, name, type);
    }

    /** {@inheritDoc} */
    @Override
    public <B extends ComponentBuilder<B>> ComponentBuilder<B> newComponent(
            final String name, final String type, final String value) {
        return new DefaultComponentBuilder<>(this, name, type, value);
    }

    /** {@inheritDoc} */
    @Override
    public PropertyComponentBuilder newProperty(final String name, final String value) {
        return new DefaultPropertyComponentBuilder(this, name, value);
    }

    /** {@inheritDoc} */
    @Override
    public KeyValuePairComponentBuilder newKeyValuePair(final String key, final String value) {
        return new DefaultKeyValuePairComponentBuilder(this, key, value);
    }

    /** {@inheritDoc} */
    @Override
    public CustomLevelComponentBuilder newCustomLevel(final String name, final int level) {
        return new DefaultCustomLevelComponentBuilder(this, name, level);
    }

    /** {@inheritDoc} */
    @Override
    public FilterComponentBuilder newFilter(
            final String type, final Filter.Result onMatch, final Filter.Result onMismatch) {
        return new DefaultFilterComponentBuilder(this, type, onMatch.name(), onMismatch.name());
    }

    /** {@inheritDoc} */
    @Override
    public FilterComponentBuilder newFilter(final String type, final String onMatch, final String onMismatch) {
        return new DefaultFilterComponentBuilder(this, type, onMatch, onMismatch);
    }

    /** {@inheritDoc} */
    @Override
    public LayoutComponentBuilder newLayout(final String type) {
        return new DefaultLayoutComponentBuilder(this, type);
    }

    /** {@inheritDoc} */
    @Override
    public LoggerComponentBuilder newLogger(final String name) {
        return new DefaultLoggerComponentBuilder(this, name, null);
    }

    /** {@inheritDoc} */
    @Override
    public LoggerComponentBuilder newLogger(final String name, final boolean includeLocation) {
        return new DefaultLoggerComponentBuilder(this, name, null, includeLocation);
    }

    /** {@inheritDoc} */
    @Override
    public LoggerComponentBuilder newLogger(final String name, final Level level) {
        return new DefaultLoggerComponentBuilder(this, name, level.toString());
    }

    /** {@inheritDoc} */
    @Override
    public LoggerComponentBuilder newLogger(final String name, final Level level, final boolean includeLocation) {
        return new DefaultLoggerComponentBuilder(this, name, level.toString(), includeLocation);
    }

    /** {@inheritDoc} */
    @Override
    public LoggerComponentBuilder newLogger(final String name, final String level) {
        return new DefaultLoggerComponentBuilder(this, name, level);
    }

    /** {@inheritDoc} */
    @Override
    public LoggerComponentBuilder newLogger(final String name, final String level, final boolean includeLocation) {
        return new DefaultLoggerComponentBuilder(this, name, level, includeLocation);
    }

    /** {@inheritDoc} */
    @Override
    public RootLoggerComponentBuilder newRootLogger() {
        return new DefaultRootLoggerComponentBuilder(this, null);
    }

    /** {@inheritDoc} */
    @Override
    public RootLoggerComponentBuilder newRootLogger(final boolean includeLocation) {
        return new DefaultRootLoggerComponentBuilder(this, null, includeLocation);
    }

    /** {@inheritDoc} */
    @Override
    public RootLoggerComponentBuilder newRootLogger(final Level level) {
        return new DefaultRootLoggerComponentBuilder(this, level.toString());
    }

    /** {@inheritDoc} */
    @Override
    public RootLoggerComponentBuilder newRootLogger(final Level level, final boolean includeLocation) {
        return new DefaultRootLoggerComponentBuilder(this, level.toString(), includeLocation);
    }

    /** {@inheritDoc} */
    @Override
    public RootLoggerComponentBuilder newRootLogger(final String level) {
        return new DefaultRootLoggerComponentBuilder(this, level);
    }

    /** {@inheritDoc} */
    @Override
    public RootLoggerComponentBuilder newRootLogger(final String level, final boolean includeLocation) {
        return new DefaultRootLoggerComponentBuilder(this, level, includeLocation);
    }

    //
    // XML SERIALIZATION
    //

    private String formatXml(final String xml) throws TransformerException, TransformerFactoryConfigurationError {
        final StringWriter writer = new StringWriter();
        formatXml(new StreamSource(new StringReader(xml)), new StreamResult(writer));
        return writer.toString();
    }

    @SuppressFBWarnings(
            value = {"XXE_DTD_TRANSFORM_FACTORY", "XXE_XSLT_TRANSFORM_FACTORY"},
            justification = "This method only uses internally generated data.")
    public static void formatXml(final Source source, final Result result)
            throws TransformerFactoryConfigurationError, TransformerException {
        final Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(
                "{http://xml.apache.org/xslt}indent-amount", Integer.toString(XML_INDENT.length()));
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(source, result);
    }

    /** {@inheritDoc} */
    @Override
    public void writeXmlConfiguration(final OutputStream output) throws IOException {
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

    /** {@inheritDoc} */
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
        xmlWriter.writeStartDocument();
        xmlWriter.writeStartElement("Configuration");
        writeXmlAttributes(xmlWriter);
        writeXmlSections(xmlWriter);
        xmlWriter.writeEndElement(); // "Configuration"
        xmlWriter.writeEndDocument();
    }

    /**
     * Writes the top-level attributes of the main "Configuration" root element.
     *
     * @param xmlWriter the XML writer to write to
     * @throws XMLStreamException if an error occurs while writing the XML document
     */
    protected void writeXmlAttributes(final XMLStreamWriter xmlWriter) throws XMLStreamException {
        writeXmlAttribute(xmlWriter, "name", this.getConfigurationName().orElse(null));
        writeXmlAttribute(
                xmlWriter, "status", this.getStatusLevel().map(Level::toString).orElse(null));
        writeXmlAttribute(xmlWriter, "dest", this.getDestination().orElse(null));
        writeXmlAttribute(xmlWriter, "packages", this.getPackages().orElse(null));
        writeXmlAttribute(xmlWriter, "shutdownHook", this.getShutdownHook().orElse(null));
        writeXmlAttribute(
                xmlWriter,
                "shutdownTimeout",
                this.getShutdownTimeout().map(String::valueOf).orElse(null));
        writeXmlAttribute(xmlWriter, "advertiser", this.getAdvertiser().orElse(null));
        writeXmlAttribute(
                xmlWriter,
                "monitorInterval",
                this.getMonitorInterval().map(String::valueOf).orElse(null));
    }

    /**
     * Writes the sections (sub-elements) of the main "Configuration" root element.
     * @param xmlWriter the XML writer to write to
     * @throws XMLStreamException if an error occurs while writing the XML document
     */
    protected void writeXmlSections(final XMLStreamWriter xmlWriter) throws XMLStreamException {
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
    }

    /**
     * Writes an attribute to the given writer.
     * <p>
     *     If the provided value is {@code null} no attribute will be written.
     * </p>
     * @param xmlWriter the writer to write to
     * @param name the attribute name
     * @param value the attribute value (if {@code null} no action will be taken
     * @throws NullPointerException if either the {@code xmlWriter} or {@code name} attributes are {@code null}
     * @throws IllegalArgumentException if the {@code name} attribute is blank
     * @throws XMLStreamException if an error occurs while writing the attribute
     */
    private void writeXmlAttribute(XMLStreamWriter xmlWriter, String name, String value) throws XMLStreamException {
        Objects.requireNonNull(xmlWriter, "The 'xmlWriter' argument must not be null.");
        Objects.requireNonNull(name, "The 'name' argument must not be null.");
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("The 'name' argument must not be blank.");
        }
        if (value != null) {
            xmlWriter.writeAttribute(name, value);
        }
    }

    private void writeXmlSection(final XMLStreamWriter xmlWriter, final Component component) throws XMLStreamException {
        if (!component.getAttributes().isEmpty()
                || !component.getComponents().isEmpty()
                || component.getValue() != null) {
            writeXmlComponent(xmlWriter, component);
        }
    }

    private void writeXmlComponent(final XMLStreamWriter xmlWriter, final Component component)
            throws XMLStreamException {
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
        for (final Map.Entry<String, String> attribute :
                component.getAttributes().entrySet()) {
            xmlWriter.writeAttribute(attribute.getKey(), attribute.getValue());
        }
    }
}
