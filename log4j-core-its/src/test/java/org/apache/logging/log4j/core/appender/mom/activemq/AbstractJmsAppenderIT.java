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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.mom.JmsAppender;
import org.apache.logging.log4j.core.appender.mom.JmsManager;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.MessageLayout;
import org.apache.logging.log4j.core.layout.SerializedLayout;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.message.StringMapMessage;
import org.junit.Assert;
import org.junit.Test;

/**
 * Abstracts services for integration test for the JmsAppender using an embedded
 * ActiveMQ broker. The client (appender) is set up once for the whole class.
 * Subclasses decide whether the broker is reinitialized for each test method.
 * This allows to test the ability of the JmsAppender to reconnect.
 *
 * The subclasses cannot be run in parallel.
 */
public abstract class AbstractJmsAppenderIT {

    static class JmsQueueConsumer implements MessageListener {

        private final CountDownLatch countDownLatch;
        private final Collection<Object> events;
        private final Class<? extends Message> messageClass;
        private final int messageCount;

        JmsQueueConsumer(final int messageCount, final Class<? extends Message> messageClass) {
            this.messageCount = messageCount;
            this.messageClass = messageClass;
            this.countDownLatch = new CountDownLatch(messageCount);
            this.events = new ArrayList<>(messageCount);
        }

        public void awaitAndAssertAllMessagesConsumed() throws InterruptedException {
            countDownLatch.await(5, TimeUnit.SECONDS);
            assertEquals(messageCount, events.size());
        }

        @Override
        public void onMessage(final Message message) {
            try {
                try {
                    final Object event;
                    Assert.assertTrue(String.format("Expected type '%s' to be an instance of %s", message.getClass(),
                            messageClass), messageClass.isAssignableFrom(message.getClass()));
                    if (message instanceof ObjectMessage) {
                        event = ((ObjectMessage) message).getObject();
                    } else if (message instanceof javax.jms.MapMessage) {
                        event = message;
                    } else {
                        Assert.fail("Unexpected Message type: " + message);
                        event = null;
                    }
                    events.add(event);
                } finally {
                    countDownLatch.countDown();
                }
            } catch (final JMSException e) {
                e.printStackTrace();
            }
        }
    }

    static final String KEY_SERIALIZABLE_PACKAGES = "org.apache.activemq.SERIALIZABLE_PACKAGES";

    private final JmsClientTestConfigRule jmsTestConfigRule;

    public AbstractJmsAppenderIT(final JmsClientTestConfigRule jmsTestConfigRule) {
        this.jmsTestConfigRule = jmsTestConfigRule;
    }

    protected JmsAppender getJmsAppender() {
        return getJmsTestConfig().getJmsAppender();
    }

    protected JmsManager getJmsManager() {
        return getJmsTestConfig().getJmsManager();
    }

    private JmsClientTestConfig getJmsTestConfig() {
        return jmsTestConfigRule.getJmsClientTestConfig();
    }

    @Test
    public void testLogMapMessageToQueue() throws Exception {
        getJmsTestConfig().createAppender(MessageLayout.createLayout());
        final int messageCount = 100;
        final MessageConsumer messageConsumer = getJmsManager().createMessageConsumer();
        try {
            final JmsQueueConsumer consumer = new JmsQueueConsumer(messageCount, javax.jms.MapMessage.class);
            messageConsumer.setMessageListener(consumer);
            final String messageText = "Hello, World!";
            final String loggerName = this.getClass().getName();
            for (int i = 0; i < messageCount; i++) {
                final Map<String, String> map = new HashMap<>();
                map.put("messageText", messageText);
                map.put("threadName", Thread.currentThread().getName());
            // @formatter:off
			final LogEvent event = Log4jLogEvent.newBuilder()
					.setLoggerName(loggerName)
					.setLoggerFqcn(loggerName)
					.setLevel(Level.INFO)
					.setMessage(new StringMapMessage(map))
					.setTimeMillis(System.currentTimeMillis())
					.build();
			// @formatter:on
                getJmsAppender().append(event);
            }
            consumer.awaitAndAssertAllMessagesConsumed();
        } finally {
            messageConsumer.close();
        }
    }

    @Test
    public void testLogObjectMessageToQueue() throws Exception {
        getJmsTestConfig().createAppender(SerializedLayout.createLayout());
        final int messageCount = 100;
        final MessageConsumer messageConsumer = getJmsManager().createMessageConsumer();
        try {
            final JmsQueueConsumer consumer = new JmsQueueConsumer(messageCount, ObjectMessage.class);
            messageConsumer.setMessageListener(consumer);
            final String messageText = "Hello, World!";
            final String loggerName = this.getClass().getName();
            for (int i = 0; i < messageCount; i++) {
                // @formatter:off
				final LogEvent event = Log4jLogEvent.newBuilder()
					.setLoggerName(loggerName)
					.setLoggerFqcn(loggerName)
					.setLevel(Level.INFO)
					.setMessage(new SimpleMessage(messageText))
					.setThreadName(Thread.currentThread().getName())
					.setTimeMillis(System.currentTimeMillis())
					.build();
				// @formatter:on
                getJmsAppender().append(event);
            }
            consumer.awaitAndAssertAllMessagesConsumed();
        } finally {
            messageConsumer.close();
        }
    }
}
