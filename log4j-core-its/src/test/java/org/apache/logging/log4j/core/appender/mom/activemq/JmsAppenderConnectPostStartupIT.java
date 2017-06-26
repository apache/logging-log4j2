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
import org.apache.logging.log4j.test.AvailablePortSystemPropertyRule;
import org.apache.logging.log4j.test.RuleChainFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;

/**
 * Integration test for JmsAppender using an embedded ActiveMQ broker with in
 * socket communications between clients and broker. This test manages a client
 * connection to JMS like a Appender would. This test appender is managed at the
 * class level by a JmsTestConfigRule.
 * <p>
 * Tests that a JMS appender can connect to a broker AFTER Log4j startup.
 * </p>
 * <p>
 * LOG4J2-1934 JMS Appender does not know how to recover from a broken
 * connection. See https://issues.apache.org/jira/browse/LOG4J2-1934
 * </p>
 */
@Ignore
@Category(Appenders.Jms.class)
public class JmsAppenderConnectPostStartupIT extends AbstractJmsAppenderIT {

	public static final AvailablePortSystemPropertyRule portRule = AvailablePortSystemPropertyRule
			.create(ActiveMqBrokerServiceRule.PORT_PROPERTY_NAME);

	@Rule
	public final ActiveMqBrokerServiceRule activeMqBrokerServiceRule = new ActiveMqBrokerServiceRule(
			JmsAppenderConnectPostStartupIT.class.getName(), portRule.getName());

	// "admin"/"admin" are the default Apache Active MQ creds.
	private static final JmsClientTestConfigRule jmsClientTestConfigRule = new JmsClientTestConfigRule(
			ActiveMQInitialContextFactory.class.getName(), "tcp://localhost:" + portRule.getPort(), "admin", "admin");

	/**
	 * Assign the port and client ONCE for the whole test suite.
	 */
	@ClassRule
	public static final RuleChain ruleChain = RuleChainFactory.create(portRule, jmsClientTestConfigRule);

	@AfterClass
	public static void afterClass() {
		jmsClientTestConfigRule.getJmsClientTestConfig().stop();
	}

	@BeforeClass
	public static void beforeClass() {
		jmsClientTestConfigRule.getJmsClientTestConfig().start();
	}

	public JmsAppenderConnectPostStartupIT() {
		super(jmsClientTestConfigRule);
	}
}
