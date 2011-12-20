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

import org.apache.logging.log4j.core.appender.AbstractManager;

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
import java.io.Serializable;
import java.util.Properties;

/**
 *
 */
public abstract class AbstractJMSManager extends AbstractManager {

    public AbstractJMSManager(String name) {
        super(name);
    }

    protected static Context createContext(String factoryName, String providerURL, String urlPkgPrefixes,
                                           String securityPrincipalName, String securityCredentials)
        throws NamingException {

        Properties props = getEnvironment(factoryName, providerURL, urlPkgPrefixes, securityPrincipalName,
                                          securityCredentials);
        return new InitialContext(props);
    }

    protected static Object lookup(Context ctx, String name) throws NamingException {
        try {
            return ctx.lookup(name);
        } catch(NameNotFoundException e) {
            LOGGER.error("Could not find name [" + name + "].");
            throw e;
        }
    }

    protected static Properties getEnvironment(String factoryName, String providerURL, String urlPkgPrefixes,
                                               String securityPrincipalName, String securityCredentials) {
        Properties props = new Properties();
        if (factoryName != null) {
            props.put(Context.INITIAL_CONTEXT_FACTORY, factoryName);
            if (providerURL != null) {
                props.put(Context.PROVIDER_URL, providerURL);
            } else {
                LOGGER.warn("The InitalContext factory name has been provided without a ProviderURL. " +
                    "This is likely to cause problems");
            }
            if (urlPkgPrefixes != null) {
                props.put(Context.URL_PKG_PREFIXES, urlPkgPrefixes);
            }
	          if (securityPrincipalName != null) {
	              props.put(Context.SECURITY_PRINCIPAL, securityPrincipalName);
	              if (securityCredentials != null) {
	                  props.put(Context.SECURITY_CREDENTIALS, securityCredentials);
	              } else {
	                  LOGGER.warn("SecurityPrincipalName has been set without SecurityCredentials. " +
                        "This is likely to cause problems.");
	              }
	          }
            return props;
        }
        return null;
    }

    public abstract void send(Serializable Object) throws Exception;


    public synchronized void send(Serializable object, Session session, MessageProducer producer) throws Exception {
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
        } catch (JMSException ex) {
            LOGGER.error("Could not publish message via JMS " + getName());
            throw ex;
        }
    }
}
