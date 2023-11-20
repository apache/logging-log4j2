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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;
import org.apache.log4j.Level;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for UtilLoggingLevel.
 */
public class UtilLoggingLevelTest {

    /**
     * Test toLevel("fiNeSt").
     */
    public void testToLevelFINEST() {
        assertEquals(UtilLoggingLevel.FINEST, UtilLoggingLevel.toLevel("fiNeSt"));
    }

    static Stream<Arguments> namesAndLevels() {
        return UtilLoggingLevel.getAllPossibleLevels().stream()
                .map(level -> Arguments.of(level.toString() + "#" + UtilLoggingLevel.class.getName(), level));
    }

    @ParameterizedTest
    @MethodSource("namesAndLevels")
    public void testOptionConverterToLevel(final String name, final UtilLoggingLevel level) {
        assertTrue(level == OptionConverter.toLevel(name, Level.ALL), "get v1 level by name");
        // Comparison of Log4j 2.x levels
        assertTrue(level.getVersion2Level() == org.apache.logging.log4j.Level.getLevel(name), "get v2 level by name");
        // Test convertLevel
        assertTrue(level == OptionConverter.convertLevel(level.getVersion2Level()), "convert level v2 -> v1");
    }
}
