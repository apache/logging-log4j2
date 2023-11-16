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
package org.apache.logging.log4j.core.appender.db.jdbc;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.appender.db.ColumnMapping;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.Booleans;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;

/**
 * A configuration element used to configure which event properties are logged to which columns in the database table.
 *
 * @see ColumnMapping
 */
@Plugin(name = "Column", category = Core.CATEGORY_NAME, printObject = true)
public final class ColumnConfig {
    public static class Builder implements org.apache.logging.log4j.core.util.Builder<ColumnConfig> {

        @PluginConfiguration
        private Configuration configuration;

        @PluginBuilderAttribute
        @Required(message = "No name provided")
        private String name;

        @PluginBuilderAttribute
        private String pattern;

        @PluginBuilderAttribute
        private String literal;

        @PluginBuilderAttribute
        private boolean isEventTimestamp;

        @PluginBuilderAttribute
        private boolean isUnicode = true;

        @PluginBuilderAttribute
        private boolean isClob;

        @Override
        public ColumnConfig build() {
            if (Strings.isEmpty(name)) {
                LOGGER.error("The column config is not valid because it does not contain a column name.");
                return null;
            }

            final boolean isPattern = Strings.isNotEmpty(pattern);
            final boolean isLiteralValue = Strings.isNotEmpty(literal);

            if ((isPattern && isLiteralValue)
                    || (isPattern && isEventTimestamp)
                    || (isLiteralValue && isEventTimestamp)) {
                LOGGER.error("The pattern, literal, and isEventTimestamp attributes are mutually exclusive.");
                return null;
            }

            if (isEventTimestamp) {
                return new ColumnConfig(name, null, null, true, false, false);
            }

            if (isLiteralValue) {
                return new ColumnConfig(name, null, literal, false, false, false);
            }

            if (isPattern) {
                final PatternLayout layout = PatternLayout.newBuilder()
                        .withPattern(pattern)
                        .withConfiguration(configuration)
                        .withAlwaysWriteExceptions(false)
                        .build();
                return new ColumnConfig(name, layout, null, false, isUnicode, isClob);
            }

            LOGGER.error("To configure a column you must specify a pattern or literal or set isEventDate to true.");
            return null;
        }

        /**
         * If {@code "true"}, indicates that the column is a character LOB (CLOB).
         *
         * @return this.
         */
        public Builder setClob(final boolean clob) {
            isClob = clob;
            return this;
        }

        /**
         * The configuration object.
         *
         * @return this.
         */
        public Builder setConfiguration(final Configuration configuration) {
            this.configuration = configuration;
            return this;
        }

        /**
         * If {@code "true"}, indicates that this column is a date-time column in which the event timestamp should be
         * inserted. Mutually exclusive with {@code pattern!=null} and {@code literal!=null}.
         *
         * @return this.
         */
        public Builder setEventTimestamp(final boolean eventTimestamp) {
            isEventTimestamp = eventTimestamp;
            return this;
        }

        /**
         * The literal value to insert into the column as-is without any quoting or escaping. Mutually exclusive with
         * {@code pattern!=null} and {@code eventTimestamp=true}.
         *
         * @return this.
         */
        public Builder setLiteral(final String literal) {
            this.literal = literal;
            return this;
        }

        /**
         * The name of the database column as it exists within the database table.
         *
         * @return this.
         */
        public Builder setName(final String name) {
            this.name = name;
            return this;
        }

        /**
         * The {@link PatternLayout} pattern to insert in this column. Mutually exclusive with
         * {@code literal!=null} and {@code eventTimestamp=true}
         *
         * @return this.
         */
        public Builder setPattern(final String pattern) {
            this.pattern = pattern;
            return this;
        }

        /**
         * If {@code "true"}, indicates that the column is a Unicode String.
         *
         * @return this.
         */
        public Builder setUnicode(final boolean unicode) {
            isUnicode = unicode;
            return this;
        }
    }

    private static final Logger LOGGER = StatusLogger.getLogger();
    /**
     * Factory method for creating a column config within the plugin manager.
     *
     * @see Builder
     * @deprecated use {@link #newBuilder()}
     */
    @Deprecated
    public static ColumnConfig createColumnConfig(
            final Configuration config,
            final String name,
            final String pattern,
            final String literalValue,
            final String eventTimestamp,
            final String unicode,
            final String clob) {
        if (Strings.isEmpty(name)) {
            LOGGER.error("The column config is not valid because it does not contain a column name.");
            return null;
        }

        final boolean isEventTimestamp = Boolean.parseBoolean(eventTimestamp);
        final boolean isUnicode = Booleans.parseBoolean(unicode, true);
        final boolean isClob = Boolean.parseBoolean(clob);

        return newBuilder()
                .setConfiguration(config)
                .setName(name)
                .setPattern(pattern)
                .setLiteral(literalValue)
                .setEventTimestamp(isEventTimestamp)
                .setUnicode(isUnicode)
                .setClob(isClob)
                .build();
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    private final String columnName;
    private final String columnNameKey;
    private final PatternLayout layout;
    private final String literalValue;

    private final boolean eventTimestamp;

    private final boolean unicode;

    private final boolean clob;

    private ColumnConfig(
            final String columnName,
            final PatternLayout layout,
            final String literalValue,
            final boolean eventDate,
            final boolean unicode,
            final boolean clob) {
        this.columnName = columnName;
        this.columnNameKey = ColumnMapping.toKey(columnName);
        this.layout = layout;
        this.literalValue = literalValue;
        this.eventTimestamp = eventDate;
        this.unicode = unicode;
        this.clob = clob;
    }

    public String getColumnName() {
        return this.columnName;
    }

    public String getColumnNameKey() {
        return this.columnNameKey;
    }

    public PatternLayout getLayout() {
        return this.layout;
    }

    public String getLiteralValue() {
        return this.literalValue;
    }

    public boolean isClob() {
        return this.clob;
    }

    public boolean isEventTimestamp() {
        return this.eventTimestamp;
    }

    public boolean isUnicode() {
        return this.unicode;
    }

    @Override
    public String toString() {
        return "{ name=" + this.columnName + ", layout=" + this.layout + ", literal=" + this.literalValue
                + ", timestamp=" + this.eventTimestamp + " }";
    }
}
