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
import org.apache.logging.log4j.core.appender.ConsoleAppender.Target;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.message.SimpleMessage;
import org.easymock.EasyMockSupport;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 */
public class ConsoleAppenderTest {

    private static final String LOG4J_SKIP_JANSI = "log4j.skipJansi";

    @AfterClass
    public static void afterClass() {
        System.clearProperty(LOG4J_SKIP_JANSI);
    }

    @BeforeClass
    public static void beforeClass() {
        System.setProperty(LOG4J_SKIP_JANSI, "true");
    }
    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

    EasyMockSupport mocks;

    PrintStream psMock;

    @Before
    public void before() {
        System.setProperty(LOG4J_SKIP_JANSI, "true");
        mocks = new EasyMockSupport();
        psMock = mocks.createMock("psMock", PrintStream.class);
    }

    private void testConsoleStreamManagerDoesNotClose(PrintStream ps, String targetName) {
        try {
            psMock.write((byte[]) anyObject(), anyInt(), anyInt());
            expectLastCall().anyTimes();
            psMock.flush();

            mocks.replayAll();
            System.setOut(psMock);
            final Layout<String> layout = PatternLayout.createLayout(null, null, null, null, false, false, null, null);
            final ConsoleAppender app = ConsoleAppender.createAppender(layout, null, targetName, "Console", "false",
                    "false");
            app.start();
            assertTrue("Appender did not start", app.isStarted());

            final LogEvent event = new Log4jLogEvent("TestLogger", null, ConsoleAppenderTest.class.getName(),
                    Level.INFO, new SimpleMessage("Test"), null);
            app.append(event);

            app.stop();
            assertFalse("Appender did not stop", app.isStarted());
        } finally {
            System.setOut(ps);
        }
        mocks.verifyAll();
    }

    @Test
    @Ignore
    public void testFollowSystemErr() {
        testFollowSystemPrintStream(System.err, Target.SYSTEM_ERR);
    }

    @Test
    public void testFollowSystemOut() {
        testFollowSystemPrintStream(System.out, Target.SYSTEM_OUT);
    }

    private void testFollowSystemPrintStream(PrintStream ps, Target target) {
        final ConsoleAppender app = ConsoleAppender.newBuilder().setTarget(target).setFollow(true)
                .setIgnoreExceptions(false).build();
        app.start();
        try {
            final LogEvent event = new Log4jLogEvent("TestLogger", null, ConsoleAppenderTest.class.getName(),
                    Level.INFO, new SimpleMessage("Test"), null);

            assertTrue("Appender did not start", app.isStarted());
            System.setOut(new PrintStream(baos));
            app.append(event);
            System.setOut(ps);
            final String msg = baos.toString();
            assertNotNull("No message", msg);
            assertTrue("Incorrect message: \"" + msg + "\"", msg.endsWith("Test" + Constants.LINE_SEPARATOR));
        } finally {
            app.stop();
        }
        assertFalse("Appender did not stop", app.isStarted());
    }

    @Test
    @Ignore
    public void testSystemErrStreamManagerDoesNotClose() {
        testConsoleStreamManagerDoesNotClose(System.err, "SYSTEM_ERR");
    }

    @Test
    public void testSystemOutStreamManagerDoesNotClose() {
        testConsoleStreamManagerDoesNotClose(System.out, "SYSTEM_OUT");
    }

}
