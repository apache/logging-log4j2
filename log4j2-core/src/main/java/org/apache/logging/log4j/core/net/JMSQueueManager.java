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
 *
 */
public class JMSQueueManager extends AbstractJMSManager {

    private QueueConnection queueConnection;
    private QueueSession queueSession;
    private QueueSender queueSender;


    private static ManagerFactory factory = new JMSTopicManagerFactory();

    public static JMSQueueManager getJMSQueueManager(String factoryName, String providerURL, String urlPkgPrefixes,
                                                     String securityPrincipalName, String securityCredentials,
                                                     String factoryBindingName, String queueBindingName,
                                                     String userName, String password) {

        if (factoryBindingName == null) {
            logger.error("No factory name provided for JMSQueueManager");
            return null;
        }
        if (queueBindingName == null) {
            logger.error("No topic name provided for JMSQueueManager");
            return null;
        }

        String name = "JMSQueue:" + factoryBindingName + "." + queueBindingName;
        return (JMSQueueManager) getManager(name, factory, new FactoryData(factoryName, providerURL, urlPkgPrefixes,
            securityPrincipalName, securityCredentials, factoryBindingName, queueBindingName, userName, password));
    }

    public JMSQueueManager(String name, QueueConnection conn, QueueSession sess, QueueSender sender) {
        super(name);
        this.queueConnection = conn;
        this.queueSession = sess;
        this.queueSender = sender;
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
            logger.error("Error closing " + getName(), ex);
        }
    }


    private static class FactoryData {
        String factoryName;
        String providerURL;
        String urlPkgPrefixes;
        String securityPrincipalName;
        String securityCredentials;
        String factoryBindingName;
        String queueBindingName;
        String userName;
        String password;

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

    private static class JMSTopicManagerFactory implements ManagerFactory<JMSQueueManager, FactoryData> {

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

            } catch (JMSException jmsex) {

            }

            return null;
        }
    }
}
