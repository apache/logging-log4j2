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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
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
    public static final String ALLOWED_PROTOCOLS = "allowedJndiProtocols";

    private static final JndiManagerFactory FACTORY = new JndiManagerFactory();
    private static final String PREFIX = "log4j2.";
    private static final String LDAP = "ldap";
    private static final String LDAPS = "ldaps";
    private static final String JAVA = "java";
    private static final List<String> permanentAllowedHosts = NetUtils.getLocalIps();
    private static final List<String> permanentAllowedClasses = Arrays.asList(Boolean.class.getName(),
            Byte.class.getName(), Character.class.getName(), Double.class.getName(), Float.class.getName(),
            Integer.class.getName(), Long.class.getName(), Short.class.getName(), String.class.getName());
    private static final List<String> permanentAllowedProtocols = Arrays.asList(JAVA, LDAP, LDAPS);
    private static final String SERIALIZED_DATA = "javaSerializedData";
    private static final String CLASS_NAME = "javaClassName";
    private static final String REFERENCE_ADDRESS = "javaReferenceAddress";
    private static final String OBJECT_FACTORY = "javaFactory";
    private final List<String> allowedHosts;
    private final List<String> allowedClasses;
    private final List<String> allowedProtocols;

    private final DirContext context;

    private JndiManager(final String name, final DirContext context, final List<String> allowedHosts,
            final List<String> allowedClasses, final List<String> allowedProtocols) {
        super(null, name);
        this.context = context;
        this.allowedHosts = allowedHosts;
        this.allowedClasses = allowedClasses;
        this.allowedProtocols = allowedProtocols;
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
            if (uri.getScheme() != null) {
                if (!allowedProtocols.contains(uri.getScheme().toLowerCase(Locale.ROOT))) {
                    LOGGER.warn("Log4j JNDI does not allow protocol {}", uri.getScheme());
                    return null;
                }
                if (LDAP.equalsIgnoreCase(uri.getScheme()) || LDAPS.equalsIgnoreCase(uri.getScheme())) {
                    if (!allowedHosts.contains(uri.getHost())) {
                        LOGGER.warn("Attempt to access ldap server not in allowed list");
                        return null;
                    }
                    Attributes attributes = this.context.getAttributes(name);
                    if (attributes != null) {
                        // In testing the "key" for attributes seems to be lowercase while the attribute id is
                        // camelcase, but that may just be true for the test LDAP used here. This copies the Attributes
                        // to a Map ignoring the "key" and using the Attribute's id as the key in the Map so it matches
                        // the Java schema.
                        Map<String, Attribute> attributeMap = new HashMap<>();
                        NamingEnumeration<? extends Attribute> enumeration = attributes.getAll();
                        while (enumeration.hasMore()) {
                            Attribute attribute = enumeration.next();
                            attributeMap.put(attribute.getID(), attribute);
                        }
                        Attribute classNameAttr = attributeMap.get(CLASS_NAME);
                        if (attributeMap.get(SERIALIZED_DATA) != null) {
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
                        } else if (attributeMap.get(REFERENCE_ADDRESS) != null
                                || attributeMap.get(OBJECT_FACTORY) != null) {
                            LOGGER.warn("Referenceable class is not allowed for {}", name);
                            return null;
                        }
                    }
                }
            }
        } catch (URISyntaxException ex) {
            LOGGER.warn("Invalid JNDI URI - {}", name);
            return null;
        }
        return (T) this.context.lookup(name);
    }

    private static class JndiManagerFactory implements ManagerFactory<JndiManager, Properties> {

        @Override
        public JndiManager createManager(final String name, final Properties data) {
            String hosts = data != null ? data.getProperty(ALLOWED_HOSTS) : null;
            String classes = data != null ? data.getProperty(ALLOWED_CLASSES) : null;
            String protocols = data != null ? data.getProperty(ALLOWED_PROTOCOLS) : null;
            List<String> allowedHosts = new ArrayList<>();
            List<String> allowedClasses = new ArrayList<>();
            List<String> allowedProtocols = new ArrayList<>();
            addAll(hosts, allowedHosts, permanentAllowedHosts, ALLOWED_HOSTS, data);
            addAll(classes, allowedClasses, permanentAllowedClasses, ALLOWED_CLASSES, data);
            addAll(protocols, allowedProtocols, permanentAllowedProtocols, ALLOWED_PROTOCOLS, data);
            try {
                return new JndiManager(name, new InitialDirContext(data), allowedHosts, allowedClasses,
                        allowedProtocols);
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
