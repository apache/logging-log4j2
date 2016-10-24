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

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.BasicConfigurationFactory;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.net.Facility;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.junit.ThreadContextRule;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.apache.logging.log4j.util.Strings;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

public class Rfc5424LayoutTest {
    LoggerContext ctx = LoggerContext.getContext();
    Logger root = ctx.getRootLogger();


    private static final String line1 = "ATM - - [RequestContext@3692 loginId=\"JohnDoe\"] starting mdc pattern test";
    private static final String line2 = "ATM - - [RequestContext@3692 loginId=\"JohnDoe\"] empty mdc";
    private static final String line3 = "ATM - - [RequestContext@3692 loginId=\"JohnDoe\"] filled mdc";
    private static final String line4 =
        "ATM - Audit [Transfer@18060 Amount=\"200.00\" FromAccount=\"123457\" ToAccount=\"123456\"]" +
        "[RequestContext@3692 ipAddress=\"192.168.0.120\" loginId=\"JohnDoe\"] Transfer Complete";
    private static final String lineEscaped3 = "ATM - - [RequestContext@3692 escaped=\"Testing escaping #012 \\\" \\] \\\"\" loginId=\"JohnDoe\"] filled mdc";
    private static final String lineEscaped4 =
        "ATM - Audit [Transfer@18060 Amount=\"200.00\" FromAccount=\"123457\" ToAccount=\"123456\"]" +
        "[RequestContext@3692 escaped=\"Testing escaping #012 \\\" \\] \\\"\" ipAddress=\"192.168.0.120\" loginId=\"JohnDoe\"] Transfer Complete";

    static ConfigurationFactory cf = new BasicConfigurationFactory();

    @Rule
    public final ThreadContextRule threadContextRule = new ThreadContextRule(); 

    @BeforeClass
    public static void setupClass() {
        StatusLogger.getLogger().setLevel(Level.OFF);
        ConfigurationFactory.setConfigurationFactory(cf);
        final LoggerContext ctx = LoggerContext.getContext();
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
        final AbstractStringLayout layout = Rfc5424Layout.createLayout(Facility.LOCAL0, "Event", 3692, true, "RequestContext",
            null, null, true, null, "ATM", null, "key1, key2, locale", null, "loginId", null, true, null, null);
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

            for (final String frame : list) {
                int length = -1;
                final int frameLength = frame.length();
                final int firstSpacePosition = frame.indexOf(' ');
                final String messageLength = frame.substring(0, firstSpacePosition);
                try {
                    length = Integer.parseInt(messageLength);
                    // the ListAppender removes the ending newline, so we expect one less size
                    assertEquals(frameLength, messageLength.length() + length);
                }
                catch (final NumberFormatException e) {
                    assertTrue("Not a valid RFC 5425 frame", false);
                }
            }

            appender.clear();

            ThreadContext.remove("loginId");

            root.debug("This is a test");

            list = appender.getMessages();
            assertTrue("No messages expected, found " + list.size(), list.isEmpty());
        } finally {
            root.removeAppender(appender);
            appender.stop();
        }
    }

    /**
     * Test case for escaping newlines and other SD PARAM-NAME special characters.
     */
    @Test
    public void testEscape() throws Exception {
        for (final Appender appender : root.getAppenders().values()) {
            root.removeAppender(appender);
        }
        // set up layout/appender
        final AbstractStringLayout layout = Rfc5424Layout.createLayout(Facility.LOCAL0, "Event", 3692, true, "RequestContext",
            null, null, true, "#012", "ATM", null, "key1, key2, locale", null, "loginId", null, true, null, null);
        final ListAppender appender = new ListAppender("List", null, layout, true, false);

        appender.start();

        // set appender on root and set level to debug
        root.addAppender(appender);
        root.setLevel(Level.DEBUG);

        ThreadContext.put("loginId", "JohnDoe");

        // output starting message
        root.debug("starting mdc pattern test");

        root.debug("empty mdc");

        ThreadContext.put("escaped", "Testing escaping \n \" ] \"");

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
            assertTrue("Expected line 3 to end with: " + lineEscaped3 + " Actual " + list.get(2), list.get(2).endsWith(lineEscaped3));
            assertTrue("Expected line 4 to end with: " + lineEscaped4 + " Actual " + list.get(3), list.get(3).endsWith(lineEscaped4));

            appender.clear();

            ThreadContext.remove("loginId");

            root.debug("This is a test");

            list = appender.getMessages();
            assertTrue("No messages expected, found " + list.size(), list.isEmpty());
        } finally {
            root.removeAppender(appender);
            appender.stop();
        }
    }

    /**
     * Test case for MDC exception conversion pattern.
     */
    @Test
    public void testException() throws Exception {
        for (final Appender appender : root.getAppenders().values()) {
            root.removeAppender(appender);
        }
        // set up layout/appender
        final AbstractStringLayout layout = Rfc5424Layout.createLayout(Facility.LOCAL0, "Event", 3692, true, "RequestContext",
            null, null, true, null, "ATM", null, "key1, key2, locale", null, "loginId", "%xEx", true, null, null);
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
            final String string = list.get(1);
			      assertTrue("No Exception in " + string, string.contains("IllegalArgumentException"));

            appender.clear();
        } finally {
            root.removeAppender(appender);
            appender.stop();
        }
    }

    /**
     * Test case for MDC logger field inclusion.
     */
    @Test
    public void testMDCLoggerFields() throws Exception {
        for (final Appender appender : root.getAppenders().values()) {
            root.removeAppender(appender);
        }

        final LoggerFields[] loggerFields = new LoggerFields[] {
                LoggerFields.createLoggerFields(new KeyValuePair[] { new KeyValuePair("source", "%C.%M")}, null, null, false),
                LoggerFields.createLoggerFields(new KeyValuePair[] { new KeyValuePair("source2", "%C.%M")}, null, null, false)
        };

        // set up layout/appender
        final AbstractStringLayout layout = Rfc5424Layout.createLayout(Facility.LOCAL0, "Event", 3692, true, "RequestContext",
            null, null, true, null, "ATM", null, "key1, key2, locale", null, null, null, true, loggerFields, null);
        final ListAppender appender = new ListAppender("List", null, layout, true, false);
        appender.start();

        // set appender on root and set level to debug
        root.addAppender(appender);
        root.setLevel(Level.DEBUG);

        // output starting message
        root.info("starting logger fields test");

        try {

            final List<String> list = appender.getMessages();
            assertTrue("Not enough list entries", list.size() > 0);
            assertTrue("No class/method", list.get(0).contains("Rfc5424LayoutTest.testMDCLoggerFields"));

            appender.clear();
        } finally {
            root.removeAppender(appender);
            appender.stop();
        }
    }

    @Test
    public void testLoggerFields() {
        final String[] fields = new String[] {
                "[BAZ@32473 baz=\"org.apache.logging.log4j.core.layout.Rfc5424LayoutTest.testLoggerFields\"]",
                "[RequestContext@3692 bar=\"org.apache.logging.log4j.core.layout.Rfc5424LayoutTest.testLoggerFields\"]",
                "[SD-ID@32473 source=\"org.apache.logging.log4j.core.layout.Rfc5424LayoutTest.testLoggerFields\"]"
        };
        final List<String> expectedToContain = Arrays.asList(fields);

        for (final Appender appender : root.getAppenders().values()) {
            root.removeAppender(appender);
        }

        final LoggerFields[] loggerFields = new LoggerFields[] {
                LoggerFields.createLoggerFields(new KeyValuePair[] { new KeyValuePair("source", "%C.%M")}, "SD-ID",
                        "32473", false),
                LoggerFields.createLoggerFields(new KeyValuePair[] { new KeyValuePair("baz", "%C.%M"),
                        new KeyValuePair("baz", "%C.%M") }, "BAZ", "32473", false),
                LoggerFields.createLoggerFields(new KeyValuePair[] { new KeyValuePair("bar", "%C.%M")}, null, null, false)
        };

        final AbstractStringLayout layout = Rfc5424Layout.createLayout(Facility.LOCAL0, "Event", 3692, true, "RequestContext",
                null, null, true, null, "ATM", null, "key1, key2, locale", null, null, null, false, loggerFields, null);
        final ListAppender appender = new ListAppender("List", null, layout, true, false);
        appender.start();

        root.addAppender(appender);
        root.setLevel(Level.DEBUG);

        root.info("starting logger fields test");

        try {

            final List<String> list = appender.getMessages();
            assertTrue("Not enough list entries", list.size() > 0);
            final String message =  list.get(0);
            assertTrue("No class/method", message.contains("Rfc5424LayoutTest.testLoggerFields"));
            for (final String value : expectedToContain) {
                Assert.assertTrue("Message expected to contain " + value + " but did not", message.contains(value));
            }
            appender.clear();
        } finally {
            root.removeAppender(appender);
            appender.stop();
        }
    }

    @Test
    public void testDiscardEmptyLoggerFields() {
        final String mdcId = "RequestContext";

        Arrays.asList(
                "[BAZ@32473 baz=\"org.apache.logging.log4j.core.layout.Rfc5424LayoutTest.testLoggerFields\"]"  +
                        "[RequestContext@3692 bar=\"org.apache.logging.log4j.core.layout.Rfc5424LayoutTest.testLoggerFields\"]"
        );

        for (final Appender appender : root.getAppenders().values()) {
            root.removeAppender(appender);
        }

        final LoggerFields[] loggerFields = new LoggerFields[] {
                LoggerFields.createLoggerFields(new KeyValuePair[] { new KeyValuePair("dummy", Strings.EMPTY),
                        new KeyValuePair("empty", Strings.EMPTY)}, "SD-ID", "32473", true),
                LoggerFields.createLoggerFields(new KeyValuePair[] { new KeyValuePair("baz", "%C.%M"),
                        new KeyValuePair("baz", "%C.%M") }, "BAZ", "32473", false),
                LoggerFields.createLoggerFields(new KeyValuePair[] { new KeyValuePair("bar", "%C.%M")}, null, null, false)
        };

        final AbstractStringLayout layout = Rfc5424Layout.createLayout(Facility.LOCAL0, "Event", 3692, true, mdcId,
                null, null, true, null, "ATM", null, "key1, key2, locale", null, null, null, false, loggerFields, null);
        final ListAppender appender = new ListAppender("List", null, layout, true, false);
        appender.start();

        root.addAppender(appender);
        root.setLevel(Level.DEBUG);

        root.info("starting logger fields test");

        try {

            final List<String> list = appender.getMessages();
            assertTrue("Not enough list entries", list.size() > 0);
            final String message =  list.get(0);
            Assert.assertTrue("SD-ID should have been discarded", !message.contains("SD-ID"));
            Assert.assertTrue("BAZ should have been included", message.contains("BAZ"));
            Assert.assertTrue(mdcId + "should have been included", message.contains(mdcId));
            appender.clear();
        } finally {
            root.removeAppender(appender);
            appender.stop();
        }
    }

    @Test
    public void testSubstituteStructuredData() {
        final String mdcId = "RequestContext";

        final String expectedToContain = "ATM - MSG-ID - Message";

        for (final Appender appender : root.getAppenders().values()) {
            root.removeAppender(appender);
        }

        final AbstractStringLayout layout = Rfc5424Layout.createLayout(Facility.LOCAL0, "Event", 3692, false, mdcId,
                null, null, true, null, "ATM", "MSG-ID", "key1, key2, locale", null, null, null, false, null, null);
        final ListAppender appender = new ListAppender("List", null, layout, true, false);
        appender.start();

        root.addAppender(appender);
        root.setLevel(Level.DEBUG);

        root.info("Message");

        try {
            final List<String> list = appender.getMessages();
            assertTrue("Not enough list entries", list.size() > 0);
            final String message =  list.get(0);
            Assert.assertTrue("Not the expected message received", message.contains(expectedToContain));
            appender.clear();
        } finally {
            root.removeAppender(appender);
            appender.stop();
        }
    }

    @Test
    public void testParameterizedMessage() {
        for (final Appender appender : root.getAppenders().values()) {
            root.removeAppender(appender);
        }
        // set up appender
        final AbstractStringLayout layout = Rfc5424Layout.createLayout(Facility.LOCAL0, "Event", 3692, true, "RequestContext",
            null, null, true, null, "ATM", null, "key1, key2, locale", null, null, null, true, null, null);
        final ListAppender appender = new ListAppender("List", null, layout, true, false);

        appender.start();

        // set appender on root and set level to debug
        root.addAppender(appender);
        root.setLevel(Level.DEBUG);
        root.info("Hello {}", "World");
        try {
            final List<String> list = appender.getMessages();
            assertTrue("Not enough list entries", list.size() > 0);
            final String message =  list.get(0);
            assertTrue("Incorrect message. Expected - Hello World, Actual - " + message, message.contains("Hello World"));
        } finally {
            root.removeAppender(appender);
            appender.stop();
        }
    }
}
