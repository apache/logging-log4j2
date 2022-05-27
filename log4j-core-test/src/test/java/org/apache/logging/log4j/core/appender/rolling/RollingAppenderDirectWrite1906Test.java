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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusListener;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.junit.CleanUpDirectories;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

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
public class RollingAppenderDirectWrite1906Test {

    private static final String CONFIG = "log4j-rolling-direct-1906.xml";

    private static final String DIR = "target/rolling-direct-1906";

    @BeforeAll
    static void beforeAll() {
        StatusLogger.getLogger().registerListener(new NoopStatusListener());
    }

    @Test
    @CleanUpDirectories(DIR)
    @LoggerContextSource(value = CONFIG, timeout = 10)
    public void testAppender(final LoggerContext context) throws Exception {
        final var logger = context.getLogger(getClass());
        int count = 100;
        for (int i=0; i < count; ++i) {
            logger.debug("This is test message number " + i);
            Thread.sleep(50);
        }
        Thread.sleep(50);
        final File dir = new File(DIR);
        assertTrue(dir.exists() && dir.listFiles().length > 0, "Directory not created");
        final File[] files = dir.listFiles();
        assertNotNull(files);
        assertThat(files, hasItemInArray(that(hasName(that(endsWith(".log"))))));
        int found = 0;
        for (File file: files) {
            String actual = file.getName();
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                assertNotNull(line, "No log event in file " + actual);
                String[] parts = line.split((" "));
                String expected = "rollingfile." + parts[0] + ".log";

                assertEquals(expected, actual, logFileNameError(expected, actual));
                ++found;
            }
            reader.close();
        }
        assertEquals(count, found, "Incorrect number of events read. Expected " + count + ", Actual " + found);

    }


    private String logFileNameError(String expected, String actual) {
        final List<StatusData> statusData = StatusLogger.getLogger().getStatusData();
        final StringBuilder sb = new StringBuilder();
        for (StatusData statusItem : statusData) {
            sb.append(statusItem.getFormattedStatus());
            sb.append("\n");
        }
        sb.append("Incorrect file name. Expected: ").append(expected).append(" Actual: ").append(actual);
        return sb.toString();
    }

    private static class NoopStatusListener implements StatusListener {
        @Override
        public void log(StatusData data) {

        }

        @Override
        public Level getStatusLevel() {
            return Level.TRACE;
        }

        @Override
        public void close() throws IOException {

        }
    }
}
