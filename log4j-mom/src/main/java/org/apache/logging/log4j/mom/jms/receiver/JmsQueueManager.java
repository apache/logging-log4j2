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
package org.apache.logging.log4j.mom.jms.receiver;

import java.io.Serializable;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.NamingException;

import org.apache.logging.log4j.core.appender.ManagerFactory;

/**
 * Manager for a JMS Queue.
 */
public class JmsQueueManager extends AbstractJmsManager {

    private static final JMSQueueManagerFactory FACTORY = new JMSQueueManagerFactory();

    private QueueInfo info;
    private final String factoryBindingName;
    private final String queueBindingName;
    private final String userName;
    private final String password;
    private final Context context;

    /**
     * The Constructor.
     * @param name The unique name of the connection.
     * @param context The context.
     * @param factoryBindingName The factory binding name.
     * @param queueBindingName The queue binding name.
     * @param userName The user name.
     * @param password The credentials for the user.
     * @param info The Queue connection info.
     */
    protected JmsQueueManager(final String name, final Context context, final String factoryBindingName,
                              final String queueBindingName, final String userName, final String password,
                              final QueueInfo info) {
        super(name);
        this.context = context;
        this.factoryBindingName = factoryBindingName;
        this.queueBindingName = queueBindingName;
        this.userName = userName;
        this.password = password;
        this.info = info;
    }

    /**
     * Obtain a JmsQueueManager.
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
     * @return The JmsQueueManager.
     */
    public static JmsQueueManager getJmsQueueManager(final String factoryName, final String providerURL,
                                                     final String urlPkgPrefixes, final String securityPrincipalName,
                                                     final String securityCredentials, final String factoryBindingName,
                                                     final String queueBindingName, final String userName,
                                                     final String password) {

        if (factoryBindingName == null) {
            LOGGER.error("No factory name provided for JmsQueueManager");
            return null;
        }
        if (queueBindingName == null) {
            LOGGER.error("No topic name provided for JmsQueueManager");
            return null;
        }

        final String name = "JMSQueue:" + factoryBindingName + '.' + queueBindingName;
        return getManager(name, FACTORY, new FactoryData(factoryName, providerURL, urlPkgPrefixes,
            securityPrincipalName, securityCredentials, factoryBindingName, queueBindingName, userName, password));
    }

    @Override
    public synchronized void send(final Serializable object) throws Exception {
        if (info == null) {
            info = connect(context, factoryBindingName, queueBindingName, userName, password, false);
        }
        try {
            super.send(object, info.session, info.sender);
        } catch (final Exception ex) {
            cleanup(true);
            throw ex;
        }
    }

    @Override
    public void releaseSub() {
        if (info != null) {
            cleanup(false);
        }
    }

    private void cleanup(final boolean quiet) {
        try {
            info.session.close();
        } catch (final Exception e) {
            if (!quiet) {
                LOGGER.error("Error closing session for " + getName(), e);
            }
        }
        try {
            info.conn.close();
        } catch (final Exception e) {
            if (!quiet) {
                LOGGER.error("Error closing connection for " + getName(), e);
            }
        }
        info = null;
    }

    /**
     * Data for the factory.
     */
    private static class FactoryData {
        private final String factoryName;
        private final String providerURL;
        private final String urlPkgPrefixes;
        private final String securityPrincipalName;
        private final String securityCredentials;
        private final String factoryBindingName;
        private final String queueBindingName;
        private final String userName;
        private final String password;

        public FactoryData(final String factoryName, final String providerURL, final String urlPkgPrefixes,
                           final String securityPrincipalName, final String securityCredentials,
                           final String factoryBindingName, final String queueBindingName, final String userName,
                           final String password) {
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

    private static QueueInfo connect(final Context context, final String factoryBindingName,
                                     final String queueBindingName, final String userName, final String password,
                                     final boolean suppress) throws Exception {
        try {
            final QueueConnectionFactory factory = (QueueConnectionFactory) lookup(context, factoryBindingName);
            QueueConnection conn;
            if (userName != null) {
                conn = factory.createQueueConnection(userName, password);
            } else {
                conn = factory.createQueueConnection();
            }
            final QueueSession sess = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            final Queue queue = (Queue) lookup(context, queueBindingName);
            final QueueSender sender = sess.createSender(queue);
            conn.start();
            return new QueueInfo(conn, sess, sender);
        } catch (final NamingException ex) {
            LOGGER.warn("Unable to locate connection factory " + factoryBindingName, ex);
            if (!suppress) {
                throw ex;
            }
        } catch (final JMSException ex) {
            LOGGER.warn("Unable to create connection to queue " + queueBindingName, ex);
            if (!suppress) {
                throw ex;
            }
        }
        return null;
    }

    /** Queue connection information */
    private static class QueueInfo {
        private final QueueConnection conn;
        private final QueueSession session;
        private final QueueSender sender;

        public QueueInfo(final QueueConnection conn, final QueueSession session, final QueueSender sender) {
            this.conn = conn;
            this.session = session;
            this.sender = sender;
        }
    }

    /**
     * Factory to create the JmsQueueManager.
     */
    private static class JMSQueueManagerFactory implements ManagerFactory<JmsQueueManager, FactoryData> {

        @Override
        public JmsQueueManager createManager(final String name, final FactoryData data) {
            try {
                final Context ctx = createContext(data.factoryName, data.providerURL, data.urlPkgPrefixes,
                                            data.securityPrincipalName, data.securityCredentials);
                final QueueInfo info = connect(ctx, data.factoryBindingName, data.queueBindingName, data.userName,
                    data.password, true);
                return new JmsQueueManager(name, ctx, data.factoryBindingName, data.queueBindingName,
                    data.userName, data.password, info);
            } catch (final NamingException ex) {
                LOGGER.error("Unable to locate resource", ex);
            } catch (final Exception ex) {
                LOGGER.error("Unable to connect", ex);
            }

            return null;
        }
    }
}
