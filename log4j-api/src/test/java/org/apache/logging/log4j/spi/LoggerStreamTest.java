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
package org.apache.logging.log4j.spi;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.TestLogger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class LoggerStreamTest {
    private List<String> results;
    private LoggerStream stream;
    private Level level;
    private String logMessage;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[][]{
                        { Level.DEBUG, "debug log string test" },
                        { Level.INFO, "info log string test" },
                        { Level.WARN, "DANGER ZONE" },
                        { Level.ERROR, "MAYDAY! MAYDAY!" },
                        { Level.FATAL, "ABANDON SHIP!" }
                }
        );
    }

    public LoggerStreamTest(final Level level, final String logMessage) {
        this.level = level;
        this.logMessage = logMessage;
    }

    @Before
    public void setUp() throws Exception {
        TestLogger logger = (TestLogger) LogManager.getLogger();
        results = logger.getEntries();
        assertEmpty();
        stream = new LoggerStream(logger, level);
        assertEmpty();
    }

    @After
    public void tearDown() throws Exception {
        results.clear();
    }

    private void assertEmpty() {
        assertTrue("There should be no results yet.", results.isEmpty());
    }

    private void assertNumResults(int numResults) {
        assertEquals("Unexpected number of results.", numResults, results.size());
    }

    private void assertMessageStartsWith(final String message) {
        assertNumResults(1);
        final String start = ' ' + level.name() + ' ' + message;
        assertThat(results.get(0), startsWith(start));
    }

    @Test
    public void testWrite_Int() throws Exception {
        for (byte b : logMessage.getBytes()) {
            stream.write(b);
            assertEmpty();
        }
        stream.write('\n');
        assertMessageStartsWith(logMessage);
    }

    @Test
    public void testWrite_ByteArray() throws Exception {
        final byte[] bytes = logMessage.getBytes();
        stream.write(bytes);
        assertEmpty();
        stream.write('\n');
        assertMessageStartsWith(logMessage);
    }

    @Test
    public void testWrite_ByteArray_Offset_Length() throws Exception {
        final byte[] bytes = logMessage.getBytes();
        int middle = bytes.length/2;
        int length = bytes.length - middle;
        final String right = new String(bytes, middle, length);
        stream.write(bytes, middle, length);
        assertEmpty();
        stream.write('\n');
        assertMessageStartsWith(right);
    }

    @Test
    public void testPrint_Boolean() throws Exception {
        stream.print(true);
        assertEmpty();
        stream.println();
        assertMessageStartsWith("true");
    }

    @Test
    public void testPrint_Character() throws Exception {
        for (char c : logMessage.toCharArray()) {
            stream.print(c);
            assertEmpty();
        }
        stream.println();
        assertMessageStartsWith(logMessage);
    }

    @Test
    public void testPrint_Integer() throws Exception {
        int n = logMessage.codePointAt(0);
        stream.print(n);
        assertEmpty();
        stream.println();
        assertMessageStartsWith(String.valueOf(n));
    }

    @Test
    public void testPrint_CharacterArray() throws Exception {
        stream.print(logMessage.toCharArray());
        assertEmpty();
        stream.println();
        assertMessageStartsWith(logMessage);
    }

    @Test
    public void testPrint_String() throws Exception {
        stream.print(logMessage);
        assertEmpty();
        stream.println();
        assertMessageStartsWith(logMessage);
    }

    @Test
    public void testPrint_Object() throws Exception {
        final Object o = logMessage;
        stream.print(o);
        assertEmpty();
        stream.println();
        assertMessageStartsWith(logMessage);
    }

    @Test
    public void testPrintf() throws Exception {
        stream.printf("<<<%s>>>", logMessage);
        assertEmpty();
        stream.println();
        assertMessageStartsWith("<<<" + logMessage);
    }

    @Test
    public void testFormat() throws Exception {
        stream.format("[%s]", logMessage).println();
        assertMessageStartsWith("[" + logMessage);
    }
}
