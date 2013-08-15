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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 *
 */
public class FileOutputTest {

    private static final String CONFIG = "log4j-filetest.xml";
    private static final String STATUS_LOG = "target/status.log";

    @BeforeClass
    public static void setupClass() {
        final File file = new File(STATUS_LOG);
        file.delete();
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
        final LoggerContext ctx = (LoggerContext) LogManager.getContext();
        final Configuration config = ctx.getConfiguration();
        if (config instanceof XMLConfiguration) {
            final String name = ((XMLConfiguration) config).getName();
            if (name == null || !name.equals("XMLConfigTest")) {
                ctx.reconfigure();
            }
        } else {
            ctx.reconfigure();
        }
    }

    @AfterClass
    public static void cleanupClass() {
        System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        final LoggerContext ctx = (LoggerContext) LogManager.getContext();
        ctx.reconfigure();
        StatusLogger.getLogger().reset();
        final File file = new File(STATUS_LOG);
        file.delete();
    }

    @Test
    public void testConfig() {
        final File file = new File(STATUS_LOG);
        assertTrue("Status output file does not exist", file.exists());
        assertTrue("File is empty", file.length() > 0);
    }

}
