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
package org.apache.logging.log4j.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.logging.log4j.core.impl.ContextAnchor;
import org.apache.logging.log4j.web.fixtures.MockJakartaFilterChain;
import org.apache.logging.log4j.web.fixtures.MockJakartaFilterConfig;
import org.apache.logging.log4j.web.fixtures.MockJakartaServletContext;
import org.apache.logging.log4j.web.fixtures.MockJakartaServletRequest;
import org.apache.logging.log4j.web.fixtures.MockJakartaServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JakartaServletLifecycleIntegrationTest {

    private MockJakartaServletContext servletContext;

    @BeforeEach
    void setUp() throws IOException {
        ContextAnchor.THREAD_CONTEXT.remove();
        servletContext = new MockJakartaServletContext();
        try (InputStream in = getClass().getResourceAsStream("/fixtures/jakarta-servlet-context.properties")) {
            assertNotNull(in, "Fixture properties should be available on the test classpath");
            final Properties properties = new Properties();
            properties.load(in);
            servletContext.setContextPath(properties.getProperty("contextPath", "/test-app"));
            servletContext.setMajorVersion(Integer.parseInt(properties.getProperty("servletMajorVersion", "5")));
            servletContext.setEffectiveMajorVersion(
                    Integer.parseInt(properties.getProperty("servletEffectiveMajorVersion", "5")));
        }
    }

    @AfterEach
    void tearDown() {
        ContextAnchor.THREAD_CONTEXT.remove();
    }

    @Test
    void containerInitializerRegistersFilterAndListener() throws Exception {
        new Log4jServletContainerInitializer().onStartup(null, servletContext);

        assertEquals(Log4jServletFilter.class, servletContext.getRegisteredFilters().get("log4jServletFilter"));
        assertEquals(1, servletContext.getRegisteredListeners().size());
        assertInstanceOf(
                Log4jServletContextListener.class,
                servletContext.getRegisteredListeners().iterator().next());
        assertNotNull(servletContext.getAttribute(Log4jWebSupport.SUPPORT_ATTRIBUTE));
    }

    @Test
    void filterAndListenerManageLoggerContextAcrossRequestLifecycle() throws Exception {
        new Log4jServletContainerInitializer().onStartup(null, servletContext);

        final Log4jServletFilter filter = new Log4jServletFilter();
        filter.init(new MockJakartaFilterConfig("log4jServletFilter", servletContext));

        final MockJakartaServletRequest request = new MockJakartaServletRequest();
        request.setServletContext(servletContext);
        final MockJakartaServletResponse response = new MockJakartaServletResponse();
        final MockJakartaFilterChain chain = new MockJakartaFilterChain();

        assertNull(ContextAnchor.THREAD_CONTEXT.get(), "Thread context should be clear before the request");
        filter.doFilter(request, response, chain);
        assertTrue(chain.wasInvoked(), "Filter chain should have been invoked");
        assertNull(ContextAnchor.THREAD_CONTEXT.get(), "Thread context should be cleared after the request");
        assertNull(
                request.getAttribute(Log4jServletFilter.ALREADY_FILTERED_ATTRIBUTE),
                "Per-request filter marker should be removed");

        final ServletContextListener listener = servletContext.getRegisteredListeners().stream()
                .filter(Log4jServletContextListener.class::isInstance)
                .map(ServletContextListener.class::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Log4jServletContextListener was not registered"));
        listener.contextDestroyed(new ServletContextEvent(servletContext));

        filter.destroy();
    }

    @Test
    void servletSixMajorVersionIsSupported() throws Exception {
        servletContext.setMajorVersion(6);
        servletContext.setEffectiveMajorVersion(6);

        new Log4jServletContainerInitializer().onStartup(null, servletContext);

        assertSame(Log4jServletFilter.class, servletContext.getRegisteredFilters().get("log4jServletFilter"));
    }
}
