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
package org.apache.logging.log4j.core.net;

import org.apache.logging.log4j.core.appender.ManagerFactory;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.NamingException;
import java.io.Serializable;

/**
 * Manager for a JMS Queue.
 */
public class JMSQueueManager extends AbstractJMSManager {

    private static final ManagerFactory factory = new JMSQueueManagerFactory();

    private final QueueConnection queueConnection;
    private final QueueSession queueSession;
    private final QueueSender queueSender;

    /**
     * The Constructor.
     * @param name The unique name of the connection.
     * @param conn The QueueConnection.
     * @param sess The QueueSession.
     * @param sender The QueueSender.
     */
    protected JMSQueueManager(String name, QueueConnection conn, QueueSession sess, QueueSender sender) {
        super(name);
        this.queueConnection = conn;
        this.queueSession = sess;
        this.queueSender = sender;
    }

    /**
     * Obtain a JMSQueueManager.
     * @param factoryName The fully qualified class name of the InitialContextFactory.
     * @param providerURL The URL of the provider to use.
     * @param urlPkgPrefixes A colon-separated list of package prefixes for the class name of the factory class that
     * will create a URL context factory
     * @param securityPrincipalName The name of the identity of the Principal.
     * @param securityCredentials The security credentials of the Principal.
     * @param factoryBindingName The name to locate in the Context that provides the QueueConnectionFactory.
     * @param queueBindingName The name to use to locate the Queue.
     * @param userName The userid to use to create the Queue Connection.
     * @param password The password to use to create the Queue Connection.
     * @return The JMSQueueManager.
     */
    public static JMSQueueManager getJMSQueueManager(String factoryName, String providerURL, String urlPkgPrefixes,
                                                     String securityPrincipalName, String securityCredentials,
                                                     String factoryBindingName, String queueBindingName,
                                                     String userName, String password) {

        if (factoryBindingName == null) {
            LOGGER.error("No factory name provided for JMSQueueManager");
            return null;
        }
        if (queueBindingName == null) {
            LOGGER.error("No topic name provided for JMSQueueManager");
            return null;
        }

        String name = "JMSQueue:" + factoryBindingName + "." + queueBindingName;
        return (JMSQueueManager) getManager(name, factory, new FactoryData(factoryName, providerURL, urlPkgPrefixes,
            securityPrincipalName, securityCredentials, factoryBindingName, queueBindingName, userName, password));
    }

    @Override
    public void send(Serializable object) throws Exception {
        super.send(object, queueSession, queueSender);
    }

    @Override
    public void releaseSub() {
        try {
            if (queueSession != null) {
                queueSession.close();
            }
            if (queueConnection != null) {
                queueConnection.close();
            }
        } catch (JMSException ex) {
            LOGGER.error("Error closing " + getName(), ex);
        }
    }

    /**
     * Data for the factory.
     */
    private static class FactoryData {
        private String factoryName;
        private String providerURL;
        private String urlPkgPrefixes;
        private String securityPrincipalName;
        private String securityCredentials;
        private String factoryBindingName;
        private String queueBindingName;
        private String userName;
        private String password;

        public FactoryData(String factoryName, String providerURL, String urlPkgPrefixes, String securityPrincipalName,
                           String securityCredentials, String factoryBindingName, String queueBindingName,
                           String userName, String password) {
            this.factoryName = factoryName;
            this.providerURL = providerURL;
            this.urlPkgPrefixes = urlPkgPrefixes;
            this.securityPrincipalName = securityPrincipalName;
            this.securityCredentials = securityCredentials;
            this.factoryBindingName = factoryBindingName;
            this.queueBindingName = queueBindingName;
            this.userName = userName;
            this.password = password;
        }
    }

    /**
     * Factory to create the JMSQueueManager.
     */
    private static class JMSQueueManagerFactory implements ManagerFactory<JMSQueueManager, FactoryData> {

        public JMSQueueManager createManager(String name, FactoryData data) {
            try {
                Context ctx = createContext(data.factoryName, data.providerURL, data.urlPkgPrefixes,
                                            data.securityPrincipalName, data.securityCredentials);
                QueueConnectionFactory factory = (QueueConnectionFactory) lookup(ctx, data.factoryBindingName);
                QueueConnection conn;
                if (data.userName != null) {
                    conn = factory.createQueueConnection(data.userName, data.password);
                } else {
                    conn = factory.createQueueConnection();
                }
                QueueSession sess = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                Queue queue = (Queue) lookup(ctx, data.queueBindingName);
                QueueSender sender = sess.createSender(queue);
                conn.start();
                return new JMSQueueManager(name, conn, sess, sender);

            } catch (NamingException ex) {
                LOGGER.error("Unable to locate resource", ex);
            } catch (JMSException jmsex) {
                LOGGER.error("Unable to establish connection", jmsex);
            }

            return null;
        }
    }
}
