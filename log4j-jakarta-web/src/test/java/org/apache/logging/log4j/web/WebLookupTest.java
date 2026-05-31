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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.ServletContext;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.composite.CompositeConfiguration;
import org.apache.logging.log4j.core.impl.ContextAnchor;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class WebLookupTest {

    @AfterEach
    void tearDown() {
        ContextAnchor.THREAD_CONTEXT.remove();
    }

    // TODO: re-enable when https://github.com/spring-projects/spring-framework/issues/25354 is fixed

    //    @Test
    //    public void testLookup() throws Exception {
    //        ContextAnchor.THREAD_CONTEXT.remove();
    //        final ServletContext servletContext = new MockServletContext();
    //        ((MockServletContext) servletContext).setContextPath("/WebApp");
    //        servletContext.setAttribute("TestAttr", "AttrValue");
    //        servletContext.setInitParameter("TestParam", "ParamValue");
    //        servletContext.setAttribute("Name1", "Ben");
    //        servletContext.setInitParameter("Name2", "Jerry");
    //        final Log4jWebLifeCycle initializer = WebLoggerContextUtils.getWebLifeCycle(servletContext);
    //        try {
    //            initializer.start();
    //            initializer.setLoggerContext();
    //            final LoggerContext ctx = ContextAnchor.THREAD_CONTEXT.get();
    //            assertNotNull(ctx, "No LoggerContext");
    //            assertNotNull(WebLoggerContextUtils.getServletContext(), "No ServletContext");
    //            final Configuration config = ctx.getConfiguration();
    //            assertNotNull(config, "No Configuration");
    //            final StrSubstitutor substitutor = config.getStrSubstitutor();
    //            assertNotNull(substitutor, "No Interpolator");
    //            String value = substitutor.replace("${web:initParam.TestParam}");
    //            assertNotNull(value, "No value for TestParam");
    //            assertEquals("ParamValue", value, "Incorrect value for TestParam: " + value);
    //            value = substitutor.replace("${web:attr.TestAttr}");
    //            assertNotNull(value, "No value for TestAttr");
    //            assertEquals("AttrValue", value, "Incorrect value for TestAttr: " + value);
    //            value = substitutor.replace("${web:Name1}");
    //            assertNotNull(value, "No value for Name1");
    //            assertEquals("Ben", value, "Incorrect value for Name1: " + value);
    //            value = substitutor.replace("${web:Name2}");
    //            assertNotNull(value, "No value for Name2");
    //            assertEquals("Jerry", value, "Incorrect value for Name2: " + value);
    //            value = substitutor.replace("${web:contextPathName}");
    //            assertNotNull(value, "No value for context name");
    //            assertEquals("WebApp", value, "Incorrect value for context name");
    //        } catch (final IllegalStateException e) {
    //            fail("Failed to initialize Log4j properly." + e.getMessage());
    //        }
    //        initializer.stop();
    //        ContextAnchor.THREAD_CONTEXT.remove();
    //    }
    //
    //    @Test
    //    public void testLookup2() throws Exception {
    //        ContextAnchor.THREAD_CONTEXT.remove();
    //        final ServletContext servletContext = new MockServletContext();
    //        ((MockServletContext) servletContext).setContextPath("/");
    //        servletContext.setAttribute("TestAttr", "AttrValue");
    //        servletContext.setInitParameter("myapp.logdir", "target");
    //        servletContext.setAttribute("Name1", "Ben");
    //        servletContext.setInitParameter("Name2", "Jerry");
    //        servletContext.setInitParameter("log4jConfiguration", "WEB-INF/classes/log4j-webvar.xml");
    //        final Log4jWebLifeCycle initializer = WebLoggerContextUtils.getWebLifeCycle(servletContext);
    //        initializer.start();
    //        initializer.setLoggerContext();
    //        final LoggerContext ctx = ContextAnchor.THREAD_CONTEXT.get();
    //        assertNotNull(ctx, "No LoggerContext");
    //        assertNotNull(WebLoggerContextUtils.getServletContext(), "No ServletContext");
    //        final Configuration config = ctx.getConfiguration();
    //        assertNotNull(config, "No Configuration");
    //        final Map<String, Appender> appenders = config.getAppenders();
    //        for (final Map.Entry<String, Appender> entry : appenders.entrySet()) {
    //            if (entry.getKey().equals("file")) {
    //                final FileAppender fa = (FileAppender) entry.getValue();
    //                assertEquals("target/myapp.log", fa.getFileName());
    //            }
    //        }
    //        final StrSubstitutor substitutor = config.getStrSubstitutor();
    //        String value = substitutor.replace("${web:contextPathName:-default}");
    //        assertEquals("default", value, "Incorrect value for context name");
    //        assertNotNull(value, "No value for context name");
    //        initializer.stop();
    //        ContextAnchor.THREAD_CONTEXT.remove();
    //    }
    /**
     * Regression test for GitHub issue #2351:
     * "Missing servlet context in web lookup when using composite configuration".
     *
     * When log4jConfiguration contains a comma-separated list of config files,
     * the resulting composite LoggerContext must still expose the ServletContext
     * via WebLoggerContextUtils.getServletContext() so that ${web:*} lookups resolve.
     */
    @Test
    void testCompositeConfigurationServletContextName() throws Exception {
        ContextAnchor.THREAD_CONTEXT.remove();

        final String expectedServletContextName = "CompositeTest";

        // Use Mockito to create a minimal ServletContext (no Spring dependency needed)
        final ServletContext servletContext = mock(ServletContext.class);
        when(servletContext.getServletContextName()).thenReturn(expectedServletContextName);
        when(servletContext.getContextPath()).thenReturn("/composite-test");
        // Composite configuration: two comma-separated config files
        when(servletContext.getInitParameter(Log4jWebSupport.LOG4J_CONFIG_LOCATION))
                .thenReturn("log4j2-combined.xml,log4j2-override.xml");
        // Let the initializer resolve each file via the servlet context resource lookup
        when(servletContext.getResource("log4j2-combined.xml"))
                .thenReturn(getClass().getResource("/log4j2-combined.xml"));
        when(servletContext.getResource("log4j2-override.xml"))
                .thenReturn(getClass().getResource("/log4j2-override.xml"));

        final Log4jWebLifeCycle initializer = WebLoggerContextUtils.getWebLifeCycle(servletContext);
        try {
            initializer.start();
            initializer.setLoggerContext();

            final LoggerContext ctx = ContextAnchor.THREAD_CONTEXT.get();
            assertNotNull(ctx, "No LoggerContext");

            // The servlet context MUST be reachable via the web lookup for composite config.
            // Before the fix this returns null, breaking all ${web:*} lookups.
            assertNotNull(
                    WebLoggerContextUtils.getServletContext(),
                    "ServletContext is null in composite configuration - "
                            + "${web:*} lookups will not resolve (issue #2351)");

            final Configuration config = ctx.getConfiguration();
            assertNotNull(config, "No Configuration");
            assertInstanceOf(
                    CompositeConfiguration.class,
                    config,
                    "Expected CompositeConfiguration for comma-separated log4jConfiguration");
            final StrSubstitutor substitutor = config.getStrSubstitutor();
            assertNotNull(substitutor, "No StrSubstitutor");

            // Core assertion: ${web:servletContextName} must resolve to the actual name
            final String value = substitutor.replace("${web:servletContextName}");
            assertEquals(
                    expectedServletContextName,
                    value,
                    "${web:servletContextName} did not resolve in composite configuration (issue #2351)");
        } finally {
            initializer.stop();
            ContextAnchor.THREAD_CONTEXT.remove();
        }
    }
}
