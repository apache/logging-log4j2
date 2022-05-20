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
package org.apache.logging.log4j.jdbc.appender;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.jdbc.appender.util.JndiUtil;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * A {@link JdbcAppender} connection source that uses a {@link DataSource} to connect to the database.
 */
@Configurable(elementType = "connectionSource", printObject = true)
@Plugin("DataSource")
public final class DataSourceConnectionSource extends AbstractConnectionSource {
    private static final Logger LOGGER = StatusLogger.getLogger();

    private final DataSource dataSource;
    private final String description;

    private DataSourceConnectionSource(final String dataSourceName, final DataSource dataSource) {
        this.dataSource = dataSource;
        this.description = "dataSource{ name=" + dataSourceName + ", value=" + dataSource + " }";
    }

    @Override
    public Connection getConnection() throws SQLException {
        return this.dataSource.getConnection();
    }

    @Override
    public String toString() {
        return this.description;
    }

    /**
     * Factory method for creating a connection source within the plugin manager.
     *
     * @param jndiName The full JNDI path where the data source is bound. Should start with java:/comp/env or
     *                 environment-equivalent.
     * @return the created connection source.
     */
    @PluginFactory
    public static DataSourceConnectionSource createConnectionSource(@PluginAttribute final String jndiName) {
        if (!Constants.JNDI_JDBC_ENABLED) {
            LOGGER.error("JNDI must be enabled by setting log4j2.enableJndiJdbc=true");
            return null;
        }
        if (Strings.isEmpty(jndiName)) {
            LOGGER.error("No JNDI name provided.");
            return null;
        }

        final DataSource dataSource = JndiUtil.getDataSource(jndiName);
        if (dataSource == null) {
            return null;
        }

        return new DataSourceConnectionSource(jndiName, dataSource);
    }
}
