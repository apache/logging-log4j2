/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.jackson.Log4jXmlObjectMapper;
import org.apache.logging.log4j.core.lookup.JavaLookup;
import org.apache.logging.log4j.core.test.BasicConfigurationFactory;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.test.junit.UsingAnyThreadContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link XmlLayout}.
 */
@UsingAnyThreadContext
@Tag("Layouts.Xml")
public class XmlLayoutTest {
    private static final String body = "<Message>empty mdc</Message>";
    static ConfigurationFactory cf = new BasicConfigurationFactory();
    private static final String markerTag = "<Marker name=\"EVENT\"/>";

    @AfterAll
    public static void cleanupClass() {
        ConfigurationFactory.removeConfigurationFactory(cf);
    }

    @BeforeAll
    public static void setupClass() {
        ConfigurationFactory.setConfigurationFactory(cf);
        final LoggerContext ctx = LoggerContext.getContext();
        ctx.reconfigure();
    }

    LoggerContext ctx = LoggerContext.getContext();

    Logger rootLogger = this.ctx.getRootLogger();

    private void checkAttribute(final String name, final String value, final boolean compact, final String str) {
        assertTrue(str.contains(name + "=\"" + value + "\""), str);
    }

    private void checkAttributeName(final String name, final boolean compact, final String str) {
        assertTrue(str.contains(name + "=\""), str);
    }

    private void checkContains(final String expected, final List<String> list) {
        for (final String string : list) {
            final String trimedLine = string.trim();
            if (trimedLine.contains(expected)) {
                return;
            }
        }
        fail("Cannot find " + expected + " in " + list);
    }

    private void checkElement(final String key, final String value, final boolean compact, final String str) {
        // <item key="MDC.A" value="A_Value"/>
        assertTrue(str.contains(String.format("<item key=\"%s\" value=\"%s\"/>", key, value)), str);
    }

    private void checkElementName(
            final String name,
            final boolean compact,
            final String str,
            final boolean withAttributes,
            final boolean withChildren) {
        // simple checks, don't try to be too smart here, we're just looking for the names and basic shape.
        // start
        final String startStr = withAttributes ? "<" + name + " " : "<" + name + ">";
        final int startPos = str.indexOf(startStr);
        assertTrue(startPos >= 0, str);
        // end
        final String endStr = withChildren ? "</" + name + ">" : "/>";
        final int endPos = str.indexOf(endStr, startPos + startStr.length());
        assertTrue(endPos >= 0, str);
    }

    private void checkElementNameAbsent(final String name, final boolean compact, final String str) {
        assertFalse(str.contains("<" + name));
    }

    /**
     * @param includeSource TODO
     * @param compact
     * @param includeContext TODO
     * @throws IOException
     * @throws JsonParseException
     * @throws JsonMappingException
     */
    private void testAllFeatures(
            final boolean includeSource,
            final boolean compact,
            final boolean includeContext,
            final boolean includeStacktrace)
            throws IOException, JsonParseException, JsonMappingException {
        final Log4jLogEvent expected = LogEventFixtures.createLogEvent();
        final XmlLayout layout = XmlLayout.newBuilder()
                .setLocationInfo(includeSource)
                .setProperties(includeContext)
                .setComplete(false)
                .setCompact(compact)
                .setIncludeStacktrace(includeStacktrace)
                .setCharset(StandardCharsets.UTF_8)
                .build();
        final String str = layout.toSerializable(expected);
        // System.out.println(str);
        assertEquals(!compact, str.contains("\n"), str);
        assertEquals(includeSource, str.contains("<Source"), str);
        assertEquals(includeContext, str.contains("<ContextMap"), str);
        final Log4jLogEvent actual = new Log4jXmlObjectMapper().readValue(str, Log4jLogEvent.class);
        LogEventFixtures.assertEqualLogEvents(expected, actual, includeSource, includeContext, includeStacktrace);
        if (includeContext) {
            this.checkElement("MDC.A", "A_Value", compact, str);
            this.checkElement("MDC.B", "B_Value", compact, str);
        }

        //
        assertNull(actual.getThrown());
        // check some attrs
        assertTrue(str.contains("loggerFqcn=\"f.q.c.n\""), str);
        assertTrue(str.contains("loggerName=\"a.B\""), str);
        // make sure short names are used
        assertTrue(str.contains("<Event "), str);
        if (includeStacktrace) {
            assertTrue(str.contains("class="), str);
            assertTrue(str.contains("method="), str);
            assertTrue(str.contains("file="), str);
            assertTrue(str.contains("line="), str);
        }
        //
        // make sure the names we want are used
        // this.checkAttributeName("timeMillis", compact, str);
        this.checkElementName("Instant", compact, str, true, false);
        this.checkAttributeName("epochSecond", compact, str);
        this.checkAttributeName("nanoOfSecond", compact, str);
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
    public void testLayout() {
        final Map<String, Appender> appenders = this.rootLogger.getAppenders();
        for (final Appender appender : appenders.values()) {
            this.rootLogger.removeAppender(appender);
        }
        // set up appender
        final XmlLayout layout = XmlLayout.newBuilder()
                .setLocationInfo(true)
                .setProperties(true)
                .setComplete(true)
                .setCompact(false)
                .setIncludeStacktrace(true)
                .build();

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
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", string, "Incorrect header: " + string);
        assertEquals("</Events>", list.get(list.size() - 1), "Incorrect footer");
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
        final XmlLayout layout = XmlLayout.newBuilder()
                .setLocationInfo(false)
                .setProperties(true)
                .setComplete(true)
                .setCompact(false)
                .setIncludeStacktrace(true)
                .build();

        final Log4jLogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("a.B") //
                .setLoggerFqcn("f.q.c.n") //
                .setLevel(Level.DEBUG) //
                .setMessage(new SimpleMessage("M")) //
                .setThreadName("threadName") //
                .setTimeMillis(1)
                .build();
        final String str = layout.toSerializable(event);
        assertTrue(str.contains("loggerName=\"a.B\""), str);
    }

    @Test
    public void testAdditionalFields() {
        final AbstractJacksonLayout layout = XmlLayout.newBuilder()
                .setLocationInfo(false)
                .setProperties(false)
                .setIncludeStacktrace(false)
                .setAdditionalFields(new KeyValuePair[] {
                    new KeyValuePair("KEY1", "VALUE1"), new KeyValuePair("KEY2", "${java:runtime}"),
                })
                .setCharset(StandardCharsets.UTF_8)
                .setConfiguration(ctx.getConfiguration())
                .build();
        final String str = layout.toSerializable(LogEventFixtures.createLogEvent());
        assertTrue(str.contains("<KEY1>VALUE1</KEY1>"), str);
        assertTrue(str.contains("<KEY2>" + new JavaLookup().getRuntime() + "</KEY2>"), str);
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

    @Test
    public void testStacktraceAsString() {
        final String str = prepareXMLForStacktraceTests(true);
        assertTrue(str.contains("<ExtendedStackTrace>java.lang.NullPointerException"), str);
    }

    @Test
    public void testStacktraceAsNonString() {
        final String str = prepareXMLForStacktraceTests(false);
        assertTrue(str.contains("<ExtendedStackTrace><ExtendedStackTraceItem"), str);
    }

    private String prepareXMLForStacktraceTests(final boolean stacktraceAsString) {
        final Log4jLogEvent expected = LogEventFixtures.createLogEvent();
        // @formatter:off
        final AbstractJacksonLayout layout = XmlLayout.newBuilder()
                .setCompact(true)
                .setIncludeStacktrace(true)
                .setStacktraceAsString(stacktraceAsString)
                .build();
        // @formatter:off
        return layout.toSerializable(expected);
    }

    @Test
    public void testIncludeNullDelimiterTrue() {
        final AbstractJacksonLayout layout = XmlLayout.newBuilder()
                .setCompact(true)
                .setIncludeNullDelimiter(true)
                .build();
        final String str = layout.toSerializable(LogEventFixtures.createLogEvent());
        assertTrue(str.endsWith("\0"));
    }

    @Test
    public void testIncludeNullDelimiterFalse() {
        final AbstractJacksonLayout layout = XmlLayout.newBuilder()
                .setCompact(true)
                .setIncludeNullDelimiter(false)
                .build();
        final String str = layout.toSerializable(LogEventFixtures.createLogEvent());
        assertFalse(str.endsWith("\0"));
    }
}
