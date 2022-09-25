/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.core.appender.rolling;

import static org.apache.logging.log4j.hamcrest.Descriptors.that;
import static org.apache.logging.log4j.hamcrest.FileMatchers.hasName;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 * LOG4J2-1725.
 */
public class RollingAppenderReconfigureTest {

    private static final String DIR = "target/rolling1";

    private static final String CONFIG = "log4j-rolling-reconfigure.xml";

    private static final File CONFIG_FILE = new File("target/test-classes/", CONFIG);

    public static LoggerContextRule loggerContextRule = LoggerContextRule
            .createShutdownTimeoutLoggerContextRule(CONFIG);

    @Rule
    public RuleChain chain = loggerContextRule.withCleanFoldersRule(DIR);

    private Logger logger;

    @Before
    public void setUp() throws Exception {
        this.logger = loggerContextRule.getLogger(RollingAppenderReconfigureTest.class.getName());
    }

    @Test
    public void testReconfigure() throws Exception {
        for (int i = 0; i < 500; ++i) {
            final String message = "This is test message number " + i;
            logger.debug(message);
        }

        final File dir = new File(DIR);
        assertTrue("Directory not created", dir.exists());
        final File[] files = dir.listFiles();
        assertThat(files, hasItemInArray(that(hasName(that(endsWith(".current"))))));
        assertThat(files, hasItemInArray(that(hasName(that(endsWith(".rolled"))))));

        final String originalXmlConfig = FileUtils.readFileToString(CONFIG_FILE, "UTF-8");
        try {
            final String updatedXmlConfig = originalXmlConfig.replace("target/rolling1/rollingtest.%i.rolled",
                    "target/rolling1/rollingtest.%i.reconfigured");
            FileUtils.write(CONFIG_FILE, updatedXmlConfig, "UTF-8");

            // Reconfigure
            loggerContextRule.getLoggerContext().reconfigure();

            for (int i = 0; i < 500; ++i) {
                final String message = "This is test message number " + i;
                logger.debug(message);
            }

            final File[] filesAfterReconfigured = dir.listFiles();
            assertThat(filesAfterReconfigured, hasItemInArray(that(hasName(that(endsWith(".reconfigured"))))));
            assertThat(filesAfterReconfigured, hasItemInArray(that(hasName(that(endsWith(".current"))))));
            assertThat(filesAfterReconfigured, hasItemInArray(that(hasName(that(endsWith(".rolled"))))));
        } finally {
            FileUtils.write(CONFIG_FILE, originalXmlConfig, "UTF-8");
        }
    }
}
