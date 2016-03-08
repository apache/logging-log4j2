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
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.CoreLoggerContexts;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;

import static org.junit.Assert.*;

/**
 * Tests that logged strings appear in the file, that the initial file size is the specified specified region length,
 * that the file is extended by region length when necessary, and that the file is shrunk to its actual usage when done.
 * 
 * @since 2.1
 */
public class MemoryMappedFileAppenderRemapTest {

    final String LOGFILE = "target/MemoryMappedFileAppenderRemapTest.log";

    @Before
    public void before() {
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, "MemoryMappedFileAppenderRemapTest.xml");
    }

    @Test
    public void testMemMapExtendsIfNeeded() throws Exception {
        final File f = new File(LOGFILE);
        if (f.exists()) {
            assertTrue(f.delete());
        }
        assertTrue(!f.exists());

        final Logger log = LogManager.getLogger();
        final char[] text = new char[200];
        Arrays.fill(text, 'A');
        try {
            log.warn("Test log1");
            assertTrue(f.exists());
            assertEquals("initial length", 256, f.length());

            log.warn(new String(text));
            assertEquals("grown", 256 * 2, f.length());
            
            log.warn(new String(text));
            assertEquals("grown again", 256 * 3, f.length());
        } finally {
            CoreLoggerContexts.stopLoggerContext(false);
        }
        final int LINESEP = System.lineSeparator().length();
        assertEquals("Shrunk to actual used size", 658 + 3 * LINESEP, f.length());

        String line1, line2, line3, line4;
        try (final BufferedReader reader = new BufferedReader(new FileReader(LOGFILE))) {
            line1 = reader.readLine();
            line2 = reader.readLine();
            line3 = reader.readLine();
            line4 = reader.readLine();
        }
        assertNotNull(line1);
        assertThat(line1, containsString("Test log1"));

        assertNotNull(line2);
        assertThat(line2, containsString(new String(text)));

        assertNotNull(line3);
        assertThat(line3, containsString(new String(text)));

        assertNull("only three lines were logged", line4);
    }
}
