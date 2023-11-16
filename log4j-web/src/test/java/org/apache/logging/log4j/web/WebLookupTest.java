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

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import javax.servlet.ServletContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.impl.ContextAnchor;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockServletContext;

public class WebLookupTest {

    @Test
    public void testLookup() throws Exception {
        ContextAnchor.THREAD_CONTEXT.remove();
        final ServletContext servletContext = new MockServletContext();
        ((MockServletContext) servletContext).setContextPath("/WebApp");
        servletContext.setAttribute("TestAttr", "AttrValue");
        servletContext.setInitParameter("TestParam", "ParamValue");
        servletContext.setAttribute("Name1", "Ben");
        servletContext.setInitParameter("Name2", "Jerry");
        final Log4jWebLifeCycle initializer = WebLoggerContextUtils.getWebLifeCycle(servletContext);
        try {
            initializer.start();
            initializer.setLoggerContext();
            final LoggerContext ctx = ContextAnchor.THREAD_CONTEXT.get();
            assertNotNull(ctx, "No LoggerContext");
            assertNotNull(WebLoggerContextUtils.getServletContext(), "No ServletContext");
            final Configuration config = ctx.getConfiguration();
            assertNotNull(config, "No Configuration");
            final StrSubstitutor substitutor = config.getStrSubstitutor();
            assertNotNull(substitutor, "No Interpolator");
            String value = substitutor.replace("${web:initParam.TestParam}");
            assertNotNull(value, "No value for TestParam");
            assertEquals("ParamValue", value, "Incorrect value for TestParam: " + value);
            value = substitutor.replace("${web:attr.TestAttr}");
            assertNotNull(value, "No value for TestAttr");
            assertEquals("AttrValue", value, "Incorrect value for TestAttr: " + value);
            value = substitutor.replace("${web:Name1}");
            assertNotNull(value, "No value for Name1");
            assertEquals("Ben", value, "Incorrect value for Name1: " + value);
            value = substitutor.replace("${web:Name2}");
            assertNotNull(value, "No value for Name2");
            assertEquals("Jerry", value, "Incorrect value for Name2: " + value);
            value = substitutor.replace("${web:contextPathName}");
            assertNotNull(value, "No value for context name");
            assertEquals("WebApp", value, "Incorrect value for context name");
        } catch (final IllegalStateException e) {
            fail("Failed to initialize Log4j properly." + e.getMessage());
        }
        initializer.stop();
        ContextAnchor.THREAD_CONTEXT.remove();
    }

    @Test
    public void testLookup2() throws Exception {
        ContextAnchor.THREAD_CONTEXT.remove();
        final ServletContext servletContext = new MockServletContext();
        ((MockServletContext) servletContext).setContextPath("/");
        servletContext.setAttribute("TestAttr", "AttrValue");
        servletContext.setInitParameter("myapp.logdir", "target");
        servletContext.setAttribute("Name1", "Ben");
        servletContext.setInitParameter("Name2", "Jerry");
        servletContext.setInitParameter("log4jConfiguration", "WEB-INF/classes/log4j-webvar.xml");
        final Log4jWebLifeCycle initializer = WebLoggerContextUtils.getWebLifeCycle(servletContext);
        initializer.start();
        initializer.setLoggerContext();
        final LoggerContext ctx = ContextAnchor.THREAD_CONTEXT.get();
        assertNotNull(ctx, "No LoggerContext");
        assertNotNull(WebLoggerContextUtils.getServletContext(), "No ServletContext");
        final Configuration config = ctx.getConfiguration();
        assertNotNull(config, "No Configuration");
        final Map<String, Appender> appenders = config.getAppenders();
        for (final Map.Entry<String, Appender> entry : appenders.entrySet()) {
            if (entry.getKey().equals("file")) {
                final FileAppender fa = (FileAppender) entry.getValue();
                assertEquals("target/myapp.log", fa.getFileName());
            }
        }
        final StrSubstitutor substitutor = config.getStrSubstitutor();
        final String value = substitutor.replace("${web:contextPathName:-default}");
        assertEquals("default", value, "Incorrect value for context name");
        assertNotNull(value, "No value for context name");
        initializer.stop();
        ContextAnchor.THREAD_CONTEXT.remove();
    }
}
