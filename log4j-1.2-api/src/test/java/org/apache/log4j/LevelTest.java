/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

import java.util.Locale;
import org.apache.log4j.util.SerializationTestHelper;
import org.junit.Test;


/**
 * Tests of Level.
 *
 * @since 1.2.12
 */
public class LevelTest {

    /**
     * Serialize Level.INFO and check against witness.
     *
     * @throws Exception if exception during test.
     */
    @Test
    public void testSerializeINFO() throws Exception {
        final int[] skip = new int[]{};
        SerializationTestHelper.assertSerializationEquals(
            "target/test-classes/witness/serialization/info.bin",
            Level.INFO, skip, Integer.MAX_VALUE);
    }

    /**
     * Deserialize witness and see if resolved to Level.INFO.
     *
     * @throws Exception if exception during test.
     */
    @Test
    public void testDeserializeINFO() throws Exception {
        final Object obj =
            SerializationTestHelper.deserializeStream(
                "target/test-classes/witness/serialization/info.bin");
        assertThat(obj instanceof Level).isTrue();
        final Level info = (Level) obj;
        assertThat(info.toString()).isEqualTo("INFO");
        //
        //  JDK 1.1 doesn't support readResolve necessary for the assertion
        if (!System.getProperty("java.version").startsWith("1.1.")) {
            assertThat(obj == Level.INFO).isTrue();
        }
    }

    /**
     * Tests that a custom level can be serialized and deserialized
     * and is not resolved to a stock level.
     *
     * @throws Exception if exception during test.
     */
    @Test
    public void testCustomLevelSerialization() throws Exception {
        final CustomLevel custom = new CustomLevel();
        final Object obj = SerializationTestHelper.serializeClone(custom);
        assertThat(obj instanceof CustomLevel).isTrue();

        final CustomLevel clone = (CustomLevel) obj;
        assertThat(clone.level).isEqualTo(Level.INFO.level);
        assertThat(clone.levelStr).isEqualTo(Level.INFO.levelStr);
        assertThat(clone.syslogEquivalent).isEqualTo(Level.INFO.syslogEquivalent);
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
            super(
                Level.INFO.level, Level.INFO.levelStr, Level.INFO.syslogEquivalent);
        }
    }

    /**
     * Tests Level.TRACE_INT.
     */
    @Test
    public void testTraceInt() {
        assertThat(Level.TRACE_INT).isEqualTo(5000);
    }

    /**
     * Tests Level.TRACE.
     */
    @Test
    public void testTrace() {
        assertThat(Level.TRACE.toString()).isEqualTo("TRACE");
        assertThat(Level.TRACE.toInt()).isEqualTo(5000);
        assertThat(Level.TRACE.getSyslogEquivalent()).isEqualTo(7);
    }

    /**
     * Tests Level.toLevel(Level.TRACE_INT).
     */
    @Test
    public void testIntToTrace() {
        final Level trace = Level.toLevel(5000);
        assertThat(trace.toString()).isEqualTo("TRACE");
    }

    /**
     * Tests Level.toLevel("TRACE");
     */
    @Test
    public void testStringToTrace() {
        final Level trace = Level.toLevel("TRACE");
        assertThat(trace.toString()).isEqualTo("TRACE");
    }

    /**
     * Tests that Level extends Priority.
     */
    @Test
    public void testLevelExtendsPriority() {
        assertThat(Priority.class.isAssignableFrom(Level.class)).isTrue();
    }

    /**
     * Tests Level.OFF.
     */
    @Test
    public void testOFF() {
        assertThat(Level.OFF instanceof Level).isTrue();
    }

    /**
     * Tests Level.FATAL.
     */
    @Test
    public void testFATAL() {
        assertThat(Level.FATAL instanceof Level).isTrue();
    }

    /**
     * Tests Level.ERROR.
     */
    @Test
    public void testERROR() {
        assertThat(Level.ERROR instanceof Level).isTrue();
    }

    /**
     * Tests Level.WARN.
     */
    @Test
    public void testWARN() {
        assertThat(Level.WARN instanceof Level).isTrue();
    }

    /**
     * Tests Level.INFO.
     */
    @Test
    public void testINFO() {
        assertThat(Level.INFO instanceof Level).isTrue();
    }

    /**
     * Tests Level.DEBUG.
     */
    @Test
    public void testDEBUG() {
        assertThat(Level.DEBUG instanceof Level).isTrue();
    }

    /**
     * Tests Level.TRACE.
     */
    @Test
    public void testTRACE() {
        assertThat(Level.TRACE instanceof Level).isTrue();
    }

    /**
     * Tests Level.ALL.
     */
    @Test
    public void testALL() {
        assertThat(Level.ALL instanceof Level).isTrue();
    }

    /**
     * Tests Level.toLevel(Level.All_INT).
     */
    @Test
    public void testIntToAll() {
        final Level level = Level.toLevel(Priority.ALL_INT);
        assertThat(level.toString()).isEqualTo("ALL");
    }

    /**
     * Tests Level.toLevel(Level.FATAL_INT).
     */
    @Test
    public void testIntToFatal() {
        final Level level = Level.toLevel(Priority.FATAL_INT);
        assertThat(level.toString()).isEqualTo("FATAL");
    }


    /**
     * Tests Level.toLevel(Level.OFF_INT).
     */
    @Test
    public void testIntToOff() {
        final Level level = Level.toLevel(Priority.OFF_INT);
        assertThat(level.toString()).isEqualTo("OFF");
    }

    /**
     * Tests Level.toLevel(17, Level.FATAL).
     */
    @Test
    public void testToLevelUnrecognizedInt() {
        final Level level = Level.toLevel(17, Level.FATAL);
        assertThat(level.toString()).isEqualTo("FATAL");
    }

    /**
     * Tests Level.toLevel(null, Level.FATAL).
     */
    @Test
    public void testToLevelNull() {
        final Level level = Level.toLevel(null, Level.FATAL);
        assertThat(level.toString()).isEqualTo("FATAL");
    }

    /**
     * Test that dotless lower I + "nfo" is recognized as INFO.
     */
    @Test
    public void testDotlessLowerI() {
        final Level level = Level.toLevel("\u0131nfo");
        assertThat(level.toString()).isEqualTo("INFO");
    }

    /**
     * Test that dotted lower I + "nfo" is recognized as INFO
     * even in Turkish locale.
     */
    @Test
    public void testDottedLowerI() {
        final Locale defaultLocale = Locale.getDefault();
        final Locale turkey = new Locale("tr", "TR");
        Locale.setDefault(turkey);
        final Level level = Level.toLevel("info");
        Locale.setDefault(defaultLocale);
        assertThat(level.toString()).isEqualTo("INFO");
    }


}

