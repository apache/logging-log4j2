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
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Container for building Configurations. This class is not normally directly manipulated by users
 * of the Assembler API.
 * @since 2.4
 */
@ProviderType
public class Component {

    private final Map<String, String> attributes = new LinkedHashMap<>();
    private final List<Component> components = new ArrayList<>();
    private final String pluginType;
    private final @Nullable String value;

    /**
     * Default constructor.
     *
     * @deprecated - use {@link Component(String)} - a non-{@code null} '{@code pluginType}' must be specified
     */
    @Deprecated
    public Component() {
        this.pluginType = "";
        this.value = null;
    }

    public Component(final String pluginType) {
        this(pluginType, null, null);
    }

    public Component(final String pluginType, final @Nullable String name) {
        this(pluginType, name, null);
    }

    public Component(final String pluginType, @Nullable final String name, @Nullable final String value) {
        this.pluginType = pluginType;
        this.value = value;
        if (name != null && !name.isEmpty()) {
            attributes.put("name", name);
        }
    }

    /**
     * Puts the given key/value pair to the attribute map.
     * <p>
     *   If the new value is {@code null}, than any existing entry with the given {@code key} is ejected from the map.
     * </p>
     * @param key the key
     * @param newValue the new value
     * @return the previous value or {@code null} if none was set
     */
    public @Nullable String addAttribute(final String key, final @Nullable String newValue) {
        return putAttribute(key, newValue);
    }

    public void addComponent(final Component component) {
        Objects.requireNonNull(component, "The 'component' argument cannot be null.");
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

    public @Nullable String getValue() {
        return value;
    }

    /**
     * Puts the given key/value pair to the attribute map.
     * <p>
     *   If the new value is {@code null}, than any existing entry with the given {@code key} is ejected from the map.
     * </p>
     * @param key the key
     * @param newValue the new value
     * @return the previous value or {@code null} if none was set
     */
    protected @Nullable String putAttribute(final String key, final @Nullable String newValue) {

        Objects.requireNonNull(key, "The 'key' argument cannot be null.");

        if (newValue == null) {
            return attributes.remove(key);
        } else {
            return attributes.put(key, newValue);
        }
    }
}
