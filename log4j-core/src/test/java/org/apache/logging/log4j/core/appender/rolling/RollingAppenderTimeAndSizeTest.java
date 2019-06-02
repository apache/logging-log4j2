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
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Random;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 *
 */
public class RollingAppenderTimeAndSizeTest {

    private static final String CONFIG = "log4j-rolling3.xml";

    private static final String DIR = "target/rolling3/test";

    public static LoggerContextRule loggerContextRule = LoggerContextRule.createShutdownTimeoutLoggerContextRule(CONFIG);

    @Rule
    public RuleChain chain = loggerContextRule.withCleanFoldersRule(DIR);

    private Logger logger;

    @Before
    public void setUp() throws Exception {
        this.logger = loggerContextRule.getLogger(RollingAppenderTimeAndSizeTest.class.getName());
    }

    @Test
    public void testAppender() throws Exception {
		Random rand = new Random();
		final File logFile = new File("target/rolling3/rollingtest.log");
		assertTrue("target/rolling3/rollingtest.log does not exist", logFile.exists());
		FileTime time = (FileTime) Files.getAttribute(logFile.toPath(), "creationTime");
		for (int j=0; j < 100; ++j) {
			int count = rand.nextInt(50);
			for (int i = 0; i < count; ++i) {
				logger.debug("This is test message number " + i);
			}
			Thread.sleep(rand.nextInt(50));
		}
		Thread.sleep(50);
		final File dir = new File(DIR);
		assertTrue("Directory not created", dir.exists() && dir.listFiles().length > 0);
		final File[] files = dir.listFiles();
		Arrays.sort(files);
		assertNotNull(files);
		assertThat(files, hasItemInArray(that(hasName(that(endsWith(".log"))))));
		int found = 0;
		int fileCounter = 0;
		String previous = "";
		for (final File file: files) {
			final String actual = file.getName();
			StringBuilder padding = new StringBuilder();
			String length = Long.toString(file.length());
			for (int i = length.length(); i < 10; ++i) {
				padding.append(" ");
			}
			final String[] fileParts = actual.split("_|\\.");
			fileCounter = previous.equals(fileParts[1]) ? ++fileCounter : 1;
			previous = fileParts[1];
			assertEquals("Incorrect file name. Expected counter value of " + fileCounter + " in " + actual,
				Integer.toString(fileCounter), fileParts[2]);
		}
		FileTime endTime = (FileTime) Files.getAttribute(logFile.toPath(), "creationTime");
		assertNotEquals("Creation times are equal", time, endTime);
    }
}
