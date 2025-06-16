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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.Test;

class ColumnConfigTest {

    @Test
    void testNullNameNoConfig() {
        final ColumnConfig config = ColumnConfig.newBuilder().setPattern("%l").build();

        assertNull(config, "The result should be null.");
    }

    @Test
    void testPatternAndLiteralNoConfig() {
        final ColumnConfig config = ColumnConfig.newBuilder()
                .setName("col")
                .setPattern("%l")
                .setLiteral("literal")
                .build();

        assertNull(config, "The result should be null.");
    }

    @Test
    void testPatternAndDateNoConfig() {
        final ColumnConfig config = ColumnConfig.newBuilder()
                .setName("col")
                .setPattern("%l")
                .setEventTimestamp(true)
                .build();

        assertNull(config, "The result should be null.");
    }

    @Test
    void testLiteralAndDateNoConfig() {
        final ColumnConfig config = ColumnConfig.newBuilder()
                .setName("col")
                .setLiteral("literal")
                .setEventTimestamp(true)
                .build();

        assertNull(config, "The result should be null.");
    }

    @Test
    void testNoSettingNoConfig01() {
        final ColumnConfig config = ColumnConfig.newBuilder().setName("col").build();

        assertNull(config, "The result should be null.");
    }

    @Test
    void testNoSettingNoConfig02() {
        final ColumnConfig config = ColumnConfig.newBuilder()
                .setName("col")
                .setEventTimestamp(false)
                .build();

        assertNull(config, "The result should be null.");
    }

    @Test
    void testNoSettingNoConfig03() {
        final ColumnConfig config = ColumnConfig.newBuilder()
                .setName("col")
                .setPattern(Strings.EMPTY)
                .setLiteral(Strings.EMPTY)
                .setEventTimestamp(false)
                .build();

        assertNull(config, "The result should be null.");
    }

    @Test
    void testDateColumn01() {
        final ColumnConfig config =
                ColumnConfig.newBuilder().setName("col").setEventTimestamp(true).build();

        assertNotNull(config, "The result should not be null.");
        assertEquals("col", config.getColumnName(), "The column name is not correct.");
        assertNull(config.getLayout(), "The pattern should be null.");
        assertNull(config.getLiteralValue(), "The literal value should be null.");
        assertTrue(config.isEventTimestamp(), "The timestamp flag should be true.");
        assertFalse(config.isUnicode(), "The unicode flag should be false.");
        assertFalse(config.isClob(), "The clob flag should be false.");
    }

    @Test
    void testDateColumn02() {
        final ColumnConfig config = ColumnConfig.newBuilder()
                .setName("col2")
                .setEventTimestamp(true)
                .setUnicode(true)
                .setClob(true)
                .build();

        assertNotNull(config, "The result should not be null.");
        assertEquals("col2", config.getColumnName(), "The column name is not correct.");
        assertNull(config.getLayout(), "The pattern should be null.");
        assertNull(config.getLiteralValue(), "The literal value should be null.");
        assertTrue(config.isEventTimestamp(), "The timestamp flag should be true.");
        assertFalse(config.isUnicode(), "The unicode flag should be false.");
        assertFalse(config.isClob(), "The clob flag should be false.");
    }

    @Test
    void testPatternColumn01() {
        final ColumnConfig config =
                ColumnConfig.newBuilder().setName("col").setPattern("%l").build();

        assertNotNull(config, "The result should not be null.");
        assertEquals("col", config.getColumnName(), "The column name is not correct.");
        assertNotNull(config.getLayout(), "The pattern should not be null.");
        assertEquals("%l", config.getLayout().toString(), "The pattern is not correct.");
        assertNull(config.getLiteralValue(), "The literal value should be null.");
        assertFalse(config.isEventTimestamp(), "The timestamp flag should be false.");
        assertTrue(config.isUnicode(), "The unicode flag should be true.");
        assertFalse(config.isClob(), "The clob flag should be false.");
    }

    @Test
    void testPatternColumn02() {
        final ColumnConfig config = ColumnConfig.newBuilder()
                .setName("col2")
                .setPattern("%X{id} %level")
                .setLiteral(Strings.EMPTY)
                .setEventTimestamp(false)
                .setUnicode(false)
                .setClob(true)
                .build();

        assertNotNull(config, "The result should not be null.");
        assertEquals("col2", config.getColumnName(), "The column name is not correct.");
        assertNotNull(config.getLayout(), "The pattern should not be null.");
        assertEquals("%X{id} %level", config.getLayout().toString(), "The pattern is not correct.");
        assertNull(config.getLiteralValue(), "The literal value should be null.");
        assertFalse(config.isEventTimestamp(), "The timestamp flag should be false.");
        assertFalse(config.isUnicode(), "The unicode flag should be false.");
        assertTrue(config.isClob(), "The clob flag should be true.");
    }

    @Test
    void testPatternColumn03() {
        final ColumnConfig config = ColumnConfig.newBuilder()
                .setName("col3")
                .setPattern("%X{id} %level")
                .setLiteral(Strings.EMPTY)
                .setEventTimestamp(false)
                .setUnicode(true)
                .setClob(false)
                .build();

        assertNotNull(config, "The result should not be null.");
        assertEquals("col3", config.getColumnName(), "The column name is not correct.");
        assertNotNull(config.getLayout(), "The pattern should not be null.");
        assertEquals("%X{id} %level", config.getLayout().toString(), "The pattern is not correct.");
        assertNull(config.getLiteralValue(), "The literal value should be null.");
        assertFalse(config.isEventTimestamp(), "The timestamp flag should be false.");
        assertTrue(config.isUnicode(), "The unicode flag should be true.");
        assertFalse(config.isClob(), "The clob flag should be false.");
    }

    @Test
    void testLiteralColumn01() {
        final ColumnConfig config = ColumnConfig.newBuilder()
                .setName("col")
                .setLiteral("literalValue01")
                .build();

        assertNotNull(config, "The result should not be null.");
        assertEquals("col", config.getColumnName(), "The column name is not correct.");
        assertNull(config.getLayout(), "The pattern should be null.");
        assertNotNull(config.getLiteralValue(), "The literal value should be null.");
        assertEquals("literalValue01", config.getLiteralValue(), "The literal value is not correct.");
        assertFalse(config.isEventTimestamp(), "The timestamp flag should be false.");
        assertFalse(config.isUnicode(), "The unicode flag should be false.");
        assertFalse(config.isClob(), "The clob flag should be false.");
    }

    @Test
    void testLiteralColumn02() {
        final ColumnConfig config = ColumnConfig.newBuilder()
                .setName("col2")
                .setLiteral("USER1.MY_SEQUENCE.NEXT")
                .setUnicode(true)
                .setClob(true)
                .build();

        assertNotNull(config, "The result should not be null.");
        assertEquals("col2", config.getColumnName(), "The column name is not correct.");
        assertNull(config.getLayout(), "The pattern should be null.");
        assertNotNull(config.getLiteralValue(), "The literal value should be null.");
        assertEquals("USER1.MY_SEQUENCE.NEXT", config.getLiteralValue(), "The literal value is not correct.");
        assertFalse(config.isEventTimestamp(), "The timestamp flag should be false.");
        assertFalse(config.isUnicode(), "The unicode flag should be false.");
        assertFalse(config.isClob(), "The clob flag should be false.");
    }
}
