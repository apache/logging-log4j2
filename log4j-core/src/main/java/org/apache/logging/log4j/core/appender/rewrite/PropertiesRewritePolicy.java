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
package org.apache.logging.log4j.core.appender.rewrite;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * This policy modifies events by replacing or possibly adding keys and values to the MapMessage.
 */
@Plugin(name = "PropertiesRewritePolicy", category = "Core", elementType = "rewritePolicy", printObject = true)
public final class PropertiesRewritePolicy implements RewritePolicy {
    /**
     * Allow subclasses access to the status logger without creating another instance.
     */
    protected static final Logger LOGGER = StatusLogger.getLogger();

    private final Map<Property, Boolean> properties;

    private final Configuration config;

    private PropertiesRewritePolicy(final Configuration config, final List<Property> props) {
        this.config = config;
        this.properties = new HashMap<Property, Boolean>(props.size());
        for (final Property property : props) {
            final Boolean interpolate = Boolean.valueOf(property.getValue().contains("${"));
            properties.put(property, interpolate);
        }
    }

    /**
     * Rewrite the event.
     * @param source a logging event that may be returned or
     * used to create a new logging event.
     * @return The LogEvent after rewriting.
     */
    @Override
    public LogEvent rewrite(final LogEvent source) {
        final Map<String, String> props = new HashMap<String, String>(source.getContextMap());
        for (final Map.Entry<Property, Boolean> entry : properties.entrySet()) {
            final Property prop = entry.getKey();
            props.put(prop.getName(), entry.getValue().booleanValue() ?
                config.getStrSubstitutor().replace(prop.getValue()) : prop.getValue());
        }

        return new Log4jLogEvent(source.getLoggerName(), source.getMarker(), source.getLoggerFQCN(), source.getLevel(),
            source.getMessage(), source.getThrown(), props, source.getContextStack(), source.getThreadName(),
            source.getSource(), source.getTimeMillis());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(" {");
        boolean first = true;
        for (final Map.Entry<Property, Boolean> entry : properties.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            final Property prop = entry.getKey();
            sb.append(prop.getName()).append('=').append(prop.getValue());
            first = false;
        }
        sb.append('}');
        return sb.toString();
    }

    /**
     * The factory method to create the PropertiesRewritePolicy.
     * @param config The Configuration.
     * @param props key/value pairs for the new keys and values.
     * @return The PropertiesRewritePolicy.
     */
    @PluginFactory
    public static PropertiesRewritePolicy createPolicy(@PluginConfiguration final Configuration config,
                                                @PluginElement("Properties") final Property[] props) {
        if (props == null || props.length == 0) {
            LOGGER.error("Properties must be specified for the PropertiesRewritePolicy");
            return null;
        }
        final List<Property> properties = Arrays.asList(props);
        return new PropertiesRewritePolicy(config, properties);
    }
}
