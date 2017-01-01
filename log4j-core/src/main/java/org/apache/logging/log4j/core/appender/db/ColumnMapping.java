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
package org.apache.logging.log4j.core.appender.db;

import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.StringLayout;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.spi.ThreadContextMap;
import org.apache.logging.log4j.spi.ThreadContextStack;

/**
 * A configuration element for specifying a database column name mapping.
 *
 * @since 2.8
 */
@Plugin(name = "ColumnMapping", category = Core.CATEGORY_NAME, printObject = true)
public class ColumnMapping {

    /**
     * Column name.
     */
    private final String name;
    /**
     * Layout of value to write to database (before type conversion). Not applicable if {@link #type} is a collection.
     */
    private final StringLayout layout;
    /**
     * Class to convert value to before storing in database. If the type is a {@link ThreadContextMap}, then the
     * MDC will be used. If the type is a {@link ThreadContextStack}, then the NDC will be used.
     */
    private final Class<?> type;

    private ColumnMapping(final String name, final StringLayout layout, final Class<?> type) {
        this.name = name;
        this.layout = layout;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public StringLayout getLayout() {
        return layout;
    }

    public Class<?> getType() {
        return type;
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<ColumnMapping> {

        @PluginBuilderAttribute
        @Required(message = "No column name provided")
        private String name;

        @PluginElement("Layout")
        private StringLayout layout;

        @PluginBuilderAttribute
        private String pattern;

        @PluginBuilderAttribute
        @Required(message = "No conversion type provided")
        private Class<?> type = String.class;

        @PluginConfiguration
        private Configuration configuration;

        public Builder setName(final String name) {
            this.name = name;
            return this;
        }

        public Builder setLayout(final StringLayout layout) {
            this.layout = layout;
            return this;
        }

        public Builder setPattern(final String pattern) {
            this.pattern = pattern;
            return this;
        }

        public Builder setType(final Class<?> type) {
            this.type = type;
            return this;
        }

        public Builder setConfiguration(final Configuration configuration) {
            this.configuration = configuration;
            return this;
        }

        @Override
        public ColumnMapping build() {
            if (pattern != null) {
                layout = PatternLayout.newBuilder()
                    .withPattern(pattern)
                    .withConfiguration(configuration)
                    .build();
            }
            if (!(layout != null
                || ThreadContextMap.class.isAssignableFrom(type)
                || ThreadContextStack.class.isAssignableFrom(type))) {
                throw new IllegalStateException(
                    "No layout specified and type (" + type + ") is not compatible with ThreadContextMap or ThreadContextStack");
            }
            return new ColumnMapping(name, layout, type);
        }
    }
}
