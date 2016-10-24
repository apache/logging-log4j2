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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.BasicConfigurationFactory;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.junit.ThreadContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class HtmlLayoutTest {
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
        assertTrue("Incorrect number of lines. Require at least 85 " + list.size(), list.size() > 85);
        final String string = list.get(3);
        assertTrue("Incorrect header: " + string, string.equals("<meta charset=\"UTF-8\"/>"));
        assertTrue("Incorrect title", list.get(4).equals("<title>Log4j Log Messages</title>"));
        assertTrue("Incorrect footer", list.get(list.size() - 1).equals("</body></html>"));
        if (includeLocation) {
            assertTrue("Incorrect multiline", list.get(50).equals(multiLine));
            assertTrue("Missing location", html.contains("HtmlLayoutTest.java:"));
            assertTrue("Incorrect body", list.get(71).equals(body));
        } else {
            assertFalse("Location should not be in the output table", html.contains("<td>HtmlLayoutTest.java:"));
        }
        for (final Appender app : appenders.values()) {
            root.addAppender(app);
        }
    }
}
