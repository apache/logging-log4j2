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

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.lookup.ConfigurationStrSubstitutor;
import org.apache.logging.log4j.core.lookup.Interpolator;
import org.apache.logging.log4j.core.lookup.PropertiesLookup;
import org.apache.logging.log4j.core.lookup.StrLookup;

/**
 * Handles properties defined in the configuration.
 */
@Plugin(name = "properties", category = Node.CATEGORY, printObject = true)
public final class PropertiesPlugin {

    private PropertiesPlugin() {
    }

    /**
     * Creates the Properties component.
     * @param properties An array of Property elements.
     * @param config The Configuration.
     * @return An Interpolator that includes the configuration properties.
     */
    @PluginFactory
    public static StrLookup configureSubstitutor(@PluginElement("Properties") final Property[] properties,
                                                 @PluginConfiguration final Configuration config) {
        final Property[] props = properties == null ? Property.EMPTY_ARRAY : properties;
        final Map<String, String> map = new HashMap<>(config.getProperties());
        // Properties may reference other properties, so we build them into an interpolator with a mutable
        // map, and handle replacement ourselves using property.getRawValue instead of getValue which has
        // already been substituted by the plugin system.
        // This way, we don't risk unnecessarily evaluating escaped data, and the property values presented
        // to other components at runtime are fully evaluated.
        boolean anyNeedLookups = false;
        for (final Property prop : props) {
            String rawValue = prop.getRawValue();
            anyNeedLookups |= prop.isRawValueNeedsLookup();
            // handle properties created with the old constructor (null rawValue)
            map.put(prop.getName(), rawValue == null ? prop.getValue() : rawValue);
        }
        final Interpolator interpolator = new Interpolator(new PropertiesLookup(map), config.getPluginPackages());
        if (anyNeedLookups) {
            final ConfigurationStrSubstitutor substitutor = new ConfigurationStrSubstitutor(interpolator);
            for (final Property prop : props) {
                if (prop.isRawValueNeedsLookup()) {
                    final String value = substitutor.replace(prop.getRawValue());
                    map.put(prop.getName(), value);
                }
            }
        }
        return interpolator;
    }
}
