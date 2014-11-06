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

package org.apache.logging.log4j.jul;

import java.util.Arrays;
import java.util.Collection;

import org.apache.logging.log4j.Level;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.*;

/**
 * Tests that all JUL levels are mapped to a Log4j level.
 */
@RunWith(Parameterized.class)
public class JavaLevelTranslatorTest {

    private final java.util.logging.Level javaLevel;
    private final Level log4jLevel;

    public JavaLevelTranslatorTest(final java.util.logging.Level javaLevel, final Level log4jLevel) {
        this.javaLevel = javaLevel;
        this.log4jLevel = log4jLevel;
    }

    @Parameterized.Parameters
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

    @Test
    public void testToLevel() throws Exception {
        final Level actualLevel = LevelTranslator.toLevel(javaLevel);
        assertEquals(log4jLevel, actualLevel);
    }

    @Test
    public void testToJavaLevel() throws Exception {
        final java.util.logging.Level actualLevel = LevelTranslator.toJavaLevel(log4jLevel);
        assertEquals(javaLevel, actualLevel);
    }
}