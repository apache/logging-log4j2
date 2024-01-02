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
package org.apache.logging.log4j.core.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.di.ConfigurableInstanceFactory;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.plugins.model.PluginType;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.StringBuilders;
import org.apache.logging.log4j.util.Strings;

/**
 * Processes a parsed tree of configuration {@linkplain Node nodes} into configurable plugin objects described
 * by the configuration tree.
 *
 * @see org.apache.logging.log4j.plugins.Configurable
 */
public class ConfigurationProcessor {
    private static final Logger LOGGER = StatusLogger.getLogger();

    private final ConfigurableInstanceFactory instanceFactory;
    private final ThreadLocal<Node> currentNode = new ThreadLocal<>();

    public ConfigurationProcessor(final ConfigurableInstanceFactory instanceFactory) {
        instanceFactory.registerBinding(Node.CURRENT_NODE, currentNode::get);
        this.instanceFactory = instanceFactory;
    }

    /**
     * Processes a node in a configuration tree into a plugin instance. Nodes are processed in a depth-first
     * fashion unless a plugin enables the {@linkplain Configurable#deferChildren() defer children} option in
     * which case child nodes will be not be instantiated.
     *
     * @param node root node of a configuration tree to create
     * @return the configured plugin described by the node
     * @param <T> the type of object this plugin creates
     */
    public <T> T processNodeTree(final Node node) {
        final PluginType<?> type = node.getType();
        if (type != null && type.isDeferChildren()) {
            LOGGER.debug("Deferring configuration of child nodes of {}", node.getName());
            node.setObject(withNode(node, this::instantiate));
        } else {
            LOGGER.debug("Configuring child nodes of {}", node.getName());
            node.getChildren().forEach(child -> child.setObject(withNode(child, this::processNodeTree)));
            if (type == null) {
                if (node.getParent() == null) {
                    LOGGER.error("Unable to locate plugin for node {}", node.getName());
                }
            } else {
                LOGGER.debug("Configured child nodes of {}", node.getName());
                node.setObject(withNode(node, this::instantiate));
            }
        }
        verifyAttributesConsumed(node);
        verifyChildrenConsumed(node);
        return node.getObject();
    }

    private <T> T withNode(final Node node, final Function<Node, T> function) {
        currentNode.set(node);
        try {
            return function.apply(node);
        } finally {
            currentNode.remove();
        }
    }

    private Object instantiate(final Node node) {
        final PluginType<?> type = node.getType();
        final Class<?> pluginClass = type.getPluginClass();
        final List<Node> children = node.getChildren();
        // support for plugin classes that implement Map; unused in Log4j, but possibly used by custom plugins
        if (Map.class.isAssignableFrom(pluginClass)) {
            LOGGER.debug("Instantiating Map of child nodes of {}", node.getName());
            return children.stream().collect(Collectors.toMap(Node::getName, Node::getObject));
        }
        // support for plugin classes that implement Collection; unused in Log4j, but possibly used by custom plugins
        if (Collection.class.isAssignableFrom(pluginClass)) {
            LOGGER.debug("Instantiating List of child nodes of {}", node.getName());
            return children.stream().map(Node::getObject).collect(Collectors.toList());
        }
        try {
            final Key<?> key = Key.builder(pluginClass)
                    .setNamespace(type.getNamespace())
                    .setName(type.getName())
                    .get();
            return instanceFactory.getInstance(key);
        } catch (final Throwable e) {
            LOGGER.error("Could not configure plugin element {}: {}", node.getName(), e.toString(), e);
            return null;
        }
    }

    private static void verifyAttributesConsumed(final Node node) {
        final Map<String, String> attrs = node.getAttributes();
        if (!attrs.isEmpty()) {
            final StringBuilder sb = new StringBuilder();
            for (final String key : attrs.keySet()) {
                if (sb.length() == 0) {
                    sb.append(node.getName());
                    sb.append(" contains ");
                    if (attrs.size() == 1) {
                        sb.append("an invalid element or attribute ");
                    } else {
                        sb.append("invalid attributes ");
                    }
                } else {
                    sb.append(", ");
                }
                StringBuilders.appendDqValue(sb, key);
            }
            LOGGER.error(sb.toString());
        }
    }

    private static void verifyChildrenConsumed(final Node node) {
        final PluginType<?> type = node.getType();
        if (type != null && !type.isDeferChildren() && node.hasChildren()) {
            for (final Node child : node.getChildren()) {
                final String nodeType = node.getType().getElementType();
                final String name = node.getName();
                final String context =
                        nodeType.equalsIgnoreCase(name) || Strings.isBlank(nodeType) ? name : nodeType + ' ' + name;
                LOGGER.error("{} has no field or parameter that matches element {}", context, child.getName());
            }
        }
    }
}
