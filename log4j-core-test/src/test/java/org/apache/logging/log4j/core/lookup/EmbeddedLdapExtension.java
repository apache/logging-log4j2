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
package org.apache.logging.log4j.core.lookup;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.LDAPException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import javax.naming.ldap.LdapContext;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.zapodot.junit.ldap.internal.jndi.ContextProxyFactory;

public class EmbeddedLdapExtension implements BeforeEachCallback, AfterEachCallback {

    private static final String LDIF_FILENAME = "JndiRestrictedLookup.ldif";
    private static final String JAVA_RT_CONTROL_FACTORY = "com.sun.jndi.ldap.DefaultResponseControlFactory";
    private static final String JAVA_RT_CONTEXT_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";
    public static final String DEFAULT_BIND_DSN = "cn=Directory manager";
    public static final String DEFAULT_BIND_CREDENTIALS = "password";
    public static final String LDAP_SERVER_LISTENER_NAME = "test-listener";

    private String domainDsn;
    private InMemoryDirectoryServer ldapServer = null;
    private InitialDirContext initialDirContext = null;

    public EmbeddedLdapExtension(String domainDsn) {
        this.domainDsn = domainDsn;
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        startLdapServer();
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        shutdownLdapServer();
    }

    void startLdapServer() throws LDAPException, NamingException, UnsupportedEncodingException {
        InMemoryDirectoryServerConfig config = getConfig();

        ldapServer = new InMemoryDirectoryServer(config);
        String path = Resources.getResource(LDIF_FILENAME).getPath();
        ldapServer.importFromLDIF(false, URLDecoder.decode(path, Charsets.UTF_8.name()));
        ldapServer.startListening();

        initialDirContext = buildInitialDirContext();
    }

    private InMemoryDirectoryServerConfig getConfig() throws LDAPException {
        String[] domainDsnArray = new String[] {this.domainDsn};
        InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig(domainDsnArray);

        config.addAdditionalBindCredentials(DEFAULT_BIND_DSN, DEFAULT_BIND_CREDENTIALS);

        InetAddress bindAddress = InetAddress.getLoopbackAddress();
        Integer bindPort = 0;
        InMemoryListenerConfig listenerConfig =
                InMemoryListenerConfig.createLDAPConfig(LDAP_SERVER_LISTENER_NAME, bindAddress, bindPort, null);
        config.setListenerConfigs(listenerConfig);

        config.setSchema(null);

        return config;
    }

    private InitialDirContext buildInitialDirContext() throws NamingException {
        Hashtable<String, String> environment = new Hashtable<>();

        environment.put(LdapContext.CONTROL_FACTORIES, JAVA_RT_CONTROL_FACTORY);
        environment.put(Context.INITIAL_CONTEXT_FACTORY, JAVA_RT_CONTEXT_FACTORY);

        environment.put(
                Context.PROVIDER_URL,
                String.format("ldap://%s:%s", ldapServer.getListenAddress().getHostName(), embeddedServerPort()));

        return new InitialDirContext(environment);
    }

    void shutdownLdapServer() {
        try {
            if (initialDirContext != null) {
                initialDirContext.close();
            }
        } catch (NamingException e) {
            // logger.info("Could not close initial context, forcing server shutdown anyway", e);
        } finally {
            ldapServer.shutDown(true);
        }
    }

    int embeddedServerPort() {
        return ldapServer.getListenPort();
    }

    Context context() {
        return ContextProxyFactory.asDelegatingContext(initialDirContext);
    }
}
