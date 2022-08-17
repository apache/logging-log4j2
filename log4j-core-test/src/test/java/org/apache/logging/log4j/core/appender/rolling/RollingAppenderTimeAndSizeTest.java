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
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.test.junit.CleanUpDirectories;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Random;

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
public class RollingAppenderTimeAndSizeTest {

    private static final String CONFIG = "log4j-rolling3.xml";

    private static final String DIR = "target/rolling3/test";

    @Test
    @CleanUpDirectories(DIR)
    @LoggerContextSource(value = CONFIG, timeout = 10)
    public void testAppender(final Logger logger) throws Exception {
        Random rand = new Random();
        final File logFile = new File("target/rolling3/rollingtest.log");
        assertTrue(logFile.exists(), "target/rolling3/rollingtest.log does not exist");
        FileTime time = (FileTime) Files.getAttribute(logFile.toPath(), "creationTime");
        for (int j = 0; j < 100; ++j) {
            int count = rand.nextInt(50);
            for (int i = 0; i < count; ++i) {
                logger.debug("This is test message number " + i);
            }
            Thread.sleep(rand.nextInt(50));
        }
        Thread.sleep(50);
        final File dir = new File(DIR);
        assertTrue(dir.exists() && dir.listFiles().length > 0, "Directory not created");
        final File[] files = dir.listFiles();
        assertNotNull(files);
        Arrays.sort(files);
        assertNotNull(files);
        assertThat(files, hasItemInArray(that(hasName(that(endsWith(".log"))))));
        int fileCounter = 0;
        String previous = "";
        for (final File file : files) {
            final String actual = file.getName();
            final String[] fileParts = actual.split("[_.]");
            fileCounter = previous.equals(fileParts[1]) ? ++fileCounter : 1;
            previous = fileParts[1];
            assertEquals(Integer.toString(fileCounter), fileParts[2],
                    "Incorrect file name. Expected counter value of " + fileCounter + " in " + actual);
        }
        FileTime endTime = (FileTime) Files.getAttribute(logFile.toPath(), "creationTime");
        assertNotEquals(time, endTime, "Creation times are equal");
    }
}
