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
package org.apache.logging.log4j.plugins;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.plugins.model.PluginType;
import org.apache.logging.log4j.util.Cast;

/**
 * Configurations are represented as a tree of Node instances. Each Node may have
 * {@linkplain #getAttributes() attributes}, {@linkplain #getChildren() children nodes},
 * an {@linkplain #getValue() optional value} (which is a special kind of attribute for certain configuration file
 * formats which support the concept), and a {@linkplain #getName() name} which corresponds to a
 * {@link Plugin} class in the {@linkplain Configurable Core namespace} and is specified via an element name or
 * the {@code type} attribute. Configuration factories parse a configuration resource into a tree of Nodes with a
 * single root Node.
 */
public class Node {

    /**
     * Main plugin namespace for plugins which are represented as a configuration node. Such plugins tend to be
     * available as XML elements in a configuration file.
     *
     * @since 2.1
     * @see Configurable
     */
    public static final String CORE_NAMESPACE = "Core";

    /**
     * Key describing the current node being configured.
     */
    public static final Key<Node> CURRENT_NODE =
            Key.builder(Node.class).setQualifierType(PluginNode.class).get();

    private Node parent;
    private final String name;
    private String value;
    private final PluginType<?> type;
    private final Map<String, String> attributes = new HashMap<>();
    private final List<Node> children = new ArrayList<>();
    private Object object;

    /**
     * Creates a new instance of {@code Node} and initializes it
     * with a name and the corresponding XML element.
     *
     * @param parent the node's parent.
     * @param name the node's name.
     * @param type The Plugin Type associated with the node.
     */
    public Node(final Node parent, final String name, final PluginType<?> type) {
        this.parent = parent;
        this.name = name;
        this.type = type;
    }

    /**
     * Constructs a root node. Root nodes have no defined type, name, or parent node.
     */
    public Node() {
        this.parent = null;
        this.name = null;
        this.type = null;
    }

    /**
     * Constructs a fresh copy of the provided Node.
     *
     * @param node original node to copy
     */
    public Node(final Node node) {
        this.parent = node.parent;
        this.name = node.name;
        this.type = node.type;
        this.attributes.putAll(node.getAttributes());
        this.value = node.getValue();
        for (final Node child : node.getChildren()) {
            this.children.add(new Node(child));
        }
        this.object = node.object;
    }

    public void setParent(final Node parent) {
        this.parent = parent;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public List<Node> getChildren() {
        return children;
    }

    public void addChild(final Node child) {
        children.add(child);
        child.setParent(this);
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public Node getParent() {
        return parent;
    }

    public String getName() {
        return name;
    }

    public boolean isRoot() {
        return parent == null;
    }

    public void setObject(final Object obj) {
        object = obj;
    }

    public <T> T getObject() {
        return Cast.cast(object);
    }

    /**
     * Returns this node's object cast to the given class.
     *
     * @param clazz the class to cast this node's object to.
     * @param <T>   the type to cast to.
     * @return this node's object.
     * @since 2.1
     */
    public <T> T getObject(final Class<T> clazz) {
        return clazz.cast(object);
    }

    /**
     * Determines if this node's object is an instance of the given class.
     *
     * @param clazz the class to check.
     * @return {@code true} if this node's object is an instance of the given class.
     * @since 2.1
     */
    public boolean isInstanceOf(final Class<?> clazz) {
        return clazz.isInstance(object);
    }

    public PluginType<?> getType() {
        return type;
    }

    /**
     * Finds and removes the attribute with a name equaling ignoring case either the provided name or one of the provided
     * aliases.
     *
     * @param name    name of attribute to find
     * @param aliases aliases of attribute to find
     * @return the removed attribute value if found or empty if no attributes match
     */
    public Optional<String> removeMatchingAttribute(final String name, final Collection<String> aliases) {
        final var iterator = attributes.entrySet().iterator();
        while (iterator.hasNext()) {
            final var entry = iterator.next();
            final String key = entry.getKey();
            if (key.equalsIgnoreCase(name) || aliases.stream().anyMatch(key::equalsIgnoreCase)) {
                iterator.remove();
                return Optional.ofNullable(entry.getValue());
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        if (object == null) {
            return "null";
        }
        return type.isObjectPrintable()
                ? object.toString()
                : type.getPluginClass().getName() + " with name " + name;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Supplier<Node> {

        private final Map<String, String> attributes = new LinkedHashMap<>();
        private final List<Builder> children = new ArrayList<>();
        private String name;
        private String value;
        private PluginType<?> pluginType;

        public Builder setName(final String name) {
            this.name = name;
            return this;
        }

        public Builder setValue(final String value) {
            this.value = value;
            return this;
        }

        public Builder setPluginType(final PluginType<?> pluginType) {
            this.pluginType = pluginType;
            return this;
        }

        public Builder setAttribute(final String name, final String value) {
            attributes.put(name, value);
            return this;
        }

        public Builder setAttributes(final Map<String, String> attributes) {
            this.attributes.putAll(attributes);
            return this;
        }

        public Builder addChild(final Consumer<Builder> child) {
            final Builder builder = new Builder();
            child.accept(builder);
            this.children.add(builder);
            return this;
        }

        @Override
        public Node get() {
            final Node node = new Node(null, name, pluginType);
            node.setValue(value);
            node.attributes.putAll(attributes);
            for (final Builder child : children) {
                node.addChild(child.get());
            }
            return node;
        }
    }
}
