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

import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.BasicConfigurationFactory;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.helpers.Charsets;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class HTMLLayoutTest {
    LoggerContext ctx = (LoggerContext) LogManager.getContext();
    Logger root = ctx.getLogger("");

    static ConfigurationFactory cf = new BasicConfigurationFactory();

    @BeforeClass
    public static void setupClass() {
        ConfigurationFactory.setConfigurationFactory(cf);
        final LoggerContext ctx = (LoggerContext) LogManager.getContext();
        ctx.reconfigure();
    }

    @AfterClass
    public static void cleanupClass() {
        ConfigurationFactory.removeConfigurationFactory(cf);
    }

    private static final String body =
        "<tr><td bgcolor=\"#993300\" style=\"color:White; font-size : small;\" colspan=\"6\">java.lang.NullPointerException: test";

    private static final String multiLine =
        "<td title=\"Message\">First line<br />Second line</td>";


    @Test
    public void testDefaultContentType() {
        final HTMLLayout layout = HTMLLayout.createLayout("true", null, null, null, "small", null);
        assertEquals("text/html; charset=UTF-8", layout.getContentType());
    }

    @Test
    public void testContentType() {
        final HTMLLayout layout = HTMLLayout.createLayout("true", null, "text/html; charset=UTF-16", null, "small", null);
        assertEquals("text/html; charset=UTF-16", layout.getContentType());
    }

    @Test
    public void testDefaultCharset() {
        final HTMLLayout layout = HTMLLayout.createLayout("true", null, null, null, "small", null);
        assertEquals(Charsets.UTF_8, layout.getCharset());
    }

    /**
     * Test case for MDC conversion pattern.
     */
    @Test
    public void testLayout() {

        // set up appender
        final HTMLLayout layout = HTMLLayout.createLayout("true", null, null, null, "small", null);
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
        assertTrue("Incorrect number of lines. Require at least 85 " + list.size(), list.size() > 85);
        final String string = list.get(3);
        assertTrue("Incorrect header: " + string, string.equals("<meta charset=\"UTF-8\"/>"));
        assertTrue("Incorrect title", list.get(4).equals("<title>Log4j Log Messages</title>"));
        assertTrue("Incorrect footer", list.get(list.size() - 1).equals("</body></html>"));
        assertTrue("Incorrect multiline", list.get(50).equals(multiLine));
        assertTrue("Incorrect body", list.get(71).equals(body));

    }
}
