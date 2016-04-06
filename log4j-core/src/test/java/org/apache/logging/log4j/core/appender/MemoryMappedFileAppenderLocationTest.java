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
package org.apache.logging.log4j.core.appender;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.CoreLoggerContexts;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.util.Integers;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;

import static org.junit.Assert.*;

/**
 * Tests that logged strings and their location appear in the file,
 * that the file size is the next power of two of the specified mapped region length
 * and that the file is shrunk to its actual usage when done.
 *
 * @since 2.1
 */
public class MemoryMappedFileAppenderLocationTest {

    final String LOGFILE = "target/MemoryMappedFileAppenderLocationTest.log";

    @Before
    public void before() {
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY,
                "MemoryMappedFileAppenderLocationTest.xml");
    }

    @Test
    public void testMemMapLocation() throws Exception {
        final File f = new File(LOGFILE);
        if (f.exists()) {
            assertTrue("deleted ok", f.delete());
        }
        assertTrue(!f.exists());

        final int expectedFileLength = Integers.ceilingNextPowerOfTwo(32000);
        assertEquals(32768, expectedFileLength);

        final Logger log = LogManager.getLogger();
        try {
            log.warn("Test log1");
            assertTrue(f.exists());
            assertEquals("initial length", expectedFileLength, f.length());

            log.warn("Test log2");
            assertEquals("not grown", expectedFileLength, f.length());
        } finally {
            CoreLoggerContexts.stopLoggerContext(false);
        }
        final int LINESEP = System.lineSeparator().length();
        assertEquals("Shrunk to actual used size", 474 + 2 * LINESEP, f.length());

        String line1, line2, line3;
        try (final BufferedReader reader = new BufferedReader(new FileReader(LOGFILE))) {
            line1 = reader.readLine();
            line2 = reader.readLine();
            line3 = reader.readLine();
        }
        assertNotNull(line1);
        assertThat(line1, containsString("Test log1"));
        final String location1 = "org.apache.logging.log4j.core.appender.MemoryMappedFileAppenderLocationTest.testMemMapLocation(MemoryMappedFileAppenderLocationTest.java:65)";
        assertThat(line1, containsString(location1));

        assertNotNull(line2);
        assertThat(line2, containsString("Test log2"));
        final String location2 = "org.apache.logging.log4j.core.appender.MemoryMappedFileAppenderLocationTest.testMemMapLocation(MemoryMappedFileAppenderLocationTest.java:69)";
        assertThat(line2, containsString(location2));

        assertNull("only two lines were logged", line3);
    }
}
