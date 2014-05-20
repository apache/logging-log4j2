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

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.NetUtils;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class HostNameTest {

    private static final String CONFIG = "log4j-test2.xml";
    private static Configuration config;
    private static ListAppender app;
    private static ListAppender host;
    private static RollingFileAppender hostFile;
    private static LoggerContext ctx;

    @BeforeClass
    public static void setupClass() {
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
        ctx = (LoggerContext) LogManager.getContext(false);
    }

    @AfterClass
    public static void cleanupClass() {
        System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        ctx.reconfigure();
        StatusLogger.getLogger().reset();
    }

    @Before
    public void before() {
        config = ctx.getConfiguration();
        for (final Map.Entry<String, Appender> entry : config.getAppenders().entrySet()) {
            if (entry.getKey().equals("List")) {
                app = (ListAppender) entry.getValue();
            } else if (entry.getKey().equals("HostTest")) {
                host = (ListAppender) entry.getValue();
            } else if (entry.getKey().equals("HostFile")) {
                hostFile = (RollingFileAppender) entry.getValue();
            }
        }
        assertNotNull("No Host FileAppender", hostFile);
        app.clear();
        host.clear();
    }


    @Test
    public void testHostname() {
        final org.apache.logging.log4j.Logger testLogger = LogManager.getLogger("org.apache.logging.log4j.hosttest");
        testLogger.debug("Hello, {}", "World");
        final List<String> msgs = host.getMessages();
        assertTrue("Incorrect number of events. Expected 1, actual " + msgs.size(), msgs.size() == 1);
        String expected = NetUtils.getLocalHostname() + Constants.LINE_SEPARATOR;
        assertTrue("Incorrect hostname - expected " + expected + " actual - " + msgs.get(0),
            msgs.get(0).endsWith(expected));
        assertNotNull("No Host FileAppender file name", hostFile.getFileName());
        expected = "target/" + NetUtils.getLocalHostname() + ".log";
        String name = hostFile.getFileName();
        assertTrue("Incorrect HostFile FileAppender file name - expected " + expected + " actual - " + name,
            name.equals(expected));
        name = hostFile.getFilePattern();
        assertNotNull("No file pattern", name);
        expected = "target/" + NetUtils.getLocalHostname() + "-%d{MM-dd-yyyy}-%i.log";
        assertTrue("Incorrect HostFile FileAppender file pattern - expected " + expected + " actual - " + name,
            name.equals(expected));

    }
}
