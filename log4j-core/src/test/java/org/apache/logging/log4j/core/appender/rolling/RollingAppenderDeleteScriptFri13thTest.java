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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.RuleChain;

import static org.junit.Assert.*;

/**
 *
 */
public class RollingAppenderDeleteScriptFri13thTest {

    private static final String CONFIG = "log4j-rolling-with-custom-delete-script-fri13th.xml";
    private static final String DIR = "target/rolling-with-delete-script-fri13th/test";

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
        final File dir = new File(DIR);
        dir.mkdirs();
        for (int i = 1; i <= 30; i++) {
            String day = i < 10 ? "0" + i : "" + i;
            new File(dir, "test-201511" + day + "-0.log").createNewFile();
        }
        assertEquals("Dir " + DIR + " filecount", 30, dir.listFiles().length);

        final Logger logger = ctx.getLogger();
        // Trigger the rollover
        for (int i = 0; i < 2; ++i) {
            // 30 chars per message: each message triggers a rollover
            logger.debug("This is a test message number " + i); // 30 chars:
        }
        Thread.sleep(100); // Allow time for rollover to complete

        assertTrue("Dir " + DIR + " should exist", dir.exists());
        assertTrue("Dir " + DIR + " filecount=" + dir.listFiles().length, dir.listFiles().length >= 30);

        final File[] files = dir.listFiles();
        for (File file : files) {
            System.out.println(file);
        }
        for (File file : files) {
            assertTrue(file.getName() + " starts with 'test-'", file.getName().startsWith("test-"));
            assertTrue(file.getName() + " ends with '.log'", file.getName().endsWith(".log"));
            String strDate = file.getName().substring(5, 13);
            assertFalse(file + " is not Fri 13th", strDate.endsWith("13"));
        }
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
