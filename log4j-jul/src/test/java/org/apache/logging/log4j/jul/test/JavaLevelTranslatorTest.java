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
package org.apache.logging.log4j.jul.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.jul.LevelTranslator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests that all JUL levels are mapped to a Log4j level.
 */
class JavaLevelTranslatorTest {

    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            // All 9 JUL levels, All 8 Log4j levels and extras
            // @formatter:off
            {java.util.logging.Level.OFF, Level.OFF},
            {java.util.logging.Level.SEVERE, Level.ERROR},
            {java.util.logging.Level.WARNING, Level.WARN},
            {java.util.logging.Level.INFO, Level.INFO},
            {java.util.logging.Level.CONFIG, LevelTranslator.CONFIG},
            {java.util.logging.Level.FINE, Level.DEBUG},
            {java.util.logging.Level.FINER, Level.TRACE},
            {java.util.logging.Level.FINEST, LevelTranslator.FINEST},
            {java.util.logging.Level.ALL, Level.ALL}
            // @formatter:on
        });
    }

    @MethodSource("data")
    @ParameterizedTest
    void testToLevel(final java.util.logging.Level javaLevel, final Level log4jLevel) {
        final Level actualLevel = LevelTranslator.toLevel(javaLevel);
        assertEquals(log4jLevel, actualLevel);
    }

    @MethodSource("data")
    @ParameterizedTest
    void testToJavaLevel(final java.util.logging.Level javaLevel, final Level log4jLevel) {
        final java.util.logging.Level actualLevel = LevelTranslator.toJavaLevel(log4jLevel);
        assertEquals(javaLevel, actualLevel);
    }
}
