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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.BasicConfigurationFactory;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.jackson.Log4jXmlObjectMapper;
import org.apache.logging.log4j.core.util.Charsets;
import org.apache.logging.log4j.core.util.Throwables;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Tests {@link XMLLayout}.
 */
public class XMLLayoutTest {
    private static final String body = "<Message>empty mdc</Message>";
    static ConfigurationFactory cf = new BasicConfigurationFactory();
    private static final String markerTag = "<Marker name=\"EVENT\"/>";

    @AfterClass
    public static void cleanupClass() {
        ConfigurationFactory.removeConfigurationFactory(cf);
        ThreadContext.clearAll();
    }

    @BeforeClass
    public static void setupClass() {
        ThreadContext.clearAll();
        ConfigurationFactory.setConfigurationFactory(cf);
        final LoggerContext ctx = (LoggerContext) LogManager.getContext();
        ctx.reconfigure();
    }

    LoggerContext ctx = (LoggerContext) LogManager.getContext();

    Logger rootLogger = this.ctx.getLogger("");

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

    /**
     * @param includeSource TODO
     * @param compact
     * @param includeContext TODO
     * @throws IOException
     * @throws JsonParseException
     * @throws JsonMappingException
     */
    private void testAllFeatures(final boolean includeSource, final boolean compact, final boolean includeContext) throws IOException,
            JsonParseException, JsonMappingException {
        final Log4jLogEvent expected = LogEventFixtures.createLogEvent();
        final XMLLayout layout = XMLLayout.createLayout(Boolean.toString(includeSource), Boolean.toString(includeContext), "false",
                Boolean.toString(compact), "UTF-8");
        final String str = layout.toSerializable(expected);
        // System.out.println(str);
        assertEquals(str, !compact, str.contains("\n"));
        assertEquals(str, includeSource, str.contains("Source"));
        assertEquals(str, includeContext, str.contains("ContextMap"));
        final Log4jLogEvent actual = new Log4jXmlObjectMapper().readValue(str, Log4jLogEvent.class);
        LogEventFixtures.assertEqualLogEvents(expected, actual, includeSource, includeContext);
        if (includeContext) {
            this.checkElement("MDC.A", "A_Value", compact, str);
            this.checkElement("MDC.B", "B_Value", compact, str);
        }

        //
        assertNull(actual.getThrown());
        // check some attrs
        assertTrue(str, str.contains("loggerFQCN=\"f.q.c.n\""));
        assertTrue(str, str.contains("loggerName=\"a.B\""));
        // make sure short names are used
        assertTrue(str, str.contains("<Event "));
        assertTrue(str, str.contains("class="));
        assertTrue(str, str.contains("method="));
        assertTrue(str, str.contains("file="));
        assertTrue(str, str.contains("line="));
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
        this.checkElementName("Cause", compact, str, true, true);
        this.checkAttributeName("class", compact, str);
        this.checkAttributeName("method", compact, str);
        this.checkAttributeName("file", compact, str);
        this.checkAttributeName("line", compact, str);
        this.checkAttributeName("exact", compact, str);
        this.checkAttributeName("location", compact, str);
        this.checkAttributeName("version", compact, str);
        this.checkAttributeName("commonElementCount", compact, str);
        this.checkAttributeName("message", compact, str);
        this.checkAttributeName("localizedMessage", compact, str);
        this.checkElementName("ExtendedStackTrace", compact, str, false, true);
        if (Throwables.isSuppressedAvailable()) {
            this.checkElementName("Suppressed", compact, str, false, true);
        }
        this.checkAttributeName("loggerFQCN", compact, str);
        this.checkAttributeName("endOfBatch", compact, str);
        if (includeContext) {
            this.checkElementName("ContextMap", compact, str, false, true);
        }
        this.checkElementName("ContextStack", compact, str, false, true);
        if (includeSource) {
            this.checkElementName("Source", compact, str, true, false);
        }
        // check some attrs
        this.checkAttribute("loggerFQCN", "f.q.c.n", compact, str);
        this.checkAttribute("loggerName", "a.B", compact, str);
    }

    @Test
    public void testContentType() {
        final XMLLayout layout = XMLLayout.createLayout(null, null, null, null, null);
        assertEquals("text/xml; charset=UTF-8", layout.getContentType());
    }

    @Test
    public void testDefaultCharset() {
        final XMLLayout layout = XMLLayout.createLayout(null, null, null, null, null);
        assertEquals(Charsets.UTF_8, layout.getCharset());
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
        final XMLLayout layout = XMLLayout.createLayout("true", "true", "true", null, null);
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
        this.checkContains("loggerFQCN=\"org.apache.logging.log4j.spi.AbstractLoggerProvider\"", list);
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
        final XMLLayout layout = XMLLayout.createLayout("false", "true", "true", null, null);
        final Log4jLogEvent event = Log4jLogEvent.createEvent("a.B", null, "f.q.c.n", Level.DEBUG, new SimpleMessage("M"), null, null,
                null, "threadName", null, 1);
        final String str = layout.toSerializable(event);
        assertTrue(str, str.contains("loggerName=\"a.B\""));
    }

    @Test
    public void testLocationOffCompactOffMdcOff() throws Exception {
        this.testAllFeatures(false, false, false);
    }

    @Test
    public void testLocationOnCompactOnMdcOn() throws Exception {
        this.testAllFeatures(true, true, true);
    }
}
