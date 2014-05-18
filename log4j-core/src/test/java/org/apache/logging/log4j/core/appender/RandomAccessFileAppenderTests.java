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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Collection;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.junit.CleanFiles;
import org.apache.logging.log4j.junit.InitialLoggerContext;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Simple tests for both the RandomAccessFileAppender and RollingRandomAccessFileAppender.
 */
@RunWith(Parameterized.class)
public class RandomAccessFileAppenderTests {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[][]{
                        { "RandomAccessFileAppenderTest", false },
                        { "RandomAccessFileAppenderLocationTest", true },
                        { "RollingRandomAccessFileAppenderTest", false },
                        { "RollingRandomAccessFileAppenderLocationTest", true }
                }
        );
    }

    @Rule
    public InitialLoggerContext init;

    @Rule
    public CleanFiles files;

    private final File logFile;
    private final boolean locationEnabled;

    public RandomAccessFileAppenderTests(final String testName, final boolean locationEnabled) {
        this.init = new InitialLoggerContext(testName + ".xml");
        this.logFile = new File("target", testName + ".log");
        this.files = new CleanFiles(this.logFile);
        this.locationEnabled = locationEnabled;
    }

    @Test
    public void testRandomAccessConfiguration() throws Exception {
        final Logger logger = this.init.getLogger("com.foo.Bar");
        final String message = "This is a test log message brought to you by Slurm.";
        logger.info(message);
        this.init.getContext().stop(); // stop async thread

        String line;
        final BufferedReader reader = new BufferedReader(new FileReader(this.logFile));
        try {
            line = reader.readLine();
        } finally {
            reader.close();
        }
        assertNotNull(line);
        assertThat(line, containsString(message));
        final Matcher<String> containsLocationInformation = containsString("testRandomAccessConfiguration");
        final Matcher<String> containsLocationInformationIfEnabled = this.locationEnabled ?
                containsLocationInformation : not(containsLocationInformation);
        assertThat(line, containsLocationInformationIfEnabled);
    }
}
