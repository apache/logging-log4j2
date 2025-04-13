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
package org.apache.logging.log4j.core.pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.Test;

class EncodingPatternConverterTest {

    @Test
    void testReplacement() {
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName(EncodingPatternConverterTest.class.getName()) //
                .setLevel(Level.DEBUG) //
                .setMessage(new SimpleMessage("Test \r\n<div class=\"test\">this</div> & <div class='test'>that</div>"))
                .build();
        final StringBuilder sb = new StringBuilder();
        final LoggerContext ctx = LoggerContext.getContext();
        final String[] options = new String[] {"%msg"};
        final EncodingPatternConverter converter =
                EncodingPatternConverter.newInstance(ctx.getConfiguration(), options);
        assertNotNull(converter, "Error creating converter");
        converter.format(event, sb);
        assertEquals(
                "Test \\r\\n&lt;div class=&quot;test&quot;&gt;this&lt;&#x2F;div&gt; &amp; &lt;div class=&apos;test&apos;&gt;that&lt;&#x2F;div&gt;",
                sb.toString());
    }

    @Test
    void testJsonEscaping() {
        final LogEvent event = Log4jLogEvent.newBuilder()
                .setLoggerName(getClass().getName())
                .setLevel(Level.DEBUG)
                .setMessage(new SimpleMessage(
                        "This string contains \"quotes\" and \\ backslash and \u001F control and\nnewline"))
                .build();
        final String expected =
                "This string contains \\\"quotes\\\" and \\\\ backslash and \\u001F control and\\nnewline";
        final StringBuilder sb = new StringBuilder();
        final LoggerContext ctx = LoggerContext.getContext();
        final String[] options = new String[] {"%msg", "JSON"};
        final EncodingPatternConverter converter =
                EncodingPatternConverter.newInstance(ctx.getConfiguration(), options);

        assertNotNull(converter, "Error creating converter");
        converter.format(event, sb);

        assertEquals(expected, sb.toString());
    }

    @Test
    void testCrlfEscaping() {
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName(EncodingPatternConverterTest.class.getName()) //
                .setLevel(Level.DEBUG) //
                .setMessage(
                        new SimpleMessage("Test \r\n<div class=\"test\">this\r</div> & \n<div class='test'>that</div>"))
                .build();
        final StringBuilder sb = new StringBuilder();
        final LoggerContext ctx = LoggerContext.getContext();
        final String[] options = new String[] {"%msg", "CRLF"};
        final EncodingPatternConverter converter =
                EncodingPatternConverter.newInstance(ctx.getConfiguration(), options);
        assertNotNull(converter, "Error creating converter");
        converter.format(event, sb);
        assertEquals("Test \\r\\n<div class=\"test\">this\\r</div> & \\n<div class='test'>that</div>", sb.toString());
    }

    @Test
    void testXmlEscaping() {
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName(EncodingPatternConverterTest.class.getName()) //
                .setLevel(Level.DEBUG) //
                .setMessage(new SimpleMessage("Test \r\n<div class=\"test\">this</div> & <div class='test'>that</div>"))
                .build();
        final StringBuilder sb = new StringBuilder();
        final LoggerContext ctx = LoggerContext.getContext();
        final String[] options = new String[] {"%msg", "XML"};
        final EncodingPatternConverter converter =
                EncodingPatternConverter.newInstance(ctx.getConfiguration(), options);
        assertNotNull(converter, "Error creating converter");
        converter.format(event, sb);
        assertEquals(
                "Test \r\n&lt;div class=&quot;test&quot;&gt;this&lt;/div&gt; &amp; &lt;div class=&apos;test&apos;&gt;that&lt;/div&gt;",
                sb.toString());
    }

    @Test
    void testHandlesThrowable() {
        final Configuration configuration = new DefaultConfiguration();
        assertFalse(EncodingPatternConverter.newInstance(configuration, new String[] {"%msg", "XML"})
                .handlesThrowable());
        assertTrue(EncodingPatternConverter.newInstance(configuration, new String[] {"%xThrowable{full}", "JSON"})
                .handlesThrowable());
        assertTrue(EncodingPatternConverter.newInstance(configuration, new String[] {"%ex", "XML"})
                .handlesThrowable());
    }
}
