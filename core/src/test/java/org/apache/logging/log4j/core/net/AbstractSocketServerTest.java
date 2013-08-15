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
package org.apache.logging.log4j.core.net;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.SocketAppender;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.After;
import org.junit.Test;

/**
 *
 */
public abstract class AbstractSocketServerTest {

    private class ThreadFilter extends AbstractFilter {

        public ThreadFilter(final Result onMatch, final Result onMismatch) {
            super(onMatch, onMismatch);
        }

        @Override
        public Filter.Result filter(final LogEvent event) {
            return event.getThreadName().equals(Thread.currentThread().getName()) ? onMatch : onMismatch;
        }
    }

    private static final String MESSAGE_1 = "This is a test message";
    private static final String MESSAGE_2 = "This is test message 2";

    private final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);

    private final String port;
    private final String protocol;
    private final boolean expectLengthException;

    private final Logger root = ctx.getLogger(AbstractSocketServerTest.class.getSimpleName());

    protected AbstractSocketServerTest(final String protocol, final String port, final boolean expectLengthException) {
        this.protocol = protocol;
        this.port = port;
        this.expectLengthException = expectLengthException;
    }

    @After
    public void tearDown() {
        final Map<String, Appender> map = root.getAppenders();
        for (final Map.Entry<String, Appender> entry : map.entrySet()) {
            final Appender app = entry.getValue();
            root.removeAppender(app);
            app.stop();
        }
    }

    @Test
    public void test64KMessages() throws Exception {
        final char[] a64K = new char[1024 * 64];
        Arrays.fill(a64K, 'a');
        final String m1 = new String(a64K);
        final String m2 = MESSAGE_2 + m1;
        if (expectLengthException) {
            try {
                testServer(m1, m2);

            } catch (final AppenderLoggingException are) {
                assertTrue("", are.getCause() != null && are.getCause() instanceof IOException);
                // Failure expected.
            }
        } else {
            testServer(m1, m2);
        }
    }

    protected void testServer(final String message1, final String message2) throws Exception {
        final Filter socketFilter = new ThreadFilter(Filter.Result.NEUTRAL, Filter.Result.DENY);
        final Filter serverFilter = new ThreadFilter(Filter.Result.DENY, Filter.Result.NEUTRAL);
        final SocketAppender appender = SocketAppender.createAppender("localhost", this.port, this.protocol, "-1", null, "Test", null,
                "false", null, socketFilter, null, null);
        appender.start();
        final ListAppender listApp = new ListAppender("Events", serverFilter, null, false, false);
        listApp.start();
        final PatternLayout layout = PatternLayout.createLayout("%m %ex%n", null, null, null, null);
        final ConsoleAppender console = ConsoleAppender.createAppender(layout, null, "SYSTEM_OUT", "Console", "false", "true");
        final Logger serverLogger = ctx.getLogger(this.getClass().getName());
        serverLogger.addAppender(console);
        serverLogger.setAdditive(false);

        // set appender on root and set level to debug
        root.addAppender(appender);
        root.addAppender(listApp);
        root.setAdditive(false);
        root.setLevel(Level.DEBUG);
        root.debug(message1);
        root.debug(message2);
        Thread.sleep(100);
        final List<LogEvent> events = listApp.getEvents();
        assertNotNull("No event retrieved", events);
        assertTrue("No events retrieved", events.size() > 0);
        assertTrue("Incorrect event", events.get(0).getMessage().getFormattedMessage().equals(message1));
        assertTrue("Incorrect number of events received: " + events.size(), events.size() == 2);
        assertTrue("Incorrect event", events.get(1).getMessage().getFormattedMessage().equals(message2));
    }

    @Test
    public void testShortMessages() throws Exception {
        testServer(MESSAGE_1, MESSAGE_2);
    }

}
