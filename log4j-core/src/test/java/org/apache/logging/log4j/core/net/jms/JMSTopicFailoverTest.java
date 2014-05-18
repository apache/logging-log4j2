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
package org.apache.logging.log4j.core.net.jms;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.status.StatusConsoleListener;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockejb.jms.MockTopic;
import org.mockejb.jms.TopicConnectionFactoryImpl;
import org.mockejb.jndi.MockContextFactory;

/**
 *
 */
public class JMSTopicFailoverTest {

    private static final String FACTORY_NAME = "TopicConnectionFactory";
    private static final String TOPIC_NAME = "Log4j2Topic";

    private static Context context;

    private static final String CONFIG = "log4j-jmstopic-failover.xml";

    private static Configuration config;
    private static ListAppender app;
    private static LoggerContext ctx;

    @BeforeClass
    public static void setupClass() throws Exception {
        setupQueue();
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
        ctx = (LoggerContext) LogManager.getContext(false);
    }

    @AfterClass
    public static void cleanupClass() {
        System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        ctx.reconfigure();
        StatusLogger.getLogger().reset();
    }

    @Before
    public void before() {
        config = ctx.getConfiguration();
        for (final Map.Entry<String, Appender> entry : config.getAppenders().entrySet()) {
            if (entry.getKey().equals("List")) {
                app = (ListAppender) entry.getValue();
                break;
            }
        }
        assertNotNull("No Appender", app);
        app.clear();
        ThreadContext.clearMap();
    }

    private static void setupQueue() throws Exception {
        // MockContextFactory becomes the primary JNDI provider
        final StatusConsoleListener l = new StatusConsoleListener(Level.ERROR);
        StatusLogger.getLogger().registerListener(l);
        MockContextFactory.setAsInitial();
        context = new InitialContext();
        context.rebind(FACTORY_NAME, new TopicConnectionFactoryImpl() );
        //context.rebind(QUEUE_NAME, new MockQueue(QUEUE_NAME));
        //System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
        //receiver = new JMSQueueReceiver(FACTORY_NAME, QUEUE_NAME, null, null);
    }

    @Test
    public void testFailover() throws Exception {
        ThreadContext.put("appender", "Failover");
        final Logger logger = LogManager.getLogger(JMSTopicFailoverTest.class);
        logger.debug("Test Message");
        final List<LogEvent> events = app.getEvents();
        assertNotNull("No events returned", events);
        assertTrue("No events returned", events.size() > 0);
        assertTrue("Incorrect event", "Test Message".equals(events.get(0).getMessage().getFormattedMessage()));
    }

    @Test
    public void testReconnect() throws Exception {
        context.rebind(TOPIC_NAME, new MockTopic(TOPIC_NAME));
        final AbstractJMSReceiver receiver = new JMSTopicReceiver(FACTORY_NAME, TOPIC_NAME, null, null);
        ThreadContext.put("appender", "Failover");
        final Logger logger = LogManager.getLogger(JMSTopicFailoverTest.class);
        logger.debug("Test Message");
        final List<LogEvent> events = app.getEvents();
        assertNotNull("No events returned", events);
        assertTrue("No events returned", events.size() > 0);
        assertTrue("Incorrect event", "Test Message".equals(events.get(0).getMessage().getFormattedMessage()));
    }
}
