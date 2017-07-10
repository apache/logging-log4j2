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

package org.apache.logging.log4j.core.appender;

import java.util.List;

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
 * OutputStreamManager Tests.
 */
public class OutputStreamManagerTest {
    private static final String CONFIG = "multipleIncompatibleAppendersTest.xml";

    @ClassRule
    public static LoggerContextRule context = new LoggerContextRule(CONFIG);

    @Test
    public void narrow() throws Exception {
        final Logger logger = LogManager.getLogger(OutputStreamManagerTest.class);
        logger.info("test");
        final List<StatusData> statusData = StatusLogger.getLogger().getStatusData();
        StatusData data = statusData.get(0);
        if (data.getMessage().getFormattedMessage().contains("WindowsAnsiOutputStream")) {
            data = statusData.get(1);
        }
        assertEquals(Level.ERROR, data.getLevel());
        assertEquals("Could not create plugin of type class org.apache.logging.log4j.core.appender.RollingRandomAccessFileAppender for element RollingRandomAccessFile",
                data.getMessage().getFormattedMessage());
        assertEquals("org.apache.logging.log4j.core.config.ConfigurationException: Configuration has multiple incompatible Appenders pointing to the same resource 'target/multiIncompatibleAppender.log'",
                data.getThrowable().toString());
    }

}