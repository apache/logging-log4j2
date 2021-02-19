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
import org.junit.Test;

/**
 * Tests of Priority.
 *
 */
public class PriorityTest {

    /**
     * Tests Priority.OFF_INT.
     */
    @Test
    public void testOffInt() {
        assertThat(Priority.OFF_INT).isEqualTo(Integer.MAX_VALUE);
    }

    /**
     * Tests Priority.FATAL_INT.
     */
    @Test
    public void testFatalInt() {
        assertThat(Priority.FATAL_INT).isEqualTo(50000);
    }

    /**
     * Tests Priority.ERROR_INT.
     */
    @Test
    public void testErrorInt() {
        assertThat(Priority.ERROR_INT).isEqualTo(40000);
    }

    /**
     * Tests Priority.WARN_INT.
     */
    @Test
    public void testWarnInt() {
        assertThat(Priority.WARN_INT).isEqualTo(30000);
    }

    /**
     * Tests Priority.INFO_INT.
     */
    @Test
    public void testInfoInt() {
        assertThat(Priority.INFO_INT).isEqualTo(20000);
    }

    /**
     * Tests Priority.DEBUG_INT.
     */
    @Test
    public void testDebugInt() {
        assertThat(Priority.DEBUG_INT).isEqualTo(10000);
    }

    /**
     * Tests Priority.ALL_INT.
     */
    @Test
    public void testAllInt() {
        assertThat(Priority.ALL_INT).isEqualTo(Integer.MIN_VALUE);
    }

    /**
     * Tests Priority.FATAL.
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testFatal() {
        assertThat(Priority.FATAL instanceof Level).isTrue();
    }

    /**
     * Tests Priority.ERROR.
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testERROR() {
        assertThat(Priority.ERROR instanceof Level).isTrue();
    }

    /**
     * Tests Priority.WARN.
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testWARN() {
        assertThat(Priority.WARN instanceof Level).isTrue();
    }

    /**
     * Tests Priority.INFO.
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testINFO() {
        assertThat(Priority.INFO instanceof Level).isTrue();
    }

    /**
     * Tests Priority.DEBUG.
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testDEBUG() {
        assertThat(Priority.DEBUG instanceof Level).isTrue();
    }

    /**
     * Tests Priority.equals(null).
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testEqualsNull() {
        assertThat(Priority.DEBUG.equals(null)).isFalse();
    }

    /**
     * Tests Priority.equals(Level.DEBUG).
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testEqualsLevel() {
        //
        //   this behavior violates the equals contract.
        //
        assertThat(Priority.DEBUG.equals(Level.DEBUG)).isTrue();
    }

    /**
     * Tests getAllPossiblePriorities().
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testGetAllPossiblePriorities() {
        final Priority[] priorities = Priority.getAllPossiblePriorities();
        assertThat(priorities).hasSize(5);
    }

    /**
     * Tests toPriority(String).
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testToPriorityString() {
        assertThat(Priority.toPriority("DEBUG") == Level.DEBUG).isTrue();
    }

    /**
     * Tests toPriority(int).
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testToPriorityInt() {
        assertThat(Priority.toPriority(Priority.DEBUG_INT) == Level.DEBUG).isTrue();
    }

    /**
     * Tests toPriority(String, Priority).
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testToPriorityStringPriority() {
        assertThat(Priority.toPriority("foo", Priority.DEBUG) == Priority.DEBUG).isTrue();
    }

    /**
     * Tests toPriority(int, Priority).
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testToPriorityIntPriority() {
        assertThat(Priority.toPriority(17, Priority.DEBUG) == Priority.DEBUG).isTrue();
    }

    /**
     * Test that dotless lower I + "nfo" is recognized as INFO.
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testDotlessLowerI() {
        final Priority level = Priority.toPriority("\u0131nfo");
        assertThat(level.toString()).isEqualTo("INFO");
    }

    /**
     * Test that dotted lower I + "nfo" is recognized as INFO
     * even in Turkish locale.
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testDottedLowerI() {
        final Locale defaultLocale = Locale.getDefault();
        final Locale turkey = new Locale("tr", "TR");
        Locale.setDefault(turkey);
        final Priority level = Priority.toPriority("info");
        Locale.setDefault(defaultLocale);
        assertThat(level.toString()).isEqualTo("INFO");
  }

}

