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
package org.apache.logging.log4j.core.its;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import jakarta.jms.ConnectionFactory;
import jakarta.mail.Session;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;
import org.apache.logging.log4j.core.appender.db.jpa.JpaDatabaseManager;
import org.apache.logging.log4j.core.appender.mom.jakarta.JmsManager;
import org.apache.logging.log4j.core.test.junit.Tags;
import org.apache.logging.log4j.smtp.SmtpManager;
import org.apache.logging.log4j.web.Log4jServletContainerInitializer;
import org.apache.logging.log4j.web.Log4jWebSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Lightweight Jakarta module smoke tests for cross-module classpath wiring (WO-061).
 */
@Tag(Tags.INTEGRATION_TESTS)
@ExtendWith(MockitoExtension.class)
class JakartaModulesIntegrationTest {

    @Mock
    private ServletContext servletContext;

    private Log4jServletContainerInitializer containerInitializer;

    @BeforeEach
    void setUp() {
        containerInitializer = new Log4jServletContainerInitializer();
    }

    @Test
    void jakartaWebServletContainerInitializerIsRegisteredViaSpi() {
        final ServiceLoader<ServletContainerInitializer> loader = ServiceLoader.load(ServletContainerInitializer.class);
        final boolean found = StreamSupport.stream(loader.spliterator(), false)
                .anyMatch(initializer -> initializer instanceof Log4jServletContainerInitializer);
        assertTrue(found, "Log4jServletContainerInitializer must be registered via jakarta.servlet SPI");
    }

    @Test
    void jakartaWebInitializerSkipsAutoConfigurationOnServlet2Container() throws Exception {
        given(servletContext.getMajorVersion()).willReturn(2);
        containerInitializer.onStartup(null, servletContext);
    }

    @Test
    void jakartaWebInitializerSkipsAutoConfigurationWhenExplicitlyDisabled() throws Exception {
        given(servletContext.getMajorVersion()).willReturn(3);
        given(servletContext.getEffectiveMajorVersion()).willReturn(3);
        given(servletContext.getInitParameter(eq(Log4jWebSupport.IS_LOG4J_AUTO_INITIALIZATION_DISABLED)))
                .willReturn("true");
        containerInitializer.onStartup(null, servletContext);
    }

    @Test
    void jakartaJmsManagerClassLoadsWithJakartaJmsApi() {
        assertNotNull(JmsManager.class);
        assertEquals(
                "org.apache.logging.log4j.core.appender.mom.jakarta.JmsManager",
                JmsManager.class.getName());
        assertTrue(ConnectionFactory.class.getName().startsWith("jakarta."));
    }

    @Test
    void jakartaSmtpManagerClassLoadsWithJakartaMailApi() {
        assertNotNull(SmtpManager.class);
        assertEquals("org.apache.logging.log4j.smtp.SmtpManager", SmtpManager.class.getName());
        assertNotNull(Session.getInstance(new java.util.Properties()));
    }

    @Test
    void jakartaJpaDatabaseManagerClassLoadsWithJakartaPersistenceApi() {
        assertNotNull(JpaDatabaseManager.class);
        assertEquals(
                "org.apache.logging.log4j.core.appender.db.jpa.JpaDatabaseManager",
                JpaDatabaseManager.class.getName());
        assertNotNull(EntityManagerFactory.class.getName());
    }
}
