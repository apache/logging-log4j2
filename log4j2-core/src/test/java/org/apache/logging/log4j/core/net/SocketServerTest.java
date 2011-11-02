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
import org.apache.logging.log4j.core.filter.CompositeFilter;
import org.apache.logging.log4j.core.filter.FilterBase;
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

    LoggerContext ctx = (LoggerContext) LogManager.getContext();
    Logger root = ctx.getLogger("SocketServerTest");

    @BeforeClass
    public static void setupClass() throws Exception {
        ((LoggerContext) LogManager.getContext()).reconfigure();
        tcp = new SocketServer(PORTNUM);
        thread = new Thread(tcp);
        thread.start();

    }

    @AfterClass
    public static void cleanupClass() {
        tcp.shutdown();
        try {
            thread.join();
        } catch (InterruptedException iex) {

        }
    }

    @After
    public void teardown() {
        Map<String,Appender> map = root.getAppenders();
        for (Map.Entry<String, Appender> entry : map.entrySet()) {
            Appender app = entry.getValue();
            root.removeAppender(app);
            app.stop();
        }
    }

    @Test
    public void testServer() throws Exception {
        Filter socketFilter = new ThreadFilter(Filter.Result.NEUTRAL, Filter.Result.DENY);
        Filter serverFilter = new ThreadFilter(Filter.Result.DENY, Filter.Result.NEUTRAL);
        CompositeFilter socketFilters = CompositeFilter.createFilters(new Filter[]{socketFilter});
        SocketAppender appender = SocketAppender.createAppender("localhost", PORT, "tcp", "-1",
            "Test", null, null, null, socketFilters);
        appender.start();
        CompositeFilter serverFilters = CompositeFilter.createFilters(new Filter[]{serverFilter});
        ListAppender listApp = new ListAppender("Events", serverFilters, null, false, false);
        appender.start();
        PatternLayout layout = new PatternLayout("%m %ex%n");
        ConsoleAppender console = ConsoleAppender.createAppender(layout, null, "SYSTEM_OUT", "Console", "true");
        Logger serverLogger = ctx.getLogger(SocketServer.class.getName());
        serverLogger.addAppender(console);
        serverLogger.setAdditive(false);

        // set appender on root and set level to debug
        root.addAppender(appender);
        root.addAppender(listApp);
        root.setAdditive(false);
        root.setLevel(Level.DEBUG);
        root.debug("This is a test message");
        Thread.sleep(100);
        List<LogEvent> events = listApp.getEvents();
        assertNotNull("No event retrieved", events);
        assertTrue("No events retrieved", events.size() > 0);
        assertTrue("Incorrect event", events.get(0).getMessage().getFormattedMessage().equals("This is a test message"));
    }

    private class ThreadFilter extends FilterBase {

        public ThreadFilter(Result onMatch, Result onMismatch) {
            super(onMatch, onMismatch);
        }

        public Filter.Result filter(LogEvent event) {
            return event.getThreadName().equals(Thread.currentThread().getName()) ? onMatch : onMismatch;
        }
    }

}
