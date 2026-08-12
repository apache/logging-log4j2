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

import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.PluginValue;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;

/**
 * Represents a key/value pair in the configuration.
 * <p>
 * This class is a strangler facade over {@link org.apache.logging.log4j.common.config.Property}.
 * </p>
 */
@Plugin(name = "Property", category = Node.CATEGORY, printObject = true)
public final class Property {

    /**
     * @since 2.11.2
     */
    public static final Property[] EMPTY_ARRAY = {};

    private final org.apache.logging.log4j.common.config.Property delegate;

    private Property(final org.apache.logging.log4j.common.config.Property delegate) {
        this.delegate = delegate;
    }

    /**
     * Returns the property name.
     * @return the property name.
     */
    public String getName() {
        return delegate.getName();
    }

    /**
     * Returns the original raw property value without substitution.
     * @return the raw value of the property, or empty string if it is not set.
     */
    public String getRawValue() {
        return delegate.getRawValue();
    }

    /**
     * Returns the property value.
     * @return the value of the property.
     */
    public String getValue() {
        return delegate.getValue();
    }

    /**
     * Returns {@code true} if the value contains a substitutable property that requires a lookup to be resolved.
     * @return {@code true} if the value contains {@code "${}"}, {@code false} otherwise
     */
    public boolean isValueNeedsLookup() {
        return delegate.isValueNeedsLookup();
    }

    /**
     * Evaluate this property with the provided substitutor. If {@link #isValueNeedsLookup()} is {@code false},
     * the {@link #getValue() value} is returned, otherwise the {@link #getRawValue() raw value} is evaluated
     * with the given substitutor.
     */
    public String evaluate(final StrSubstitutor substitutor) {
        return delegate.isValueNeedsLookup()
                // Unescape the raw value first, handling '$${ctx:foo}' -> '${ctx:foo}'
                ? substitutor.replace(PropertiesPlugin.unescape(getRawValue()))
                : getValue();
    }

    /**
     * Returns the shared {@link org.apache.logging.log4j.common.config.Property} delegate.
     *
     * @return the shared property value
     */
    public org.apache.logging.log4j.common.config.Property asCommonProperty() {
        return delegate;
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
        return new Property(org.apache.logging.log4j.common.config.Property.createProperty(name, rawValue, value));
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
        return delegate.toString();
    }
}
