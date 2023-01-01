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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.impl.ContextDataFactory;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Factory;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.StringMap;

/**
 * This policy modifies events by replacing or possibly adding keys and values to the MapMessage.
 */
@Configurable(elementType = "rewritePolicy", printObject = true)
@Plugin
public final class PropertiesRewritePolicy implements RewritePolicy {

    /**
     * Allows subclasses access to the status logger without creating another instance.
     */
    protected static final Logger LOGGER = StatusLogger.getLogger();

    private final ContextDataFactory contextDataFactory;
    private final StrSubstitutor strSubstitutor;
    private final Map<Property, Boolean> properties;


    private PropertiesRewritePolicy(final ContextDataFactory contextDataFactory, final StrSubstitutor strSubstitutor,
                                    final List<Property> props) {
        this.contextDataFactory = contextDataFactory;
        this.strSubstitutor = strSubstitutor;
        this.properties = new HashMap<>(props.size());
        for (final Property property : props) {
            final boolean interpolate = property.getValue().contains("${");
            properties.put(property, interpolate);
        }
    }

    /**
     * Rewrites the event.
     * @param source a logging event that may be returned or
     * used to create a new logging event.
     * @return The LogEvent after rewriting.
     */
    @Override
    public LogEvent rewrite(final LogEvent source) {
        final StringMap newContextData = contextDataFactory.createContextData(source.getContextData());
        for (final Map.Entry<Property, Boolean> entry : properties.entrySet()) {
            final Property prop = entry.getKey();
            newContextData.putValue(prop.getName(), entry.getValue() ?
                strSubstitutor.replace(prop.getValue()) : prop.getValue());
        }

        return LogEvent.builderFrom(source)
                .setContextData(newContextData)
                .build();
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

    @Factory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Supplier<PropertiesRewritePolicy> {
        private ContextDataFactory contextDataFactory;
        private StrSubstitutor strSubstitutor;
        private List<Property> properties;

        public ContextDataFactory getContextDataFactory() {
            return contextDataFactory;
        }

        @Inject
        public Builder setContextDataFactory(final ContextDataFactory contextDataFactory) {
            this.contextDataFactory = contextDataFactory;
            return this;
        }

        public StrSubstitutor getStrSubstitutor() {
            return strSubstitutor;
        }

        @Inject
        public Builder setStrSubstitutor(final StrSubstitutor strSubstitutor) {
            this.strSubstitutor = strSubstitutor;
            return this;
        }

        public List<Property> getProperties() {
            return properties;
        }

        public Builder setProperties(@PluginElement("Properties") final Property[] props) {
            properties = List.of(props);
            return this;
        }

        @Override
        public PropertiesRewritePolicy get() {
            return new PropertiesRewritePolicy(contextDataFactory, strSubstitutor, properties);
        }
    }
}
