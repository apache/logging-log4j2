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
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;
import javax.jms.Message;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.appender.mom.JmsManager.JmsManagerConfiguration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAliases;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.layout.SerializedLayout;
import org.apache.logging.log4j.core.net.JndiManager;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Generic JMS Appender plugin for both queues and topics. This Appender replaces the previous split ones. However,
 * configurations set up for the 2.0 version of the JMS appenders will still work.
 */
@Plugin(name = "JMS", category = Node.CATEGORY, elementType = Appender.ELEMENT_TYPE, printObject = true)
@PluginAliases({ "JMSQueue", "JMSTopic" })
public class JmsAppender extends AbstractAppender {

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<JmsAppender> {

        @PluginBuilderAttribute
        @Required(message = "A name for the JmsAppender must be specified")
        private String name;

        @PluginBuilderAttribute
        private String factoryName;

        @PluginBuilderAttribute
        private String providerUrl;

        @PluginBuilderAttribute
        private String urlPkgPrefixes;

        @PluginBuilderAttribute
        private String securityPrincipalName;

        @PluginBuilderAttribute(sensitive = true)
        private String securityCredentials;

        @PluginBuilderAttribute
        @Required(message = "A javax.jms.ConnectionFactory JNDI name must be specified")
        private String factoryBindingName;

        @PluginBuilderAttribute
        @PluginAliases({ "queueBindingName", "topicBindingName" })
        @Required(message = "A javax.jms.Destination JNDI name must be specified")
        private String destinationBindingName;

        @PluginBuilderAttribute
        private String userName;

        @PluginBuilderAttribute(sensitive = true)
        private String password;

        @PluginElement("Layout")
        private Layout<? extends Serializable> layout = SerializedLayout.createLayout();

        @PluginElement("Filter")
        private Filter filter;

        @PluginElement("ReconnectOnExceptionMessage")
        private String[] reconnectOnExceptionMessages = new String[] { "closed" };

        @PluginBuilderAttribute("reconnectAttempts")
        private final int reconnectAttempts = 3;

        @PluginBuilderAttribute("reconnectIntervalMillis")
        private final long reconnectIntervalMillis = 1000;

        @PluginBuilderAttribute
        private boolean ignoreExceptions = true;

        // Programmatic access only for now.
        private JmsManager jmsManager;

        private Builder() {
        }

        @SuppressWarnings("resource") // actualJmsManager and jndiManager are managed by the JmsAppender
        @Override
        public JmsAppender build() {
            JmsManager actualJmsManager = jmsManager;
            JndiManager jndiManager = null;
            JmsManagerConfiguration configuration = null;
            if (actualJmsManager == null) {
                jndiManager = JndiManager.getJndiManager(factoryName, providerUrl, urlPkgPrefixes,
                        securityPrincipalName, securityCredentials, null);
                configuration = new JmsManagerConfiguration(jndiManager, factoryBindingName, destinationBindingName,
                        userName, password);
                actualJmsManager = AbstractManager.getManager(name, JmsManager.FACTORY, configuration);
            }
            // TODO Try to reconnect later by letting the manager be null?
            if (actualJmsManager == null) {
                // JmsManagerFactory has already logged an ERROR.
                return null;
            }
            return new JmsAppender(name, filter, layout, ignoreExceptions, reconnectOnExceptionMessages,
                    reconnectAttempts, reconnectIntervalMillis, actualJmsManager);
        }

        public Builder setDestinationBindingName(final String destinationBindingName) {
            this.destinationBindingName = destinationBindingName;
            return this;
        }

        public Builder setFactoryBindingName(final String factoryBindingName) {
            this.factoryBindingName = factoryBindingName;
            return this;
        }

        public Builder setFactoryName(final String factoryName) {
            this.factoryName = factoryName;
            return this;
        }

        public Builder setFilter(final Filter filter) {
            this.filter = filter;
            return this;
        }

        public Builder setIgnoreExceptions(final boolean ignoreExceptions) {
            this.ignoreExceptions = ignoreExceptions;
            return this;
        }

        public Builder setJmsManager(final JmsManager jmsManager) {
            this.jmsManager = jmsManager;
            return this;
        }

        public Builder setLayout(final Layout<? extends Serializable> layout) {
            this.layout = layout;
            return this;
        }

        public Builder setName(final String name) {
            this.name = name;
            return this;
        }

        public Builder setPassword(final String password) {
            this.password = password;
            return this;
        }

        public Builder setProviderUrl(final String providerUrl) {
            this.providerUrl = providerUrl;
            return this;
        }

        public Builder setSecurityCredentials(final String securityCredentials) {
            this.securityCredentials = securityCredentials;
            return this;
        }

        public Builder setSecurityPrincipalName(final String securityPrincipalName) {
            this.securityPrincipalName = securityPrincipalName;
            return this;
        }

        public Builder setUrlPkgPrefixes(final String urlPkgPrefixes) {
            this.urlPkgPrefixes = urlPkgPrefixes;
            return this;
        }

        /**
         * @deprecated Use {@link #setUserName(String)}.
         */
        @Deprecated
        public Builder setUsername(final String username) {
            this.userName = username;
            return this;
        }

        public Builder setUserName(final String userName) {
            this.userName = userName;
            return this;
        }

        /**
         * Does not include the password.
         */
        @Override
        public String toString() {
            return "Builder [name=" + name + ", factoryName=" + factoryName + ", providerUrl=" + providerUrl
                    + ", urlPkgPrefixes=" + urlPkgPrefixes + ", securityPrincipalName=" + securityPrincipalName
                    + ", securityCredentials=" + securityCredentials + ", factoryBindingName=" + factoryBindingName
                    + ", destinationBindingName=" + destinationBindingName + ", username=" + userName + ", layout="
                    + layout + ", filter=" + filter + ", ignoreExceptions=" + ignoreExceptions + ", jmsManager="
                    + jmsManager + "]";
        }

        public void setReconnectOnExceptionMessage(final String[] reconnectOnExceptionMessage) {
            this.reconnectOnExceptionMessages = reconnectOnExceptionMessage;
        }

    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    private volatile JmsManager manager;
    private final String[] reconnectOnExceptionMessages;
    private final int reconnectAttempts;
    private final long reconnectIntervalMillis;

    /**
     * 
     * @throws JMSException not thrown as of 2.9 but retained in the signature for compatibility
     * @deprecated Use the other constructor
     */
    @Deprecated
    protected JmsAppender(final String name, final Filter filter, final Layout<? extends Serializable> layout,
            final boolean ignoreExceptions, final JmsManager manager) throws JMSException {
        super(name, filter, layout, ignoreExceptions);
        this.manager = manager;
        this.reconnectOnExceptionMessages = null;
        this.reconnectAttempts = 0;
        this.reconnectIntervalMillis = 0;
    }

    protected JmsAppender(final String name, final Filter filter, final Layout<? extends Serializable> layout,
            final boolean ignoreExceptions, final String[] reconnectOnExceptionMessage, final int reconnectAttempts,
            final long reconnectIntervalMillis, final JmsManager manager) {
        super(name, filter, layout, ignoreExceptions);
        this.manager = manager;
        this.reconnectOnExceptionMessages = reconnectOnExceptionMessage;
        this.reconnectAttempts = reconnectAttempts;
        this.reconnectIntervalMillis = reconnectIntervalMillis;
    }

    @Override
    public void append(final LogEvent event) {
        Serializable serializable = null;
        try {
            serializable = getLayout().toSerializable(event);
            send(event, serializable);
        } catch (final JMSException e) {
            // Try to reconnect once under specific conditions
            // reconnectOnExceptionMessages MUST be set to demonstrate intent
            // This is designed to handle the use case where an application is running and the JMS broker is recycled.
            if (reconnectOnExceptionMessages == null) {
                throw new AppenderLoggingException(e);
            }
            boolean reconnect = false;
            for (final String message : reconnectOnExceptionMessages) {
                reconnect = Objects.toString(e.getMessage()).contains(message);
                if (reconnect) {
                    break;
                }
            }
            if (reconnect) {
                int count = 0;
                while (count < reconnectAttempts) {
                    // TODO How to best synchronize this?
                    final JmsManagerConfiguration config = this.manager.getJmsManagerConfiguration();
                    this.manager = AbstractManager.getManager(getName(), JmsManager.FACTORY, config);
                    try {
                        if (serializable != null) {
                            count++;
                            StatusLogger.getLogger().debug(
                                    "Reconnect attempt {} of {} for JMS appender {} and configuration {} due to {}",
                                    count, reconnectAttempts, getName(), config, e.toString(), e);
                            send(event, serializable);
                            return;
                        }
                    } catch (final JMSException e1) {
                        if (count == reconnectAttempts) {
                            throw new AppenderLoggingException(e);
                        }
                        StatusLogger.getLogger().debug(
                                "Reconnect attempt {} of {} FAILED for JMS appender {} and configuration {} due to {}; slepping {} milliseconds...",
                                count, reconnectAttempts, getName(), config, e.toString(), reconnectIntervalMillis, e);
                        if (reconnectIntervalMillis > 0) {
                            try {
                                Thread.sleep(reconnectIntervalMillis);
                            } catch (final InterruptedException e2) {
                                throw new AppenderLoggingException(e2);
                            }
                        }
                    }
                }
            }
        }
    }

    private void send(final LogEvent event, final Serializable serializable) throws JMSException {
        final Message message = this.manager.createMessage(serializable);
        message.setJMSTimestamp(event.getTimeMillis());
        this.manager.send(message);
    }

    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        setStopping();
        boolean stopped = super.stop(timeout, timeUnit, false);
        stopped &= this.manager.stop(timeout, timeUnit);
        setStopped();
        return stopped;
    }

}
