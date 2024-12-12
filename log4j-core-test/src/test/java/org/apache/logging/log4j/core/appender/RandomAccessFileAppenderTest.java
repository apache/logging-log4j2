/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.appender;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Simple tests for both the RandomAccessFileAppender and RollingRandomAccessFileAppender.
 */
public class RandomAccessFileAppenderTest {

    private File logFile;

    private void setLogFile(String testName) {
        logFile = new File("target", testName + ".log");
    }

    @AfterEach
    void cleanUp() throws IOException {
        Files.deleteIfExists(logFile.toPath());
    }

    private void testRandomAccessConfiguration(LoggerContext context, final boolean locationEnabled) throws Exception {
        final Logger logger = context.getLogger("com.foo.Bar");
        final String message = "This is a test log message brought to you by Slurm.";
        logger.info(message);
        context.stop(); // stop async thread

        String line;
        try (final BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            line = reader.readLine();
        }
        assertNotNull(line);
        assertThat(line, containsString(message));
        final Matcher<String> containsLocationInformation = containsString("testRandomAccessConfiguration");
        final Matcher<String> containsLocationInformationIfEnabled =
                locationEnabled ? containsLocationInformation : not(containsLocationInformation);
        assertThat(line, containsLocationInformationIfEnabled);
    }

    @Test
    @LoggerContextSource("RandomAccessFileAppenderTest.xml")
    public void test1(LoggerContext context) throws Exception {
        setLogFile("RandomAccessFileAppenderTest");
        testRandomAccessConfiguration(context, false);
    }

    @Test
    @LoggerContextSource("RandomAccessFileAppenderLocationTest.xml")
    public void test2(LoggerContext context) throws Exception {
        setLogFile("RandomAccessFileAppenderLocationTest");
        testRandomAccessConfiguration(context, true);
    }

    @Test
    @LoggerContextSource("RollingRandomAccessFileAppenderTest.xml")
    public void test3(LoggerContext context) throws Exception {
        setLogFile("RollingRandomAccessFileAppenderTest");
        testRandomAccessConfiguration(context, false);
    }

    @Test
    @LoggerContextSource("RollingRandomAccessFileAppenderLocationTest.xml")
    public void test4(LoggerContext context) throws Exception {
        setLogFile("RollingRandomAccessFileAppenderLocationTest");
        testRandomAccessConfiguration(context, true);
    }

    @Test
    @LoggerContextSource("RollingRandomAccessFileAppenderLocationPropsTest.properties")
    public void test5(LoggerContext context) throws Exception {
        setLogFile("RollingRandomAccessFileAppenderLocationPropsTest");
        testRandomAccessConfiguration(context, false);
    }
}
