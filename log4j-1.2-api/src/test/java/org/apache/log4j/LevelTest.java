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
package org.apache.log4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.util.SerializationTestHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests of Level.
 *
 * @since 1.2.12
 */
class LevelTest {

    /**
     * Serialize Level.INFO and check against witness.
     *
     * @throws Exception if exception during test.
     */
    @Test
    void testSerializeINFO() throws Exception {
        final int[] skip = new int[] {};
        SerializationTestHelper.assertSerializationEquals(
                "target/test-classes/witness/serialization/info.bin", Level.INFO, skip, Integer.MAX_VALUE);
    }

    /**
     * Deserialize witness and see if resolved to Level.INFO.
     *
     * @throws Exception if exception during test.
     */
    @Test
    void testDeserializeINFO() throws Exception {
        final Object obj =
                SerializationTestHelper.deserializeStream("target/test-classes/witness/serialization/info.bin");
        assertInstanceOf(Level.class, obj);
        final Level info = (Level) obj;
        assertEquals("INFO", info.toString());
        //
        //  JDK 1.1 doesn't support readResolve necessary for the assertion
        if (!System.getProperty("java.version").startsWith("1.1.")) {
            assertEquals(Level.INFO, obj);
        }
    }

    /**
     * Tests that a custom level can be serialized and deserialized
     * and is not resolved to a stock level.
     *
     * @throws Exception if exception during test.
     */
    @Test
    void testCustomLevelSerialization() throws Exception {
        final CustomLevel custom = new CustomLevel();
        final Object obj = SerializationTestHelper.serializeClone(custom);
        assertInstanceOf(CustomLevel.class, obj);

        final CustomLevel clone = (CustomLevel) obj;
        assertEquals(Level.INFO.level, clone.level);
        assertEquals(Level.INFO.levelStr, clone.levelStr);
        assertEquals(Level.INFO.syslogEquivalent, clone.syslogEquivalent);
        assertEquals(OptionConverter.createLevel(custom), clone.version2Level);
    }

    /**
     * Custom level to check that custom levels are
     * serializable, but not resolved to a plain Level.
     */
    private static class CustomLevel extends Level {
        /**
         * Generated serial version ID.
         */
        private static final long serialVersionUID = -6931920872225831135L;

        /**
         * Create an instance of CustomLevel.
         */
        public CustomLevel() {
            super(Level.INFO.level, Level.INFO.levelStr, Level.INFO.syslogEquivalent);
        }
    }

    /**
     * Tests Level.TRACE_INT.
     */
    @Test
    void testTraceInt() {
        assertEquals(5000, Level.TRACE_INT);
    }

    /**
     * Tests Level.TRACE.
     */
    @Test
    void testTrace() {
        assertEquals("TRACE", Level.TRACE.toString());
        assertEquals(5000, Level.TRACE.toInt());
        assertEquals(7, Level.TRACE.getSyslogEquivalent());
    }

    /**
     * Tests Level.toLevel(Level.TRACE_INT).
     */
    @Test
    void testIntToTrace() {
        final Level trace = Level.toLevel(5000);
        assertEquals("TRACE", trace.toString());
    }

    /**
     * Tests Level.toLevel("TRACE");
     */
    @Test
    void testStringToTrace() {
        final Level trace = Level.toLevel("TRACE");
        assertEquals("TRACE", trace.toString());
    }

    /**
     * Tests that Level extends Priority.
     */
    @Test
    void testLevelExtendsPriority() {
        assertTrue(Priority.class.isAssignableFrom(Level.class));
    }

    /**
     * Tests Level.OFF.
     */
    @Test
    void testOFF() {
        assertInstanceOf(Level.class, Level.OFF);
    }

    /**
     * Tests Level.FATAL.
     */
    @Test
    void testFATAL() {
        assertInstanceOf(Level.class, Level.FATAL);
    }

    /**
     * Tests Level.ERROR.
     */
    @Test
    void testERROR() {
        assertInstanceOf(Level.class, Level.ERROR);
    }

    /**
     * Tests Level.WARN.
     */
    @Test
    void testWARN() {
        assertInstanceOf(Level.class, Level.WARN);
    }

    /**
     * Tests Level.INFO.
     */
    @Test
    void testINFO() {
        assertInstanceOf(Level.class, Level.INFO);
    }

    /**
     * Tests Level.DEBUG.
     */
    @Test
    void testDEBUG() {
        assertInstanceOf(Level.class, Level.DEBUG);
    }

    /**
     * Tests Level.TRACE.
     */
    @Test
    void testTRACE() {
        assertInstanceOf(Level.class, Level.TRACE);
    }

    /**
     * Tests Level.ALL.
     */
    @Test
    void testALL() {
        assertInstanceOf(Level.class, Level.ALL);
    }

    /**
     * Tests version2Level.
     */
    @ParameterizedTest
    @MethodSource("org.apache.log4j.helpers.OptionConverterLevelTest#standardLevels")
    void testVersion2Level(final Level log4j1Level, final org.apache.logging.log4j.Level log4j2Level) {
        assertEquals(log4j2Level, log4j1Level.getVersion2Level());
    }

    /**
     * Tests Level.toLevel(Level.All_INT).
     */
    @Test
    void testIntToAll() {
        final Level level = Level.toLevel(Priority.ALL_INT);
        assertEquals("ALL", level.toString());
    }

    /**
     * Tests Level.toLevel(Level.FATAL_INT).
     */
    @Test
    void testIntToFatal() {
        final Level level = Level.toLevel(Priority.FATAL_INT);
        assertEquals("FATAL", level.toString());
    }

    /**
     * Tests Level.toLevel(Level.OFF_INT).
     */
    @Test
    void testIntToOff() {
        final Level level = Level.toLevel(Priority.OFF_INT);
        assertEquals("OFF", level.toString());
    }

    /**
     * Tests Level.toLevel(17, Level.FATAL).
     */
    @Test
    void testToLevelUnrecognizedInt() {
        final Level level = Level.toLevel(17, Level.FATAL);
        assertEquals("FATAL", level.toString());
    }

    /**
     * Tests Level.toLevel(null, Level.FATAL).
     */
    @Test
    void testToLevelNull() {
        final Level level = Level.toLevel(null, Level.FATAL);
        assertEquals("FATAL", level.toString());
    }

    /**
     * Test that dotless lower I + "nfo" is recognized as INFO.
     */
    @Test
    void testDotlessLowerI() {
        final Level level = Level.toLevel("\u0131nfo");
        assertEquals("INFO", level.toString());
    }

    /**
     * Test that dotted lower I + "nfo" is recognized as INFO
     * even in Turkish locale.
     */
    @Test
    void testDottedLowerI() {
        final Locale defaultLocale = Locale.getDefault();
        final Locale turkey = new Locale("tr", "TR");
        Locale.setDefault(turkey);
        final Level level = Level.toLevel("info");
        Locale.setDefault(defaultLocale);
        assertEquals("INFO", level.toString());
    }
}
