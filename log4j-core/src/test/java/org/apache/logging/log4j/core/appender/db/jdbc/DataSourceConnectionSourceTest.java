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
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.logging.log4j.junit.JndiRule;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@RunWith(Parameterized.class)
public class DataSourceConnectionSourceTest {

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] data() {
        return new Object[][]{
            {"java:/comp/env/jdbc/Logging01"},
            {"java:/comp/env/jdbc/Logging02"}
        };
    }

    private static final String CONFIG = "log4j-fatalOnly.xml";

    @Rule
    public final RuleChain rules;
    private final DataSource dataSource = mock(DataSource.class);
    private final String jndiURL;

    public DataSourceConnectionSourceTest(final String jndiURL) {
        this.rules = RuleChain.outerRule(new JndiRule(jndiURL, dataSource))
            .around(new LoggerContextRule(CONFIG));
        this.jndiURL = jndiURL;
    }

    @Test
    public void testNullJndiName() {
        final DataSourceConnectionSource source = DataSourceConnectionSource.createConnectionSource(null);

        assertNull("The connection source should be null.", source);
    }

    @Test
    public void testEmptyJndiName() {
        final DataSourceConnectionSource source = DataSourceConnectionSource.createConnectionSource("");

        assertNull("The connection source should be null.", source);
    }

    @Test
    public void testNoDataSource() {
        final DataSourceConnectionSource source = DataSourceConnectionSource.createConnectionSource(jndiURL + "123");

        assertNull("The connection source should be null.", source);
    }

    @Test
    public void testDataSource() throws NamingException, SQLException {
        final Connection connection1 = mock(Connection.class);
        final Connection connection2 = mock(Connection.class);

        given(dataSource.getConnection()).willReturn(connection1, connection2);

        DataSourceConnectionSource source = DataSourceConnectionSource.createConnectionSource(jndiURL);

        assertNotNull("The connection source should not be null.", source);
        assertEquals("The toString value is not correct.", "dataSource{ name=" + jndiURL + ", value="
            + dataSource + " }", source.toString());
        assertSame("The connection is not correct (1).", connection1, source.getConnection());
        assertSame("The connection is not correct (2).", connection2, source.getConnection());

        source = DataSourceConnectionSource.createConnectionSource(jndiURL.substring(0, jndiURL.length() - 1));

        assertNull("The connection source should be null now.", source);
    }

}
