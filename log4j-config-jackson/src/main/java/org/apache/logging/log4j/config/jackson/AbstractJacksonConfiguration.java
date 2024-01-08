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
package org.apache.logging.log4j.config.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Reconfigurable;
import org.apache.logging.log4j.core.config.status.StatusConfiguration;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.model.PluginType;

/**
 * Base class for all Jackson-based configurations.
 */
public abstract class AbstractJacksonConfiguration extends AbstractConfiguration implements Reconfigurable {

    private final List<Status> status = new ArrayList<>();
    private JsonNode root;

    public AbstractJacksonConfiguration(
            final LoggerContext loggerContext, final ConfigurationSource configurationSource) {
        super(loggerContext, configurationSource);
        final byte[] buffer;
        try {
            try (final InputStream configStream = configurationSource.getInputStream()) {
                buffer = configStream.readAllBytes();
            }
            final InputStream is = new ByteArrayInputStream(buffer);
            root = getObjectMapper().readTree(is);
            if (root.size() == 1) {
                for (final JsonNode node : root) {
                    root = node;
                }
            }
            processAttributes(rootNode, root);
            final StatusConfiguration statusConfig = new StatusConfiguration().setStatus(getDefaultStatus());
            int monitorIntervalSeconds = 0;
            for (final Map.Entry<String, String> entry :
                    rootNode.getAttributes().entrySet()) {
                final String key = entry.getKey();
                final String value = getConfigurationStrSubstitutor().replace(entry.getValue());
                // TODO: this duplicates a lot of the XmlConfiguration constructor
                if ("status".equalsIgnoreCase(key)) {
                    statusConfig.setStatus(value);
                } else if ("dest".equalsIgnoreCase(key)) {
                    statusConfig.setDestination(value);
                } else if ("shutdownHook".equalsIgnoreCase(key)) {
                    isShutdownHookEnabled = !"disable".equalsIgnoreCase(value);
                } else if ("shutdownTimeout".equalsIgnoreCase(key)) {
                    shutdownTimeoutMillis = Long.parseLong(value);
                } else if ("verbose".equalsIgnoreCase(entry.getKey())) {
                    statusConfig.setVerbosity(value);
                } else if ("packages".equalsIgnoreCase(key)) {
                    LOGGER.warn("The packages attribute is no longer supported");
                } else if ("name".equalsIgnoreCase(key)) {
                    setName(value);
                } else if ("monitorInterval".equalsIgnoreCase(key)) {
                    monitorIntervalSeconds = Integer.parseInt(value);
                } else if ("advertiser".equalsIgnoreCase(key)) {
                    createAdvertiser(value, configurationSource, buffer, "application/json");
                }
            }
            initializeWatchers(this, configurationSource, monitorIntervalSeconds);
            statusConfig.initialize();
            if (getName() == null) {
                setName(configurationSource.getLocation());
            }
        } catch (final Exception ex) {
            LOGGER.error("Error parsing " + configurationSource.getLocation(), ex);
        }
    }

    /**
     * Creates the object mapper to parse the configuration in a tree.
     */
    protected abstract ObjectMapper getObjectMapper();

    /**
     * Recreates the configuration by rereading the configuration file.
     * <p>
     *     Used in dependent classes.
     * </p>
     */
    protected abstract Configuration createConfiguration(
            LoggerContext loggerContext, ConfigurationSource configurationSource);

    @Override
    public void setup() {
        final Iterator<Entry<String, JsonNode>> iter = root.fields();
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
            return createConfiguration(getLoggerContext(), source);
        } catch (final IOException ex) {
            LOGGER.error("Cannot locate file {}", getConfigurationSource(), ex);
        }
        return null;
    }

    private Node constructNode(final String name, final Node parent, final JsonNode jsonNode) {
        final PluginType<?> type = corePlugins.get(getType(jsonNode, name));
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
                        LOGGER.debug("Processing node for array entry {}[{}]", entry.getKey(), i);
                        children.add(constructNode(entry.getKey(), node, n.get(i)));
                    }
                } else {
                    LOGGER.debug("Processing node for object {}", entry.getKey());
                    children.add(constructNode(entry.getKey(), node, n));
                }
            } else {
                LOGGER.debug("Node {} is of type {}", entry.getKey(), n.getNodeType());
            }
        }

        final String t;
        if (type == null) {
            t = "null";
        } else {
            t = type.getElementType() + ':' + type.getPluginClass();
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
            if ("type".equalsIgnoreCase(entry.getKey())) {
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
            if (!"type".equalsIgnoreCase(entry.getKey())) {
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

    /**
     * A node factory that sorts children by name.
     */
    public static final class SortingNodeFactory extends JsonNodeFactory {

        public static final JsonNodeFactory INSTANCE = new SortingNodeFactory();

        private SortingNodeFactory() {}

        @Override
        public ObjectNode objectNode() {
            return new ObjectNode(this, new TreeMap<>());
        }
    }
}
