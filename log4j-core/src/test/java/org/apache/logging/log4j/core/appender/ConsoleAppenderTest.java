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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.ConsoleAppender.Target;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.Strings;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeastOnce;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
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

    ByteArrayOutputStream baos;

    @Mock
    PrintStream psMock;

    @Before
    public void before() {
        System.setProperty(LOG4J_SKIP_JANSI, "true");
        baos = new ByteArrayOutputStream();
    }

    private enum SystemSetter {
        SYSTEM_OUT {
            @Override
            void systemSet(final PrintStream printStream) {
                System.setOut(printStream);
            }
        },
        SYSTEM_ERR {
            @Override
            void systemSet(final PrintStream printStream) {
                System.setErr(printStream);
            }
        },
        ;
        abstract void systemSet(PrintStream printStream);
    }

    private void testConsoleStreamManagerDoesNotClose(final PrintStream ps, final Target targetName, final SystemSetter systemSetter) {
        try {
            systemSetter.systemSet(psMock);
            final Layout<String> layout = PatternLayout.newBuilder().withAlwaysWriteExceptions(true).build();
            final ConsoleAppender app = ConsoleAppender.newBuilder().withLayout(layout).setTarget(targetName)
                    .withName("Console").withIgnoreExceptions(false).build();
            app.start();
            assertTrue("Appender did not start", app.isStarted());

            final LogEvent event = Log4jLogEvent.newBuilder() //
                    .setLoggerName("TestLogger") //
                    .setLoggerFqcn(ConsoleAppenderTest.class.getName()) //
                    .setLevel(Level.INFO) //
                    .setMessage(new SimpleMessage("Test")) //
                    .build();
            app.append(event);

            app.stop();
            assertFalse("Appender did not stop", app.isStarted());
        } finally {
            systemSetter.systemSet(ps);
        }
        then(psMock).should(atLeastOnce()).write(any(byte[].class), anyInt(), anyInt());
        then(psMock).should(atLeastOnce()).flush();
    }

    @Test
    public void testFollowSystemErr() {
        testFollowSystemPrintStream(System.err, Target.SYSTEM_ERR, SystemSetter.SYSTEM_ERR);
    }

    @Test
    public void testFollowSystemOut() {
        testFollowSystemPrintStream(System.out, Target.SYSTEM_OUT, SystemSetter.SYSTEM_OUT);
    }

    private void testFollowSystemPrintStream(final PrintStream ps, final Target target, final SystemSetter systemSetter) {
        final ConsoleAppender app = ConsoleAppender.newBuilder().setTarget(target).setFollow(true)
                .withIgnoreExceptions(false).withName("test").build();
        Assert.assertEquals(target, app.getTarget());
        app.start();
        try {
            final LogEvent event = Log4jLogEvent.newBuilder() //
                    .setLoggerName("TestLogger") //
                    .setLoggerFqcn(ConsoleAppenderTest.class.getName()) //
                    .setLevel(Level.INFO) //
                    .setMessage(new SimpleMessage("Test")) //
                    .build();

            assertTrue("Appender did not start", app.isStarted());
            systemSetter.systemSet(new PrintStream(baos));
            try {
                app.append(event);
            } finally {
                systemSetter.systemSet(ps);
            }
            final String msg = baos.toString();
            assertNotNull("No message", msg);
            assertTrue("Incorrect message: \"" + msg + "\"", msg.endsWith("Test" + Strings.LINE_SEPARATOR));
        } finally {
            app.stop();
        }
        assertFalse("Appender did not stop", app.isStarted());
    }

    @Test
    public void testSystemErrStreamManagerDoesNotClose() {
        testConsoleStreamManagerDoesNotClose(System.err, Target.SYSTEM_ERR, SystemSetter.SYSTEM_ERR);
    }

    @Test
    public void testSystemOutStreamManagerDoesNotClose() {
        testConsoleStreamManagerDoesNotClose(System.out, Target.SYSTEM_OUT, SystemSetter.SYSTEM_OUT);
    }

}
