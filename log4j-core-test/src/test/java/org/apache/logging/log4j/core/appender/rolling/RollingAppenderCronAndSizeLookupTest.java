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
package org.apache.logging.log4j.core.appender.rolling;

import static org.apache.logging.log4j.core.test.hamcrest.Descriptors.that;
import static org.apache.logging.log4j.core.test.hamcrest.FileMatchers.hasName;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.Random;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.CleanFoldersRuleExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * LOG4J2-1804.
 */
public class RollingAppenderCronAndSizeLookupTest {

    private static final String CONFIG = "log4j-rolling-cron-and-size-lookup.xml";

    private static final String DIR = "target/rolling-cron-size-lookup";

    @RegisterExtension
    CleanFoldersRuleExtension extension = new CleanFoldersRuleExtension(
            DIR,
            CONFIG,
            RollingAppenderCronAndSizeLookupTest.class.getName(),
            this.getClass().getClassLoader());

    private Logger logger;

    @BeforeEach
    public void setUp(final LoggerContext loggerContext) {
        this.logger = loggerContext.getLogger(RollingAppenderCronAndSizeLookupTest.class.getName());
    }

    @Test
    public void testAppender() throws Exception {
        final Random rand = new Random();
        // Loop for 500 times with a 5ms wait guarantees at least 2 time based rollovers.
        for (int j = 0; j < 500; ++j) {
            for (int i = 0; i < 10; ++i) {
                logger.debug("This is test message number " + i);
            }
            Thread.sleep(5);
        }
        Thread.sleep(50);
        final File dir = new File(DIR);
        assertTrue(dir.exists() && dir.listFiles().length > 0, "Directory not created");
        final File[] files = dir.listFiles();
        Arrays.sort(files);
        assertNotNull(files);
        assertThat(files, hasItemInArray(that(hasName(that(endsWith(".log"))))));
        final int found = 0;
        final int fileCounter = 0;
        String previous = "";
        for (final File file : files) {
            final String actual = file.getName();
            if (previous.isEmpty()) {
                previous = actual;
            } else {
                assertNotSame("File names snould not be equal", previous, actual);
            }
        }
    }
}
