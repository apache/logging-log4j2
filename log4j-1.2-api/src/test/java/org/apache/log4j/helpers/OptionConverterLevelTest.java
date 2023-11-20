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
package org.apache.log4j.helpers;

import static org.apache.log4j.helpers.OptionConverter.toLog4j1Level;
import static org.apache.log4j.helpers.OptionConverter.toLog4j2Level;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.stream.Stream;
import org.apache.log4j.Level;
import org.apache.log4j.Priority;
import org.apache.log4j.bridge.LogEventAdapter;
import org.apache.logging.log4j.spi.StandardLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class OptionConverterLevelTest {

    static Stream<Arguments> standardLevels() {
        return Arrays.stream(StandardLevel.values())
                .map(Enum::name)
                .map(name -> Arguments.of(Level.toLevel(name), org.apache.logging.log4j.Level.toLevel(name)));
    }

    /**
     * Test if the standard levels are transformed correctly.
     *
     * @param log4j1Level
     * @param log4j2Level
     */
    @ParameterizedTest
    @MethodSource("standardLevels")
    public void testStandardLevelConversion(final Level log4j1Level, final org.apache.logging.log4j.Level log4j2Level) {
        assertEquals(log4j2Level, OptionConverter.convertLevel(log4j1Level));
        assertEquals(log4j1Level, OptionConverter.convertLevel(log4j2Level));
    }

    /**
     * Test if the conversion works at an integer level.
     *
     * @param log4j1Level
     * @param log4j2Level
     */
    @ParameterizedTest
    @MethodSource("standardLevels")
    public void testStandardIntLevelConversion(
            final Level log4j1Level, final org.apache.logging.log4j.Level log4j2Level) {
        assertEquals(log4j2Level.intLevel(), toLog4j2Level(log4j1Level.toInt()));
        assertEquals(log4j1Level.toInt(), toLog4j1Level(log4j2Level.intLevel()));
    }

    @Test
    public void testMaxMinCutoff() {
        // The cutoff values are transformed into ALL and OFF
        assertEquals(StandardLevel.ALL.intLevel(), toLog4j2Level(OptionConverter.MIN_CUTOFF_LEVEL));
        assertEquals(StandardLevel.OFF.intLevel(), toLog4j2Level(OptionConverter.MAX_CUTOFF_LEVEL));
        // Maximal and minimal Log4j 1.x values different from ALL or OFF
        int minTransformed = toLog4j1Level(toLog4j2Level(OptionConverter.MIN_CUTOFF_LEVEL + 1));
        assertEquals(OptionConverter.MIN_CUTOFF_LEVEL + 1, minTransformed);
        int maxTransformed = toLog4j1Level(toLog4j2Level(OptionConverter.MAX_CUTOFF_LEVEL - 1));
        assertEquals(OptionConverter.MAX_CUTOFF_LEVEL - 1, maxTransformed);
        // Maximal and minimal Log4j 2.x value different from ALL or OFF
        minTransformed = toLog4j2Level(toLog4j1Level(StandardLevel.OFF.intLevel() + 1));
        assertEquals(StandardLevel.OFF.intLevel() + 1, minTransformed);
        maxTransformed = toLog4j2Level(toLog4j1Level(StandardLevel.ALL.intLevel() - 1));
        assertEquals(StandardLevel.ALL.intLevel() - 1, maxTransformed);
    }

    /**
     * Test if the values in at least the TRACE to FATAL range are transformed
     * correctly.
     */
    @Test
    public void testUsefulRange() {
        for (int intLevel = StandardLevel.OFF.intLevel(); intLevel <= StandardLevel.TRACE.intLevel(); intLevel++) {
            assertEquals(intLevel, toLog4j2Level(toLog4j1Level(intLevel)));
        }
        for (int intLevel = Level.TRACE_INT; intLevel < OptionConverter.MAX_CUTOFF_LEVEL; intLevel = intLevel + 100) {
            assertEquals(intLevel, toLog4j1Level(toLog4j2Level(intLevel)));
        }
    }

    /**
     * Levels defined in Log4j 2.x should have an equivalent in Log4j 1.x. Those are
     * used in {@link LogEventAdapter}.
     */
    @Test
    public void testCustomLog4j2Levels() {
        final int infoDebug = (StandardLevel.INFO.intLevel() + StandardLevel.DEBUG.intLevel()) / 2;
        final org.apache.logging.log4j.Level v2Level = org.apache.logging.log4j.Level.forName("INFO_DEBUG", infoDebug);
        final Level v1Level =
                OptionConverter.toLevel("INFO_DEBUG#" + org.apache.logging.log4j.Level.class.getName(), null);
        assertNotNull(v1Level);
        assertEquals(v2Level, v1Level.getVersion2Level());
        final int expectedLevel = (Priority.INFO_INT + Priority.DEBUG_INT) / 2;
        assertEquals(expectedLevel, v1Level.toInt());
        // convertLevel
        assertEquals(v1Level, OptionConverter.convertLevel(v2Level));
        // Non-existent level
        assertNull(OptionConverter.toLevel("WARN_INFO#" + org.apache.logging.log4j.Level.class.getName(), null));
    }
}
