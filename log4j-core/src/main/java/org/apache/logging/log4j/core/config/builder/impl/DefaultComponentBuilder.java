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
package org.apache.logging.log4j.core.config.builder.impl;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.builder.api.Component;
import org.apache.logging.log4j.core.config.builder.api.ComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic component that captures attributes and Components in preparation for assembling the Appender's
 * Component.
 */
class DefaultComponentBuilder<T extends ComponentBuilder<T>> implements ComponentBuilder<T> {

    private ConfigurationBuilder<? extends Configuration> builder;
    private String type;
    private Map<String, String> attributes = new HashMap<>();
    private List<Component> components = new ArrayList<>();
    private String name;
    private String value;

    public DefaultComponentBuilder(ConfigurationBuilder<? extends Configuration> builder, String type) {
        this(builder, null, type, null);
    }

    public DefaultComponentBuilder(ConfigurationBuilder<? extends Configuration> builder, String name, String type) {
        this(builder, name, type, null);
    }

    public DefaultComponentBuilder(ConfigurationBuilder<? extends Configuration> builder, String name, String type,
            String value) {
        this.type = type;
        this.builder = builder;
        this.name = name;
        this.value = value;
    }

    @Override
    public T addAttribute(String key, boolean value) {
        return put(key, Boolean.toString(value));
    }
    
    @Override
    public T addAttribute(String key, Enum<?> value) {
        return put(key, value.name());
    }

    @Override
    public T addAttribute(String key, int value) {
        return put(key, Integer.toString(value));
    }


    @Override
    public T addAttribute(String key, Level level) {
        return put(key, level.toString());
    }

    @Override
    public T addAttribute(String key, Object value) {
        return put(key, value.toString());
    }


    @Override
    public T addAttribute(String key, String value) {
        return put(key, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T addComponent(ComponentBuilder<?> builder) {
        components.add(builder.build());
        return (T) this;
    }

    @Override
    public Component build() {
        Component component = new Component(type, name, value);
        component.getAttributes().putAll(attributes);
        component.getComponents().addAll(components);
        return component;
    }

    @Override
    public ConfigurationBuilder<? extends Configuration> getBuilder() {
        return builder;
    }

    @Override
    public String getName() {
        return name;
    }

    protected T put(String key, String value) {
        attributes.put(key, value);
        return (T) this;
    }
}
