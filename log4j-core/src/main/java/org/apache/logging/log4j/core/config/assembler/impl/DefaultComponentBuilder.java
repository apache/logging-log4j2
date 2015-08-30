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
package org.apache.logging.log4j.core.config.assembler.impl;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.assembler.api.Component;
import org.apache.logging.log4j.core.config.assembler.api.ComponentBuilder;
import org.apache.logging.log4j.core.config.assembler.api.ConfigurationBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic component that captures attributes and Components in preparation for assembling the Appender's
 * Component.
 */
@SuppressWarnings("rawtypes")
public class DefaultComponentBuilder<T extends ComponentBuilder> implements ComponentBuilder<T> {

    private ConfigurationBuilder<? extends Configuration> assembler;
    private String type;
    private Map<String, String> attributes = new HashMap<>();
    private List<Component> components = new ArrayList<>();
    private String name;
    private String value;

    public DefaultComponentBuilder(ConfigurationBuilder<? extends Configuration> assembler, String type) {
        this(assembler, null, type, null);
    }

    public DefaultComponentBuilder(ConfigurationBuilder<? extends Configuration> assembler, String name, String type) {
        this(assembler, name, type, null);
    }

    public DefaultComponentBuilder(ConfigurationBuilder<? extends Configuration> assembler, String name, String type,
            String value) {
        this.type = type;
        this.assembler = assembler;
        this.name = name;
        this.value = value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T addAttribute(String key, Level level) {
        attributes.put(key, level.toString());
        return (T) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T addAttribute(String key, String value) {
        attributes.put(key, value);
        return (T) this;
    }


    @Override
    @SuppressWarnings("unchecked")
    public T addAttribute(String key, Enum<?> value) {
        attributes.put(key, value.name());
        return (T) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T addAttribute(String key, int value) {
        attributes.put(key, Integer.toString(value));
        return (T) this;
    }


    @Override
    @SuppressWarnings("unchecked")
    public T addAttribute(String key, boolean value) {
        attributes.put(key, Boolean.toString(value));
        return (T) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T addAttribute(String key, Object value) {
        attributes.put(key, value.toString());
        return (T) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T addComponent(ComponentBuilder<?> assembler) {
        components.add(assembler.build());
        return (T) this;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ConfigurationBuilder<? extends Configuration> getBuilder() {
        return assembler;
    }

    @Override
    public Component build() {
        Component component = new Component(type, name, value);
        component.getAttributes().putAll(attributes);
        component.getComponents().addAll(components);
        return component;
    }
}
