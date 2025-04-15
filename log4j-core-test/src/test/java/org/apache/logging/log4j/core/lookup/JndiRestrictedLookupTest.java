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

import static org.junit.Assert.fail;

import java.io.Serializable;
import javax.naming.Context;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;
import org.apache.logging.log4j.message.Message;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.zapodot.junit.ldap.EmbeddedLdapRule;
import org.zapodot.junit.ldap.EmbeddedLdapRuleBuilder;

/**
 * JndiLookupTest
 */
public class JndiRestrictedLookupTest {

    private static final String LDAP_URL = "ldap://127.0.0.1:";
    private static final String RESOURCE = "JndiExploit";
    private static final String TEST_STRING = "TestString";
    private static final String TEST_MESSAGE = "TestMessage";
    private static final String LEVEL = "TestLevel";
    private static final String DOMAIN_DSN = "dc=apache,dc=org";
    private static final String DOMAIN = "apache.org";

    @Rule
    public EmbeddedLdapRule embeddedLdapRule = EmbeddedLdapRuleBuilder.newInstance()
            .usingDomainDsn(DOMAIN_DSN)
            .importingLdifs("JndiRestrictedLookup.ldif")
            .build();

    @BeforeClass
    public static void beforeClass() {
        System.setProperty("log4j2.enableJndiLookup", "true");
    }

    @Test
    @SuppressWarnings("BanJNDI")
    public void testBadUriLookup() throws Exception {
        final int port = embeddedLdapRule.embeddedServerPort();
        final Context context = embeddedLdapRule.context();
        context.bind("cn=" + RESOURCE + "," + DOMAIN_DSN, new Fruit("Test Message"));
        final StrLookup lookup = new JndiLookup();
        final String result = lookup.lookup(
                LDAP_URL + port + "/" + "cn=" + RESOURCE + "," + DOMAIN_DSN + "?Type=A Type&Name=1100110&Char=!");
        if (result != null) {
            fail("Lookup returned an object");
        }
    }

    @Test
    @SuppressWarnings("BanJNDI")
    public void testReferenceLookup() throws Exception {
        final int port = embeddedLdapRule.embeddedServerPort();
        final Context context = embeddedLdapRule.context();
        context.bind("cn=" + RESOURCE + "," + DOMAIN_DSN, new Fruit("Test Message"));
        final StrLookup lookup = new JndiLookup();
        final String result = lookup.lookup(LDAP_URL + port + "/" + "cn=" + RESOURCE + "," + DOMAIN_DSN);
        if (result != null) {
            fail("Lookup returned an object");
        }
    }

    @Test
    @SuppressWarnings("BanJNDI")
    public void testSerializableLookup() throws Exception {
        final int port = embeddedLdapRule.embeddedServerPort();
        final Context context = embeddedLdapRule.context();
        context.bind("cn=" + TEST_STRING + "," + DOMAIN_DSN, "Test Message");
        final StrLookup lookup = new JndiLookup();
        final String result = lookup.lookup(LDAP_URL + port + "/" + "cn=" + TEST_STRING + "," + DOMAIN_DSN);
        if (result != null) {
            fail("LDAP is enabled");
        }
    }

    @Test
    @SuppressWarnings("BanJNDI")
    public void testBadSerializableLookup() throws Exception {
        final int port = embeddedLdapRule.embeddedServerPort();
        final Context context = embeddedLdapRule.context();
        context.bind("cn=" + TEST_MESSAGE + "," + DOMAIN_DSN, new SerializableMessage("Test Message"));
        final StrLookup lookup = new JndiLookup();
        final String result = lookup.lookup(LDAP_URL + port + "/" + "cn=" + TEST_MESSAGE + "," + DOMAIN_DSN);
        if (result != null) {
            fail("Lookup returned an object");
        }
    }

    @Test
    public void testDnsLookup() {
        final StrLookup lookup = new JndiLookup();
        final String result = lookup.lookup("dns:/" + DOMAIN);
        if (result != null) {
            fail("No DNS data returned");
        }
    }

    static class Fruit implements Referenceable {
        String fruit;

        public Fruit(final String f) {
            fruit = f;
        }

        public Reference getReference() {

            return new Reference(
                    Fruit.class.getName(),
                    new StringRefAddr("fruit", fruit),
                    JndiExploit.class.getName(),
                    null); // factory location
        }

        public String toString() {
            return fruit;
        }
    }

    static class SerializableMessage implements Serializable, Message {
        private final String message;

        SerializableMessage(final String message) {
            this.message = message;
        }

        @Override
        public String getFormattedMessage() {
            return message;
        }

        @Override
        public Object[] getParameters() {
            return null;
        }

        @Override
        public Throwable getThrowable() {
            return null;
        }
    }
}
