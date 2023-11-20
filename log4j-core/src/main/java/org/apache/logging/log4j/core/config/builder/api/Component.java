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
package org.apache.logging.log4j.core.config.builder.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Container for building Configurations. This class is not normally directly manipulated by users
 * of the Assembler API.
 * @since 2.4
 */
public class Component {

    private final Map<String, String> attributes = new LinkedHashMap<>();
    private final List<Component> components = new ArrayList<>();
    private final String pluginType;
    private final String value;

    public Component(final String pluginType) {
        this(pluginType, null, null);
    }

    public Component(final String pluginType, final String name) {
        this(pluginType, name, null);
    }

    public Component(final String pluginType, final String name, final String value) {
        this.pluginType = pluginType;
        this.value = value;
        if (name != null && name.length() > 0) {
            attributes.put("name", name);
        }
    }

    public Component() {
        this.pluginType = null;
        this.value = null;
    }

    public String addAttribute(final String key, final String newValue) {
        return attributes.put(key, newValue);
    }

    public void addComponent(final Component component) {
        components.add(component);
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public List<Component> getComponents() {
        return components;
    }

    public String getPluginType() {
        return pluginType;
    }

    public String getValue() {
        return value;
    }
}
