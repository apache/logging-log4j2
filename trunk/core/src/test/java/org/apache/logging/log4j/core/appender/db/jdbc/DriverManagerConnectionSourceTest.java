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
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.core.helpers.NameUtil;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class DriverManagerConnectionSourceTest {
    private Driver driver;

    @Before
    public void setUp() throws SQLException {
        this.driver = createStrictMock(Driver.class);
        replay(this.driver);
        DriverManager.registerDriver(driver);
        verify(this.driver);
        reset(this.driver);
    }

    @After
    public void tearDown() throws SQLException {
        DriverManager.deregisterDriver(this.driver);
        verify(this.driver);
    }

    @Test
    public void testNoUrl01() {
        replay(this.driver);

        final DriverManagerConnectionSource source = DriverManagerConnectionSource.createConnectionSource(null,
                "username01", "password01");

        assertNull("The connection source should be null.", source);
    }

    @Test
    public void testNoUrl02() {
        replay(this.driver);

        final DriverManagerConnectionSource source = DriverManagerConnectionSource.createConnectionSource("",
                "username01", "password01");

        assertNull("The connection source should be null.", source);
    }

    @Test
    public void testInvalidUrl() throws SQLException {
        expect(this.driver.acceptsURL("log4j2:test:appender:jdbc:url://localhost:2737/database")).andReturn(false);
        replay(this.driver);

        final DriverManagerConnectionSource source = DriverManagerConnectionSource.createConnectionSource(
                "log4j2:test:appender:jdbc:url://localhost:2737/database", "username01", "password01");

        assertNull("The connection source should be null.", source);
    }

    @Test
    public void testValidUrlUsernamePassword() throws SQLException {
        final Connection connection1 = createStrictMock(Connection.class);
        final Connection connection2 = createStrictMock(Connection.class);

        final Capture<Properties> capture = new Capture<Properties>(CaptureType.ALL);

        expect(this.driver.acceptsURL("log4j2:test:appender:jdbc:url://localhost:2737/database")).andReturn(true);
        expect(this.driver.connect(eq("log4j2:test:appender:jdbc:url://localhost:2737/database"), capture(capture)))
                .andReturn(connection1);
        expect(this.driver.connect(eq("log4j2:test:appender:jdbc:url://localhost:2737/database"), capture(capture)))
                .andReturn(connection2);
        replay(this.driver, connection1, connection2);

        final DriverManagerConnectionSource source = DriverManagerConnectionSource.createConnectionSource(
                "log4j2:test:appender:jdbc:url://localhost:2737/database", "username01", "password01");

        assertNotNull("The connection source should not be null.", source);
        assertEquals("The toString value is not correct.",
                "driverManager{ url=log4j2:test:appender:jdbc:url://localhost:2737/database, username=username01, "
                        + "passwordHash=" + NameUtil.md5("password01" + DriverManagerConnectionSource.class.getName())
                        + " }", source.toString());
        assertSame("The connection is not correct (1).", connection1, source.getConnection());
        assertSame("The connection is not correct (2).", connection2, source.getConnection());

        final List<Properties> captured = capture.getValues();
        assertEquals("The number of captured properties is not correct.", 2, captured.size());

        final Properties properties1 = captured.get(0);
        assertNotNull("The properties object should not be null (1).", properties1);
        assertEquals("The username is not correct (1).", "username01", properties1.getProperty("user"));
        assertEquals("The password is not correct (1).", "password01", properties1.getProperty("password"));

        final Properties properties2 = captured.get(1);
        assertNotNull("The properties object should not be null (2).", properties2);
        assertEquals("The username is not correct (2).", "username01", properties2.getProperty("user"));
        assertEquals("The password is not correct (2).", "password01", properties2.getProperty("password"));

        verify(connection1, connection2);
    }

    @Test
    public void testValidUrlNoUsernamePassword01() throws SQLException {
        final Connection connection1 = createStrictMock(Connection.class);
        final Connection connection2 = createStrictMock(Connection.class);

        final Capture<Properties> capture = new Capture<Properties>(CaptureType.ALL);

        expect(this.driver.acceptsURL("log4j2:test:appender:jdbc:url://localhost:2737/anotherDb")).andReturn(true);
        expect(this.driver.connect(eq("log4j2:test:appender:jdbc:url://localhost:2737/anotherDb"), capture(capture)))
                .andReturn(connection1);
        expect(this.driver.connect(eq("log4j2:test:appender:jdbc:url://localhost:2737/anotherDb"), capture(capture)))
                .andReturn(connection2);
        replay(this.driver, connection1, connection2);

        final DriverManagerConnectionSource source = DriverManagerConnectionSource.createConnectionSource(
                "log4j2:test:appender:jdbc:url://localhost:2737/anotherDb", null, null);

        assertNotNull("The connection source should not be null.", source);
        assertEquals("The toString value is not correct.",
                "driverManager{ url=log4j2:test:appender:jdbc:url://localhost:2737/anotherDb, username=null, "
                        + "passwordHash=" + NameUtil.md5(null + DriverManagerConnectionSource.class.getName()) + " }",
                source.toString());
        assertSame("The connection is not correct (1).", connection1, source.getConnection());
        assertSame("The connection is not correct (2).", connection2, source.getConnection());

        final List<Properties> captured = capture.getValues();
        assertEquals("The number of captured properties is not correct.", 2, captured.size());

        final Properties properties1 = captured.get(0);
        assertNotNull("The properties object should not be null (1).", properties1);
        assertNull("The username should be null (1).", properties1.getProperty("user"));
        assertNull("The password should be null (1).", properties1.getProperty("password"));

        final Properties properties2 = captured.get(1);
        assertNotNull("The properties object should not be null (2).", properties2);
        assertNull("The username should be null (2).", properties2.getProperty("user"));
        assertNull("The password should be null (2).", properties2.getProperty("password"));

        verify(connection1, connection2);
    }

    @Test
    public void testValidUrlNoUsernamePassword02() throws SQLException {
        final Connection connection1 = createStrictMock(Connection.class);
        final Connection connection2 = createStrictMock(Connection.class);

        final Capture<Properties> capture = new Capture<Properties>(CaptureType.ALL);

        expect(this.driver.acceptsURL("log4j2:test:appender:jdbc:url://localhost:2737/finalDb")).andReturn(true);
        expect(this.driver.connect(eq("log4j2:test:appender:jdbc:url://localhost:2737/finalDb"), capture(capture)))
                .andReturn(connection1);
        expect(this.driver.connect(eq("log4j2:test:appender:jdbc:url://localhost:2737/finalDb"), capture(capture)))
                .andReturn(connection2);
        replay(this.driver, connection1, connection2);

        final DriverManagerConnectionSource source = DriverManagerConnectionSource.createConnectionSource(
                "log4j2:test:appender:jdbc:url://localhost:2737/finalDb", "     ", "");

        assertNotNull("The connection source should not be null.", source);
        assertEquals("The toString value is not correct.",
                "driverManager{ url=log4j2:test:appender:jdbc:url://localhost:2737/finalDb, username=null, "
                        + "passwordHash=" + NameUtil.md5(null + DriverManagerConnectionSource.class.getName()) + " }",
                source.toString());
        assertSame("The connection is not correct (1).", connection1, source.getConnection());
        assertSame("The connection is not correct (2).", connection2, source.getConnection());

        final List<Properties> captured = capture.getValues();
        assertEquals("The number of captured properties is not correct.", 2, captured.size());

        final Properties properties1 = captured.get(0);
        assertNotNull("The properties object should not be null (1).", properties1);
        assertNull("The username should be null (1).", properties1.getProperty("user"));
        assertNull("The password should be null (1).", properties1.getProperty("password"));

        final Properties properties2 = captured.get(1);
        assertNotNull("The properties object should not be null (2).", properties2);
        assertNull("The username should be null (2).", properties2.getProperty("user"));
        assertNull("The password should be null (2).", properties2.getProperty("password"));

        verify(connection1, connection2);
    }
}
