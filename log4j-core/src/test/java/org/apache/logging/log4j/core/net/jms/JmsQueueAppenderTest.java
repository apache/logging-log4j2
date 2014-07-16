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

import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.status.StatusStdOutListener;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockejb.jms.MockQueue;
import org.mockejb.jms.QueueConnectionFactoryImpl;
import org.mockejb.jndi.MockContextFactory;

import static org.junit.Assert.*;

/**
 *
 */
public class JmsQueueAppenderTest {

    private static final String FACTORY_NAME = "TestQueueConnectionFactory";
    private static final String QUEUE_NAME = "TestQueue";

    private static Context context;
    private static AbstractJmsReceiver receiver;

    private static final String CONFIG = "log4j-jmsqueue.xml";

    LoggerContext ctx = (LoggerContext) LogManager.getContext();
    Logger root = ctx.getLogger("JmsQueueTest");

    @BeforeClass
    public static void setupClass() throws Exception {
        // MockContextFactory becomes the primary JNDI provider
        final StatusStdOutListener listener = new StatusStdOutListener();
        listener.setStatusLevel(Level.ERROR);
        StatusLogger.getLogger().registerListener(listener);
        MockContextFactory.setAsInitial();
        context = new InitialContext();
        context.rebind(FACTORY_NAME, new QueueConnectionFactoryImpl());
        context.rebind(QUEUE_NAME, new MockQueue(QUEUE_NAME));
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
        receiver = new JmsQueueReceiver(FACTORY_NAME, QUEUE_NAME, null, null);
    }

    @AfterClass
    public static void cleanupClass() {
        StatusLogger.getLogger().reset();
    }

    @Test
    public void testConfiguration() throws Exception {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext();
        final Configuration config = ctx.getConfiguration();
        final Map<String, Appender> appenders = config.getAppenders();
        assertTrue(appenders.containsKey("JMSQueue"));
    }
}
