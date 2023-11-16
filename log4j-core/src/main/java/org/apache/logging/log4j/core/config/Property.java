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
package org.apache.logging.log4j.core.config;

import java.util.Objects;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.PluginValue;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;

/**
 * Represents a key/value pair in the configuration.
 */
@Plugin(name = "property", category = Node.CATEGORY, printObject = true)
public final class Property {

    /**
     * @since 2.11.2
     */
    public static final Property[] EMPTY_ARRAY = {};

    private static final Logger LOGGER = StatusLogger.getLogger();

    private final String name;
    private final String rawValue;
    private final String value;
    private final boolean valueNeedsLookup;

    private Property(final String name, final String rawValue, final String value) {
        this.name = name;
        this.rawValue = rawValue;
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
     * Returns the original raw property value without substitution.
     * @return the raw value of the property, or empty string if it is not set.
     */
    public String getRawValue() {
        return Objects.toString(rawValue, Strings.EMPTY);
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
     * @return {@code true} if the value contains {@code "${}"}, {@code false} otherwise
     */
    public boolean isValueNeedsLookup() {
        return valueNeedsLookup;
    }

    /**
     * Evaluate this property with the provided substitutor. If {@link #isValueNeedsLookup()} is {@code false},
     * the {@link #getValue() value} is returned, otherwise the {@link #getRawValue() raw value} is evaluated
     * with the given substitutor.
     */
    public String evaluate(final StrSubstitutor substitutor) {
        return valueNeedsLookup
                // Unescape the raw value first, handling '$${ctx:foo}' -> '${ctx:foo}'
                ? substitutor.replace(PropertiesPlugin.unescape(getRawValue()))
                : getValue();
    }

    /**
     * Creates a Property.
     *
     * @param name The key.
     * @param value The value.
     * @return A Property.
     */
    public static Property createProperty(final String name, final String value) {
        return createProperty(name, value, value);
    }

    /**
     * Creates a Property.
     *
     * @param name The key.
     * @param rawValue The value without any substitution applied.
     * @param value The value.
     * @return A Property.
     */
    public static Property createProperty(final String name, final String rawValue, final String value) {
        if (name == null) {
            throw new IllegalArgumentException("Property name cannot be null");
        }
        return new Property(name, rawValue, value);
    }

    /**
     * Creates a Property.
     *
     * @param name The key.
     * @param rawValue The value without any substitution applied.
     * @param configuration configuration used to resolve the property value from the rawValue
     * @return A Property.
     */
    @PluginFactory
    public static Property createProperty(
            @PluginAttribute("name") final String name,
            @PluginValue(value = "value", substitute = false) final String rawValue,
            @PluginConfiguration final Configuration configuration) {
        return createProperty(
                name,
                rawValue,
                configuration == null
                        ? rawValue
                        : configuration.getStrSubstitutor().replace(rawValue));
    }

    @Override
    public String toString() {
        return name + '=' + getValue();
    }
}
