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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 *
 */
public class LevelTest {

    @Test
    public void testDefault() {
        final Level level = Level.toLevel("Information", Level.ERROR);
        assertNotNull(level);
        assertEquals(Level.ERROR, level);
    }

    @Test
    public void testForNameEquals() {
        final String name = "Foo";
        final int intValue = 1;
        final Level level = Level.forName(name, intValue);
        assertNotNull(level);
        assertEquals(level, Level.forName(name, intValue));
        assertEquals(level, Level.getLevel(name));
        assertEquals(intValue, Level.getLevel(name).intLevel());
    }

    @Test
    public void testGoodLevels() {
        final Level level = Level.toLevel("INFO");
        assertNotNull(level);
        assertEquals(Level.INFO, level);
    }

    @Test
    public void testIsInRangeErrorToDebug() {
        assertFalse(Level.OFF.isInRange(Level.ERROR, Level.DEBUG));
        assertFalse(Level.FATAL.isInRange(Level.ERROR, Level.DEBUG));
        assertTrue(Level.ERROR.isInRange(Level.ERROR, Level.DEBUG));
        assertTrue(Level.WARN.isInRange(Level.ERROR, Level.DEBUG));
        assertTrue(Level.INFO.isInRange(Level.ERROR, Level.DEBUG));
        assertTrue(Level.DEBUG.isInRange(Level.ERROR, Level.DEBUG));
        assertFalse(Level.TRACE.isInRange(Level.ERROR, Level.DEBUG));
        assertFalse(Level.ALL.isInRange(Level.ERROR, Level.DEBUG));
    }

    @Test
    public void testIsInRangeFatalToTrace() {
        assertFalse(Level.OFF.isInRange(Level.FATAL, Level.TRACE));
        assertTrue(Level.FATAL.isInRange(Level.FATAL, Level.TRACE));
        assertTrue(Level.ERROR.isInRange(Level.FATAL, Level.TRACE));
        assertTrue(Level.WARN.isInRange(Level.FATAL, Level.TRACE));
        assertTrue(Level.INFO.isInRange(Level.FATAL, Level.TRACE));
        assertTrue(Level.DEBUG.isInRange(Level.FATAL, Level.TRACE));
        assertTrue(Level.TRACE.isInRange(Level.FATAL, Level.TRACE));
        assertFalse(Level.ALL.isInRange(Level.FATAL, Level.TRACE));
    }

    @Test
    public void testIsInRangeOffToAll() {
        assertTrue(Level.OFF.isInRange(Level.OFF, Level.ALL));
        assertTrue(Level.FATAL.isInRange(Level.OFF, Level.ALL));
        assertTrue(Level.ERROR.isInRange(Level.OFF, Level.ALL));
        assertTrue(Level.WARN.isInRange(Level.OFF, Level.ALL));
        assertTrue(Level.INFO.isInRange(Level.OFF, Level.ALL));
        assertTrue(Level.DEBUG.isInRange(Level.OFF, Level.ALL));
        assertTrue(Level.TRACE.isInRange(Level.OFF, Level.ALL));
        assertTrue(Level.ALL.isInRange(Level.OFF, Level.ALL));
    }

    @Test
    public void testIsInRangeSameLevels() {
        // Level.OFF
        assertTrue(Level.OFF.isInRange(Level.OFF, Level.OFF));
        assertFalse(Level.OFF.isInRange(Level.FATAL, Level.FATAL));
        assertFalse(Level.OFF.isInRange(Level.ERROR, Level.ERROR));
        assertFalse(Level.OFF.isInRange(Level.WARN, Level.WARN));
        assertFalse(Level.OFF.isInRange(Level.INFO, Level.INFO));
        assertFalse(Level.OFF.isInRange(Level.DEBUG, Level.DEBUG));
        assertFalse(Level.OFF.isInRange(Level.TRACE, Level.TRACE));
        assertFalse(Level.OFF.isInRange(Level.ALL, Level.ALL));
        // Level.FATAL
        assertFalse(Level.FATAL.isInRange(Level.OFF, Level.OFF));
        assertTrue(Level.FATAL.isInRange(Level.FATAL, Level.FATAL));
        assertFalse(Level.FATAL.isInRange(Level.ERROR, Level.ERROR));
        assertFalse(Level.FATAL.isInRange(Level.WARN, Level.WARN));
        assertFalse(Level.FATAL.isInRange(Level.INFO, Level.INFO));
        assertFalse(Level.FATAL.isInRange(Level.DEBUG, Level.DEBUG));
        assertFalse(Level.FATAL.isInRange(Level.TRACE, Level.TRACE));
        assertFalse(Level.FATAL.isInRange(Level.ALL, Level.ALL));
        // Level.ERROR
        assertFalse(Level.ERROR.isInRange(Level.OFF, Level.OFF));
        assertFalse(Level.ERROR.isInRange(Level.FATAL, Level.FATAL));
        assertTrue(Level.ERROR.isInRange(Level.ERROR, Level.ERROR));
        assertFalse(Level.ERROR.isInRange(Level.WARN, Level.WARN));
        assertFalse(Level.ERROR.isInRange(Level.INFO, Level.INFO));
        assertFalse(Level.ERROR.isInRange(Level.DEBUG, Level.DEBUG));
        assertFalse(Level.ERROR.isInRange(Level.TRACE, Level.TRACE));
        assertFalse(Level.ERROR.isInRange(Level.ALL, Level.ALL));
        // Level.WARN
        assertFalse(Level.WARN.isInRange(Level.OFF, Level.OFF));
        assertFalse(Level.WARN.isInRange(Level.FATAL, Level.FATAL));
        assertFalse(Level.WARN.isInRange(Level.ERROR, Level.ERROR));
        assertTrue(Level.WARN.isInRange(Level.WARN, Level.WARN));
        assertFalse(Level.WARN.isInRange(Level.INFO, Level.INFO));
        assertFalse(Level.WARN.isInRange(Level.DEBUG, Level.DEBUG));
        assertFalse(Level.WARN.isInRange(Level.TRACE, Level.TRACE));
        assertFalse(Level.WARN.isInRange(Level.ALL, Level.ALL));
        // Level.INFO
        assertFalse(Level.INFO.isInRange(Level.OFF, Level.OFF));
        assertFalse(Level.INFO.isInRange(Level.FATAL, Level.FATAL));
        assertFalse(Level.INFO.isInRange(Level.ERROR, Level.ERROR));
        assertFalse(Level.INFO.isInRange(Level.WARN, Level.WARN));
        assertTrue(Level.INFO.isInRange(Level.INFO, Level.INFO));
        assertFalse(Level.INFO.isInRange(Level.DEBUG, Level.DEBUG));
        assertFalse(Level.INFO.isInRange(Level.TRACE, Level.TRACE));
        assertFalse(Level.INFO.isInRange(Level.ALL, Level.ALL));
        // Level.DEBUG
        assertFalse(Level.DEBUG.isInRange(Level.OFF, Level.OFF));
        assertFalse(Level.DEBUG.isInRange(Level.FATAL, Level.FATAL));
        assertFalse(Level.DEBUG.isInRange(Level.ERROR, Level.ERROR));
        assertFalse(Level.DEBUG.isInRange(Level.WARN, Level.WARN));
        assertFalse(Level.DEBUG.isInRange(Level.INFO, Level.INFO));
        assertTrue(Level.DEBUG.isInRange(Level.DEBUG, Level.DEBUG));
        assertFalse(Level.DEBUG.isInRange(Level.TRACE, Level.TRACE));
        assertFalse(Level.DEBUG.isInRange(Level.ALL, Level.ALL));
        // Level.TRACE
        assertFalse(Level.TRACE.isInRange(Level.OFF, Level.OFF));
        assertFalse(Level.TRACE.isInRange(Level.FATAL, Level.FATAL));
        assertFalse(Level.TRACE.isInRange(Level.ERROR, Level.ERROR));
        assertFalse(Level.TRACE.isInRange(Level.WARN, Level.WARN));
        assertFalse(Level.TRACE.isInRange(Level.INFO, Level.INFO));
        assertFalse(Level.TRACE.isInRange(Level.DEBUG, Level.DEBUG));
        assertTrue(Level.TRACE.isInRange(Level.TRACE, Level.TRACE));
        assertFalse(Level.TRACE.isInRange(Level.ALL, Level.ALL));
        // Level.ALL
        assertFalse(Level.ALL.isInRange(Level.OFF, Level.OFF));
        assertFalse(Level.ALL.isInRange(Level.FATAL, Level.FATAL));
        assertFalse(Level.ALL.isInRange(Level.ERROR, Level.ERROR));
        assertFalse(Level.ALL.isInRange(Level.WARN, Level.WARN));
        assertFalse(Level.ALL.isInRange(Level.INFO, Level.INFO));
        assertFalse(Level.ALL.isInRange(Level.DEBUG, Level.DEBUG));
        assertFalse(Level.ALL.isInRange(Level.TRACE, Level.TRACE));
        assertTrue(Level.ALL.isInRange(Level.ALL, Level.ALL));
    }

    @Test
    public void testIsInRangeWarnToInfo() {
        assertFalse(Level.OFF.isInRange(Level.WARN, Level.INFO));
        assertFalse(Level.FATAL.isInRange(Level.WARN, Level.INFO));
        assertFalse(Level.ERROR.isInRange(Level.WARN, Level.INFO));
        assertTrue(Level.WARN.isInRange(Level.WARN, Level.INFO));
        assertTrue(Level.INFO.isInRange(Level.WARN, Level.INFO));
        assertFalse(Level.DEBUG.isInRange(Level.WARN, Level.INFO));
        assertFalse(Level.TRACE.isInRange(Level.WARN, Level.INFO));
        assertFalse(Level.ALL.isInRange(Level.WARN, Level.INFO));
    }

    @Test
    public void testIsLessSpecificThan() {
        // Level.OFF
        assertTrue(Level.OFF.isLessSpecificThan(Level.OFF));
        assertFalse(Level.OFF.isLessSpecificThan(Level.FATAL));
        assertFalse(Level.OFF.isLessSpecificThan(Level.ERROR));
        assertFalse(Level.OFF.isLessSpecificThan(Level.WARN));
        assertFalse(Level.OFF.isLessSpecificThan(Level.INFO));
        assertFalse(Level.OFF.isLessSpecificThan(Level.DEBUG));
        assertFalse(Level.OFF.isLessSpecificThan(Level.TRACE));
        assertFalse(Level.OFF.isLessSpecificThan(Level.ALL));
        // Level.FATAL
        assertTrue(Level.FATAL.isLessSpecificThan(Level.OFF));
        assertTrue(Level.FATAL.isLessSpecificThan(Level.FATAL));
        assertFalse(Level.FATAL.isLessSpecificThan(Level.ERROR));
        assertFalse(Level.FATAL.isLessSpecificThan(Level.WARN));
        assertFalse(Level.FATAL.isLessSpecificThan(Level.INFO));
        assertFalse(Level.FATAL.isLessSpecificThan(Level.DEBUG));
        assertFalse(Level.FATAL.isLessSpecificThan(Level.TRACE));
        assertFalse(Level.FATAL.isLessSpecificThan(Level.ALL));
        // Level.ERROR
        assertTrue(Level.ERROR.isLessSpecificThan(Level.OFF));
        assertTrue(Level.ERROR.isLessSpecificThan(Level.FATAL));
        assertTrue(Level.ERROR.isLessSpecificThan(Level.ERROR));
        assertFalse(Level.ERROR.isLessSpecificThan(Level.WARN));
        assertFalse(Level.ERROR.isLessSpecificThan(Level.INFO));
        assertFalse(Level.ERROR.isLessSpecificThan(Level.DEBUG));
        assertFalse(Level.ERROR.isLessSpecificThan(Level.TRACE));
        assertFalse(Level.ERROR.isLessSpecificThan(Level.ALL));
        // Level.ERROR
        assertTrue(Level.WARN.isLessSpecificThan(Level.OFF));
        assertTrue(Level.WARN.isLessSpecificThan(Level.FATAL));
        assertTrue(Level.WARN.isLessSpecificThan(Level.ERROR));
        assertTrue(Level.WARN.isLessSpecificThan(Level.WARN));
        assertFalse(Level.WARN.isLessSpecificThan(Level.INFO));
        assertFalse(Level.WARN.isLessSpecificThan(Level.DEBUG));
        assertFalse(Level.WARN.isLessSpecificThan(Level.TRACE));
        assertFalse(Level.WARN.isLessSpecificThan(Level.ALL));
        // Level.WARN
        assertTrue(Level.WARN.isLessSpecificThan(Level.OFF));
        assertTrue(Level.WARN.isLessSpecificThan(Level.FATAL));
        assertTrue(Level.WARN.isLessSpecificThan(Level.ERROR));
        assertTrue(Level.WARN.isLessSpecificThan(Level.WARN));
        assertFalse(Level.WARN.isLessSpecificThan(Level.INFO));
        assertFalse(Level.WARN.isLessSpecificThan(Level.DEBUG));
        assertFalse(Level.WARN.isLessSpecificThan(Level.TRACE));
        assertFalse(Level.WARN.isLessSpecificThan(Level.ALL));
        // Level.INFO
        assertTrue(Level.INFO.isLessSpecificThan(Level.OFF));
        assertTrue(Level.INFO.isLessSpecificThan(Level.FATAL));
        assertTrue(Level.INFO.isLessSpecificThan(Level.ERROR));
        assertTrue(Level.INFO.isLessSpecificThan(Level.WARN));
        assertTrue(Level.INFO.isLessSpecificThan(Level.INFO));
        assertFalse(Level.INFO.isLessSpecificThan(Level.DEBUG));
        assertFalse(Level.INFO.isLessSpecificThan(Level.TRACE));
        assertFalse(Level.INFO.isLessSpecificThan(Level.ALL));
        // Level.DEBUG
        assertTrue(Level.DEBUG.isLessSpecificThan(Level.OFF));
        assertTrue(Level.DEBUG.isLessSpecificThan(Level.FATAL));
        assertTrue(Level.DEBUG.isLessSpecificThan(Level.ERROR));
        assertTrue(Level.DEBUG.isLessSpecificThan(Level.WARN));
        assertTrue(Level.DEBUG.isLessSpecificThan(Level.INFO));
        assertTrue(Level.DEBUG.isLessSpecificThan(Level.DEBUG));
        assertFalse(Level.DEBUG.isLessSpecificThan(Level.TRACE));
        assertFalse(Level.DEBUG.isLessSpecificThan(Level.ALL));
        // Level.TRACE
        assertTrue(Level.TRACE.isLessSpecificThan(Level.OFF));
        assertTrue(Level.TRACE.isLessSpecificThan(Level.FATAL));
        assertTrue(Level.TRACE.isLessSpecificThan(Level.ERROR));
        assertTrue(Level.TRACE.isLessSpecificThan(Level.WARN));
        assertTrue(Level.TRACE.isLessSpecificThan(Level.INFO));
        assertTrue(Level.TRACE.isLessSpecificThan(Level.DEBUG));
        assertTrue(Level.TRACE.isLessSpecificThan(Level.TRACE));
        assertFalse(Level.TRACE.isLessSpecificThan(Level.ALL));
        // Level.ALL
        assertTrue(Level.ALL.isLessSpecificThan(Level.OFF));
        assertTrue(Level.ALL.isLessSpecificThan(Level.FATAL));
        assertTrue(Level.ALL.isLessSpecificThan(Level.ERROR));
        assertTrue(Level.ALL.isLessSpecificThan(Level.WARN));
        assertTrue(Level.ALL.isLessSpecificThan(Level.INFO));
        assertTrue(Level.ALL.isLessSpecificThan(Level.DEBUG));
        assertTrue(Level.ALL.isLessSpecificThan(Level.TRACE));
        assertTrue(Level.ALL.isLessSpecificThan(Level.ALL));
    }

}
