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
package org.apache.logging.log4j.core.net.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.SocketAppender;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.core.layout.JSONLayout;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.layout.XMLLayout;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 */
public abstract class AbstractSocketServerTest {

    protected static Thread thread;

    private class ThreadFilter extends AbstractFilter {

        public ThreadFilter(final Result onMatch, final Result onMismatch) {
            super(onMatch, onMismatch);
        }

        @Override
        public Filter.Result filter(final LogEvent event) {
            return event.getThreadName().equals(Thread.currentThread().getName()) ? onMatch : onMismatch;
        }
    }

    private static final String MESSAGE = "This is test message";

    private static final String MESSAGE_2 = "This is test message 2";

    static final String PORT = "8199";

    static final int PORT_NUM = Integer.parseInt(PORT);

    private final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);

    private final boolean expectLengthException;

    private final String port;

    private final String protocol;

    private final Logger rootLogger = ctx.getLogger(AbstractSocketServerTest.class.getSimpleName());

    protected AbstractSocketServerTest(final String protocol, final String port, final boolean expectLengthException) {
        this.protocol = protocol;
        this.port = port;
        this.expectLengthException = expectLengthException;
    }

    protected Layout<String> createJsonLayout() {
        return JSONLayout.createLayout("true", "true", "false", "false", null);
    }

    protected abstract Layout<? extends Serializable> createLayout();

    protected Layout<? extends Serializable> createSerializedLayout() {
        return null;
    }

    protected Layout<String> createXmlLayout() {
        return XMLLayout.createLayout("true", "true", "false", null, null);
    }

    @After
    public void tearDown() {
        final Map<String, Appender> map = rootLogger.getAppenders();
        for (final Map.Entry<String, Appender> entry : map.entrySet()) {
            final Appender appender = entry.getValue();
            rootLogger.removeAppender(appender);
            appender.stop();
        }
    }

    @Test
    @Ignore("Broken test?")
    public void test1000ShortMessages() throws Exception {
        testServer(1000);
    }

    @Test
    @Ignore("Broken test?")
    public void test100ShortMessages() throws Exception {
        testServer(100);
    }

    @Test
    public void test10ShortMessages() throws Exception {
        testServer(10);
    }

    @Test
    public void test1ShortMessages() throws Exception {
        testServer(1);
    }

    @Test
    public void test2ShortMessages() throws Exception {
        testServer(2);
    }

    @Test
    public void test64KBMessages() throws Exception {
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

    private void testServer(final int size) throws Exception {
        final String[] messages = new String[size];
        for (int i = 0; i < messages.length; i++) {
            messages[i] = MESSAGE + " " + i;
        }
        testServer(messages);
    }

    protected void testServer(final String... messages) throws Exception {
        final Filter socketFilter = new ThreadFilter(Filter.Result.NEUTRAL, Filter.Result.DENY);
        final Filter serverFilter = new ThreadFilter(Filter.Result.DENY, Filter.Result.NEUTRAL);
        final Layout<? extends Serializable> socketLayout = createLayout();
        final SocketAppender socketAppender = SocketAppender.createAppender("localhost", this.port, this.protocol, "-1",
                null, "Test", "true", "false", socketLayout, socketFilter, null, null);
        socketAppender.start();
        final ListAppender listAppender = new ListAppender("Events", serverFilter, null, false, false);
        listAppender.start();
        final PatternLayout layout = PatternLayout.createLayout("%m %ex%n", null, null, null, null, null, null, null);
        final ConsoleAppender console = ConsoleAppender.createAppender(layout, null, "SYSTEM_OUT", "Console", "false",
                "true");
        final Logger serverLogger = ctx.getLogger(this.getClass().getName());
        serverLogger.addAppender(console);
        serverLogger.setAdditive(false);

        // set appender on root and set level to debug
        rootLogger.addAppender(socketAppender);
        rootLogger.addAppender(listAppender);
        rootLogger.setAdditive(false);
        rootLogger.setLevel(Level.DEBUG);
        for (final String message : messages) {
            rootLogger.debug(message);
        }
        final int MAX_TRIES = 100;
        for (int i = 0; i < MAX_TRIES; i++) {
            if (listAppender.getEvents().size() < messages.length) {
                try {
                    // Let the server-side read the messages.
                    Thread.sleep(40);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                break;
            }
        }
        final List<LogEvent> events = listAppender.getEvents();
        assertNotNull("No event retrieved", events);
        assertEquals("Incorrect number of events received", messages.length, events.size());
        for (int i = 0; i < messages.length; i++) {
            assertTrue("Incorrect event", events.get(i).getMessage().getFormattedMessage().equals(messages[i]));
        }
    }

}
