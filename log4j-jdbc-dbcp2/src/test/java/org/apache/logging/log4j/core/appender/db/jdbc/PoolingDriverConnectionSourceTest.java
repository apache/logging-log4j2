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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.sql.Connection;
import java.sql.SQLException;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.test.appender.db.jdbc.JdbcH2TestHelper;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link PoolingDriverConnectionSource}.
 */
class PoolingDriverConnectionSourceTest {

    @Test
    void testH2Properties() throws SQLException {
        final Property[] properties = new Property[] {
            // @formatter:off
            Property.createProperty("username", JdbcH2TestHelper.USER_NAME),
            Property.createProperty("password", JdbcH2TestHelper.PASSWORD),
            // @formatter:on
        };
        // @formatter:off
        final PoolingDriverConnectionSource source =
                PoolingDriverConnectionSource.newPoolingDriverConnectionSourceBuilder()
                        .setConnectionString(JdbcH2TestHelper.CONNECTION_STRING_IN_MEMORY)
                        .setProperties(properties)
                        .build();
        // @formatter:on
        openAndClose(source);
    }

    @Test
    void testH2PropertiesAndPoolName() throws SQLException {
        final Property[] properties = new Property[] {
            // @formatter:off
            Property.createProperty("username", JdbcH2TestHelper.USER_NAME),
            Property.createProperty("password", JdbcH2TestHelper.PASSWORD),
            // @formatter:on
        };
        // @formatter:off
        final PoolingDriverConnectionSource source =
                PoolingDriverConnectionSource.newPoolingDriverConnectionSourceBuilder()
                        .setConnectionString(JdbcH2TestHelper.CONNECTION_STRING_IN_MEMORY)
                        .setProperties(properties)
                        .setPoolName("MyPoolName")
                        .build();
        openAndClose(source);
    }

    @Test
    void testH2UserAndPassword() throws SQLException {
        // @formatter:off
        final PoolingDriverConnectionSource source =
                PoolingDriverConnectionSource.newPoolingDriverConnectionSourceBuilder()
                        .setConnectionString(JdbcH2TestHelper.CONNECTION_STRING_IN_MEMORY)
                        .setUserName(JdbcH2TestHelper.USER_NAME.toCharArray())
                        .setPassword(JdbcH2TestHelper.PASSWORD.toCharArray())
                        .build();
        // @formatter:on
        openAndClose(source);
    }

    private void openAndClose(final PoolingDriverConnectionSource source) throws SQLException {
        assertNotNull(source, "PoolingDriverConnectionSource is null");
        try (final Connection conn = source.getConnection()) {
            assertFalse(conn.isClosed());
        } finally {
            source.stop();
        }
    }

    @Test
    void testH2UserPasswordAndPoolName() throws SQLException {
        // @formatter:off
        final PoolingDriverConnectionSource source =
                PoolingDriverConnectionSource.newPoolingDriverConnectionSourceBuilder()
                        .setConnectionString(JdbcH2TestHelper.CONNECTION_STRING_IN_MEMORY)
                        .setUserName(JdbcH2TestHelper.USER_NAME.toCharArray())
                        .setPassword(JdbcH2TestHelper.PASSWORD.toCharArray())
                        .setPoolName("MyPoolName")
                        .build();
        // @formatter:on
        openAndClose(source);
    }

    @Test
    void testPoolableConnectionFactoryConfig() throws SQLException {
        final PoolableConnectionFactoryConfig poolableConnectionFactoryConfig =
                PoolableConnectionFactoryConfig.newBuilder()
                        .setMaxConnLifetimeMillis(30000)
                        .build();
        // @formatter:off
        final PoolingDriverConnectionSource source =
                PoolingDriverConnectionSource.newPoolingDriverConnectionSourceBuilder()
                        .setConnectionString(JdbcH2TestHelper.CONNECTION_STRING_IN_MEMORY)
                        .setUserName(JdbcH2TestHelper.USER_NAME.toCharArray())
                        .setPassword(JdbcH2TestHelper.PASSWORD.toCharArray())
                        .setPoolName("MyPoolName")
                        .setPoolableConnectionFactoryConfig(poolableConnectionFactoryConfig)
                        .build();
        // @formatter:on
        openAndClose(source);
    }
}
