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

/**
 * Builds arbitrary components and is the base type for the provided components.
 * @param <T> The ComponentBuilder's own type for fluent APIs.
 * @since 2.4
 */
public interface ComponentBuilder<T extends ComponentBuilder<T>> extends Builder<Component> {

    /**
     * Adds a String attribute.
     * @param key The attribute key.
     * @param value The value of the attribute.
     * @return This ComponentBuilder.
     */
    T addAttribute(String key, String value);

    /**
     * Adds a logging Level attribute.
     * @param key The attribute key.
     * @param level The logging Level.
     * @return This ComponentBuilder.
     */
    T addAttribute(String key, Level level);

    /**
     * Adds an enumeration attribute.
     * @param key The attribute key.
     * @param value The enumeration.
     * @return This ComponentBuilder.
     */
    T addAttribute(String key, Enum<?> value);

    /**
     * Adds an integer attribute.
     * @param key The attribute key.
     * @param value The integer value.
     * @return This ComponentBuilder.
     */
    T addAttribute(String key, int value);

    /**
     * Adds a boolean attribute.
     * @param key The attribute key.
     * @param value The boolean value.
     * @return This ComponentBuilder.
     */
    T addAttribute(String key, boolean value);

    /**
     * Adds an Object attribute.
     * @param key The attribute key.
     * @param value The object value.
     * @return This ComponentBuilder.
     */
    T addAttribute(String key, Object value);

    /**
     * Adds a sub component.
     * @param builder The Assembler for the subcomponent with all of its attributes and sub-components set.
     * @return This ComponentBuilder (<em>not</em> the argument).
     */
    T addComponent(ComponentBuilder<?> builder);

    /**
     * Returns the name of the component, if any.
     * @return The component's name or null if it doesn't have one.
     */
    String getName();

    /**
     * Retrieves the ConfigurationBuilder.
     * @return The ConfigurationBuilder.
     */
    ConfigurationBuilder<? extends Configuration> getBuilder();
}
