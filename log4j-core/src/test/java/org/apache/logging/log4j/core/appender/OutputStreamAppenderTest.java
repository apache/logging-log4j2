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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.test.appender.InMemoryAppender;
import org.junit.Test;

/**
 *
 */
public class OutputStreamAppenderTest {

    @Test
    public void testAppender() {
        final Layout<String> layout = PatternLayout.createLayout(null, null, null, null, null, null, null, null);
        final InMemoryAppender app = new InMemoryAppender("test", layout, null, false);
        final LogEvent event = new Log4jLogEvent("TestLogger", null, OutputStreamAppenderTest.class.getName(), Level.INFO,
            new SimpleMessage("Test"), null);
        app.start();
        assertTrue("Appender did not start", app.isStarted());
        app.append(event);
        final String msg = app.toString();
        assertNotNull("No message", msg);
        assertTrue("Incorrect message: " + msg , msg.endsWith("Test" + Constants.LINE_SEPARATOR));
        app.stop();
        assertFalse("Appender did not stop", app.isStarted());
    }
}
