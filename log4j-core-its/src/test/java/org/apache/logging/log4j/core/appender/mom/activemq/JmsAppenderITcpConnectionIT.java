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
import org.apache.logging.log4j.server.mom.activemq.ActiveMqBrokerServiceRule;
import org.apache.logging.log4j.test.AvailablePortSystemPropertyRule;
import org.apache.logging.log4j.test.RuleChainFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;

/**
 * Integration test for JmsAppender using an embedded ActiveMQ broker with in
 * socket communications between clients and broker. This test manages a client
 * connection to JMS like an Appender would. This test appender is managed at
 * the class level by a JmsTestConfigRule.
 * <p>
 * The test manages an Apache ActiveMQ broken embedded in this test. A new
 * broker is started and stopped for each test method on the same port, which
 * means that the JMS Appender needs to reconnect to JMS for the second test
 * run, which ever that test maybe.
 * </p>
 */
@Category(Appenders.Jms.class)
public class JmsAppenderITcpConnectionIT extends AbstractJmsAppenderIT {

	public static final AvailablePortSystemPropertyRule portRule = AvailablePortSystemPropertyRule
			.create(ActiveMqBrokerServiceRule.PORT_PROPERTY_NAME);

	public static final ActiveMqBrokerServiceRule activeMqBrokerServiceRule = new ActiveMqBrokerServiceRule(
			JmsAppenderITcpConnectionIT.class.getName(), portRule.getName());

	// "admin"/"admin" are the default Apache Active MQ creds.
	public static final JmsClientTestConfigRule jmsClientTestConfigRule = new JmsClientTestConfigRule(
			activeMqBrokerServiceRule, ActiveMQInitialContextFactory.class.getName(), "admin", "admin".toCharArray());

	/**
	 * We assign a port only ONCE ands start the broker ONCE for the whole test
	 * suite.
	 */
	@ClassRule
	public static final RuleChain ruleChain = RuleChainFactory.create(portRule, activeMqBrokerServiceRule,
			jmsClientTestConfigRule);

	@AfterClass
	public static void afterClass() {
		jmsClientTestConfigRule.getJmsClientTestConfig().stop();
	}

	@BeforeClass
	public static void beforeClass() {
		jmsClientTestConfigRule.getJmsClientTestConfig().start();
	}

	public JmsAppenderITcpConnectionIT() {
		super(jmsClientTestConfigRule);
	}
}
