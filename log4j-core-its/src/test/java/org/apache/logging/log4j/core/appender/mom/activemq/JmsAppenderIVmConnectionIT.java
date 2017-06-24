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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.experimental.categories.Category;

/**
 * Integration test for JmsAppender using an embedded ActiveMQ broker with in
 * direct VM communication between clients and broker.
 * <p>
 * This test manages a client connection to JMS like an Appender would. This
 * test appender is managed at the class level by a JmsTestConfigRule.
 * </p>
 * <p>
 * This test does not manage an Apache ActiveMQ broker explicitly, rather it
 * lets ActiveMQ use its "vm" protocol.
 * </p>
 */
@Category(Appenders.Jms.class)
public class JmsAppenderIVmConnectionIT extends AbstractJmsAppenderIT {

	@ClassRule
	public static final JmsClientTestConfigRule jmsClientTestConfigRule = new JmsClientTestConfigRule(
			ActiveMQInitialContextFactory.class.getName(), "vm://localhost?broker.persistent=false", null, null);

	@AfterClass
	public static void afterClass() {
		jmsClientTestConfigRule.getJmsClientTestConfig().stop();
	}

	@BeforeClass
	public static void beforeClass() {
		jmsClientTestConfigRule.getJmsClientTestConfig().start();
	}

	public JmsAppenderIVmConnectionIT() {
		super(jmsClientTestConfigRule);
	}
}
