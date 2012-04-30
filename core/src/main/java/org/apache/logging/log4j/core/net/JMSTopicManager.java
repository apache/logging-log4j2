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
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.naming.Context;
import javax.naming.NamingException;
import java.io.Serializable;

/**
 * Manager for JMS Topic connections.
 */
public class JMSTopicManager extends AbstractJMSManager {

    private static ManagerFactory factory = new JMSTopicManagerFactory();

    private TopicConnection topicConnection;
    private TopicSession topicSession;
    private TopicPublisher topicPublisher;

    /**
     * Constructor.
     * @param name The unique name of the connection.
     * @param conn The TopicConnection.
     * @param sess The TopicSession.
     * @param pub The TopicPublisher.
     */
    public JMSTopicManager(String name, TopicConnection conn, TopicSession sess, TopicPublisher pub) {
        super(name);
        this.topicConnection = conn;
        this.topicSession = sess;
        this.topicPublisher = pub;
    }

    /**
     * Obtain a JSMTopicManager.
     * @param factoryName The fully qualified class name of the InitialContextFactory.
     * @param providerURL The URL of the provider to use.
     * @param urlPkgPrefixes A colon-separated list of package prefixes for the class name of the factory class that
     * will create a URL context factory
     * @param securityPrincipalName The name of the identity of the Principal.
     * @param securityCredentials The security credentials of the Principal.
     * @param factoryBindingName The name to locate in the Context that provides the TopicConnectionFactory.
     * @param topicBindingName The name to use to locate the Topic.
     * @param userName The userid to use to create the Topic Connection.
     * @param password The password to use to create the Topic Connection.
     * @return A JMSTopicManager.
     */
    public static JMSTopicManager getJMSTopicManager(String factoryName, String providerURL, String urlPkgPrefixes,
                                                     String securityPrincipalName, String securityCredentials,
                                                     String factoryBindingName, String topicBindingName,
                                                     String userName, String password) {

        if (factoryBindingName == null) {
            LOGGER.error("No factory name provided for JMSTopicManager");
            return null;
        }
        if (topicBindingName == null) {
            LOGGER.error("No topic name provided for JMSTopicManager");
            return null;
        }

        String name = "JMSTopic:" + factoryBindingName + "." + topicBindingName;
        return (JMSTopicManager) getManager(name, factory, new FactoryData(factoryName, providerURL, urlPkgPrefixes,
            securityPrincipalName, securityCredentials, factoryBindingName, topicBindingName, userName, password));
    }


    @Override
    public void send(Serializable object) throws Exception {
        super.send(object, topicSession, topicPublisher);
    }

    @Override
    public void releaseSub() {
        try {
            if (topicSession != null) {
                topicSession.close();
            }
            if (topicConnection != null) {
                topicConnection.close();
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
        private String topicBindingName;
        private String userName;
        private String password;

        public FactoryData(String factoryName, String providerURL, String urlPkgPrefixes, String securityPrincipalName,
                           String securityCredentials, String factoryBindingName, String topicBindingName,
                           String userName, String password) {
            this.factoryName = factoryName;
            this.providerURL = providerURL;
            this.urlPkgPrefixes = urlPkgPrefixes;
            this.securityPrincipalName = securityPrincipalName;
            this.securityCredentials = securityCredentials;
            this.factoryBindingName = factoryBindingName;
            this.topicBindingName = topicBindingName;
            this.userName = userName;
            this.password = password;
        }
    }

    /**
     * Factory to create a JMSTopicManager.
     */
    private static class JMSTopicManagerFactory implements ManagerFactory<JMSTopicManager, FactoryData> {

        public JMSTopicManager createManager(String name, FactoryData data) {
            try {
                Context ctx = createContext(data.factoryName, data.providerURL, data.urlPkgPrefixes,
                                            data.securityPrincipalName, data.securityCredentials);
                TopicConnectionFactory factory = (TopicConnectionFactory) lookup(ctx, data.factoryBindingName);
                TopicConnection conn;
                if (data.userName != null) {
                    conn = factory.createTopicConnection(data.userName, data.password);
                } else {
                    conn = factory.createTopicConnection();
                }
                TopicSession sess = conn.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
                Topic topic = (Topic) lookup(ctx, data.topicBindingName);
                TopicPublisher pub = sess.createPublisher(topic);
                conn.start();
                return new JMSTopicManager(name, conn, sess, pub);
            } catch (NamingException ex) {
                LOGGER.error("Bad Name " + data.topicBindingName, ex);
            } catch (JMSException jmsex) {
                LOGGER.error("Unable to create publisher ", jmsex);
            }

            return null;
        }
    }
}
