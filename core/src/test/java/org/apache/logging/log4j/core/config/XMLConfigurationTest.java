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
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.filter.ThreadContextMapFilter;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

/**
 *
 */
public class XMLConfigurationTest {

    private static final String CONFIG = "log4j-test1.xml";
    private static final String LOGFILE = "target/test.log";

    @BeforeClass
    public static void setupClass() {
        System.setProperty(XMLConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
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
        System.clearProperty(XMLConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        final LoggerContext ctx = (LoggerContext) LogManager.getContext();
        ctx.reconfigure();
        StatusLogger.getLogger().reset();
    }

    @Test
    public void testLogger() {
        final Logger logger = LogManager.getLogger("org.apache.logging.log4j.test1.Test");
        assertTrue(logger instanceof org.apache.logging.log4j.core.Logger);
        final org.apache.logging.log4j.core.Logger l = (org.apache.logging.log4j.core.Logger) logger;
        assertEquals(Level.DEBUG, l.getLevel());
        final int filterCount = l.filterCount();
        assertTrue("number of filters - " + filterCount, filterCount == 1);
        final Iterator<Filter> iter = l.getFilters();
        final Filter filter = iter.next();
        assertTrue(filter instanceof ThreadContextMapFilter);
        final Map<String, Appender<?>> appenders = l.getAppenders();
        assertNotNull(appenders);
        assertTrue("number of appenders = " + appenders.size(), appenders.size() == 1);
        final Appender a = appenders.get("STDOUT");
        assertNotNull(a);
        assertEquals(a.getName(), "STDOUT");
    }

    public void testConfiguredAppenders() {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext();
        final Configuration c = ctx.getConfiguration();
        final Map<String, Appender<?>> apps = c.getAppenders();
        assertNotNull(apps);
        assertEquals(apps.size(), 3);
    }

    @Test
    public void logToFile() throws Exception {
        final FileOutputStream fos = new FileOutputStream(LOGFILE, false);
        fos.flush();
        fos.close();
        final Logger logger = LogManager.getLogger("org.apache.logging.log4j.test2.Test");
        logger.debug("This is a test");
        final DataInputStream is = new DataInputStream(new BufferedInputStream(new FileInputStream(LOGFILE)));
        try {
            int count = 0;
            String str = "";
            while (is.available() != 0) {
                str = is.readLine();
                ++count;
            }
            assertTrue("Incorrect count " + count, count == 1);
            assertTrue("Bad data", str.endsWith("This is a test"));
        } finally {
            is.close();
        }
    }

}
