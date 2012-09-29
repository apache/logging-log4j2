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

    private List<Status> status = new ArrayList<Status>();

    private Element rootElement = null;

    private boolean strict = false;

    private String schema = null;

    private Validator validator;

    private List<String> messages = new ArrayList<String>();

    private File configFile;

    public XMLConfiguration(InputSource source, File configFile) {
        this.configFile = configFile;
        byte[] buffer = null;

        try {
            InputStream configStream = source.getByteStream();
            buffer = toByteArray(configStream);
            configStream.close();
            source = new InputSource(new ByteArrayInputStream(buffer));
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.parse(source);
            rootElement = document.getDocumentElement();
            Map<String, String> attrs = processAttributes(rootNode, rootElement);
            Level status = Level.OFF;
            boolean verbose = false;
            PrintStream stream = System.out;

            for (Map.Entry<String, String> entry : attrs.entrySet()) {
                if ("status".equalsIgnoreCase(entry.getKey())) {
                    status = Level.toLevel(getSubst().replace(entry.getValue()).toUpperCase(), null);
                    if (status == null) {
                        status = Level.ERROR;
                        messages.add("Invalid status specified: " + entry.getValue() + ". Defaulting to ERROR");
                    }
                } else if ("dest".equalsIgnoreCase(entry.getKey())) {
                    String dest = entry.getValue();
                    if (dest != null) {
                        if (dest.equalsIgnoreCase("err")) {
                            stream = System.err;
                        } else {
                            try {
                                File destFile = FileUtils.fileFromURI(new URI(dest));
                                stream = new PrintStream(new FileOutputStream(destFile));
                            } catch (URISyntaxException use) {
                                System.err.println("Unable to write to " + dest + ". Writing to stdout");
                            }
                        }
                    }
                } else if ("verbose".equalsIgnoreCase(entry.getKey())) {
                    verbose = Boolean.parseBoolean(getSubst().replace(entry.getValue()));
                } else if ("packages".equalsIgnoreCase(getSubst().replace(entry.getKey()))) {
                    String[] packages = entry.getValue().split(",");
                    for (String p : packages) {
                        PluginManager.addPackage(p);
                    }
                } else if ("name".equalsIgnoreCase(entry.getKey())) {
                    setName(getSubst().replace(entry.getValue()));
                } else if ("strict".equalsIgnoreCase(entry.getKey())) {
                    strict = Boolean.parseBoolean(getSubst().replace(entry.getValue()));
                } else if ("schema".equalsIgnoreCase(entry.getKey())) {
                    schema = getSubst().replace(entry.getValue());
                } else if ("monitorInterval".equalsIgnoreCase(entry.getKey())) {
                    int interval = Integer.parseInt(getSubst().replace(entry.getValue()));
                    if (interval > 0 && configFile != null) {
                        monitor = new FileConfigurationMonitor(this, configFile, listeners, interval);
                    }
                }
            }
            Iterator<StatusListener> iter = ((StatusLogger) LOGGER).getListeners();
            boolean found = false;
            while (iter.hasNext()) {
                StatusListener listener = iter.next();
                if (listener instanceof StatusConsoleListener) {
                    found = true;
                    ((StatusConsoleListener) listener).setLevel(status);
                    if (!verbose) {
                        ((StatusConsoleListener) listener).setFilters(VERBOSE_CLASSES);
                    }
                }
            }
            if (!found && status != Level.OFF) {
                StatusConsoleListener listener = new StatusConsoleListener(status, stream);
                if (!verbose) {
                    listener.setFilters(VERBOSE_CLASSES);
                }
                ((StatusLogger) LOGGER).registerListener(listener);
                for (String msg : messages) {
                    LOGGER.error(msg);
                }
            }

        } catch (SAXException domEx) {
            LOGGER.error("Error parsing " + source.getSystemId(), domEx);
        } catch (IOException ioe) {
            LOGGER.error("Error parsing " + source.getSystemId(), ioe);
        } catch (ParserConfigurationException pex) {
            LOGGER.error("Error parsing " + source.getSystemId(), pex);
        }
        if (strict && schema != null && buffer != null) {
            InputStream is = null;
            try {
                is = getClass().getClassLoader().getResourceAsStream(schema);
            } catch (Exception ex) {
                LOGGER.error("Unable to access schema " + schema);
            }
            if (is != null) {
                Source src = new StreamSource(is, LOG4J_XSD);
                SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                Schema schema = null;
                try {
                    schema = factory.newSchema(src);
                } catch (SAXException ex) {
                    LOGGER.error("Error parsing Log4j schema", ex);
                }
                if (schema != null) {
                    validator = schema.newValidator();
                    try {
                        validator.validate(new StreamSource(new ByteArrayInputStream(buffer)));
                    } catch (IOException ioe) {
                        LOGGER.error("Error reading configuration for validation", ioe);
                    } catch (SAXException ex) {
                        LOGGER.error("Error validating configuration", ex);
                    }
                }
            }
        }

        if (getName() == null) {
            setName(source.getSystemId());
        }
    }

    @Override
    public void setup() {
        constructHierarchy(rootNode, rootElement);
        if (status.size() > 0) {
            for (Status s : status) {
                LOGGER.error("Error processing element " + s.name + ": " + s.errorType);
            }
            return;
        }
        rootElement = null;
    }

    public Configuration reconfigure() {
        if (configFile != null) {
            try {
                InputSource source = new InputSource(new FileInputStream(configFile));
                source.setSystemId(configFile.getAbsolutePath());
                return new XMLConfiguration(source, configFile);
            } catch (FileNotFoundException ex) {
                LOGGER.error("Cannot locate file " + configFile, ex);
            }
        }
        return null;
    }

    private void constructHierarchy(Node node, Element element) {
        processAttributes(node, element);
        StringBuffer buffer = new StringBuffer();
        NodeList list = element.getChildNodes();
        List<Node> children = node.getChildren();
        for (int i = 0; i < list.getLength(); i++) {
            org.w3c.dom.Node w3cNode = list.item(i);
            if (w3cNode instanceof Element) {
                Element child = (Element) w3cNode;
                String name = getType(child);
                PluginType type = getPluginManager().getPluginType(name);
                Node childNode = new Node(node, name, type);
                constructHierarchy(childNode, child);
                if (type == null) {
                    String value = childNode.getValue();
                    if (!childNode.hasChildren() && value != null) {
                        node.getAttributes().put(name, value);
                    } else {
                        status.add(new Status(name, element, ErrorType.CLASS_NOT_FOUND));
                    }
                } else {
                    children.add(childNode);
                }
            } else if (w3cNode instanceof Text) {
                Text data = (Text) w3cNode;
                buffer.append(data.getData());
            }
        }

        String text = buffer.toString().trim();
        if (text.length() > 0 || (!node.hasChildren() && !node.isRoot())) {
            node.setValue(text);
        }
    }

    private String getType(Element element) {
        if (strict) {
            NamedNodeMap attrs = element.getAttributes();
            for (int i = 0; i < attrs.getLength(); ++i) {
                org.w3c.dom.Node w3cNode = attrs.item(i);
                if (w3cNode instanceof Attr) {
                    Attr attr = (Attr) w3cNode;
                    if (attr.getName().equalsIgnoreCase("type")) {
                        String type = attr.getValue();
                        attrs.removeNamedItem(attr.getName());
                        return type;
                    }
                }
            }
        }
        return element.getTagName();
    }

    private byte[] toByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[BUF_SIZE];

        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        return buffer.toByteArray();
    }

    private Map<String, String> processAttributes(Node node, Element element) {
        NamedNodeMap attrs = element.getAttributes();
        Map<String, String> attributes = node.getAttributes();

        for (int i = 0; i < attrs.getLength(); ++i) {
            org.w3c.dom.Node w3cNode = attrs.item(i);
            if (w3cNode instanceof Attr) {
                Attr attr = (Attr) w3cNode;
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
        private Element element;
        private String name;
        private ErrorType errorType;

        public Status(String name, Element element, ErrorType errorType) {
            this.name = name;
            this.element = element;
            this.errorType = errorType;
        }
    }

}
