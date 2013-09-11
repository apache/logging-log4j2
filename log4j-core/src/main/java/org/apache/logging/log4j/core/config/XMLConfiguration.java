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
package org.apache.logging.log4j.core.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.plugins.PluginManager;
import org.apache.logging.log4j.core.config.plugins.PluginType;
import org.apache.logging.log4j.core.config.plugins.ResolverUtil;
import org.apache.logging.log4j.core.helpers.FileUtils;
import org.apache.logging.log4j.status.StatusConsoleListener;
import org.apache.logging.log4j.status.StatusListener;
import org.apache.logging.log4j.status.StatusLogger;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Creates a Node hierarchy from an XML file.
 */
public class XMLConfiguration extends BaseConfiguration implements Reconfigurable {

    private static final String XINCLUDE_FIXUP_LANGUAGE = "http://apache.org/xml/features/xinclude/fixup-language";

    private static final String XINCLUDE_FIXUP_BASE_URIS = "http://apache.org/xml/features/xinclude/fixup-base-uris";

    private static final String[] VERBOSE_CLASSES = new String[] {ResolverUtil.class.getName()};

    private static final String LOG4J_XSD = "Log4j-config.xsd";

    private static final int BUF_SIZE = 16384;

    private final List<Status> status = new ArrayList<Status>();

    private Element rootElement;

    private boolean strict;

    private String schema;

    private final File configFile;

    /**
     * Creates a new DocumentBuilder suitable for parsing a configuration file.
     *
     * @return a new DocumentBuilder
     * @throws ParserConfigurationException
     */
    static DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        enableXInclude(factory);
        return factory.newDocumentBuilder();
    }

    /**
     * Enables XInclude for the given DocumentBuilderFactory
     *
     * @param factory a DocumentBuilderFactory
     */
    private static void enableXInclude(final DocumentBuilderFactory factory) {
        try {
            // Alternative: We set if a system property on the command line is set, for example:
            // -DLog4j.XInclude=true
            factory.setXIncludeAware(true);
        } catch (UnsupportedOperationException e) {
            LOGGER.warn("The DocumentBuilderFactory does not support XInclude: " + factory, e);
        } catch (AbstractMethodError err) {
            LOGGER.warn("The DocumentBuilderFactory is out of date and does not support XInclude: " + factory);
        }
        try {
            // Alternative: We could specify all features and values with system properties like:
            // -DLog4j.DocumentBuilderFactory.Feature="http://apache.org/xml/features/xinclude/fixup-base-uris true"
            factory.setFeature(XINCLUDE_FIXUP_BASE_URIS, true);
        } catch (ParserConfigurationException e) {
            LOGGER.warn("The DocumentBuilderFactory [" + factory + "] does not support the feature ["
                    + XINCLUDE_FIXUP_BASE_URIS + "]", e);
        } catch (AbstractMethodError err) {
            LOGGER.warn("The DocumentBuilderFactory is out of date and does not support setFeature: " + factory);
        }
        try {
            factory.setFeature(XINCLUDE_FIXUP_LANGUAGE, true);
        } catch (ParserConfigurationException e) {
            LOGGER.warn("The DocumentBuilderFactory [" + factory + "] does not support the feature ["
                    + XINCLUDE_FIXUP_LANGUAGE + "]", e);
        } catch (AbstractMethodError err) {
            LOGGER.warn("The DocumentBuilderFactory is out of date and does not support setFeature: " + factory);
        }
    }

    public XMLConfiguration(final ConfigurationFactory.ConfigurationSource configSource) {
        this.configFile = configSource.getFile();
        byte[] buffer = null;

        try {
            final List<String> messages = new ArrayList<String>();
            final InputStream configStream = configSource.getInputStream();
            buffer = toByteArray(configStream);
            configStream.close();
            final InputSource source = new InputSource(new ByteArrayInputStream(buffer));
            final Document document = newDocumentBuilder().parse(source);
            rootElement = document.getDocumentElement();
            final Map<String, String> attrs = processAttributes(rootNode, rootElement);
            Level status = getDefaultStatus();
            boolean verbose = false;
            PrintStream stream = System.out;

            for (final Map.Entry<String, String> entry : attrs.entrySet()) {
                if ("status".equalsIgnoreCase(entry.getKey())) {
                    final Level stat = Level.toLevel(getStrSubstitutor().replace(entry.getValue()), null);
                    if (stat != null) {
                        status = stat;
                    } else {
                        messages.add("Invalid status specified: " + entry.getValue() + ". Defaulting to " + status);
                    }
                } else if ("dest".equalsIgnoreCase(entry.getKey())) {
                    final String dest = getStrSubstitutor().replace(entry.getValue());
                    if (dest != null) {
                        if (dest.equalsIgnoreCase("err")) {
                            stream = System.err;
                        } else {
                            try {
                                final File destFile = FileUtils.fileFromURI(new URI(dest));
                                final String enc = Charset.defaultCharset().name();
                                stream = new PrintStream(new FileOutputStream(destFile), true, enc);
                            } catch (final URISyntaxException use) {
                                System.err.println("Unable to write to " + dest + ". Writing to stdout");
                            }
                        }
                    }
                } else if ("shutdownHook".equalsIgnoreCase(entry.getKey())) {
                    String hook = getStrSubstitutor().replace(entry.getValue());
                    isShutdownHookEnabled = !hook.equalsIgnoreCase("disable");
                } else if ("verbose".equalsIgnoreCase(entry.getKey())) {
                    verbose = Boolean.parseBoolean(getStrSubstitutor().replace(entry.getValue()));
                } else if ("packages".equalsIgnoreCase(getStrSubstitutor().replace(entry.getKey()))) {
                    final String[] packages = entry.getValue().split(",");
                    for (final String p : packages) {
                        PluginManager.addPackage(p);
                    }
                } else if ("name".equalsIgnoreCase(entry.getKey())) {
                    setName(getStrSubstitutor().replace(entry.getValue()));
                } else if ("strict".equalsIgnoreCase(entry.getKey())) {
                    strict = Boolean.parseBoolean(getStrSubstitutor().replace(entry.getValue()));
                } else if ("schema".equalsIgnoreCase(entry.getKey())) {
                    schema = getStrSubstitutor().replace(entry.getValue());
                } else if ("monitorInterval".equalsIgnoreCase(entry.getKey())) {
                    final int interval = Integer.parseInt(getStrSubstitutor().replace(entry.getValue()));
                    if (interval > 0 && configFile != null) {
                        monitor = new FileConfigurationMonitor(this, configFile, listeners, interval);
                    }
                } else if ("advertiser".equalsIgnoreCase(entry.getKey())) {
                    createAdvertiser(getStrSubstitutor().replace(entry.getValue()), configSource, buffer, "text/xml");
                }
            }
            final Iterator<StatusListener> iter = ((StatusLogger) LOGGER).getListeners();
            boolean found = false;
            while (iter.hasNext()) {
                final StatusListener listener = iter.next();
                if (listener instanceof StatusConsoleListener) {
                    found = true;
                    ((StatusConsoleListener) listener).setLevel(status);
                    if (!verbose) {
                        ((StatusConsoleListener) listener).setFilters(VERBOSE_CLASSES);
                    }
                }
            }
            if (!found && status != Level.OFF) {
                final StatusConsoleListener listener = new StatusConsoleListener(status, stream);
                if (!verbose) {
                    listener.setFilters(VERBOSE_CLASSES);
                }
                ((StatusLogger) LOGGER).registerListener(listener);
                for (final String msg : messages) {
                    LOGGER.error(msg);
                }
            }

        } catch (final SAXException domEx) {
            LOGGER.error("Error parsing " + configSource.getLocation(), domEx);
        } catch (final IOException ioe) {
            LOGGER.error("Error parsing " + configSource.getLocation(), ioe);
        } catch (final ParserConfigurationException pex) {
            LOGGER.error("Error parsing " + configSource.getLocation(), pex);
        }
        if (strict && schema != null && buffer != null) {
            InputStream is = null;
            try {
                is = getClass().getClassLoader().getResourceAsStream(schema);
            } catch (final Exception ex) {
                LOGGER.error("Unable to access schema " + schema);
            }
            if (is != null) {
                final Source src = new StreamSource(is, LOG4J_XSD);
                final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                Schema schema = null;
                try {
                    schema = factory.newSchema(src);
                } catch (final SAXException ex) {
                    LOGGER.error("Error parsing Log4j schema", ex);
                }
                if (schema != null) {
                    Validator validator = schema.newValidator();
                    try {
                        validator.validate(new StreamSource(new ByteArrayInputStream(buffer)));
                    } catch (final IOException ioe) {
                        LOGGER.error("Error reading configuration for validation", ioe);
                    } catch (final SAXException ex) {
                        LOGGER.error("Error validating configuration", ex);
                    }
                }
            }
        }

        if (getName() == null) {
            setName(configSource.getLocation());
        }
    }

    @Override
    public void setup() {
        if (rootElement == null) {
            LOGGER.error("No logging configuration");
            return;
        }
        constructHierarchy(rootNode, rootElement);
        if (status.size() > 0) {
            for (final Status s : status) {
                LOGGER.error("Error processing element " + s.name + ": " + s.errorType);
            }
            return;
        }
        rootElement = null;
    }

    @Override
    public Configuration reconfigure() {
        if (configFile != null) {
            try {
                final ConfigurationFactory.ConfigurationSource source =
                    new ConfigurationFactory.ConfigurationSource(new FileInputStream(configFile), configFile);
                return new XMLConfiguration(source);
            } catch (final FileNotFoundException ex) {
                LOGGER.error("Cannot locate file " + configFile, ex);
            }
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
        if (text.length() > 0 || (!node.hasChildren() && !node.isRoot())) {
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

    private byte[] toByteArray(final InputStream is) throws IOException {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        final byte[] data = new byte[BUF_SIZE];

        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        return buffer.toByteArray();
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

    /**
     * The error that occurred.
     */
    private enum ErrorType {
        CLASS_NOT_FOUND
    }

    /**
     * Status for recording errors.
     */
    private class Status {
        private final Element element;
        private final String name;
        private final ErrorType errorType;

        public Status(final String name, final Element element, final ErrorType errorType) {
            this.name = name;
            this.element = element;
            this.errorType = errorType;
        }
    }

}
