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
package org.apache.logging.log4j.core.appender.rolling;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.test.junit.CleanUpDirectories;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import static org.apache.logging.log4j.core.test.hamcrest.Descriptors.that;
import static org.apache.logging.log4j.core.test.hamcrest.FileMatchers.hasName;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 */
@Tag("sleepy")
public class RollingAppenderDirectWriteTest {

    private static final String CONFIG = "log4j-rolling-direct.xml";

    private static final String DIR = "target/rolling-direct";

    @Test
    @CleanUpDirectories(DIR)
    @LoggerContextSource(value = CONFIG, timeout = 10)
    public void testAppender(final LoggerContext context) throws Exception {
        final var logger = context.getLogger(getClass());
        int count = 100;
        for (int i=0; i < count; ++i) {
            logger.debug("This is test message number " + i);
        }
        Thread.sleep(50);
        final File dir = new File(DIR);
        assertTrue(dir.exists() && dir.listFiles().length > 0, "Directory not created");
        final File[] files = dir.listFiles();
        assertNotNull(files);
        assertThat(files, hasItemInArray(that(hasName(that(endsWith(".gz"))))));
        int found = 0;
        for (File file: files) {
            String actual = file.getName();
            BufferedReader reader;
            if (file.getName().endsWith(".gz")) {
                reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file))));
            } else {
                reader = new BufferedReader(new FileReader(file));
            }
            String line;
            while ((line = reader.readLine()) != null) {
                assertNotNull(line, "No log event in file " + actual);
                String[] parts = line.split((" "));
                String expected = "test1-" + parts[0];
                assertTrue(actual.startsWith(expected),
                        "Incorrect file name. Expected file prefix: " + expected + " Actual: " + actual);
                ++found;
            }
            reader.close();
        }
        assertEquals(count, found, "Incorrect number of events read. Expected " + count + ", Actual " + found);
    }
}
