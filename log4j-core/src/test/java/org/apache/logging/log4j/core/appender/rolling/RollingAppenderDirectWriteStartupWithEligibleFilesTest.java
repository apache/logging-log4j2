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

import org.apache.commons.io.file.PathUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.util.datetime.FixedDateFormat;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 *
 */
public class RollingAppenderDirectWriteStartupWithEligibleFilesTest {

    private static final String CONFIG = "log4j-rolling-direct-startup-with-eligible-files.xml";

    private static final String DIR = "target/rolling-direct-startup-with-eligible-files";

    private static final String FILE = "eligible-files-test-18.log";

    private static final String MESSAGE = "This is a test message number ";

    @Rule
    public LoggerContextRule loggerContextRule = LoggerContextRule
            .createShutdownTimeoutLoggerContextRule(CONFIG);

    @BeforeClass
    public static void beforeClass() throws Exception {
        final Path logDir = Paths.get(DIR);

        if(Files.exists(logDir)){
            PathUtils.cleanDirectory(logDir);
        }


        writeTextTo(DIR + "/eligible-files-test-13.log", MESSAGE + 13);
        writeTextTo(DIR + "/eligible-files-test-14.log", MESSAGE + 14);
        writeTextTo(DIR + "/eligible-files-test-15.log", MESSAGE + 15);
        writeTextTo(DIR + "/eligible-files-test-16.log", MESSAGE + 16);
        writeTextTo(DIR + "/eligible-files-test-17.log", MESSAGE + 17);

    }

    private static Path writeTextTo(final String location, final String message) throws IOException {
        final Path path = Paths.get(location);
        Files.createDirectories(path.getParent());
        Files.write(path, message.getBytes());
        return path;
    }

    @Test
    public void testRollingDirectWriteWithAlreadyEligibleFiles() throws Exception {

        final Logger logger = loggerContextRule.getLogger();

        logger.debug("This is a test message number 18"); // 32 chars:

        Thread.sleep(100); // Allow time for rollover to complete

        final File dir = new File(DIR);
        assertTrue("Dir " + DIR + " should exist", dir.exists());
        assertTrue("Dir " + DIR + " should contain files", dir.listFiles().length > 0);

        final File[] files = dir.listFiles();
        for (final File file : files) {
            System.out.println(file + " (" + file.length() + "B) "
                    + FixedDateFormat.create(FixedDateFormat.FixedFormat.ABSOLUTE).format(file.lastModified()));
        }


        final List<String> expected = Arrays.asList(
                "eligible-files-test-14.log",
                "eligible-files-test-15.log",
                "eligible-files-test-16.log",
                "eligible-files-test-17.log",
                FILE);
        assertEquals(Arrays.toString(files), expected.size(), files.length);
        for (final File file : files) {
            if (!expected.contains(file.getName()) && !file.getName().startsWith("eligible-files-test-")) {
                fail("unexpected file" + file);
            }
        }

        assertTrue(FILE + "Should exist at the startup", Arrays.asList(files).contains(new File(DIR + "/" + FILE)));
    }
}
