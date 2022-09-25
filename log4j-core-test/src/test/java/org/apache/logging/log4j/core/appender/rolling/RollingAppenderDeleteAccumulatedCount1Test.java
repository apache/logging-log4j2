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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.datetime.FixedDateFormat;
import org.apache.logging.log4j.core.util.datetime.FixedDateFormat.FixedFormat;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 * Tests that sibling conditions are invoked in configured order.
 * This does not work for properties configurations. Use nested conditions instead.
 */
public class RollingAppenderDeleteAccumulatedCount1Test {
    private static final String CONFIG = "log4j-rolling-with-custom-delete-accum-count1.xml";
    private static final String DIR = "target/rolling-with-delete-accum-count1/test";

    private final LoggerContextRule loggerContextRule = LoggerContextRule.createShutdownTimeoutLoggerContextRule(CONFIG);

    @Rule
    public RuleChain chain = loggerContextRule.withCleanFoldersRule(DIR);

    @Test
    public void testAppender() throws Exception {
        final Path p1 = writeTextTo(DIR + "/my-1.log"); // glob="test-*.log"
        final Path p2 = writeTextTo(DIR + "/my-2.log");
        final Path p3 = writeTextTo(DIR + "/my-3.log");
        final Path p4 = writeTextTo(DIR + "/my-4.log");
        final Path p5 = writeTextTo(DIR + "/my-5.log");

        final Logger logger = loggerContextRule.getLogger();
        for (int i = 0; i < 10; ++i) {
            updateLastModified(p1, p2, p3, p4, p5); // make my-*.log files most recent

            // 30 chars per message: each message triggers a rollover
            logger.debug("This is a test message number " + i); // 30 chars:
        }
        Thread.sleep(100); // Allow time for rollover to complete

        final File dir = new File(DIR);
        assertTrue("Dir " + DIR + " should exist", dir.exists());
        assertTrue("Dir " + DIR + " should contain files", dir.listFiles().length > 0);

        final File[] files = dir.listFiles();
        for (final File file : files) {
            System.out.println(file + " (" + file.length() + "B) "
                    + FixedDateFormat.create(FixedFormat.ABSOLUTE).format(file.lastModified()));
        }
        final List<String> expected = Arrays.asList("my-1.log", "my-2.log", "my-3.log", "my-4.log", "my-5.log");
        assertEquals(Arrays.toString(files), expected.size() + 6, files.length);
        for (final File file : files) {
            if (!expected.contains(file.getName()) && !file.getName().startsWith("test-")) {
                fail("unexpected file" + file);
            }
        }
    }

    private void updateLastModified(final Path... paths) throws IOException {
        for (final Path path : paths) {
            Files.setLastModifiedTime(path, FileTime.fromMillis(System.currentTimeMillis() + 2000));
        }
    }

    private Path writeTextTo(final String location) throws IOException {
        final Path path = Paths.get(location);
        Files.createDirectories(path.getParent());
        try (BufferedWriter buffy = Files.newBufferedWriter(path, Charset.defaultCharset())) {
            buffy.write("some text");
            buffy.newLine();
            buffy.flush();
        }
        return path;
    }
}
