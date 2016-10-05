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
package org.apache.logging.log4j.core.layout;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.BasicConfigurationFactory;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.net.Facility;
import org.apache.logging.log4j.junit.ThreadContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.List;

import static org.junit.Assert.*;

/**
 *
 */
public class Rfc3164SyslogLayoutTest {

    LoggerContext ctx = LoggerContext.getContext();
    Logger root = ctx.getRootLogger();

    static ConfigurationFactory cf = new BasicConfigurationFactory();

    @Rule
    public final ThreadContextRule threadContextRule = new ThreadContextRule();

    @BeforeClass
    public static void setupClass() {
        ConfigurationFactory.setConfigurationFactory(cf);
        final LoggerContext ctx = LoggerContext.getContext();
        ctx.reconfigure();
    }

    @AfterClass
    public static void cleanupClass() {
        ConfigurationFactory.removeConfigurationFactory(cf);
    }

    @Test
    public void testGetLogContent() throws Exception {

        for (final Appender appender : root.getAppenders().values()) {
            root.removeAppender(appender);
        }

        // set up appender
        final Rfc3164SyslogLayout layout =
                Rfc3164SyslogLayout.createLayout(Facility.LOCAL0, false, null,
                        null, "%m %X", null, null, null, true);
        assertNotNull(layout);

        //ConsoleAppender appender = new ConsoleAppender("Console", layout);
        final ListAppender appender = new ListAppender("List", null, layout, true, false);
        appender.start();

        // set appender on root and set level to debug
        root.addAppender(appender);
        root.setLevel(Level.DEBUG);

        ThreadContext.put("key1", "value1");

        root.debug("filled mdc");

        appender.stop();

        final List<String> list = appender.getMessages();

        assertEquals(1, list.size());
        final String log = list.get(0);
        assertTrue(log, log.contains("filled mdc"));
        assertTrue(log, log.endsWith(" {key1=value1}"));
    }

    @Test
    public void testCreateLayoutDefault() throws Exception {

        final Rfc3164SyslogLayout layout =
                Rfc3164SyslogLayout.createLayout(null, false, null, null, null, null, null, null, true);
        assertNotNull(layout);
    }

    @Test
    public void testCreateLayout() throws Exception {

        final Rfc3164SyslogLayout layout =
                Rfc3164SyslogLayout.createLayout(Facility.LOCAL7, true, "-",
                        Charset.forName("UTF-16"), "%m %X", null, null, null, false);
        assertNotNull(layout);
    }
}