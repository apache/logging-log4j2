/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.appender.mom;

import java.io.Serializable;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
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
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.net.JndiManager;
import org.apache.logging.log4j.core.util.Log4jThread;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Consider this class <b>private</b>; it is only <b>public</b> for access by integration tests.
 *
 * <p>
 * JMS connection and session manager. Can be used to access MessageProducer, MessageConsumer, and Message objects
 * involving a configured ConnectionFactory and Destination.
 * </p>
 */
public class JmsManager extends AbstractManager {

    public static class JmsManagerConfiguration {
        private final Properties jndiProperties;
        private final String connectionFactoryName;
        private final String destinationName;
        private final String userName;
        private final char[] password;
        private final boolean immediateFail;
        private final boolean retry;
        private final long reconnectIntervalMillis;

        JmsManagerConfiguration(
                final Properties jndiProperties,
                final String connectionFactoryName,
                final String destinationName,
                final String userName,
                final char[] password,
                final boolean immediateFail,
                final long reconnectIntervalMillis) {
            this.jndiProperties = jndiProperties;
            this.connectionFactoryName = connectionFactoryName;
            this.destinationName = destinationName;
            this.userName = userName;
            this.password = password;
            this.immediateFail = immediateFail;
            this.reconnectIntervalMillis = reconnectIntervalMillis;
            this.retry = reconnectIntervalMillis > 0;
        }

        public String getConnectionFactoryName() {
            return connectionFactoryName;
        }

        public String getDestinationName() {
            return destinationName;
        }

        public JndiManager getJndiManager() {
            return JndiManager.getJndiManager(getJndiProperties());
        }

        public Properties getJndiProperties() {
            return jndiProperties;
        }

        public char[] getPassword() {
            return password;
        }

        public long getReconnectIntervalMillis() {
            return reconnectIntervalMillis;
        }

        public String getUserName() {
            return userName;
        }

        public boolean isImmediateFail() {
            return immediateFail;
        }

        public boolean isRetry() {
            return retry;
        }

        @Override
        public String toString() {
            return "JmsManagerConfiguration [jndiProperties=" + jndiProperties + ", connectionFactoryName="
                    + connectionFactoryName + ", destinationName=" + destinationName + ", userName=" + userName
                    + ", immediateFail=" + immediateFail + ", retry=" + retry + ", reconnectIntervalMillis="
                    + reconnectIntervalMillis + "]";
        }
    }

    private static class JmsManagerFactory implements ManagerFactory<JmsManager, JmsManagerConfiguration> {

        @Override
        public JmsManager createManager(final String name, final JmsManagerConfiguration data) {
            if (JndiManager.isJndiJmsEnabled()) {
                try {
                    return new JmsManager(name, data);
                } catch (final Exception e) {
                    logger().error("Error creating JmsManager using JmsManagerConfiguration [{}]", data, e);
                    return null;
                }
            }
            logger().error("JNDI must be enabled by setting log4j2.enableJndiJms=true");
            return null;
        }
    }

    /**
     * Handles reconnecting to JMS on a Thread.
     */
    private final class Reconnector extends Log4jThread {

        private final CountDownLatch latch = new CountDownLatch(1);

        private volatile boolean shutdown;

        private final Object owner;

        private Reconnector(final Object owner) {
            super("JmsManager-Reconnector");
            this.owner = owner;
        }

        public void latch() {
            try {
                latch.await();
            } catch (final InterruptedException ex) {
                // Ignore the exception.
            }
        }

        void reconnect() throws NamingException, JMSException {
            final JndiManager jndiManager2 = getJndiManager();
            final Connection connection2 = createConnection(jndiManager2);
            final Session session2 = createSession(connection2);
            final Destination destination2 = createDestination(jndiManager2);
            final MessageProducer messageProducer2 = createMessageProducer(session2, destination2);
            connection2.start();
            synchronized (owner) {
                jndiManager = jndiManager2;
                connection = connection2;
                session = session2;
                destination = destination2;
                messageProducer = messageProducer2;
                reconnector = null;
                shutdown = true;
            }
            logger().debug("Connection reestablished to {}", configuration);
        }

        @Override
        public void run() {
            while (!shutdown) {
                try {
                    sleep(configuration.getReconnectIntervalMillis());
                    reconnect();
                } catch (final InterruptedException | JMSException | NamingException e) {
                    logger().debug(
                                    "Cannot reestablish JMS connection to {}: {}",
                                    configuration,
                                    e.getLocalizedMessage(),
                                    e);
                } finally {
                    latch.countDown();
                }
            }
        }

        public void shutdown() {
            shutdown = true;
        }
    }

    static final JmsManagerFactory FACTORY = new JmsManagerFactory();

    /**
     * Gets a JmsManager using the specified configuration parameters.
     *
     * @param name
     *            The name to use for this JmsManager.
     * @param connectionFactoryName
     *            The binding name for the {@link javax.jms.ConnectionFactory}.
     * @param destinationName
     *            The binding name for the {@link javax.jms.Destination}.
     * @param userName
     *            The userName to connect with or {@code null} for no authentication.
     * @param password
     *            The password to use with the given userName or {@code null} for no authentication.
     * @param immediateFail
     *            Whether or not to fail immediately with a {@link AppenderLoggingException} when connecting to JMS
     *            fails.
     * @param reconnectIntervalMillis
     *            How to log sleep in milliseconds before trying to reconnect to JMS.
     * @param jndiProperties
     *            JNDI properties.
     * @return The JmsManager as configured.
     */
    public static JmsManager getJmsManager(
            final String name,
            final Properties jndiProperties,
            final String connectionFactoryName,
            final String destinationName,
            final String userName,
            final char[] password,
            final boolean immediateFail,
            final long reconnectIntervalMillis) {
        final JmsManagerConfiguration configuration = new JmsManagerConfiguration(
                jndiProperties,
                connectionFactoryName,
                destinationName,
                userName,
                password,
                immediateFail,
                reconnectIntervalMillis);
        return getManager(name, FACTORY, configuration);
    }

    private final JmsManagerConfiguration configuration;

    private volatile Reconnector reconnector;
    private volatile JndiManager jndiManager;
    private volatile Connection connection;
    private volatile Session session;
    private volatile Destination destination;
    private volatile MessageProducer messageProducer;

    private JmsManager(final String name, final JmsManagerConfiguration configuration) {
        super(null, name);
        this.configuration = configuration;
        this.jndiManager = configuration.getJndiManager();
        try {
            this.connection = createConnection(this.jndiManager);
            this.session = createSession(this.connection);
            this.destination = createDestination(this.jndiManager);
            this.messageProducer = createMessageProducer(this.session, this.destination);
            this.connection.start();
        } catch (NamingException | JMSException e) {
            this.reconnector = createReconnector();
            this.reconnector.start();
        }
    }

    private boolean closeConnection() {
        if (connection == null) {
            return true;
        }
        final Connection temp = connection;
        connection = null;
        try {
            temp.close();
            return true;
        } catch (final JMSException e) {
            StatusLogger.getLogger()
                    .debug(
                            "Caught exception closing JMS Connection: {} ({}); continuing JMS manager shutdown",
                            e.getLocalizedMessage(),
                            temp,
                            e);
            return false;
        }
    }

    private boolean closeJndiManager() {
        if (jndiManager == null) {
            return true;
        }
        final JndiManager tmp = jndiManager;
        jndiManager = null;
        tmp.close();
        return true;
    }

    private boolean closeMessageProducer() {
        if (messageProducer == null) {
            return true;
        }
        final MessageProducer temp = messageProducer;
        messageProducer = null;
        try {
            temp.close();
            return true;
        } catch (final JMSException e) {
            StatusLogger.getLogger()
                    .debug(
                            "Caught exception closing JMS MessageProducer: {} ({}); continuing JMS manager shutdown",
                            e.getLocalizedMessage(),
                            temp,
                            e);
            return false;
        }
    }

    private boolean closeSession() {
        if (session == null) {
            return true;
        }
        final Session temp = session;
        session = null;
        try {
            temp.close();
            return true;
        } catch (final JMSException e) {
            StatusLogger.getLogger()
                    .debug(
                            "Caught exception closing JMS Session: {} ({}); continuing JMS manager shutdown",
                            e.getLocalizedMessage(),
                            temp,
                            e);
            return false;
        }
    }

    private Connection createConnection(final JndiManager jndiManager) throws NamingException, JMSException {
        final ConnectionFactory connectionFactory = jndiManager.lookup(configuration.getConnectionFactoryName());
        if (configuration.getUserName() != null && configuration.getPassword() != null) {
            return connectionFactory.createConnection(
                    configuration.getUserName(),
                    configuration.getPassword() == null ? null : String.valueOf(configuration.getPassword()));
        }
        return connectionFactory.createConnection();
    }

    private Destination createDestination(final JndiManager jndiManager) throws NamingException {
        return jndiManager.lookup(configuration.getDestinationName());
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
     * @throws JMSException if the JMS provider fails to create this message due to some internal error.
     */
    public Message createMessage(final Serializable object) throws JMSException {
        if (object instanceof String) {
            return this.session.createTextMessage((String) object);
        } else if (object instanceof org.apache.logging.log4j.message.MapMessage) {
            return map((org.apache.logging.log4j.message.MapMessage<?, ?>) object, this.session.createMapMessage());
        }
        return this.session.createObjectMessage(object);
    }

    private void createMessageAndSend(final LogEvent event, final Serializable serializable) throws JMSException {
        final Message message = createMessage(serializable);
        message.setJMSTimestamp(event.getTimeMillis());
        messageProducer.send(message);
    }

    /**
     * Creates a MessageConsumer on this Destination using the current Session.
     *
     * @return A MessageConsumer on this Destination.
     * @throws JMSException if the session fails to create a consumer due to some internal error.
     */
    public MessageConsumer createMessageConsumer() throws JMSException {
        return this.session.createConsumer(this.destination);
    }

    /**
     * Creates a MessageProducer on this Destination using the current Session.
     *
     * @param session
     *            The JMS Session to use to create the MessageProducer
     * @param destination
     *            The JMS Destination for the MessageProducer
     * @return A MessageProducer on this Destination.
     * @throws JMSException if the session fails to create a MessageProducer due to some internal error.
     */
    public MessageProducer createMessageProducer(final Session session, final Destination destination)
            throws JMSException {
        return session.createProducer(destination);
    }

    private Reconnector createReconnector() {
        final Reconnector recon = new Reconnector(this);
        recon.setDaemon(true);
        recon.setPriority(Thread.MIN_PRIORITY);
        return recon;
    }

    private Session createSession(final Connection connection) throws JMSException {
        return connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    }

    public JmsManagerConfiguration getJmsManagerConfiguration() {
        return configuration;
    }

    JndiManager getJndiManager() {
        return configuration.getJndiManager();
    }

    <T> T lookup(final String destinationName) throws NamingException {
        return this.jndiManager.lookup(destinationName);
    }

    private MapMessage map(
            final org.apache.logging.log4j.message.MapMessage<?, ?> log4jMapMessage, final MapMessage jmsMapMessage) {
        // Map without calling org.apache.logging.log4j.message.MapMessage#getData() which makes a copy of the map.
        log4jMapMessage.forEach((key, value) -> {
            try {
                jmsMapMessage.setObject(key, value);
            } catch (final JMSException e) {
                throw new IllegalArgumentException(
                        String.format(
                                "%s mapping key '%s' to value '%s': %s",
                                e.getClass(), key, value, e.getLocalizedMessage()),
                        e);
            }
        });
        return jmsMapMessage;
    }

    @Override
    protected boolean releaseSub(final long timeout, final TimeUnit timeUnit) {
        if (reconnector != null) {
            reconnector.shutdown();
            reconnector.interrupt();
            reconnector = null;
        }
        boolean closed = false;
        closed &= closeJndiManager();
        closed &= closeMessageProducer();
        closed &= closeSession();
        closed &= closeConnection();
        return closed && this.jndiManager.stop(timeout, timeUnit);
    }

    void send(final LogEvent event, final Serializable serializable) {
        if (messageProducer == null) {
            if (reconnector != null && !configuration.isImmediateFail()) {
                reconnector.latch();
                if (messageProducer == null) {
                    throw new AppenderLoggingException(
                            "Error sending to JMS Manager '" + getName() + "': JMS message producer not available");
                }
            }
        }
        synchronized (this) {
            try {
                createMessageAndSend(event, serializable);
            } catch (final JMSException causeEx) {
                if (configuration.isRetry() && reconnector == null) {
                    reconnector = createReconnector();
                    try {
                        closeJndiManager();
                        reconnector.reconnect();
                    } catch (NamingException | JMSException reconnEx) {
                        logger().debug(
                                        "Cannot reestablish JMS connection to {}: {}; starting reconnector thread {}",
                                        configuration,
                                        reconnEx.getLocalizedMessage(),
                                        reconnector.getName(),
                                        reconnEx);
                        reconnector.start();
                        throw new AppenderLoggingException(
                                String.format("JMS exception sending to %s for %s", getName(), configuration), causeEx);
                    }
                    try {
                        createMessageAndSend(event, serializable);
                    } catch (final JMSException e) {
                        throw new AppenderLoggingException(
                                String.format(
                                        "Error sending to %s after reestablishing JMS connection for %s",
                                        getName(), configuration),
                                causeEx);
                    }
                }
            }
        }
    }
}
