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
package org.apache.logging.log4j.core.layout;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.pattern.PatternFormatter;
import org.apache.logging.log4j.core.pattern.PatternParser;
import org.apache.logging.log4j.status.StatusLogger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Selects the pattern to use based on the Marker in the LogEvent.
 */
@Plugin(name = "MarkerPatternSelector", category = Node.CATEGORY, elementType = PatternSelector.ELEMENT_TYPE, printObject = true)
public class MarkerPatternSelector implements PatternSelector {

    private final Map<String, PatternFormatter[]> formatterMap = new HashMap<>();

    private final Map<String, String> patternMap = new HashMap<>();

    private final PatternFormatter[] defaultFormatters;

    private final String defaultPattern;

    private static Logger LOGGER = StatusLogger.getLogger();


    public MarkerPatternSelector(final PatternMatch[] properties, final String defaultPattern,
                                 final boolean alwaysWriteExceptions, final boolean noConsoleNoAnsi,
                                 final Configuration config) {
        final PatternParser parser = PatternLayout.createPatternParser(config);
        for (final PatternMatch property : properties) {
            try {
                final List<PatternFormatter> list = parser.parse(property.getPattern(), alwaysWriteExceptions, noConsoleNoAnsi);
                formatterMap.put(property.getKey(), list.toArray(new PatternFormatter[list.size()]));
                patternMap.put(property.getKey(), property.getPattern());
            } catch (final RuntimeException ex) {
                throw new IllegalArgumentException("Cannot parse pattern '" + property.getPattern() + "'", ex);
            }
        }
        try {
            final List<PatternFormatter> list = parser.parse(defaultPattern, alwaysWriteExceptions, noConsoleNoAnsi);
            defaultFormatters = list.toArray(new PatternFormatter[list.size()]);
            this.defaultPattern = defaultPattern;
        } catch (final RuntimeException ex) {
            throw new IllegalArgumentException("Cannot parse pattern '" + defaultPattern + "'", ex);
        }
    }

    @Override
    public PatternFormatter[] getFormatters(final LogEvent event) {
        final Marker marker = event.getMarker();
        if (marker == null) {
            return defaultFormatters;
        }
        for (final String key : formatterMap.keySet()) {
            if (marker.isInstanceOf(key)) {
                return formatterMap.get(key);
            }
        }
        return defaultFormatters;
    }


    @PluginFactory
    public static MarkerPatternSelector createSelector(@PluginElement("PatternMatch") final PatternMatch[] properties,
                                                       @PluginAttribute("defaultPattern") String defaultPattern,
                                                       @PluginAttribute(value = "alwaysWriteExceptions", defaultBoolean = true) final boolean alwaysWriteExceptions,
                                                       @PluginAttribute(value = "noConsoleNoAnsi", defaultBoolean = false) final boolean noConsoleNoAnsi,
                                                       @PluginConfiguration final Configuration config) {
        if (defaultPattern == null) {
            defaultPattern = PatternLayout.DEFAULT_CONVERSION_PATTERN;
        }
        if (properties == null || properties.length == 0) {
            LOGGER.warn("No marker patterns were provided");
        }
        return new MarkerPatternSelector(properties, defaultPattern, alwaysWriteExceptions,
                noConsoleNoAnsi, config);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (final Map.Entry<String, String> entry : patternMap.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append("key=\"").append(entry.getKey()).append("\", pattern=\"").append(entry.getValue()).append("\"");
            first = false;
        }
        if (!first) {
            sb.append(", ");
        }
        sb.append("default=\"").append(defaultPattern).append("\"");
        return sb.toString();
    }
}
