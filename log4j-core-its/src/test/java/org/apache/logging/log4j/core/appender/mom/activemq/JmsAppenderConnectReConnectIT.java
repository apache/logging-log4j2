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

import org.apache.activemq.jndi.ActiveMQInitialContextFactory;
import org.apache.logging.log4j.categories.Appenders;
import org.apache.logging.log4j.core.layout.MessageLayout;
import org.apache.logging.log4j.test.AvailablePortFinder;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Tests that a JMS Appender can reconnect to a JMS broker after it has been recycled.
 * <p>
 * LOG4J2-1934 JMS Appender does not know how to recover from a broken connection. See
 * https://issues.apache.org/jira/browse/LOG4J2-1934
 * </p>
 * <p>
 * This test class' single test method performs the following:
 * </p>
 * <ol>
 * <li>Starts Apache ActiveMQ</li>
 * <li>Starts a JMS Appender</li>
 * <li>Logs an event</li>
 * <li>Stops Apache ActiveMQ</li>
 * <li>Starts Apache ActiveMQ</li>
 * <li>Logs an event</li>
 * </ol>
 */
@Category(Appenders.Jms.class)
public class JmsAppenderConnectReConnectIT extends AbstractJmsAppenderReconnectIT {

    @Test
    public void testConnectReConnect() throws Exception {
        // Start broker
        final int port = AvailablePortFinder.getNextAvailable();
        final String brokerUrlString = "tcp://localhost:" + port;
        brokerService = ActiveMqBrokerServiceHelper.startBrokerService(JmsAppenderConnectReConnectIT.class.getName(),
                brokerUrlString, port);
        // Start appender
        jmsClientTestConfig = new JmsClientTestConfig(ActiveMQInitialContextFactory.class.getName(), brokerUrlString,
                "admin", "admin".toCharArray());
        jmsClientTestConfig.start();
        appender = jmsClientTestConfig.createAppender(MessageLayout.createLayout());
        // Log message
        appendEvent(appender);
        // Stop broker
        ActiveMqBrokerServiceHelper.stopBrokerService(brokerService);
        // Restart broker
        brokerService = ActiveMqBrokerServiceHelper.startBrokerService(JmsAppenderConnectReConnectIT.class.getName(),
                brokerUrlString, port);
        // Logging again should cause the appender to reconnect
        appendEvent(appender);
    }

}
