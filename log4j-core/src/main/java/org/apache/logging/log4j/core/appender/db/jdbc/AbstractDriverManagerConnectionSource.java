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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * A {@link ConnectionSource} that uses a JDBC connection string, a user name, and a password to call
 * {@link DriverManager#getConnection(String, String, String)}.
 * <p>
 * This plugin does not provide any connection pooling unless it is available through the connection string and driver
 * itself. This handy to get you off the ground without having to deal with JNDI.
 * </p>
 */
public class AbstractDriverManagerConnectionSource extends AbstractConnectionSource {

    /**
     * Builds DriverManagerConnectionSource instances.
     *
     * @param <B>
     *            This builder type or a subclass.
     */
    public static class Builder<B extends Builder<B>> {

        @PluginBuilderAttribute
        @Required
        protected String connectionString;

        @PluginBuilderAttribute
        protected String driverClassName;

        @PluginBuilderAttribute
        protected char[] password;

        @PluginElement("Properties")
        protected Property[] properties;

        @PluginBuilderAttribute
        protected char[] userName;

        @SuppressWarnings("unchecked")
        protected B asBuilder() {
            return (B) this;
        }

        public String getConnectionString() {
            return connectionString;
        }

        public String getDriverClassName() {
            return driverClassName;
        }

        public char[] getPassword() {
            return password;
        }

        public Property[] getProperties() {
            return properties;
        }

        public char[] getUserName() {
            return userName;
        }

        public B setConnectionString(final String connectionString) {
            this.connectionString = connectionString;
            return asBuilder();
        }

        public B setDriverClassName(final String driverClassName) {
            this.driverClassName = driverClassName;
            return asBuilder();
        }

        public B setPassword(final char[] password) {
            this.password = password;
            return asBuilder();
        }

        public B setProperties(final Property[] properties) {
            this.properties = properties;
            return asBuilder();
        }

        public B setUserName(final char[] userName) {
            this.userName = userName;
            return asBuilder();
        }
    }

    private static final Logger LOGGER = StatusLogger.getLogger();

    public static Logger getLogger() {
        return LOGGER;
    }

    private final String actualConnectionString;
    private final String connectionString;
    private final String driverClassName;
    private final char[] password;
    private final Property[] properties;
    private final char[] userName;

    public AbstractDriverManagerConnectionSource(
            final String driverClassName,
            final String connectionString,
            final String actualConnectionString,
            final char[] userName,
            final char[] password,
            final Property[] properties) {
        this.driverClassName = driverClassName;
        this.connectionString = connectionString;
        this.actualConnectionString = actualConnectionString;
        this.userName = userName;
        this.password = password;
        this.properties = properties;
    }

    public String getActualConnectionString() {
        return actualConnectionString;
    }

    @SuppressWarnings("resource") // The JDBC Connection is freed when the connection source is stopped.
    @Override
    public Connection getConnection() throws SQLException {
        loadDriver();
        final String actualConnectionString = getActualConnectionString();
        LOGGER.debug("{} getting connection for '{}'", getClass().getSimpleName(), actualConnectionString);
        Connection connection;
        if (properties != null && properties.length > 0) {
            if (userName != null || password != null) {
                throw new SQLException("Either set the userName and password, or set the Properties, but not both.");
            }
            connection = DriverManager.getConnection(actualConnectionString, toProperties(properties));
        } else {
            connection = DriverManager.getConnection(actualConnectionString, toString(userName), toString(password));
        }
        LOGGER.debug(
                "{} acquired connection for '{}': {} ({}{@})",
                getClass().getSimpleName(),
                actualConnectionString,
                connection,
                connection.getClass().getName(),
                Integer.toHexString(connection.hashCode()));
        return connection;
    }

    public String getConnectionString() {
        return connectionString;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public char[] getPassword() {
        return password;
    }

    public Property[] getProperties() {
        return properties;
    }

    public char[] getUserName() {
        return userName;
    }

    protected void loadDriver() throws SQLException {
        loadDriver(driverClassName);
    }

    /**
     * Loads a JDBC driver for the given class name
     *
     * @param className
     *            the fully-qualified class name for a JDBC Driver.
     * @throws SQLException
     *             thrown when loading the driver throws an exception.
     */
    protected void loadDriver(final String className) throws SQLException {
        if (className != null) {
            // Hack for old JDBC drivers.
            LOGGER.debug("Loading driver class {}", className);
            try {
                Class.forName(className);
            } catch (final Exception e) {
                throw new SQLException(
                        String.format(
                                "The %s could not load the JDBC driver %s: %s",
                                getClass().getSimpleName(), className, e.toString()),
                        e);
            }
        }
    }

    protected Properties toProperties(final Property[] properties) {
        final Properties props = new Properties();
        for (final Property property : properties) {
            props.setProperty(property.getName(), property.getValue());
        }
        return props;
    }

    @Override
    public String toString() {
        return this.connectionString;
    }

    protected String toString(final char[] value) {
        return value == null ? null : String.valueOf(value);
    }
}
