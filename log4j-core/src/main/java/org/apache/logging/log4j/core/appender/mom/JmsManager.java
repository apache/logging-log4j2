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

package org.apache.logging.log4j.core.appender.mom;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.naming.NamingException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.net.JndiManager;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.BiConsumer;

/**
 * JMS connection and session manager. Can be used to access MessageProducer, MessageConsumer, and Message objects
 * involving a configured ConnectionFactory and Destination.
 */
public class JmsManager extends AbstractManager {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static final JmsManagerFactory FACTORY = new JmsManagerFactory();

    private final JndiManager jndiManager;
    private final Connection connection;
    private final Session session;
    private final Destination destination;

    private JmsManager(final String name, final JndiManager jndiManager, final String connectionFactoryName,
                       final String destinationName, final String username, final String password)
        throws NamingException, JMSException {
        super(null, name);
        this.jndiManager = jndiManager;
        final ConnectionFactory connectionFactory = this.jndiManager.lookup(connectionFactoryName);
        if (username != null && password != null) {
            this.connection = connectionFactory.createConnection(username, password);
        } else {
            this.connection = connectionFactory.createConnection();
        }
        this.session = this.connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        this.destination = this.jndiManager.lookup(destinationName);
        this.connection.start();
    }

    /**
     * Gets a JmsManager using the specified configuration parameters.
     *
     * @param name                  The name to use for this JmsManager.
     * @param jndiManager           The JndiManager to look up JMS information through.
     * @param connectionFactoryName The binding name for the {@link javax.jms.ConnectionFactory}.
     * @param destinationName       The binding name for the {@link javax.jms.Destination}.
     * @param username              The username to connect with or {@code null} for no authentication.
     * @param password              The password to use with the given username or {@code null} for no authentication.
     * @return The JmsManager as configured.
     */
    public static JmsManager getJmsManager(final String name, final JndiManager jndiManager,
                                           final String connectionFactoryName, final String destinationName,
                                           final String username, final String password) {
        final JmsConfiguration configuration = new JmsConfiguration(jndiManager, connectionFactoryName, destinationName,
            username, password);
        return getManager(name, FACTORY, configuration);
    }

    /**
     * Creates a MessageConsumer on this Destination using the current Session.
     *
     * @return A MessageConsumer on this Destination.
     * @throws JMSException
     */
    public MessageConsumer createMessageConsumer() throws JMSException {
        return this.session.createConsumer(this.destination);
    }

    /**
     * Creates a MessageProducer on this Destination using the current Session.
     *
     * @return A MessageProducer on this Destination.
     * @throws JMSException
     */
    public MessageProducer createMessageProducer() throws JMSException {
        return this.session.createProducer(this.destination);
    }

    /**
     * Creates a TextMessage, MapMessage, or ObjectMessage from a Serializable object.
     * <p>
     * For instance, when using a text-based {@link org.apache.logging.log4j.core.Layout} such as
     * {@link org.apache.logging.log4j.core.layout.PatternLayout}, the {@link org.apache.logging.log4j.core.LogEvent}
     * message will be serialized to a String.
     * </p>
     * <p>
     * When using a layout such as {@link org.apache.logging.log4j.core.layout.SerializedLayout}, the LogEvent message
     * will be serialized as a Java object.
     * </p>
     * <p>
     * When using a layout such as {@link org.apache.logging.log4j.core.layout.MessageLayout} and the LogEvent message
     * is a Log4j MapMessage, the message will be serialized as a JMS MapMessage.
     * </p>
     *
     * @param object
     *            The LogEvent or String message to wrap.
     * @return A new JMS message containing the provided object.
     * @throws JMSException
     */
    public Message createMessage(final Serializable object) throws JMSException {
        if (object instanceof String) {
            return this.session.createTextMessage((String) object);
        } else if (object instanceof org.apache.logging.log4j.message.MapMessage) {
            return map((org.apache.logging.log4j.message.MapMessage<?, ?>) object, this.session.createMapMessage());
        }
        return this.session.createObjectMessage(object);
    }

    private MapMessage map(final org.apache.logging.log4j.message.MapMessage<?, ?> log4jMapMessage, final MapMessage jmsMapMessage) {
        // Map without calling rg.apache.logging.log4j.message.MapMessage#getData() which makes a copy of the map.
        log4jMapMessage.forEach(new BiConsumer<String, Object>() {
            @Override
            public void accept(final String key, final Object value) {
                try {
                    jmsMapMessage.setObject(key, value);
                } catch (JMSException e) {
                    throw new IllegalArgumentException(String.format("%s mapping key '%s' to value '%s': %s",
                            e.getClass(), key, value, e.getLocalizedMessage()), e);
                }
            }
        });
        return jmsMapMessage;
    }


    @Override
    protected boolean releaseSub(final long timeout, final TimeUnit timeUnit) {
        boolean closed = true;
        try {
            this.session.close();
        } catch (final JMSException ignored) {
            // ignore
            closed = false;
        }
        try {
            this.connection.close();
        } catch (final JMSException ignored) {
            // ignore
            closed = false;
        }
        return closed && this.jndiManager.stop(timeout, timeUnit);
    }

    private static class JmsConfiguration {
        private final JndiManager jndiManager;
        private final String connectionFactoryName;
        private final String destinationName;
        private final String username;
        private final String password;

        private JmsConfiguration(final JndiManager jndiManager, final String connectionFactoryName, final String destinationName,
                                 final String username, final String password) {
            this.jndiManager = jndiManager;
            this.connectionFactoryName = connectionFactoryName;
            this.destinationName = destinationName;
            this.username = username;
            this.password = password;
        }

        /**
         * Does not include the password.
         */
        @Override
        public String toString() {
            return "JmsConfiguration [jndiManager=" + jndiManager + ", connectionFactoryName=" + connectionFactoryName
                    + ", destinationName=" + destinationName + ", username=" + username + "]";
        }
    }

    private static class JmsManagerFactory implements ManagerFactory<JmsManager, JmsConfiguration> {

        @Override
        public JmsManager createManager(final String name, final JmsConfiguration data) {
            try {
                return new JmsManager(name, data.jndiManager, data.connectionFactoryName, data.destinationName,
                    data.username, data.password);
            } catch (final Exception e) {
                LOGGER.error("Error creating JmsManager using ConnectionFactory [{}] and Destination [{}].",
                    data.connectionFactoryName, data.destinationName, e);
                return null;
            }
        }
    }

}
