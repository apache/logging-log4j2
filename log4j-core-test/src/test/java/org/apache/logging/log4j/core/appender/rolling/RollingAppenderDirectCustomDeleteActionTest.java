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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.regex.Pattern;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.test.junit.CleanFoldersRuleExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 *
 */
public class RollingAppenderDirectCustomDeleteActionTest implements RolloverListener {

    private static final String CONFIG = "log4j-rolling-direct-with-custom-delete.xml";
    private static final String DIR = "target/rolling-direct-with-delete/test";

    @RegisterExtension
    CleanFoldersRuleExtension extension = new CleanFoldersRuleExtension(
            DIR,
            CONFIG,
            RollingAppenderDirectCustomDeleteActionTest.class.getName(),
            this.getClass().getClassLoader());

    private boolean fileFound = false;

    @Test
    public void testAppender(final LoggerContext loggerContext) throws Exception {
        final Logger logger = loggerContext.getLogger(RollingAppenderDirectCustomDeleteActionTest.class.getName());
        final RollingFileAppender app = loggerContext.getConfiguration().getAppender("RollingFile");
        assertNotNull(app, "No RollingFileAppender");
        app.getManager().addRolloverListener(this);
        // Trigger the rollover
        for (int i = 0; i < 10; ++i) {
            // 30 chars per message: each message triggers a rollover
            logger.debug("This is a test message number " + i); // 30 chars:
        }
        Thread.sleep(100); // Allow time for rollover to complete

        final File dir = new File(DIR);
        assertTrue(dir.exists(), "Dir " + DIR + " should exist");
        assertTrue(dir.listFiles().length > 0, "Dir " + DIR + " should contain files");

        final File[] files = dir.listFiles();
        assertNotNull(files, "No fiels");
        System.out.println(files[0].getName());
        final long count = Arrays.stream(files)
                .filter((f) -> f.getName().endsWith("test-4.log"))
                .count();
        assertTrue(fileFound, "Deleted file was not created");
        assertEquals(0, count, "File count expected: 0, actual: " + count);
    }

    public static void main(final String[] args) {
        final Pattern p = Pattern.compile("test-.?[2,4,6,8,0]\\.log\\.gz");
        for (int i = 0; i < 16; i++) {
            final String str = "test-" + i + ".log.gz";
            final java.util.regex.Matcher m = p.matcher(str);
            System.out.println(m.matches() + ": " + str);
        }
    }

    @Override
    public void rolloverTriggered(final String fileName) {
        if (fileName.endsWith("test-4.log")) {
            fileFound = true;
        }
    }

    @Override
    public void rolloverComplete(final String fileName) {}
}
