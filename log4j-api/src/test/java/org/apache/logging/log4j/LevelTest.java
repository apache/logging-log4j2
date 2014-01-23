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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
    public void testGoodDebugLevel() {
        final Level level = Level.toLevel("DEBUG");
        assertNotNull(level);
        assertEquals(Level.DEBUG, level);
    }

    @Test
    public void testGoodDiagLevel() {
        final Level level = Level.toLevel("DIAG");
        assertNotNull(level);
        assertEquals(Level.DIAG, level);
    }

    @Test
    public void testGoodErrorLevel() {
        final Level level = Level.toLevel("ERROR");
        assertNotNull(level);
        assertEquals(Level.ERROR, level);
    }

    @Test
    public void testGoodFatalLevel() {
        final Level level = Level.toLevel("FATAL");
        assertNotNull(level);
        assertEquals(Level.FATAL, level);
    }

    @Test
    public void testGoodInfoLevel() {
        final Level level = Level.toLevel("INFO");
        assertNotNull(level);
        assertEquals(Level.INFO, level);
    }

    @Test
    public void testGoodNoticeLevel() {
        final Level level = Level.toLevel("NOTICE");
        assertNotNull(level);
        assertEquals(Level.NOTICE, level);
    }

    @Test
    public void testGoodOffLevel() {
        final Level level = Level.toLevel("OFF");
        assertNotNull(level);
        assertEquals(Level.OFF, level);
    }

    @Test
    public void testGoodTraceLevel() {
        final Level level = Level.toLevel("TRACE");
        assertNotNull(level);
        assertEquals(Level.TRACE, level);
    }


    @Test
    public void testGoodVerboseLevel() {
        final Level level = Level.toLevel("VERBOSE");
        assertNotNull(level);
        assertEquals(Level.VERBOSE, level);
    }

    @Test
    public void testGoodWarnLevel() {
        final Level level = Level.toLevel("WARN");
        assertNotNull(level);
        assertEquals(Level.WARN, level);
    }
}
