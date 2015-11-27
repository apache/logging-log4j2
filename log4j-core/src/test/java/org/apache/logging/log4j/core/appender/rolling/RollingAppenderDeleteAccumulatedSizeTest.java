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

import java.io.File;
import java.util.Arrays;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.datetime.FixedDateFormat;
import org.apache.logging.log4j.core.util.datetime.FixedDateFormat.FixedFormat;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.RuleChain;

import static org.junit.Assert.*;

/**
 *
 */
public class RollingAppenderDeleteAccumulatedSizeTest {
    private static final String CONFIG = "log4j-rolling-with-custom-delete-accum-size.xml";
    private static final String DIR = "target/rolling-with-delete-accum-size/test";

    private final LoggerContextRule ctx = new LoggerContextRule(CONFIG);

    @Rule
    public RuleChain chain = RuleChain.outerRule(new ExternalResource() {
        @Override
        protected void before() throws Throwable {
            deleteDir();
        }
    }).around(ctx);

    @Test
    public void testAppender() throws Exception {

        final Logger logger = ctx.getLogger();
        for (int i = 0; i < 10; ++i) {
            // 30 chars per message: each message triggers a rollover
            logger.debug("This is a test message number " + i); // 30 chars:
        }
        Thread.sleep(100); // Allow time for rollover to complete

        final File dir = new File(DIR);
        assertTrue("Dir " + DIR + " should exist", dir.exists());
        assertTrue("Dir " + DIR + " should contain files", dir.listFiles().length > 0);

        final File[] files = dir.listFiles();
        for (File file : files) {
            System.out.println(file + " (" + file.length() + "B) "
                    + FixedDateFormat.create(FixedFormat.ABSOLUTE).format(file.lastModified()));
        }
        assertEquals(Arrays.toString(files), 4, files.length);
        long total = 0;
        for (File file : files) {
            // sometimes test-6.log remains
            assertTrue("unexpected file " + file, Arrays
                    .asList("test-6.log", "test-7.log", "test-8.log", "test-9.log", "test-10.log").contains(file.getName()));
            total += file.length();
        }
        assertTrue("accumulatedSize=" + total, total <= 500);
    }

    private static void deleteDir() {
        final File dir = new File(DIR);
        if (dir.exists()) {
            final File[] files = dir.listFiles();
            for (final File file : files) {
                file.delete();
            }
            dir.delete();
        }
    }
}
