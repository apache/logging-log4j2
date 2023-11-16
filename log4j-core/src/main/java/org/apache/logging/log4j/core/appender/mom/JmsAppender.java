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
import java.util.concurrent.TimeUnit;
import javax.jms.JMSException;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.logging.log4j.core.appender.mom.JmsManager.JmsManagerConfiguration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAliases;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.net.JndiManager;

/**
 * Generic JMS Appender plugin for both queues and topics. This Appender replaces the previous split ones. However,
 * configurations set up for the 2.0 version of the JMS appenders will still work.
 */
@Plugin(name = "JMS", category = Node.CATEGORY, elementType = Appender.ELEMENT_TYPE, printObject = true)
@PluginAliases({"JMSQueue", "JMSTopic"})
public class JmsAppender extends AbstractAppender {

    public static class Builder<B extends Builder<B>> extends AbstractAppender.Builder<B>
            implements org.apache.logging.log4j.core.util.Builder<JmsAppender> {

        public static final int DEFAULT_RECONNECT_INTERVAL_MILLIS = 5000;

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
        @PluginAliases({"queueBindingName", "topicBindingName"})
        @Required(message = "A javax.jms.Destination JNDI name must be specified")
        private String destinationBindingName;

        @PluginBuilderAttribute
        private String userName;

        @PluginBuilderAttribute(sensitive = true)
        private char[] password;

        @PluginBuilderAttribute
        private long reconnectIntervalMillis = DEFAULT_RECONNECT_INTERVAL_MILLIS;

        @PluginBuilderAttribute
        private boolean immediateFail;

        // Programmatic access only for now.
        private JmsManager jmsManager;

        private Builder() {}

        @SuppressWarnings("resource") // actualJmsManager and jndiManager are managed by the JmsAppender
        @Override
        public JmsAppender build() {
            JmsManager actualJmsManager = jmsManager;
            JmsManagerConfiguration configuration = null;
            if (actualJmsManager == null) {
                final Properties jndiProperties = JndiManager.createProperties(
                        factoryName, providerUrl, urlPkgPrefixes, securityPrincipalName, securityCredentials, null);
                configuration = new JmsManagerConfiguration(
                        jndiProperties,
                        factoryBindingName,
                        destinationBindingName,
                        userName,
                        password,
                        false,
                        reconnectIntervalMillis);
                actualJmsManager = AbstractManager.getManager(getName(), JmsManager.FACTORY, configuration);
            }
            if (actualJmsManager == null) {
                // JmsManagerFactory has already logged an ERROR.
                return null;
            }
            final Layout<? extends Serializable> layout = getLayout();
            if (layout == null) {
                LOGGER.error("No layout provided for JmsAppender");
                return null;
            }
            try {
                return new JmsAppender(
                        getName(), getFilter(), layout, isIgnoreExceptions(), getPropertyArray(), actualJmsManager);
            } catch (final JMSException e) {
                // Never happens since the ctor no longer actually throws a JMSException.
                throw new IllegalStateException(e);
            }
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

        public Builder setImmediateFail(final boolean immediateFail) {
            this.immediateFail = immediateFail;
            return this;
        }

        public Builder setJmsManager(final JmsManager jmsManager) {
            this.jmsManager = jmsManager;
            return this;
        }

        public Builder setPassword(final char[] password) {
            this.password = password;
            return this;
        }

        /**
         * @deprecated Use setPassword(char[])
         */
        @Deprecated
        public Builder setPassword(final String password) {
            this.password = password == null ? null : password.toCharArray();
            return this;
        }

        public Builder setProviderUrl(final String providerUrl) {
            this.providerUrl = providerUrl;
            return this;
        }

        public Builder setReconnectIntervalMillis(final long reconnectIntervalMillis) {
            this.reconnectIntervalMillis = reconnectIntervalMillis;
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
            return "Builder [name=" + getName() + ", factoryName=" + factoryName + ", providerUrl=" + providerUrl
                    + ", urlPkgPrefixes=" + urlPkgPrefixes + ", securityPrincipalName=" + securityPrincipalName
                    + ", securityCredentials=" + securityCredentials + ", factoryBindingName=" + factoryBindingName
                    + ", destinationBindingName=" + destinationBindingName + ", username=" + userName + ", layout="
                    + getLayout() + ", filter=" + getFilter() + ", ignoreExceptions=" + isIgnoreExceptions()
                    + ", jmsManager=" + jmsManager + "]";
        }
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    private volatile JmsManager manager;

    /**
     * Constructs a new instance.
     *
     * @throws JMSException not thrown as of 2.9 but retained in the signature for compatibility, will be removed in 3.0
     */
    protected JmsAppender(
            final String name,
            final Filter filter,
            final Layout<? extends Serializable> layout,
            final boolean ignoreExceptions,
            final Property[] properties,
            final JmsManager manager)
            throws JMSException {
        super(name, filter, layout, ignoreExceptions, properties);
        this.manager = manager;
    }

    /**
     * Constructs a new instance.
     *
     * @throws JMSException not thrown as of 2.9 but retained in the signature for compatibility, will be removed in 3.0
     * @deprecated Use {@link #JmsAppender(String, Filter, Layout, boolean, Property[], JmsManager)}.
     */
    @Deprecated
    protected JmsAppender(
            final String name,
            final Filter filter,
            final Layout<? extends Serializable> layout,
            final boolean ignoreExceptions,
            final JmsManager manager)
            throws JMSException {
        super(name, filter, layout, ignoreExceptions, Property.EMPTY_ARRAY);
        this.manager = manager;
    }

    @Override
    public void append(final LogEvent event) {
        this.manager.send(event, toSerializable(event));
    }

    public JmsManager getManager() {
        return manager;
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
