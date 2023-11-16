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
package org.apache.logging.log4j.core.layout;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.impl.LocationAware;
import org.apache.logging.log4j.core.pattern.PatternFormatter;
import org.apache.logging.log4j.core.pattern.PatternParser;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Selects the pattern to use based on the Level in the LogEvent.
 */
@Plugin(
        name = "LevelPatternSelector",
        category = Node.CATEGORY,
        elementType = PatternSelector.ELEMENT_TYPE,
        printObject = true)
public class LevelPatternSelector implements PatternSelector, LocationAware {

    /**
     * Custom MarkerPatternSelector builder. Use the {@link LevelPatternSelector#newBuilder() builder factory method} to create this.
     */
    public static class Builder implements org.apache.logging.log4j.core.util.Builder<LevelPatternSelector> {

        @PluginElement("PatternMatch")
        private PatternMatch[] properties;

        @PluginBuilderAttribute("defaultPattern")
        private String defaultPattern;

        @PluginBuilderAttribute(value = "alwaysWriteExceptions")
        private boolean alwaysWriteExceptions = true;

        @PluginBuilderAttribute(value = "disableAnsi")
        private boolean disableAnsi;

        @PluginBuilderAttribute(value = "noConsoleNoAnsi")
        private boolean noConsoleNoAnsi;

        @PluginConfiguration
        private Configuration configuration;

        @Override
        public LevelPatternSelector build() {
            if (defaultPattern == null) {
                defaultPattern = PatternLayout.DEFAULT_CONVERSION_PATTERN;
            }
            if (properties == null || properties.length == 0) {
                LOGGER.warn("No marker patterns were provided with PatternMatch");
                return null;
            }
            return new LevelPatternSelector(
                    properties, defaultPattern, alwaysWriteExceptions, disableAnsi, noConsoleNoAnsi, configuration);
        }

        public Builder setProperties(final PatternMatch[] properties) {
            this.properties = properties;
            return this;
        }

        public Builder setDefaultPattern(final String defaultPattern) {
            this.defaultPattern = defaultPattern;
            return this;
        }

        public Builder setAlwaysWriteExceptions(final boolean alwaysWriteExceptions) {
            this.alwaysWriteExceptions = alwaysWriteExceptions;
            return this;
        }

        public Builder setDisableAnsi(final boolean disableAnsi) {
            this.disableAnsi = disableAnsi;
            return this;
        }

        public Builder setNoConsoleNoAnsi(final boolean noConsoleNoAnsi) {
            this.noConsoleNoAnsi = noConsoleNoAnsi;
            return this;
        }

        public Builder setConfiguration(final Configuration configuration) {
            this.configuration = configuration;
            return this;
        }
    }

    private final Map<String, PatternFormatter[]> formatterMap = new HashMap<>();

    private final Map<String, String> patternMap = new HashMap<>();

    private final PatternFormatter[] defaultFormatters;

    private final String defaultPattern;

    private static Logger LOGGER = StatusLogger.getLogger();

    private final boolean requiresLocation;

    /**
     * @deprecated Use {@link #newBuilder()} instead. This will be private in a future version.
     */
    @Deprecated
    public LevelPatternSelector(
            final PatternMatch[] properties,
            final String defaultPattern,
            final boolean alwaysWriteExceptions,
            final boolean noConsoleNoAnsi,
            final Configuration config) {
        this(properties, defaultPattern, alwaysWriteExceptions, false, noConsoleNoAnsi, config);
    }

    private LevelPatternSelector(
            final PatternMatch[] properties,
            final String defaultPattern,
            final boolean alwaysWriteExceptions,
            final boolean disableAnsi,
            final boolean noConsoleNoAnsi,
            final Configuration config) {
        boolean needsLocation = false;
        final PatternParser parser = PatternLayout.createPatternParser(config);
        for (final PatternMatch property : properties) {
            try {
                final List<PatternFormatter> list =
                        parser.parse(property.getPattern(), alwaysWriteExceptions, disableAnsi, noConsoleNoAnsi);
                final PatternFormatter[] formatters = list.toArray(PatternFormatter.EMPTY_ARRAY);
                formatterMap.put(property.getKey(), formatters);
                for (int i = 0; !needsLocation && i < formatters.length; ++i) {
                    needsLocation = formatters[i].requiresLocation();
                }

                patternMap.put(property.getKey(), property.getPattern());
            } catch (final RuntimeException ex) {
                throw new IllegalArgumentException("Cannot parse pattern '" + property.getPattern() + "'", ex);
            }
        }
        try {
            final List<PatternFormatter> list =
                    parser.parse(defaultPattern, alwaysWriteExceptions, disableAnsi, noConsoleNoAnsi);
            defaultFormatters = list.toArray(PatternFormatter.EMPTY_ARRAY);
            this.defaultPattern = defaultPattern;
            for (int i = 0; !needsLocation && i < defaultFormatters.length; ++i) {
                needsLocation = defaultFormatters[i].requiresLocation();
            }
        } catch (final RuntimeException ex) {
            throw new IllegalArgumentException("Cannot parse pattern '" + defaultPattern + "'", ex);
        }
        requiresLocation = needsLocation;
    }

    @Override
    public boolean requiresLocation() {
        return requiresLocation;
    }

    @Override
    public PatternFormatter[] getFormatters(final LogEvent event) {
        final Level level = event.getLevel();
        if (level == null) {
            return defaultFormatters;
        }
        for (final String key : formatterMap.keySet()) {
            if (level.name().equalsIgnoreCase(key)) {
                return formatterMap.get(key);
            }
        }
        return defaultFormatters;
    }

    /**
     * Creates a builder for a custom ScriptPatternSelector.
     *
     * @return a ScriptPatternSelector builder.
     */
    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Deprecated, use {@link #newBuilder()} instead.
     * @param properties PatternMatch configuration items
     * @param defaultPattern the default pattern
     * @param alwaysWriteExceptions To always write exceptions even if the pattern contains no exception conversions.
     * @param noConsoleNoAnsi Do not output ANSI escape codes if System.console() is null.
     * @param configuration the current configuration
     * @return a new MarkerPatternSelector.
     * @deprecated Use {@link #newBuilder()} instead.
     */
    @Deprecated
    public static LevelPatternSelector createSelector(
            final PatternMatch[] properties,
            final String defaultPattern,
            final boolean alwaysWriteExceptions,
            final boolean noConsoleNoAnsi,
            final Configuration configuration) {
        final Builder builder = newBuilder();
        builder.setProperties(properties);
        builder.setDefaultPattern(defaultPattern);
        builder.setAlwaysWriteExceptions(alwaysWriteExceptions);
        builder.setNoConsoleNoAnsi(noConsoleNoAnsi);
        builder.setConfiguration(configuration);
        return builder.build();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (final Map.Entry<String, String> entry : patternMap.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append("key=\"")
                    .append(entry.getKey())
                    .append("\", pattern=\"")
                    .append(entry.getValue())
                    .append("\"");
            first = false;
        }
        if (!first) {
            sb.append(", ");
        }
        sb.append("default=\"").append(defaultPattern).append("\"");
        return sb.toString();
    }
}
