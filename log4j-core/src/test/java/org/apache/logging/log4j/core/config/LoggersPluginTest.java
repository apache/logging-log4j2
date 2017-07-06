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

package org.apache.logging.log4j.core.config;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests LoggersPlugin.
 */
public class LoggersPluginTest {
    private static final String CONFIG = "multipleRootLoggersTest.xml";

    @ClassRule
    public static LoggerContextRule context = new LoggerContextRule(CONFIG);

    @Test
    public void testEmptyAttribute() throws Exception {
        final Logger logger = LogManager.getLogger();
        logger.info("Test");
        final StatusData data = StatusLogger.getLogger().getStatusData().get(0);
        //System.out.println(data.getFormattedStatus());

        assertEquals(Level.ERROR, data.getLevel());
        assertTrue(data.getMessage().getFormattedMessage().contains("multiple root loggers"));
    }
}