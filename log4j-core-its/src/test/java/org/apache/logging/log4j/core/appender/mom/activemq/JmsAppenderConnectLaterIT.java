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

import java.util.HashMap;
import java.util.Map;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.jndi.ActiveMQInitialContextFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.categories.Appenders;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.mom.JmsAppender;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.MessageLayout;
import org.apache.logging.log4j.message.StringMapMessage;
import org.apache.logging.log4j.test.AvailablePortFinder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Tests that a JMS Appender can reconnect to a JMS broker after it has been
 * recycled.
 * <p>
 * LOG4J2-1934 JMS Appender does not know how to recover from a broken
 * connection. See https://issues.apache.org/jira/browse/LOG4J2-1934
 * </p>
 */
@Ignore
@Category(Appenders.Jms.class)
public class JmsAppenderConnectLaterIT {

    private void appendEvent(final JmsAppender appender) {
        final Map<String, String> map = new HashMap<>();
        final String messageText = "Hello, World!";
        final String loggerName = this.getClass().getName();
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
        appender.append(event);
    }

    @Test
    public void testConnectReConnect() throws Exception {
        // Start broker
        final int port = AvailablePortFinder.getNextAvailable();
        final String brokerUrlString = "tcp://localhost:" + port;
        // Start appender
        // final JmsClientTestConfig jmsClientTestConfig = new JmsClientTestConfig(
        // ActiveMQInitialContextFactory.class.getName(), brokerUrlString, "admin",
        // "admin".toCharArray());
        // jmsClientTestConfig.start();
        // final JmsAppender appender =
        // jmsClientTestConfig.createAppender(MessageLayout.createLayout());
        
        // @formatter:off
		final JmsAppender appender = JmsAppender.newBuilder()
		        .setName("JmsAppender")
		        .setLayout(MessageLayout.createLayout())
		        .setIgnoreExceptions(true)
		        .setFactoryBindingName("ConnectionFactory")
		        .setProviderUrl(brokerUrlString)
		        .setUserName("admin")
		        .setPassword("admin".toCharArray())
		        .build();
	    // @formatter:on
		appender.start();

        // Log message
        appendEvent(appender);
        // Start broker
        BrokerService brokerService = ActiveMqBrokerServiceHelper
                .startBrokerService(JmsAppenderConnectLaterIT.class.getName(), brokerUrlString, port);
        // Stop broker
        ActiveMqBrokerServiceHelper.stopBrokerService(brokerService);
        // Restart broker
        brokerService = ActiveMqBrokerServiceHelper.startBrokerService(JmsAppenderConnectLaterIT.class.getName(),
                brokerUrlString, port);
        // Logging again should cause the appender to reconnect
        appendEvent(appender);
        // Stop broker
        ActiveMqBrokerServiceHelper.stopBrokerService(brokerService);
    }

}
