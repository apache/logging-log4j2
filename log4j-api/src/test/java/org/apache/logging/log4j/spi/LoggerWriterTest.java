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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class LoggerWriterTest {
    private List<String> results;
    private LoggerWriter writer;
    private Level level;
    private String logMessage;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] { { Level.DEBUG, "debug log string test" }, { Level.INFO, "info log string test" }, { Level.WARN, "DANGER ZONE" },
                { Level.ERROR, "MAYDAY! MAYDAY!" }, { Level.FATAL, "ABANDON SHIP!" } });
    }

    public LoggerWriterTest(final Level level, final String logMessage) {
        this.level = level;
        this.logMessage = logMessage;
    }

    @Before
    public void setUp() throws Exception {
        TestLogger logger = (TestLogger) LogManager.getLogger();
        results = logger.getEntries();
        assertEmpty();
        writer = new LoggerWriter(logger, null, level);
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

    private void assertMessages(final String... messages) {
        assertNumResults(messages.length);
        for (int i = 0; i < messages.length; i++) {
            final String start = ' ' + level.name() + ' ' + messages[i];
            assertThat(results.get(i), startsWith(start));
        }
    }

    @Test
    public void testWrite_CharArray() throws Exception {
        final char[] chars = logMessage.toCharArray();
        writer.write(chars);
        assertEmpty();
        writer.write('\n');
        assertMessages(logMessage);
    }

    @Test
    public void testWrite_CharArray_Offset_Length() throws Exception {
        final char[] chars = logMessage.toCharArray();
        int middle = chars.length / 2;
        int length = chars.length - middle;
        final String right = new String(chars, middle, length);
        writer.write(chars, middle, length);
        assertEmpty();
        writer.write('\n');
        assertMessages(right);
    }

    @Test
    public void testWrite_Character() throws Exception {
        for (char c : logMessage.toCharArray()) {
            writer.write(c);
            assertEmpty();
        }
        writer.write('\n');
        assertMessages(logMessage);
    }

    @Test
    public void testWrite_IgnoresWindowsNewline() throws IOException {
        writer.write(logMessage + "\r\n");
        assertMessages(logMessage);
    }

    @Test
    public void testWrite_MultipleLines() throws IOException {
        writer.write(logMessage + '\n' + logMessage + '\n');
        assertMessages(logMessage, logMessage);
    }

    @Test
    public void testClose_NoRemainingData() throws IOException {
        writer.close();
        assertEmpty();
    }

    @Test
    public void testClose_HasRemainingData() throws IOException {
        writer.write(logMessage);
        assertEmpty();
        writer.close();
        assertMessages(logMessage);
    }
}
