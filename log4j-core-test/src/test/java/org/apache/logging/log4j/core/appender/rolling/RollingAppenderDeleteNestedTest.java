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
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.time.internal.format.FixedDateFormat;
import org.apache.logging.log4j.core.time.internal.format.FixedDateFormat.FixedFormat;
import org.apache.logging.log4j.test.junit.CleanUpDirectories;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 */
@Tag("sleepy")
public class RollingAppenderDeleteNestedTest {
    private static final String CONFIG = "log4j-rolling-with-custom-delete-nested.xml";
    private static final String DIR = "target/rolling-with-delete-nested/test";

    @Test
    @CleanUpDirectories(DIR)
    @LoggerContextSource(value = CONFIG, timeout = 10)
    public void testAppender(final LoggerContext context) throws Exception {
        final Path p1 = writeTextTo(DIR + "/my-1.log"); // glob="test-*.log"
        final Path p2 = writeTextTo(DIR + "/my-2.log");
        final Path p3 = writeTextTo(DIR + "/my-3.log");
        final Path p4 = writeTextTo(DIR + "/my-4.log");
        final Path p5 = writeTextTo(DIR + "/my-5.log");

        final Logger logger = context.getLogger(getClass());
        for (int i = 0; i < 10; ++i) {
            updateLastModified(p1, p2, p3, p4, p5); // make my-*.log files most recent

            // 30 chars per message: each message triggers a rollover
            logger.debug("This is a test message number " + i); // 30 chars:
        }
        Thread.sleep(100); // Allow time for rollover to complete

        final File dir = new File(DIR);
        assertTrue(dir.exists(), "Dir " + DIR + " should exist");

        final File[] files = dir.listFiles();
        assertNotNull(files);
        assertTrue(files.length > 0, "Dir " + DIR + " should contain files");
        for (final File file : files) {
            BasicFileAttributes fileAttributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            System.out.println(file + " (" + fileAttributes.size() + "B) "
                    + FixedDateFormat.create(FixedFormat.ABSOLUTE).format(fileAttributes.lastModifiedTime().toMillis()));
        }

        final List<String> expected = Arrays.asList("my-1.log", "my-2.log", "my-3.log", "my-4.log", "my-5.log");
        assertEquals(expected.size() + 3, files.length, Arrays.toString(files));
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
