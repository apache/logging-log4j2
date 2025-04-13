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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests of Priority.
 *
 */
class PriorityTest {

    /**
     * Tests Priority.OFF_INT.
     */
    @Test
    void testOffInt() {
        assertEquals(Integer.MAX_VALUE, Priority.OFF_INT);
    }

    /**
     * Tests Priority.FATAL_INT.
     */
    @Test
    void testFatalInt() {
        assertEquals(50000, Priority.FATAL_INT);
    }

    /**
     * Tests Priority.ERROR_INT.
     */
    @Test
    void testErrorInt() {
        assertEquals(40000, Priority.ERROR_INT);
    }

    /**
     * Tests Priority.WARN_INT.
     */
    @Test
    void testWarnInt() {
        assertEquals(30000, Priority.WARN_INT);
    }

    /**
     * Tests Priority.INFO_INT.
     */
    @Test
    void testInfoInt() {
        assertEquals(20000, Priority.INFO_INT);
    }

    /**
     * Tests Priority.DEBUG_INT.
     */
    @Test
    void testDebugInt() {
        assertEquals(10000, Priority.DEBUG_INT);
    }

    /**
     * Tests Priority.ALL_INT.
     */
    @Test
    void testAllInt() {
        assertEquals(Integer.MIN_VALUE, Priority.ALL_INT);
    }

    @SuppressWarnings("deprecation")
    static Stream<Arguments> testVersion2Level() {
        return Stream.of(
                Arguments.of(Priority.FATAL, org.apache.logging.log4j.Level.FATAL),
                Arguments.of(Priority.ERROR, org.apache.logging.log4j.Level.ERROR),
                Arguments.of(Priority.WARN, org.apache.logging.log4j.Level.WARN),
                Arguments.of(Priority.INFO, org.apache.logging.log4j.Level.INFO),
                Arguments.of(Priority.DEBUG, org.apache.logging.log4j.Level.DEBUG));
    }

    /**
     * Tests version2Level.
     */
    @ParameterizedTest
    @MethodSource()
    void testVersion2Level(final Priority log4j1Priority, final org.apache.logging.log4j.Level log4j2Level) {
        assertEquals(log4j2Level, log4j1Priority.getVersion2Level());
    }

    /**
     * Tests Priority.FATAL.
     */
    @Test
    @SuppressWarnings("deprecation")
    void testFATAL() {
        assertFalse(Priority.FATAL instanceof Level);
    }

    /**
     * Tests Priority.ERROR.
     */
    @Test
    @SuppressWarnings("deprecation")
    void testERROR() {
        assertFalse(Priority.ERROR instanceof Level);
    }

    /**
     * Tests Priority.WARN.
     */
    @Test
    @SuppressWarnings("deprecation")
    void testWARN() {
        assertFalse(Priority.WARN instanceof Level);
    }

    /**
     * Tests Priority.INFO.
     */
    @Test
    @SuppressWarnings("deprecation")
    void testINFO() {
        assertFalse(Priority.INFO instanceof Level);
    }

    /**
     * Tests Priority.DEBUG.
     */
    @Test
    @SuppressWarnings("deprecation")
    void testDEBUG() {
        assertFalse(Priority.DEBUG instanceof Level);
    }

    /**
     * Tests Priority.equals(null).
     */
    @Test
    @SuppressWarnings("deprecation")
    void testEqualsNull() {
        assertNotEquals(null, Priority.DEBUG);
    }

    /**
     * Tests Priority.equals(Level.DEBUG).
     */
    @Test
    @SuppressWarnings("deprecation")
    void testEqualsLevel() {
        //
        //   this behavior violates the equals contract.
        //
        assertEquals(Priority.DEBUG, Level.DEBUG);
    }

    /**
     * Tests getAllPossiblePriorities().
     */
    @Test
    @SuppressWarnings("deprecation")
    void testGetAllPossiblePriorities() {
        final Priority[] priorities = Priority.getAllPossiblePriorities();
        assertEquals(5, priorities.length);
    }

    /**
     * Tests toPriority(String).
     */
    @Test
    @SuppressWarnings("deprecation")
    void testToPriorityString() {
        assertEquals(Level.DEBUG, Priority.toPriority("DEBUG"));
    }

    /**
     * Tests toPriority(int).
     */
    @Test
    @SuppressWarnings("deprecation")
    void testToPriorityInt() {
        assertEquals(Level.DEBUG, Priority.toPriority(Priority.DEBUG_INT));
    }

    /**
     * Tests toPriority(String, Priority).
     */
    @Test
    @SuppressWarnings("deprecation")
    void testToPriorityStringPriority() {
        assertEquals(Priority.DEBUG, Priority.toPriority("foo", Priority.DEBUG));
    }

    /**
     * Tests toPriority(int, Priority).
     */
    @Test
    @SuppressWarnings("deprecation")
    void testToPriorityIntPriority() {
        assertEquals(Priority.DEBUG, Priority.toPriority(17, Priority.DEBUG));
    }

    /**
     * Test that dotless lower I + "nfo" is recognized as INFO.
     */
    @Test
    @SuppressWarnings("deprecation")
    void testDotlessLowerI() {
        final Priority level = Priority.toPriority("\u0131nfo");
        assertEquals("INFO", level.toString());
    }

    /**
     * Test that dotted lower I + "nfo" is recognized as INFO
     * even in Turkish locale.
     */
    @Test
    @SuppressWarnings("deprecation")
    void testDottedLowerI() {
        final Locale defaultLocale = Locale.getDefault();
        final Locale turkey = new Locale("tr", "TR");
        Locale.setDefault(turkey);
        final Priority level = Priority.toPriority("info");
        Locale.setDefault(defaultLocale);
        assertEquals("INFO", level.toString());
    }
}
