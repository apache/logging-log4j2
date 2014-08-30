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
package org.apache.logging.log4j.mom.jms.manager;

import java.io.Serializable;
import java.util.Properties;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.apache.logging.log4j.core.appender.AbstractManager;

/**
 * Base Class for Managers of JMS connections.
 */
public abstract class AbstractJmsManager extends AbstractManager {

    /**
     * The Constructor.
     * @param name The name of the Appender.
     */
    public AbstractJmsManager(final String name) {
        super(name);
    }

    /**
     * Create the InitialContext.
     * @param factoryName The fully qualified class name of the InitialContextFactory.
     * @param providerURL The URL of the provider to use.
     * @param urlPkgPrefixes A colon-separated list of package prefixes for the class name of the factory class that
     * will create a URL context factory
     * @param securityPrincipalName The name of the identity of the Principal.
     * @param securityCredentials The security credentials of the Principal.
     * @return the InitialContext.
     * @throws NamingException if a naming error occurs.
     */
    protected static Context createContext(final String factoryName, final String providerURL,
                                           final String urlPkgPrefixes, final String securityPrincipalName,
                                           final String securityCredentials)
        throws NamingException {

        final Properties props = getEnvironment(factoryName, providerURL, urlPkgPrefixes, securityPrincipalName,
                                          securityCredentials);
        return new InitialContext(props);
    }

    /**
     * Looks up the name in the context.
     * @param ctx The Context.
     * @param name The name to locate.
     * @return The object to be located.
     * @throws NamingException If an error occurs locating the name.
     */
    protected static Object lookup(final Context ctx, final String name) throws NamingException {
        try {
            return ctx.lookup(name);
        } catch (final NameNotFoundException e) {
            LOGGER.warn("Could not find name [{}].", name);
            throw e;
        }
    }

    /**
     * Sets up the properties to pass to the InitialContext.
     * @param factoryName The fully qualified class name of the InitialContextFactory.
     * @param providerURL The URL of the provider to use.
     * @param urlPkgPrefixes A colon-separated list of package prefixes for the class name of the factory class that
     * will create a URL context factory
     * @param securityPrincipalName The name of the identity of the Principal.
     * @param securityCredentials The security credentials of the Principal.
     * @return The Properties.
     * @see javax.naming.Context
     */
    protected static Properties getEnvironment(final String factoryName, final String providerURL,
                                               final String urlPkgPrefixes, final String securityPrincipalName,
                                               final String securityCredentials) {
        final Properties props = new Properties();
        if (factoryName != null) {
            props.setProperty(Context.INITIAL_CONTEXT_FACTORY, factoryName);
            if (providerURL != null) {
                props.setProperty(Context.PROVIDER_URL, providerURL);
            } else {
                LOGGER.warn("The InitialContext factory name has been provided without a ProviderURL. " +
                    "This is likely to cause problems");
            }
            if (urlPkgPrefixes != null) {
                props.setProperty(Context.URL_PKG_PREFIXES, urlPkgPrefixes);
            }
            if (securityPrincipalName != null) {
                props.setProperty(Context.SECURITY_PRINCIPAL, securityPrincipalName);
                if (securityCredentials != null) {
                    props.setProperty(Context.SECURITY_CREDENTIALS, securityCredentials);
                } else {
                    LOGGER.warn("SecurityPrincipalName has been set without SecurityCredentials. " +
                        "This is likely to cause problems.");
                }
            }
            return props;
        }
        return null;
    }

    /**
     * Send the message.
     * @param object The Object to sent.
     * @throws Exception if an error occurs.
     */
    public abstract void send(Serializable object) throws Exception;

    /**
     * Send the Object.
     * @param object The Object to send.
     * @param session The Session.
     * @param producer The MessageProducer.
     * @throws Exception if an error occurs.
     */
    public synchronized void send(final Serializable object, final Session session, final MessageProducer producer)
        throws Exception {
        try {
            Message msg;
            if (object instanceof String) {
                msg = session.createTextMessage();
                ((TextMessage) msg).setText((String) object);
            } else {
                msg = session.createObjectMessage();
                ((ObjectMessage) msg).setObject(object);
            }
            producer.send(msg);
        } catch (final JMSException ex) {
            LOGGER.error("Could not publish message via JMS {}", getName());
            throw ex;
        }
    }
}
