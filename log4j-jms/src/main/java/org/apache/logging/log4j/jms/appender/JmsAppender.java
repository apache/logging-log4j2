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

package org.apache.logging.log4j.jms.appender;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.jms.appender.JmsManager.JmsManagerConfiguration;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAliases;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.plugins.validation.constraints.Required;

import javax.jms.JMSException;
import java.io.Serializable;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Generic JMS Appender plugin for both queues and topics. This Appender replaces the previous split ones. However,
 * configurations set up for the 2.0 version of the JMS appenders will still work.
 */
@Configurable(elementType = Appender.ELEMENT_TYPE, printObject = true)
@Plugin("JMS")
@PluginAliases({ "JMSQueue", "JMSTopic" })
public class JmsAppender extends AbstractAppender {

    public static class Builder<B extends Builder<B>> extends AbstractAppender.Builder<B>
            implements org.apache.logging.log4j.plugins.util.Builder<JmsAppender> {

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
        @PluginAliases({ "queueBindingName", "topicBindingName" })
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

        private Builder() {
        }

        @SuppressWarnings("resource") // actualJmsManager and jndiManager are managed by the JmsAppender
        @Override
        public JmsAppender build() {
            if (!Constants.JNDI_JMS_ENABLED) {
                LOGGER.error("JNDI has not been enabled. The log4j2.enableJndi property must be set to true");
                return null;
            }
            JmsManager actualJmsManager = jmsManager;
            JmsManagerConfiguration configuration = null;
            if (actualJmsManager == null) {
                Properties additionalProperties = null;
                final Properties jndiProperties = JmsManager.createJndiProperties(factoryName, providerUrl,
                        urlPkgPrefixes, securityPrincipalName, securityCredentials, additionalProperties);
                configuration = new JmsManagerConfiguration(jndiProperties, factoryBindingName, destinationBindingName,
                        userName, password, false, reconnectIntervalMillis);
                actualJmsManager = AbstractManager.getManager(getName(), JmsManager.FACTORY, configuration);
            }
            if (actualJmsManager == null) {
                // JmsManagerFactory has already logged an ERROR.
                return null;
            }
            if (getLayout() == null) {
                LOGGER.error("No layout provided for JmsAppender");
                return null;
            }
            try {
                return new JmsAppender(getName(), getFilter(), getLayout(), isIgnoreExceptions(), getPropertyArray(), actualJmsManager);
            } catch (final JMSException e) {
                //  Never happens since the ctor no longer actually throws a JMSException.
                throw new IllegalStateException(e);
            }
        }

        public B setDestinationBindingName(final String destinationBindingName) {
            this.destinationBindingName = destinationBindingName;
            return asBuilder();
        }

        public B setFactoryBindingName(final String factoryBindingName) {
            this.factoryBindingName = factoryBindingName;
            return asBuilder();
        }

        public B setFactoryName(final String factoryName) {
            this.factoryName = factoryName;
            return asBuilder();
        }

        public B setImmediateFail(final boolean immediateFail) {
            this.immediateFail = immediateFail;
            return asBuilder();
        }

        public B setJmsManager(final JmsManager jmsManager) {
            this.jmsManager = jmsManager;
            return asBuilder();
        }

        public B setPassword(final char[] password) {
            this.password = password;
            return asBuilder();
        }

        public B setProviderUrl(final String providerUrl) {
            this.providerUrl = providerUrl;
            return asBuilder();
        }

        public B setReconnectIntervalMillis(final long reconnectIntervalMillis) {
            this.reconnectIntervalMillis = reconnectIntervalMillis;
            return asBuilder();
        }

        public B setSecurityCredentials(final String securityCredentials) {
            this.securityCredentials = securityCredentials;
            return asBuilder();
        }

        public B setSecurityPrincipalName(final String securityPrincipalName) {
            this.securityPrincipalName = securityPrincipalName;
            return asBuilder();
        }

        public B setUrlPkgPrefixes(final String urlPkgPrefixes) {
            this.urlPkgPrefixes = urlPkgPrefixes;
            return asBuilder();
        }

        public B setUserName(final String userName) {
            this.userName = userName;
            return asBuilder();
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

    @PluginFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    private final JmsManager manager;

    /**
     * @param name The Appender's name.
     * @param filter The filter to attach to the Appender, if any.
     * @param layout The layout to use to render the event.
     * @param ignoreExceptions true if exceptions should be ignore, false otherwise.
     * @param properties TODO
     * @param manager The JMSManager.
     * @throws JMSException
     *             not thrown as of 2.9 but retained in the signature for compatibility, will be removed in 3.0.
     */
    protected JmsAppender(final String name, final Filter filter, final Layout<? extends Serializable> layout,
            final boolean ignoreExceptions, Property[] properties, final JmsManager manager) throws JMSException {
        super(name, filter, layout, ignoreExceptions, properties);
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
