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
import java.util.concurrent.TimeUnit;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
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
 * @param <T> The BuiltConfiguration type.
 * @since 2.4
 */
public class DefaultConfigurationBuilder<T extends BuiltConfiguration> implements ConfigurationBuilder<T> {

    private static final String INDENT = "  ";

    private final Component root = new Component();
    private Component loggers;
    private Component appenders;
    private Component filters;
    private Component properties;
    private Component customLevels;
    private Component scripts;
    private final Class<T> clazz;
    private ConfigurationSource source;
    private int monitorInterval;
    private Level level;
    private String destination;
    private String packages;
    private String shutdownFlag;
    private long shutdownTimeoutMillis;
    private String advertiser;
    private LoggerContext loggerContext;
    private String name;

    @SuppressFBWarnings(
            value = {"XXE_DTD_TRANSFORM_FACTORY", "XXE_XSLT_TRANSFORM_FACTORY"},
            justification = "This method only uses internally generated data.")
    public static void formatXml(final Source source, final Result result)
            throws TransformerConfigurationException, TransformerFactoryConfigurationError, TransformerException {
        final Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", Integer.toString(INDENT.length()));
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(source, result);
    }

    @SuppressWarnings("unchecked")
    public DefaultConfigurationBuilder() {
        this((Class<T>) BuiltConfiguration.class);
        root.addAttribute("name", "Built");
    }

    public DefaultConfigurationBuilder(final Class<T> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("A Configuration class must be provided");
        }
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

    protected ConfigurationBuilder<T> add(final Component parent, final ComponentBuilder<?> builder) {
        parent.getComponents().add(builder.build());
        return this;
    }

    @Override
    public ConfigurationBuilder<T> add(final AppenderComponentBuilder builder) {
        return add(appenders, builder);
    }

    @Override
    public ConfigurationBuilder<T> add(final CustomLevelComponentBuilder builder) {
        return add(customLevels, builder);
    }

    @Override
    public ConfigurationBuilder<T> add(final FilterComponentBuilder builder) {
        return add(filters, builder);
    }

    @Override
    public ConfigurationBuilder<T> add(final ScriptComponentBuilder builder) {
        return add(scripts, builder);
    }

    @Override
    public ConfigurationBuilder<T> add(final ScriptFileComponentBuilder builder) {
        return add(scripts, builder);
    }

    @Override
    public ConfigurationBuilder<T> add(final LoggerComponentBuilder builder) {
        return add(loggers, builder);
    }

    @Override
    public ConfigurationBuilder<T> add(final RootLoggerComponentBuilder builder) {
        for (final Component c : loggers.getComponents()) {
            if (c.getPluginType().equals(LoggerConfig.ROOT)) {
                throw new ConfigurationException("Root Logger was previously defined");
            }
        }
        return add(loggers, builder);
    }

    @Override
    public ConfigurationBuilder<T> addProperty(final String key, final String value) {
        properties.addComponent(newComponent(key, "Property", value).build());
        return this;
    }

    @Override
    public T build() {
        return build(true);
    }

    @Override
    public T build(final boolean initialize) {
        T configuration;
        try {
            if (source == null) {
                source = ConfigurationSource.NULL_SOURCE;
            }
            final Constructor<T> constructor =
                    clazz.getConstructor(LoggerContext.class, ConfigurationSource.class, Component.class);
            configuration = constructor.newInstance(loggerContext, source, root);
            configuration.getRootNode().getAttributes().putAll(root.getAttributes());
            if (name != null) {
                configuration.setName(name);
            }
            if (level != null) {
                configuration.getStatusConfiguration().withStatus(level);
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
                configuration.createAdvertiser(advertiser, source);
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

    private String formatXml(final String xml)
            throws TransformerConfigurationException, TransformerException, TransformerFactoryConfigurationError {
        final StringWriter writer = new StringWriter();
        formatXml(new StreamSource(new StringReader(xml)), new StreamResult(writer));
        return writer.toString();
    }

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
        if (name != null) {
            xmlWriter.writeAttribute("name", name);
        }
        if (level != null) {
            xmlWriter.writeAttribute("status", level.name());
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

    @Override
    public ScriptComponentBuilder newScript(final String name, final String language, final String text) {
        return new DefaultScriptComponentBuilder(this, name, language, text);
    }

    @Override
    public ScriptFileComponentBuilder newScriptFile(final String path) {
        return new DefaultScriptFileComponentBuilder(this, path, path);
    }

    @Override
    public ScriptFileComponentBuilder newScriptFile(final String name, final String path) {
        return new DefaultScriptFileComponentBuilder(this, name, path);
    }

    @Override
    public AppenderComponentBuilder newAppender(final String name, final String type) {
        return new DefaultAppenderComponentBuilder(this, name, type);
    }

    @Override
    public AppenderRefComponentBuilder newAppenderRef(final String ref) {
        return new DefaultAppenderRefComponentBuilder(this, ref);
    }

    @Override
    public LoggerComponentBuilder newAsyncLogger(final String name) {
        return new DefaultLoggerComponentBuilder(this, name, null, "AsyncLogger");
    }

    @Override
    public LoggerComponentBuilder newAsyncLogger(final String name, final boolean includeLocation) {
        return new DefaultLoggerComponentBuilder(this, name, null, "AsyncLogger", includeLocation);
    }

    @Override
    public LoggerComponentBuilder newAsyncLogger(final String name, final Level level) {
        return new DefaultLoggerComponentBuilder(this, name, level.toString(), "AsyncLogger");
    }

    @Override
    public LoggerComponentBuilder newAsyncLogger(final String name, final Level level, final boolean includeLocation) {
        return new DefaultLoggerComponentBuilder(this, name, level.toString(), "AsyncLogger", includeLocation);
    }

    @Override
    public LoggerComponentBuilder newAsyncLogger(final String name, final String level) {
        return new DefaultLoggerComponentBuilder(this, name, level, "AsyncLogger");
    }

    @Override
    public LoggerComponentBuilder newAsyncLogger(final String name, final String level, final boolean includeLocation) {
        return new DefaultLoggerComponentBuilder(this, name, level, "AsyncLogger", includeLocation);
    }

    @Override
    public RootLoggerComponentBuilder newAsyncRootLogger() {
        return new DefaultRootLoggerComponentBuilder(this, "AsyncRoot");
    }

    @Override
    public RootLoggerComponentBuilder newAsyncRootLogger(final boolean includeLocation) {
        return new DefaultRootLoggerComponentBuilder(this, null, "AsyncRoot", includeLocation);
    }

    @Override
    public RootLoggerComponentBuilder newAsyncRootLogger(final Level level) {
        return new DefaultRootLoggerComponentBuilder(this, level.toString(), "AsyncRoot");
    }

    @Override
    public RootLoggerComponentBuilder newAsyncRootLogger(final Level level, final boolean includeLocation) {
        return new DefaultRootLoggerComponentBuilder(this, level.toString(), "AsyncRoot", includeLocation);
    }

    @Override
    public RootLoggerComponentBuilder newAsyncRootLogger(final String level) {
        return new DefaultRootLoggerComponentBuilder(this, level, "AsyncRoot");
    }

    @Override
    public RootLoggerComponentBuilder newAsyncRootLogger(final String level, final boolean includeLocation) {
        return new DefaultRootLoggerComponentBuilder(this, level, "AsyncRoot", includeLocation);
    }

    @Override
    public <B extends ComponentBuilder<B>> ComponentBuilder<B> newComponent(final String type) {
        return new DefaultComponentBuilder<>(this, type);
    }

    @Override
    public <B extends ComponentBuilder<B>> ComponentBuilder<B> newComponent(final String name, final String type) {
        return new DefaultComponentBuilder<>(this, name, type);
    }

    @Override
    public <B extends ComponentBuilder<B>> ComponentBuilder<B> newComponent(
            final String name, final String type, final String value) {
        return new DefaultComponentBuilder<>(this, name, type, value);
    }

    @Override
    public PropertyComponentBuilder newProperty(final String name, final String value) {
        return new DefaultPropertyComponentBuilder(this, name, value);
    }

    @Override
    public KeyValuePairComponentBuilder newKeyValuePair(final String key, final String value) {
        return new DefaultKeyValuePairComponentBuilder(this, key, value);
    }

    @Override
    public CustomLevelComponentBuilder newCustomLevel(final String name, final int level) {
        return new DefaultCustomLevelComponentBuilder(this, name, level);
    }

    @Override
    public FilterComponentBuilder newFilter(
            final String type, final Filter.Result onMatch, final Filter.Result onMismatch) {
        return new DefaultFilterComponentBuilder(this, type, onMatch.name(), onMismatch.name());
    }

    @Override
    public FilterComponentBuilder newFilter(final String type, final String onMatch, final String onMismatch) {
        return new DefaultFilterComponentBuilder(this, type, onMatch, onMismatch);
    }

    @Override
    public LayoutComponentBuilder newLayout(final String type) {
        return new DefaultLayoutComponentBuilder(this, type);
    }

    @Override
    public LoggerComponentBuilder newLogger(final String name) {
        return new DefaultLoggerComponentBuilder(this, name, null);
    }

    @Override
    public LoggerComponentBuilder newLogger(final String name, final boolean includeLocation) {
        return new DefaultLoggerComponentBuilder(this, name, null, includeLocation);
    }

    @Override
    public LoggerComponentBuilder newLogger(final String name, final Level level) {
        return new DefaultLoggerComponentBuilder(this, name, level.toString());
    }

    @Override
    public LoggerComponentBuilder newLogger(final String name, final Level level, final boolean includeLocation) {
        return new DefaultLoggerComponentBuilder(this, name, level.toString(), includeLocation);
    }

    @Override
    public LoggerComponentBuilder newLogger(final String name, final String level) {
        return new DefaultLoggerComponentBuilder(this, name, level);
    }

    @Override
    public LoggerComponentBuilder newLogger(final String name, final String level, final boolean includeLocation) {
        return new DefaultLoggerComponentBuilder(this, name, level, includeLocation);
    }

    @Override
    public RootLoggerComponentBuilder newRootLogger() {
        return new DefaultRootLoggerComponentBuilder(this, null);
    }

    @Override
    public RootLoggerComponentBuilder newRootLogger(final boolean includeLocation) {
        return new DefaultRootLoggerComponentBuilder(this, null, includeLocation);
    }

    @Override
    public RootLoggerComponentBuilder newRootLogger(final Level level) {
        return new DefaultRootLoggerComponentBuilder(this, level.toString());
    }

    @Override
    public RootLoggerComponentBuilder newRootLogger(final Level level, final boolean includeLocation) {
        return new DefaultRootLoggerComponentBuilder(this, level.toString(), includeLocation);
    }

    @Override
    public RootLoggerComponentBuilder newRootLogger(final String level) {
        return new DefaultRootLoggerComponentBuilder(this, level);
    }

    @Override
    public RootLoggerComponentBuilder newRootLogger(final String level, final boolean includeLocation) {
        return new DefaultRootLoggerComponentBuilder(this, level, includeLocation);
    }

    @Override
    public ConfigurationBuilder<T> setAdvertiser(final String advertiser) {
        this.advertiser = advertiser;
        return this;
    }

    /**
     * Set the name of the configuration.
     *
     * @param name the name of the {@link Configuration}. By default is {@code "Assembled"}.
     * @return this builder instance
     */
    @Override
    public ConfigurationBuilder<T> setConfigurationName(final String name) {
        this.name = name;
        return this;
    }

    /**
     * Set the ConfigurationSource.
     *
     * @param configurationSource the {@link ConfigurationSource}
     * @return this builder instance
     */
    @Override
    public ConfigurationBuilder<T> setConfigurationSource(final ConfigurationSource configurationSource) {
        source = configurationSource;
        return this;
    }

    @Override
    public ConfigurationBuilder<T> setMonitorInterval(final String intervalSeconds) {
        monitorInterval = Integers.parseInt(intervalSeconds);
        return this;
    }

    @Override
    public ConfigurationBuilder<T> setPackages(final String packages) {
        this.packages = packages;
        return this;
    }

    @Override
    public ConfigurationBuilder<T> setShutdownHook(final String flag) {
        this.shutdownFlag = flag;
        return this;
    }

    @Override
    public ConfigurationBuilder<T> setShutdownTimeout(final long timeout, final TimeUnit timeUnit) {
        this.shutdownTimeoutMillis = timeUnit.toMillis(timeout);
        return this;
    }

    @Override
    public ConfigurationBuilder<T> setStatusLevel(final Level level) {
        this.level = level;
        return this;
    }

    /**
     * @deprecated This method is ineffective and only kept for binary backward compatibility.
     */
    @Override
    @Deprecated
    public ConfigurationBuilder<T> setVerbosity(final String verbosity) {
        return this;
    }

    @Override
    public ConfigurationBuilder<T> setDestination(final String destination) {
        this.destination = destination;
        return this;
    }

    @Override
    public void setLoggerContext(final LoggerContext loggerContext) {
        this.loggerContext = loggerContext;
    }

    @Override
    public ConfigurationBuilder<T> addRootProperty(final String key, final String value) {
        root.getAttributes().put(key, value);
        return this;
    }
}
