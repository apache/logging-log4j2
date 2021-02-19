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

import static org.apache.logging.log4j.hamcrest.Descriptors.that;
import static org.apache.logging.log4j.hamcrest.FileMatchers.hasName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.assertj.core.api.HamcrestCondition;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 *
 */
public class RollingAppenderDirectWriteTest {

    private static final String CONFIG = "log4j-rolling-direct.xml";

    private static final String DIR = "target/rolling-direct";

    public static LoggerContextRule loggerContextRule = LoggerContextRule.createShutdownTimeoutLoggerContextRule(CONFIG);

    @Rule
    public RuleChain chain = loggerContextRule.withCleanFoldersRule(DIR);

    private Logger logger;

    @Before
    public void setUp() throws Exception {
        this.logger = loggerContextRule.getLogger(RollingAppenderDirectWriteTest.class.getName());
    }

    @Test
    public void testAppender() throws Exception {
        int count = 100;
        for (int i=0; i < count; ++i) {
            logger.debug("This is test message number " + i);
        }
        Thread.sleep(50);
        final File dir = new File(DIR);
        assertThat(dir.exists() && dir.listFiles().length > 0).describedAs("Directory not created").isTrue();
        final File[] files = dir.listFiles();
        assertThat(files).isNotNull();
        assertThat(files).is(new HamcrestCondition<>(hasItemInArray(that(hasName(that(endsWith(".gz")))))));
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
                assertThat(line).describedAs("No log event in file " + actual).isNotNull();
                String[] parts = line.split((" "));
                String expected = "test1-" + parts[0];
                assertThat(actual.startsWith(expected)).describedAs("Incorrect file name. Expected file prefix: " + expected + " Actual: " + actual).isTrue();
                ++found;
            }
            reader.close();
        }
        assertThat(found).describedAs("Incorrect number of events read. Expected " + count + ", Actual " + found).isEqualTo(count);
    }
}
