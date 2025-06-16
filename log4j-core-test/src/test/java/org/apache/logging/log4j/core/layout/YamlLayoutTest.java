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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.jackson.Log4jYamlObjectMapper;
import org.apache.logging.log4j.core.lookup.JavaLookup;
import org.apache.logging.log4j.core.test.BasicConfigurationFactory;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests the YamlLayout class.
 */
@Tag("Layouts.Yaml")
class YamlLayoutTest {
    static ConfigurationFactory cf = new BasicConfigurationFactory();

    @AfterAll
    static void cleanupClass() {
        ConfigurationFactory.removeConfigurationFactory(cf);
        ThreadContext.clearAll();
    }

    @BeforeAll
    static void setupClass() {
        ThreadContext.clearAll();
        ConfigurationFactory.setConfigurationFactory(cf);
        final LoggerContext ctx = LoggerContext.getContext();
        ctx.reconfigure();
    }

    LoggerContext ctx = LoggerContext.getContext();

    Logger rootLogger = this.ctx.getRootLogger();

    private void checkAt(final String expected, final int lineIndex, final List<String> list) {
        final String trimedLine = list.get(lineIndex).trim();
        assertEquals(expected, trimedLine, "Incorrect line index " + lineIndex + ": " + Strings.dquote(trimedLine));
    }

    private void checkContains(final String expected, final List<String> list) {
        for (final String string : list) {
            final String trimedLine = string.trim();
            if (trimedLine.equals(expected)) {
                return;
            }
        }
        fail("Cannot find " + expected + " in " + list);
    }

    private void checkMapEntry(final String key, final String value, final boolean compact, final String str) {
        // "name":"value"
        // final String expected = String.format("- key: \"%s\"\n  value: \"%s\"", key, value);
        final String expected = String.format("%s: \"%s\"", key, value);
        assertTrue(str.contains(expected), "Cannot find " + expected + " in " + str);
    }

    private void checkProperty(
            final String key, final String value, final boolean compact, final String str, final boolean isValue) {
        final String propSep = this.toPropertySeparator(compact, isValue);
        // {"key":"MDC.B","value":"B_Value"}
        final String expected = String.format("%s%s\"%s\"", key, propSep, value);
        assertTrue(str.contains(expected), "Cannot find " + expected + " in " + str);
    }

    private void checkPropertyName(final String name, final boolean compact, final String str, final boolean isValue) {
        final String propSep = this.toPropertySeparator(compact, isValue);
        assertTrue(str.contains(name + propSep), str);
    }

    private void checkPropertyNameAbsent(
            final String name, final boolean compact, final String str, final boolean isValue) {
        final String propSep = this.toPropertySeparator(compact, isValue);
        assertFalse(str.contains(name + propSep), str);
    }

    private void testAllFeatures(
            final boolean includeSource,
            final boolean compact,
            final boolean eventEol,
            final boolean includeContext,
            final boolean contextMapAslist,
            final boolean includeStacktrace)
            throws Exception {
        final Log4jLogEvent expected = LogEventFixtures.createLogEvent();
        final AbstractJacksonLayout layout = YamlLayout.newBuilder()
                .setLocationInfo(includeSource)
                .setProperties(includeContext)
                .setIncludeStacktrace(includeStacktrace)
                .setCharset(StandardCharsets.UTF_8)
                .build();
        final String str = layout.toSerializable(expected);
        // System.out.println(str);
        // Just check for \n since \r might or might not be there.
        assertEquals(!compact || eventEol, str.contains("\n"), str);
        assertEquals(includeSource, str.contains("source"), str);
        assertEquals(includeContext, str.contains("contextMap"), str);
        final Log4jLogEvent actual = new Log4jYamlObjectMapper(contextMapAslist, includeStacktrace, false)
                .readValue(str, Log4jLogEvent.class);
        LogEventFixtures.assertEqualLogEvents(expected, actual, includeSource, includeContext, includeStacktrace);
        if (includeContext) {
            this.checkMapEntry("MDC.A", "A_Value", compact, str);
            this.checkMapEntry("MDC.B", "B_Value", compact, str);
        }
        //
        assertNull(actual.getThrown());
        // make sure the names we want are used
        this.checkPropertyName("instant", compact, str, false);
        this.checkPropertyName("thread", compact, str, true); // and not threadName
        this.checkPropertyName("level", compact, str, true);
        this.checkPropertyName("loggerName", compact, str, true);
        this.checkPropertyName("marker", compact, str, false);
        this.checkPropertyName("name", compact, str, true);
        this.checkPropertyName("parents", compact, str, false);
        this.checkPropertyName("message", compact, str, true);
        this.checkPropertyName("thrown", compact, str, false);
        this.checkPropertyName("cause", compact, str, false);
        this.checkPropertyName("commonElementCount", compact, str, true);
        this.checkPropertyName("localizedMessage", compact, str, true);
        if (includeStacktrace) {
            this.checkPropertyName("extendedStackTrace", compact, str, false);
            this.checkPropertyName("class", compact, str, true);
            this.checkPropertyName("method", compact, str, true);
            this.checkPropertyName("file", compact, str, true);
            this.checkPropertyName("line", compact, str, true);
            this.checkPropertyName("exact", compact, str, true);
            this.checkPropertyName("location", compact, str, true);
            this.checkPropertyName("version", compact, str, true);
        } else {
            this.checkPropertyNameAbsent("extendedStackTrace", compact, str, false);
        }
        this.checkPropertyName("suppressed", compact, str, false);
        this.checkPropertyName("loggerFqcn", compact, str, true);
        this.checkPropertyName("endOfBatch", compact, str, true);
        if (includeContext) {
            this.checkPropertyName("contextMap", compact, str, false);
        } else {
            this.checkPropertyNameAbsent("contextMap", compact, str, false);
        }
        this.checkPropertyName("contextStack", compact, str, false);
        if (includeSource) {
            this.checkPropertyName("source", compact, str, false);
        } else {
            this.checkPropertyNameAbsent("source", compact, str, false);
        }
        // check some attrs
        this.checkProperty("loggerFqcn", "f.q.c.n", compact, str, true);
        this.checkProperty("loggerName", "a.B", compact, str, true);
    }

    @Test
    void testContentType() {
        final AbstractJacksonLayout layout = YamlLayout.createDefaultLayout();
        assertEquals("application/yaml; charset=UTF-8", layout.getContentType());
    }

    @Test
    void testDefaultCharset() {
        final AbstractJacksonLayout layout = YamlLayout.createDefaultLayout();
        assertEquals(StandardCharsets.UTF_8, layout.getCharset());
    }

    @Test
    void testEscapeLayout() {
        final Map<String, Appender> appenders = this.rootLogger.getAppenders();
        for (final Appender appender : appenders.values()) {
            this.rootLogger.removeAppender(appender);
        }
        final Configuration configuration = rootLogger.getContext().getConfiguration();
        // set up appender
        final AbstractJacksonLayout layout = YamlLayout.newBuilder()
                .setLocationInfo(true)
                .setProperties(true)
                .setIncludeStacktrace(true)
                .setConfiguration(configuration)
                .build();

        final ListAppender appender = new ListAppender("List", null, layout, true, false);
        appender.start();

        // set appender on root and set level to debug
        this.rootLogger.addAppender(appender);
        this.rootLogger.setLevel(Level.DEBUG);

        // output starting message
        this.rootLogger.debug("Here is a quote ' and then a double quote \"");

        appender.stop();

        final List<String> list = appender.getMessages();

        this.checkAt("---", 0, list);
        this.checkContains("level: \"DEBUG\"", list);
        this.checkContains("message: \"Here is a quote ' and then a double quote \\\"\"", list);
        this.checkContains("loggerFqcn: \"" + AbstractLogger.class.getName() + "\"", list);
        for (final Appender app : appenders.values()) {
            this.rootLogger.addAppender(app);
        }
    }

    /**
     * Test case for MDC conversion pattern.
     */
    @Test
    void testLayout() {
        final Map<String, Appender> appenders = this.rootLogger.getAppenders();
        for (final Appender appender : appenders.values()) {
            this.rootLogger.removeAppender(appender);
        }
        final Configuration configuration = rootLogger.getContext().getConfiguration();
        // set up appender
        // Use [[ and ]] to test header and footer (instead of [ and ])
        final AbstractJacksonLayout layout = YamlLayout.createLayout(configuration, true, true, "[[", "]]", null, true);
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

        this.checkAt("---", 0, list);
        this.checkContains("loggerFqcn: \"" + AbstractLogger.class.getName() + "\"", list);
        this.checkContains("level: \"DEBUG\"", list);
        this.checkContains("message: \"starting mdc pattern test\"", list);
        for (final Appender app : appenders.values()) {
            this.rootLogger.addAppender(app);
        }
    }

    @Test
    void testLayoutLoggerName() throws Exception {
        final AbstractJacksonLayout layout = YamlLayout.newBuilder()
                .setLocationInfo(false)
                .setProperties(false)
                .setIncludeStacktrace(true)
                .setCharset(StandardCharsets.UTF_8)
                .build();
        final Log4jLogEvent expected = Log4jLogEvent.newBuilder() //
                .setLoggerName("a.B") //
                .setLoggerFqcn("f.q.c.n") //
                .setLevel(Level.DEBUG) //
                .setMessage(new SimpleMessage("M")) //
                .setThreadName("threadName") //
                .setTimeMillis(1)
                .build();
        final String str = layout.toSerializable(expected);
        assertTrue(str.contains("loggerName: \"a.B\""), str);
        final Log4jLogEvent actual = new Log4jYamlObjectMapper(false, true, false).readValue(str, Log4jLogEvent.class);
        assertEquals(expected.getLoggerName(), actual.getLoggerName());
        assertEquals(expected, actual);
    }

    @Test
    void testAdditionalFields() throws Exception {
        final AbstractJacksonLayout layout = YamlLayout.newBuilder()
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
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        final JsonNode node = mapper.readTree(str);
        assertThat(getJSONChildNodeValueAsText(node, "KEY1"), equalTo("VALUE1"));
        assertThat(getJSONChildNodeValueAsText(node, "KEY2"), equalTo(new JavaLookup().getRuntime()));
    }

    private String getJSONChildNodeValueAsText(final JsonNode parentNode, final String key) {
        final JsonNode childNode = parentNode.get(key);
        if (childNode != null) {
            return childNode.asText();
        } else {
            return Strings.EMPTY;
        }
    }

    @Test
    void testLocationOffCompactOffMdcOff() throws Exception {
        this.testAllFeatures(false, false, false, false, false, true);
    }

    @Test
    void testLocationOnCompactOffEventEolOffMdcOn() throws Exception {
        this.testAllFeatures(true, false, false, true, false, true);
    }

    @Test
    void testExcludeStacktrace() throws Exception {
        this.testAllFeatures(false, false, false, false, false, false);
    }

    @Test
    void testStacktraceAsString() {
        final String str = prepareYAMLForStacktraceTests(true);
        assertTrue(str.contains("extendedStackTrace: \"java.lang.NullPointerException"), str);
    }

    @Test
    void testStacktraceAsNonString() {
        final String str = prepareYAMLForStacktraceTests(false);
        assertTrue(str.contains("extendedStackTrace:\n    - "), str);
    }

    private String prepareYAMLForStacktraceTests(final boolean stacktraceAsString) {
        final Log4jLogEvent expected = LogEventFixtures.createLogEvent();
        // @formatter:off
        final AbstractJacksonLayout layout = YamlLayout.newBuilder()
                .setIncludeStacktrace(true)
                .setStacktraceAsString(stacktraceAsString)
                .build();
        // @formatter:off
        return layout.toSerializable(expected);
    }

    @Test
    void testIncludeNullDelimiterTrue() {
        final AbstractJacksonLayout layout =
                YamlLayout.newBuilder().setIncludeNullDelimiter(true).build();
        final String str = layout.toSerializable(LogEventFixtures.createLogEvent());
        assertTrue(str.endsWith("\0"));
    }

    @Test
    void testIncludeNullDelimiterFalse() {
        final AbstractJacksonLayout layout =
                YamlLayout.newBuilder().setIncludeNullDelimiter(false).build();
        final String str = layout.toSerializable(LogEventFixtures.createLogEvent());
        assertFalse(str.endsWith("\0"));
    }

    private String toPropertySeparator(final boolean compact, final boolean value) {
        return value ? ": " : ":";
    }
}
