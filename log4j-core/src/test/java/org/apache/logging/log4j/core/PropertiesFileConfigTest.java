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

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

/**
 *
 */
public class PropertiesFileConfigTest {

    private static final String CONFIG = "target/test-classes/log4j-test2.properties";

    @ClassRule
    public static LoggerContextRule context = new LoggerContextRule(CONFIG);

    private final org.apache.logging.log4j.Logger logger = context.getLogger("LoggerTest");

    @Before
    public void before() {
        context.getListAppender("List").clear();
    }

    @Test
    public void testReconfiguration() throws Exception {
        final Configuration oldConfig = context.getConfiguration();
        final int MONITOR_INTERVAL_SECONDS = 5;
        final File file = new File(CONFIG);
        final long orig = file.lastModified();
        final long newTime = orig + 10000;
        assertTrue("setLastModified should have succeeded.", file.setLastModified(newTime));
        TimeUnit.SECONDS.sleep(MONITOR_INTERVAL_SECONDS + 1);
        for (int i = 0; i < 17; ++i) {
            logger.debug("Reconfigure");
        }
        final int loopCount = 0;
        Configuration newConfig;
        do {
            Thread.sleep(100);
            newConfig = context.getConfiguration();
        } while (newConfig == oldConfig && loopCount < 5);
        assertNotSame("Reconfiguration failed", newConfig, oldConfig);
    }
}

