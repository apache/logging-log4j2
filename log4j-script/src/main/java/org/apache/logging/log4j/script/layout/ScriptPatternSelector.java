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
package org.apache.logging.log4j.script.layout;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.layout.PatternMatch;
import org.apache.logging.log4j.core.layout.PatternSelector;
import org.apache.logging.log4j.core.pattern.PatternFormatter;
import org.apache.logging.log4j.core.pattern.PatternParser;
import org.apache.logging.log4j.core.script.ScriptBindings;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.script.AbstractScript;
import org.apache.logging.log4j.script.ScriptManagerImpl;
import org.apache.logging.log4j.script.ScriptRef;
import org.apache.logging.log4j.status.StatusLogger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Selects the pattern to use based on the result of executing a Script. The returned value will be used as the "key"
 * to choose between one of the configured patterns. If no key is returned or there is no match the default
 * pattern will be used.
 */
@Configurable(elementType = PatternSelector.ELEMENT_TYPE, printObject = true)
@Plugin
public class ScriptPatternSelector implements PatternSelector {

    /**
     * Custom ScriptPatternSelector builder. Use the {@link #newBuilder() builder factory method} to create this.
     */
    public static class Builder implements org.apache.logging.log4j.plugins.util.Builder<ScriptPatternSelector> {

        @PluginElement("Script")
        private AbstractScript script;

        @PluginElement("PatternMatch")
        private PatternMatch[] properties;

        @PluginBuilderAttribute("defaultPattern")
        private String defaultPattern;

        @PluginBuilderAttribute("alwaysWriteExceptions")
        private boolean alwaysWriteExceptions = true;

        @PluginBuilderAttribute("disableAnsi")
        private boolean disableAnsi;

        @PluginBuilderAttribute("noConsoleNoAnsi")
        private boolean noConsoleNoAnsi;

        @PluginConfiguration
        private Configuration configuration;

        private Builder() {
            // nothing
        }

        @Override
        public ScriptPatternSelector build() {
            if (script == null) {
                LOGGER.error("A Script, ScriptFile or ScriptRef element must be provided for this ScriptFilter");
                return null;
            }
            if (configuration.getScriptManager() == null) {
                LOGGER.error("Script support is not enabled");
                return null;
            }
            if (script instanceof ScriptRef) {
                if (configuration.getScriptManager().getScript(script.getName()) == null) {
                    LOGGER.error("No script with name {} has been declared.", script.getName());
                    return null;
                }
            } else {
                if (!configuration.getScriptManager().addScript(script)) {
                    return null;
                }
            }
            if (defaultPattern == null) {
                defaultPattern = PatternLayout.DEFAULT_CONVERSION_PATTERN;
            }
            if (properties == null || properties.length == 0) {
                LOGGER.warn("No marker patterns were provided");
                return null;
            }
            return new ScriptPatternSelector(script, properties, defaultPattern, alwaysWriteExceptions, disableAnsi,
                    noConsoleNoAnsi, configuration);
        }

        public Builder setScript(final AbstractScript script) {
            this.script = script;
            return this;
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

        public Builder setConfiguration(final Configuration config) {
            this.configuration = config;
            return this;
        }
    }

    private final Map<String, PatternFormatter[]> formatterMap = new HashMap<>();

    private final Map<String, String> patternMap = new HashMap<>();

    private final PatternFormatter[] defaultFormatters;

    private final String defaultPattern;

    private static final Logger LOGGER = StatusLogger.getLogger();
    private final AbstractScript script;
    private final Configuration configuration;
    private final boolean requiresLocation;

    private ScriptPatternSelector(final AbstractScript script, final PatternMatch[] properties, final String defaultPattern,
                                 final boolean alwaysWriteExceptions, final boolean disableAnsi,
                                 final boolean noConsoleNoAnsi, final Configuration config) {
        this.script = script;
        this.configuration = config;
        final PatternParser parser = PatternLayout.createPatternParser(config);
        boolean needsLocation = false;
        for (final PatternMatch property : properties) {
            try {
                final List<PatternFormatter> list = parser.parse(property.getPattern(), alwaysWriteExceptions, disableAnsi, noConsoleNoAnsi);
                final PatternFormatter[] formatters = list.toArray(new PatternFormatter[list.size()]);
                formatterMap.put(property.getKey(), formatters);
                patternMap.put(property.getKey(), property.getPattern());
                for (int i = 0; !needsLocation && i < formatters.length; ++i) {
                    needsLocation = formatters[i].requiresLocation();
                }
            } catch (final RuntimeException ex) {
                throw new IllegalArgumentException("Cannot parse pattern '" + property.getPattern() + "'", ex);
            }
        }
        try {
            final List<PatternFormatter> list = parser.parse(defaultPattern, alwaysWriteExceptions, disableAnsi, noConsoleNoAnsi);
            defaultFormatters = list.toArray(new PatternFormatter[list.size()]);
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
        final ScriptBindings bindings = ScriptManagerImpl.createBindings();
        bindings.putAll(configuration.getProperties());
        bindings.put("substitutor", configuration.getStrSubstitutor());
        bindings.put("logEvent", event);
        final Object object = configuration.getScriptManager().execute(script.getName(), bindings);
        if (object == null) {
            return defaultFormatters;
        }
        final PatternFormatter[] patternFormatter = formatterMap.get(object.toString());

        return patternFormatter == null ? defaultFormatters : patternFormatter;
    }


    /**
     * Creates a builder for a custom ScriptPatternSelector.
     *
     * @return a ScriptPatternSelector builder.
     */
    @PluginFactory
    public static Builder newBuilder() {
        return new Builder();
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
