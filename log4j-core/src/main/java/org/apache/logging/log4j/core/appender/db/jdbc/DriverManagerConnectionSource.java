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

import java.sql.DriverManager;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;

/**
 * A {@link ConnectionSource} that uses a JDBC connection string, a user name, and a password to call
 * {@link DriverManager#getConnection(String, String, String)}.
 * <p>
 * This plugin does not provide any connection pooling unless it is available through the connection string and driver
 * itself. This handy to get you off the ground without having to deal with JNDI.
 * </p>
 */
@Plugin(name = "DriverManager", category = Core.CATEGORY_NAME, elementType = "connectionSource", printObject = true)
public class DriverManagerConnectionSource extends AbstractDriverManagerConnectionSource {

    /**
     * Builds DriverManagerConnectionSource instances.
     *
     * @param <B>
     *            This builder type or a subclass.
     */
    public static class Builder<B extends Builder<B>> extends AbstractDriverManagerConnectionSource.Builder<B>
            implements org.apache.logging.log4j.core.util.Builder<DriverManagerConnectionSource> {

        @Override
        public DriverManagerConnectionSource build() {
            return new DriverManagerConnectionSource(
                    getDriverClassName(),
                    getConnectionString(),
                    getConnectionString(),
                    getUserName(),
                    getPassword(),
                    getProperties());
        }
    }

    @PluginBuilderFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }

    public DriverManagerConnectionSource(
            final String driverClassName,
            final String connectionString,
            final String actualConnectionString,
            final char[] userName,
            final char[] password,
            final Property[] properties) {
        super(driverClassName, connectionString, actualConnectionString, userName, password, properties);
    }
}
