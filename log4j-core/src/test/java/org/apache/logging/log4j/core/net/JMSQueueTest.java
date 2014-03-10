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

import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.JMSQueueAppender;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.core.filter.CompositeFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.net.jms.AbstractJMSReceiver;
import org.apache.logging.log4j.core.net.jms.JMSQueueReceiver;
import org.apache.logging.log4j.core.net.jms.JMSTopicReceiver;
import org.apache.logging.log4j.status.StatusConsoleListener;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockejb.jms.MockQueue;
import org.mockejb.jms.QueueConnectionFactoryImpl;
import org.mockejb.jndi.MockContextFactory;

/**
 *
 */
public class JMSQueueTest {

    private static final String FACTORY_NAME = "TestQueueConnectionFactory";
    private static final String QUEUE_NAME = "TestQueue";

    private static Context context;
    private static AbstractJMSReceiver receiver;

    LoggerContext ctx = (LoggerContext) LogManager.getContext();
    Logger root = ctx.getLogger("JMSQueueTest");

    @BeforeClass
    public static void setupClass() throws Exception {
        // MockContextFactory becomes the primary JNDI provider
        final StatusConsoleListener l = new StatusConsoleListener(Level.ERROR);
        StatusLogger.getLogger().registerListener(l);
        MockContextFactory.setAsInitial();
        context = new InitialContext();
        context.rebind(FACTORY_NAME, new QueueConnectionFactoryImpl() );
        context.rebind(QUEUE_NAME, new MockQueue(QUEUE_NAME));
        ((LoggerContext) LogManager.getContext()).reconfigure();
        receiver = new JMSQueueReceiver(FACTORY_NAME, QUEUE_NAME, null, null);
    }

    @AfterClass
    public static void cleanupClass() {
        StatusLogger.getLogger().reset();
    }

    @After
    public void teardown() {
        final Map<String,Appender> map = root.getAppenders();
        for (final Map.Entry<String, Appender> entry : map.entrySet()) {
            final Appender app = entry.getValue();
            root.removeAppender(app);
            app.stop();
        }
    }

    @Test
    public void testServer() throws Exception {
        final Filter clientFilter = new MessageFilter(Filter.Result.NEUTRAL, Filter.Result.DENY);
        final Filter serverFilter = new MessageFilter(Filter.Result.DENY, Filter.Result.NEUTRAL);
        final CompositeFilter clientFilters = CompositeFilter.createFilters(new Filter[]{clientFilter});
        final JMSQueueAppender appender = JMSQueueAppender.createAppender("Test", null, null, null, null, null, FACTORY_NAME,
                QUEUE_NAME, null, null, null, clientFilters, "true");
        appender.start();
        final CompositeFilter serverFilters = CompositeFilter.createFilters(new Filter[]{serverFilter});
        final ListAppender listApp = new ListAppender("Events", serverFilters, null, false, false);
        listApp.start();
        final PatternLayout layout = PatternLayout.createLayout("%m %ex%n", null, null, null, null, null, null, null);
        final ConsoleAppender console = ConsoleAppender.createAppender(layout, null, "SYSTEM_OUT", "Console", "false", "true");
        console.start();
        final Logger serverLogger = ctx.getLogger(JMSTopicReceiver.class.getName());
        serverLogger.addAppender(console);
        serverLogger.setAdditive(false);


        // set appender on root and set level to debug
        root.addAppender(listApp);
        root.addAppender(appender);
        root.setAdditive(false);
        root.setLevel(Level.DEBUG);
        root.debug("This is a test message");
        Thread.sleep(100);
        final List<LogEvent> events = listApp.getEvents();
        assertNotNull("No event retrieved", events);
        assertTrue("No events retrieved", events.size() > 0);
        assertTrue("Incorrect event", events.get(0).getMessage().getFormattedMessage().equals("This is a test message"));
    }

    private class MessageFilter extends AbstractFilter {
        public MessageFilter(final Result onMatch, final Result onMismatch) {
            super(onMatch, onMismatch);
        }

        @Override
        public Result filter(final LogEvent event) {
            final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (final StackTraceElement element : stackTrace) {
                if (element.getMethodName().equals("onMessage")) {
                    return onMatch;
                } else if (element.getMethodName().equals("testServer")) {
                    return onMismatch;
                }
            }
            return onMismatch;
        }
    }
}
