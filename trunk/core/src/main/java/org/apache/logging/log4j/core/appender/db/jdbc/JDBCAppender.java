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

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.appender.db.AbstractDatabaseAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 * This Appender writes logging events to a relational database using standard JDBC mechanisms. It takes a list of
 * {@link ColumnConfig}s with which it determines how to save the event data into the appropriate columns in the table.
 * A {@link ConnectionSource} plugin instance instructs the appender (and {@link JDBCDatabaseManager}) how to connect to
 * the database. This appender can be reconfigured at run time.
 * 
 * @see ColumnConfig
 * @see ConnectionSource
 */
@Plugin(name = "Jdbc", category = "Core", elementType = "appender", printObject = true)
public final class JDBCAppender extends AbstractDatabaseAppender<JDBCDatabaseManager> {
    private final String description;

    private JDBCAppender(final String name, final Filter filter, final boolean handleException,
                         final JDBCDatabaseManager manager) {
        super(name, filter, handleException, manager);
        this.description = this.getName() + "{ manager=" + this.getManager() + " }";
    }

    @Override
    public String toString() {
        return this.description;
    }

    /**
     * Factory method for creating a JDBC appender within the plugin manager.
     *
     * @param name The name of the appender.
     * @param suppressExceptions {@code "true"} (default) if logging exceptions should be hidden from the application,
     *                           {@code "false"} otherwise.
     * @param filter The filter, if any, to use.
     * @param connectionSource The connections source from which database connections should be retrieved.
     * @param bufferSize If an integer greater than 0, this causes the appender to buffer log events and flush whenever
     *                   the buffer reaches this size.
     * @param tableName The name of the database table to insert log events into.
     * @param columnConfigs Information about the columns that log event data should be inserted into and how to insert
     *                      that data.
     * @return a new JDBC appender.
     */
    @PluginFactory
    public static JDBCAppender createAppender(@PluginAttr("name") final String name,
                                              @PluginAttr("suppressExceptions") final String suppressExceptions,
                                              @PluginElement("filter") final Filter filter,
                                              @PluginElement("connectionSource") final ConnectionSource
                                                      connectionSource,
                                              @PluginAttr("bufferSize") final String bufferSize,
                                              @PluginAttr("tableName") final String tableName,
                                              @PluginElement("columnConfigs") final ColumnConfig[] columnConfigs) {
        int bufferSizeInt;
        try {
            bufferSizeInt = bufferSize == null || bufferSize.length() == 0 ? 0 : Integer.parseInt(bufferSize);
        } catch (final NumberFormatException e) {
            LOGGER.warn("Buffer size [" + bufferSize + "] not an integer, using no buffer.");
            bufferSizeInt = 0;
        }

        final boolean handleExceptions = suppressExceptions == null || !Boolean.parseBoolean(suppressExceptions);

        final StringBuilder managerName = new StringBuilder("jdbcManager{ description=").append(name)
                .append(", bufferSize=").append(bufferSizeInt).append(", connectionSource=")
                .append(connectionSource.toString()).append(", tableName=").append(tableName).append(", columns=[ ");

        int i = 0;
        for (final ColumnConfig column : columnConfigs) {
            if (i++ > 0) {
                managerName.append(", ");
            }
            managerName.append(column.toString());
        }

        managerName.append(" ] }");

        final JDBCDatabaseManager manager = JDBCDatabaseManager.getJDBCDatabaseManager(
                managerName.toString(), bufferSizeInt, connectionSource, tableName, columnConfigs
        );
        if (manager == null) {
            return null;
        }

        return new JDBCAppender(name, filter, handleExceptions, manager);
    }
}
