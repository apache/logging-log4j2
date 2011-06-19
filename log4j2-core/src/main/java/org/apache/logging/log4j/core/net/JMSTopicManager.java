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
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.naming.Context;
import javax.naming.NamingException;
import java.io.Serializable;

/**
 *
 */
public class JMSTopicManager extends AbstractJMSManager {

    private TopicConnection topicConnection;
    private TopicSession topicSession;
    private TopicPublisher topicPublisher;


    private static ManagerFactory factory = new JMSTopicManagerFactory();

    public static JMSTopicManager getJMSTopicManager(String factoryName, String providerURL, String urlPkgPrefixes,
                                                     String securityPrincipalName, String securityCredentials,
                                                     String factoryBindingName, String topicBindingName,
                                                     String userName, String password) {

        if (factoryBindingName == null) {
            logger.error("No factory name provided for JMSTopicManager");
            return null;
        }
        if (topicBindingName == null) {
            logger.error("No topic name provided for JMSTopicManager");
            return null;
        }

        String name = "JMSTopic:" + factoryBindingName + "." + topicBindingName;
        return (JMSTopicManager) getManager(name, factory, new FactoryData(factoryName, providerURL, urlPkgPrefixes,
            securityPrincipalName, securityCredentials, factoryBindingName, topicBindingName, userName, password));
    }

    public JMSTopicManager(String name, TopicConnection conn, TopicSession sess, TopicPublisher pub) {
        super(name);
        this.topicConnection = conn;
        this.topicSession = sess;
        this.topicPublisher = pub;
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
        String topicBindingName;
        String userName;
        String password;

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
                logger.error("Bad Name " + data.topicBindingName, ex);
            } catch (JMSException jmsex) {
                logger.error("Unable to create publisher ", jmsex);
            }

            return null;
        }
    }
}
