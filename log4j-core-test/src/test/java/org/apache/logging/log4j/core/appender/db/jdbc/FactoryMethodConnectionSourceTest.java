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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@LoggerContextSource("log4j-fatalOnly.xml")
public class FactoryMethodConnectionSourceTest {
    private static final ThreadLocal<Object> holder = new ThreadLocal<>();

    @AfterEach
    public void tearDown() {
        holder.remove();
    }

    @Test
    public void testNoClassName() {
        final FactoryMethodConnectionSource source =
                FactoryMethodConnectionSource.createConnectionSource(null, "method");
        assertNull(source, "The connection source should be null.");
    }

    @Test
    public void testNoMethodName() {
        final FactoryMethodConnectionSource source =
                FactoryMethodConnectionSource.createConnectionSource("someClass", null);

        assertNull(source, "The connection source should be null.");
    }

    @Test
    public void testBadClassName() {
        final FactoryMethodConnectionSource source =
                FactoryMethodConnectionSource.createConnectionSource("org.apache.BadClass", "factoryMethod");

        assertNull(source, "The connection source should be null.");
    }

    @Test
    public void testBadMethodName() {
        final FactoryMethodConnectionSource source = FactoryMethodConnectionSource.createConnectionSource(
                this.getClass().getName(), "factoryMethod");

        assertNull(source, "The connection source should be null.");
    }

    @Test
    public void testBadReturnType() {
        final FactoryMethodConnectionSource source = FactoryMethodConnectionSource.createConnectionSource(
                BadReturnTypeFactory.class.getName(), "factoryMethod01");

        assertNull(source, "The connection source should be null.");
    }

    @Test
    public void testDataSourceReturnType() throws SQLException {
        final DataSource dataSource = mock(DataSource.class);
        try (final Connection connection1 = mock(Connection.class);
                final Connection connection2 = mock(Connection.class)) {

            given(dataSource.getConnection()).willReturn(connection1, connection2);

            holder.set(dataSource);

            final FactoryMethodConnectionSource source = FactoryMethodConnectionSource.createConnectionSource(
                    DataSourceFactory.class.getName(), "factoryMethod02");

            assertNotNull(source, "The connection source should not be null.");
            assertEquals(
                    "factory{ public static javax.sql.DataSource[" + dataSource + "] "
                            + DataSourceFactory.class.getName() + ".factoryMethod02() }",
                    source.toString(),
                    "The toString value is not correct.");
            assertSame(connection1, source.getConnection(), "The connection is not correct (1).");
            assertSame(connection2, source.getConnection(), "The connection is not correct (2).");
        }
    }

    @Test
    public void testConnectionReturnType() throws SQLException {
        try (final Connection connection = mock(Connection.class)) {

            holder.set(connection);

            final FactoryMethodConnectionSource source = FactoryMethodConnectionSource.createConnectionSource(
                    ConnectionFactory.class.getName(), "anotherMethod03");

            assertNotNull(source, "The connection source should not be null.");
            assertEquals(
                    "factory{ public static java.sql.Connection " + ConnectionFactory.class.getName()
                            + ".anotherMethod03() }",
                    source.toString(),
                    "The toString value is not correct.");
            assertSame(connection, source.getConnection(), "The connection is not correct (1).");
            assertSame(connection, source.getConnection(), "The connection is not correct (2).");
        }
    }

    @SuppressWarnings("unused")
    protected static final class BadReturnTypeFactory {
        public static String factoryMethod01() {
            return "hello";
        }
    }

    @SuppressWarnings("unused")
    protected static final class DataSourceFactory {
        public static DataSource factoryMethod02() {
            return (DataSource) holder.get();
        }
    }

    @SuppressWarnings("unused")
    protected static final class ConnectionFactory {
        public static Connection anotherMethod03() {
            return (Connection) holder.get();
        }
    }
}
