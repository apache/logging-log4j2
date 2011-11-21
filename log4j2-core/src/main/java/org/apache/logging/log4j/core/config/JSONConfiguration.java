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
import org.apache.logging.log4j.status.StatusConsoleListener;
import org.apache.logging.log4j.status.StatusListener;
import org.apache.logging.log4j.status.StatusLogger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Creates a Node hierarchy from a JSON file.
 */
public class JSONConfiguration extends BaseConfiguration {
    private List<Status> status = new ArrayList<Status>();

    private static final String[] verboseClasses = new String[] { ResolverUtil.class.getName() };

    private JsonNode root;

    public JSONConfiguration(InputSource source, File configFile) {
        byte[] buffer;

        try {
            buffer = toByteArray(source.getByteStream());
            InputStream is = new ByteArrayInputStream(buffer);
            source = new InputSource(is);
            root = new ObjectMapper().readTree(is);
            if (root.size() == 1) {
                Iterator<JsonNode> i = root.getElements();
                root = i.next();
            }
            processAttributes(rootNode, root);
            Level status = Level.OFF;
            boolean verbose = false;
            for (Map.Entry<String, String> entry : rootNode.getAttributes().entrySet()) {
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
                } else if ("monitorInterval".equalsIgnoreCase(entry.getKey())) {
                    int interval = Integer.parseInt(entry.getValue());
                    if (interval > 0 && configFile != null) {
                        monitor = new FileConfigurationMonitor(configFile, listeners, interval);
                    }
                }
            }

            Iterator<StatusListener> statusIter = ((StatusLogger) logger).getListeners();
            boolean found = false;
            while (statusIter.hasNext()) {
                StatusListener listener = statusIter.next();
                if (listener instanceof StatusConsoleListener) {
                    found = true;
                    ((StatusConsoleListener) listener).setLevel(status);
                    if (!verbose) {
                        ((StatusConsoleListener) listener).setFilters(verboseClasses);
                    }
                }
            }
            if (!found && status != Level.OFF) {
                StatusConsoleListener listener = new StatusConsoleListener(status);
                if (!verbose) {
                    listener.setFilters(verboseClasses);
                }
                ((StatusLogger) logger).registerListener(listener);
            }
            if (getName() == null) {
                setName(source.getSystemId());
            }
        } catch (Exception ex) {
            logger.error("Error parsing " + source.getSystemId(), ex);
            ex.printStackTrace();
        }
    }

     public void setup() {
        Iterator<Map.Entry<String, JsonNode>> iter = root.getFields();
        List<Node> children = rootNode.getChildren();
        while (iter.hasNext()) {
            Map.Entry<String, JsonNode> entry = iter.next();
            JsonNode n = entry.getValue();
            if (n.isObject()) {
                logger.debug("Processing node for object " + entry.getKey());
                children.add(constructNode(entry.getKey(), rootNode, n));
            } else if (n.isArray()) {
                logger.error("Arrays are not supported at the root configuration.");
            }
        }
        logger.debug("Completed parsing configuration");
        if (status.size() > 0) {
            for (Status s : status) {
                logger.error("Error processing element " + s.name + ": " + s.errorType);
            }
            return;
        }
    }

    private Node constructNode(String name, Node parent, JsonNode jsonNode) {
        PluginType type = getPluginManager().getPluginType(name);
        Node node = new Node(parent, name, type);
        processAttributes(node, jsonNode);
        Iterator<Map.Entry<String, JsonNode>> iter = jsonNode.getFields();
        List<Node> children = node.getChildren();
        while (iter.hasNext()) {
            Map.Entry<String, JsonNode> entry = iter.next();
            JsonNode n = entry.getValue();
            if (n.isArray() || n.isObject()) {
                if (type == null) {
                    status.add(new Status(name, n, ErrorType.CLASS_NOT_FOUND));
                }
                if (n.isArray()) {
                    logger.debug("Processing node for array " + entry.getKey());
                    for (int i=0; i < n.size(); ++i) {
                        PluginType entryType = getPluginManager().getPluginType(entry.getKey());
                        Node item = new Node(node, entry.getKey(), entryType);
                        processAttributes(item, n.get(i));
                        logger.debug("Processing " + entry.getKey() + "[" + i + "]");
                        Iterator<Map.Entry<String, JsonNode>> itemIter = n.get(i).getFields();
                        List<Node> itemChildren = item.getChildren();
                        while (itemIter.hasNext()) {
                            Map.Entry<String, JsonNode> itemEntry = itemIter.next();
                            if (itemEntry.getValue().isObject()) {
                                logger.debug("Processing node for object " + itemEntry.getKey());
                                itemChildren.add(constructNode(itemEntry.getKey(), item, itemEntry.getValue()));
                            }
                        }
                        children.add(item);
                    }
                } else {
                    logger.debug("Processing node for object " + entry.getKey());
                    children.add(constructNode(entry.getKey(), node, n));
                }


            }
        }

        String t;
        if (type == null) {
            t = "null";
        } else {
            t = type.getElementName() + ":" + type.getPluginClass();
        }

        String p = node.getParent() == null ? "null" : node.getParent().getName() == null ? "root" : node.getParent().getName();
        logger.debug("Returning " + node.getName() + " with parent " + p + " of type " +  t);
        return node;
    }

    private void processAttributes(Node parent, JsonNode node) {
        Map<String, String> attrs = parent.getAttributes();
        Iterator<Map.Entry<String, JsonNode>> iter = node.getFields();
        while (iter.hasNext()) {
            Map.Entry<String, JsonNode> entry = iter.next();
            JsonNode n = entry.getValue();
            if (n.isValueNode()) {
                attrs.put(entry.getKey(), n.asText());
            }
        }
    }

    protected byte[] toByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];

        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        return buffer.toByteArray();
    }

     private enum ErrorType {
        CLASS_NOT_FOUND
    }

     private class Status {
        JsonNode node;
        String name;
        ErrorType errorType;

        public Status(String name, JsonNode node, ErrorType errorType) {
            this.name = name;
            this.node = node;
            this.errorType = errorType;
        }
    }
}
