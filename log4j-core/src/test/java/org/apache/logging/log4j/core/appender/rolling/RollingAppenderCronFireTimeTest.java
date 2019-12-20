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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Random;

import static org.apache.logging.log4j.hamcrest.Descriptors.that;
import static org.apache.logging.log4j.hamcrest.FileMatchers.hasName;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.junit.Assert.*;

/**
 * LOG4J2-2741.
 * Log files must not be overwritten when using {@link CronTriggeringPolicy} with only a date pattern in the filePattern
 * and without {@code %i}.
 */
public class RollingAppenderCronFireTimeTest {

    private static final String CONFIG = "log4j-rolling-cron-firetime.xml";

    private static final String DIR = "target/rolling-cron-firetime";

    public static LoggerContextRule loggerContextRule = LoggerContextRule.createShutdownTimeoutLoggerContextRule(CONFIG);

    @Rule
    public RuleChain chain = loggerContextRule.withCleanFoldersRule(DIR);

    private Logger logger;

    @Before
    public void setUp() throws Exception {
        this.logger = loggerContextRule.getLogger(RollingAppenderCronFireTimeTest.class.getName());
    }

    @Test
    public void testAppender() throws Exception {
        final String testMessagePrefix = "This is test message number ";
        int totalLogMessagesWritten = 0;
        Random rand = new Random();
        for (int j=0; j < 100; ++j) {
            totalLogMessagesWritten++;
            logger.debug(testMessagePrefix + totalLogMessagesWritten);
            Thread.sleep(rand.nextInt(200));
        }

        final File dir = new File(DIR);
        assertTrue("Directory not created", dir.exists() && dir.listFiles().length > 0);
        final File[] files = dir.listFiles();
        Arrays.sort(files);
        assertNotNull(files);
        Assert.assertEquals("number of log messages written vs number of log messages in log files",
                totalLogMessagesWritten, countLines(files));
    }

    private int countLines(File... files) throws IOException {
        int totalLines = 0;
        for(File file : files){
            totalLines += Files.lines(file.toPath()).count();
        }
        return totalLines;
    }
}
