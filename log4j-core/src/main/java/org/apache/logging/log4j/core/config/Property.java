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
package org.apache.logging.log4j.core.config;

import java.util.Objects;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.PluginValue;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;

/**
 * Represents a key/value pair in the configuration.
 */
@Plugin(name = "property", category = Node.CATEGORY, printObject = true)
public final class Property {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private final String name;
    private final String value;
    private final boolean valueNeedsLookup;

    private Property(final String name, final String value) {
        this.name = name;
        this.value = value;
        this.valueNeedsLookup = value != null && value.contains("${");
    }

    /**
     * Returns the property name.
     * @return the property name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the property value.
     * @return the value of the property.
     */
    public String getValue() {
        return Objects.toString(value, Strings.EMPTY); // LOG4J2-1313 null would be same as Property not existing
    }

    /**
     * Returns {@code true} if the value contains a substitutable property that requires a lookup to be resolved.
     * @return {@code true} if the value contains {@code "${"}, {@code false} otherwise
     */
    public boolean isValueNeedsLookup() {
        return valueNeedsLookup;
    }

    /**
     * Creates a Property.
     *
     * @param name The key.
     * @param value The value.
     * @return A Property.
     */
    @PluginFactory
    public static Property createProperty(
            @PluginAttribute("name") final String name,
            @PluginValue("value") final String value) {
        if (name == null) {
            LOGGER.error("Property name cannot be null");
        }
        return new Property(name, value);
    }

    @Override
    public String toString() {
        return name + '=' + getValue();
    }
}
