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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.builder.api.Component;
import org.apache.logging.log4j.core.config.builder.api.ComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Generic base default {@link ComponentBuilder} implementation that captures attributes and children
 * which are used to build a new {@link Component} instance.
 *
 * @param <T> the type of the component builder
 * @param <CB> the type of the configuration builder
 * @since 2.4
 */
@ProviderType
class DefaultComponentBuilder<T extends ComponentBuilder<T>, CB extends ConfigurationBuilder<? extends Configuration>>
        implements ComponentBuilder<T> {

    private final CB builder;
    private final String pluginType;
    private final Map<String, String> attributes = new LinkedHashMap<>();
    private final List<Component> components = new ArrayList<>();
    private final @Nullable String name;
    private final @Nullable String value;

    /**
     * Constructs a new instance with the given configuration builder and plugin-type with {@code null} name and value.
     * @param builder the configuration builder
     * @param pluginType the plugin-type of the component being built
     * @throws NullPointerException if either {@code builder} or {@code type} argument is null
     */
    public DefaultComponentBuilder(final CB builder, final String pluginType) {
        this(builder, pluginType, null, null);
    }

    /**
     * Constructs a new instancer with the given configuration builder, plugin-type, name, and {@code null} value.
     * @param builder the configuration builder
     * @param pluginType the plugin-type of the component being built
     * @param name the component name
     * @throws NullPointerException if either {@code builder} or {@code type} argument is null
     */
    public DefaultComponentBuilder(final CB builder, final String pluginType, final @Nullable String name) {
        this(builder, pluginType, name, null);
    }

    /**
     * Constructs a new instance with the given configuration builder, plugin-type, name and value.
     * @param builder the configuration builder
     * @param pluginType the type (plugin-type) of the component being built
     * @param name the component name
     * @param value the component value
     * @throws NullPointerException if either {@code builder} or {@code type} argument is null
     */
    public DefaultComponentBuilder(
            final CB builder, final String pluginType, final @Nullable String name, final @Nullable String value) {
        super();
        this.builder = Objects.requireNonNull(builder, "The 'builder' argument must not be null.");
        this.pluginType = Objects.requireNonNull(pluginType, "The 'type' argument must not be null.");
        this.name = name;
        this.value = value;
    }

    /** {@inheritDoc} */
    @Override
    public Component build() {
        final Component component = new Component(pluginType, name, value);
        component.getAttributes().putAll(attributes);
        component.getComponents().addAll(components);
        return component;
    }

    /** {@inheritDoc} */
    @Override
    public T addComponent(final ComponentBuilder<?> builder) {
        Objects.requireNonNull(builder, "The 'builder' argument must not be null.");
        components.add(builder.build());
        return self();
    }

    /** {@inheritDoc} */
    @Override
    public @NonNull CB getBuilder() {
        return builder;
    }

    /** {@inheritDoc} */
    @Override
    public @Nullable String getName() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public T setAttribute(final String key, final boolean value) {
        return putAttribute(key, Boolean.toString(value));
    }

    /** {@inheritDoc} */
    @Override
    public T setAttribute(final String key, final int value) {
        return putAttribute(key, Integer.toString(value));
    }

    /** {@inheritDoc} */
    @Override
    public T setAttribute(final String key, final @Nullable Enum<?> value) {
        return putAttribute(key, (value != null) ? value.name() : null);
    }

    /** {@inheritDoc} */
    @Override
    public T setAttribute(final String key, final @Nullable Level level) {
        return putAttribute(key, (level != null) ? level.toString() : null);
    }

    /** {@inheritDoc} */
    @Override
    public T setAttribute(final String key, final @Nullable Object value) {
        return putAttribute(key, Objects.toString(value, null));
    }

    /** {@inheritDoc} */
    @Override
    public T setAttribute(final String key, final @Nullable String value) {
        return putAttribute(key, value);
    }

    /**
     * Clears the internal state removing all configured attributes and components.
     * <p>
     *   This method is primarily intended to be used in testing.
     * </p>
     */
    protected void clear() {
        synchronized (this) {
            attributes.clear();
            components.clear();
        }
    }

    /**
     * Gets the value of the component attribute with the given key.
     *
     * @param key the key
     * @return the attribute value or {@code null} if not found
     */
    protected @Nullable String getAttribute(final @NonNull String key) {

        Objects.requireNonNull(key, "The 'key' argument must not be null.");

        return this.attributes.get(key);
    }

    /**
     * Gets the map of key/value component attributes.
     * <p>
     *   The result map is guaranteed to have both non-{@code null} keys and values.
     * </p>
     * @return an <i>immutable</i> map of the key/value attributes
     */
    protected Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    /**
     * Gets the list of child components.
     * @return an <i>immutable</i> list of the child components
     */
    protected List<Component> getComponents() {
        return Collections.unmodifiableList(this.components);
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
     * @throws NullPointerException if the given {@code key} argument is {@code null}
     */
    private T putAttribute(final @NonNull String key, final @Nullable String value) {

        Objects.requireNonNull(key, "The 'key' argument must not be null.");

        if (value != null) {
            attributes.put(key, value);
        } else {
            attributes.remove(key);
        }

        return self();
    }

    /**
     * Returns an instance of this builder cast to its generic type.
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    private T self() {
        return (T) this;
    }
}
