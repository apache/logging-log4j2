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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.categories.Layouts;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.BasicConfigurationFactory;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.jackson.Log4jXmlObjectMapper;
import org.apache.logging.log4j.junit.ThreadContextRule;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.*;

/**
 * Tests {@link XmlLayout}.
 */
@Category(Layouts.Xml.class)
public class XmlLayoutTest {
    private static final String body = "<Message>empty mdc</Message>";
    static ConfigurationFactory cf = new BasicConfigurationFactory();
    private static final String markerTag = "<Marker name=\"EVENT\"/>";

    @Rule
    public final ThreadContextRule threadContextRule = new ThreadContextRule(); 

    @AfterClass
    public static void cleanupClass() {
        ConfigurationFactory.removeConfigurationFactory(cf);
    }

    @BeforeClass
    public static void setupClass() {
        ConfigurationFactory.setConfigurationFactory(cf);
        final LoggerContext ctx = LoggerContext.getContext();
        ctx.reconfigure();
    }

    LoggerContext ctx = LoggerContext.getContext();

    Logger rootLogger = this.ctx.getRootLogger();

    private void checkAttribute(final String name, final String value, final boolean compact, final String str) {
        Assert.assertTrue(str, str.contains(name + "=\"" + value + "\""));
    }

    private void checkAttributeName(final String name, final boolean compact, final String str) {
        Assert.assertTrue(str, str.contains(name + "=\""));
    }

    private void checkContains(final String expected, final List<String> list) {
        for (final String string : list) {
            final String trimedLine = string.trim();
            if (trimedLine.contains(expected)) {
                return;
            }
        }
        Assert.fail("Cannot find " + expected + " in " + list);
    }

    private void checkElement(final String key, final String value, final boolean compact, final String str) {
        // <item key="MDC.A" value="A_Value"/>
        assertTrue(str, str.contains(String.format("<item key=\"%s\" value=\"%s\"/>", key, value)));
    }

    private void checkElementName(final String name, final boolean compact, final String str, final boolean withAttributes,
            final boolean withChildren) {
        // simple checks, don't try to be too smart here, we're just looking for the names and basic shape.
        // start
        final String startStr = withAttributes ? "<" + name + " " : "<" + name + ">";
        final int startPos = str.indexOf(startStr);
        Assert.assertTrue(str, startPos >= 0);
        // end
        final String endStr = withChildren ? "</" + name + ">" : "/>";
        final int endPos = str.indexOf(endStr, startPos + startStr.length());
        Assert.assertTrue(str, endPos >= 0);
    }

    private void checkElementNameAbsent(final String name, final boolean compact, final String str) {
        Assert.assertFalse(str.contains("<" + name));
    }

    /**
     * @param includeSource TODO
     * @param compact
     * @param includeContext TODO
     * @throws IOException
     * @throws JsonParseException
     * @throws JsonMappingException
     */
    private void testAllFeatures(final boolean includeSource, final boolean compact, final boolean includeContext, final boolean includeStacktrace) throws IOException,
            JsonParseException, JsonMappingException {
        final Log4jLogEvent expected = LogEventFixtures.createLogEvent();
        final XmlLayout layout = XmlLayout.createLayout(includeSource, includeContext, false, compact, StandardCharsets.UTF_8, includeStacktrace);
        final String str = layout.toSerializable(expected);
        // System.out.println(str);
        assertEquals(str, !compact, str.contains("\n"));
        assertEquals(str, includeSource, str.contains("Source"));
        assertEquals(str, includeContext, str.contains("ContextMap"));
        final Log4jLogEvent actual = new Log4jXmlObjectMapper().readValue(str, Log4jLogEvent.class);
        LogEventFixtures.assertEqualLogEvents(expected, actual, includeSource, includeContext, includeStacktrace);
        if (includeContext) {
            this.checkElement("MDC.A", "A_Value", compact, str);
            this.checkElement("MDC.B", "B_Value", compact, str);
        }

        //
        assertNull(actual.getThrown());
        // check some attrs
        assertTrue(str, str.contains("loggerFqcn=\"f.q.c.n\""));
        assertTrue(str, str.contains("loggerName=\"a.B\""));
        // make sure short names are used
        assertTrue(str, str.contains("<Event "));
        if (includeStacktrace) {
            assertTrue(str, str.contains("class="));
            assertTrue(str, str.contains("method="));
            assertTrue(str, str.contains("file="));
            assertTrue(str, str.contains("line="));
        }
        //
        // make sure the names we want are used
        this.checkAttributeName("timeMillis", compact, str);
        this.checkAttributeName("thread", compact, str); // and not threadName
        this.checkAttributeName("level", compact, str);
        this.checkAttributeName("loggerName", compact, str);
        this.checkElementName("Marker", compact, str, true, true);
        this.checkAttributeName("name", compact, str);
        this.checkElementName("Parents", compact, str, false, true);
        this.checkElementName("Message", compact, str, false, true);
        this.checkElementName("Thrown", compact, str, true, true);
        this.checkElementName("Cause", compact, str, true, includeStacktrace);
        this.checkAttributeName("commonElementCount", compact, str);
        this.checkAttributeName("message", compact, str);
        this.checkAttributeName("localizedMessage", compact, str);
        if (includeStacktrace) {
            this.checkElementName("ExtendedStackTrace", compact, str, false, true);
            this.checkAttributeName("class", compact, str);
            this.checkAttributeName("method", compact, str);
            this.checkAttributeName("file", compact, str);
            this.checkAttributeName("line", compact, str);
            this.checkAttributeName("exact", compact, str);
            this.checkAttributeName("location", compact, str);
            this.checkAttributeName("version", compact, str);
        } else {
            this.checkElementNameAbsent("ExtendedStackTrace", compact, str);
        }
        this.checkElementName("Suppressed", compact, str, false, true);
        this.checkAttributeName("loggerFqcn", compact, str);
        this.checkAttributeName("endOfBatch", compact, str);
        if (includeContext) {
            this.checkElementName("ContextMap", compact, str, false, true);
        } else {
            this.checkElementNameAbsent("ContextMap", compact, str);
        }
        this.checkElementName("ContextStack", compact, str, false, true);
        if (includeSource) {
            this.checkElementName("Source", compact, str, true, false);
        } else {
            this.checkElementNameAbsent("Source", compact, str);
        }
        // check some attrs
        this.checkAttribute("loggerFqcn", "f.q.c.n", compact, str);
        this.checkAttribute("loggerName", "a.B", compact, str);
    }

    @Test
    public void testContentType() {
        final XmlLayout layout = XmlLayout.createDefaultLayout();
        assertEquals("text/xml; charset=UTF-8", layout.getContentType());
    }

    @Test
    public void testDefaultCharset() {
        final XmlLayout layout = XmlLayout.createDefaultLayout();
        assertEquals(StandardCharsets.UTF_8, layout.getCharset());
    }

    /**
     * Test case for MDC conversion pattern.
     */
    @Test
    public void testLayout() throws Exception {
        final Map<String, Appender> appenders = this.rootLogger.getAppenders();
        for (final Appender appender : appenders.values()) {
            this.rootLogger.removeAppender(appender);
        }
        // set up appender
        final XmlLayout layout = XmlLayout.createLayout(true, true, true, false, null, true);
        final ListAppender appender = new ListAppender("List", null, layout, true, false);
        appender.start();

        // set appender on root and set level to debug
        this.rootLogger.addAppender(appender);
        this.rootLogger.setLevel(Level.DEBUG);

        // output starting message
        this.rootLogger.debug("starting mdc pattern test");

        this.rootLogger.debug("empty mdc");

        ThreadContext.put("key1", "value1");
        ThreadContext.put("key2", "value2");

        this.rootLogger.debug("filled mdc");

        ThreadContext.remove("key1");
        ThreadContext.remove("key2");

        this.rootLogger.error("finished mdc pattern test", new NullPointerException("test"));

        final Marker marker = MarkerManager.getMarker("EVENT");
        this.rootLogger.error(marker, "marker test");

        appender.stop();

        final List<String> list = appender.getMessages();

        final String string = list.get(0);
        assertTrue("Incorrect header: " + string, string.equals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertTrue("Incorrect footer", list.get(list.size() - 1).equals("</Events>"));
        this.checkContains("loggerFqcn=\"" + AbstractLogger.class.getName() + "\"", list);
        this.checkContains("level=\"DEBUG\"", list);
        this.checkContains(">starting mdc pattern test</Message>", list);
        // this.checkContains("<Message>starting mdc pattern test</Message>", list);

        // <Marker xmlns="" _class="org.apache.logging.log4j.MarkerManager..Log4jMarker" name="EVENT"/>
        this.checkContains("<Marker", list);
        this.checkContains("name=\"EVENT\"/>", list);

        for (final Appender app : appenders.values()) {
            this.rootLogger.addAppender(app);
        }
    }

    @Test
    public void testLayoutLoggerName() {
        final XmlLayout layout = XmlLayout.createLayout(false, true, true, false, null, true);
        final Log4jLogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("a.B") //
                .setLoggerFqcn("f.q.c.n") //
                .setLevel(Level.DEBUG) //
                .setMessage(new SimpleMessage("M")) //
                .setThreadName("threadName") //
                .setTimeMillis(1).build();
        final String str = layout.toSerializable(event);
        assertTrue(str, str.contains("loggerName=\"a.B\""));
    }

    @Test
    public void testLocationOffCompactOffMdcOff() throws Exception {
        this.testAllFeatures(false, false, false, true);
    }

    @Test
    public void testLocationOnCompactOnMdcOn() throws Exception {
        this.testAllFeatures(true, true, true, true);
    }

    @Test
    public void testExcludeStacktrace() throws Exception {
        this.testAllFeatures(false, false, false, false);
    }
}
