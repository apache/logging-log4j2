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
package org.apache.logging.log4j.core;

import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.junit.Named;
import org.apache.logging.log4j.junit.LoggerContextSource;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@LoggerContextSource("log4j-test2.properties")
public class PropertiesFileConfigTest {

    private static final String CONFIG = "target/test-classes/log4j-test2.properties";

    private final org.apache.logging.log4j.Logger logger;

    public PropertiesFileConfigTest(final LoggerContext context) {
        logger = context.getLogger("LoggerTest");
    }

    @BeforeEach
    void clear(@Named("List") final ListAppender appender) {
        appender.clear();
    }

    @Test
    public void testReconfiguration(final LoggerContext context) throws Exception {
        final Configuration oldConfig = context.getConfiguration();
        final int MONITOR_INTERVAL_SECONDS = 5;
        final File file = new File(CONFIG);
        final long orig = file.lastModified();
        final long newTime = orig + 10000;
        assertTrue(file.setLastModified(newTime), "setLastModified should have succeeded.");
        TimeUnit.SECONDS.sleep(MONITOR_INTERVAL_SECONDS + 1);
        for (int i = 0; i < 17; ++i) {
            logger.info("Reconfigure");
        }
        int loopCount = 0;
        Configuration newConfig;
        do {
            Thread.sleep(100);
            newConfig = context.getConfiguration();
        } while (newConfig == oldConfig && loopCount++ < 5);
        assertNotSame(newConfig, oldConfig, "Reconfiguration failed");
    }
}

