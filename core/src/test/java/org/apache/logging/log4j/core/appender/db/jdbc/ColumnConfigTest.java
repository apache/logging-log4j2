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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ColumnConfigTest {
    @Before
    public void setUp() {

    }

    @After
    public void tearDown() {

    }

    @Test
    public void testDateColumn01() {
        final ColumnConfig config = ColumnConfig.createColumnConfig(null, "columnName01", null, null, "true");

        assertNotNull("The result should not be null.", config);
        assertEquals("The column name is not correct.", "columnName01", config.getColumnName());
        assertNull("The pattern should be null.", config.getLayout());
        assertNull("The literal value should be null.", config.getLiteralValue());
        assertTrue("The timestamp flag should be true.", config.isEventTimestamp());
    }

    @Test
    public void testDateColumn02() {
        final ColumnConfig config = ColumnConfig.createColumnConfig(null, "anotherName02", null, null, "true");

        assertNotNull("The result should not be null.", config);
        assertEquals("The column name is not correct.", "anotherName02", config.getColumnName());
        assertNull("The pattern should be null.", config.getLayout());
        assertNull("The literal value should be null.", config.getLiteralValue());
        assertTrue("The timestamp flag should be true.", config.isEventTimestamp());
    }

    @Test
    public void testLiteralAndDateNoConfig() {
        final ColumnConfig config = ColumnConfig.createColumnConfig(null, "columnName01", null, "literal", "true");

        assertNull("The result should be null.", config);
    }

    @Test
    public void testLiteralColumn01() {
        final ColumnConfig config = ColumnConfig.createColumnConfig(null, "columnName01", null, "literalValue01", null);

        assertNotNull("The result should not be null.", config);
        assertEquals("The column name is not correct.", "columnName01", config.getColumnName());
        assertNull("The pattern should be null.", config.getLayout());
        assertNotNull("The literal value should be null.", config.getLiteralValue());
        assertEquals("The literal value is not correct.", "literalValue01", config.getLiteralValue());
        assertFalse("The timestamp flag should be false.", config.isEventTimestamp());
    }

    @Test
    public void testLiteralColumn02() {
        final ColumnConfig config = ColumnConfig.createColumnConfig(null, "anotherName02", null,
                "USER1.MY_SEQUENCE.NEXT", null);

        assertNotNull("The result should not be null.", config);
        assertEquals("The column name is not correct.", "anotherName02", config.getColumnName());
        assertNull("The pattern should be null.", config.getLayout());
        assertNotNull("The literal value should be null.", config.getLiteralValue());
        assertEquals("The literal value is not correct.", "USER1.MY_SEQUENCE.NEXT", config.getLiteralValue());
        assertFalse("The timestamp flag should be false.", config.isEventTimestamp());
    }

    @Test
    public void testNoSettingNoConfig01() {
        final ColumnConfig config = ColumnConfig.createColumnConfig(null, "columnName01", null, null, null);

        assertNull("The result should be null.", config);
    }

    @Test
    public void testNoSettingNoConfig02() {
        final ColumnConfig config = ColumnConfig.createColumnConfig(null, "columnName01", null, null, "false");

        assertNull("The result should be null.", config);
    }

    @Test
    public void testNoSettingNoConfig03() {
        final ColumnConfig config = ColumnConfig.createColumnConfig(null, "columnName01", "", "", "");

        assertNull("The result should be null.", config);
    }

    @Test
    public void testNullNameNoConfig() {
        final ColumnConfig config = ColumnConfig.createColumnConfig(null, null, "%l", null, null);

        assertNull("The result should be null.", config);
    }

    @Test
    public void testPatternAndDateNoConfig() {
        final ColumnConfig config = ColumnConfig.createColumnConfig(null, "columnName01", "%l", null, "true");

        assertNull("The result should be null.", config);
    }

    @Test
    public void testPatternAndLiteralNoConfig() {
        final ColumnConfig config = ColumnConfig.createColumnConfig(null, "columnName01", "%l", "literal", null);

        assertNull("The result should be null.", config);
    }

    @Test
    public void testPatternColumn01() {
        final ColumnConfig config = ColumnConfig.createColumnConfig(null, "columnName01", "%l", null, null);

        assertNotNull("The result should not be null.", config);
        assertEquals("The column name is not correct.", "columnName01", config.getColumnName());
        assertNotNull("The pattern should not be null.", config.getLayout());
        assertEquals("The pattern is not correct.", "%l", config.getLayout().toString());
        assertNull("The literal value should be null.", config.getLiteralValue());
        assertFalse("The timestamp flag should be false.", config.isEventTimestamp());
    }

    @Test
    public void testPatternColumn02() {
        final ColumnConfig config = ColumnConfig
                .createColumnConfig(null, "anotherName02", "%X{id} %level", "", "false");

        assertNotNull("The result should not be null.", config);
        assertEquals("The column name is not correct.", "anotherName02", config.getColumnName());
        assertNotNull("The pattern should not be null.", config.getLayout());
        assertEquals("The pattern is not correct.", "%X{id} %level", config.getLayout().toString());
        assertNull("The literal value should be null.", config.getLiteralValue());
        assertFalse("The timestamp flag should be false.", config.isEventTimestamp());
    }
}
