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
import org.apache.logging.log4j.internal.StatusConsoleListener;
import org.apache.logging.log4j.internal.StatusLogger;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Creates a Node hierarchy from an XML file.
 */
public class XMLConfiguration extends BaseConfiguration {

    private List<Status> status = new ArrayList<Status>();

    private Element rootElement = null;

    private static final String[] verboseClasses = new String[] { ResolverUtil.class.getName() };

    public XMLConfiguration(InputSource source) {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.parse(source);
            rootElement = document.getDocumentElement();
            Map<String, String> attrs = processAttributes(rootNode, rootElement);
            Level status = Level.OFF;
            boolean verbose = false;

            for (Map.Entry<String, String> entry : attrs.entrySet()) {
                if ("status".equalsIgnoreCase(entry.getKey())) {
                    status = Level.toLevel(entry.getValue().toUpperCase(), Level.OFF);
                } else if ("verbose".equalsIgnoreCase(entry.getKey())) {
                    verbose = Boolean.parseBoolean(entry.getValue());
                } else if ("packages".equalsIgnoreCase(entry.getKey())) {
                    String[] packages = entry.getValue().split(",");
                    for (String p : packages) {
                        PluginManager.addPackage(p);
                    }
                } else if ("name".equalsIgnoreCase(entry.getKey())) {
                    setName(entry.getValue());
                }
            }
            if (status != Level.OFF) {
                StatusConsoleListener listener = new StatusConsoleListener(status);
                if (!verbose) {
                    listener.setFilters(verboseClasses);
                }
                ((StatusLogger) logger).registerListener(listener);
            }

        } catch (SAXException domEx) {
            logger.error("Error parsing " + source.getSystemId(), domEx);
        } catch (IOException ioe) {
            logger.error("Error parsing " + source.getSystemId(), ioe);
        } catch (ParserConfigurationException pex) {
            logger.error("Error parsing " + source.getSystemId(), pex);
        }
        if (getName() == null) {
            setName(source.getSystemId());
        }
    }

    public void setup() {
        constructHierarchy(rootNode, rootElement);
        if (status.size() > 0) {
            for (Status s : status) {
                logger.error("Error processing element " + s.name + ": " + s.errorType);
            }
            return;
        }
        rootElement = null;
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
                String name = child.getTagName();
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

    private enum ErrorType {
        CLASS_NOT_FOUND
    }

    private class Status {
        Element element;
        String name;
        ErrorType errorType;

        public Status(String name, Element element, ErrorType errorType) {
            this.name = name;
            this.element = element;
            this.errorType = errorType;
        }
    }



}
