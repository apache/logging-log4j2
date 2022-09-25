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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 *
 */
public class RollingAppenderDeleteMaxDepthTest {

    private static final String CONFIG = "log4j-rolling-with-custom-delete-maxdepth.xml";
    private static final String DIR = "target/rolling-with-delete-depth/test";

    private final LoggerContextRule loggerContextRule = LoggerContextRule.createShutdownTimeoutLoggerContextRule(CONFIG);

    @Rule
    public RuleChain chain = loggerContextRule.withCleanFoldersRule(DIR);

    @Test
    public void testAppender() throws Exception {
        // create some files that match the glob but exceed maxDepth
        final Path p1 = writeTextTo(DIR + "/1/test-4.log"); // glob="**/test-4.log"
        final Path p2 = writeTextTo(DIR + "/2/test-4.log");
        final Path p3 = writeTextTo(DIR + "/1/2/test-4.log");
        final Path p4 = writeTextTo(DIR + "/1/2/3/test-4.log");

        final Logger logger = loggerContextRule.getLogger();
        for (int i = 0; i < 10; ++i) {
            // 30 chars per message: each message triggers a rollover
            logger.debug("This is a test message number " + i); // 30 chars:
        }
        Thread.sleep(100); // Allow time for rollover to complete

        final File dir = new File(DIR);
        assertTrue("Dir " + DIR + " should exist", dir.exists());
        assertTrue("Dir " + DIR + " should contain files", dir.listFiles().length > 0);

        final File[] files = dir.listFiles();
        final List<String> expected = Arrays.asList("1", "2", "test-1.log", "test-2.log", "test-3.log");
        assertEquals(Arrays.toString(files), expected.size(), files.length);
        for (final File file : files) {
            assertTrue("test-4.log should have been deleted",
                    expected.contains(file.getName()));
        }

        assertTrue(p1 + " should not have been deleted", Files.exists(p1));
        assertTrue(p2 + " should not have been deleted", Files.exists(p2));
        assertTrue(p3 + " should not have been deleted", Files.exists(p3));
        assertTrue(p4 + " should not have been deleted", Files.exists(p4));
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

    public static void main(final String[] args) {
        final Pattern p = Pattern.compile("test-.?[2,4,6,8,0]\\.log\\.gz");
        for (int i = 0; i < 16; i++) {
            final String str = "test-" + i + ".log.gz";
            final java.util.regex.Matcher m = p.matcher(str);
            System.out.println(m.matches() + ": " + str);
        }
    }
}
