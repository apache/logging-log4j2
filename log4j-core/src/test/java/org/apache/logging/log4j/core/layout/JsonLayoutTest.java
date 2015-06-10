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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.BasicConfigurationFactory;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.jackson.Log4jJsonObjectMapper;
import org.apache.logging.log4j.core.util.Throwables;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.apache.logging.log4j.util.Strings;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the JsonLayout class.
 */
public class JsonLayoutTest {
    static ConfigurationFactory cf = new BasicConfigurationFactory();

    private static final String DQUOTE = "\"";

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

    private void checkAt(final String expected, final int lineIndex, final List<String> list) {
        final String trimedLine = list.get(lineIndex).trim();
        assertTrue("Incorrect line index " + lineIndex + ": " + Strings.dquote(trimedLine), trimedLine.equals(expected));
    }

    private void checkContains(final String expected, final List<String> list) {
        for (final String string : list) {
            final String trimedLine = string.trim();
            if (trimedLine.equals(expected)) {
                return;
            }
        }
        Assert.fail("Cannot find " + expected + " in " + list);
    }

    private void checkMapEntry(final String key, final String value, final boolean compact, final String str) {
        final String propSep = this.toPropertySeparator(compact);
        // "name":"value"
        final String expected = String.format("{\"key\":\"%s\",\"value\":\"%s\"}", key, value);
        assertTrue("Cannot find " + expected + " in " + str, str.contains(expected));
    }

    private void checkProperty(final String key, final String value, final boolean compact, final String str) {
        final String propSep = this.toPropertySeparator(compact);
        // {"key":"MDC.B","value":"B_Value"}
        final String expected = String.format("\"%s\"%s\"%s\"", key, propSep, value);
        assertTrue("Cannot find " + expected + " in " + str, str.contains(expected));
    }

    private void checkPropertyName(final String name, final boolean compact, final String str) {
        final String propSep = this.toPropertySeparator(compact);
        assertTrue(str, str.contains(DQUOTE + name + DQUOTE + propSep));
    }

    private void testAllFeatures(final boolean includeSource, final boolean compact, final boolean eventEol, final boolean includeContext)
            throws Exception {
        final Log4jLogEvent expected = LogEventFixtures.createLogEvent();
        final AbstractJacksonLayout layout = JsonLayout.createLayout(includeSource,
                includeContext, false, compact, eventEol, StandardCharsets.UTF_8);
        final String str = layout.toSerializable(expected);
        // System.out.println(str);
        final String propSep = this.toPropertySeparator(compact);
        // Just check for \n since \r might or might not be there.
        assertEquals(str, !compact || eventEol, str.contains("\n"));
        assertEquals(str, includeSource, str.contains("source"));
        assertEquals(str, includeContext, str.contains("contextMap"));
        final Log4jLogEvent actual = new Log4jJsonObjectMapper().readValue(str, Log4jLogEvent.class);
        LogEventFixtures.assertEqualLogEvents(expected, actual, includeSource, includeContext);
        if (includeContext) {
            this.checkMapEntry("MDC.A", "A_Value", compact, str);
            this.checkMapEntry("MDC.B", "B_Value", compact, str);
        }
        //
        assertNull(actual.getThrown());
        // make sure the names we want are used
        this.checkPropertyName("timeMillis", compact, str);
        this.checkPropertyName("thread", compact, str); // and not threadName
        this.checkPropertyName("level", compact, str);
        this.checkPropertyName("loggerName", compact, str);
        this.checkPropertyName("marker", compact, str);
        this.checkPropertyName("name", compact, str);
        this.checkPropertyName("parents", compact, str);
        this.checkPropertyName("message", compact, str);
        this.checkPropertyName("thrown", compact, str);
        this.checkPropertyName("cause", compact, str);
        this.checkPropertyName("class", compact, str);
        this.checkPropertyName("method", compact, str);
        this.checkPropertyName("file", compact, str);
        this.checkPropertyName("line", compact, str);
        this.checkPropertyName("exact", compact, str);
        this.checkPropertyName("location", compact, str);
        this.checkPropertyName("version", compact, str);
        this.checkPropertyName("commonElementCount", compact, str);
        this.checkPropertyName("localizedMessage", compact, str);
        this.checkPropertyName("extendedStackTrace", compact, str);
        if (Throwables.isGetSuppressedAvailable()) {
            this.checkPropertyName("suppressed", compact, str);
        }
        this.checkPropertyName("loggerFqcn", compact, str);
        this.checkPropertyName("endOfBatch", compact, str);
        if (includeContext) {
            this.checkPropertyName("contextMap", compact, str);
        }
        this.checkPropertyName("contextStack", compact, str);
        if (includeSource) {
            this.checkPropertyName("source", compact, str);
        }
        // check some attrs
        this.checkProperty("loggerFqcn", "f.q.c.n", compact, str);
        this.checkProperty("loggerName", "a.B", compact, str);
    }

    @Test
    public void testContentType() {
        final AbstractJacksonLayout layout = JsonLayout.createDefaultLayout();
        assertEquals("application/json; charset=UTF-8", layout.getContentType());
    }

    @Test
    public void testDefaultCharset() {
        final AbstractJacksonLayout layout = JsonLayout.createDefaultLayout();
        assertEquals(StandardCharsets.UTF_8, layout.getCharset());
    }

    @Test
    public void testEscapeLayout() throws Exception {
        final Map<String, Appender> appenders = this.rootLogger.getAppenders();
        for (final Appender appender : appenders.values()) {
            this.rootLogger.removeAppender(appender);
        }
        // set up appender
        final AbstractJacksonLayout layout = JsonLayout.createLayout(true, true, true, false, false, null);
        final ListAppender appender = new ListAppender("List", null, layout, true, false);
        appender.start();

        // set appender on root and set level to debug
        this.rootLogger.addAppender(appender);
        this.rootLogger.setLevel(Level.DEBUG);

        // output starting message
        this.rootLogger.debug("Here is a quote ' and then a double quote \"");

        appender.stop();

        final List<String> list = appender.getMessages();

        this.checkAt("[", 0, list);
        this.checkAt("{", 1, list);
        this.checkContains("\"level\" : \"DEBUG\",", list);
        this.checkContains("\"message\" : \"Here is a quote ' and then a double quote \\\"\",", list);
        this.checkContains("\"loggerFqcn\" : \"" + AbstractLogger.class.getName() + "\",", list);
        for (final Appender app : appenders.values()) {
            this.rootLogger.addAppender(app);
        }
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
        final AbstractJacksonLayout layout = JsonLayout.createLayout(true, true, true, false, false, null);
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

        appender.stop();

        final List<String> list = appender.getMessages();

        this.checkAt("[", 0, list);
        this.checkAt("{", 1, list);
        this.checkContains("\"loggerFqcn\" : \"" + AbstractLogger.class.getName() + "\",", list);
        this.checkContains("\"level\" : \"DEBUG\",", list);
        this.checkContains("\"message\" : \"starting mdc pattern test\",", list);
        for (final Appender app : appenders.values()) {
            this.rootLogger.addAppender(app);
        }
    }

    @Test
    public void testLayoutLoggerName() throws Exception {
        final AbstractJacksonLayout layout = JsonLayout.createLayout(false, false, false, true, false, StandardCharsets.UTF_8);
        final Log4jLogEvent expected = Log4jLogEvent.createEvent("a.B", null, "f.q.c.n", Level.DEBUG, 
                new SimpleMessage("M"), null, null, null, null, "threadName", null, 1);
        final String str = layout.toSerializable(expected);
        assertTrue(str, str.contains("\"loggerName\":\"a.B\""));
        final Log4jLogEvent actual = new Log4jJsonObjectMapper().readValue(str, Log4jLogEvent.class);
        assertEquals(expected.getLoggerName(), actual.getLoggerName());
        assertEquals(expected, actual);
    }

    @Test
    public void testLocationOffCompactOffMdcOff() throws Exception {
        this.testAllFeatures(false, false, false, false);
    }

    @Test
    public void testLocationOnCompactOnMdcOn() throws Exception {
        this.testAllFeatures(true, true, false, true);
    }

    @Test
    public void testLocationOnCompactOnEventEolOnMdcOn() throws Exception {
        this.testAllFeatures(true, true, true, true);
    }

    private String toPropertySeparator(final boolean compact) {
        return compact ? ":" : " : ";
    }
}
