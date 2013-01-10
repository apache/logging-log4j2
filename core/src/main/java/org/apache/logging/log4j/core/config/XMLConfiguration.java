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

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Creates a Node hierarchy from an XML file.
 */
public class XMLConfiguration extends BaseConfiguration implements Reconfigurable {

    private static final String[] VERBOSE_CLASSES = new String[] {ResolverUtil.class.getName()};

    private static final String LOG4J_XSD = "Log4J-V2.0.xsd";

    private static final int BUF_SIZE = 16384;

    private final List<Status> status = new ArrayList<Status>();

    private Element rootElement;

    private boolean strict;

    private String schema;

    private Validator validator;

    private final List<String> messages = new ArrayList<String>();

    private final File configFile;

    public XMLConfiguration(final ConfigurationFactory.ConfigurationSource configSource) {
        this.configFile = configSource.getFile();
        byte[] buffer = null;

        try {
            final InputStream configStream = configSource.getInputStream();
            buffer = toByteArray(configStream);
            configStream.close();
            final InputSource source = new InputSource(new ByteArrayInputStream(buffer));
            final DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            final Document document = builder.parse(source);
            rootElement = document.getDocumentElement();
            final Map<String, String> attrs = processAttributes(rootNode, rootElement);
            Level status = Level.OFF;
            boolean verbose = false;
            PrintStream stream = System.out;

            for (final Map.Entry<String, String> entry : attrs.entrySet()) {
                if ("status".equalsIgnoreCase(entry.getKey())) {
                    status = Level.toLevel(getSubst().replace(entry.getValue()), null);
                    if (status == null) {
                        status = Level.ERROR;
                        messages.add("Invalid status specified: " + entry.getValue() + ". Defaulting to ERROR");
                    }
                } else if ("dest".equalsIgnoreCase(entry.getKey())) {
                    final String dest = entry.getValue();
                    if (dest != null) {
                        if (dest.equalsIgnoreCase("err")) {
                            stream = System.err;
                        } else {
                            try {
                                final File destFile = FileUtils.fileFromURI(new URI(dest));
                                stream = new PrintStream(new FileOutputStream(destFile));
                            } catch (final URISyntaxException use) {
                                System.err.println("Unable to write to " + dest + ". Writing to stdout");
                            }
                        }
                    }
                } else if ("verbose".equalsIgnoreCase(entry.getKey())) {
                    verbose = Boolean.parseBoolean(getSubst().replace(entry.getValue()));
                } else if ("packages".equalsIgnoreCase(getSubst().replace(entry.getKey()))) {
                    final String[] packages = entry.getValue().split(",");
                    for (final String p : packages) {
                        PluginManager.addPackage(p);
                    }
                } else if ("name".equalsIgnoreCase(entry.getKey())) {
                    setName(getSubst().replace(entry.getValue()));
                } else if ("strict".equalsIgnoreCase(entry.getKey())) {
                    strict = Boolean.parseBoolean(getSubst().replace(entry.getValue()));
                } else if ("schema".equalsIgnoreCase(entry.getKey())) {
                    schema = getSubst().replace(entry.getValue());
                } else if ("monitorInterval".equalsIgnoreCase(entry.getKey())) {
                    final int interval = Integer.parseInt(getSubst().replace(entry.getValue()));
                    if (interval > 0 && configFile != null) {
                        monitor = new FileConfigurationMonitor(this, configFile, listeners, interval);
                    }
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
                    validator = schema.newValidator();
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
        constructHierarchy(rootNode, rootElement);
        if (status.size() > 0) {
            for (final Status s : status) {
                LOGGER.error("Error processing element " + s.name + ": " + s.errorType);
            }
            return;
        }
        rootElement = null;
    }

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
        final StringBuffer buffer = new StringBuffer();
        final NodeList list = element.getChildNodes();
        final List<Node> children = node.getChildren();
        for (int i = 0; i < list.getLength(); i++) {
            final org.w3c.dom.Node w3cNode = list.item(i);
            if (w3cNode instanceof Element) {
                final Element child = (Element) w3cNode;
                final String name = getType(child);
                final PluginType type = getPluginManager().getPluginType(name);
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
