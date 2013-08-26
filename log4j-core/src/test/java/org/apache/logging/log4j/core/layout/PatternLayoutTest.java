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
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.BasicConfigurationFactory;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.util.Compare;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 *
 */
public class PatternLayoutTest {
    static String OUTPUT_FILE   = "target/output/PatternParser";
    static String WITNESS_FILE  = "witness/PatternParser";
    LoggerContext ctx = (LoggerContext) LogManager.getContext();
    Logger root = ctx.getLogger("");

    static String msgPattern = "%m%n";
    static final String regexPattern = "%replace{%logger %msg}{\\.}{/}";
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


    /**
     * Test case for MDC conversion pattern.
     */
    @Test
    public void mdcPattern() throws Exception {

        final String mdcMsgPattern1 = "%m : %X%n";
        final String mdcMsgPattern2 = "%m : %X{key1}%n";
        final String mdcMsgPattern3 = "%m : %X{key2}%n";
        final String mdcMsgPattern4 = "%m : %X{key3}%n";
        final String mdcMsgPattern5 = "%m : %X{key1},%X{key2},%X{key3}%n";

        // set up appender
        final PatternLayout layout = PatternLayout.createLayout(msgPattern, ctx.getConfiguration(), null, null, null);
        //FileOutputStream fos = new FileOutputStream(OUTPUT_FILE + "_mdc");
        final FileAppender appender = FileAppender.createAppender(OUTPUT_FILE + "_mdc", "false", "false", "File", "false",
            "true", "false", layout, null, "false", null, null);
        appender.start();

        // set appender on root and set level to debug
        root.addAppender(appender);
        root.setLevel(Level.DEBUG);

        // output starting message
        root.debug("starting mdc pattern test");

        layout.setConversionPattern(mdcMsgPattern1);
        root.debug("empty mdc, no key specified in pattern");

        layout.setConversionPattern(mdcMsgPattern2);
        root.debug("empty mdc, key1 in pattern");

        layout.setConversionPattern(mdcMsgPattern3);
        root.debug("empty mdc, key2 in pattern");

        layout.setConversionPattern(mdcMsgPattern4);
        root.debug("empty mdc, key3 in pattern");

        layout.setConversionPattern(mdcMsgPattern5);
        root.debug("empty mdc, key1, key2, and key3 in pattern");

        ThreadContext.put("key1", "value1");
        ThreadContext.put("key2", "value2");

        layout.setConversionPattern(mdcMsgPattern1);
        root.debug("filled mdc, no key specified in pattern");

        layout.setConversionPattern(mdcMsgPattern2);
        root.debug("filled mdc, key1 in pattern");

        layout.setConversionPattern(mdcMsgPattern3);
        root.debug("filled mdc, key2 in pattern");

        layout.setConversionPattern(mdcMsgPattern4);
        root.debug("filled mdc, key3 in pattern");

        layout.setConversionPattern(mdcMsgPattern5);
        root.debug("filled mdc, key1, key2, and key3 in pattern");

        ThreadContext.remove("key1");
        ThreadContext.remove("key2");

        layout.setConversionPattern(msgPattern);
        root.debug("finished mdc pattern test");

        assertTrue(Compare.compare(this.getClass(), OUTPUT_FILE + "_mdc", WITNESS_FILE + "_mdc"));

        root.removeAppender(appender);

        appender.stop();
    }

    @Test
    public void testRegex() throws Exception {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext();
        final PatternLayout layout = PatternLayout.createLayout(regexPattern, ctx.getConfiguration(),
            null, null, null);
        final LogEvent event = new Log4jLogEvent(this.getClass().getName(), null, "org.apache.logging.log4j.core.Logger",
            Level.INFO, new SimpleMessage("Hello, world!"), null);
        final byte[] result = layout.toByteArray(event);
        assertEquals("org/apache/logging/log4j/core/layout/PatternLayoutTest Hello, world!", new String(result));
    }
}
