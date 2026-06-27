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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
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
import org.apache.logging.log4j.core.config.plugins.util.PluginManager;
import org.apache.logging.log4j.core.config.plugins.util.PluginType;
import org.apache.logging.log4j.core.config.status.StatusConfiguration;
import org.apache.logging.log4j.core.internal.annotation.SuppressFBWarnings;
import org.apache.logging.log4j.core.util.Closer;
import org.apache.logging.log4j.core.util.Integers;
import org.apache.logging.log4j.core.util.NetUtils;
import org.apache.logging.log4j.core.util.Patterns;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.ls.LSException;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Creates a Node hierarchy from an XML file.
 */
public class XmlConfiguration extends AbstractConfiguration implements Reconfigurable {

    /**
     * Property that enables XInclude processing in XML configuration files. Disabled by default.
     */
    private static final String ENABLE_XINCLUDE_PROP = "log4j2.configurationEnableXInclude";

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
            final Document document = newDocumentBuilder(xIncludeAware).parse(source);
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
            if (strict && schemaResource != null) {
                validateDocument(document, "classpath:" + schemaResource);
            }
        } catch (final SAXException | LSException | ParserConfigurationException | IOException e) {
            LOGGER.error("Error parsing " + configSource.getLocation(), e);
        }

        if (getName() == null) {
            setName(configSource.getLocation());
        }
    }

    /**
     * Creates a new DocumentBuilder suitable for parsing a configuration file.
     *
     * @param xIncludeAware enabled XInclude
     * @return a new DocumentBuilder
     * @throws ParserConfigurationException if a DocumentBuilder cannot be created, which satisfies the configuration requested.
     */
    static DocumentBuilder newDocumentBuilder(final boolean xIncludeAware) throws ParserConfigurationException {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        disableDtdProcessing(factory);

        if (xIncludeAware) {
            factory.setXIncludeAware(true);
        }
        final DocumentBuilder builder = factory.newDocumentBuilder();
        if (xIncludeAware) {
            // Resolve the resources referenced by `xi:include` through `ConfigurationSource`, so they honor the Log4j
            // URI conventions (e.g. the `classpath:` scheme) and the `ALLOWED_PROTOCOLS` restrictions.
            builder.setEntityResolver(ConfigurationSourceResolver.INSTANCE);
        }
        return builder;
    }

    private static void disableDtdProcessing(final DocumentBuilderFactory factory) {
        factory.setValidating(false);
        factory.setExpandEntityReferences(false);
        setFeature(factory, "http://xml.org/sax/features/external-general-entities", false);
        setFeature(factory, "http://xml.org/sax/features/external-parameter-entities", false);
        setFeature(factory, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    }

    private static void setFeature(
            final DocumentBuilderFactory factory, final String featureName, final boolean value) {
        try {
            factory.setFeature(featureName, value);
        } catch (final ParserConfigurationException e) {
            LOGGER.warn(
                    "The DocumentBuilderFactory [{}] does not support the feature [{}]: {}", factory, featureName, e);
        } catch (final AbstractMethodError err) {
            LOGGER.warn(
                    "The DocumentBuilderFactory [{}] is out of date and does not support setFeature: {}", factory, err);
        }
    }

    private static void validateDocument(final Document document, final String schemaLocation)
            throws ConfigurationException {
        try {
            // Resolve the schema, and the resources it imports, through the same resolver as the configuration file.
            final ConfigurationSource schemaSource = ConfigurationSource.fromUri(NetUtils.toURI(schemaLocation));
            if (schemaSource != null) {
                // Parse the schema with XInclude disabled:
                // a schema has its own modularity features (`xsd:include`/`xsd:import`).
                final Document schemaDocument =
                        newDocumentBuilder(false).parse(ConfigurationSourceResolver.toInputSource(schemaSource));
                final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                factory.setResourceResolver(ConfigurationSourceResolver.INSTANCE);
                // The system id is the base URI against which the schema's `xsd:include`/`xsd:import` resources
                // are resolved by the resource resolver above.
                final Schema schema = factory.newSchema(new DOMSource(schemaDocument, schemaSource.getLocation()));
                final Validator validator = schema.newValidator();
                validator.setResourceResolver(ConfigurationSourceResolver.INSTANCE);
                validator.validate(new DOMSource(document));
            } else {
                throw new ConfigurationException("Failed to load XML schema from " + schemaLocation);
            }
        } catch (LSException | SAXException | ParserConfigurationException | IOException e) {
            throw new ConfigurationException(
                    "Error validating " + document.getBaseURI() + " using schema " + schemaLocation, e);
        }
    }

    @Override
    public void setup() {
        if (rootElement == null) {
            LOGGER.error("No logging configuration");
            return;
        }
        constructHierarchy(rootNode, rootElement, pluginManager, strict);
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

    // Package-private for testing
    static void constructHierarchy(
            final Node node, final Element element, final PluginManager pluginManager, final boolean strict) {
        processAttributes(node, element);
        final StringBuilder buffer = new StringBuilder();
        final NodeList list = element.getChildNodes();
        final List<Node> children = node.getChildren();
        for (int i = 0; i < list.getLength(); i++) {
            final org.w3c.dom.Node w3cNode = list.item(i);
            if (w3cNode instanceof Element) {
                final Element child = (Element) w3cNode;
                final String name = getType(child, strict);
                final PluginType<?> type = pluginManager.getPluginType(name);
                final Node childNode = new Node(node, name, type);
                constructHierarchy(childNode, child, pluginManager, strict);
                if (type == null) {
                    final String value = childNode.getValue();
                    if (!childNode.hasChildren() && value != null) {
                        node.getAttributes().put(name, value);
                    } else {
                        LOGGER.error("Error processing element {} ({}): CLASS_NOT_FOUND", name, element);
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

    private static String getType(final Element element, final boolean strict) {
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

    private static Map<String, String> processAttributes(final Node node, final Element element) {
        final NamedNodeMap attrs = element.getAttributes();
        final Map<String, String> attributes = node.getAttributes();

        for (int i = 0; i < attrs.getLength(); ++i) {
            final org.w3c.dom.Node w3cNode = attrs.item(i);
            if (w3cNode instanceof Attr) {
                final Attr attr = (Attr) w3cNode;
                // The XInclude `fixup-base-uris` and `fixup-language` features (both enabled by default) add
                // `xml:base` and `xml:lang` attributes to the top-level included elements.
                if (XMLConstants.XML_NS_URI.equals(attr.getNamespaceURI())) {
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
     * Resolves the resources referenced by an XML configuration through {@link ConfigurationSource}, the same way the
     * configuration file itself is resolved: the targets of {@code xi:include} (as an {@link EntityResolver}) and the
     * resources imported by an XML Schema (as an {@link LSResourceResolver}).
     *
     * <p>This adds support for the Log4j URI conventions (such as the {@code classpath:} scheme) and subjects every
     * referenced resource to the {@code ALLOWED_PROTOCOLS} restrictions.</p>
     */
    private static final class ConfigurationSourceResolver implements EntityResolver, LSResourceResolver {

        private static final ConfigurationSourceResolver INSTANCE = new ConfigurationSourceResolver();

        /**
         * Resolves an external entity, used while expanding {@code xi:include} elements.
         */
        @Override
        public InputSource resolveEntity(final String publicId, final String systemId) throws SAXException {
            final InputSource inputSource = toInputSource(toConfigurationSource(systemId, null));
            inputSource.setPublicId(publicId);
            return inputSource;
        }

        /**
         * Resolves a resource imported by an XML Schema ({@code xsd:import}/{@code xsd:include}).
         *
         * <p>Throws an {@link LSException} when the resource cannot be resolved, instead of returning {@code null}: a
         * {@code null} return would let the parser fall back to its own URL resolution, bypassing the
         * {@code ALLOWED_PROTOCOLS} restrictions.</p>
         */
        @Override
        public LSInput resolveResource(
                final String type,
                final String namespaceURI,
                final String publicId,
                final String systemId,
                final String baseURI) {
            try {
                return new ConfigurationSourceLSInput(toConfigurationSource(systemId, baseURI));
            } catch (final SAXException e) {
                throw new LSException(LSException.PARSE_ERR, e.getMessage());
            }
        }

        private static ConfigurationSource toConfigurationSource(final String systemId, final String baseURI)
                throws SAXException {
            if (systemId == null) {
                throw new SAXException("System id missing.");
            }
            try {
                final URI uri = baseURI != null ? new URI(baseURI).resolve(systemId) : new URI(systemId);
                final ConfigurationSource configurationSource = ConfigurationSource.fromUri(uri);
                if (configurationSource == null) {
                    throw new SAXException("Unable to resolve system id " + systemId);
                }
                return configurationSource;
            } catch (final URISyntaxException e) {
                throw new SAXException("System id is not a valid URI: " + systemId, e);
            }
        }

        static InputSource toInputSource(final ConfigurationSource configurationSource) {
            final InputSource inputSource = new InputSource(configurationSource.getInputStream());
            inputSource.setSystemId(configurationSource.getLocation());
            return inputSource;
        }
    }

    /**
     * Minimal {@link LSInput} backed by a {@link ConfigurationSource}.
     */
    private static final class ConfigurationSourceLSInput implements LSInput {
        private final ConfigurationSource configurationSource;

        ConfigurationSourceLSInput(final ConfigurationSource configurationSource) {
            this.configurationSource = configurationSource;
        }

        @Override
        public InputStream getByteStream() {
            return configurationSource.getInputStream();
        }

        @Override
        public String getSystemId() {
            return configurationSource.getLocation();
        }

        @Override
        public String getPublicId() {
            return null;
        }

        @Override
        public String getBaseURI() {
            return null;
        }

        @Override
        public Reader getCharacterStream() {
            return null;
        }

        @Override
        public String getStringData() {
            return null;
        }

        @Override
        public String getEncoding() {
            return null;
        }

        @Override
        public boolean getCertifiedText() {
            return false;
        }

        @Override
        public void setByteStream(final InputStream byteStream) {}

        @Override
        public void setCharacterStream(final Reader characterStream) {}

        @Override
        public void setStringData(final String stringData) {}

        @Override
        public void setSystemId(final String systemId) {}

        @Override
        public void setPublicId(final String publicId) {}

        @Override
        public void setBaseURI(final String baseURI) {}

        @Override
        public void setEncoding(final String encoding) {}

        @Override
        public void setCertifiedText(final boolean certifiedText) {}
    }
}
