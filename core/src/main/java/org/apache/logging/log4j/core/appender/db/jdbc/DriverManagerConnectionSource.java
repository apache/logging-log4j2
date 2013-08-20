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

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.helpers.NameUtil;
import org.apache.logging.log4j.core.helpers.Strings;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * A {@link JDBCAppender} connection source that uses a standard JDBC URL, username, and password to connect to the
 * database.
 */
@Plugin(name = "DriverManager", category = "Core", elementType = "connectionSource", printObject = true)
public final class DriverManagerConnectionSource implements ConnectionSource {
    private static final Logger LOGGER = StatusLogger.getLogger();

    private final String databasePassword;
    private final String databaseUrl;
    private final String databaseUsername;
    private final String description;

    private DriverManagerConnectionSource(final String databaseUrl, final String databaseUsername,
                                          final String databasePassword) {
        this.databaseUrl = databaseUrl;
        this.databaseUsername = databaseUsername;
        this.databasePassword = databasePassword;
        this.description = "driverManager{ url=" + this.databaseUrl + ", username=" + this.databaseUsername
                + ", passwordHash=" + NameUtil.md5(this.databasePassword + this.getClass().getName()) + " }";
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (this.databaseUsername == null) {
            return DriverManager.getConnection(this.databaseUrl);
        }
        return DriverManager.getConnection(this.databaseUrl, this.databaseUsername, this.databasePassword);
    }

    @Override
    public String toString() {
        return this.description;
    }

    /**
     * Factory method for creating a connection source within the plugin manager.
     *
     * @param url The JDBC URL to use to connect to the logging database. A driver that can accept this URL must be on
     *            the classpath.
     * @param username The username with which to log in to the database, if applicable.
     * @param password The password with which to log in to the database, if applicable.
     * @return the created connection source.
     */
    @PluginFactory
    public static DriverManagerConnectionSource createConnectionSource(
            @PluginAttribute("url") final String url,
            @PluginAttribute("username") String username,
            @PluginAttribute("password") String password) {
        if (Strings.isEmpty(url)) {
            LOGGER.error("No JDBC URL specified for the database.");
            return null;
        }

        Driver driver;
        try {
            driver = DriverManager.getDriver(url);
        } catch (final SQLException e) {
            LOGGER.error("No matching driver found for database URL [" + url + "].", e);
            return null;
        }

        if (driver == null) {
            LOGGER.error("No matching driver found for database URL [" + url + "].");
            return null;
        }

        if (username == null || username.trim().isEmpty()) {
            username = null;
            password = null;
        }

        return new DriverManagerConnectionSource(url, username, password);
    }
}
