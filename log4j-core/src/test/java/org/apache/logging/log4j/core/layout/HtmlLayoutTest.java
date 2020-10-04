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

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.AbstractLogEvent;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.BasicConfigurationFactory;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.junit.UsingAnyThreadContext;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.apache.logging.log4j.core.util.datetime.FixedDateFormat.FixedFormat;

@UsingAnyThreadContext
public class HtmlLayoutTest {
    private static class MyLogEvent extends AbstractLogEvent {
        private static final long serialVersionUID = 0;

        @Override
        public Instant getInstant() {
            MutableInstant result = new MutableInstant();
            result.initFromEpochMilli(getTimeMillis(), 456789);
            return result;
        }

        @Override
        public long getTimeMillis() {
            final Calendar cal = Calendar.getInstance();
            cal.set(2012, Calendar.NOVEMBER, 02, 14, 34, 02);
            cal.set(Calendar.MILLISECOND, 123);
            return cal.getTimeInMillis();
        }

        @Override
        public Level getLevel() {
            return Level.DEBUG;
        }

        @Override
        public Message getMessage() {
            return new SimpleMessage("msg");
        }
    }

    private final LoggerContext ctx = LoggerContext.getContext();
    private final Logger root = ctx.getRootLogger();

    static ConfigurationFactory cf = new BasicConfigurationFactory();

    @BeforeAll
    public static void setupClass() {
        ConfigurationFactory.setConfigurationFactory(cf);
        final LoggerContext ctx = LoggerContext.getContext();
        ctx.reconfigure();
    }

    @AfterAll
    public static void cleanupClass() {
        ConfigurationFactory.removeConfigurationFactory(cf);
    }

    private static final String body = "<tr><td bgcolor=\"#993300\" style=\"color:White; font-size : small;\" colspan=\"6\">java.lang.NullPointerException: test";

    private static final String multiLine = "<td title=\"Message\">First line<br />Second line</td>";

    @Test
    public void testDefaultContentType() {
        final HtmlLayout layout = HtmlLayout.createDefaultLayout();
        assertEquals("text/html; charset=UTF-8", layout.getContentType());
    }

    @Test
    public void testContentType() {
        final HtmlLayout layout = HtmlLayout.newBuilder()
            .withContentType("text/html; charset=UTF-16")
            .build();
        assertEquals("text/html; charset=UTF-16", layout.getContentType());
        // TODO: make sure this following bit works as well
//        assertEquals(Charset.forName("UTF-16"), layout.getCharset());
    }

    @Test
    public void testDefaultCharset() {
        final HtmlLayout layout = HtmlLayout.createDefaultLayout();
        assertEquals(StandardCharsets.UTF_8, layout.getCharset());
    }

    /**
     * Test case for MDC conversion pattern.
     */
    @Test
    public void testLayoutIncludeLocationNo() throws Exception {
        testLayout(false);
    }

    @Test
    public void testLayoutIncludeLocationYes() throws Exception {
        testLayout(true);
    }

    private void testLayout(final boolean includeLocation) throws Exception {
        final Map<String, Appender> appenders = root.getAppenders();
        for (final Appender appender : appenders.values()) {
            root.removeAppender(appender);
        }
        // set up appender
        final HtmlLayout layout = HtmlLayout.newBuilder()
            .withLocationInfo(includeLocation)
            .build();
        final ListAppender appender = new ListAppender("List", null, layout, true, false);
        appender.start();

        // set appender on root and set level to debug
        root.addAppender(appender);
        root.setLevel(Level.DEBUG);

        // output starting message
        root.debug("starting mdc pattern test");

        root.debug("empty mdc");

        root.debug("First line\nSecond line");

        ThreadContext.put("key1", "value1");
        ThreadContext.put("key2", "value2");

        root.debug("filled mdc");

        ThreadContext.remove("key1");
        ThreadContext.remove("key2");

        root.error("finished mdc pattern test", new NullPointerException("test"));

        appender.stop();

        final List<String> list = appender.getMessages();
        final StringBuilder sb = new StringBuilder();
        for (final String string : list) {
            sb.append(string);
        }
        final String html = sb.toString();
        assertTrue(list.size() > 85, "Incorrect number of lines. Require at least 85 " + list.size());
        final String string = list.get(3);
        assertEquals("<meta charset=\"UTF-8\"/>", string, "Incorrect header: " + string);
        assertEquals("<title>Log4j Log Messages</title>", list.get(4), "Incorrect title");
        assertEquals("</body></html>", list.get(list.size() - 1), "Incorrect footer");
        if (includeLocation) {
            assertEquals(list.get(50), multiLine, "Incorrect multiline");
            assertTrue(html.contains("HtmlLayoutTest.java:"), "Missing location");
            assertEquals(list.get(71), body, "Incorrect body");
        } else {
            assertFalse(html.contains("<td>HtmlLayoutTest.java:"), "Location should not be in the output table");
        }
        for (final Appender app : appenders.values()) {
            root.addAppender(app);
        }
    }

    @Test
    public void testLayoutWithoutDataPattern() {
        final HtmlLayout layout = HtmlLayout.newBuilder().build();

        MyLogEvent event = new MyLogEvent();
        String actual = getDateLine(layout.toSerializable(event));

        long jvmStratTime = ManagementFactory.getRuntimeMXBean().getStartTime();
        assertEquals("<td>" + (event.getTimeMillis() - jvmStratTime) + "</td>", actual, "Incorrect date:" + actual);
    }

    @Test
    public void testLayoutWithDatePatternJvmElapseTime() {
        final HtmlLayout layout = HtmlLayout.newBuilder().setDatePattern("JVM_ELAPSE_TIME").build();

        MyLogEvent event = new MyLogEvent();
        String actual = getDateLine(layout.toSerializable(event));

        long jvmStratTime = ManagementFactory.getRuntimeMXBean().getStartTime();
        assertEquals("<td>" + (event.getTimeMillis() - jvmStratTime) + "</td>", actual, "Incorrect date:" + actual);
    }

    @Test
    public void testLayoutWithDatePatternUnix() {
        final HtmlLayout layout = HtmlLayout.newBuilder().setDatePattern("UNIX").build();

        MyLogEvent event = new MyLogEvent();
        String actual = getDateLine(layout.toSerializable(event));

        assertEquals("<td>" + event.getInstant().getEpochSecond() + "</td>", actual, "Incorrect date:" + actual);
    }

    @Test
    public void testLayoutWithDatePatternUnixMillis() {
        final HtmlLayout layout = HtmlLayout.newBuilder().setDatePattern("UNIX_MILLIS").build();

        MyLogEvent event = new MyLogEvent();
        String actual = getDateLine(layout.toSerializable(event));

        assertEquals("<td>" + event.getTimeMillis() + "</td>", actual, "Incorrect date:" + actual);
    }

    @Test
    public void testLayoutWithDatePatternFixedFormat() {
        for (final String timeZone : new String[] {"GMT+8", "UTC", null}) {
            for (final FixedFormat format : FixedFormat.values()) {
                testLayoutWithDatePatternFixedFormat(format, timeZone);
            }
        }
    }
    
    private String getDateLine(String logEventString) {
        return logEventString.split(System.lineSeparator())[2];
    }

    private void testLayoutWithDatePatternFixedFormat(FixedFormat format, String timezone) {
        final HtmlLayout layout = HtmlLayout.newBuilder().setDatePattern(format.name()).setTimezone(timezone).build();

        LogEvent event = new MyLogEvent();
        String actual = getDateLine(layout.toSerializable(event));

        // build expected date string
        java.time.Instant instant =
            java.time.Instant.ofEpochSecond(event.getInstant().getEpochSecond(), event.getInstant().getNanoOfSecond());
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        if (timezone != null) {
            zonedDateTime = zonedDateTime.withZoneSameInstant(ZoneId.of(timezone));
        }

        // For DateTimeFormatter of jdk,
        // Pattern letter 'S' means fraction-of-second, 'n' means nano-of-second. Log4j2 needs S.
        // Pattern letter 'X' (upper case) will output 'Z' when the offset to be output would be zero,
        // whereas pattern letter 'x' (lower case) will output '+00', '+0000', or '+00:00'. Log4j2 needs x.
        DateTimeFormatter dateTimeFormatter =
            DateTimeFormatter.ofPattern(format.getPattern().replace('n', 'S').replace('X', 'x'));
        String expected = zonedDateTime.format(dateTimeFormatter);

        assertEquals("<td>" + expected + "</td>", actual,
            MessageFormat.format("Incorrect date={0}, format={1}, timezone={2}", actual, format.name(), timezone));
    }
}
