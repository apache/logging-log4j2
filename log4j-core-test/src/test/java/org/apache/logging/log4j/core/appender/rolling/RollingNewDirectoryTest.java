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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 * Tests
 */
public class RollingNewDirectoryTest {
    private static final String CONFIG = "log4j-rolling-new-directory.xml";

    private static final String DIR = "target/rolling-new-directory";

    public static LoggerContextRule loggerContextRule =
            LoggerContextRule.createShutdownTimeoutLoggerContextRule(CONFIG);

    @Rule
    public RuleChain chain = loggerContextRule.withCleanFoldersRule(DIR);

    private Logger logger;

    @Before
    public void setUp() throws Exception {
        this.logger = loggerContextRule.getLogger(RollingNewDirectoryTest.class.getName());
    }

    @Test
    public void streamClosedError() throws Exception {
        for (int i = 0; i < 10; ++i) {
            logger.info("AAA");
            Thread.sleep(300);
        }
        final File dir = new File(DIR);
        assertNotNull("No directory created", dir);
        assertTrue("Child irectories not created", dir.exists() && dir.listFiles().length > 2);
    }
}
