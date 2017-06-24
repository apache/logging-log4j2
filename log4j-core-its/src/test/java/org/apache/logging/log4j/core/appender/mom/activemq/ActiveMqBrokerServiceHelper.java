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

import java.io.IOException;

import org.apache.activemq.broker.BrokerService;

/**
 * Helps starts an embedded Apache ActiveMQ service broker.
 */
public class ActiveMqBrokerServiceHelper {

	static BrokerService startBrokerService(final String brokerName, String brokerUrlString, final int port) throws Exception {
		// TODO Abstract out scheme
		brokerUrlString = "tcp://localhost:" + port;
		final BrokerService broker = new BrokerService();
		// configure the Broker
		broker.setBrokerName(brokerName);
		broker.addConnector(brokerUrlString);
		broker.setPersistent(false);
		broker.start();
		broker.waitUntilStarted();
		return broker;
	}

	static void stopBrokerService(final BrokerService broker) throws IOException, Exception {
		broker.deleteAllMessages();
		broker.stop();
		broker.waitUntilStopped();
	}

}
