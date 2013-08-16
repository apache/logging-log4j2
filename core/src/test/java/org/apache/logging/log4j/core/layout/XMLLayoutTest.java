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
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.BasicConfigurationFactory;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.helpers.Charsets;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class XMLLayoutTest {
    private static final String body = "<Message><![CDATA[empty mdc]]></Message>";
    static ConfigurationFactory cf = new BasicConfigurationFactory();

    @AfterClass
    public static void cleanupClass() {
        ConfigurationFactory.removeConfigurationFactory(cf);
    }

    @BeforeClass
    public static void setupClass() {
        ConfigurationFactory.setConfigurationFactory(cf);
        final LoggerContext ctx = (LoggerContext) LogManager.getContext();
        ctx.reconfigure();
    }

    LoggerContext ctx = (LoggerContext) LogManager.getContext();

    Logger root = ctx.getLogger("");

    @Test
    public void testContentType() {
        final XMLLayout layout = XMLLayout.createLayout(null, null, null, null, null, null);
        assertEquals("text/xml; charset=UTF-8", layout.getContentType());
    }

    @Test
    public void testDefaultCharset() {
        final XMLLayout layout = XMLLayout.createLayout(null, null, null, null, null, null);
        assertEquals(Charsets.UTF_8, layout.getCharset());
    }

    /**
     * Test case for MDC conversion pattern.
     */
    @Test
    public void testLayout() throws Exception {

        // set up appender
        final XMLLayout layout = XMLLayout.createLayout("true", "true", "true", null, null, null);
        final ListAppender appender = new ListAppender("List", null, layout, true, false);
        appender.start();

        // set appender on root and set level to debug
        root.addAppender(appender);
        root.setLevel(Level.DEBUG);

        // output starting message
        root.debug("starting mdc pattern test");

        root.debug("empty mdc");

        ThreadContext.put("key1", "value1");
        ThreadContext.put("key2", "value2");

        root.debug("filled mdc");

        ThreadContext.remove("key1");
        ThreadContext.remove("key2");

        root.error("finished mdc pattern test", new NullPointerException("test"));

        appender.stop();

        final List<String> list = appender.getMessages();

        assertTrue("Incorrect number of lines. Require at least 50 " + list.size(), list.size() > 50);
        final String string = list.get(0);
        assertTrue("Incorrect header: " + string, string.equals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertTrue("Incorrect footer", list.get(list.size() - 1).equals("</Events>"));
        assertTrue("Incorrect body. Expected " + body + " Actual: " + list.get(7), list.get(7).trim().equals(body));
    }
}
