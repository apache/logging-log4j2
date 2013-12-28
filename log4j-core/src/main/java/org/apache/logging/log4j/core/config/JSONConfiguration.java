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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.plugins.PluginManager;
import org.apache.logging.log4j.core.config.plugins.PluginType;
import org.apache.logging.log4j.core.config.plugins.ResolverUtil;
import org.apache.logging.log4j.core.helpers.FileUtils;
import org.apache.logging.log4j.status.StatusConsoleListener;
import org.apache.logging.log4j.status.StatusListener;
import org.apache.logging.log4j.status.StatusLogger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Creates a Node hierarchy from a JSON file.
 */
public class JSONConfiguration extends BaseConfiguration implements Reconfigurable {

    private static final String[] VERBOSE_CLASSES = new String[] {ResolverUtil.class.getName()};

    private static final int BUF_SIZE = 16384;

    private final List<Status> status = new ArrayList<Status>();

    private JsonNode root;

    private final List<String> messages = new ArrayList<String>();

    private final File configFile;

    public JSONConfiguration(final ConfigurationFactory.ConfigurationSource configSource) {
        this.configFile = configSource.getFile();
        byte[] buffer;

        try {
            final InputStream configStream = configSource.getInputStream();
            buffer = toByteArray(configStream);
            configStream.close();
            final InputStream is = new ByteArrayInputStream(buffer);
            final ObjectMapper mapper = new ObjectMapper().configure(JsonParser.Feature.ALLOW_COMMENTS, true);
            root = mapper.readTree(is);
            if (root.size() == 1) {
                final Iterator<JsonNode> i = root.elements();
                root = i.next();
            }
            processAttributes(rootNode, root);
            Level status = getDefaultStatus();
            boolean verbose = false;
            PrintStream stream = System.out;
            for (final Map.Entry<String, String> entry : rootNode.getAttributes().entrySet()) {
                if ("status".equalsIgnoreCase(entry.getKey())) {
                    status = Level.toLevel(getStrSubstitutor().replace(entry.getValue()), null);
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
                } else if ("packages".equalsIgnoreCase(entry.getKey())) {
                    final String[] packages = getStrSubstitutor().replace(entry.getValue()).split(",");
                    for (final String p : packages) {
                        PluginManager.addPackage(p);
                    }
                } else if ("name".equalsIgnoreCase(entry.getKey())) {
                    setName(getStrSubstitutor().replace(entry.getValue()));
                } else if ("monitorInterval".equalsIgnoreCase(entry.getKey())) {
                    final int interval = Integer.parseInt(getStrSubstitutor().replace(entry.getValue()));
                    if (interval > 0 && configFile != null) {
                        monitor = new FileConfigurationMonitor(this, configFile, listeners, interval);
                    }
                } else if ("advertiser".equalsIgnoreCase(entry.getKey())) {
                    createAdvertiser(getStrSubstitutor().replace(entry.getValue()), configSource, buffer,
                        "application/json");
                }
            }

            final Iterator<StatusListener> statusIter = ((StatusLogger) LOGGER).getListeners();
            boolean found = false;
            while (statusIter.hasNext()) {
                final StatusListener listener = statusIter.next();
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
            if (getName() == null) {
                setName(configSource.getLocation());
            }
        } catch (final Exception ex) {
            LOGGER.error("Error parsing " + configSource.getLocation(), ex);
            ex.printStackTrace();
        }
    }

    @Override
    public void stop() {
        super.stop();
    }

    @Override
    public void setup() {
        final Iterator<Map.Entry<String, JsonNode>> iter = root.fields();
        final List<Node> children = rootNode.getChildren();
        while (iter.hasNext()) {
            final Map.Entry<String, JsonNode> entry = iter.next();
            final JsonNode n = entry.getValue();
            if (n.isObject()) {
                LOGGER.debug("Processing node for object " + entry.getKey());
                children.add(constructNode(entry.getKey(), rootNode, n));
            } else if (n.isArray()) {
                LOGGER.error("Arrays are not supported at the root configuration.");
            }
        }
        LOGGER.debug("Completed parsing configuration");
        if (status.size() > 0) {
            for (final Status s : status) {
                LOGGER.error("Error processing element " + s.name + ": " + s.errorType);
            }
            return;
        }
    }

    @Override
    public Configuration reconfigure() {
        if (configFile != null) {
            try {
                final ConfigurationFactory.ConfigurationSource source =
                    new ConfigurationFactory.ConfigurationSource(new FileInputStream(configFile), configFile);
                return new JSONConfiguration(source);
            } catch (final FileNotFoundException ex) {
                LOGGER.error("Cannot locate file " + configFile, ex);
            }
        }
        return null;
    }

    private Node constructNode(final String name, final Node parent, final JsonNode jsonNode) {
        final PluginType<?> type = pluginManager.getPluginType(name);
        final Node node = new Node(parent, name, type);
        processAttributes(node, jsonNode);
        final Iterator<Map.Entry<String, JsonNode>> iter = jsonNode.fields();
        final List<Node> children = node.getChildren();
        while (iter.hasNext()) {
            final Map.Entry<String, JsonNode> entry = iter.next();
            final JsonNode n = entry.getValue();
            if (n.isArray() || n.isObject()) {
                if (type == null) {
                    status.add(new Status(name, n, ErrorType.CLASS_NOT_FOUND));
                }
                if (n.isArray()) {
                    LOGGER.debug("Processing node for array " + entry.getKey());
                    for (int i = 0; i < n.size(); ++i) {
                        final String pluginType = getType(n.get(i), entry.getKey());
                        final PluginType<?> entryType = pluginManager.getPluginType(pluginType);
                        final Node item = new Node(node, entry.getKey(), entryType);
                        processAttributes(item, n.get(i));
                        if (pluginType.equals(entry.getKey())) {
                            LOGGER.debug("Processing " + entry.getKey() + "[" + i + "]");
                        } else {
                            LOGGER.debug("Processing " + pluginType + " " + entry.getKey() + "[" + i + "]");
                        }
                        final Iterator<Map.Entry<String, JsonNode>> itemIter = n.get(i).fields();
                        final List<Node> itemChildren = item.getChildren();
                        while (itemIter.hasNext()) {
                            final Map.Entry<String, JsonNode> itemEntry = itemIter.next();
                            if (itemEntry.getValue().isObject()) {
                                LOGGER.debug("Processing node for object " + itemEntry.getKey());
                                itemChildren.add(constructNode(itemEntry.getKey(), item, itemEntry.getValue()));
                            } else if (itemEntry.getValue().isArray()) {
                                JsonNode array = itemEntry.getValue();
                                String entryName = itemEntry.getKey();
                                LOGGER.debug("Processing array for object " + entryName);
                                final PluginType<?> itemEntryType = pluginManager.getPluginType(entryName);
                                for (int j = 0; j < array.size(); ++j) {
                                    itemChildren.add(constructNode(entryName, item, array.get(j)));
                                }
                            }

                        }
                        children.add(item);
                    }
                } else {
                    LOGGER.debug("Processing node for object " + entry.getKey());
                    children.add(constructNode(entry.getKey(), node, n));
                }
            } else {
                LOGGER.debug("Node {} is of type {}", entry.getKey(), n.getNodeType());
            }
        }

        String t;
        if (type == null) {
            t = "null";
        } else {
            t = type.getElementName() + ":" + type.getPluginClass();
        }

        final String p = node.getParent() == null ? "null" : node.getParent().getName() == null ?
            "root" : node.getParent().getName();
        LOGGER.debug("Returning " + node.getName() + " with parent " + p + " of type " +  t);
        return node;
    }

    private String getType(final JsonNode node, final String name) {
        final Iterator<Map.Entry<String, JsonNode>> iter = node.fields();
        while (iter.hasNext()) {
            final Map.Entry<String, JsonNode> entry = iter.next();
            if (entry.getKey().equalsIgnoreCase("type")) {
                final JsonNode n = entry.getValue();
                if (n.isValueNode()) {
                    return n.asText();
                }
            }
        }
        return name;
    }

    private void processAttributes(final Node parent, final JsonNode node) {
        final Map<String, String> attrs = parent.getAttributes();
        final Iterator<Map.Entry<String, JsonNode>> iter = node.fields();
        while (iter.hasNext()) {
            final Map.Entry<String, JsonNode> entry = iter.next();
            if (!entry.getKey().equalsIgnoreCase("type")) {
                final JsonNode n = entry.getValue();
                if (n.isValueNode()) {
                    attrs.put(entry.getKey(), n.asText());
                }
            }
        }
    }

    protected byte[] toByteArray(final InputStream is) throws IOException {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        final byte[] data = new byte[BUF_SIZE];

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
        private final JsonNode node;
        private final String name;
        private final ErrorType errorType;

        public Status(final String name, final JsonNode node, final ErrorType errorType) {
            this.name = name;
            this.node = node;
            this.errorType = errorType;
        }
    }
}
