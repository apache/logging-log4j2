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
import org.apache.logging.log4j.core.lookup.Interpolator;
import org.apache.logging.log4j.core.lookup.MapLookup;
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
        if (properties == null) {
            return new Interpolator(config.getProperties());
        }
        final Map<String, String> map = new HashMap<>(config.getProperties());

        for (final Property prop : properties) {
            map.put(prop.getName(), prop.getValue());
        }

        return new Interpolator(new MapLookup(map), config.getPluginPackages());
    }
}
