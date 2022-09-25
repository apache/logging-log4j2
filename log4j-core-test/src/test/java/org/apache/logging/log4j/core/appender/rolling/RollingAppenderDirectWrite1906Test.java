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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusListener;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import static org.apache.logging.log4j.hamcrest.Descriptors.that;
import static org.apache.logging.log4j.hamcrest.FileMatchers.hasName;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 *
 */
public class RollingAppenderDirectWrite1906Test {

    private static final String CONFIG = "log4j-rolling-direct-1906.xml";

    private static final String DIR = "target/rolling-direct-1906";

    public static LoggerContextRule loggerContextRule = LoggerContextRule.createShutdownTimeoutLoggerContextRule(CONFIG);

    @Rule
    public RuleChain chain = loggerContextRule.withCleanFoldersRule(DIR);

    private Logger logger;

    @BeforeClass
    public static void setupClass() throws Exception {
        StatusLogger.getLogger().registerListener(new NoopStatusListener());
    }

    @Before
    public void setUp() throws Exception {
        this.logger = loggerContextRule.getLogger(RollingAppenderDirectWrite1906Test.class.getName());
    }

    @Test
    public void testAppender() throws Exception {
        final int count = 100;
        for (int i=0; i < count; ++i) {
            logger.debug("This is test message number " + i);
            Thread.sleep(50);
        }
        Thread.sleep(50);
        final File dir = new File(DIR);
        assertTrue("Directory not created", dir.exists() && dir.listFiles().length > 0);
        final File[] files = dir.listFiles();
        assertNotNull(files);
        assertThat(files, hasItemInArray(that(hasName(that(endsWith(".log"))))));
        int found = 0;
        for (final File file: files) {
            final String actual = file.getName();
            final BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                assertNotNull("No log event in file " + actual, line);
                final String[] parts = line.split((" "));
                final String expected = "rollingfile." + parts[0] + ".log";

                assertEquals(logFileNameError(expected, actual), expected, actual);
                ++found;
            }
            reader.close();
        }
        assertEquals("Incorrect number of events read. Expected " + count + ", Actual " + found, count, found);

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
