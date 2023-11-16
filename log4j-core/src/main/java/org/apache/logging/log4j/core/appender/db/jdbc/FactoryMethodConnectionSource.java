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

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;

/**
 * A {@link JdbcAppender} connection source that uses a public static factory method to obtain a {@link Connection} or
 * {@link DataSource}.
 */
@Plugin(name = "ConnectionFactory", category = Core.CATEGORY_NAME, elementType = "connectionSource", printObject = true)
public final class FactoryMethodConnectionSource extends AbstractConnectionSource {
    private static final Logger LOGGER = StatusLogger.getLogger();

    private final DataSource dataSource;
    private final String description;

    private FactoryMethodConnectionSource(
            final DataSource dataSource, final String className, final String methodName, final String returnType) {
        this.dataSource = dataSource;
        this.description = "factory{ public static " + returnType + ' ' + className + '.' + methodName + "() }";
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
     * @param className The name of a public class that contains a static method capable of returning either a
     *                  {@link DataSource} or a {@link Connection}.
     * @param methodName The name of the public static method on the aforementioned class that returns the data source
     *                   or connection. If this method returns a {@link Connection}, it should return a new connection
     *                   every call.
     * @return the created connection source.
     */
    @PluginFactory
    public static FactoryMethodConnectionSource createConnectionSource(
            @PluginAttribute("class") final String className, @PluginAttribute("method") final String methodName) {
        if (Strings.isEmpty(className) || Strings.isEmpty(methodName)) {
            LOGGER.error("No class name or method name specified for the connection factory method.");
            return null;
        }

        final Method method;
        try {
            final Class<?> factoryClass = Loader.loadClass(className);
            method = factoryClass.getMethod(methodName);
        } catch (final Exception e) {
            LOGGER.error(e.toString(), e);
            return null;
        }

        final Class<?> returnType = method.getReturnType();
        String returnTypeString = returnType.getName();
        DataSource dataSource;
        if (returnType == DataSource.class) {
            try {
                dataSource = (DataSource) method.invoke(null);
                returnTypeString += "[" + dataSource + ']';
            } catch (final Exception e) {
                LOGGER.error(e.toString(), e);
                return null;
            }
        } else if (returnType == Connection.class) {
            dataSource = new DataSource() {
                @Override
                public Connection getConnection() throws SQLException {
                    try {
                        return (Connection) method.invoke(null);
                    } catch (final Exception e) {
                        throw new SQLException("Failed to obtain connection from factory method.", e);
                    }
                }

                @Override
                public Connection getConnection(final String username, final String password) throws SQLException {
                    throw new UnsupportedOperationException();
                }

                @Override
                public int getLoginTimeout() throws SQLException {
                    throw new UnsupportedOperationException();
                }

                @Override
                public PrintWriter getLogWriter() throws SQLException {
                    throw new UnsupportedOperationException();
                }

                @Override
                @SuppressWarnings("unused")
                public java.util.logging.Logger getParentLogger() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean isWrapperFor(final Class<?> iface) throws SQLException {
                    return false;
                }

                @Override
                public void setLoginTimeout(final int seconds) throws SQLException {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void setLogWriter(final PrintWriter out) throws SQLException {
                    throw new UnsupportedOperationException();
                }

                @Override
                public <T> T unwrap(final Class<T> iface) throws SQLException {
                    return null;
                }
            };
        } else {
            LOGGER.error(
                    "Method [{}.{}()] returns unsupported type [{}].", className, methodName, returnType.getName());
            return null;
        }

        return new FactoryMethodConnectionSource(dataSource, className, methodName, returnTypeString);
    }
}
