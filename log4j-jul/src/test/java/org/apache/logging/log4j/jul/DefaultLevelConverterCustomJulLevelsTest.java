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

import org.apache.logging.log4j.Level;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests {@link DefaultLevelConverter} for custom JUL levels.
 * 
 * @since 2.4
 */
public class DefaultLevelConverterCustomJulLevelsTest {

    static class CustomLevel extends java.util.logging.Level {

        private static final long serialVersionUID = 1L;

        static CustomLevel ALL_P_1 = new CustomLevel("ALL_P_1", java.util.logging.Level.ALL.intValue() + 1);

        static CustomLevel FINEST_P_1 = new CustomLevel("FINEST_P_1", java.util.logging.Level.FINEST.intValue() + 1);
        static CustomLevel FINEST_M_1 = new CustomLevel("FINEST_M_1", java.util.logging.Level.FINEST.intValue() - 1);

        static CustomLevel FINER_P_1 = new CustomLevel("FINER_P_1", java.util.logging.Level.FINER.intValue() + 1);
        static CustomLevel FINER_M_1 = new CustomLevel("FINER_M_1", java.util.logging.Level.FINER.intValue() - 1);

        static CustomLevel FINE_P_1 = new CustomLevel("FINE_P_1", java.util.logging.Level.FINE.intValue() + 1);
        static CustomLevel FINE_M_1 = new CustomLevel("FINE_M_1", java.util.logging.Level.FINE.intValue() - 1);

        static CustomLevel CONFIG_P_1 = new CustomLevel("CONFIG_P_1", java.util.logging.Level.CONFIG.intValue() + 1);
        static CustomLevel CONFIG_M_1 = new CustomLevel("CONFIG_M_1", java.util.logging.Level.CONFIG.intValue() - 1);

        static CustomLevel INFO_P_1 = new CustomLevel("INFO_P_1", java.util.logging.Level.INFO.intValue() + 1);
        static CustomLevel INFO_M_1 = new CustomLevel("INFO_M_1", java.util.logging.Level.INFO.intValue() - 1);

        static CustomLevel WARNING_P_1 = new CustomLevel("WARNING_P_1", java.util.logging.Level.WARNING.intValue() + 1);
        static CustomLevel WARNING_M_1 = new CustomLevel("WARNING_M_1", java.util.logging.Level.WARNING.intValue() - 1);

        static CustomLevel SEVERE_P_1 = new CustomLevel("SEVERE_P_1", java.util.logging.Level.SEVERE.intValue() + 1);
        static CustomLevel SEVERE_M_1 = new CustomLevel("SEVERE_M_1", java.util.logging.Level.SEVERE.intValue() - 1);

        static CustomLevel OFF_M_1 = new CustomLevel("OFF_M_1", java.util.logging.Level.OFF.intValue() - 1);

        protected CustomLevel(final String name, final int value) {
            super(name, value);
        }
    }

    private final DefaultLevelConverter converter = new DefaultLevelConverter();

    @Test
    public void testCustomJulLevelNearAll() {
        // Sanity check:
        Assert.assertEquals(Level.ALL, converter.toLevel(java.util.logging.Level.ALL));
        // Test:
        Assert.assertEquals(Level.ALL, converter.toLevel(CustomLevel.ALL_P_1));
    }

    @Test
    public void testCustomJulLevelNearFinest() {
        // Sanity check:
        Assert.assertEquals(LevelTranslator.FINEST, converter.toLevel(java.util.logging.Level.FINEST));
        // Test:
        Assert.assertEquals(LevelTranslator.FINEST, converter.toLevel(CustomLevel.FINEST_P_1));
        Assert.assertEquals(LevelTranslator.FINEST, converter.toLevel(CustomLevel.FINEST_M_1));
    }

    @Test
    public void testCustomJulLevelNearFiner() {
        // Sanity check:
        Assert.assertEquals(Level.TRACE, converter.toLevel(java.util.logging.Level.FINER));
        // Test:
        Assert.assertEquals(Level.TRACE, converter.toLevel(CustomLevel.FINER_P_1));
        Assert.assertEquals(Level.TRACE, converter.toLevel(CustomLevel.FINER_M_1));
    }

    @Test
    public void testCustomJulLevelNearFine() {
        // Sanity check:
        Assert.assertEquals(Level.DEBUG, converter.toLevel(java.util.logging.Level.FINE));
        // Test:
        Assert.assertEquals(Level.DEBUG, converter.toLevel(CustomLevel.FINE_P_1));
        Assert.assertEquals(Level.DEBUG, converter.toLevel(CustomLevel.FINE_M_1));
    }

    @Test
    public void testCustomJulLevelNearConfig() {
        // Sanity check:
        Assert.assertEquals(LevelTranslator.CONFIG, converter.toLevel(java.util.logging.Level.CONFIG));
        // Test:
        Assert.assertEquals(LevelTranslator.CONFIG, converter.toLevel(CustomLevel.CONFIG_P_1));
        Assert.assertEquals(LevelTranslator.CONFIG, converter.toLevel(CustomLevel.CONFIG_M_1));
    }

    @Test
    public void testCustomJulLevelNearInfo() {
        // Sanity check:
        Assert.assertEquals(Level.INFO, converter.toLevel(java.util.logging.Level.INFO));
        // Test:
        Assert.assertEquals(Level.INFO, converter.toLevel(CustomLevel.INFO_P_1));
        Assert.assertEquals(Level.INFO, converter.toLevel(CustomLevel.INFO_M_1));
    }

    @Test
    public void testCustomJulLevelNearWarning() {
        // Sanity check:
        Assert.assertEquals(Level.WARN, converter.toLevel(java.util.logging.Level.WARNING));
        // Test:
        Assert.assertEquals(Level.WARN, converter.toLevel(CustomLevel.WARNING_P_1));
        Assert.assertEquals(Level.WARN, converter.toLevel(CustomLevel.WARNING_M_1));
    }

    @Test
    public void testCustomJulLevelNearSevere() {
        // Sanity check:
        Assert.assertEquals(Level.ERROR, converter.toLevel(java.util.logging.Level.SEVERE));
        // Test:
        Assert.assertEquals(Level.ERROR, converter.toLevel(CustomLevel.SEVERE_P_1));
        Assert.assertEquals(Level.ERROR, converter.toLevel(CustomLevel.SEVERE_M_1));
    }

    @Test
    public void testCustomJulLevelNearOff() {
        // Sanity check:
        Assert.assertEquals(Level.OFF, converter.toLevel(java.util.logging.Level.OFF));
        // Test:
        Assert.assertEquals(Level.OFF, converter.toLevel(CustomLevel.OFF_M_1));
    }
}
