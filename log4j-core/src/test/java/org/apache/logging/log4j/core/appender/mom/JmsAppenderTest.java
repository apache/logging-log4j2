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

package org.apache.logging.log4j.core.appender.mom;

import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.util.JndiCloser;
import org.apache.logging.log4j.junit.InitialLoggerContext;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockejb.jms.MockQueue;
import org.mockejb.jms.MockTopic;
import org.mockejb.jms.QueueConnectionFactoryImpl;
import org.mockejb.jms.TopicConnectionFactoryImpl;
import org.mockejb.jndi.MockContextFactory;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

public class JmsAppenderTest {

    private static final String CONNECTION_FACTORY_NAME = "jms/queues";
    private static final String TOPIC_FACTORY_NAME = "jms/topics";
    private static final String DESTINATION_NAME = "jms/destination";
    private static final String QUEUE_NAME = "jms/queue";
    private static final String TOPIC_NAME = "jms/topic";
    private static final String LOG_MESSAGE = "Hello, world!";

    private static Context context;

    private static MockQueue destination;
    private static MockQueue queue;
    private static MockTopic topic;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MockContextFactory.setAsInitial();
        context = new InitialContext();
        context.rebind(CONNECTION_FACTORY_NAME, new QueueConnectionFactoryImpl());
        context.rebind(TOPIC_FACTORY_NAME, new TopicConnectionFactoryImpl());
        destination = new MockQueue(DESTINATION_NAME);
        context.rebind(DESTINATION_NAME, destination);
        queue = new MockQueue(QUEUE_NAME);
        context.rebind(QUEUE_NAME, queue);
        topic = new MockTopic(TOPIC_NAME);
        context.rebind(TOPIC_NAME, topic);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        JndiCloser.close(context);
    }

    @Rule
    public InitialLoggerContext ctx = new InitialLoggerContext("JmsAppenderTest.xml");

    @Test
    public void testAppendToQueue() throws Exception {
        assertEquals(0, destination.size());
        final JmsAppender appender = (JmsAppender) ctx.getRequiredAppender("JmsAppender");
        final LogEvent event = createLogEvent();
        appender.append(event);
        assertEquals(1, destination.size());
        final Message message = destination.getMessageAt(0);
        assertNotNull(message);
        assertThat(message, instanceOf(TextMessage.class));
        final TextMessage textMessage = (TextMessage) message;
        assertEquals(LOG_MESSAGE, textMessage.getText());
    }

    @Test
    public void testJmsQueueAppenderCompatibility() throws Exception {
        assertEquals(0, queue.size());
        final JmsAppender appender = (JmsAppender) ctx.getRequiredAppender("JmsQueueAppender");
        final LogEvent expected = createLogEvent();
        appender.append(expected);
        assertEquals(1, queue.size());
        final Message message = queue.getMessageAt(0);
        assertNotNull(message);
        assertThat(message, instanceOf(ObjectMessage.class));
        final ObjectMessage objectMessage = (ObjectMessage) message;
        final Object o = objectMessage.getObject();
        assertThat(o, instanceOf(LogEvent.class));
        final LogEvent actual = (LogEvent) o;
        assertEquals(expected.getMessage().getFormattedMessage(), actual.getMessage().getFormattedMessage());
    }

    @Test
    public void testJmsTopicAppenderCompatibility() throws Exception {
        assertEquals(0, topic.size());
        final JmsAppender appender = (JmsAppender) ctx.getRequiredAppender("JmsTopicAppender");
        final LogEvent expected = createLogEvent();
        appender.append(expected);
        assertEquals(1, topic.size());
        final Message message = topic.getMessageAt(0);
        assertNotNull(message);
        assertThat(message, instanceOf(ObjectMessage.class));
        final ObjectMessage objectMessage = (ObjectMessage) message;
        final Object o = objectMessage.getObject();
        assertThat(o, instanceOf(LogEvent.class));
        final LogEvent actual = (LogEvent) o;
        assertEquals(expected.getMessage().getFormattedMessage(), actual.getMessage().getFormattedMessage());
    }

    private static Log4jLogEvent createLogEvent() {
        return Log4jLogEvent.newBuilder()
            .setLoggerName(JmsAppenderTest.class.getName())
            .setLoggerFqcn(JmsAppenderTest.class.getName())
            .setLevel(Level.INFO)
            .setMessage(new SimpleMessage(LOG_MESSAGE))
            .build();
    }

}