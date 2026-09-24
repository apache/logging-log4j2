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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalDate;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.CleanFoldersRuleExtension;
import org.apache.logging.log4j.core.util.Constants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class RollingAppenderDeleteScriptFri13thTest {

    private static final String CONFIG = "log4j-rolling-with-custom-delete-script-fri13th.xml";
    private static final String DIR = "target/rolling-with-delete-script-fri13th/test";

    @BeforeAll
    public static void beforeAll() {
        System.setProperty(Constants.SCRIPT_LANGUAGES, "Groovy, Javascript");
    }

    @RegisterExtension
    CleanFoldersRuleExtension extension = new CleanFoldersRuleExtension(
            DIR,
            CONFIG,
            RollingAppenderDeleteScriptTest.class.getName(),
            this.getClass().getClassLoader());

    @Test
    public void testAppender(final LoggerContext loggerContext) throws Exception {
        LocalDate now = LocalDate.now();
        // Ignore on Friday 13th
        assumeFalse(now.getDayOfWeek() == DayOfWeek.FRIDAY && now.getDayOfMonth() == 13);
        final File dir = new File(DIR);
        dir.mkdirs();
        for (int i = 1; i <= 30; i++) {
            final String day = i < 10 ? "0" + i : "" + i;
            new File(dir, "test-201511" + day + "-0.log").createNewFile();
        }
        assertEquals(30, dir.listFiles().length, "Dir " + DIR + " filecount");

        final Logger logger = loggerContext.getLogger(RollingAppenderDeleteScriptFri13thTest.class.getName());
        // Trigger the rollover
        while (dir.listFiles().length < 32) {
            // 60+ chars per message: each message should trigger a rollover
            logger.debug("This is a very, very, very, very long test message............."); // 60+ chars:
            Thread.sleep(100); // Allow time for rollover to complete
        }

        final File[] files = dir.listFiles();
        for (final File file : files) {
            System.out.println(file);
        }
        for (final File file : files) {
            assertTrue(file.getName().startsWith("test-"), file.getName() + " starts with 'test-'");
            assertTrue(file.getName().endsWith(".log"), file.getName() + " ends with '.log'");
            final String strDate = file.getName().substring(5, 13);
            assertFalse(strDate.endsWith("20151113"), file + " is not Fri 13th");
        }
    }
}
