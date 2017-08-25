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

import java.util.Properties;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.mom.JmsAppender;
import org.apache.logging.log4j.core.appender.mom.JmsManager;
import org.apache.logging.log4j.core.net.JndiManager;

/**
 * All the JMS information and state needed to configure and get a test going.
 */
class JmsClientTestConfig {

    private JmsAppender jmsAppender;
    private final String jmsInitialContextFactoryClassName;
    private JmsManager jmsManager;
    private final char[] jmsPassword;
    private final String jmsProviderUrlStr;
    private final String jmsUserName;

    JmsClientTestConfig(final String jmsInitialContextFactoryClassName, final String jmsProviderUrlStr,
            final String jmsUserName, final char[] jmsPassword) {
        this.jmsInitialContextFactoryClassName = jmsInitialContextFactoryClassName;
        this.jmsProviderUrlStr = jmsProviderUrlStr;
        this.jmsUserName = jmsUserName;
        this.jmsPassword = jmsPassword;
    }

    JmsAppender createAppender(final Layout<?> layout) {
        // @formatter:off
		jmsAppender = JmsAppender.newBuilder()
				.setName("JmsAppender")
				.setLayout(layout)
				.setIgnoreExceptions(true)
				.setJmsManager(jmsManager)
				.setReconnectIntervalMillis(2000)
				.build();
		// @formatter:on
        jmsAppender.start();
        return jmsAppender;
    }

    JmsAppender getJmsAppender() {
        return jmsAppender;
    }

    String getJmsInitialContextFactoryClassName() {
        return jmsInitialContextFactoryClassName;
    }

    JmsManager getJmsManager() {
        return jmsManager;
    }

    char[] getJmsPassword() {
        return jmsPassword;
    }

    String getJmsProviderUrlStr() {
        return jmsProviderUrlStr;
    }

    String getJmsUserName() {
        return jmsUserName;
    }

    void setJmsAppender(final JmsAppender jmsAppender) {
        this.jmsAppender = jmsAppender;
    }

    void setJmsManager(final JmsManager jmsManager) {
        this.jmsManager = jmsManager;
    }

    void start() {
        System.setProperty(AbstractJmsAppenderIT.KEY_SERIALIZABLE_PACKAGES,
                "org.apache.logging.log4j.core.impl,org.apache.logging.log4j.util,org.apache.logging.log4j,java.rmi");
        final Properties additional = new Properties();
        additional.setProperty("queue.TestQueue", "TestQueue");
        // jndiManager is closed in stop() through the jmsManager
        final Properties jndiProperties = JndiManager.createProperties(jmsInitialContextFactoryClassName,
                jmsProviderUrlStr, null, null, null, additional);
        final String name = JmsManager.class.getName() + "-" + getClass().getSimpleName() + "@" + hashCode();
        jmsManager = JmsManager.getJmsManager(name, jndiProperties, "ConnectionFactory", "TestQueue", jmsUserName,
                jmsPassword, false, JmsAppender.Builder.DEFAULT_RECONNECT_INTERVAL_MILLIS);
    }

    void stop() {
        if (jmsManager != null) {
            jmsManager.close();
            jmsManager = null;
        }
        System.getProperties().remove(AbstractJmsAppenderIT.KEY_SERIALIZABLE_PACKAGES);
    }
}