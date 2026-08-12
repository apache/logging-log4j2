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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.net.JndiManager;
import org.apache.logging.log4j.core.test.categories.Appenders;
import org.apache.logging.log4j.core.test.junit.JndiRule;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(Appenders.Jms.class)
public class JmsManagerTest {

    private static final String LOG_MESSAGE = "Hello from JmsManagerTest";
    private static final Properties EMPTY_JNDI_PROPERTIES = new Properties();

    private final JmsMockFixtures fixtures = JmsMockFixtures.create();

    @Rule
    public final JndiRule jndiRule = new JndiRule(fixtures.createBindings());

    public JmsManagerTest() throws JMSException {}

    @BeforeClass
    public static void enableJndiJms() {
        System.setProperty("log4j2.enableJndiJms", "true");
    }

    @AfterClass
    public static void disableJndiJms() {
        System.clearProperty("log4j2.enableJndiJms");
    }

    @Test
    public void testGetJmsManagerFailsWhenJndiJmsDisabled() {
        System.clearProperty("log4j2.enableJndiJms");
        try {
            assertThrows(
                    IllegalStateException.class,
                    () -> createManager(
                            "disabled-jndi",
                            JmsMockFixtures.CONNECTION_FACTORY_NAME,
                            JmsMockFixtures.QUEUE_NAME,
                            null,
                            null,
                            false,
                            0));
        } finally {
            System.setProperty("log4j2.enableJndiJms", "true");
        }
    }

    @Test
    public void testConnectionLifecycle() throws Exception {
        final JmsManager manager =
                createManager("lifecycle", JmsMockFixtures.CONNECTION_FACTORY_NAME, JmsMockFixtures.QUEUE_NAME);
        assertNotNull(manager);

        then(fixtures.getConnection()).should().start();
        manager.stop(0, TimeUnit.MILLISECONDS);

        then(fixtures.getQueueProducer()).should().close();
        then(fixtures.getSession()).should().close();
        then(fixtures.getConnection()).should().close();
    }

    @Test
    public void testSendToQueueDestination() throws Exception {
        final JmsManager manager =
                createManager("send-queue", JmsMockFixtures.CONNECTION_FACTORY_NAME, JmsMockFixtures.QUEUE_NAME);
        manager.send(createLogEvent(), LOG_MESSAGE);

        then(fixtures.getSession()).should().createTextMessage(eq(LOG_MESSAGE));
        then(fixtures.getTextMessage()).should().setJMSTimestamp(anyLong());
        then(fixtures.getQueueProducer()).should().send(fixtures.getTextMessage());
        manager.stop(0, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testSendToTopicDestination() throws Exception {
        final JmsManager manager =
                createManager("send-topic", JmsMockFixtures.CONNECTION_FACTORY_NAME, JmsMockFixtures.TOPIC_NAME);
        manager.send(createLogEvent(), LOG_MESSAGE);

        then(fixtures.getSession()).should().createTextMessage(eq(LOG_MESSAGE));
        then(fixtures.getTextMessage()).should().setJMSTimestamp(anyLong());
        then(fixtures.getTopicProducer()).should().send(fixtures.getTextMessage());
        manager.stop(0, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testAuthenticatedConnectionUsesCredentials() throws Exception {
        final JmsManager manager = createManager(
                "authenticated",
                JmsMockFixtures.CONNECTION_FACTORY_NAME,
                JmsMockFixtures.QUEUE_NAME,
                "jms-user",
                "jms-pass".toCharArray(),
                false,
                0);
        assertNotNull(manager);

        then(fixtures.getConnectionFactory()).should().createConnection(eq("jms-user"), eq("jms-pass"));
        manager.stop(0, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testReconnectAfterSendFailure() throws Exception {
        willThrow(new JMSException("broken connection"))
                .willAnswer(invocation -> null)
                .given(fixtures.getQueueProducer())
                .send(any(Message.class));

        final JmsManager manager = createManager(
                "reconnect-send",
                JmsMockFixtures.CONNECTION_FACTORY_NAME,
                JmsMockFixtures.QUEUE_NAME,
                null,
                null,
                false,
                5000);
        manager.send(createLogEvent(), LOG_MESSAGE);

        then(fixtures.getQueueProducer()).should(times(2)).send(fixtures.getTextMessage());
        then(fixtures.getConnection()).should(times(2)).start();
        manager.stop(0, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testSendFailureWithoutRetryDoesNotReconnect() throws Exception {
        willThrow(new JMSException("broken connection"))
                .given(fixtures.getQueueProducer())
                .send(any(Message.class));

        final JmsManager manager = createManager(
                "no-retry", JmsMockFixtures.CONNECTION_FACTORY_NAME, JmsMockFixtures.QUEUE_NAME, null, null, false, 0);
        manager.send(createLogEvent(), LOG_MESSAGE);

        then(fixtures.getQueueProducer()).should(times(1)).send(any(Message.class));
        manager.stop(0, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testInitialConnectionFailureStartsReconnectorAndSendWaits() throws Exception {
        given(fixtures.getConnectionFactory().createConnection())
                .willThrow(new JMSException("broker unavailable"))
                .willReturn(fixtures.getConnection());

        final JmsManager manager = createManager(
                "initial-failure",
                JmsMockFixtures.CONNECTION_FACTORY_NAME,
                JmsMockFixtures.QUEUE_NAME,
                null,
                null,
                false,
                10);
        assertNotNull(manager);

        manager.send(createLogEvent(), LOG_MESSAGE);

        then(fixtures.getQueueProducer()).should().send(fixtures.getTextMessage());
        manager.stop(0, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testImmediateFailWhenProducerUnavailableThrowsNullPointerException() throws Exception {
        given(fixtures.getConnectionFactory().createConnection()).willThrow(new JMSException("broker unavailable"));

        final JmsManager manager = createManager(
                "immediate-fail",
                JmsMockFixtures.CONNECTION_FACTORY_NAME,
                JmsMockFixtures.QUEUE_NAME,
                null,
                null,
                true,
                0);
        assertNotNull(manager);

        assertThrows(NullPointerException.class, () -> manager.send(createLogEvent(), LOG_MESSAGE));
        manager.stop(0, TimeUnit.MILLISECONDS);
    }

    private static JmsManager createManager(
            final String name, final String connectionFactoryName, final String destinationName) {
        return createManager(connectionFactoryName, destinationName, null, null, false, 0, name);
    }

    private static JmsManager createManager(
            final String name,
            final String connectionFactoryName,
            final String destinationName,
            final String userName,
            final char[] password,
            final boolean immediateFail,
            final long reconnectIntervalMillis) {
        return createManager(
                connectionFactoryName,
                destinationName,
                userName,
                password,
                immediateFail,
                reconnectIntervalMillis,
                name);
    }

    private static JmsManager createManager(
            final String connectionFactoryName,
            final String destinationName,
            final String userName,
            final char[] password,
            final boolean immediateFail,
            final long reconnectIntervalMillis,
            final String name) {
        return JmsManager.getJmsManager(
                JmsManagerTest.class.getName() + ':' + name,
                JndiManager.createProperties(null, null, null, null, null, EMPTY_JNDI_PROPERTIES),
                connectionFactoryName,
                destinationName,
                userName,
                password,
                immediateFail,
                reconnectIntervalMillis);
    }

    private LogEvent createLogEvent() {
        return Log4jLogEvent.newBuilder()
                .setLoggerName(JmsManagerTest.class.getName())
                .setLoggerFqcn(JmsManagerTest.class.getName())
                .setLevel(Level.INFO)
                .setMessage(new SimpleMessage(LOG_MESSAGE))
                .build();
    }
}
