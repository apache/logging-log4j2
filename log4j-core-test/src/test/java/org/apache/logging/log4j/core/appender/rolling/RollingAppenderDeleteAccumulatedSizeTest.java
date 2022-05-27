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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 */
@Tag("sleepy")
public class RollingAppenderDeleteAccumulatedSizeTest {
    private static final String CONFIG = "log4j-rolling-with-custom-delete-accum-size.xml";
    private static final String DIR = "target/rolling-with-delete-accum-size/test";

    @Test
    @CleanUpDirectories(DIR)
    @LoggerContextSource(value = CONFIG, timeout = 10)
    public void testAppender(final LoggerContext context) throws Exception {

        final Logger logger = context.getLogger(getClass());
        for (int i = 0; i < 10; ++i) {
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
        assertEquals(4, files.length, Arrays.toString(files));
        long total = 0;
        for (final File file : files) {
            // sometimes test-6.log remains
            assertTrue(file.getName().startsWith("test-"), "unexpected file " + file);
            total += file.length();
        }
        assertTrue(total <= 500, "accumulatedSize=" + total);
    }
}
