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
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
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
@Plugin(name = "DriverManager", category = Core.CATEGORY_NAME, elementType = "connectionSource", printObject = true)
public class DriverManagerConnectionSource implements ConnectionSource {

    /**
     * Builds DriverManagerConnectionSource instances.
     *
     * @param <B>
     *            This builder type or a subclass.
     * @param <T>
     *            The type to build.
     */
    public static class Builder<B extends Builder<B, T>, T extends DriverManagerConnectionSource>
            implements org.apache.logging.log4j.core.util.Builder<T> {

        @PluginBuilderAttribute
        @Required
        private String connectionString;

        @PluginBuilderAttribute
        private String driverClassName;

        @PluginBuilderAttribute
        private char[] password;

        @PluginElement("Properties")
        private Property[] properties;

        @PluginBuilderAttribute
        private char[] userName;

        @SuppressWarnings("unchecked")
        protected B asBuilder() {
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        @Override
        public T build() {
            return (T) new DriverManagerConnectionSource(driverClassName, connectionString, connectionString, userName,
                    password, properties);
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

    @PluginBuilderFactory
    public static <B extends Builder<B, T>, T extends DriverManagerConnectionSource> B newBuilder() {
        return new Builder<B, T>().asBuilder();
    }

    private final String actualConnectionString;
    private final String connectionString;
    private final String driverClassName;
    private final char[] password;
    private final Property[] properties;
    private final char[] userName;

    public DriverManagerConnectionSource(final String driverClassName, final String connectionString,
            String actualConnectionString, final char[] userName, final char[] password, final Property[] properties) {
        super();
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

    @Override
    public Connection getConnection() throws SQLException {
        loadDriver();
        // No, you cannot see the user name and password.
        final String actualConnectionString = getActualConnectionString();
        LOGGER.debug("Getting connection from DriverManage for '{}'", actualConnectionString);
        if (properties != null && properties.length > 0) {
            if (userName != null || password != null) {
                throw new SQLException("Either set the userName and password, or set the Properties, but not both.");
            }
            return DriverManager.getConnection(actualConnectionString, toProperties(properties));
        }
        return DriverManager.getConnection(actualConnectionString, toString(userName), toString(password));
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
            try {
                Class.forName(className);
            } catch (final Exception e) {
                throw new SQLException(String.format("The %s could not load the JDBC driver %s: %s",
                        getClass().getSimpleName(), className, e.toString()), e);
            }
        }
    }

    private Properties toProperties(final Property[] properties) {
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

    private String toString(final char[] value) {
        return value == null ? null : String.valueOf(value);
    }
}
