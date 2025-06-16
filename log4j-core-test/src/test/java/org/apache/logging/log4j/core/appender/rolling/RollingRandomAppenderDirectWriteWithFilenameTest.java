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

import static org.junit.Assert.assertFalse;

import java.io.File;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 *
 */
public class RollingRandomAppenderDirectWriteWithFilenameTest {

    private static final String CONFIG = "log4j2-random-1833.xml";

    private static final String DIR = "target/random-1833";

    public static LoggerContextRule loggerContextRule =
            LoggerContextRule.createShutdownTimeoutLoggerContextRule(CONFIG);

    @Rule
    public RuleChain chain = loggerContextRule.withCleanFoldersRule(DIR);

    private Logger logger;

    @Before
    public void setUp() {
        this.logger = loggerContextRule.getLogger(RollingRandomAppenderDirectWriteWithFilenameTest.class.getName());
    }

    @Test
    public void testAppender() {
        final File dir = new File(DIR);
        assertFalse("Directory created", dir.exists());
    }
}
