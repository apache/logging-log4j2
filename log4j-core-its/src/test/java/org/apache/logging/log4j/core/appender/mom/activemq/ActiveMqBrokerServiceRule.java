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

import org.apache.activemq.broker.BrokerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.TestMarkers;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * JUnit {@link TestRule} to manage an in-JVM Apache ActiveMQ broker with socket
 * communications between clients and broker.
 *
 * @author <a href="mailto:ggregory@rocketsoftware.com">Gary Gregory</a>
 */
public class ActiveMqBrokerServiceRule implements TestRule {

	static final Logger logger = LogManager.getLogger(ActiveMqBrokerServiceRule.class);

	/**
	 * Apache Active MQ uses this property name to lookup which port to use to
	 * connect to a broker.
	 */
	static final String PORT_PROPERTY_NAME = "org.apache.activemq.AMQ_PORT";

	private final String brokerName;

	private String brokerUrlString;

	private final String portPropertyName;

	public ActiveMqBrokerServiceRule(final String brokerName, final String portPropertyName) {
		this.brokerName = brokerName;
		this.portPropertyName = portPropertyName;
	}

	@Override
	public Statement apply(final Statement base, final Description description) {
		return new Statement() {

			@Override
			public void evaluate() throws Throwable {
				final BrokerService broker = ActiveMqBrokerServiceHelper.startBrokerService(brokerName, brokerUrlString,
						Integer.parseInt(System.getProperty(portPropertyName)));
				logger.debug(TestMarkers.TEST_RULE_LIFE_CYCLE, "{} started Apache Active MQ {}",
						this.getClass().getSimpleName(), this);
				try {
					base.evaluate();
				} finally {
					ActiveMqBrokerServiceHelper.stopBrokerService(broker);
					logger.debug(TestMarkers.TEST_RULE_LIFE_CYCLE, "{} stopped Apache Active MQ {}",
							this.getClass().getSimpleName(), this);
				}
			}

		};
	}

	public String getBrokerName() {
		return brokerName;
	}

	public String getBrokerUrlString() {
		return brokerUrlString;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("ActiveMqBrokerServiceRule [brokerName=");
		builder.append(brokerName);
		builder.append(", bindAddress=");
		builder.append(brokerUrlString);
		builder.append("]");
		return builder.toString();
	}

}
