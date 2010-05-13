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

import org.apache.logging.log4j.core.config.plugins.PluginType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */

public class Node {

    private Node parent;
    private String name;
    private String value;
    private PluginType type;
    private Map<String, String> attributes = new HashMap<String, String>();
    private List<Node> children = new ArrayList<Node>();
    private Object object;
    private Configuration config;


    /**
     * Creates a new instance of <code>Node</code> and initializes it
     * with a name and the corresponding XML element.
     *
     * @param parent the node's parent.
     * @param name the node's name.
     * @param type The Plugin Type associated with the node.
     */
    public Node(Node parent, String name, PluginType type) {
        this.parent = parent;
        this.name = name;
        this.type = type;
        this.config = parent.config;
    }

    public Node(Configuration config) {
        this.config = config;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public List<Node> getChildren() {
        return children;
    }

    public boolean hasChildren() {
        return children.size() > 0;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
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

    public void setObject(Object obj) {
        object = obj;
    }

    public Object getObject() {
        return object;
    }
}
