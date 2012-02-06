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
import org.apache.logging.log4j.core.appender.JMSTopicAppender;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.apache.logging.log4j.core.filter.CompositeFilter;
import org.apache.logging.log4j.core.filter.FilterBase;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.status.StatusConsoleListener;
import org.apache.logging.log4j.status.StatusLogger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.mockejb.jms.MockTopic;
import org.mockejb.jms.TopicConnectionFactoryImpl;
import org.mockejb.jndi.MockContextFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class JMSTopicTest {

    private static final String FACTORY_NAME = "TestTopicConnectionFactory";
    private static final String TOPIC_NAME = "TestTopic";

    private static Context context;
    private static AbstractJMSReceiver receiver;

    LoggerContext ctx = (LoggerContext) LogManager.getContext();
    Logger root = ctx.getLogger("JMSTopicTest");

    @BeforeClass
    public static void setupClass() throws Exception {
        // MockContextFactory becomes the primary JNDI provider
        StatusConsoleListener l = new StatusConsoleListener(Level.ERROR);
        StatusLogger.getLogger().registerListener(l);
        MockContextFactory.setAsInitial();
        context = new InitialContext();
        context.rebind(FACTORY_NAME, new TopicConnectionFactoryImpl() );
        context.rebind(TOPIC_NAME, new MockTopic(TOPIC_NAME) );
        ((LoggerContext) LogManager.getContext()).reconfigure();
        receiver = new JMSTopicReceiver(FACTORY_NAME, TOPIC_NAME, null, null);
    }

    @AfterClass
    public static void cleanupClass() {
        StatusLogger.getLogger().reset();
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
        Filter clientFilter = new MessageFilter(Filter.Result.NEUTRAL, Filter.Result.DENY);
        Filter serverFilter = new MessageFilter(Filter.Result.DENY, Filter.Result.NEUTRAL);
        CompositeFilter clientFilters = CompositeFilter.createFilters(new Filter[]{clientFilter});
        JMSTopicAppender appender = JMSTopicAppender.createAppender(null, null, null, null, null, FACTORY_NAME,
                TOPIC_NAME, null, null, null, clientFilters, "true");
        appender.start();
        CompositeFilter serverFilters = CompositeFilter.createFilters(new Filter[]{serverFilter});
        ListAppender listApp = new ListAppender("Events", serverFilters, null, false, false);
        listApp.start();
        PatternLayout layout = PatternLayout.createLayout("%m %ex%n", null, null, null);
        ConsoleAppender console = ConsoleAppender.createAppender(layout, null, "SYSTEM_OUT", "Console", "true");
        console.start();
        Logger serverLogger = ctx.getLogger(JMSTopicReceiver.class.getName());
        serverLogger.addAppender(console);
        serverLogger.setAdditive(false);


        // set appender on root and set level to debug
        root.addAppender(listApp);
        root.addAppender(appender);
        root.setAdditive(false);
        root.setLevel(Level.DEBUG);
        root.debug("This is a test message");
        Thread.sleep(100);
        List<LogEvent> events = listApp.getEvents();
        assertNotNull("No event retrieved", events);
        assertTrue("No events retrieved", events.size() > 0);
        assertTrue("Incorrect event", events.get(0).getMessage().getFormattedMessage().equals("This is a test message"));
    }

    private class MessageFilter extends FilterBase {
        public MessageFilter(Result onMatch, Result onMismatch) {
            super(onMatch, onMismatch);
        }

        public Result filter(LogEvent event) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement element : stackTrace) {
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
