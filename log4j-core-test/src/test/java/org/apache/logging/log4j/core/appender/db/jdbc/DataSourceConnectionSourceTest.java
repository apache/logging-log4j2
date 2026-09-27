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
import org.apache.logging.log4j.core.test.junit.JndiRule;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@LoggerContextSource("log4j-fatalOnly.xml")
public abstract class DataSourceConnectionSourceTest extends AbstractJdbcDataSourceTest {

    @RegisterExtension
    private JndiRule jndiRule;

    private final DataSource dataSource = mock(DataSource.class);
    private final String jndiURL;

    public DataSourceConnectionSourceTest(final String jndiURL) {
        this.jndiRule = new JndiRule(jndiURL, dataSource);
        this.jndiURL = jndiURL;
    }

    @Test
    public void testNullJndiName() {
        final DataSourceConnectionSource source = DataSourceConnectionSource.createConnectionSource(null);

        assertNull(source, "The connection source should be null.");
    }

    @Test
    public void testEmptyJndiName() {
        final DataSourceConnectionSource source = DataSourceConnectionSource.createConnectionSource("");

        assertNull(source, "The connection source should be null.");
    }

    @Test
    public void testNoDataSource() {
        final DataSourceConnectionSource source = DataSourceConnectionSource.createConnectionSource(jndiURL + "123");

        assertNull(source, "The connection source should be null.");
    }

    @Test
    public void testDataSource() throws SQLException {
        try (final Connection connection1 = mock(Connection.class);
                final Connection connection2 = mock(Connection.class)) {

            given(dataSource.getConnection()).willReturn(connection1, connection2);

            DataSourceConnectionSource source = DataSourceConnectionSource.createConnectionSource(jndiURL);

            assertNotNull(source, "The connection source should not be null.");
            assertEquals(
                    "dataSource{ name=" + jndiURL + ", value=" + dataSource + " }",
                    source.toString(),
                    "The toString value is not correct.");
            assertSame(connection1, source.getConnection(), "The connection is not correct (1).");
            assertSame(connection2, source.getConnection(), "The connection is not correct (2).");

            source = DataSourceConnectionSource.createConnectionSource(jndiURL.substring(0, jndiURL.length() - 1));

            assertNull(source, "The connection source should be null now.");
        }
    }
}
