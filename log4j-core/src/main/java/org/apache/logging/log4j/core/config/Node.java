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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.core.config.plugins.util.PluginType;

/**
 * A Configuration node.
 */
public class Node {

    private final Node parent;
    private final String name;
    private String value;
    private final PluginType<?> type;
    private final Map<String, String> attributes = new HashMap<String, String>();
    private final List<Node> children = new ArrayList<Node>();
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

    public Node() {
        this.parent = null;
        this.name = null;
        this.type = null;
    }

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

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public List<Node> getChildren() {
        return children;
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

    public Object getObject() {
        return object;
    }

    public PluginType<?> getType() {
        return type;
    }

    @Override
    public String toString() {
        if (object == null) {
            return "null";
        }
        return type.isObjectPrintable() ? object.toString() :
            type.getPluginClass().getName() + " with name " + name;
    }
}
