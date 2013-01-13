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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.BasicConfigurationFactory;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertTrue;

/**
 *
 */
public class RFC5424LayoutTest {
    LoggerContext ctx = (LoggerContext) LogManager.getContext();
    Logger root = ctx.getLogger("");


    private static final String line1 = "ATM - - [RequestContext@3692 loginId=\"JohnDoe\"] starting mdc pattern test";
    private static final String line2 = "ATM - - [RequestContext@3692 loginId=\"JohnDoe\"] empty mdc";
    private static final String line3 = "ATM - - [RequestContext@3692 loginId=\"JohnDoe\"] filled mdc";
    private static final String line4 =
        "ATM - Audit [Transfer@18060 Amount=\"200.00\" FromAccount=\"123457\" ToAccount=\"123456\"]" +
        "[RequestContext@18060 ipAddress=\"192.168.0.120\" loginId=\"JohnDoe\"] Transfer Complete";

    static ConfigurationFactory cf = new BasicConfigurationFactory();

    @BeforeClass
    public static void setupClass() {
        StatusLogger.getLogger().setLevel(Level.OFF);
        ConfigurationFactory.setConfigurationFactory(cf);
        final LoggerContext ctx = (LoggerContext) LogManager.getContext();
        ctx.reconfigure();
    }

    @AfterClass
    public static void cleanupClass() {
        ConfigurationFactory.removeConfigurationFactory(cf);
    }

    /**
     * Test case for MDC conversion pattern.
     */
    @Test
    public void testLayout() throws Exception {
        for (final Appender appender : root.getAppenders().values()) {
            root.removeAppender(appender);
        }
        // set up appender
        final AbstractStringLayout layout = RFC5424Layout.createLayout("Local0", "Event", "3692", "true", "RequestContext",
            "true", null, "ATM", null, "key1, key2, locale", null, "loginId", null, null, null);
        final ListAppender appender = new ListAppender("List", null, layout, true, false);

        appender.start();

        // set appender on root and set level to debug
        root.addAppender(appender);
        root.setLevel(Level.DEBUG);

        ThreadContext.put("loginId", "JohnDoe");

        // output starting message
        root.debug("starting mdc pattern test");

        root.debug("empty mdc");

        ThreadContext.put("key1", "value1");
        ThreadContext.put("key2", "value2");

        root.debug("filled mdc");

        ThreadContext.put("ipAddress", "192.168.0.120");
        ThreadContext.put("locale", Locale.US.getDisplayName());
        try {
            final StructuredDataMessage msg = new StructuredDataMessage("Transfer@18060", "Transfer Complete", "Audit");
            msg.put("ToAccount", "123456");
            msg.put("FromAccount", "123457");
            msg.put("Amount", "200.00");
            root.info(MarkerManager.getMarker("EVENT"), msg);

            List<String> list = appender.getMessages();

            assertTrue("Expected line 1 to end with: " + line1 + " Actual " + list.get(0), list.get(0).endsWith(line1));
            assertTrue("Expected line 2 to end with: " + line2 + " Actual " + list.get(1), list.get(1).endsWith(line2));
            assertTrue("Expected line 3 to end with: " + line3 + " Actual " + list.get(2), list.get(2).endsWith(line3));
            assertTrue("Expected line 4 to end with: " + line4 + " Actual " + list.get(3), list.get(3).endsWith(line4));

            appender.clear();

            ThreadContext.remove("loginId");

            root.debug("This is a test");

            list = appender.getMessages();
            assertTrue("No messages expected, found " + list.size(), list.size() == 0);
        } finally {
            root.removeAppender(appender);
            ThreadContext.clear();

            appender.stop();
        }

    }

    /**
     * Test case for MDC conversion pattern.
     */
    @Test
    public void testException() throws Exception {
        for (final Appender appender : root.getAppenders().values()) {
            root.removeAppender(appender);
        }
        // set up appender
        final AbstractStringLayout layout = RFC5424Layout.createLayout("Local0", "Event", "3692", "true", "RequestContext",
            "true", null, "ATM", null, "key1, key2, locale", null, "loginId", null, "%xEx", null);
        final ListAppender appender = new ListAppender("List", null, layout, true, false);
        appender.start();

        // set appender on root and set level to debug
        root.addAppender(appender);
        root.setLevel(Level.DEBUG);

        ThreadContext.put("loginId", "JohnDoe");

        // output starting message
        root.debug("starting mdc pattern test", new IllegalArgumentException("Test"));

        try {

            final List<String> list = appender.getMessages();

            assertTrue("Not enough list entries", list.size() > 1);
            assertTrue("No Exception", list.get(1).contains("IllegalArgumentException"));

            appender.clear();
        } finally {
            root.removeAppender(appender);
            ThreadContext.clear();

            appender.stop();
        }
    }

}
