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

import org.junit.Test;

import static org.junit.Assert.*;

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

    @Test
    public void testIsMoreSpecificThan() {
        // Level.OFF
        assertTrue(Level.OFF.isMoreSpecificThan(Level.OFF));
        assertTrue(Level.OFF.isMoreSpecificThan(Level.FATAL));
        assertTrue(Level.OFF.isMoreSpecificThan(Level.ERROR));
        assertTrue(Level.OFF.isMoreSpecificThan(Level.WARN));
        assertTrue(Level.OFF.isMoreSpecificThan(Level.INFO));
        assertTrue(Level.OFF.isMoreSpecificThan(Level.DEBUG));
        assertTrue(Level.OFF.isMoreSpecificThan(Level.TRACE));
        assertTrue(Level.OFF.isMoreSpecificThan(Level.ALL));
        // Level.FATAL
        assertFalse(Level.FATAL.isMoreSpecificThan(Level.OFF));
        assertTrue(Level.FATAL.isMoreSpecificThan(Level.FATAL));
        assertTrue(Level.FATAL.isMoreSpecificThan(Level.ERROR));
        assertTrue(Level.FATAL.isMoreSpecificThan(Level.WARN));
        assertTrue(Level.FATAL.isMoreSpecificThan(Level.INFO));
        assertTrue(Level.FATAL.isMoreSpecificThan(Level.DEBUG));
        assertTrue(Level.FATAL.isMoreSpecificThan(Level.TRACE));
        assertTrue(Level.FATAL.isMoreSpecificThan(Level.ALL));
        // Level.ERROR
        assertFalse(Level.ERROR.isMoreSpecificThan(Level.OFF));
        assertFalse(Level.ERROR.isMoreSpecificThan(Level.FATAL));
        assertTrue(Level.ERROR.isMoreSpecificThan(Level.ERROR));
        assertTrue(Level.ERROR.isMoreSpecificThan(Level.WARN));
        assertTrue(Level.ERROR.isMoreSpecificThan(Level.INFO));
        assertTrue(Level.ERROR.isMoreSpecificThan(Level.DEBUG));
        assertTrue(Level.ERROR.isMoreSpecificThan(Level.TRACE));
        assertTrue(Level.ERROR.isMoreSpecificThan(Level.ALL));
        // Level.WARN
        assertFalse(Level.WARN.isMoreSpecificThan(Level.OFF));
        assertFalse(Level.WARN.isMoreSpecificThan(Level.FATAL));
        assertFalse(Level.WARN.isMoreSpecificThan(Level.ERROR));
        assertTrue(Level.WARN.isMoreSpecificThan(Level.WARN));
        assertTrue(Level.WARN.isMoreSpecificThan(Level.INFO));
        assertTrue(Level.WARN.isMoreSpecificThan(Level.DEBUG));
        assertTrue(Level.WARN.isMoreSpecificThan(Level.TRACE));
        assertTrue(Level.WARN.isMoreSpecificThan(Level.ALL));
        // Level.INFO
        assertFalse(Level.INFO.isMoreSpecificThan(Level.OFF));
        assertFalse(Level.INFO.isMoreSpecificThan(Level.FATAL));
        assertFalse(Level.INFO.isMoreSpecificThan(Level.ERROR));
        assertFalse(Level.INFO.isMoreSpecificThan(Level.WARN));
        assertTrue(Level.INFO.isMoreSpecificThan(Level.INFO));
        assertTrue(Level.INFO.isMoreSpecificThan(Level.DEBUG));
        assertTrue(Level.INFO.isMoreSpecificThan(Level.TRACE));
        assertTrue(Level.INFO.isMoreSpecificThan(Level.ALL));
        // Level.DEBUG
        assertFalse(Level.DEBUG.isMoreSpecificThan(Level.OFF));
        assertFalse(Level.DEBUG.isMoreSpecificThan(Level.FATAL));
        assertFalse(Level.DEBUG.isMoreSpecificThan(Level.ERROR));
        assertFalse(Level.DEBUG.isMoreSpecificThan(Level.WARN));
        assertFalse(Level.DEBUG.isMoreSpecificThan(Level.INFO));
        assertTrue(Level.DEBUG.isMoreSpecificThan(Level.DEBUG));
        assertTrue(Level.DEBUG.isMoreSpecificThan(Level.TRACE));
        assertTrue(Level.DEBUG.isMoreSpecificThan(Level.ALL));
        // Level.TRACE
        assertFalse(Level.TRACE.isMoreSpecificThan(Level.OFF));
        assertFalse(Level.TRACE.isMoreSpecificThan(Level.FATAL));
        assertFalse(Level.TRACE.isMoreSpecificThan(Level.ERROR));
        assertFalse(Level.TRACE.isMoreSpecificThan(Level.WARN));
        assertFalse(Level.TRACE.isMoreSpecificThan(Level.INFO));
        assertFalse(Level.TRACE.isMoreSpecificThan(Level.DEBUG));
        assertTrue(Level.TRACE.isMoreSpecificThan(Level.TRACE));
        assertTrue(Level.TRACE.isMoreSpecificThan(Level.ALL));
        // Level.ALL
        assertFalse(Level.ALL.isMoreSpecificThan(Level.OFF));
        assertFalse(Level.ALL.isMoreSpecificThan(Level.FATAL));
        assertFalse(Level.ALL.isMoreSpecificThan(Level.ERROR));
        assertFalse(Level.ALL.isMoreSpecificThan(Level.WARN));
        assertFalse(Level.ALL.isMoreSpecificThan(Level.INFO));
        assertFalse(Level.ALL.isMoreSpecificThan(Level.DEBUG));
        assertFalse(Level.ALL.isMoreSpecificThan(Level.TRACE));
        assertTrue(Level.ALL.isMoreSpecificThan(Level.ALL));
    }

}
