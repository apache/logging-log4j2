/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.appender.mom.jakarta;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.MapMessage;
import jakarta.jms.MessageProducer;
import jakarta.jms.ObjectMessage;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reusable Mockito fixtures for jakarta.jms types used by JMS appender tests.
 */
public final class JmsMockFixtures {

    public static final String CONNECTION_FACTORY_NAME = "jms/connectionFactory";
    public static final String QUEUE_FACTORY_NAME = "jms/queues";
    public static final String TOPIC_FACTORY_NAME = "jms/topics";
    public static final String DESTINATION_NAME = "jms/destination";
    public static final String DESTINATION_NAME_ML = "jms/destination-ml";
    public static final String QUEUE_NAME = "jms/queue";
    public static final String TOPIC_NAME = "jms/topic";

    private final ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
    private final Connection connection = mock(Connection.class);
    private final Session session = mock(Session.class);
    private final Destination destination = mock(Destination.class);
    private final Destination destinationMl = mock(Destination.class);
    private final Queue queue = mock(Queue.class);
    private final Topic topic = mock(Topic.class);
    private final MessageProducer messageProducer = mock(MessageProducer.class);
    private final MessageProducer messageProducerMl = mock(MessageProducer.class);
    private final MessageProducer queueProducer = mock(MessageProducer.class);
    private final MessageProducer topicProducer = mock(MessageProducer.class);
    private final TextMessage textMessage = mock(TextMessage.class);
    private final ObjectMessage objectMessage = mock(ObjectMessage.class);
    private final MapMessage mapMessage = mock(MapMessage.class);

    private JmsMockFixtures() throws JMSException {
        configureSuccessfulConnection();
    }

    public static JmsMockFixtures create() throws JMSException {
        return new JmsMockFixtures();
    }

    private void configureSuccessfulConnection() throws JMSException {
        given(connectionFactory.createConnection()).willReturn(connection);
        given(connectionFactory.createConnection(anyString(), anyString())).willReturn(connection);
        given(connection.createSession(eq(false), eq(Session.AUTO_ACKNOWLEDGE))).willReturn(session);
        given(session.createProducer(eq(destination))).willReturn(messageProducer);
        given(session.createProducer(eq(destinationMl))).willReturn(messageProducerMl);
        given(session.createProducer(eq(queue))).willReturn(queueProducer);
        given(session.createProducer(eq(topic))).willReturn(topicProducer);
        given(session.createTextMessage(anyString())).willReturn(textMessage);
        given(session.createObjectMessage(isA(Serializable.class))).willReturn(objectMessage);
        given(session.createMapMessage()).willReturn(mapMessage);
    }

    public Map<String, Object> createBindings() {
        final Map<String, Object> map = new ConcurrentHashMap<>();
        map.put(CONNECTION_FACTORY_NAME, connectionFactory);
        map.put(DESTINATION_NAME, destination);
        map.put(DESTINATION_NAME_ML, destinationMl);
        map.put(QUEUE_FACTORY_NAME, connectionFactory);
        map.put(QUEUE_NAME, queue);
        map.put(TOPIC_FACTORY_NAME, connectionFactory);
        map.put(TOPIC_NAME, topic);
        return map;
    }

    public Map<String, Object> queueOnlyBindings() {
        final Map<String, Object> map = new HashMap<>();
        map.put(CONNECTION_FACTORY_NAME, connectionFactory);
        map.put(QUEUE_NAME, queue);
        return map;
    }

    public Map<String, Object> topicOnlyBindings() {
        final Map<String, Object> map = new HashMap<>();
        map.put(CONNECTION_FACTORY_NAME, connectionFactory);
        map.put(TOPIC_NAME, topic);
        return map;
    }

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public Connection getConnection() {
        return connection;
    }

    public Session getSession() {
        return session;
    }

    public Destination getDestination() {
        return destination;
    }

    public Destination getDestinationMl() {
        return destinationMl;
    }

    public Queue getQueue() {
        return queue;
    }

    public Topic getTopic() {
        return topic;
    }

    public MessageProducer getMessageProducer() {
        return messageProducer;
    }

    public MessageProducer getMessageProducerMl() {
        return messageProducerMl;
    }

    public MessageProducer getQueueProducer() {
        return queueProducer;
    }

    public MessageProducer getTopicProducer() {
        return topicProducer;
    }

    public TextMessage getTextMessage() {
        return textMessage;
    }

    public ObjectMessage getObjectMessage() {
        return objectMessage;
    }

    public MapMessage getMapMessage() {
        return mapMessage;
    }
}
