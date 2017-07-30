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
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.layout.MessageLayout;
import org.apache.logging.log4j.server.mom.activemq.ActiveMqBrokerServiceHelper;
import org.apache.logging.log4j.test.AvailablePortFinder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Tests that a JMS Appender start when there is no broker and connect the broker when it is started later..
 * <p>
 * LOG4J2-1934 JMS Appender does not know how to recover from a broken connection. See
 * https://issues.apache.org/jira/browse/LOG4J2-1934
 * </p>
 * <p>
 * This test class' single test method performs the following:
 * </p>
 * <ol>
 * <li>Starts a JMS Appender</li>
 * <li>Logs an event (fails and starts the reconnect thread)</li>
 * <li>Starts Apache ActiveMQ</li>
 * <li>Logs an event successfully</li>
 * </ol>
 */
@Category(Appenders.Jms.class)
public class JmsAppenderConnectPostStartupIT extends AbstractJmsAppenderReconnectIT {

    @Test
    public void testConnectPostStartup() throws Exception {
        //
        // Start appender
        final int port = AvailablePortFinder.getNextAvailable();
        final String brokerUrlString = "tcp://localhost:" + port;
        jmsClientTestConfig = new JmsClientTestConfig(ActiveMQInitialContextFactory.class.getName(), brokerUrlString,
                "admin", "admin".toCharArray());
        jmsClientTestConfig.start();
        appender = jmsClientTestConfig.createAppender(MessageLayout.createLayout());
        //
        // Logging will fail but the JMS manager is now running a reconnect thread.
        try {
            appendEvent(appender);
            Assert.fail("Expected to catch a " + AppenderLoggingException.class.getName());
        } catch (final AppenderLoggingException e) {
            // Expected.
        }
        //
        // Start broker
        brokerService = ActiveMqBrokerServiceHelper.startBrokerService(JmsAppenderConnectPostStartupIT.class.getName(),
                brokerUrlString, port);
        //
        // Logging now should just work
        Thread.sleep(appender.getManager().getJmsManagerConfiguration().getReconnectIntervalMillis());
        appendEvent(appender);
    }
}
