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

import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.message.SimpleMessage;
import org.easymock.EasyMockSupport;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
public class ConsoleAppenderTest {

    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    EasyMockSupport mocks = new EasyMockSupport();
    PrintStream psMock = mocks.createMock("psMock", PrintStream.class);

    @BeforeClass
    public static void before() {
        System.setProperty("log4j.skipJansi", "true");
    }

    @AfterClass
    public static void after() {
        System.clearProperty("log4j.skipJansi");
    }

    @Test
    public void testConsoleStreamManagerDoesNotClose() {
        final PrintStream ps = System.out;

        psMock.write((byte[]) anyObject(), anyInt(), anyInt());
        expectLastCall().anyTimes();
        psMock.flush();

        mocks.replayAll();
        System.setOut(psMock);
        final Layout<String> layout = PatternLayout.createLayout(null, null, null, null, false, false, null, null);
        final ConsoleAppender app = ConsoleAppender.createAppender(layout, null, "SYSTEM_OUT", "Console", "false",
                "false");
        app.start();
        assertTrue("Appender did not start", app.isStarted());

        final LogEvent event = new Log4jLogEvent("TestLogger", null, ConsoleAppenderTest.class.getName(), Level.INFO,
                new SimpleMessage("Test"), null);
        app.append(event);

        app.stop();
        assertFalse("Appender did not stop", app.isStarted());

        System.setOut(ps);
        mocks.verifyAll();
    }
    
    @Test
    public void testFollow() {
        final PrintStream ps = System.out;
        final ConsoleAppender app = ConsoleAppender.newBuilder()
            .setFollow(true)
            .setIgnoreExceptions(false)
            .build();
        app.start();
        final LogEvent event = new Log4jLogEvent("TestLogger", null, ConsoleAppenderTest.class.getName(), Level.INFO,
            new SimpleMessage("Test"), null);

        assertTrue("Appender did not start", app.isStarted());
        System.setOut(new PrintStream(baos));
        app.append(event);
        System.setOut(ps);
        final String msg = baos.toString();
        assertNotNull("No message", msg);
        assertTrue("Incorrect message: " + msg , msg.endsWith("Test" + Constants.LINE_SEPARATOR));
        app.stop();
        assertFalse("Appender did not stop", app.isStarted());
    }

}
