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
import java.sql.SQLException;
import javax.sql.DataSource;

import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class FactoryMethodConnectionSourceTest {
    private static ThreadLocal<Object> holder = new ThreadLocal<>();
    private static final String CONFIG = "log4j-fatalOnly.xml";

    @ClassRule
    public static LoggerContextRule ctx = new LoggerContextRule(CONFIG);

    @After
    public void tearDown() {
        holder.remove();
    }

    @Test
    public void testNoClassName() {
        final FactoryMethodConnectionSource source =
                FactoryMethodConnectionSource.createConnectionSource(null, "method");

        assertNull("The connection source should be null.", source);
    }

    @Test
    public void testNoMethodName() {
        final FactoryMethodConnectionSource source =
                FactoryMethodConnectionSource.createConnectionSource("someClass", null);

        assertNull("The connection source should be null.", source);
    }

    @Test
    public void testBadClassName() {
        final FactoryMethodConnectionSource source =
                FactoryMethodConnectionSource.createConnectionSource("org.apache.BadClass", "factoryMethod");

        assertNull("The connection source should be null.", source);
    }

    @Test
    public void testBadMethodName() {
        final FactoryMethodConnectionSource source =
                FactoryMethodConnectionSource.createConnectionSource(this.getClass().getName(), "factoryMethod");

        assertNull("The connection source should be null.", source);
    }

    @Test
    public void testBadReturnType() {
        final FactoryMethodConnectionSource source = FactoryMethodConnectionSource.createConnectionSource(
                BadReturnTypeFactory.class.getName(), "factoryMethod01"
        );

        assertNull("The connection source should be null.", source);
    }

    @Test
    public void testDataSourceReturnType() throws SQLException {
        final DataSource dataSource = mock(DataSource.class);
        final Connection connection1 = mock(Connection.class);
        final Connection connection2 = mock(Connection.class);

        given(dataSource.getConnection()).willReturn(connection1, connection2);

        holder.set(dataSource);

        final FactoryMethodConnectionSource source = FactoryMethodConnectionSource.createConnectionSource(
                DataSourceFactory.class.getName(), "factoryMethod02"
        );

        assertNotNull("The connection source should not be null.", source);
        assertEquals("The toString value is not correct.", "factory{ public static javax.sql.DataSource[" + dataSource
                + "] " + DataSourceFactory.class.getName() + ".factoryMethod02() }", source.toString());
        assertSame("The connection is not correct (1).", connection1, source.getConnection());
        assertSame("The connection is not correct (2).", connection2, source.getConnection());
    }

    @Test
    public void testConnectionReturnType() throws SQLException {
        final Connection connection = mock(Connection.class);

        holder.set(connection);

        final FactoryMethodConnectionSource source = FactoryMethodConnectionSource.createConnectionSource(
                ConnectionFactory.class.getName(), "anotherMethod03"
        );

        assertNotNull("The connection source should not be null.", source);
        assertEquals("The toString value is not correct.", "factory{ public static java.sql.Connection "
                + ConnectionFactory.class.getName() + ".anotherMethod03() }", source.toString());
        assertSame("The connection is not correct (1).", connection, source.getConnection());
        assertSame("The connection is not correct (2).", connection, source.getConnection());
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
