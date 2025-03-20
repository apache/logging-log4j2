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
package org.apache.logging.log4j.core.config.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.Reconfigurable;
import org.apache.logging.log4j.core.config.plugins.util.PluginType;
import org.apache.logging.log4j.core.config.status.StatusConfiguration;
import org.apache.logging.log4j.core.util.Integers;
import org.apache.logging.log4j.core.util.Patterns;

/**
 * Creates a Node hierarchy from a JSON file.
 */
public class JsonConfiguration extends AbstractConfiguration implements Reconfigurable {

    private final List<Status> status = new ArrayList<>();
    private JsonNode root;

    public JsonConfiguration(final LoggerContext loggerContext, final ConfigurationSource configSource) {
        super(loggerContext, configSource);
        final File configFile = configSource.getFile();
        byte[] buffer;
        try {
            try (final InputStream configStream = configSource.getInputStream()) {
                buffer = toByteArray(configStream);
            }
            final InputStream is = new ByteArrayInputStream(buffer);
            root = getObjectMapper().readTree(is);
            if (root.size() == 1) {
                for (final JsonNode node : root) {
                    root = node;
                }
            }
            processAttributes(rootNode, root);
            final StatusConfiguration statusConfig = new StatusConfiguration().withStatus(getDefaultStatus());
            int monitorIntervalSeconds = 0;
            for (final Map.Entry<String, String> entry :
                    rootNode.getAttributes().entrySet()) {
                final String key = entry.getKey();
                final String value = getConfigurationStrSubstitutor().replace(entry.getValue());
                // TODO: this duplicates a lot of the XmlConfiguration constructor
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
                } else if ("monitorInterval".equalsIgnoreCase(key)) {
                    monitorIntervalSeconds = Integers.parseInt(value);
                } else if ("advertiser".equalsIgnoreCase(key)) {
                    createAdvertiser(value, configSource, buffer, "application/json");
                }
            }
            initializeWatchers(this, configSource, monitorIntervalSeconds);
            statusConfig.initialize();
            if (getName() == null) {
                setName(configSource.getLocation());
            }
        } catch (final Exception ex) {
            LOGGER.error("Error parsing " + configSource.getLocation(), ex);
        }
    }

    protected ObjectMapper getObjectMapper() {
        return new ObjectMapper().configure(JsonParser.Feature.ALLOW_COMMENTS, true);
    }

    @Override
    public void setup() {
        final Iterator<Map.Entry<String, JsonNode>> iter = root.fields();
        final List<Node> children = rootNode.getChildren();
        while (iter.hasNext()) {
            final Map.Entry<String, JsonNode> entry = iter.next();
            final JsonNode n = entry.getValue();
            if (n.isObject()) {
                LOGGER.debug("Processing node for object {}", entry.getKey());
                children.add(constructNode(entry.getKey(), rootNode, n));
            } else if (n.isArray()) {
                LOGGER.error("Arrays are not supported at the root configuration.");
            }
        }
        LOGGER.debug("Completed parsing configuration");
        if (status.size() > 0) {
            for (final Status s : status) {
                LOGGER.error("Error processing element {}: {}", s.name, s.errorType);
            }
        }
    }

    @Override
    public Configuration reconfigure() {
        try {
            final ConfigurationSource source = getConfigurationSource().resetInputStream();
            if (source == null) {
                return null;
            }
            return new JsonConfiguration(getLoggerContext(), source);
        } catch (final IOException ex) {
            LOGGER.error("Cannot locate file {}", getConfigurationSource(), ex);
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
                    LOGGER.debug("Processing node for array {}", entry.getKey());
                    for (int i = 0; i < n.size(); ++i) {
                        final String pluginType = getType(n.get(i), entry.getKey());
                        final PluginType<?> entryType = pluginManager.getPluginType(pluginType);
                        final Node item = new Node(node, entry.getKey(), entryType);
                        processAttributes(item, n.get(i));
                        if (pluginType.equals(entry.getKey())) {
                            LOGGER.debug("Processing {}[{}]", entry.getKey(), i);
                        } else {
                            LOGGER.debug("Processing {} {}[{}]", pluginType, entry.getKey(), i);
                        }
                        final Iterator<Map.Entry<String, JsonNode>> itemIter =
                                n.get(i).fields();
                        final List<Node> itemChildren = item.getChildren();
                        while (itemIter.hasNext()) {
                            final Map.Entry<String, JsonNode> itemEntry = itemIter.next();
                            if (itemEntry.getValue().isObject()) {
                                LOGGER.debug("Processing node for object {}", itemEntry.getKey());
                                itemChildren.add(constructNode(itemEntry.getKey(), item, itemEntry.getValue()));
                            } else if (itemEntry.getValue().isArray()) {
                                final JsonNode array = itemEntry.getValue();
                                final String entryName = itemEntry.getKey();
                                LOGGER.debug("Processing array for object {}", entryName);
                                for (int j = 0; j < array.size(); ++j) {
                                    itemChildren.add(constructNode(entryName, item, array.get(j)));
                                }
                            }
                        }
                        children.add(item);
                    }
                } else {
                    LOGGER.debug("Processing node for object {}", entry.getKey());
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
            t = type.getElementName() + ':' + type.getPluginClass();
        }

        final String p = node.getParent() == null
                ? "null"
                : node.getParent().getName() == null
                        ? LoggerConfig.ROOT
                        : node.getParent().getName();
        LOGGER.debug("Returning {} with parent {} of type {}", node.getName(), p, t);
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

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[location=" + getConfigurationSource() + "]";
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
        private final JsonNode node;
        private final String name;
        private final ErrorType errorType;

        public Status(final String name, final JsonNode node, final ErrorType errorType) {
            this.name = name;
            this.node = node;
            this.errorType = errorType;
        }

        @Override
        public String toString() {
            return "Status [name=" + name + ", errorType=" + errorType + ", node=" + node + "]";
        }
    }
}
