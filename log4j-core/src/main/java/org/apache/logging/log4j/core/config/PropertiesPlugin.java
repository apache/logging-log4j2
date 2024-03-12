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

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.lookup.Interpolator;
import org.apache.logging.log4j.core.lookup.LookupResult;
import org.apache.logging.log4j.core.lookup.PropertiesLookup;
import org.apache.logging.log4j.core.lookup.StrLookup;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;

/**
 * Handles properties defined in the configuration.
 */
@Plugin(name = "properties", category = Node.CATEGORY, printObject = true)
public final class PropertiesPlugin {

    private static final StrSubstitutor UNESCAPING_SUBSTITUTOR = createUnescapingSubstitutor();

    private PropertiesPlugin() {}

    /**
     * Creates the Properties component.
     * @param properties An array of Property elements.
     * @param config The Configuration.
     * @return An Interpolator that includes the configuration properties.
     */
    @PluginFactory
    public static StrLookup configureSubstitutor(
            @PluginElement("Properties") final Property[] properties, @PluginConfiguration final Configuration config) {
        // For backwards compatibility, we unescape all escaped lookups when properties are parsed.
        // This matches previous behavior for escaped components which were meant to be executed later on.
        final Property[] unescapedProperties = new Property[properties == null ? 0 : properties.length];
        for (int i = 0; i < unescapedProperties.length; i++) {
            unescapedProperties[i] = unescape(properties[i]);
        }
        final Interpolator interpolator = new Interpolator(
                new PropertiesLookup(unescapedProperties, config.getProperties()), config.getPluginPackages());
        interpolator.setConfiguration(config);
        interpolator.setLoggerContext(config.getLoggerContext());
        return interpolator;
    }

    private static Property unescape(final Property input) {
        return Property.createProperty(input.getName(), unescape(input.getRawValue()), input.getValue());
    }

    // Visible for testing
    static String unescape(final String input) {
        return UNESCAPING_SUBSTITUTOR.replace(input);
    }

    /**
     * Creates a new {@link StrSubstitutor} which is configured with no lookups and does not handle
     * defaults. This allows it to unescape one level of escaped lookups without any further processing
     * or removing replacing {@code ${ctx:foo:-default}} with {@code default}.
     */
    private static StrSubstitutor createUnescapingSubstitutor() {
        final StrSubstitutor substitutor = new StrSubstitutor(NullLookup.INSTANCE);
        substitutor.setValueDelimiter(null);
        substitutor.setValueDelimiterMatcher(null);
        return substitutor;
    }

    private enum NullLookup implements StrLookup {
        INSTANCE;

        @Override
        public String lookup(final String key) {
            return null;
        }

        @Override
        public String lookup(final LogEvent event, final String key) {
            return null;
        }

        @Override
        public LookupResult evaluate(final String key) {
            return null;
        }

        @Override
        public LookupResult evaluate(final LogEvent event, final String key) {
            return null;
        }
    }
}
