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

import org.apache.logging.log4j.util.Strings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ColumnConfigTest {
    @Before
    public void setUp() {

    }

    @After
    public void tearDown() {

    }

    @Test
    public void testNullNameNoConfig() {
        final ColumnConfig config = ColumnConfig.createColumnConfig(null, null, "%l", null, null, null, null);

        assertNull("The result should be null.", config);
    }

    @Test
    public void testPatternAndLiteralNoConfig() {
        final ColumnConfig config =
                ColumnConfig.createColumnConfig(null, "columnName01", "%l", "literal", null, null, null);

        assertNull("The result should be null.", config);
    }

    @Test
    public void testPatternAndDateNoConfig() {
        final ColumnConfig config =
                ColumnConfig.createColumnConfig(null, "columnName01", "%l", null, "true", null, null);

        assertNull("The result should be null.", config);
    }

    @Test
    public void testLiteralAndDateNoConfig() {
        final ColumnConfig config =
                ColumnConfig.createColumnConfig(null, "columnName01", null, "literal", "true", null, null);

        assertNull("The result should be null.", config);
    }

    @Test
    public void testNoSettingNoConfig01() {
        final ColumnConfig config =
                ColumnConfig.createColumnConfig(null, "columnName01", null, null, null, null, null);

        assertNull("The result should be null.", config);
    }

    @Test
    public void testNoSettingNoConfig02() {
        final ColumnConfig config =
                ColumnConfig.createColumnConfig(null, "columnName01", null, null, "false", null, null);

        assertNull("The result should be null.", config);
    }

    @Test
    public void testNoSettingNoConfig03() {
        final ColumnConfig config =
                ColumnConfig.createColumnConfig(null, "columnName01", Strings.EMPTY, Strings.EMPTY, Strings.EMPTY, null, null);

        assertNull("The result should be null.", config);
    }

    @Test
    public void testDateColumn01() {
        final ColumnConfig config =
                ColumnConfig.createColumnConfig(null, "columnName01", null, null, "true", null, null);

        assertNotNull("The result should not be null.", config);
        assertEquals("The column name is not correct.", "columnName01", config.getColumnName());
        assertNull("The pattern should be null.", config.getLayout());
        assertNull("The literal value should be null.", config.getLiteralValue());
        assertTrue("The timestamp flag should be true.", config.isEventTimestamp());
        assertFalse("The unicode flag should be false.", config.isUnicode());
        assertFalse("The clob flag should be false.", config.isClob());
    }

    @Test
    public void testDateColumn02() {
        final ColumnConfig config =
                ColumnConfig.createColumnConfig(null, "anotherName02", null, null, "true", "true", "true");

        assertNotNull("The result should not be null.", config);
        assertEquals("The column name is not correct.", "anotherName02", config.getColumnName());
        assertNull("The pattern should be null.", config.getLayout());
        assertNull("The literal value should be null.", config.getLiteralValue());
        assertTrue("The timestamp flag should be true.", config.isEventTimestamp());
        assertFalse("The unicode flag should be false.", config.isUnicode());
        assertFalse("The clob flag should be false.", config.isClob());
    }

    @Test
    public void testPatternColumn01() {
        final ColumnConfig config =
                ColumnConfig.createColumnConfig(null, "columnName01", "%l", null, null, null, null);

        assertNotNull("The result should not be null.", config);
        assertEquals("The column name is not correct.", "columnName01", config.getColumnName());
        assertNotNull("The pattern should not be null.", config.getLayout());
        assertEquals("The pattern is not correct.", "%l", config.getLayout().toString());
        assertNull("The literal value should be null.", config.getLiteralValue());
        assertFalse("The timestamp flag should be false.", config.isEventTimestamp());
        assertTrue("The unicode flag should be true.", config.isUnicode());
        assertFalse("The clob flag should be false.", config.isClob());
    }

    @Test
    public void testPatternColumn02() {
        final ColumnConfig config =
                ColumnConfig.createColumnConfig(null, "anotherName02", "%X{id} %level", Strings.EMPTY, "false", "false", "true");

        assertNotNull("The result should not be null.", config);
        assertEquals("The column name is not correct.", "anotherName02", config.getColumnName());
        assertNotNull("The pattern should not be null.", config.getLayout());
        assertEquals("The pattern is not correct.", "%X{id} %level", config.getLayout().toString());
        assertNull("The literal value should be null.", config.getLiteralValue());
        assertFalse("The timestamp flag should be false.", config.isEventTimestamp());
        assertFalse("The unicode flag should be false.", config.isUnicode());
        assertTrue("The clob flag should be true.", config.isClob());
    }

    @Test
    public void testPatternColumn03() {
        final ColumnConfig config =
                ColumnConfig.createColumnConfig(null, "anotherName02", "%X{id} %level", Strings.EMPTY, "false", "true", "false");

        assertNotNull("The result should not be null.", config);
        assertEquals("The column name is not correct.", "anotherName02", config.getColumnName());
        assertNotNull("The pattern should not be null.", config.getLayout());
        assertEquals("The pattern is not correct.", "%X{id} %level", config.getLayout().toString());
        assertNull("The literal value should be null.", config.getLiteralValue());
        assertFalse("The timestamp flag should be false.", config.isEventTimestamp());
        assertTrue("The unicode flag should be true.", config.isUnicode());
        assertFalse("The clob flag should be false.", config.isClob());
    }

    @Test
    public void testLiteralColumn01() {
        final ColumnConfig config =
                ColumnConfig.createColumnConfig(null, "columnName01", null, "literalValue01", null, null, null);

        assertNotNull("The result should not be null.", config);
        assertEquals("The column name is not correct.", "columnName01", config.getColumnName());
        assertNull("The pattern should be null.", config.getLayout());
        assertNotNull("The literal value should be null.", config.getLiteralValue());
        assertEquals("The literal value is not correct.", "literalValue01", config.getLiteralValue());
        assertFalse("The timestamp flag should be false.", config.isEventTimestamp());
        assertFalse("The unicode flag should be false.", config.isUnicode());
        assertFalse("The clob flag should be false.", config.isClob());
    }

    @Test
    public void testLiteralColumn02() {
        final ColumnConfig config = ColumnConfig.createColumnConfig(
                null, "anotherName02", null, "USER1.MY_SEQUENCE.NEXT", null, "true", "true"
        );

        assertNotNull("The result should not be null.", config);
        assertEquals("The column name is not correct.", "anotherName02", config.getColumnName());
        assertNull("The pattern should be null.", config.getLayout());
        assertNotNull("The literal value should be null.", config.getLiteralValue());
        assertEquals("The literal value is not correct.", "USER1.MY_SEQUENCE.NEXT", config.getLiteralValue());
        assertFalse("The timestamp flag should be false.", config.isEventTimestamp());
        assertFalse("The unicode flag should be false.", config.isUnicode());
        assertFalse("The clob flag should be false.", config.isClob());
    }
}
