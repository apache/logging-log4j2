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

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.naming.Context;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.Referenceable;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.util.JndiCloser;
import org.apache.logging.log4j.core.util.NetUtils;
import org.apache.logging.log4j.util.PropertiesUtil;

/**
 * Manages a JNDI {@link javax.naming.directory.DirContext}.
 *
 * @since 2.1
 */
public class JndiManager extends AbstractManager {

    public static final String ALLOWED_HOSTS = "allowedLdapHosts";
    public static final String ALLOWED_CLASSES = "allowedLdapClasses";

    private static final JndiManagerFactory FACTORY = new JndiManagerFactory();
    private static final String PREFIX = "log4j2.";
    private static final List<String> permanentAllowedHosts = new ArrayList<>();
    private static final List<String> permanentAllowedClasses = new ArrayList<>();
    private static final String LDAP = "ldap";
    private static final String SERIALIZED_DATA = "javaserializeddata";
    private static final String CLASS_NAME = "javaclassname";
    private static final String REFERENCE_ADDRESS = "javareferenceaddress";
    private static final String OBJECT_FACTORY = "javafactory";
    private final List<String> allowedHosts;
    private final List<String> allowedClasses;

    static {
        permanentAllowedHosts.addAll(NetUtils.getLocalIps());
        permanentAllowedClasses.add(Boolean.class.getName());
        permanentAllowedClasses.add(Byte.class.getName());
        permanentAllowedClasses.add(Character.class.getName());
        permanentAllowedClasses.add(Double.class.getName());
        permanentAllowedClasses.add(Float.class.getName());
        permanentAllowedClasses.add(Integer.class.getName());
        permanentAllowedClasses.add(Long.class.getName());
        permanentAllowedClasses.add(Number.class.getName());
        permanentAllowedClasses.add(Short.class.getName());
        permanentAllowedClasses.add(String.class.getName());
    }


    private final DirContext context;

    private JndiManager(final String name, final DirContext context, final List<String> allowedHosts,
            final List<String> allowedClasses) {
        super(null, name);
        this.context = context;
        this.allowedHosts = allowedHosts;
        this.allowedClasses = allowedClasses;
    }

    /**
     * Gets the default JndiManager using the default {@link javax.naming.InitialContext}.
     *
     * @return the default JndiManager
     */
    public static JndiManager getDefaultManager() {
        return getManager(JndiManager.class.getName(), FACTORY, null);
    }

    /**
     * Gets a named JndiManager using the default {@link javax.naming.InitialContext}.
     *
     * @param name the name of the JndiManager instance to create or use if available
     * @return a default JndiManager
     */
    public static JndiManager getDefaultManager(final String name) {
        return getManager(name, FACTORY, null);
    }

    /**
     * Gets a JndiManager with the provided configuration information.
     *
     * @param initialContextFactoryName Fully qualified class name of an implementation of
     *                                  {@link javax.naming.spi.InitialContextFactory}.
     * @param providerURL               The provider URL to use for the JNDI connection (specific to the above factory).
     * @param urlPkgPrefixes            A colon-separated list of package prefixes for the class name of the factory
     *                                  class that will create a URL context factory
     * @param securityPrincipal         The name of the identity of the Principal.
     * @param securityCredentials       The security credentials of the Principal.
     * @param additionalProperties      Any additional JNDI environment properties to set or {@code null} for none.
     * @return the JndiManager for the provided parameters.
     */
    public static JndiManager getJndiManager(final String initialContextFactoryName,
            final String providerURL,
            final String urlPkgPrefixes,
            final String securityPrincipal,
            final String securityCredentials,
            final Properties additionalProperties) {
        final Properties properties = createProperties(initialContextFactoryName, providerURL, urlPkgPrefixes,
                securityPrincipal, securityCredentials, additionalProperties);
        return getManager(createManagerName(), FACTORY, properties);
    }

    /**
     * Gets a JndiManager with the provided configuration information.
     *
     * @param properties JNDI properties, usually created by calling {@link #createProperties(String, String, String, String, String, Properties)}.
     * @return the JndiManager for the provided parameters.
     * @see #createProperties(String, String, String, String, String, Properties)
     * @since 2.9
     */
    public static JndiManager getJndiManager(final Properties properties) {
        return getManager(createManagerName(), FACTORY, properties);
    }

    private static String createManagerName() {
        return JndiManager.class.getName() + '@' + JndiManager.class.hashCode();
    }

    /**
     * Creates JNDI Properties with the provided configuration information.
     *
     * @param initialContextFactoryName
     *            Fully qualified class name of an implementation of {@link javax.naming.spi.InitialContextFactory}.
     * @param providerURL
     *            The provider URL to use for the JNDI connection (specific to the above factory).
     * @param urlPkgPrefixes
     *            A colon-separated list of package prefixes for the class name of the factory class that will create a
     *            URL context factory
     * @param securityPrincipal
     *            The name of the identity of the Principal.
     * @param securityCredentials
     *            The security credentials of the Principal.
     * @param additionalProperties
     *            Any additional JNDI environment properties to set or {@code null} for none.
     * @return the Properties for the provided parameters.
     * @since 2.9
     */
    public static Properties createProperties(final String initialContextFactoryName, final String providerURL,
            final String urlPkgPrefixes, final String securityPrincipal, final String securityCredentials,
            final Properties additionalProperties) {
        if (initialContextFactoryName == null) {
            return null;
        }
        final Properties properties = new Properties();
        properties.setProperty(Context.INITIAL_CONTEXT_FACTORY, initialContextFactoryName);
        if (providerURL != null) {
            properties.setProperty(Context.PROVIDER_URL, providerURL);
        } else {
            LOGGER.warn("The JNDI InitialContextFactory class name [{}] was provided, but there was no associated "
                    + "provider URL. This is likely to cause problems.", initialContextFactoryName);
        }
        if (urlPkgPrefixes != null) {
            properties.setProperty(Context.URL_PKG_PREFIXES, urlPkgPrefixes);
        }
        if (securityPrincipal != null) {
            properties.setProperty(Context.SECURITY_PRINCIPAL, securityPrincipal);
            if (securityCredentials != null) {
                properties.setProperty(Context.SECURITY_CREDENTIALS, securityCredentials);
            } else {
                LOGGER.warn("A security principal [{}] was provided, but with no corresponding security credentials.",
                        securityPrincipal);
            }
        }
        if (additionalProperties != null) {
            properties.putAll(additionalProperties);
        }
        return properties;
    }

    @Override
    protected boolean releaseSub(final long timeout, final TimeUnit timeUnit) {
        return JndiCloser.closeSilently(this.context);
    }

    /**
     * Looks up a named object through this JNDI context.
     *
     * @param name name of the object to look up.
     * @param <T>  the type of the object.
     * @return the named object if it could be located.
     * @throws  NamingException if a naming exception is encountered
     */
    @SuppressWarnings("unchecked")
    public synchronized <T> T lookup(final String name) throws NamingException {
        try {
            URI uri = new URI(name);
            if (LDAP.equalsIgnoreCase(uri.getScheme())) {
                if (!allowedHosts.contains(uri.getHost())) {
                    LOGGER.warn("Attempt to access ldap server not in allowed list");
                    return null;
                }
                Attributes attributes = this.context.getAttributes(name);
                if (attributes != null) {
                    Attribute classNameAttr = attributes.get(CLASS_NAME);
                    if (attributes.get(SERIALIZED_DATA) != null) {
                        if (classNameAttr != null) {
                            String className = classNameAttr.get().toString();
                            if (!allowedClasses.contains(className)) {
                                LOGGER.warn("Deserialization of {} is not allowed", className);
                                return null;
                            }
                        } else {
                            LOGGER.warn("No class name provided for {}", name);
                            return null;
                        }
                    } else if (attributes.get(REFERENCE_ADDRESS) != null || attributes.get(OBJECT_FACTORY) != null){
                        LOGGER.warn("Referenceable class is not allowed for {}", name);
                        return null;
                    }
                }
            }
        } catch (URISyntaxException ex) {
            // This is OK.
        }
        return (T) this.context.lookup(name);
    }

    private static class JndiManagerFactory implements ManagerFactory<JndiManager, Properties> {

        @Override
        public JndiManager createManager(final String name, final Properties data) {
            String hosts = data != null ? data.getProperty(ALLOWED_HOSTS) : null;
            String classes = data != null ? data.getProperty(ALLOWED_CLASSES) : null;
            List<String> allowedHosts = new ArrayList<>();
            List<String> allowedClasses = new ArrayList<>();
            addAll(hosts, allowedHosts, permanentAllowedHosts, ALLOWED_HOSTS, data);
            addAll(classes, allowedClasses, permanentAllowedClasses, ALLOWED_CLASSES, data);
            try {
                return new JndiManager(name, new InitialDirContext(data), allowedHosts, allowedClasses);
            } catch (final NamingException e) {
                LOGGER.error("Error creating JNDI InitialContext.", e);
                return null;
            }
        }

        private void addAll(String toSplit, List<String> list, List<String> permanentList, String propertyName,
                Properties data) {
            if (toSplit != null) {
                list.addAll(Arrays.asList(toSplit.split("\\s*,\\s*")));
                data.remove(propertyName);
            }
            toSplit = PropertiesUtil.getProperties().getStringProperty(PREFIX + propertyName);
            if (toSplit != null) {
                list.addAll(Arrays.asList(toSplit.split("\\s*,\\s*")));
            }
            list.addAll(permanentList);
        }
    }

    @Override
    public String toString() {
        return "JndiManager [context=" + context + ", count=" + count + "]";
    }

}
