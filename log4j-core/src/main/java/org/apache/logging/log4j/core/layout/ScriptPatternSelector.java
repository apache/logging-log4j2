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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.SimpleBindings;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.pattern.PatternFormatter;
import org.apache.logging.log4j.core.pattern.PatternParser;
import org.apache.logging.log4j.core.script.AbstractScript;
import org.apache.logging.log4j.core.script.ScriptRef;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Selects the pattern to use based on the Marker in the LogEvent.
 */
@Plugin(name = "ScriptPatternSelector", category = Node.CATEGORY, elementType = PatternSelector.ELEMENT_TYPE, printObject = true)
public class ScriptPatternSelector implements PatternSelector {

    /**
     * Custom ScriptPatternSelector builder. Use the {@link #newBuilder() builder factory method} to create this.
     */
    public static class Builder implements org.apache.logging.log4j.core.util.Builder<ScriptPatternSelector> {

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
            if (script instanceof ScriptRef) {
                if (configuration.getScriptManager().getScript(script.getName()) == null) {
                    LOGGER.error("No script with name {} has been declared.", script.getName());
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

    private static Logger LOGGER = StatusLogger.getLogger();
    private final AbstractScript script;
    private final Configuration configuration;


    /**
     * @deprecated Use {@link #newBuilder()} instead. This will be private in a future version.
     */
    @Deprecated
    public ScriptPatternSelector(final AbstractScript script, final PatternMatch[] properties, final String defaultPattern,
                                 final boolean alwaysWriteExceptions, final boolean disableAnsi,
                                 final boolean noConsoleNoAnsi, final Configuration config) {
        this.script = script;
        this.configuration = config;
        if (!(script instanceof ScriptRef)) {
            config.getScriptManager().addScript(script);
        }
        final PatternParser parser = PatternLayout.createPatternParser(config);
        for (final PatternMatch property : properties) {
            try {
                final List<PatternFormatter> list = parser.parse(property.getPattern(), alwaysWriteExceptions, disableAnsi, noConsoleNoAnsi);
                formatterMap.put(property.getKey(), list.toArray(new PatternFormatter[list.size()]));
                patternMap.put(property.getKey(), property.getPattern());
            } catch (final RuntimeException ex) {
                throw new IllegalArgumentException("Cannot parse pattern '" + property.getPattern() + "'", ex);
            }
        }
        try {
            final List<PatternFormatter> list = parser.parse(defaultPattern, alwaysWriteExceptions, disableAnsi, noConsoleNoAnsi);
            defaultFormatters = list.toArray(new PatternFormatter[list.size()]);
            this.defaultPattern = defaultPattern;
        } catch (final RuntimeException ex) {
            throw new IllegalArgumentException("Cannot parse pattern '" + defaultPattern + "'", ex);
        }
    }

    @Override
    public PatternFormatter[] getFormatters(final LogEvent event) {
        final SimpleBindings bindings = new SimpleBindings();
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
    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Deprecated, use {@link #newBuilder()} instead.
     * 
     * @param script
     * @param properties
     * @param defaultPattern
     * @param alwaysWriteExceptions
     * @param noConsoleNoAnsi
     * @param configuration
     * @return a new ScriptPatternSelector
     * @deprecated Use {@link #newBuilder()} instead.
     */
    @Deprecated
    public static ScriptPatternSelector createSelector(
            final AbstractScript script,
            final PatternMatch[] properties,
            final String defaultPattern,
            final boolean alwaysWriteExceptions,
            final boolean noConsoleNoAnsi,
            final Configuration configuration) {
        final Builder builder = newBuilder();
        builder.setScript(script);
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
