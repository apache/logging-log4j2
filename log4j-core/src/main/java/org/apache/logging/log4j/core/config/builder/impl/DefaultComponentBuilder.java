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
import java.util.Optional;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.builder.api.Component;
import org.apache.logging.log4j.core.config.builder.api.ComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Generic component that captures attributes and Components in preparation for assembling the Appender's
 * Component.
 *
 * @since 2.4
 */
class DefaultComponentBuilder<T extends ComponentBuilder<T>, CB extends ConfigurationBuilder<? extends Configuration>>
        implements ComponentBuilder<T> {

    private final @NonNull CB builder;
    private final @Nullable String name;
    private final @NonNull String type;
    private final @Nullable String value;
    private final Map<String, String> attributes = new LinkedHashMap<>();
    private final List<Component> components = new ArrayList<>();

    /**
     * Constructs a component builder with the given configuration builder and type with {@code null} name and value.
     * @param builder the configuration builder
     * @param type the type (plugin-type) of the component being built
     * @throws NullPointerException if either {@code builder} or {@code type} argument is null
     */
    public DefaultComponentBuilder(final @NonNull CB builder, final @NonNull String type) {
        this(builder, null, type, null);
    }

    /**
     * Constructs a component builder with the given configuration builder, name, type, and {@code null} value.
     * @param builder the configuration builder
     * @param name the component name
     * @param type the type (plugin-type) of the component being built
     * @throws NullPointerException if either {@code builder} or {@code type} argument is null
     */
    public DefaultComponentBuilder(final @NonNull CB builder, final @Nullable String name, final @NonNull String type) {
        this(builder, name, type, null);
    }

    /**
     * Constructs a component builder with the given configuration builder, name, type, and value.
     * @param builder the configuration builder
     * @param name the component name
     * @param type the type (plugin-type) of the component being built
     * @param value the component value
     * @throws NullPointerException if either {@code builder} or {@code type} argument is null
     */
    public DefaultComponentBuilder(
            final @NonNull CB builder,
            final @Nullable String name,
            final @NonNull String type,
            final @Nullable String value) {
        super();
        this.builder = Objects.requireNonNull(builder, "The 'builder' argument must not be null.");
        this.type = Objects.requireNonNull(type, "The 'type' argument must not be null.");
        this.name = name;
        this.value = value;
    }

    /** {@inheritDoc} */
    @Override
    public @NonNull T addAttribute(final @NonNull String key, final boolean value) {
        return put(key, Boolean.toString(value));
    }

    /** {@inheritDoc} */
    @Override
    public @NonNull T addAttribute(final @NonNull String key, final int value) {
        return put(key, Integer.toString(value));
    }

    /** {@inheritDoc} */
    @Override
    public @NonNull T addAttribute(final @NonNull String key, final @Nullable Enum<?> value) {
        return put(key, Optional.ofNullable(value).map(Enum::name).orElse(null));
    }

    /** {@inheritDoc} */
    @Override
    public @NonNull T addAttribute(final @NonNull String key, final @Nullable Level level) {
        return put(key, Optional.ofNullable(level).map(Level::toString).orElse(null));
    }

    /** {@inheritDoc} */
    @Override
    public @NonNull T addAttribute(final @NonNull String key, final @Nullable Object value) {
        return put(key, Optional.ofNullable(value).map(Object::toString).orElse(null));
    }

    /** {@inheritDoc} */
    @Override
    public @NonNull T addAttribute(final @NonNull String key, final @Nullable String value) {
        return put(key, value);
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
    protected @NonNull Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    /** {@inheritDoc} */
    @Override
    public @NonNull T addComponent(final @NonNull ComponentBuilder<?> builder) {
        Objects.requireNonNull(builder, "The 'builder' argument must not be null.");
        components.add(builder.build());
        return self();
    }

    /** {@inheritDoc} */
    @Override
    public @NonNull Component build() {
        final Component component = new Component(type, name, value);
        component.getAttributes().putAll(attributes);
        component.getComponents().addAll(components);
        return component;
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
    private @NonNull T put(final @NonNull String key, final @Nullable String value) {

        Objects.requireNonNull(key, "The 'key' argument must not be null.");

        if (value != null) {
            attributes.put(key, value);
        } else {
            attributes.remove(key);
        }

        return self();
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
     * Returns an instance of this builder cast to its generic type.
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    protected @NonNull T self() {
        return (T) this;
    }
}
