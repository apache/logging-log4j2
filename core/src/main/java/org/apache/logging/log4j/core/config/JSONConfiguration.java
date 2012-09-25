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
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.xml.sax.InputSource;

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
 * Creates a Node hierarchy from a JSON file.
 */
public class JSONConfiguration extends BaseConfiguration implements Reconfigurable {

    private static final String[] VERBOSE_CLASSES = new String[] {ResolverUtil.class.getName()};

    private static final int BUF_SIZE = 16384;

    private List<Status> status = new ArrayList<Status>();

    private JsonNode root;

    private List<String> messages = new ArrayList<String>();

    private File configFile;

    public JSONConfiguration(InputSource source, File configFile) {
        this.configFile = configFile;
        byte[] buffer;

        try {
            buffer = toByteArray(source.getByteStream());
            InputStream is = new ByteArrayInputStream(buffer);
            source = new InputSource(is);
            ObjectMapper mapper = new ObjectMapper().configure(JsonParser.Feature.ALLOW_COMMENTS, true);
            root = mapper.readTree(is);
            if (root.size() == 1) {
                Iterator<JsonNode> i = root.getElements();
                root = i.next();
            }
            processAttributes(rootNode, root);
            Level status = Level.OFF;
            boolean verbose = false;
            PrintStream stream = System.out;
            for (Map.Entry<String, String> entry : rootNode.getAttributes().entrySet()) {
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
                } else if ("packages".equalsIgnoreCase(entry.getKey())) {
                    String[] packages = getSubst().replace(entry.getValue()).split(",");
                    for (String p : packages) {
                        PluginManager.addPackage(p);
                    }
                } else if ("name".equalsIgnoreCase(entry.getKey())) {
                    setName(getSubst().replace(entry.getValue()));
                } else if ("monitorInterval".equalsIgnoreCase(entry.getKey())) {
                    int interval = Integer.parseInt(getSubst().replace(entry.getValue()));
                    if (interval > 0 && configFile != null) {
                        monitor = new FileConfigurationMonitor(this, configFile, listeners, interval);
                    }
                }
            }

            Iterator<StatusListener> statusIter = ((StatusLogger) LOGGER).getListeners();
            boolean found = false;
            while (statusIter.hasNext()) {
                StatusListener listener = statusIter.next();
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
            if (getName() == null) {
                setName(source.getSystemId());
            }
        } catch (Exception ex) {
            LOGGER.error("Error parsing " + source.getSystemId(), ex);
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
                LOGGER.debug("Processing node for object " + entry.getKey());
                children.add(constructNode(entry.getKey(), rootNode, n));
            } else if (n.isArray()) {
                LOGGER.error("Arrays are not supported at the root configuration.");
            }
        }
        LOGGER.debug("Completed parsing configuration");
        if (status.size() > 0) {
            for (Status s : status) {
                LOGGER.error("Error processing element " + s.name + ": " + s.errorType);
            }
            return;
        }
    }

    public Configuration reconfigure() {
        if (configFile != null) {
            try {
                InputSource source = new InputSource(new FileInputStream(configFile));
                source.setSystemId(configFile.getAbsolutePath());
                return new JSONConfiguration(source, configFile);
            } catch (FileNotFoundException ex) {
                LOGGER.error("Cannot locate file " + configFile, ex);
            }
        }
        return null;
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
                    LOGGER.debug("Processing node for array " + entry.getKey());
                    for (int i = 0; i < n.size(); ++i) {
                        String pluginType = getType(n.get(i), entry.getKey());
                        PluginType entryType = getPluginManager().getPluginType(pluginType);
                        Node item = new Node(node, entry.getKey(), entryType);
                        processAttributes(item, n.get(i));
                        if (pluginType.equals(entry.getKey())) {
                            LOGGER.debug("Processing " + entry.getKey() + "[" + i + "]");
                        } else {
                            LOGGER.debug("Processing " + pluginType + " " + entry.getKey() + "[" + i + "]");
                        }
                        Iterator<Map.Entry<String, JsonNode>> itemIter = n.get(i).getFields();
                        List<Node> itemChildren = item.getChildren();
                        while (itemIter.hasNext()) {
                            Map.Entry<String, JsonNode> itemEntry = itemIter.next();
                            if (itemEntry.getValue().isObject()) {
                                LOGGER.debug("Processing node for object " + itemEntry.getKey());
                                itemChildren.add(constructNode(itemEntry.getKey(), item, itemEntry.getValue()));
                            }
                        }
                        children.add(item);
                    }
                } else {
                    LOGGER.debug("Processing node for object " + entry.getKey());
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

        String p = node.getParent() == null ? "null" : node.getParent().getName() == null ?
            "root" : node.getParent().getName();
        LOGGER.debug("Returning " + node.getName() + " with parent " + p + " of type " +  t);
        return node;
    }

    private String getType(JsonNode node, String name) {
        Iterator<Map.Entry<String, JsonNode>> iter = node.getFields();
        while (iter.hasNext()) {
            Map.Entry<String, JsonNode> entry = iter.next();
            if (entry.getKey().equalsIgnoreCase("type")) {
                JsonNode n = entry.getValue();
                if (n.isValueNode()) {
                    return n.asText();
                }
            }
        }
        return name;
    }

    private void processAttributes(Node parent, JsonNode node) {
        Map<String, String> attrs = parent.getAttributes();
        Iterator<Map.Entry<String, JsonNode>> iter = node.getFields();
        while (iter.hasNext()) {
            Map.Entry<String, JsonNode> entry = iter.next();
            if (!entry.getKey().equalsIgnoreCase("type")) {
                JsonNode n = entry.getValue();
                if (n.isValueNode()) {
                    attrs.put(entry.getKey(), n.asText());
                }
            }
        }
    }

    protected byte[] toByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[BUF_SIZE];

        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        return buffer.toByteArray();
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
        private JsonNode node;
        private String name;
        private ErrorType errorType;

        public Status(String name, JsonNode node, ErrorType errorType) {
            this.name = name;
            this.node = node;
            this.errorType = errorType;
        }
    }
}
