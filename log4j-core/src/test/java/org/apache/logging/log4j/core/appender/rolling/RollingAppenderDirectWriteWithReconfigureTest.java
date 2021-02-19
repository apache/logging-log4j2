/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache license, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the license for the specific language
 * governing permissions and limitations under the license.
 */
package org.apache.logging.log4j.core.appender.rolling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 *
 */
public class RollingAppenderDirectWriteWithReconfigureTest {

    private static final String CONFIG = "log4j-rolling-direct-reconfigure.xml";

    private static final String DIR = "target/rolling-direct-reconfigure";

    private static final int MAX_TRIES = 10;

    public static LoggerContextRule loggerContextRule = LoggerContextRule
            .createShutdownTimeoutLoggerContextRule(CONFIG);

    @Rule
    public RuleChain chain = loggerContextRule.withCleanFoldersRule(DIR);

    private Logger logger;

    @Before
    public void setUp() throws Exception {
        this.logger = loggerContextRule.getLogger(RollingAppenderDirectWriteWithReconfigureTest.class.getName());
    }

    @Test
    public void testRollingFileAppenderWithReconfigure() throws Exception {
        logger.debug("Before reconfigure");

        @SuppressWarnings("resource") // managed by the rule.
        final LoggerContext context = loggerContextRule.getLoggerContext();
        Configuration config = context.getConfiguration();
        context.setConfigLocation(new URI(CONFIG));
        context.reconfigure();
        logger.debug("Force a rollover");
        final File dir = new File(DIR);
        for (int i = 0; i < MAX_TRIES; ++i) {
            Thread.sleep(200);
            if (config != context.getConfiguration()) {
                break;
            }
        }

        assertThat(dir.exists() && dir.listFiles().length > 0).describedAs("Directory not created").isTrue();
        final File[] files = dir.listFiles();
        assertThat(files).isNotNull();
        assertThat(dir.listFiles().length).isEqualTo(2);
    }
}
