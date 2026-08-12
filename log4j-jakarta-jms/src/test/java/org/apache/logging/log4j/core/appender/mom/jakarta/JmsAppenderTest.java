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

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import jakarta.jms.JMSException;
import java.io.Serializable;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.test.categories.Appenders;
import org.apache.logging.log4j.core.test.junit.JndiRule;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.message.StringMapMessage;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;

@Category(Appenders.Jms.class)
public class JmsAppenderTest {

    private static final String LOG_MESSAGE = "Hello, world!";

    private final JmsMockFixtures fixtures = JmsMockFixtures.create();

    private final JndiRule jndiRule = new JndiRule(fixtures.createBindings());
    private final LoggerContextRule ctx = new LoggerContextRule("JmsJakartaAppenderTest.xml");

    @Rule
    public RuleChain rules = RuleChain.outerRule(jndiRule).around(ctx);

    public JmsAppenderTest() throws JMSException {}

    @AfterClass
    public static void afterClass() {
        System.clearProperty("log4j2.enableJndiJms");
    }

    @BeforeClass
    public static void beforeClass() {
        System.setProperty("log4j2.enableJndiJms", "true");
    }

    @Before
    public void setUp() throws Exception {
        // four appenders connect through the same mocked ConnectionFactory
        then(fixtures.getConnection()).should(times(4)).start();
    }

    private Log4jLogEvent createLogEvent() {
        return createLogEvent(new SimpleMessage(LOG_MESSAGE));
    }

    private Log4jLogEvent createLogEvent(final Message message) {
        return Log4jLogEvent.newBuilder()
                .setLoggerName(JmsAppenderTest.class.getName())
                .setLoggerFqcn(JmsAppenderTest.class.getName())
                .setLevel(Level.INFO)
                .setMessage(message)
                .build();
    }

    private Log4jLogEvent createMapMessageLogEvent() {
        final StringMapMessage mapMessage = new StringMapMessage();
        return createLogEvent(mapMessage.with("testMesage", LOG_MESSAGE));
    }

    @Test
    public void testAppendToQueue() throws Exception {
        final JmsAppender appender = (JmsAppender) ctx.getRequiredAppender("JmsAppender");
        final LogEvent event = createLogEvent();
        appender.append(event);
        then(fixtures.getSession()).should().createTextMessage(eq(LOG_MESSAGE));
        then(fixtures.getTextMessage()).should().setJMSTimestamp(anyLong());
        then(fixtures.getMessageProducer()).should().send(fixtures.getTextMessage());
        appender.stop();
        then(fixtures.getSession()).should().close();
        then(fixtures.getConnection()).should().close();
    }

    @Test
    public void testAppendToQueueWithMessageLayout() throws Exception {
        final JmsAppender appender = (JmsAppender) ctx.getRequiredAppender("JmsAppender-MessageLayout");
        final LogEvent event = createMapMessageLogEvent();
        appender.append(event);
        then(fixtures.getSession()).should().createMapMessage();
        then(fixtures.getMapMessage()).should().setJMSTimestamp(anyLong());
        then(fixtures.getMessageProducerMl()).should().send(fixtures.getMapMessage());
        appender.stop();
        then(fixtures.getSession()).should().close();
        then(fixtures.getConnection()).should().close();
    }

    @Test
    public void testJmsQueueAppenderCompatibility() throws Exception {
        final JmsAppender appender = (JmsAppender) ctx.getRequiredAppender("JmsQueueAppender");
        final LogEvent expected = createLogEvent();
        appender.append(expected);
        then(fixtures.getSession()).should().createObjectMessage(isA(Serializable.class));
        then(fixtures.getObjectMessage()).should().setJMSTimestamp(anyLong());
        then(fixtures.getQueueProducer()).should().send(fixtures.getObjectMessage());
        appender.stop();
        then(fixtures.getSession()).should().close();
        then(fixtures.getConnection()).should().close();
    }

    @Test
    public void testJmsTopicAppenderCompatibility() throws Exception {
        final JmsAppender appender = (JmsAppender) ctx.getRequiredAppender("JmsTopicAppender");
        final LogEvent expected = createLogEvent();
        appender.append(expected);
        then(fixtures.getSession()).should().createObjectMessage(isA(Serializable.class));
        then(fixtures.getObjectMessage()).should().setJMSTimestamp(anyLong());
        then(fixtures.getTopicProducer()).should().send(fixtures.getObjectMessage());
        appender.stop();
        then(fixtures.getSession()).should().close();
        then(fixtures.getConnection()).should().close();
    }
}
