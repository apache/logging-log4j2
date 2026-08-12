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
package org.apache.logging.log4j.smtp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import java.lang.reflect.Method;
import java.util.Properties;
import javax.net.ssl.SSLSocketFactory;
import org.apache.logging.log4j.core.net.MailManager;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.junit.jupiter.api.Test;

class SmtpManagerFactoryTest {

    @Test
    void createManagerSetsSmtpSessionProperties() {
        final MailManager.FactoryData data = SmtpTestSupport.createDefaultFactoryData();
        final SmtpManager manager = SmtpTestSupport.createManager(data);
        final Session session = SmtpTestSupport.getSession(manager);
        final Properties properties = session.getProperties();

        assertEquals("smtp", properties.getProperty("mail.transport.protocol"));
        assertEquals(SmtpMockFixtures.MOCK_HOST, properties.getProperty("mail.smtp.host"));
        assertEquals(String.valueOf(SmtpMockFixtures.MOCK_PORT), properties.getProperty("mail.smtp.port"));
        assertEquals("true", properties.getProperty("mail.smtp.auth"));
        assertFalse(session.getDebug());
    }

    @Test
    void createManagerWithoutCredentialsDoesNotEnableAuth() {
        final MailManager.FactoryData data = SmtpTestSupport.createFactoryData(
                SmtpMockFixtures.MOCK_TO,
                null,
                null,
                SmtpMockFixtures.MOCK_FROM,
                null,
                "Subject",
                "smtp",
                SmtpMockFixtures.MOCK_HOST,
                SmtpMockFixtures.MOCK_PORT,
                null,
                null,
                false,
                1,
                null);
        final Session session = SmtpTestSupport.getSession(SmtpTestSupport.createManager(data));

        assertNull(session.getProperties().getProperty("mail.smtp.auth"));
    }

    @Test
    void buildAuthenticatorReturnsExpectedCredentials() throws Exception {
        final SmtpManager.SMTPManagerFactory factory = new SmtpManager.SMTPManagerFactory();
        final Method buildAuthenticator = SmtpManager.SMTPManagerFactory.class.getDeclaredMethod(
                "buildAuthenticator", String.class, String.class);
        buildAuthenticator.setAccessible(true);

        final Authenticator authenticator = (Authenticator)
                buildAuthenticator.invoke(factory, SmtpMockFixtures.MOCK_USERNAME, SmtpMockFixtures.MOCK_PASSWORD);
        assertNotNull(authenticator);

        final Method getPasswordAuthentication = Authenticator.class.getDeclaredMethod("getPasswordAuthentication");
        getPasswordAuthentication.setAccessible(true);
        final PasswordAuthentication passwordAuthentication =
                (PasswordAuthentication) getPasswordAuthentication.invoke(authenticator);

        assertEquals(SmtpMockFixtures.MOCK_USERNAME, passwordAuthentication.getUserName());
        assertEquals(SmtpMockFixtures.MOCK_PASSWORD, passwordAuthentication.getPassword());
    }

    @Test
    void buildAuthenticatorReturnsNullWhenUsernameOrPasswordMissing() throws Exception {
        final SmtpManager.SMTPManagerFactory factory = new SmtpManager.SMTPManagerFactory();
        final Method buildAuthenticator = SmtpManager.SMTPManagerFactory.class.getDeclaredMethod(
                "buildAuthenticator", String.class, String.class);
        buildAuthenticator.setAccessible(true);

        assertNull(buildAuthenticator.invoke(factory, null, SmtpMockFixtures.MOCK_PASSWORD));
        assertNull(buildAuthenticator.invoke(factory, SmtpMockFixtures.MOCK_USERNAME, null));
    }

    @Test
    void createManagerAppliesSmtpsSslProperties() throws Exception {
        final SslConfiguration sslConfiguration = SslConfiguration.createSSLConfiguration(null, null, null);
        final MailManager.FactoryData data = SmtpTestSupport.createFactoryData(
                SmtpMockFixtures.MOCK_TO,
                null,
                null,
                SmtpMockFixtures.MOCK_FROM,
                null,
                "Subject",
                "smtps",
                SmtpMockFixtures.MOCK_HOST,
                465,
                SmtpMockFixtures.MOCK_USERNAME,
                SmtpMockFixtures.MOCK_PASSWORD,
                false,
                1,
                sslConfiguration);
        final Session session = SmtpTestSupport.getSession(SmtpTestSupport.createManager(data));
        final Properties properties = session.getProperties();

        assertEquals(SmtpMockFixtures.MOCK_HOST, properties.getProperty("mail.smtps.host"));
        assertEquals("465", properties.getProperty("mail.smtps.port"));
        assertEquals("true", properties.getProperty("mail.smtps.auth"));
        assertTrue(properties.get("mail.smtps.ssl.socketFactory") instanceof SSLSocketFactory);
        assertEquals(
                Boolean.toString(sslConfiguration.isVerifyHostName()),
                properties.getProperty("mail.smtps.ssl.checkserveridentity"));
    }

    @Test
    void mockMailSessionPropertiesResourceMatchesFactoryDefaults() throws Exception {
        final Properties fixtureProperties = SmtpMockFixtures.loadMockSessionProperties();
        assertEquals("smtp", fixtureProperties.getProperty("mail.transport.protocol"));
        assertEquals(SmtpMockFixtures.MOCK_HOST, fixtureProperties.getProperty("mail.smtp.host"));
        assertEquals(String.valueOf(SmtpMockFixtures.MOCK_PORT), fixtureProperties.getProperty("mail.smtp.port"));
        assertEquals("true", fixtureProperties.getProperty("mail.smtp.auth"));
        assertEquals("true", fixtureProperties.getProperty("mail.smtp.starttls.enable"));
        assertEquals("true", fixtureProperties.getProperty("mail.smtps.ssl.enable"));
    }
}
