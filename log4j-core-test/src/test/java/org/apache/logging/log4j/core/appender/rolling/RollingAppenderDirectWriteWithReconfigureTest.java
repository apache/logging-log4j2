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

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.test.junit.CleanUpDirectories;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 */
@Tag("sleepy")
public class RollingAppenderDirectWriteWithReconfigureTest {

    private static final String CONFIG = "log4j-rolling-direct-reconfigure.xml";

    private static final String DIR = "target/rolling-direct-reconfigure";

    private static final int MAX_TRIES = 10;

    @Test
    @CleanUpDirectories(DIR)
    @LoggerContextSource(value = CONFIG, timeout = 10)
    public void testRollingFileAppenderWithReconfigure(final LoggerContext context) throws Exception {
        final var logger = context.getLogger(getClass());
        logger.debug("Before reconfigure");

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

        assertTrue(dir.exists(), "Directory not created");
        final File[] files = dir.listFiles();
        assertNotNull(files);
        assertTrue(files.length > 0, "Directory not created");
        assertThat(files.length, is(equalTo(2)));
    }
}
