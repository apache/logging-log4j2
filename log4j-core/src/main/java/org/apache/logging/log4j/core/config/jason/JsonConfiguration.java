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
package org.apache.logging.log4j.core.config.jason;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Reconfigurable;
import org.apache.logging.log4j.core.config.status.StatusConfiguration;
import org.apache.logging.log4j.core.util.JsonReader;
import org.apache.logging.log4j.core.util.Patterns;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.model.PluginType;
import org.apache.logging.log4j.plugins.util.ResolverUtil;
import org.apache.logging.log4j.util.Cast;

public class JsonConfiguration extends AbstractConfiguration implements Reconfigurable {
    private static final String[] VERBOSE_CLASSES = new String[] { ResolverUtil.class.getName() };

    private final List<Status> statuses = new ArrayList<>();
    private Map<String, Object> root;

    public JsonConfiguration(final LoggerContext loggerContext, final ConfigurationSource configurationSource) {
        super(loggerContext, configurationSource);
        try {
            final byte[] bytes;
            try (final var configStream = configurationSource.getInputStream()) {
                bytes = configStream.readAllBytes();
                root = Cast.cast(JsonReader.read(new String(bytes, StandardCharsets.UTF_8)));
            }
            if (root.size() == 1) {
                for (final Object value : root.values()) {
                    root = Cast.cast(value);
                }
            }
            processAttributes(rootNode, root);
            final StatusConfiguration statusConfig = new StatusConfiguration().setVerboseClasses(VERBOSE_CLASSES)
                    .setStatus(getDefaultStatus());
            final AtomicInteger monitorIntervalSeconds = new AtomicInteger();

            rootNode.getAttributes().forEach((key, value) -> {
                if ("status".equalsIgnoreCase(key)) {
                    statusConfig.setStatus(value);
                } else if ("dest".equalsIgnoreCase(key)) {
                    statusConfig.setDestination(value);
                } else if ("shutdownHook".equalsIgnoreCase(key)) {
                    isShutdownHookEnabled = !"disable".equalsIgnoreCase(value);
                } else if ("shutdownTimeout".equalsIgnoreCase(key)) {
                    shutdownTimeoutMillis = Long.parseLong(value);
                } else if ("verbose".equalsIgnoreCase(key)) {
                    statusConfig.setVerbosity(value);
                } else if ("packages".equalsIgnoreCase(key)) {
                    pluginPackages.addAll(Arrays.asList(value.split(Patterns.COMMA_SEPARATOR)));
                } else if ("name".equalsIgnoreCase(key)) {
                    setName(value);
                } else if ("monitorInterval".equalsIgnoreCase(key)) {
                    monitorIntervalSeconds.setOpaque(Integer.parseInt(value));
                } else if ("advertiser".equalsIgnoreCase(key)) {
                    createAdvertiser(value, configurationSource, bytes, "application/json");
                }
            });
            initializeWatchers(this, configurationSource, monitorIntervalSeconds.getOpaque());
            statusConfig.initialize();
            if (getName() == null) {
                setName(configurationSource.getLocation());
            }
        } catch (final Exception e) {
            LOGGER.error("Error parsing {}", configurationSource.getLocation(), e);
        }
    }

    @Override
    public void setup() {
        final List<Node> children = rootNode.getChildren();
        root.forEach((key, value) -> {
            if (value instanceof Map) {
                LOGGER.debug("Processing node for object {}", key);
                children.add(constructNode(key, rootNode, Cast.cast(value)));
            }
        });
        LOGGER.debug("Completed parsing configuration");
        if (statuses.size() > 0) {
            for (final var s : statuses) {
                LOGGER.error("Error processing element {}: {}", s.name, s.errorType);
            }
        }
    }

    private Node constructNode(final String key, final Node parent, final Map<String, Object> value) {
        final PluginType<?> pluginType = corePlugins.get(key);
        final Node node = new Node(parent, key, pluginType);
        processAttributes(node, value);
        final List<Node> children = node.getChildren();

        value.forEach((k, v) -> {
            if (isValueType(v)) {
                LOGGER.debug("Node {} is of type {}", k, v != null ? v.getClass() : null);
                return;
            }
            if (pluginType == null) {
                statuses.add(new Status(v, k, ErrorType.CLASS_NOT_FOUND));
                return;
            }
            if (v instanceof List<?>) {
                LOGGER.debug("Processing node for array {}", k);
                ((List<?>) v).forEach(object -> {
                    if (object instanceof Map<?, ?>) {
                        final Map<String, Object> map = Cast.cast(object);
                        final String type = getType(map).orElse(k);
                        final PluginType<?> entryType = corePlugins.get(type);
                        final Node child = new Node(node, k, entryType);
                        processAttributes(child, map);
                        if (type.equalsIgnoreCase(k)) {
                            LOGGER.debug("Processing {}[{}]", k, children.size());
                        } else {
                            LOGGER.debug("Processing {} {}[{}]", type, k, children.size());
                        }
                        final List<Node> grandchildren = child.getChildren();
                        map.forEach((itemKey, itemValue) -> {
                            if (itemValue instanceof Map<?, ?>) {
                                LOGGER.debug("Processing node for object {}", itemKey);
                                grandchildren.add(constructNode(itemKey, child, Cast.cast(itemValue)));
                            } else if (itemValue instanceof List<?>) {
                                final List<?> list = (List<?>) itemValue;
                                LOGGER.debug("Processing array for object {}", itemKey);
                                list.forEach(subValue -> grandchildren.add(
                                        constructNode(itemKey, child, Cast.cast(subValue))));
                            }
                        });
                        children.add(child);
                    }
                });
            } else {
                LOGGER.debug("Processing node for object {}", k);
                children.add(constructNode(k, node, Cast.cast(v)));
            }
        });

        final String t;
        if (pluginType == null) {
            t = "null";
        } else {
            t = pluginType.getElementType() + ':' + pluginType.getPluginClass();
        }

        final String p = node.getParent() == null ? "null"
                : node.getParent().getName() == null ? LoggerConfig.ROOT : node.getParent().getName();
        LOGGER.debug("Returning {} with parent {} of type {}", node.getName(), p, t);
        return node;
    }

    @Override
    public Configuration reconfigure() {
        try {
            final ConfigurationSource configurationSource = getConfigurationSource().resetInputStream();
            if (configurationSource == null) {
                return null;
            }
            return new JsonConfiguration(getLoggerContext(), configurationSource);
        } catch (final IOException e) {
            LOGGER.error("Cannot locate file {}", getConfigurationSource(), e);
        }
        return null;
    }

    private static boolean isValueType(final Object value) {
        return !(value instanceof Map<?, ?> || value instanceof List<?>);
    }

    private static void processAttributes(final Node parent, final Map<String, Object> node) {
        final Map<String, String> attributes = parent.getAttributes();
        node.forEach((key, value) -> {
            if (!key.equalsIgnoreCase("type") && isValueType(value)) {
                attributes.put(key, String.valueOf(value));
            }
        });
    }

    private static Optional<String> getType(final Map<String, Object> node) {
        for (final Map.Entry<String, Object> entry : node.entrySet()) {
            if (entry.getKey().equalsIgnoreCase("type")) {
                final Object value = entry.getValue();
                if (isValueType(value)) {
                    return Optional.of(String.valueOf(value));
                }
            }
        }
        return Optional.empty();
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
        private final Object node;
        private final String name;
        private final ErrorType errorType;

        private Status(final Object node, final String name, final ErrorType errorType) {
            this.node = node;
            this.name = name;
            this.errorType = errorType;
        }

        @Override
        public String toString() {
            return "Status{" +
                    "node=" + node +
                    ", name='" + name + '\'' +
                    ", errorType=" + errorType +
                    '}';
        }
    }
}
