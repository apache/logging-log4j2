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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.CleanFoldersRuleExtension;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.Integers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 *
 */
public class RollingAppenderDeleteScriptTest {

    private static final String CONFIG = "log4j-rolling-with-custom-delete-script.xml";
    private static final String DIR = "target/rolling-with-delete-script/test";

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
        final Logger logger = loggerContext.getLogger(RollingAppenderDeleteScriptTest.class.getName());
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
        for (final File file : files) {
            System.out.println(file);
        }
        for (final File file : files) {
            assertTrue(file.getName().startsWith("test-"), file.getName() + " starts with 'test-'");
            assertTrue(file.getName().endsWith(".log"), file.getName() + " ends with '.log'");
            final String strIndex = file.getName().substring(5, file.getName().indexOf('.'));
            final int index = Integers.parseInt(strIndex);
            assertEquals(1, index % 2, file + " should have odd index");
        }
    }
}
