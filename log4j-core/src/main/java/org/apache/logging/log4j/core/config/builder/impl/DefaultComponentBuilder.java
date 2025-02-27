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
package org.apache.logging.log4j.core.config.builder.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.builder.api.Component;
import org.apache.logging.log4j.core.config.builder.api.ComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;

/**
 * Generic component that captures attributes and Components in preparation for assembling the Appender's
 * Component.
 *
 * @since 2.4
 */
class DefaultComponentBuilder<T extends ComponentBuilder<T>, CB extends ConfigurationBuilder<? extends Configuration>>
        implements ComponentBuilder<T> {

    private final CB builder;
    private final String type;
    private final Map<String, String> attributes = new LinkedHashMap<>();
    private final List<Component> components = new ArrayList<>();
    private final String name;
    private final String value;

    public DefaultComponentBuilder(final CB builder, final String type) {
        this(builder, null, type, null);
    }

    public DefaultComponentBuilder(final CB builder, final String name, final String type) {
        this(builder, name, type, null);
    }

    public DefaultComponentBuilder(final CB builder, final String name, final String type, final String value) {
        this.type = type;
        this.builder = builder;
        this.name = name;
        this.value = value;
    }

    /** {@inheritDoc} */
    @Override
    public T addAttribute(final String key, final boolean value) {
        return put(key, Boolean.toString(value));
    }

    /** {@inheritDoc} */
    @Override
    public T addAttribute(final String key, final Enum<?> value) {
        return put(key, Optional.ofNullable(value).map(Enum::name).orElse(null));
    }

    /** {@inheritDoc} */
    @Override
    public T addAttribute(final String key, final int value) {
        return put(key, Integer.toString(value));
    }

    /** {@inheritDoc} */
    @Override
    public T addAttribute(final String key, final Level level) {
        return put(key, Optional.ofNullable(level).map(Level::toString).orElse(null));
    }

    /** {@inheritDoc} */
    @Override
    public T addAttribute(final String key, final Object value) {
        return put(key, Optional.ofNullable(value).map(Object::toString).orElse(null));
    }

    /** {@inheritDoc} */
    @Override
    public T addAttribute(final String key, final String value) {
        return put(key, value);
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public T addComponent(final ComponentBuilder<?> builder) {
        components.add(builder.build());
        return (T) this;
    }

    /** {@inheritDoc} */
    @Override
    public Component build() {
        final Component component = new Component(type, name, value);
        component.getAttributes().putAll(attributes);
        component.getComponents().addAll(components);
        return component;
    }

    /** {@inheritDoc} */
    @Override
    public CB getBuilder() {
        return builder;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Puts the given key/value pair to the attribute map.
     * <p>
     *   If the value is {@code null} the component attribute with the given {@code key} will
     *   instead be removed from the map.
     * </p>
     * @param key the key
     * @param value the value
     * @return this builder (for chaining)
     */
    @SuppressWarnings("unchecked")
    protected T put(final String key, final String value) {

        if (value != null) {
            attributes.put(key, value);
        } else {
            attributes.remove(key);
        }

        return (T) this;
    }
}
