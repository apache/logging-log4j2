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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import java.io.File;
import java.time.LocalTime;

import static org.apache.logging.log4j.hamcrest.Descriptors.that;
import static org.apache.logging.log4j.hamcrest.FileMatchers.hasName;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class RollingRandomAppenderDirectWriteAndSwitchDirectorTest {

    private static final String CONFIG = "log4j-rolling-random-direct-switch-director.xml";

    private static final String DIR = "target/rolling-random-direct-switch-director";

    public static LoggerContextRule loggerContextRule = LoggerContextRule.createShutdownTimeoutLoggerContextRule(CONFIG);

    @Rule
    public RuleChain chain = loggerContextRule.withCleanFoldersRule(DIR);

    private Logger logger;

    @Before
    public void setUp() throws Exception {
        this.logger = loggerContextRule.getLogger(RollingRandomAppenderDirectWriteAndSwitchDirectorTest.class.getName());
    }

    @Test
    public void testAppender() throws Exception {
        LocalTime start = LocalTime.now();
        LocalTime end;
        do {
            end = LocalTime.now();
            logger.info("test log");
            Thread.sleep(100);
        } while (start.getSecond() == end.getSecond());

        File nextLogFile = new File(String.format("%s/%d/%d.log", DIR, end.getSecond(), end.getSecond()));
        assertTrue("nextLogFile not created", nextLogFile.exists());
    }
}
