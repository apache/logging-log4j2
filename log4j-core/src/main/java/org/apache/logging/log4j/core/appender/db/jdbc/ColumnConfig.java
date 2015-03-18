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
package org.apache.logging.log4j.core.appender.db.jdbc;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.Booleans;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;

/**
 * A configuration element used to configure which event properties are logged to which columns in the database table.
 */
@Plugin(name = "Column", category = "Core", printObject = true)
public final class ColumnConfig {
    private static final Logger LOGGER = StatusLogger.getLogger();

    private final String columnName;
    private final PatternLayout layout;
    private final String literalValue;
    private final boolean eventTimestamp;
    private final boolean unicode;
    private final boolean clob;

    private ColumnConfig(final String columnName, final PatternLayout layout, final String literalValue,
                         final boolean eventDate, final boolean unicode, final boolean clob) {
        this.columnName = columnName;
        this.layout = layout;
        this.literalValue = literalValue;
        this.eventTimestamp = eventDate;
        this.unicode = unicode;
        this.clob = clob;
    }

    public String getColumnName() {
        return this.columnName;
    }

    public PatternLayout getLayout() {
        return this.layout;
    }

    public String getLiteralValue() {
        return this.literalValue;
    }

    public boolean isEventTimestamp() {
        return this.eventTimestamp;
    }

    public boolean isUnicode() {
        return this.unicode;
    }

    public boolean isClob() {
        return this.clob;
    }

    @Override
    public String toString() {
        return "{ name=" + this.columnName + ", layout=" + this.layout + ", literal=" + this.literalValue
                + ", timestamp=" + this.eventTimestamp + " }";
    }

    /**
     * Factory method for creating a column config within the plugin manager.
     *
     * @param config The configuration object
     * @param name The name of the database column as it exists within the database table.
     * @param pattern The {@link PatternLayout} pattern to insert in this column. Mutually exclusive with
     *                {@code literalValue!=null} and {@code eventTimestamp=true}
     * @param literalValue The literal value to insert into the column as-is without any quoting or escaping. Mutually
     *                     exclusive with {@code pattern!=null} and {@code eventTimestamp=true}.
     * @param eventTimestamp If {@code "true"}, indicates that this column is a date-time column in which the event
     *                       timestamp should be inserted. Mutually exclusive with {@code pattern!=null} and
     *                       {@code literalValue!=null}.
     * @param unicode If {@code "true"}, indicates that the column is a Unicode String.
     * @param clob If {@code "true"}, indicates that the column is a character LOB (CLOB).
     * @return the created column config.
     */
    @PluginFactory
    public static ColumnConfig createColumnConfig(
            @PluginConfiguration final Configuration config,
            @PluginAttribute("name") final String name,
            @PluginAttribute("pattern") final String pattern,
            @PluginAttribute("literal") final String literalValue,
            @PluginAttribute("isEventTimestamp") final String eventTimestamp,
            @PluginAttribute("isUnicode") final String unicode,
            @PluginAttribute("isClob") final String clob) {
        if (Strings.isEmpty(name)) {
            LOGGER.error("The column config is not valid because it does not contain a column name.");
            return null;
        }

        final boolean isPattern = Strings.isNotEmpty(pattern);
        final boolean isLiteralValue = Strings.isNotEmpty(literalValue);
        final boolean isEventTimestamp = Boolean.parseBoolean(eventTimestamp);
        final boolean isUnicode = Booleans.parseBoolean(unicode, true);
        final boolean isClob = Boolean.parseBoolean(clob);

        if ((isPattern && isLiteralValue) || (isPattern && isEventTimestamp) || (isLiteralValue && isEventTimestamp)) {
            LOGGER.error("The pattern, literal, and isEventTimestamp attributes are mutually exclusive.");
            return null;
        }

        if (isEventTimestamp) {
            return new ColumnConfig(name, null, null, true, false, false);
        }
        if (isLiteralValue) {
            return new ColumnConfig(name, null, literalValue, false, false, false);
        }
        if (isPattern) {
            final PatternLayout layout =
                PatternLayout.newBuilder()
                    .withPattern(pattern)
                    .withConfiguration(config)
                    .withAlwaysWriteExceptions(false)
                    .build();
            return new ColumnConfig(name, layout, null, false, isUnicode, isClob);
        }

        LOGGER.error("To configure a column you must specify a pattern or literal or set isEventDate to true.");
        return null;
    }
}
