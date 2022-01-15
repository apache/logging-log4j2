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
package org.apache.logging.log4j.core.lookup;

import org.apache.logging.log4j.core.LogEvent;

import java.util.Collections;
import java.util.Map;

/**
 * A lookup designed for {@code Properties} defined in the configuration. This is similar
 * to {@link MapLookup} without special handling for structured messages.
 *
 * Note that this lookup is not a plugin, but wired as a default lookup in the configuration.
 */
public final class PropertiesLookup implements StrLookup {

    /**
     * Configuration from which to read properties.
     */
    private final Map<String, String> properties;

    /**
     * Constructs a new instance for the given map.
     *
     * @param properties map these.
     */
    public PropertiesLookup(final Map<String, String> properties) {
        this.properties = properties == null
                ? Collections.emptyMap()
                : properties;
    }

    /**
     * Gets the property map.
     *
     * @return the property map.
     */
    public Map<String, String> getProperties() {
        return properties;
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
        return key == null ? null : properties.get(key);
    }

    @Override
    public String toString() {
        return "PropertiesLookup{properties=" + properties + '}';
    }

}
