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
package org.apache.logging.log4j.core.config.assembler.api;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configuration;

/**
 * Assembler for arbitrary components and the base class for the provided components.
 */
@SuppressWarnings("rawtypes")
public interface ComponentAssembler<T extends ComponentAssembler> extends Assembler<Component> {

    /**
     * Add an attribute to the component.
     * @param key The attribute key.
     * @param value The value of the attribute.
     * @return The ComponentAssembler.
     */
    T addAttribute(String key, String value);

    /**
     * Add a logging Level attribute to the component.
     * @param key The attribute key.
     * @param level The logging Level.
     * @return The ComponentAssembler.
     */
    T addAttribute(String key, Level level);

    /**
     * Add an enumeration.
     * @param key The attribute key.
     * @param value The enumeration.
     * @return The ComponentAssembler.
     */
    T addAttribute(String key, Enum<?> value);

    /**
     * Add an integer attribute.
     * @param key The attribute key.
     * @param value The integer value.
     * @return The ComponentAssembler.
     */
    T addAttribute(String key, int value);

    /**
     * Add a boolean attribute.
     * @param key The attribute key.
     * @param value The integer value.
     * @return The ComponentAssembler.
     */
    T addAttribute(String key, boolean value);

    /**
     * Add an Object attribute.
     * @param key The attribute key.
     * @param value The integer value.
     * @return The ComponentAssembler.
     */
    T addAttribute(String key, Object value);

    /**
     * Add a sub component.
     * @param assembler The Assembler for the subcomponent with all of its attributes and sub-components set.
     * @return The ComponentAssembler.
     */
    T addComponent(ComponentAssembler<?> assembler);

    /**
     * Return the name of the component, if any.
     * @return The components name or null if it doesn't have one.
     */
    String getName();

    /**
     * Retrieve the ConfigurationAssembler.
     * @return The ConfiguratonAssembler.
     */
    ConfigurationAssembler<? extends Configuration> getAssembler();
}
