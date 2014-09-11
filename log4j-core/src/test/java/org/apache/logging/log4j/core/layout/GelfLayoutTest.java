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
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;

public class GelfLayoutTest {
    LoggerContext ctx = (LoggerContext) LogManager.getContext();
    Logger root = ctx.getLogger("");

    private static final String HOSTNAME = "TheHost";
    private static final String KEY1 = "Key1";
    private static final String KEY2 = "Key2";
    private static final String VALUE1 = "Value1";
    private static final String VALUE2 = "Value2";
    private static final String MDCKEY1 = "MdcKey1";
    private static final String MDCKEY2 = "MdcKey2";
    private static final String MDCVALUE1 = "MdcValue1";
    private static final String MDCVALUE2 = "MdcValue2";

    private static final String LINE1 = "empty mdc";
    private static final String LINE2 = "filled mdc";

    static ConfigurationFactory configFactory = new BasicConfigurationFactory();

    @BeforeClass
    public static void setupClass() {
        ThreadContext.clearAll();
        ConfigurationFactory.setConfigurationFactory(configFactory);
        final LoggerContext ctx = (LoggerContext) LogManager.getContext();
        ctx.reconfigure();
    }

    @AfterClass
    public static void cleanupClass() {
        ConfigurationFactory.removeConfigurationFactory(configFactory);
        ThreadContext.clearAll();
    }

    @Test
    public void testLayout() throws Exception {
        for (final Appender appender : root.getAppenders().values()) {
            root.removeAppender(appender);
        }
        // set up appender
        final GelfLayout layout = GelfLayout.createLayout(HOSTNAME, new KeyValuePair[] {
                new KeyValuePair(KEY1, VALUE1),
                new KeyValuePair(KEY2, VALUE2), }, 1024);
        // ConsoleAppender appender = new ConsoleAppender("Console", layout);
        final ListAppender eventAppender = new ListAppender("Events", null, null, true, false);
        final ListAppender appender = new ListAppender("Layouted", null, layout, true, false);
        eventAppender.start();
        appender.start();

        // set appender on root and set level to debug
        root.addAppender(eventAppender);
        root.addAppender(appender);
        root.setLevel(Level.DEBUG);

        root.debug(LINE1);

        ThreadContext.put(MDCKEY1, MDCVALUE1);
        ThreadContext.put(MDCKEY2, MDCVALUE2);

        root.info(LINE2);

        ThreadContext.clearMap();

        appender.stop();

        final List<LogEvent> events = eventAppender.getEvents();
        final List<String> list = appender.getMessages();

        //@formatter:off
        assertJsonEquals("{" +
                "\"version\": \"1.1\"," +
                "\"host\": \"" + HOSTNAME + "\"," +
                "\"timestamp\": "+GelfLayout.formatTimestamp(events.get(0).getTimeMillis())+"," +
                "\"level\": 7," +
                "\"_thread\": \"main\"," +
                "\"_logger\": \"\"," +
                "\"short_message\": \"" + LINE1 + "\"," +
                "\"_" + KEY1 + "\": \"" + VALUE1 + "\"," +
                "\"_" + KEY2 + "\": \"" + VALUE2 + "\"" +
                "}",
        list.get(0));

        assertJsonEquals("{" +
                "\"version\": \"1.1\"," +
                "\"host\": \"" + HOSTNAME + "\"," +
                "\"timestamp\": "+GelfLayout.formatTimestamp(events.get(1).getTimeMillis())+"," +
                "\"level\": 6," +
                "\"_thread\": \"main\"," +
                "\"_logger\": \"\"," +
                "\"short_message\": \"" + LINE2 + "\"," +
                "\"_" + KEY1 + "\": \"" + VALUE1 + "\"," +
                "\"_" + KEY2 + "\": \"" + VALUE2 + "\"," +
                "\"_" + MDCKEY1 + "\": \"" + MDCVALUE1 + "\"," +
                "\"_" + MDCKEY2 + "\": \"" + MDCVALUE2 + "\"" +
                "}",
        list.get(1));
        //@formatter:on
    }
}
