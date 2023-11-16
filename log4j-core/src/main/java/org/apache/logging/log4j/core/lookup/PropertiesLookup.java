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
package org.apache.logging.log4j.core.lookup;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Property;

/**
 * A lookup designed for {@code Properties} defined in the configuration. This is similar
 * to {@link MapLookup} without special handling for structured messages.
 *
 * Note that this lookup is not a plugin, but wired as a default lookup in the configuration.
 */
public final class PropertiesLookup implements StrLookup {

    /** Logger context properties. */
    private final Map<String, String> contextProperties;

    /** Configuration properties. */
    private final Map<String, ConfigurationPropertyResult> configurationProperties;

    public PropertiesLookup(final Property[] configProperties, final Map<String, String> contextProperties) {
        this.contextProperties = contextProperties == null ? Collections.emptyMap() : contextProperties;
        this.configurationProperties =
                configProperties == null ? Collections.emptyMap() : createConfigurationPropertyMap(configProperties);
    }

    /**
     * Constructs a new instance for the given map.
     *
     * @param properties map these.
     */
    public PropertiesLookup(final Map<String, String> properties) {
        this(Property.EMPTY_ARRAY, properties);
    }

    @Override
    public String lookup(@SuppressWarnings("ignored") final LogEvent event, final String key) {
        return lookup(key);
    }

    /**
     * Looks a value from configuration properties.
     * <p>
     * If the property is not defined, then null is returned.
     * </p>
     *
     * @param key the key to be looked up, may be null
     * @return the matching value, null if no match
     */
    @Override
    public String lookup(final String key) {
        final LookupResult result = evaluate(key);
        return result == null ? null : result.value();
    }

    @Override
    public LookupResult evaluate(final String key) {
        if (key == null) {
            return null;
        }
        final LookupResult configResult = configurationProperties.get(key);
        if (configResult != null) {
            return configResult;
        }
        // Allow the context map to be mutated after this lookup has been initialized.
        final String contextResult = contextProperties.get(key);
        return contextResult == null ? null : new ContextPropertyResult(contextResult);
    }

    @Override
    public LookupResult evaluate(@SuppressWarnings("ignored") final LogEvent event, final String key) {
        return evaluate(key);
    }

    @Override
    public String toString() {
        return "PropertiesLookup{" + "contextProperties="
                + contextProperties + ", configurationProperties="
                + configurationProperties + '}';
    }

    private static Map<String, ConfigurationPropertyResult> createConfigurationPropertyMap(final Property[] props) {
        // The raw property values must be used without the substitution handled by the plugin framework
        // which calls this method, otherwise we risk re-interpolating through unexpected data.
        // The PropertiesLookup is unique in that results from this lookup support recursive evaluation.
        final Map<String, ConfigurationPropertyResult> result = new HashMap<>(props.length);
        for (Property property : props) {
            result.put(property.getName(), new ConfigurationPropertyResult(property.getRawValue()));
        }
        return result;
    }

    private static final class ConfigurationPropertyResult implements LookupResult {

        private final String value;

        ConfigurationPropertyResult(final String value) {
            this.value = Objects.requireNonNull(value, "value is required");
        }

        @Override
        public String value() {
            return value;
        }

        /**
         * Properties are a special case in which lookups contained
         * within the properties map are allowed for recursive evaluation.
         */
        @Override
        public boolean isLookupEvaluationAllowedInValue() {
            return true;
        }

        @Override
        public String toString() {
            return "ConfigurationPropertyResult{'" + value + "'}";
        }
    }

    private static final class ContextPropertyResult implements LookupResult {

        private final String value;

        ContextPropertyResult(final String value) {
            this.value = Objects.requireNonNull(value, "value is required");
        }

        @Override
        public String value() {
            return value;
        }

        /**
         * Unlike configuration properties, context properties are not built around lookup syntax.
         */
        @Override
        public boolean isLookupEvaluationAllowedInValue() {
            return false;
        }

        @Override
        public String toString() {
            return "ContextPropertyResult{'" + value + "'}";
        }
    }
}
