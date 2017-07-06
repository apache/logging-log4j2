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
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.logging.log4j.core.appender.mom.JmsAppender;
import org.apache.logging.log4j.core.appender.mom.JmsManager;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.StringMapMessage;
import org.junit.After;
import org.junit.Assert;

/**
 * Subclass for tests that reconnect to Apache Active MQ. The class makes sure resources are properly shutdown after
 * each @Test method. A subclass normally only has one @Test method.
 * <p>
 * LOG4J2-1934 JMS Appender does not know how to recover from a broken connection. See
 * https://issues.apache.org/jira/browse/LOG4J2-1934
 * </p>
 */
public class AbstractJmsAppenderReconnectIT {

    protected JmsClientTestConfig jmsClientTestConfig;
    protected JmsAppender appender;
    protected BrokerService brokerService;

    @After
    public void after() {
        try {
            ActiveMqBrokerServiceHelper.stopBrokerService(brokerService);
        } catch (final Exception e) {
            // Just log to the console for now.
            e.printStackTrace();
        }
        if (appender != null) {
            appender.stop();
        }
        if (jmsClientTestConfig != null) {
            jmsClientTestConfig.stop();
        }
        // Make sure the manager is gone as to not have bad side effect on other tests.
        @SuppressWarnings("resource")
        final JmsManager appenderManager = appender.getManager();
        if (appenderManager != null) {
            Assert.assertFalse(AbstractManager.hasManager(appenderManager.getName()));
        }
        // Make sure the manager is gone as to not have bad side effect on other tests.
        @SuppressWarnings("resource")
        final JmsManager testManager = jmsClientTestConfig.getJmsManager();
        if (testManager != null) {
            Assert.assertFalse(AbstractManager.hasManager(testManager.getName()));
        }
    }

    protected void appendEvent(final JmsAppender appender) {
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

}
