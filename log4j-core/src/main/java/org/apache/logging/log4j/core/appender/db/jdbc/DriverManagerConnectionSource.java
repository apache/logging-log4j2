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
public final class DriverManagerConnectionSource implements ConnectionSource {

    /**
     * Builds DriverManagerConnectionSource instances.
     * 
     * @param <B>
     *            The type to build
     */
    public static class Builder<B extends Builder<B>>
            implements org.apache.logging.log4j.core.util.Builder<DriverManagerConnectionSource> {

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

        @Override
        public DriverManagerConnectionSource build() {
            return new DriverManagerConnectionSource(driverClassName, connectionString, userName, password, properties);
        }

        public B setConnectionString(String connectionString) {
            this.connectionString = connectionString;
            return asBuilder();
        }

        public B setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
            return asBuilder();
        }

        public B setPassword(char[] password) {
            this.password = password;
            return asBuilder();
        }

        public B setProperties(Property[] properties) {
            this.properties = properties;
            return asBuilder();
        }

        public B setUserName(char[] userName) {
            this.userName = userName;
            return asBuilder();
        }
    }

    private static final Logger LOGGER = StatusLogger.getLogger();
    
    @PluginBuilderFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }

    private final String connectionString;
    private final String driverClassName;
    private final char[] password;
    private final char[] userName;
    private final Property[] properties;

    public DriverManagerConnectionSource(String driverClassName, String connectionString, char[] userName,
            char[] password, final Property[] properties) {
        super();
        this.driverClassName = driverClassName;
        this.connectionString = connectionString;
        this.userName = userName;
        this.password = password;
        this.properties = properties;
    }

    @Override
    public Connection getConnection() throws SQLException {
        loadDriver();
        // No, you cannot see the user name and password.
        LOGGER.debug("Getting connection from DriverManage for '{}'", connectionString);
        if (properties != null && properties.length > 0) {
            if (userName != null || password != null) {
                throw new SQLException("Either set the userName and password, or set the Properties, but not both.");
            }
            return DriverManager.getConnection(connectionString, toProperties(properties));
        }
        return DriverManager.getConnection(connectionString, toString(userName), toString(password));
    }

    private void loadDriver() throws SQLException {
        if (driverClassName != null) {
            // Hack for old JDBC drivers.
            try {
                Class.forName(driverClassName);
            } catch (Exception e) {
                throw new SQLException(String.format("The %s could not load the JDBC driver %s: %s",
                        getClass().getSimpleName(), driverClassName, e.toString()), e);
            }
        }
    }

    private Properties toProperties(Property[] properties) {
        Properties props = new Properties();
        for (Property property : properties) {
            props.setProperty(property.getName(), property.getValue());
        }
        return props;
    }

    @Override
    public String toString() {
        return this.connectionString;
    }

    private String toString(char[] value) {
        return value == null ? null : String.valueOf(value);
    }
}
