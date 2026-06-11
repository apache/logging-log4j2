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
package org.apache.logging.log4j.core.config.xml;

import eu.copernik.xml.factory.XmlFactories;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.Reconfigurable;
import org.apache.logging.log4j.core.config.plugins.util.PluginType;
import org.apache.logging.log4j.core.config.status.StatusConfiguration;
import org.apache.logging.log4j.core.internal.annotation.SuppressFBWarnings;
import org.apache.logging.log4j.core.util.Closer;
import org.apache.logging.log4j.core.util.Integers;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.core.util.Patterns;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Creates a Node hierarchy from an XML file.
 */
public class XmlConfiguration extends AbstractConfiguration implements Reconfigurable {

    private static final String ENABLE_XINCLUDE_PROP = "log4j2.configurationEnableXInclude";

    private final List<Status> status = new ArrayList<>();
    private Element rootElement;
    private boolean strict;
    private String schemaResource;

    @SuppressFBWarnings(
            value = "XXE_DOCUMENT",
            justification = "The `newDocumentBuilder` method disables DTD processing.")
    public XmlConfiguration(final LoggerContext loggerContext, final ConfigurationSource configSource) {
        super(loggerContext, configSource);
        byte[] buffer = null;

        try {
            final InputStream configStream = configSource.getInputStream();
            try {
                buffer = toByteArray(configStream);
            } finally {
                Closer.closeSilently(configStream);
            }
            final InputSource source = new InputSource(new ByteArrayInputStream(buffer));
            source.setSystemId(configSource.getLocation());
            final boolean xIncludeAware = PropertiesUtil.getProperties().getBooleanProperty(ENABLE_XINCLUDE_PROP);
            final DocumentBuilder documentBuilder = newDocumentBuilder(xIncludeAware);
            final Document document = documentBuilder.parse(source);
            rootElement = document.getDocumentElement();
            final Map<String, String> attrs = processAttributes(rootNode, rootElement);
            final StatusConfiguration statusConfig = new StatusConfiguration().withStatus(getDefaultStatus());
            int monitorIntervalSeconds = 0;
            for (final Map.Entry<String, String> entry : attrs.entrySet()) {
                final String key = entry.getKey();
                final String value = getConfigurationStrSubstitutor().replace(entry.getValue());
                if ("status".equalsIgnoreCase(key)) {
                    statusConfig.withStatus(value);
                } else if ("dest".equalsIgnoreCase(key)) {
                    statusConfig.withDestination(value);
                } else if ("shutdownHook".equalsIgnoreCase(key)) {
                    isShutdownHookEnabled = !"disable".equalsIgnoreCase(value);
                } else if ("shutdownTimeout".equalsIgnoreCase(key)) {
                    shutdownTimeoutMillis = Long.parseLong(value);
                } else if ("packages".equalsIgnoreCase(key)) {
                    pluginPackages.addAll(Arrays.asList(value.split(Patterns.COMMA_SEPARATOR)));
                } else if ("name".equalsIgnoreCase(key)) {
                    setName(value);
                } else if ("strict".equalsIgnoreCase(key)) {
                    strict = Boolean.parseBoolean(value);
                } else if ("schema".equalsIgnoreCase(key)) {
                    schemaResource = value;
                } else if ("monitorInterval".equalsIgnoreCase(key)) {
                    monitorIntervalSeconds = Integers.parseInt(value);
                } else if ("advertiser".equalsIgnoreCase(key)) {
                    createAdvertiser(value, configSource, buffer, "text/xml");
                }
            }
            initializeWatchers(this, configSource, monitorIntervalSeconds);
            statusConfig.initialize();
        } catch (final SAXException | IOException | ParserConfigurationException e) {
            LOGGER.error("Error parsing " + configSource.getLocation(), e);
            throw new ConfigurationException("Error parsing " + configSource.getLocation(), e);
        }
        if (strict && schemaResource != null && buffer != null) {
            try (final InputStream is =
                    Loader.getResourceAsStream(schemaResource, XmlConfiguration.class.getClassLoader())) {
                if (is != null) {
                    final javax.xml.transform.Source src = new StreamSource(is, schemaResource);
                    final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                    Schema schema = null;
                    try {
                        schema = factory.newSchema(src);
                    } catch (final SAXException ex) {
                        LOGGER.error("Error parsing Log4j schema", ex);
                    }
                    if (schema != null) {
                        final Validator validator = schema.newValidator();
                        try {
                            validator.validate(new StreamSource(new ByteArrayInputStream(buffer)));
                        } catch (final IOException ioe) {
                            LOGGER.error("Error reading configuration for validation", ioe);
                        } catch (final SAXException ex) {
                            LOGGER.error("Error validating configuration", ex);
                        }
                    }
                }
            } catch (final Exception ex) {
                LOGGER.error("Unable to access schema {}", this.schemaResource, ex);
            }
        }

        if (getName() == null) {
            setName(configSource.getLocation());
        }
    }

    /**
     * Creates a new DocumentBuilder suitable for parsing a configuration file.
     *
     * @param enableXInclude enabled XInclude
     * @return a new DocumentBuilder
     * @throws ParserConfigurationException if a DocumentBuilder cannot be created, which satisfies the configuration requested.
     */
    static DocumentBuilder newDocumentBuilder(final boolean enableXInclude) throws ParserConfigurationException {
        final DocumentBuilderFactory factory = XmlFactories.newDocumentBuilderFactory();
        factory.setNamespaceAware(true);
        if (enableXInclude) {
            factory.setXIncludeAware(true);
        }

        final DocumentBuilder builder = factory.newDocumentBuilder();
        if (enableXInclude) {
            builder.setEntityResolver(ConfigurationSourceEntityResolver.INSTANCE);
        }
        return builder;
    }

    @Override
    public void setup() {
        if (rootElement == null) {
            LOGGER.error("No logging configuration");
            return;
        }
        constructHierarchy(rootNode, rootElement);
        if (!status.isEmpty()) {
            for (final Status s : status) {
                LOGGER.error("Error processing element {} ({}): {}", s.name, s.element, s.errorType);
            }
            return;
        }
        rootElement = null;
    }

    @Override
    public Configuration reconfigure() {
        try {
            final ConfigurationSource source = getConfigurationSource().resetInputStream();
            if (source == null) {
                return null;
            }
            final XmlConfiguration config = new XmlConfiguration(getLoggerContext(), source);
            return config.rootElement == null ? null : config;
        } catch (final IOException ex) {
            LOGGER.error("Cannot locate file {}", getConfigurationSource(), ex);
        }
        return null;
    }

    private void constructHierarchy(final Node node, final Element element) {
        processAttributes(node, element);
        final StringBuilder buffer = new StringBuilder();
        final NodeList list = element.getChildNodes();
        final List<Node> children = node.getChildren();
        for (int i = 0; i < list.getLength(); i++) {
            final org.w3c.dom.Node w3cNode = list.item(i);
            if (w3cNode instanceof Element) {
                final Element child = (Element) w3cNode;
                final String name = getType(child);
                final PluginType<?> type = pluginManager.getPluginType(name);
                final Node childNode = new Node(node, name, type);
                constructHierarchy(childNode, child);
                if (type == null) {
                    final String value = childNode.getValue();
                    if (!childNode.hasChildren() && value != null) {
                        node.getAttributes().put(name, value);
                    } else {
                        status.add(new Status(name, element, ErrorType.CLASS_NOT_FOUND));
                    }
                } else {
                    children.add(childNode);
                }
            } else if (w3cNode instanceof Text) {
                final Text data = (Text) w3cNode;
                buffer.append(data.getData());
            }
        }

        final String text = buffer.toString().trim();
        if (!text.isEmpty() || (!node.hasChildren() && !node.isRoot())) {
            node.setValue(text);
        }
    }

    private String getType(final Element element) {
        if (strict) {
            final NamedNodeMap attrs = element.getAttributes();
            for (int i = 0; i < attrs.getLength(); ++i) {
                final org.w3c.dom.Node w3cNode = attrs.item(i);
                if (w3cNode instanceof Attr) {
                    final Attr attr = (Attr) w3cNode;
                    if (attr.getName().equalsIgnoreCase("type")) {
                        final String type = attr.getValue();
                        attrs.removeNamedItem(attr.getName());
                        return type;
                    }
                }
            }
        }
        return element.getTagName();
    }

    private Map<String, String> processAttributes(final Node node, final Element element) {
        final NamedNodeMap attrs = element.getAttributes();
        final Map<String, String> attributes = node.getAttributes();

        for (int i = 0; i < attrs.getLength(); ++i) {
            final org.w3c.dom.Node w3cNode = attrs.item(i);
            if (w3cNode instanceof Attr) {
                final Attr attr = (Attr) w3cNode;
                if (attr.getName().equals("xml:base")) {
                    continue;
                }
                attributes.put(attr.getName(), attr.getValue());
            }
        }
        return attributes;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[location=" + getConfigurationSource() + ", lastModified="
                + Instant.ofEpochMilli(getConfigurationSource().getLastModified()) + "]";
    }

    /**
     * The error that occurred.
     */
    private enum ErrorType {
        CLASS_NOT_FOUND
    }

    /**
     * Status for recording errors.
     */
    private static class Status {
        private final Element element;
        private final String name;
        private final ErrorType errorType;

        public Status(final String name, final Element element, final ErrorType errorType) {
            this.name = name;
            this.element = element;
            this.errorType = errorType;
        }

        @Override
        public String toString() {
            return "Status [name=" + name + ", element=" + element + ", errorType=" + errorType + "]";
        }
    }

    /**
     * Entity resolver that resolves external entities the same way the configuration itself is resolved.
     */
    private static class ConfigurationSourceEntityResolver implements EntityResolver {

        private static final EntityResolver INSTANCE = new ConfigurationSourceEntityResolver();

        @Override
        public InputSource resolveEntity(final String publicId, final String systemId) throws SAXException {
            InputSource source = null;
            try {
                final ConfigurationSource configurationSource = ConfigurationSource.fromUri(new URI(systemId));
                if (configurationSource != null) {
                    source = new InputSource(configurationSource.getInputStream());
                    source.setPublicId(publicId);
                    source.setSystemId(systemId);
                }
            } catch (final URISyntaxException e) {
                throw new SAXException("Unable to resolve system id " + systemId, e);
            }
            if (source == null) {
                throw new SAXException("Unable to resolve entity: " + systemId);
            }
            return source;
        }
    }
}
