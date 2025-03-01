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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.util.Builder;
import org.jspecify.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Builds arbitrary components and is the base type for the provided components.
 * @param <T> The ComponentBuilder's own type for fluent APIs.
 * @since 2.4
 */
@ProviderType
public interface ComponentBuilder<T extends ComponentBuilder<T>> extends Builder<Component> {

    /**
     * Returns the name of the component.
     * @return The component's name or {@code null} if it doesn't have one.
     */
    @Nullable
    String getName();

    /**
     * Retrieves the {@code ConfigurationBuilder}.
     * @return The ConfigurationBuilder.
     */
    ConfigurationBuilder<? extends Configuration> getBuilder();

    /**
     * Adds a sub component.
     * @param builder The Assembler for the subcomponent with all of its attributes and sub-components set.
     * @return this builder (for chaining)
     * @throws NullPointerException if the given {@code builder} is {@code null}
     */
    T addComponent(ComponentBuilder<?> builder);

    /**
     * Sets a String attribute.
     * <p>
     *   If the given {@code level} value is {@code null}, the component attribute with the given key
     *   will be removed (if present).
     * </p>
     * @param key The attribute key.
     * @param value The value of the attribute.
     * @return this builder (for chaining)
     * @throws NullPointerException if the given {@code key} is {@code null}
     */
    T setAttribute(String key, @Nullable String value);

    /**
     * Sets a logging Level attribute.
     * <p>
     *   If the given {@code level} value is {@code null}, the component attribute with the given key
     *   will be removed (if present).
     * </p>
     * @param key The attribute key.
     * @param level The logging Level.
     * @return this builder (for chaining)
     * @throws NullPointerException if the given {@code key} is {@code null}
     */
    T setAttribute(String key, @Nullable Level level);

    /**
     * Sets an enumeration attribute.
     * <p>
     *   If the given {@code level} value is {@code null}, the component attribute with the given key
     *   will be removed (if present).
     * </p>
     * @param key The attribute key.
     * @param value The enumeration.
     * @return this builder (for chaining)
     * @throws NullPointerException if the given {@code key} is {@code null}
     */
    T setAttribute(String key, @Nullable Enum<?> value);

    /**
     * Sets an integer attribute.
     * @param key The attribute key.
     * @param value The integer value.
     * @return this builder (for chaining)
     * @throws NullPointerException if the given {@code key} is {@code null}
     */
    T setAttribute(String key, int value);

    /**
     * Sets a boolean attribute.
     * @param key The attribute key.
     * @param value The boolean value.
     * @return this builder (for chaining)
     * @throws NullPointerException if the given {@code key} is {@code null}
     */
    T setAttribute(String key, boolean value);

    /**
     * Sets an Object attribute.
     * <p>
     *   If the given {@code value} is {@code null}, the component attribute with the given key
     *   will be removed (if present).
     * </p>
     * @param key The attribute key.
     * @param value The object value.
     * @return this builder (for chaining)
     * @throws NullPointerException if the given {@code key} is {@code null}
     */
    T setAttribute(String key, @Nullable Object value);

    /**
     * Adds a String attribute.
     * <p>
     *   If the given {@code level} value is {@code null}, the component attribute with the given key
     *   will be removed (if present).
     * </p>
     * @param key The attribute key.
     * @param value The value of the attribute.
     * @return this builder (for chaining)
     * @throws NullPointerException if the given {@code key} is {@code null}
     * @deprecated use {@link #setAttribute(String, int)}
     */
    @Deprecated
    default T addAttribute(String key, @Nullable String value) {
        return setAttribute(key, value);
    }

    /**
     * Adds a logging Level attribute.
     * <p>
     *   If the given {@code level} value is {@code null}, the component attribute with the given key
     *   will be removed (if present).
     * </p>
     * @param key The attribute key.
     * @param level The logging Level.
     * @return this builder (for chaining)
     * @throws NullPointerException if the given {@code key} is {@code null}
     * @deprecated use {@link #setAttribute(String, Level)}
     */
    @Deprecated
    default T addAttribute(String key, @Nullable Level level) {
        return setAttribute(key, level);
    }

    /**
     * Adds an enumeration attribute.
     * <p>
     *   If the given {@code level} value is {@code null}, the component attribute with the given key
     *   will be removed (if present).
     * </p>
     * @param key The attribute key.
     * @param value The enumeration.
     * @return this builder (for chaining)
     * @throws NullPointerException if the given {@code key} is {@code null}
     * @deprecated use {@link #setAttribute(String, Enum)}
     */
    @Deprecated
    default T addAttribute(String key, @Nullable Enum<?> value) {
        return setAttribute(key, value);
    }

    /**
     * Adds an integer attribute.
     * @param key The attribute key.
     * @param value The integer value.
     * @return this builder (for chaining)
     * @throws NullPointerException if the given {@code key} is {@code null}
     * @deprecated use {@link #setAttribute(String, int)}
     */
    @Deprecated
    default T addAttribute(String key, int value) {
        return setAttribute(key, value);
    }

    /**
     * Adds a boolean attribute.
     * @param key The attribute key.
     * @param value The boolean value.
     * @return this builder (for chaining)
     * @throws NullPointerException if the given {@code key} is {@code null}
     * @deprecated use {@link #setAttribute(String, boolean)}
     */
    @Deprecated
    default T addAttribute(String key, boolean value) {
        return setAttribute(key, value);
    }

    /**
     * Adds an Object attribute.
     * <p>
     *   If the given {@code value} is {@code null}, the component attribute with the given key
     *   will be removed (if present).
     * </p>
     * @param key The attribute key.
     * @param value The object value.
     * @return this builder (for chaining)
     * @throws NullPointerException if the given {@code key} is {@code null}
     * @deprecated use {@link #setAttribute(String, Object)}
     */
    @Deprecated
    default T addAttribute(String key, @Nullable Object value) {
        return setAttribute(key, value);
    }
}
