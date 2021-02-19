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
package org.apache.logging.log4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class LevelTest {

    @Test
    public void testDefault() {
        final Level level = Level.toLevel("Information", Level.ERROR);
        assertThat(level).isNotNull();
        assertThat(level).isEqualTo(Level.ERROR);
    }

    @Test
    public void testForNameEquals() {
        final String name = "Foo";
        final int intValue = 1;
        final Level level = Level.forName(name, intValue);
        assertThat(level).isNotNull();
        assertThat(Level.forName(name, intValue)).isEqualTo(level);
        assertThat(Level.getLevel(name)).isEqualTo(level);
        assertThat(Level.getLevel(name).intLevel()).isEqualTo(intValue);
    }

    @Test
    public void testGoodLevels() {
        final Level level = Level.toLevel("INFO");
        assertThat(level).isNotNull();
        assertThat(level).isEqualTo(Level.INFO);
    }

    @Test
    public void testLevelsWithSpaces() {
        Level level = Level.toLevel(" INFO ");
        assertThat(level).isNotNull();
        assertThat(level).isEqualTo(Level.INFO);

        level = Level.valueOf(" INFO ");
        assertThat(level).isNotNull();
        assertThat(level).isEqualTo(Level.INFO);
    }

    @Test
    public void testIsInRangeErrorToDebug() {
        assertThat(Level.OFF.isInRange(Level.ERROR, Level.DEBUG)).isFalse();
        assertThat(Level.FATAL.isInRange(Level.ERROR, Level.DEBUG)).isFalse();
        assertThat(Level.ERROR.isInRange(Level.ERROR, Level.DEBUG)).isTrue();
        assertThat(Level.WARN.isInRange(Level.ERROR, Level.DEBUG)).isTrue();
        assertThat(Level.INFO.isInRange(Level.ERROR, Level.DEBUG)).isTrue();
        assertThat(Level.DEBUG.isInRange(Level.ERROR, Level.DEBUG)).isTrue();
        assertThat(Level.TRACE.isInRange(Level.ERROR, Level.DEBUG)).isFalse();
        assertThat(Level.ALL.isInRange(Level.ERROR, Level.DEBUG)).isFalse();
    }

    @Test
    public void testIsInRangeFatalToTrace() {
        assertThat(Level.OFF.isInRange(Level.FATAL, Level.TRACE)).isFalse();
        assertThat(Level.FATAL.isInRange(Level.FATAL, Level.TRACE)).isTrue();
        assertThat(Level.ERROR.isInRange(Level.FATAL, Level.TRACE)).isTrue();
        assertThat(Level.WARN.isInRange(Level.FATAL, Level.TRACE)).isTrue();
        assertThat(Level.INFO.isInRange(Level.FATAL, Level.TRACE)).isTrue();
        assertThat(Level.DEBUG.isInRange(Level.FATAL, Level.TRACE)).isTrue();
        assertThat(Level.TRACE.isInRange(Level.FATAL, Level.TRACE)).isTrue();
        assertThat(Level.ALL.isInRange(Level.FATAL, Level.TRACE)).isFalse();
    }

    @Test
    public void testIsInRangeOffToAll() {
        assertThat(Level.OFF.isInRange(Level.OFF, Level.ALL)).isTrue();
        assertThat(Level.FATAL.isInRange(Level.OFF, Level.ALL)).isTrue();
        assertThat(Level.ERROR.isInRange(Level.OFF, Level.ALL)).isTrue();
        assertThat(Level.WARN.isInRange(Level.OFF, Level.ALL)).isTrue();
        assertThat(Level.INFO.isInRange(Level.OFF, Level.ALL)).isTrue();
        assertThat(Level.DEBUG.isInRange(Level.OFF, Level.ALL)).isTrue();
        assertThat(Level.TRACE.isInRange(Level.OFF, Level.ALL)).isTrue();
        assertThat(Level.ALL.isInRange(Level.OFF, Level.ALL)).isTrue();
    }

    @Test
    public void testIsInRangeSameLevels() {
        // Level.OFF
        assertThat(Level.OFF.isInRange(Level.OFF, Level.OFF)).isTrue();
        assertThat(Level.OFF.isInRange(Level.FATAL, Level.FATAL)).isFalse();
        assertThat(Level.OFF.isInRange(Level.ERROR, Level.ERROR)).isFalse();
        assertThat(Level.OFF.isInRange(Level.WARN, Level.WARN)).isFalse();
        assertThat(Level.OFF.isInRange(Level.INFO, Level.INFO)).isFalse();
        assertThat(Level.OFF.isInRange(Level.DEBUG, Level.DEBUG)).isFalse();
        assertThat(Level.OFF.isInRange(Level.TRACE, Level.TRACE)).isFalse();
        assertThat(Level.OFF.isInRange(Level.ALL, Level.ALL)).isFalse();
        // Level.FATAL
        assertThat(Level.FATAL.isInRange(Level.OFF, Level.OFF)).isFalse();
        assertThat(Level.FATAL.isInRange(Level.FATAL, Level.FATAL)).isTrue();
        assertThat(Level.FATAL.isInRange(Level.ERROR, Level.ERROR)).isFalse();
        assertThat(Level.FATAL.isInRange(Level.WARN, Level.WARN)).isFalse();
        assertThat(Level.FATAL.isInRange(Level.INFO, Level.INFO)).isFalse();
        assertThat(Level.FATAL.isInRange(Level.DEBUG, Level.DEBUG)).isFalse();
        assertThat(Level.FATAL.isInRange(Level.TRACE, Level.TRACE)).isFalse();
        assertThat(Level.FATAL.isInRange(Level.ALL, Level.ALL)).isFalse();
        // Level.ERROR
        assertThat(Level.ERROR.isInRange(Level.OFF, Level.OFF)).isFalse();
        assertThat(Level.ERROR.isInRange(Level.FATAL, Level.FATAL)).isFalse();
        assertThat(Level.ERROR.isInRange(Level.ERROR, Level.ERROR)).isTrue();
        assertThat(Level.ERROR.isInRange(Level.WARN, Level.WARN)).isFalse();
        assertThat(Level.ERROR.isInRange(Level.INFO, Level.INFO)).isFalse();
        assertThat(Level.ERROR.isInRange(Level.DEBUG, Level.DEBUG)).isFalse();
        assertThat(Level.ERROR.isInRange(Level.TRACE, Level.TRACE)).isFalse();
        assertThat(Level.ERROR.isInRange(Level.ALL, Level.ALL)).isFalse();
        // Level.WARN
        assertThat(Level.WARN.isInRange(Level.OFF, Level.OFF)).isFalse();
        assertThat(Level.WARN.isInRange(Level.FATAL, Level.FATAL)).isFalse();
        assertThat(Level.WARN.isInRange(Level.ERROR, Level.ERROR)).isFalse();
        assertThat(Level.WARN.isInRange(Level.WARN, Level.WARN)).isTrue();
        assertThat(Level.WARN.isInRange(Level.INFO, Level.INFO)).isFalse();
        assertThat(Level.WARN.isInRange(Level.DEBUG, Level.DEBUG)).isFalse();
        assertThat(Level.WARN.isInRange(Level.TRACE, Level.TRACE)).isFalse();
        assertThat(Level.WARN.isInRange(Level.ALL, Level.ALL)).isFalse();
        // Level.INFO
        assertThat(Level.INFO.isInRange(Level.OFF, Level.OFF)).isFalse();
        assertThat(Level.INFO.isInRange(Level.FATAL, Level.FATAL)).isFalse();
        assertThat(Level.INFO.isInRange(Level.ERROR, Level.ERROR)).isFalse();
        assertThat(Level.INFO.isInRange(Level.WARN, Level.WARN)).isFalse();
        assertThat(Level.INFO.isInRange(Level.INFO, Level.INFO)).isTrue();
        assertThat(Level.INFO.isInRange(Level.DEBUG, Level.DEBUG)).isFalse();
        assertThat(Level.INFO.isInRange(Level.TRACE, Level.TRACE)).isFalse();
        assertThat(Level.INFO.isInRange(Level.ALL, Level.ALL)).isFalse();
        // Level.DEBUG
        assertThat(Level.DEBUG.isInRange(Level.OFF, Level.OFF)).isFalse();
        assertThat(Level.DEBUG.isInRange(Level.FATAL, Level.FATAL)).isFalse();
        assertThat(Level.DEBUG.isInRange(Level.ERROR, Level.ERROR)).isFalse();
        assertThat(Level.DEBUG.isInRange(Level.WARN, Level.WARN)).isFalse();
        assertThat(Level.DEBUG.isInRange(Level.INFO, Level.INFO)).isFalse();
        assertThat(Level.DEBUG.isInRange(Level.DEBUG, Level.DEBUG)).isTrue();
        assertThat(Level.DEBUG.isInRange(Level.TRACE, Level.TRACE)).isFalse();
        assertThat(Level.DEBUG.isInRange(Level.ALL, Level.ALL)).isFalse();
        // Level.TRACE
        assertThat(Level.TRACE.isInRange(Level.OFF, Level.OFF)).isFalse();
        assertThat(Level.TRACE.isInRange(Level.FATAL, Level.FATAL)).isFalse();
        assertThat(Level.TRACE.isInRange(Level.ERROR, Level.ERROR)).isFalse();
        assertThat(Level.TRACE.isInRange(Level.WARN, Level.WARN)).isFalse();
        assertThat(Level.TRACE.isInRange(Level.INFO, Level.INFO)).isFalse();
        assertThat(Level.TRACE.isInRange(Level.DEBUG, Level.DEBUG)).isFalse();
        assertThat(Level.TRACE.isInRange(Level.TRACE, Level.TRACE)).isTrue();
        assertThat(Level.TRACE.isInRange(Level.ALL, Level.ALL)).isFalse();
        // Level.ALL
        assertThat(Level.ALL.isInRange(Level.OFF, Level.OFF)).isFalse();
        assertThat(Level.ALL.isInRange(Level.FATAL, Level.FATAL)).isFalse();
        assertThat(Level.ALL.isInRange(Level.ERROR, Level.ERROR)).isFalse();
        assertThat(Level.ALL.isInRange(Level.WARN, Level.WARN)).isFalse();
        assertThat(Level.ALL.isInRange(Level.INFO, Level.INFO)).isFalse();
        assertThat(Level.ALL.isInRange(Level.DEBUG, Level.DEBUG)).isFalse();
        assertThat(Level.ALL.isInRange(Level.TRACE, Level.TRACE)).isFalse();
        assertThat(Level.ALL.isInRange(Level.ALL, Level.ALL)).isTrue();
    }

    @Test
    public void testIsInRangeWarnToInfo() {
        assertThat(Level.OFF.isInRange(Level.WARN, Level.INFO)).isFalse();
        assertThat(Level.FATAL.isInRange(Level.WARN, Level.INFO)).isFalse();
        assertThat(Level.ERROR.isInRange(Level.WARN, Level.INFO)).isFalse();
        assertThat(Level.WARN.isInRange(Level.WARN, Level.INFO)).isTrue();
        assertThat(Level.INFO.isInRange(Level.WARN, Level.INFO)).isTrue();
        assertThat(Level.DEBUG.isInRange(Level.WARN, Level.INFO)).isFalse();
        assertThat(Level.TRACE.isInRange(Level.WARN, Level.INFO)).isFalse();
        assertThat(Level.ALL.isInRange(Level.WARN, Level.INFO)).isFalse();
    }

    @Test
    public void testIsLessSpecificThan() {
        // Level.OFF
        assertThat(Level.OFF.isLessSpecificThan(Level.OFF)).isTrue();
        assertThat(Level.OFF.isLessSpecificThan(Level.FATAL)).isFalse();
        assertThat(Level.OFF.isLessSpecificThan(Level.ERROR)).isFalse();
        assertThat(Level.OFF.isLessSpecificThan(Level.WARN)).isFalse();
        assertThat(Level.OFF.isLessSpecificThan(Level.INFO)).isFalse();
        assertThat(Level.OFF.isLessSpecificThan(Level.DEBUG)).isFalse();
        assertThat(Level.OFF.isLessSpecificThan(Level.TRACE)).isFalse();
        assertThat(Level.OFF.isLessSpecificThan(Level.ALL)).isFalse();
        // Level.FATAL
        assertThat(Level.FATAL.isLessSpecificThan(Level.OFF)).isTrue();
        assertThat(Level.FATAL.isLessSpecificThan(Level.FATAL)).isTrue();
        assertThat(Level.FATAL.isLessSpecificThan(Level.ERROR)).isFalse();
        assertThat(Level.FATAL.isLessSpecificThan(Level.WARN)).isFalse();
        assertThat(Level.FATAL.isLessSpecificThan(Level.INFO)).isFalse();
        assertThat(Level.FATAL.isLessSpecificThan(Level.DEBUG)).isFalse();
        assertThat(Level.FATAL.isLessSpecificThan(Level.TRACE)).isFalse();
        assertThat(Level.FATAL.isLessSpecificThan(Level.ALL)).isFalse();
        // Level.ERROR
        assertThat(Level.ERROR.isLessSpecificThan(Level.OFF)).isTrue();
        assertThat(Level.ERROR.isLessSpecificThan(Level.FATAL)).isTrue();
        assertThat(Level.ERROR.isLessSpecificThan(Level.ERROR)).isTrue();
        assertThat(Level.ERROR.isLessSpecificThan(Level.WARN)).isFalse();
        assertThat(Level.ERROR.isLessSpecificThan(Level.INFO)).isFalse();
        assertThat(Level.ERROR.isLessSpecificThan(Level.DEBUG)).isFalse();
        assertThat(Level.ERROR.isLessSpecificThan(Level.TRACE)).isFalse();
        assertThat(Level.ERROR.isLessSpecificThan(Level.ALL)).isFalse();
        // Level.ERROR
        assertThat(Level.WARN.isLessSpecificThan(Level.OFF)).isTrue();
        assertThat(Level.WARN.isLessSpecificThan(Level.FATAL)).isTrue();
        assertThat(Level.WARN.isLessSpecificThan(Level.ERROR)).isTrue();
        assertThat(Level.WARN.isLessSpecificThan(Level.WARN)).isTrue();
        assertThat(Level.WARN.isLessSpecificThan(Level.INFO)).isFalse();
        assertThat(Level.WARN.isLessSpecificThan(Level.DEBUG)).isFalse();
        assertThat(Level.WARN.isLessSpecificThan(Level.TRACE)).isFalse();
        assertThat(Level.WARN.isLessSpecificThan(Level.ALL)).isFalse();
        // Level.WARN
        assertThat(Level.WARN.isLessSpecificThan(Level.OFF)).isTrue();
        assertThat(Level.WARN.isLessSpecificThan(Level.FATAL)).isTrue();
        assertThat(Level.WARN.isLessSpecificThan(Level.ERROR)).isTrue();
        assertThat(Level.WARN.isLessSpecificThan(Level.WARN)).isTrue();
        assertThat(Level.WARN.isLessSpecificThan(Level.INFO)).isFalse();
        assertThat(Level.WARN.isLessSpecificThan(Level.DEBUG)).isFalse();
        assertThat(Level.WARN.isLessSpecificThan(Level.TRACE)).isFalse();
        assertThat(Level.WARN.isLessSpecificThan(Level.ALL)).isFalse();
        // Level.INFO
        assertThat(Level.INFO.isLessSpecificThan(Level.OFF)).isTrue();
        assertThat(Level.INFO.isLessSpecificThan(Level.FATAL)).isTrue();
        assertThat(Level.INFO.isLessSpecificThan(Level.ERROR)).isTrue();
        assertThat(Level.INFO.isLessSpecificThan(Level.WARN)).isTrue();
        assertThat(Level.INFO.isLessSpecificThan(Level.INFO)).isTrue();
        assertThat(Level.INFO.isLessSpecificThan(Level.DEBUG)).isFalse();
        assertThat(Level.INFO.isLessSpecificThan(Level.TRACE)).isFalse();
        assertThat(Level.INFO.isLessSpecificThan(Level.ALL)).isFalse();
        // Level.DEBUG
        assertThat(Level.DEBUG.isLessSpecificThan(Level.OFF)).isTrue();
        assertThat(Level.DEBUG.isLessSpecificThan(Level.FATAL)).isTrue();
        assertThat(Level.DEBUG.isLessSpecificThan(Level.ERROR)).isTrue();
        assertThat(Level.DEBUG.isLessSpecificThan(Level.WARN)).isTrue();
        assertThat(Level.DEBUG.isLessSpecificThan(Level.INFO)).isTrue();
        assertThat(Level.DEBUG.isLessSpecificThan(Level.DEBUG)).isTrue();
        assertThat(Level.DEBUG.isLessSpecificThan(Level.TRACE)).isFalse();
        assertThat(Level.DEBUG.isLessSpecificThan(Level.ALL)).isFalse();
        // Level.TRACE
        assertThat(Level.TRACE.isLessSpecificThan(Level.OFF)).isTrue();
        assertThat(Level.TRACE.isLessSpecificThan(Level.FATAL)).isTrue();
        assertThat(Level.TRACE.isLessSpecificThan(Level.ERROR)).isTrue();
        assertThat(Level.TRACE.isLessSpecificThan(Level.WARN)).isTrue();
        assertThat(Level.TRACE.isLessSpecificThan(Level.INFO)).isTrue();
        assertThat(Level.TRACE.isLessSpecificThan(Level.DEBUG)).isTrue();
        assertThat(Level.TRACE.isLessSpecificThan(Level.TRACE)).isTrue();
        assertThat(Level.TRACE.isLessSpecificThan(Level.ALL)).isFalse();
        // Level.ALL
        assertThat(Level.ALL.isLessSpecificThan(Level.OFF)).isTrue();
        assertThat(Level.ALL.isLessSpecificThan(Level.FATAL)).isTrue();
        assertThat(Level.ALL.isLessSpecificThan(Level.ERROR)).isTrue();
        assertThat(Level.ALL.isLessSpecificThan(Level.WARN)).isTrue();
        assertThat(Level.ALL.isLessSpecificThan(Level.INFO)).isTrue();
        assertThat(Level.ALL.isLessSpecificThan(Level.DEBUG)).isTrue();
        assertThat(Level.ALL.isLessSpecificThan(Level.TRACE)).isTrue();
        assertThat(Level.ALL.isLessSpecificThan(Level.ALL)).isTrue();
    }

}
