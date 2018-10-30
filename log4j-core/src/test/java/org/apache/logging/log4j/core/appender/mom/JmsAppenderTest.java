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

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.MapMessage;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.categories.Appenders;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.junit.JndiRule;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.message.StringMapMessage;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;

@Category(Appenders.Jms.class)
public class JmsAppenderTest {

    private static final String CONNECTION_FACTORY_NAME = "jms/connectionFactory";
    private static final String QUEUE_FACTORY_NAME = "jms/queues";
    private static final String TOPIC_FACTORY_NAME = "jms/topics";
    private static final String DESTINATION_NAME = "jms/destination";
    private static final String DESTINATION_NAME_ML = "jms/destination-ml";
    private static final String QUEUE_NAME = "jms/queue";
    private static final String TOPIC_NAME = "jms/topic";
    private static final String LOG_MESSAGE = "Hello, world!";

    private final ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
    private final Connection connection = mock(Connection.class);
    private final Session session = mock(Session.class);
    private final Destination destination = mock(Destination.class);
    private final Destination destinationMl = mock(Destination.class);
    private final MessageProducer messageProducer = mock(MessageProducer.class);
    private final MessageProducer messageProducerMl = mock(MessageProducer.class);
    private final TextMessage textMessage = mock(TextMessage.class);
    private final ObjectMessage objectMessage = mock(ObjectMessage.class);
    private final MapMessage mapMessage = mock(MapMessage.class);

    private final JndiRule jndiRule = new JndiRule(createBindings());
    private final LoggerContextRule ctx = new LoggerContextRule("JmsAppenderTest.xml");

    @Rule
    public RuleChain rules = RuleChain.outerRule(jndiRule).around(ctx);

    public JmsAppenderTest() throws Exception {
        // this needs to set up before LoggerContextRule
        given(connectionFactory.createConnection()).willReturn(connection);
        given(connectionFactory.createConnection(anyString(), anyString())).willThrow(IllegalArgumentException.class);
        given(connection.createSession(eq(false), eq(Session.AUTO_ACKNOWLEDGE))).willReturn(session);
        given(session.createProducer(eq(destination))).willReturn(messageProducer);
        given(session.createProducer(eq(destinationMl))).willReturn(messageProducerMl);
        given(session.createTextMessage(anyString())).willReturn(textMessage);
        given(session.createObjectMessage(isA(Serializable.class))).willReturn(objectMessage);
        given(session.createMapMessage()).willReturn(mapMessage);
    }

    private Map<String, Object> createBindings() {
        final ConcurrentHashMap<String, Object> map = new ConcurrentHashMap<>();
        map.put(CONNECTION_FACTORY_NAME, connectionFactory);
        map.put(DESTINATION_NAME, destination);
        map.put(DESTINATION_NAME_ML, destinationMl);
        map.put(QUEUE_FACTORY_NAME, connectionFactory);
        map.put(QUEUE_NAME, destination);
        map.put(TOPIC_FACTORY_NAME, connectionFactory);
        map.put(TOPIC_NAME, destination);
        return map;
    }

    private  Log4jLogEvent createLogEvent() {
        return createLogEvent(new SimpleMessage(LOG_MESSAGE));
    }

    private Log4jLogEvent createLogEvent(final Message message) {
        // @formatter:off
        return Log4jLogEvent.newBuilder()
            .setLoggerName(JmsAppenderTest.class.getName())
            .setLoggerFqcn(JmsAppenderTest.class.getName())
            .setLevel(Level.INFO)
            .setMessage(message)
            .build();
        // @formatter:on
    }

    private Log4jLogEvent createMapMessageLogEvent() {
        final StringMapMessage mapMessage = new StringMapMessage();
        return createLogEvent(mapMessage.with("testMesage", LOG_MESSAGE));
    }

    @Before
    public void setUp() throws Exception {
        // we have 4 appenders all connecting to the same ConnectionFactory
        then(connection).should(times(4)).start();
    }

    @Test
    public void testAppendToQueue() throws Exception {
        final JmsAppender appender = (JmsAppender) ctx.getRequiredAppender("JmsAppender");
        final LogEvent event = createLogEvent();
        appender.append(event);
        then(session).should().createTextMessage(eq(LOG_MESSAGE));
        then(textMessage).should().setJMSTimestamp(anyLong());
        then(messageProducer).should().send(textMessage);
        appender.stop();
        then(session).should().close();
        then(connection).should().close();
    }

    @Test
    public void testAppendToQueueWithMessageLayout() throws Exception {
        final JmsAppender appender = (JmsAppender) ctx.getRequiredAppender("JmsAppender-MessageLayout");
        final LogEvent event = createMapMessageLogEvent();
        appender.append(event);
        then(session).should().createMapMessage();
        then(mapMessage).should().setJMSTimestamp(anyLong());
        then(messageProducerMl).should().send(mapMessage);
        appender.stop();
        then(session).should().close();
        then(connection).should().close();
    }

    @Test
    public void testJmsQueueAppenderCompatibility() throws Exception {
        final JmsAppender appender = (JmsAppender) ctx.getRequiredAppender("JmsQueueAppender");
        final LogEvent expected = createLogEvent();
        appender.append(expected);
        then(session).should().createObjectMessage(eq(expected));
        then(objectMessage).should().setJMSTimestamp(anyLong());
        then(messageProducer).should().send(objectMessage);
        appender.stop();
        then(session).should().close();
        then(connection).should().close();
    }

    @Test
    public void testJmsTopicAppenderCompatibility() throws Exception {
        final JmsAppender appender = (JmsAppender) ctx.getRequiredAppender("JmsTopicAppender");
        final LogEvent expected = createLogEvent();
        appender.append(expected);
        then(session).should().createObjectMessage(eq(expected));
        then(objectMessage).should().setJMSTimestamp(anyLong());
        then(messageProducer).should().send(objectMessage);
        appender.stop();
        then(session).should().close();
        then(connection).should().close();
    }

}