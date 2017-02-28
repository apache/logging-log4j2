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

package org.apache.logging.log4j.core.appender.mom.activemq;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.apache.activemq.jndi.ActiveMQInitialContextFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.categories.Appenders;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.mom.JmsAppender;
import org.apache.logging.log4j.core.appender.mom.JmsManager;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.SerializedLayout;
import org.apache.logging.log4j.core.net.JndiManager;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.*;

/**
 * Integration test for JmsAppender using an embedded ActiveMQ broker.
 */
@Category(Appenders.Jms.class)
public class JmsAppenderIT {

    private static final String KEY_SERIALIZABLE_PACKAGES = "org.apache.activemq.SERIALIZABLE_PACKAGES";

    private static JmsManager jmsManager;

    private JmsAppender appender;

    @BeforeClass
    public static void setUpClass() {
        System.setProperty(KEY_SERIALIZABLE_PACKAGES,
                "org.apache.logging.log4j.core.impl,org.apache.logging.log4j.util,org.apache.logging.log4j");
        final Properties additional = new Properties();
        additional.setProperty("queue.TestQueue", "TestQueue");
        final JndiManager jndiManager = JndiManager.getJndiManager(ActiveMQInitialContextFactory.class.getName(),
            "vm://localhost?broker.persistent=false", null, null, null, additional);
        jmsManager = JmsManager.getJmsManager("JmsManager", jndiManager, "ConnectionFactory", "TestQueue", null, null);
    }

    @AfterClass
    public static void tearDownClass() {
        jmsManager.close();
        System.getProperties().remove(KEY_SERIALIZABLE_PACKAGES);
    }

    @Before
    public void setUp() throws Exception {
        // @formatter:off
        appender = JmsAppender.newBuilder().
            setName("JmsAppender").
            setLayout(SerializedLayout.createLayout()).
            setIgnoreExceptions(true).
            setJmsManager(jmsManager).
            build();
        // @formatter:off
        appender.start();
    }

    @Test
    public void testLogToQueue() throws Exception {
        final int messageCount = 100;
        final MessageConsumer messageConsumer = jmsManager.createMessageConsumer();
        final JmsQueueConsumer consumer = new JmsQueueConsumer(messageCount);
        messageConsumer.setMessageListener(consumer);
        final String messageText = "Hello, World!";
        final String loggerName = this.getClass().getName();
        for (int i = 0; i < messageCount; i++) {
            final LogEvent event = Log4jLogEvent.newBuilder().setLoggerName(loggerName) //
                    .setLoggerFqcn(loggerName).setLevel(Level.INFO) //
                    .setMessage(new SimpleMessage(messageText)).setThreadName(Thread.currentThread().getName()) //
                    .setTimeMillis(System.currentTimeMillis()).build();
            appender.append(event);
        }
        consumer.awaitAndAssertAllMessagesConsumed();
    }

    private static class JmsQueueConsumer implements MessageListener {

        private final int messageCount;
        private final CountDownLatch countDownLatch;
        private final Collection<LogEvent> events;

        private JmsQueueConsumer(final int messageCount) {
            this.messageCount = messageCount;
            this.countDownLatch = new CountDownLatch(messageCount);
            this.events = new ArrayList<>(messageCount);
        }

        @Override
        public void onMessage(final Message message) {
            try {
                consume((ObjectMessage) message);
            } catch (final JMSException e) {
                e.printStackTrace();
            }
        }

        private void consume(final ObjectMessage message) throws JMSException {
            try {
                final LogEvent event = (LogEvent) message.getObject();
                events.add(event);
            } finally {
                countDownLatch.countDown();
            }
        }

        public void awaitAndAssertAllMessagesConsumed() throws InterruptedException {
            countDownLatch.await(5, TimeUnit.SECONDS);
            assertEquals(messageCount, events.size());
        }
    }
}
