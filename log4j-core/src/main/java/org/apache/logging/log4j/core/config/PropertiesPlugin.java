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

import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.lookup.InterpolatorFactory;
import org.apache.logging.log4j.core.lookup.PropertiesLookup;
import org.apache.logging.log4j.core.lookup.StrLookup;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.plugins.di.Key;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles properties defined in the configuration.
 */
@Configurable(printObject = true)
@Plugin("properties")
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
        final Map<String, String> map;
        if (properties == null) {
            map = config.getProperties();
        } else {
            map = new HashMap<>(config.getProperties());

            for (final Property prop : properties) {
                map.put(prop.getName(), prop.getValue());
            }
        }
        return config.getComponent(Key.forClass(InterpolatorFactory.class)).newInterpolator(new PropertiesLookup(map));
    }
}
