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
package org.apache.logging.log4j.core.appender;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.test.appender.InMemoryAppender;
import org.apache.logging.log4j.util.Strings;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class InMemoryAppenderTest {

    @Test
    public void testAppender() {
        final Layout<String> layout = PatternLayout.createDefaultLayout();
        final boolean writeHeader = true;
        final InMemoryAppender app = new InMemoryAppender("test", layout, null, false, writeHeader);
        final String expectedHeader = null;
        assertMessage("Test", app, expectedHeader);
    }

    @Test
    public void testHeaderRequested() {
        final PatternLayout layout = PatternLayout.newBuilder().withHeader("HEADERHEADER").build();
        final boolean writeHeader = true;
        final InMemoryAppender app = new InMemoryAppender("test", layout, null, false, writeHeader);
        final String expectedHeader = "HEADERHEADER";
        assertMessage("Test", app, expectedHeader);
    }

    @Test
    public void testHeaderSuppressed() {
        final PatternLayout layout = PatternLayout.newBuilder().withHeader("HEADERHEADER").build();
        final boolean writeHeader = false;
        final InMemoryAppender app = new InMemoryAppender("test", layout, null, false, writeHeader);
        final String expectedHeader = null;
        assertMessage("Test", app, expectedHeader);
    }

    private void assertMessage(final String string, final InMemoryAppender app, final String header) {
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("TestLogger") //
                .setLoggerFqcn(InMemoryAppenderTest.class.getName()) //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage("Test")) //
                .build();
        app.start();
        assertTrue("Appender did not start", app.isStarted());
        app.append(event);
        app.append(event);
        final String msg = app.toString();
        assertNotNull("No message", msg);
        final String expectedHeader = header == null ? "" : header;
        final String expected = expectedHeader + "Test" + Strings.LINE_SEPARATOR + "Test" + Strings.LINE_SEPARATOR;
        assertTrue("Incorrect message: " + msg, msg.equals(expected));
        app.stop();
        assertFalse("Appender did not stop", app.isStarted());
    }
}
