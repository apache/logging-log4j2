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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.apache.logging.log4j.core.appender.SocketAppender;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class SocketServerTest {

    private static final String HOST = "localhost";
    private static final String PORT = "8199";
    private static final int PORTNUM = Integer.parseInt(PORT);

    private static BlockingQueue<LogEvent> list = new ArrayBlockingQueue<LogEvent>(10);

    private static SocketServer tcp;
    private static Thread thread;

    LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
    Logger root = ctx.getLogger("SocketServerTest");

    @BeforeClass
    public static void setupClass() throws Exception {
        ((LoggerContext) LogManager.getContext(false)).reconfigure();
        tcp = new SocketServer(PORTNUM);
        thread = new Thread(tcp);
        thread.start();

    }

    @AfterClass
    public static void cleanupClass() {
        tcp.shutdown();
        try {
            thread.join();
        } catch (final InterruptedException iex) {

        }
    }

    @After
    public void teardown() {
        final Map<String,Appender<?>> map = root.getAppenders();
        for (final Map.Entry<String, Appender<?>> entry : map.entrySet()) {
            final Appender<?> app = entry.getValue();
            root.removeAppender(app);
            app.stop();
        }
    }

    @Test
    public void testServer() throws Exception {
        final Filter socketFilter = new ThreadFilter(Filter.Result.NEUTRAL, Filter.Result.DENY);
        final Filter serverFilter = new ThreadFilter(Filter.Result.DENY, Filter.Result.NEUTRAL);
        final SocketAppender appender = SocketAppender.createAppender("localhost", PORT, "tcp", "-1",
            "Test", null, null, null, socketFilter);
        appender.start();
        final ListAppender listApp = new ListAppender("Events", serverFilter, null, false, false);
        listApp.start();
        final PatternLayout layout = PatternLayout.createLayout("%m %ex%n", null, null, null);
        final ConsoleAppender console = ConsoleAppender.createAppender(layout, null, "SYSTEM_OUT", "Console", "false", "true");
        final Logger serverLogger = ctx.getLogger(SocketServer.class.getName());
        serverLogger.addAppender(console);
        serverLogger.setAdditive(false);

        // set appender on root and set level to debug
        root.addAppender(appender);
        root.addAppender(listApp);
        root.setAdditive(false);
        root.setLevel(Level.DEBUG);
        root.debug("This is a test message");
        root.debug("This is test message 2");
        Thread.sleep(100);
        final List<LogEvent> events = listApp.getEvents();
        assertNotNull("No event retrieved", events);
        assertTrue("No events retrieved", events.size() > 0);
        assertTrue("Incorrect event", events.get(0).getMessage().getFormattedMessage().equals("This is a test message"));
        assertTrue("Incorrect number of events received", events.size() == 2);
        assertTrue("Incorrect event", events.get(1).getMessage().getFormattedMessage().equals("This is test message 2"));
    }

    private class ThreadFilter extends AbstractFilter {

        public ThreadFilter(final Result onMatch, final Result onMismatch) {
            super(onMatch, onMismatch);
        }

        @Override
        public Filter.Result filter(final LogEvent event) {
            return event.getThreadName().equals(Thread.currentThread().getName()) ? onMatch : onMismatch;
        }
    }

}
